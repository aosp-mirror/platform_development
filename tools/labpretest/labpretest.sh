#!/bin/bash
#
# Copyright 2010 Google Inc. All Rights Reserved.
# Author: bgay@google.com (Bruce Gay)
#
# The labpretest.sh script is designed to emulate a typical automated test lab
# session.  It puts a device into bootloader mode, reboots into bootloader mode,
# determines device type, erases user cache, flashes a generic userdata image,
# updates the bootloader image, updates the radio image, updates the system
# image and reboots, sets up for a monkey run and finally runs a random monkey
# test. It will repeat this based on an optional parameter(-i) or default to 100
# times. It will detect if it is in a low battery situation and wait for it to
# charge again.


COUNT=100
ROOT=$(cd `dirname $0` && pwd)
ADB="$ROOT/tools/adb"
FASTBOOT="$ROOT/tools/fastboot"
MEVENTS=200
NOMONKEY=0

buildfile=''
device=''
product=''
bootpart=''
bootfile=''

while getopts "d:i::m:xh" optionName; do
  case "$optionName" in
    d) device="$OPTARG";;
    i) COUNT=$OPTARG;;
    m) MEVENTS=$OPTARG;;
    x) NOMONKEY=1;;
    h) echo "options: [-d <device ID>, -i <loop count>, -m <monkey events> -x (skips monkey)]"; exit;;
    *) echo "invalid parameter -$optionName"; exit -1;;
  esac
done

declare -r COUNT
declare -r MEVENTS
declare -r NOMONKEY


################################################
# Prints output to console with time stamp
# Arguments:
#   None
# Returns:
#   None
################################################
log_print()
{
  if [ -z "$1" ]; then
    echo "# $(date +'%D %T')"
  else
    echo "# $(date +'%D %T'): $1"
  fi
}

################################################
# Blocks until battery level is at least
# above TARGET if below LIMIT
# Globals:
#   ADB
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
wait_for_battery()
{
  TARGET=80
  LIMIT=20
  local battery
  local tick
  log_print "checking battery level"
  while [ "$battery" = "" ]; do
    battery=`$ADB -s $device shell dumpsys battery | tr -d '\r' | awk '/level:/ {print $2}'`
    sleep 2
  done
  if [ $battery -lt $LIMIT ]; then
    log_print "Battery is low, waiting for charge"
    while true; do
      battery=`$ADB -s $device shell dumpsys battery | tr -d '\r' | awk '/level:/ {print $2}'`
      if (( $battery >= $TARGET )); then break; fi
      tick=$[$TARGET - $battery]
      echo "battery charge level is $battery, sleeping for $tick seconds"
      sleep $[$TARGET - $battery * 10]
    done
    log_print "resuming test run with battery level at $battery%"
  else
    log_print "resuming test run with battery level at $battery%"
  fi
}

################################################
# Blocks until device is in fastboot mode or
# time out is reached
# Globals:
#   loop
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
fastboot_wait_for_device()
{
  local fdevice=""
  local n=0
  while [ "$device" != "$fdevice" -a $n -le 30 ]; do
    sleep 6
    fdevice=`$FASTBOOT devices | sed -n "s/\($device\).*/\1/ p"`
    let n+=1
  done
  if [ $n -gt 30 ]; then
    log_print "device time out after $loop iterations"
    exit
  else
    log_print "device returned and available"
  fi
}

################################################
# reboots device into fastboot mode or
# time out is reached
# Globals:
#   device
#   ADB
# Arguments:
#   None
# Returns:
#   None
################################################
reboot_into_fastboot_from_adb()
{
  log_print "rebooting into bootloader and waiting for availability via fastboot"
  $ADB -s $device reboot bootloader
  fastboot_wait_for_device
}

################################################
# reboots device into fastboot mode or
# times out
# Globals:
#   device
#   FASTBOOT
# Arguments:
#   None
# Returns:
#   None
################################################
reboot_into_fastboot_from_fastboot()
{
  log_print "rebooting into bootloader and waiting for availability via fastboot"
  $FASTBOOT -s $device reboot-bootloader
  fastboot_wait_for_device
}

################################################
# reboots device from fastboot to adb or
# times out
# Globals:
#   device
#   FASTBOOT
#   ADB
# Arguments:
#   None
# Returns:
#   None
################################################
reboot_into_adb_from_fastboot()
{
  log_print "rebooting and waiting for availability via adb"
  $FASTBOOT -s $device reboot
  $ADB -s $device wait-for-device
}

################################################
# reboots device from fastboot to adb or
# times out
# Globals:
#   device
#   ADB
# Arguments:
#   None
# Returns:
#   None
################################################
wait_for_boot_complete()
{
  log_print "waiting for device to finish booting"
  local result=$($ADB -s $device shell getprop dev.bootcomplete)
  local result_test=${result:1:1}
  echo -n "."
  while [ -z $result_test ]; do
    sleep 1
    echo -n "."
    result=$($ADB -s $device shell getprop dev.bootcomplete)
    result_test=${result:0:1}
  done
  log_print "finished booting"
}

################################################
# fastboot flashes partition
#
# Globals:
#   device
#   FASTBOOT
# Arguments:
#   command_name
#   command_parameters
# Returns:
#   None
################################################
fastboot_command()
{
  $FASTBOOT -s $device $1 $2 $3
  sleep 5
}

################################################
# fastboot command wrapper
#
# Globals:
#   device
#   FASTBOOT
# Arguments:
#   partition_name
#   file_name
# Returns:
#   None
################################################
flash_partition()
{
  $FASTBOOT -s $device flash $1 $2
  sleep 5
}

################################################
# adb command wrapper
#
# Globals:
#   device
#   ADB
# Arguments:
#   command_name
#   command_parameters
# Returns:
#   None
################################################
adb_command()
{
  $ADB -s $device $1 $2 $3 $4 $5
  sleep 5
}

################################################
# sets the name of the boot partition and
# bootfile, then flashes device
#
# Globals:
#   product
#   ROOT
#   bootloaderfile
#   bootpart
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
flash_bootloader_image()
{
  if [ "$bootpart" == '' ]; then
    log_print "bootpart not defined"
    exit
  fi
  if [ "$bootloaderfile" == '' ]; then
    log_print "getting bootloader file for $product"
    bootloaderfile=`ls -1 $ROOT/$product | sed -n 's/\(.*boot[0-9._]\+img\)/\1/ p'`
    if [ "$bootloaderfile" == '' ]; then
      log_print "bootloader file empty: $bootloaderfile"
      exit
    fi
    if [ ! -e "$ROOT/$product/$bootloaderfile" ]; then
      log_print "bootloader file not found: ./$product/$bootloaderfile"
      exit
    fi
    log_print "using $ROOT/$product/$bootloaderfile as the bootloader image file"
  fi
  log_print "downloading bootloader image to $device"
  flash_partition $bootpart $ROOT/$product/$bootloaderfile
  reboot_into_fastboot_from_fastboot
}

################################################
# sets the name of the radio partition and
# radiofile and flashes device
#
# Globals:
#   product
#   ROOT
#   radiofile
#   radiopart
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
flash_radio_image()
{
  if [ "$radiopart" == '' ]; then
    log_print "setting radio partion to 'radio'"
    radiopart='radio'
  fi
  if [ "$radiofile" == "" ]; then
    log_print "getting radio file for $product"
    radiofile=`ls -1 $ROOT/$product | sed -n 's/\(radio[0-9._A-Za-z]\+img\)/\1/ p'`
    if [ "$radiofile" == "" ]; then
      log_print "radio file empty: $radiofile"
      exit
    fi
    if [ ! -e "$ROOT/$product/$radiofile" ]; then
      log_print "radio file not found: ./$product/$radiofile"
      exit
    fi
    log_print "using $ROOT/$product/$radiofile as the radio image file"
  fi
  log_print "downloading radio image to $device"
  flash_partition $radiopart  $ROOT/$product/$radiofile
  reboot_into_fastboot_from_fastboot
}

################################################
# sets the name of the boot partition and
# bootfile
#
# Globals:
#   product
#   ROOT
#   buildfile
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
flash_system_image()
{
  if [ "$buildfile" == "" ]; then
    log_print "getting build file for $product"
    buildfile=`\ls -1 $ROOT/$product 2>&1 | sed -n 's/\([a-z]\+-img-[0-9]\+.zip\)/\1/ p'`
    if [ "$buildfile" == "" ]; then
      log_print "build file empty: $buildfile"
      exit
    fi
    if [ ! -e "$ROOT/$product/$buildfile" ]; then
      log_print "build file not found: ./$product/$buildfile"
      exit
    fi
    log_print "using $ROOT/$product/$buildfile as the system image file"
  fi
  log_print "downloading system image to $device"
  fastboot_command update $ROOT/$product/$buildfile

}
################################################
# flashes the userdata partition
#
# Globals:
#   product
#   ROOT
# Arguments:
#   None
# Returns:
#   None
################################################
flash_userdata_image()
{
  log_print "flashing userdata..."
  if [ -e $ROOT/$product/userdata.img ];then
    flash_partition userdata $ROOT/$product/userdata.img
  else
    log_print "userdata.img file not found: $ROOT/$product/userdata.img"
    exit
  fi
}


################################################
# flashes the device
#
# Globals:
#   product
#   ROOT
#   FASTBOOT
#   bootfile
#   bootpart
#   radiofile
# Arguments:
#   None
# Returns:
#   None
################################################
flash_device()
{
  log_print "erasing cache..."
  fastboot_command erase cache
  flash_userdata_image
  flash_bootloader_image
  flash_radio_image
  flash_system_image
  #device has been rebooted
  adb_command wait-for-device
}

################################################
# gets the device product type and sets product
#
# Globals:
#   product
#   ROOT
#   FASTBOOT
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
set_product_type()
{
  if [ "$product" == "" ]; then
    log_print "getting device product type"
    product=`$FASTBOOT -s $device getvar product 2>&1 | sed -n 's/product: \([a-z]*\)\n*/\1/ p'`
    if [ ! -e "$ROOT/$product" ]; then
      log_print "device product id not supported: $product"
      exit
    fi
  fi
  log_print "using $product as device product id"
}



#start of script
#test for dependencies
if [ ! -e $ADB ]; then
  echo "Error: adb not in path! Please correct this."
  exit
fi
if [ ! -e $FASTBOOT ]; then
  echo "Error: fastboot not in path! Please correct this."
  exit
fi
#checks to see if the called device is available
if [ "$device" != "" ]; then
  tmpdevice=`$ADB devices | sed -n "s/\($device\).*/\1/ p"`
  if [ "$device" != "$tmpdevice" ]; then
      tmpdevice=`$FASTBOOT devices | sed -n "s/\($device\).*/\1/ p"`
    if [ "$device" != "$tmpdevice" ]; then
      echo "Warning: device not found... $device"
      exit
    else
      echo "'Device '$device' found!'"
      reboot_into_adb_from_fastboot
      wait_for_boot_complete
    fi
  fi
else
  device=`$ADB devices | sed -n 's/.*\(^[0-9A-Z]\{2\}[0-9A-Z]*\).*/\1/ p'`
  if [ `echo $device | wc -w` -ne 1 ]; then
    echo 'There is more than one device found,'
    echo 'please pass the correct device ID in as a parameter.'
    exit
  fi
fi
if [ "$device" == "" ]; then
  echo 'Device not found via adb'
  device=`$FASTBOOT devices | sed -n 's/.*\(^[0-9A-Z]\{2\}[0-9A-Z]*\).*/\1/ p'`
  if [ `echo $device | wc -w` -ne 1 ]; then
    echo "There is more than one device available,"
    echo "please pass the correct device ID in as a parameter."
    exit
  fi
  if [ "$device" == "" ]; then
    echo 'Device not found via fastboot, please investigate'
    exit
  else
    echo 'Device '$device' found!'
    reboot_into_adb_from_fastboot
    wait_for_boot_complete
    echo 'Hammering on '$device
  fi
else
  echo 'Hammering on '$device
fi
reboot_into_fastboot_from_adb
set_product_type
reboot_into_adb_from_fastboot
wait_for_boot_complete

#check for availability of a custom flash info file and retreive it
if [ -e "$ROOT/$product/custom_flash.sh" ]; then
  . $ROOT/$product/custom_flash.sh
fi
echo $'\n\n'

#start of looping
for ((loop=1 ; loop <= $COUNT ; loop++ )) ; do
  echo ""
  echo ""
  echo ________________ $(date +'%D %T') - $loop - $device ______________________

  log_print "setting adb root and sleeping for 7 seconds"
  adb_command root
  wait_for_battery
  log_print "rebooting into bootloader and waiting for availability via fastboot"
  reboot_into_fastboot_from_adb
  # not necessary, but useful in testing
  log_print "using fastboot to reboot to bootloader for test purposes"
  reboot_into_fastboot_from_fastboot

  #flashing the device
  flash_device

  #preping device for monkey run
  log_print "setting adb root"
  adb_command root
  log_print "setting ro.test_harness property"
  adb_command shell setprop ro.test_harness 1

  log_print "waiting for device to finish booting"
  result=$($ADB -s $device shell getprop dev.bootcomplete)
  result_test=${result:1:1}
  echo -n "."
  while [ -z $result_test ]; do
    sleep 1
    echo -n "."
    result=$($ADB -s $device shell getprop dev.bootcomplete)
    result_test=${result:0:1}
  done

  log_print "finished booting"
  log_print "waiting for the Package Manager"
  result=$($ADB -s $device shell pm path android)
  result_test=${result:0:7}
  echo -n "."
  while [ $result_test != "package" ]; do
    sleep 1
    echo -n "."
    result=$($ADB -s $device shell pm path android)
    result_test=${result:0:7}
  done
  echo "Package Manager available"

  #lets you see what's going on
  log_print "setting shell svc power stayon true"
  adb_command shell svc power stayon true

  #calls the monkey run if not skipped
  if [ $NOMONKEY == 0 ]; then
    seed=$(($(date +%s) % 99))
    log_print "running short monkey run..."
    $ADB -s $device shell monkey -p com.android.alarmclock -p com.android.browser -p com.android.calculator2 -p com.android.calendar -p com.android.camera -p com.android.contacts -p com.google.android.gm -p com.android.im -p com.android.launcher -p com.google.android.apps.maps -p com.android.mms -p com.android.music -p com.android.phone -p com.android.settings -p com.google.android.street -p com.android.vending -p com.google.android.youtube -p com.android.email -p com.google.android.voicesearch  -c android.intent.category.LAUNCHER  --ignore-security-exceptions  -s $seed $MEVENTS
    log_print "finished running monkey, rinse, repeat..."
  else
    log_print "-x parameter used, skipping the monkey run"
  fi

  if [ $loop -eq $COUNT ]; then
    log_print "device $device has returned, testing completed, count = $loop"
    echo `echo "Device $device has returned, testing completed, count = $loop." > $ROOT/$device.log`
  else
    log_print "device $device has returned, rinse and repeat count = $loop"
    echo `echo "Device $device has returned, rinse and repeat count = $loop." > $ROOT/$device.log`
  fi
done

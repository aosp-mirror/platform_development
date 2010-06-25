#!/bin/bash
#
# Copyright 2010 Google Inc. All Rights Reserved.
# Author: bgay@google.com (Bruce Gay)
#
# used for flashing bootloader image on sholes

BOOTPART='motoboot'

################################################
# sets the name of the boot partition and
# bootfile, then flashes device
#
# Globals:
#   product
#   ROOT
#   BOOTPART
#   bootloaderfile
#   device
# Arguments:
#   None
# Returns:
#   None
################################################
flash_bootloader_image()
{
  if [ $product != "sholes" ]; then
    log_print "Wrong device type, expected sholes!"
    exit
  fi
  if [ "$bootloaderfile" == '' ]; then
    log_print "getting bootloader file for $product"
    secure=`$fastboot -s $device getvar secure 2>&1 | sed -n 's/secure: \([a-z]*\)\n*/\1/ p'`
    if [ "$secure" = "no" ]; then
      bootloaderfile=`ls -1 sholes/ | sed -n 's/^\(motoboot_unsecure.[0-9A-Z]*.img\)\n*/\1/ p'`
    else
      bootloaderfile=`ls -1 sholes/ | sed -n 's/^\(motoboot_secure.[0-9A-Z]*.img\)\n*/\1/ p'`
    fi
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
  flash_partition $BOOTPART $ROOT/$product/$bootloaderfile
  reboot_into_fastboot_from_fastboot
}

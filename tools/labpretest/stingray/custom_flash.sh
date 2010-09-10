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
  if [ $product != "stingray" ]; then
    log_print "Wrong device type, expected stingray!"
    exit
  fi
  if [ "$bootloaderfile" == '' ]; then
    log_print "getting bootloader file for $product"
    bootloaderfile=`ls -1 $product/ | sed -n 's/^\(motoboot.[0-9A-Z]*.img\)\n*/\1/ p'`
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
  #currently not supported so exiting early..."
  log_print "skipping update of userdata.img, not supported yet."
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
  #currently not supported so exiting early..."
  log_print "skipping update of radio partition, not supported yet."
}

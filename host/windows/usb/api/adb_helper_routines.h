/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_USB_API_ADB_HELPER_ROUTINES_H__
#define ANDROID_USB_API_ADB_HELPER_ROUTINES_H__
/** \file
  This file consists of declarations of helper routines used
  in the API.
*/

#include "adb_api_private_defines.h"

/** \brief Converts access type and share mode from our enum into
  SDK - complient values.

  @param[in] access_type Enumerated access type
  @param[in] sharing_mode Enumerated share mode
  @param[out] desired_access Will receive SDK - complient desired access
         flags. This parameter can be NULL.
  @param[out] desired_sharing Will receive SDK - complient share mode.
         This parameter can be NULL.
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool GetSDKComplientParam(AdbOpenAccessType access_type,
                          AdbOpenSharingMode sharing_mode,
                          ULONG* desired_access,
                          ULONG* desired_sharing);

/** \brief Given the hardware device information enumerates interfaces for
  this device.

  @param[in] hardware_dev_info A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param[in] class_id Device class ID how it is specified by our USB driver.
  @param[in] exclude_removed If true interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param[in] active_only If true only active interfaces (with flag
         SPINT_ACTIVE set) will be included in the enumeration.
  @param[out] interfaces Upon successfull completion will consist of array of
         all interfaces found for this device (matching all filters).
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool EnumerateDeviceInterfaces(HDEVINFO hardware_dev_info,
                               GUID class_id,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces);

/** \brief Enumerates all interfaces for our device class.

  This routine uses SetupDiGetClassDevs to get our device info and calls
  EnumerateDeviceInterfaces to perform the enumeration.
  @param[in] class_id Device class ID how it is specified by our USB driver
  @param[in] flags Flags to pass to SetupDiGetClassDevs to filter devices. See
         SetupDiGetClassDevs() in SDK for more info on these flags.
  @param[in] exclude_removed If true interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param[in] active_only If true only active interfaces (with flag
         SPINT_ACTIVE set) will be included in the enumeration.
  @param[out] interfaces Upon successfull completion will consist of array of
         all interfaces found for this device (matching all filters).
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool EnumerateDeviceInterfaces(GUID class_id,
                               ULONG flags,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces);

/** \brief Given the hardware device information and data gets data details.

  Given the hardware_dev_info, representing a handle to the plug and
  play information, and dev_info_data, representing a specific usb device,
  gets detailed data about the device (interface).
  @param[in] hardware_dev_info A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param[in] dev_info_data Device information data obtained via call to
         SetupDiEnumDeviceInterfaces()
  @param[out] dev_info_detail_data Upon successfull completion will consist of
         the detailed data about device interface. This routine always
         allocates memory for the output structure so content of this pointer
         doesn't matter and will be overwritten by this routine. The caller
         of this method is responsible for freeing allocated data using free()
         routine.
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool GetUsbDeviceDetails(HDEVINFO hardware_dev_info,
                       PSP_DEVICE_INTERFACE_DATA dev_info_data,
                       PSP_DEVICE_INTERFACE_DETAIL_DATA* dev_info_detail_data);

/** \brief Given the hardware device information and data gets device name.

  Given the hardware_dev_info, representing a handle to the plug and
  play information, and dev_info_data, representing a specific usb device,
  gets device name. This routine uses GetUsbDeviceDetails to extract device
  name.
  @param[in] hardware_dev_info A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param[in] dev_info_data Device information data obtained via call to
         SetupDiEnumDeviceInterfaces()
  @param[out] name Upon successfull completion will have name for the device.
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool GetUsbDeviceName(HDEVINFO hardware_dev_info,
                      PSP_DEVICE_INTERFACE_DATA dev_info_data,
                      std::wstring* name);

/** \brief Checks if given interface is available via custom USB driver.

  In this routine we will query the given interface with an IOCTL that should
  be supported by the custom driver. If IOCTL fails, we make a conclusion that
  this interface is available via WinUsb, and not via custom USB driver.
  @param[in] interface_name Name of the interface to check.
  @return true if interface is available via custom USB driver, or false
          otherwise.
*/
bool IsLegacyInterface(const wchar_t* interface_name);

#endif  // ANDROID_USB_API_ADB_HELPER_ROUTINES_H__

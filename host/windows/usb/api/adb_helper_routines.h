/*
 * Copyright (C) 2006 The Android Open Source Project
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

  @param access_type[in] Enumerated access type
  @param sharing_mode[in] Enumerated share mode
  @param desired_access[out] Will receive SDK - complient desired access
         flags. This parameter can be NULL.
  @param desired_sharing[out] Will receive SDK - complient share mode.
         This parameter can be NULL.
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool GetSDKComplientParam(AdbOpenAccessType access_type,
                          AdbOpenSharingMode sharing_mode,
                          ULONG* desired_access,
                          ULONG* desired_sharing);

/** \brief
  Given the hardware device information enumerates interfaces for this device

  @param hardware_dev_info[in] A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param class_id[in] Device class ID how it is specified by our USB driver
  @param exclude_removed[in] If true interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param active_only[in] If 'true' only active interfaces (with flag
         SPINT_ACTIVE set) will be included in the enumeration.
  @param interfaces[out] Upon successfull completion will consist of array of
         all interfaces found for this device (matching all filters).
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool EnumerateDeviceInterfaces(HDEVINFO hardware_dev_info,
                               GUID class_id,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces);

/** \brief Enumerates all interfaces for our device class

  This routine uses SetupDiGetClassDevs to get our device info and calls
  EnumerateDeviceInterfaces to perform the enumeration.
  @param class_id[in] Device class ID how it is specified by our USB driver
  @param flags[in] Flags to pass to SetupDiGetClassDevs to filter devices. See
         SetupDiGetClassDevs() in SDK for more info on these flags.
  @param exclude_removed[in] If true interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param active_only[in] If 'true' only active interfaces (with flag
         SPINT_ACTIVE set) will be included in the enumeration.
  @param interfaces[out] Upon successfull completion will consist of array of
         all interfaces found for this device (matching all filters).
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool EnumerateDeviceInterfaces(GUID class_id,
                               ULONG flags,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces);

/** \brief Given the hardware device information and data gets data details

  Given the hardware_dev_info, representing a handle to the plug and
  play information, and dev_info_data, representing a specific usb device,
  gets detailed data about the device (interface).
  @param hardware_dev_info[in] A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param dev_info_data[in] Device information data obtained via call to
         SetupDiEnumDeviceInterfaces()
  @param dev_info_detail_data[out] Upon successfull completion will consist of
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
  @param hardware_dev_info[in] A handle to hardware device information obtained
         from PnP manager via SetupDiGetClassDevs()
  @param dev_info_data[in] Device information data obtained via call to
         SetupDiEnumDeviceInterfaces()
  @param name[out] Upon successfull completion will have name for the device.
  @return True on success, false on failure, in which case GetLastError()
          provides extended information about the error that occurred.
*/
bool GetUsbDeviceName(HDEVINFO hardware_dev_info,
                      PSP_DEVICE_INTERFACE_DATA dev_info_data,
                      std::wstring* name);

#endif  // ANDROID_USB_API_ADB_HELPER_ROUTINES_H__

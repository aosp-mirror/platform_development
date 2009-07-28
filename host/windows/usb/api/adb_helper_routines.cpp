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

/** \file
  This file consists of implementation of helper routines used
  in the API.
*/

#include "stdafx.h"
#include "adb_api.h"
#include "adb_api_legacy.h"
#include "adb_helper_routines.h"
#include "adb_interface_enum.h"

bool GetSDKComplientParam(AdbOpenAccessType access_type,
                          AdbOpenSharingMode sharing_mode,
                          ULONG* desired_access,
                          ULONG* desired_sharing) {
  if (NULL != desired_access) {
    switch (access_type) {
      case AdbOpenAccessTypeReadWrite:
        *desired_access = GENERIC_READ | GENERIC_WRITE;
        break;

      case AdbOpenAccessTypeRead:
        *desired_access = GENERIC_READ;
        break;

      case AdbOpenAccessTypeWrite:
        *desired_access = GENERIC_WRITE;
        break;

      case AdbOpenAccessTypeQueryInfo:
        *desired_access = FILE_READ_ATTRIBUTES | FILE_READ_EA;
        break;

      default:
        SetLastError(ERROR_INVALID_ACCESS);
        return false;
    }
  }

  if (NULL != desired_sharing) {
    switch (sharing_mode) {
      case AdbOpenSharingModeReadWrite:
        *desired_sharing = FILE_SHARE_READ | FILE_SHARE_WRITE;
        break;

      case AdbOpenSharingModeRead:
        *desired_sharing = FILE_SHARE_READ;
        break;

      case AdbOpenSharingModeWrite:
        *desired_sharing = FILE_SHARE_WRITE;
        break;

      case AdbOpenSharingModeExclusive:
        *desired_sharing = 0;
        break;

      default:
        SetLastError(ERROR_INVALID_PARAMETER);
        return false;
    }
  }

  return true;
}

bool EnumerateDeviceInterfaces(HDEVINFO hardware_dev_info,
                               GUID class_id,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces) {
  AdbEnumInterfaceArray tmp;
  bool ret = false;

  // Enumerate interfaces on this device
  for (ULONG index = 0; ; index++) {
    SP_DEVICE_INTERFACE_DATA interface_data;
    interface_data.cbSize = sizeof(SP_DEVICE_INTERFACE_DATA);

    // SetupDiEnumDeviceInterfaces() returns information about device
    // interfaces exposed by one or more devices defined by our interface
    // class. Each call returns information about one interface. The routine
    // can be called repeatedly to get information about several interfaces
    // exposed by one or more devices.
    if (SetupDiEnumDeviceInterfaces(hardware_dev_info,
                                    0, 
                                    &class_id,
                                    index,
                                    &interface_data)) {
      // Satisfy "exclude removed" and "active only" filters.
      if ((!exclude_removed || (0 == (interface_data.Flags & SPINT_REMOVED))) &&
          (!active_only || (interface_data.Flags & SPINT_ACTIVE))) {
        std::wstring dev_name;

        if (GetUsbDeviceName(hardware_dev_info, &interface_data, &dev_name)) {
          try {
            // Add new entry to the array
            tmp.push_back(AdbInstanceEnumEntry(dev_name.c_str(),
                                               interface_data.InterfaceClassGuid,
                                               interface_data.Flags));
          } catch (... ) {
            SetLastError(ERROR_OUTOFMEMORY);
            break;
          }
        } else {
          // Something went wrong in getting device name
          break;
        }
      }
    } else {
      if (ERROR_NO_MORE_ITEMS == GetLastError()) {
        // There are no more items in the list. Enum is completed.
        ret = true;
        break;
      } else {
        // Something went wrong in SDK enum
        break;
      }
    }
  }

  // On success, swap temp array with the returning one
  if (ret)
    interfaces->swap(tmp);

  return ret;
}

bool EnumerateDeviceInterfaces(GUID class_id,
                               ULONG flags,
                               bool exclude_removed,
                               bool active_only,
                               AdbEnumInterfaceArray* interfaces) {
  // Open a handle to the plug and play dev node.
  // SetupDiGetClassDevs() returns a device information set that
  // contains info on all installed devices of a specified class.
  HDEVINFO hardware_dev_info =
    SetupDiGetClassDevs(&class_id, NULL, NULL, flags);

  bool ret = false;

  if (INVALID_HANDLE_VALUE != hardware_dev_info) {
    // Do the enum
    ret = EnumerateDeviceInterfaces(hardware_dev_info,
                                    class_id,
                                    exclude_removed,
                                    active_only,
                                    interfaces);

    // Preserve last error accross hardware_dev_info destruction
    ULONG error_to_report = ret ? NO_ERROR : GetLastError();

    SetupDiDestroyDeviceInfoList(hardware_dev_info);

    if (NO_ERROR != error_to_report)
      SetLastError(error_to_report);
  }

  return ret;
}

bool GetUsbDeviceDetails(
    HDEVINFO hardware_dev_info,
    PSP_DEVICE_INTERFACE_DATA dev_info_data,
    PSP_DEVICE_INTERFACE_DETAIL_DATA* dev_info_detail_data) {
  ULONG required_len = 0;

  // First query for the structure size. At this point we expect this call
  // to fail with ERROR_INSUFFICIENT_BUFFER error code.
  if (SetupDiGetDeviceInterfaceDetail(hardware_dev_info,
                                      dev_info_data,
                                      NULL,
                                      0,
                                      &required_len,
                                      NULL)) {
    return false;
  }

  if (ERROR_INSUFFICIENT_BUFFER != GetLastError())
    return false;

  // Allocate buffer for the structure
  PSP_DEVICE_INTERFACE_DETAIL_DATA buffer =
    reinterpret_cast<PSP_DEVICE_INTERFACE_DETAIL_DATA>(malloc(required_len));

  if (NULL == buffer) {
    SetLastError(ERROR_OUTOFMEMORY);
    return false;
  }

  buffer->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA);

  // Retrieve the information from Plug and Play.
  if (SetupDiGetDeviceInterfaceDetail(hardware_dev_info,
                                      dev_info_data,
                                      buffer,
                                      required_len,
                                      &required_len,
                                      NULL)) {
    *dev_info_detail_data = buffer;
    return true;
  } else {
    // Free the buffer if this call failed
    free(buffer);

    return false;
  }
}

bool GetUsbDeviceName(HDEVINFO hardware_dev_info,
                      PSP_DEVICE_INTERFACE_DATA dev_info_data,
                      std::wstring* name) {
  PSP_DEVICE_INTERFACE_DETAIL_DATA func_class_dev_data = NULL;
  if (!GetUsbDeviceDetails(hardware_dev_info,
                           dev_info_data,
                           &func_class_dev_data)) {
    return false;
  }

  try {
    *name = func_class_dev_data->DevicePath;
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
  }

  free(func_class_dev_data);

  return !name->empty();
}

bool IsLegacyInterface(const wchar_t* interface_name) {
  // Open USB device for this intefface
  HANDLE usb_device_handle = CreateFile(interface_name,
                                        GENERIC_READ | GENERIC_WRITE,
                                        FILE_SHARE_READ | FILE_SHARE_WRITE,
                                        NULL,
                                        OPEN_EXISTING,
                                        0,
                                        NULL);
  if (INVALID_HANDLE_VALUE == usb_device_handle)
    return NULL;

  // Try to issue ADB_IOCTL_GET_USB_DEVICE_DESCRIPTOR IOCTL that is supported
  // by the legacy driver, but is not implemented in the WinUsb driver.
  DWORD ret_bytes = 0;
  USB_DEVICE_DESCRIPTOR descriptor;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_USB_DEVICE_DESCRIPTOR,
                             NULL, 0,
                             &descriptor,
                             sizeof(descriptor),
                             &ret_bytes,
                             NULL);
  ::CloseHandle(usb_device_handle);

  // If IOCTL succeeded we've got legacy driver underneath.
  return ret ? true : false;
}

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

/** \file
  This file consists of implementation of class AdbInterfaceObject that
  encapsulates an interface on our USB device.
*/

#include "stdafx.h"
#include "adb_interface.h"
#include "adb_endpoint_object.h"

AdbInterfaceObject::AdbInterfaceObject(const wchar_t* interf_name)
    : AdbObjectHandle(AdbObjectTypeInterface),
      interface_name_(interf_name) {
  ATLASSERT(NULL != interf_name);
}

AdbInterfaceObject::~AdbInterfaceObject() {
}

ADBAPIHANDLE AdbInterfaceObject::CreateHandle() {
  // Open USB device for this intefface
  HANDLE usb_device_handle = CreateFile(interface_name().c_str(),
                                        GENERIC_READ | GENERIC_WRITE,
                                        FILE_SHARE_READ | FILE_SHARE_WRITE,
                                        NULL,
                                        OPEN_EXISTING,
                                        0,
                                        NULL);
  if (INVALID_HANDLE_VALUE == usb_device_handle)
    return NULL;

  // Now, we ensured that our usb device / interface is up and running.
  // Lets collect device, interface and pipe information
  bool ok = true;
  if (!CacheUsbDeviceDescriptor(usb_device_handle) ||
      !CacheUsbConfigurationDescriptor(usb_device_handle) ||
      !CacheUsbInterfaceDescriptor(usb_device_handle)) {
    ok = false;
  }

  // Preserve error accross handle close
  ULONG error = ok ? NO_ERROR : GetLastError();

  ::CloseHandle(usb_device_handle);

  if (NO_ERROR != error)
    SetLastError(error);

  if (!ok)
    return false;

  return AdbObjectHandle::CreateHandle();
}

bool AdbInterfaceObject::GetInterfaceName(void* buffer,
                                          unsigned long* buffer_char_size,
                                          bool ansi) {
  // Lets see if buffer is big enough
  ULONG name_len = static_cast<ULONG>(interface_name_.length() + 1);
  if ((NULL == buffer) || (*buffer_char_size < name_len)) {
    *buffer_char_size = name_len;
    SetLastError(ERROR_INSUFFICIENT_BUFFER);
    return false;
  }

  if (!ansi) {
    // If user asked for wide char name just return it
    wcscpy(reinterpret_cast<wchar_t*>(buffer), interface_name().c_str());
    return true;
  }

  // We need to convert name from wide char to ansi string
  int res = WideCharToMultiByte(CP_ACP,
                                0,
                                interface_name().c_str(),
                                static_cast<int>(name_len),
                                reinterpret_cast<PSTR>(buffer),
                                static_cast<int>(*buffer_char_size),
                                NULL,
                                NULL);
  return (res != 0);
}

bool AdbInterfaceObject::GetSerialNumber(void* buffer,
                                         unsigned long* buffer_char_size,
                                         bool ansi) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  // Open USB device for this intefface
  HANDLE usb_device_handle = CreateFile(interface_name().c_str(),
                                        GENERIC_READ,
                                        FILE_SHARE_READ | FILE_SHARE_WRITE,
                                        NULL,
                                        OPEN_EXISTING,
                                        0,
                                        NULL);
  if (INVALID_HANDLE_VALUE == usb_device_handle)
    return NULL;

  WCHAR serial_number[512];

  // Send IOCTL
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_SERIAL_NUMBER,
                             NULL, 0,
                             serial_number, sizeof(serial_number),
                             &ret_bytes,
                             NULL);

  // Preserve error accross CloseHandle
  ULONG error = ret ? NO_ERROR : GetLastError();

  ::CloseHandle(usb_device_handle);

  if (NO_ERROR != error) {
    SetLastError(error);
    return false;
  }

  unsigned long str_len =
    static_cast<unsigned long>(wcslen(serial_number) + 1);

  if ((NULL == buffer) || (*buffer_char_size < str_len)) {
    *buffer_char_size = str_len;
    SetLastError(ERROR_INSUFFICIENT_BUFFER);
    return false;
  }

  if (!ansi) {
    // If user asked for wide char name just return it
    wcscpy(reinterpret_cast<wchar_t*>(buffer), serial_number);
    return true;
  }

  // We need to convert name from wide char to ansi string
  int res = WideCharToMultiByte(CP_ACP,
                                0,
                                serial_number,
                                static_cast<int>(str_len),
                                reinterpret_cast<PSTR>(buffer),
                                static_cast<int>(*buffer_char_size),
                                NULL,
                                NULL);
  return (res != 0);
}

bool AdbInterfaceObject::GetUsbDeviceDescriptor(USB_DEVICE_DESCRIPTOR* desc) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  CopyMemory(desc, usb_device_descriptor(), sizeof(USB_DEVICE_DESCRIPTOR));

  return true;
}

bool AdbInterfaceObject::GetUsbConfigurationDescriptor(
    USB_CONFIGURATION_DESCRIPTOR* desc) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  CopyMemory(desc, usb_config_descriptor(),
             sizeof(USB_CONFIGURATION_DESCRIPTOR));

  return true;
}

bool AdbInterfaceObject::GetUsbInterfaceDescriptor(
    USB_INTERFACE_DESCRIPTOR* desc) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  CopyMemory(desc, usb_interface_descriptor(), sizeof(USB_INTERFACE_DESCRIPTOR));

  return true;
}

bool AdbInterfaceObject::GetEndpointInformation(UCHAR endpoint_index,
                                                AdbEndpointInformation* info) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  // Open USB device for this intefface
  HANDLE usb_device_handle = CreateFile(interface_name().c_str(),
                                        GENERIC_READ,
                                        FILE_SHARE_READ | FILE_SHARE_WRITE,
                                        NULL,
                                        OPEN_EXISTING,
                                        0,
                                        NULL);
  if (INVALID_HANDLE_VALUE == usb_device_handle)
    return NULL;

  // Init ICTL param
  AdbQueryEndpointInformation param;
  param.endpoint_index = endpoint_index;

  // Send IOCTL
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_ENDPOINT_INFORMATION,
                             &param, sizeof(param),
                             info, sizeof(AdbEndpointInformation),
                             &ret_bytes,
                             NULL);
  ATLASSERT(!ret || (sizeof(AdbEndpointInformation) == ret_bytes));

  // Preserve error accross CloseHandle
  ULONG error = ret ? NO_ERROR : GetLastError();

  ::CloseHandle(usb_device_handle);

  if (NO_ERROR != error)
    SetLastError(error);

  return ret ? true : false;
}

ADBAPIHANDLE AdbInterfaceObject::OpenEndpoint(
    UCHAR endpoint_index,
    AdbOpenAccessType access_type,
    AdbOpenSharingMode sharing_mode) {
  // Convert index into name
  std::wstring endpoint_name;

  try {
    if (ADB_QUERY_BULK_READ_ENDPOINT_INDEX == endpoint_index) {
      endpoint_name = DEVICE_BULK_READ_PIPE_NAME;
    } else if (ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == endpoint_index) {
      endpoint_name = DEVICE_BULK_WRITE_PIPE_NAME;
    } else {
      wchar_t fmt[265];
      swprintf(fmt, L"%ws%u", DEVICE_PIPE_NAME_PREFIX, endpoint_index);
      endpoint_name = fmt;
    }
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
    return NULL;
  }

  return OpenEndpoint(endpoint_name.c_str(), access_type, sharing_mode);
}

ADBAPIHANDLE AdbInterfaceObject::OpenEndpoint(
    const wchar_t* endpoint_name,
    AdbOpenAccessType access_type,
    AdbOpenSharingMode sharing_mode) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  AdbEndpointObject* adb_endpoint = NULL;
  
  try {
    adb_endpoint = new AdbEndpointObject(this);
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
    return NULL;
  }

  // Build full path to the object
  std::wstring endpoint_path = interface_name();
  endpoint_path += L"\\";
  endpoint_path += endpoint_name;

  ADBAPIHANDLE ret = adb_endpoint->CreateHandle(endpoint_path.c_str(),
                                                access_type,
                                                sharing_mode);

  adb_endpoint->Release();

  return ret;
}

bool AdbInterfaceObject::CacheUsbDeviceDescriptor(HANDLE usb_device_handle) {
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_USB_DEVICE_DESCRIPTOR,
                             NULL, 0,
                             &usb_device_descriptor_,
                             sizeof(usb_device_descriptor_),
                             &ret_bytes,
                             NULL);
  ATLASSERT(!ret || (sizeof(USB_DEVICE_DESCRIPTOR) == ret_bytes));

  return ret ? true : false;
}

bool AdbInterfaceObject::CacheUsbConfigurationDescriptor(
    HANDLE usb_device_handle) {
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_USB_CONFIGURATION_DESCRIPTOR,
                             NULL, 0,
                             &usb_config_descriptor_,
                             sizeof(usb_config_descriptor_),
                             &ret_bytes,
                             NULL);
  ATLASSERT(!ret || (sizeof(USB_CONFIGURATION_DESCRIPTOR) == ret_bytes));

  return ret ? true : false;
}

bool AdbInterfaceObject::CacheUsbInterfaceDescriptor(
    HANDLE usb_device_handle) {
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_device_handle,
                             ADB_IOCTL_GET_USB_INTERFACE_DESCRIPTOR,
                             NULL, 0,
                             &usb_interface_descriptor_,
                             sizeof(usb_interface_descriptor_),
                             &ret_bytes,
                             NULL);
  ATLASSERT(!ret || (sizeof(USB_INTERFACE_DESCRIPTOR) == ret_bytes));

  return ret ? true : false;
}

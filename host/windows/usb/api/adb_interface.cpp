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
  encapsulates a generic interface on our USB device.
*/

#include "stdafx.h"
#include "adb_interface.h"

AdbInterfaceObject::AdbInterfaceObject(const wchar_t* interf_name)
    : AdbObjectHandle(AdbObjectTypeInterface),
      interface_name_(interf_name) {
  ATLASSERT(NULL != interf_name);
}

AdbInterfaceObject::~AdbInterfaceObject() {
}

bool AdbInterfaceObject::GetInterfaceName(void* buffer,
                                          unsigned long* buffer_char_size,
                                          bool ansi) {
  if (NULL == buffer_char_size) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

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

bool AdbInterfaceObject::GetUsbDeviceDescriptor(USB_DEVICE_DESCRIPTOR* desc) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  if (NULL == desc) {
    SetLastError(ERROR_INVALID_PARAMETER);
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

  if (NULL == desc) {
    SetLastError(ERROR_INVALID_PARAMETER);
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

  if (NULL == desc) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  CopyMemory(desc, usb_interface_descriptor(), sizeof(USB_INTERFACE_DESCRIPTOR));

  return true;
}

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
  This file consists of implementation of class AdbWinUsbInterfaceObject
  that encapsulates an interface on our USB device that is accessible
  via WinUsb API.
*/

#include "stdafx.h"
#include "adb_winusb_interface.h"
#include "adb_winusb_endpoint_object.h"

AdbWinUsbInterfaceObject::AdbWinUsbInterfaceObject(const wchar_t* interf_name)
    : AdbInterfaceObject(interf_name),
      usb_device_handle_(INVALID_HANDLE_VALUE),
      winusb_handle_(NULL),
      interface_number_(0xFF),
      def_read_endpoint_(0xFF),
      read_endpoint_id_(0xFF),
      def_write_endpoint_(0xFF),
      write_endpoint_id_(0xFF) {
}

AdbWinUsbInterfaceObject::~AdbWinUsbInterfaceObject() {
  ATLASSERT(NULL == winusb_handle_);
  ATLASSERT(INVALID_HANDLE_VALUE == usb_device_handle_);
}

LONG AdbWinUsbInterfaceObject::Release() {
  ATLASSERT(ref_count_ > 0);
  LONG ret = InterlockedDecrement(&ref_count_);
  ATLASSERT(ret >= 0);
  if (0 == ret) {
    LastReferenceReleased();
    delete this;
  }
  return ret;
}

ADBAPIHANDLE AdbWinUsbInterfaceObject::CreateHandle() {
  // Open USB device for this inteface Note that WinUsb API
  // requires the handle to be opened for overlapped I/O.
  usb_device_handle_ = CreateFile(interface_name().c_str(),
                                  GENERIC_READ | GENERIC_WRITE,
                                  FILE_SHARE_READ | FILE_SHARE_WRITE,
                                  NULL, OPEN_EXISTING,
                                  FILE_FLAG_OVERLAPPED, NULL);
  if (INVALID_HANDLE_VALUE == usb_device_handle_)
    return NULL;

  // Initialize WinUSB API for this interface
  if (!WinUsb_Initialize(usb_device_handle_, &winusb_handle_))
    return NULL;

  // Cache current interface number that will be used in
  // WinUsb_Xxx calls performed on this interface.
  if (!WinUsb_GetCurrentAlternateSetting(winusb_handle(), &interface_number_))
    return false;

  // Cache interface properties
  unsigned long bytes_written;

  // Cache USB device descriptor
  if (!WinUsb_GetDescriptor(winusb_handle(), USB_DEVICE_DESCRIPTOR_TYPE, 0, 0,
                            reinterpret_cast<PUCHAR>(&usb_device_descriptor_),
                            sizeof(usb_device_descriptor_), &bytes_written)) {
    return false;
  }

  // Cache USB configuration descriptor
  if (!WinUsb_GetDescriptor(winusb_handle(), USB_CONFIGURATION_DESCRIPTOR_TYPE,
                            0, 0,
                            reinterpret_cast<PUCHAR>(&usb_config_descriptor_),
                            sizeof(usb_config_descriptor_), &bytes_written)) {
    return false;
  }

  // Cache USB interface descriptor
  if (!WinUsb_QueryInterfaceSettings(winusb_handle(), interface_number(),
                                     &usb_interface_descriptor_)) {
    return false;
  }

  // Save indexes and IDs for bulk read / write endpoints. We will use them to
  // convert ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX and
  // ADB_QUERY_BULK_READ_ENDPOINT_INDEX into actual endpoint indexes and IDs.
  for (UCHAR endpoint = 0; endpoint < usb_interface_descriptor_.bNumEndpoints;
       endpoint++) {
    // Get endpoint information
    WINUSB_PIPE_INFORMATION pipe_info;
    if (!WinUsb_QueryPipe(winusb_handle(), interface_number(), endpoint,
                          &pipe_info)) {
      return false;
    }

    if (UsbdPipeTypeBulk == pipe_info.PipeType) {
      // This is a bulk endpoint. Cache its index and ID.
      if (0 != (pipe_info.PipeId & USB_ENDPOINT_DIRECTION_MASK)) {
        // Use this endpoint as default bulk read endpoint
        ATLASSERT(0xFF == def_read_endpoint_);
        def_read_endpoint_ = endpoint;
        read_endpoint_id_ = pipe_info.PipeId;
      } else {
        // Use this endpoint as default bulk write endpoint
        ATLASSERT(0xFF == def_write_endpoint_);
        def_write_endpoint_ = endpoint;
        write_endpoint_id_ = pipe_info.PipeId;
      }
    }
  }

  return AdbInterfaceObject::CreateHandle();
}

bool AdbWinUsbInterfaceObject::CloseHandle() {
  if (NULL != winusb_handle_) {
    WinUsb_Free(winusb_handle_);
    winusb_handle_ = NULL;
  }
  if (INVALID_HANDLE_VALUE != usb_device_handle_) {
    ::CloseHandle(usb_device_handle_);
    usb_device_handle_ = INVALID_HANDLE_VALUE;
  }

  return AdbInterfaceObject::CloseHandle();
}

bool AdbWinUsbInterfaceObject::GetSerialNumber(void* buffer,
                                               unsigned long* buffer_char_size,
                                               bool ansi) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  if (NULL == buffer_char_size) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  // Calculate serial number string size. Note that WinUsb_GetDescriptor
  // API will not return number of bytes needed to store serial number
  // string. So we will have to start with a reasonably large preallocated
  // buffer and then loop through WinUsb_GetDescriptor calls, doubling up
  // string buffer size every time ERROR_INSUFFICIENT_BUFFER is returned.
  union {
    // Preallocate reasonably sized buffer on the stack.
    char small_buffer[64];
    USB_STRING_DESCRIPTOR initial_ser_num;
  };
  USB_STRING_DESCRIPTOR* ser_num = &initial_ser_num;
  // Buffer byte size
  unsigned long ser_num_size = sizeof(small_buffer);
  // After successful call to WinUsb_GetDescriptor will contain serial
  // number descriptor size.
  unsigned long bytes_written;
  while (!WinUsb_GetDescriptor(winusb_handle(), USB_STRING_DESCRIPTOR_TYPE,
                               usb_device_descriptor_.iSerialNumber,
                               0x0409, // English (US)
                               reinterpret_cast<PUCHAR>(ser_num),
                               ser_num_size, &bytes_written)) {
    // Any error other than ERROR_INSUFFICIENT_BUFFER is terminal here.
    if (ERROR_INSUFFICIENT_BUFFER != GetLastError()) {
      if (ser_num != &initial_ser_num)
        delete[] reinterpret_cast<char*>(ser_num);
      return false;
    }

    // Double up buffer size and reallocate string buffer
    ser_num_size *= 2;
    if (ser_num != &initial_ser_num)
      delete[] reinterpret_cast<char*>(ser_num);
    try {
      ser_num =
          reinterpret_cast<USB_STRING_DESCRIPTOR*>(new char[ser_num_size]);
    } catch (...) {
      SetLastError(ERROR_OUTOFMEMORY);
      return false;
    }
  }

  // Serial number string length
  unsigned long str_len = (ser_num->bLength -
                           FIELD_OFFSET(USB_STRING_DESCRIPTOR, bString)) /
                          sizeof(wchar_t);

  // Lets see if requested buffer is big enough to fit the string
  if ((NULL == buffer) || (*buffer_char_size < (str_len + 1))) {
    // Requested buffer is too small.
    if (ser_num != &initial_ser_num)
      delete[] reinterpret_cast<char*>(ser_num);
    *buffer_char_size = str_len + 1;
    SetLastError(ERROR_INSUFFICIENT_BUFFER);
    return false;
  }

  bool ret = true;
  if (ansi) {
    // We need to convert name from wide char to ansi string
    if (0 != WideCharToMultiByte(CP_ACP, 0, ser_num->bString,
                                 static_cast<int>(str_len),
                                 reinterpret_cast<PSTR>(buffer),
                                 static_cast<int>(*buffer_char_size),
                                 NULL, NULL)) {
      // Zero-terminate output string.
      reinterpret_cast<char*>(buffer)[str_len] = '\0';
    } else {
      ret = false;
    }
  } else {
    // For wide char output just copy string buffer,
    // and zero-terminate output string.
    CopyMemory(buffer, ser_num->bString, bytes_written);
    reinterpret_cast<wchar_t*>(buffer)[str_len] = L'\0';
  }

  if (ser_num != &initial_ser_num)
    delete[] reinterpret_cast<char*>(ser_num);

  return ret;
}

bool AdbWinUsbInterfaceObject::GetEndpointInformation(
    UCHAR endpoint_index,
    AdbEndpointInformation* info) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  if (NULL == info) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  // Get actual endpoint index for predefined read / write endpoints.
  if (ADB_QUERY_BULK_READ_ENDPOINT_INDEX == endpoint_index) {
    endpoint_index = def_read_endpoint_;
  } else if (ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == endpoint_index) {
    endpoint_index = def_write_endpoint_;
  }

  // Query endpoint information
  WINUSB_PIPE_INFORMATION pipe_info;
  if (!WinUsb_QueryPipe(winusb_handle(), interface_number(), endpoint_index,
                        &pipe_info)) {
    return false;
  }

  // Save endpoint information into output.
  info->max_packet_size = pipe_info.MaximumPacketSize;
  info->max_transfer_size = 0xFFFFFFFF;
  info->endpoint_address = pipe_info.PipeId;
  info->polling_interval = pipe_info.Interval;
  info->setting_index = interface_number();
  switch (pipe_info.PipeType) {
    case UsbdPipeTypeControl:
      info->endpoint_type = AdbEndpointTypeControl;
      break;

    case UsbdPipeTypeIsochronous:
      info->endpoint_type = AdbEndpointTypeIsochronous;
      break;

    case UsbdPipeTypeBulk:
      info->endpoint_type = AdbEndpointTypeBulk;
      break;

    case UsbdPipeTypeInterrupt:
      info->endpoint_type = AdbEndpointTypeInterrupt;
      break;

    default:
      info->endpoint_type = AdbEndpointTypeInvalid;
      break;
  }

  return true;
}

ADBAPIHANDLE AdbWinUsbInterfaceObject::OpenEndpoint(
    UCHAR endpoint_index,
    AdbOpenAccessType access_type,
    AdbOpenSharingMode sharing_mode) {
  // Convert index into id
  UCHAR endpoint_id;

  if ((ADB_QUERY_BULK_READ_ENDPOINT_INDEX == endpoint_index) ||
      (def_read_endpoint_ == endpoint_index)) {
    endpoint_id = read_endpoint_id_;
    endpoint_index = def_read_endpoint_;
  } else if ((ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == endpoint_index) ||
             (def_write_endpoint_ == endpoint_index)) {
    endpoint_id = write_endpoint_id_;
    endpoint_index = def_write_endpoint_;
  } else {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  return OpenEndpoint(endpoint_id, endpoint_index);
}

ADBAPIHANDLE AdbWinUsbInterfaceObject::OpenEndpoint(UCHAR endpoint_id,
                                                    UCHAR endpoint_index) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  AdbEndpointObject* adb_endpoint = NULL;
  
  try {
    adb_endpoint =
        new AdbWinUsbEndpointObject(this, endpoint_id, endpoint_index);
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
    return NULL;
  }

  ADBAPIHANDLE ret = adb_endpoint->CreateHandle();

  adb_endpoint->Release();

  return ret;
}

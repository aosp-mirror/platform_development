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

#ifndef ANDROID_USB_API_ADB_API_LEGACY_H_
#define ANDROID_USB_API_ADB_API_LEGACY_H_
/** \file
  This file consists of declarations of constants and structures required
  for supporting communications of this API with a legacy (custom) USB
  driver.
*/

// Enables compillation for "straight" C
#ifdef __cplusplus
  #define EXTERN_C    extern "C"
#else
  #define EXTERN_C    extern
  typedef int bool;
  #define true  1
  #define false 0
#endif

/// Name for the default bulk read pipe
#define DEVICE_BULK_READ_PIPE_NAME  L"BulkRead"

/// Name for the default bulk write pipe
#define DEVICE_BULK_WRITE_PIPE_NAME L"BulkWrite"

/// Prefix for an index-based pipe name
#define DEVICE_PIPE_NAME_PREFIX     L"PIPE_"

/** \name IOCTL codes for the driver
*/
///@{

/// Control code for IOCTL that gets USB_DEVICE_DESCRIPTOR
#define ADB_CTL_GET_USB_DEVICE_DESCRIPTOR         10

/// Control code for IOCTL that gets USB_CONFIGURATION_DESCRIPTOR
#define ADB_CTL_GET_USB_CONFIGURATION_DESCRIPTOR  11

/// Control code for IOCTL that gets USB_INTERFACE_DESCRIPTOR
#define ADB_CTL_GET_USB_INTERFACE_DESCRIPTOR      12

/// Control code for IOCTL that gets endpoint information
#define ADB_CTL_GET_ENDPOINT_INFORMATION          13

/// Control code for bulk read IOCTL
#define ADB_CTL_BULK_READ                         14

/// Control code for bulk write IOCTL
#define ADB_CTL_BULK_WRITE                        15

/// Control code for IOCTL that gets device serial number
#define ADB_CTL_GET_SERIAL_NUMBER                 16

/// IOCTL that gets USB_DEVICE_DESCRIPTOR
#define ADB_IOCTL_GET_USB_DEVICE_DESCRIPTOR \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_GET_USB_DEVICE_DESCRIPTOR, \
                       METHOD_BUFFERED, \
                       FILE_READ_ACCESS)

/// IOCTL that gets USB_CONFIGURATION_DESCRIPTOR
#define ADB_IOCTL_GET_USB_CONFIGURATION_DESCRIPTOR \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_GET_USB_CONFIGURATION_DESCRIPTOR, \
                       METHOD_BUFFERED, \
                       FILE_READ_ACCESS)

/// IOCTL that gets USB_INTERFACE_DESCRIPTOR
#define ADB_IOCTL_GET_USB_INTERFACE_DESCRIPTOR \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_GET_USB_INTERFACE_DESCRIPTOR, \
                       METHOD_BUFFERED, \
                       FILE_READ_ACCESS)

/// IOCTL that gets endpoint information
#define ADB_IOCTL_GET_ENDPOINT_INFORMATION \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_GET_ENDPOINT_INFORMATION, \
                       METHOD_BUFFERED, \
                       FILE_READ_ACCESS)

/// Bulk read IOCTL
#define ADB_IOCTL_BULK_READ \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_BULK_READ, \
                       METHOD_OUT_DIRECT, \
                       FILE_READ_ACCESS)

// For bulk write IOCTL we send request data in the form of AdbBulkTransfer
// structure and output buffer is just ULONG that receives number of bytes
// actually written. Since both of these are tiny we can use buffered I/O
// for this IOCTL.
/// Bulk write IOCTL
#define ADB_IOCTL_BULK_WRITE \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_BULK_WRITE, \
                       METHOD_BUFFERED, \
                       FILE_WRITE_ACCESS)

/// IOCTL that gets device serial number
#define ADB_IOCTL_GET_SERIAL_NUMBER \
              CTL_CODE(FILE_DEVICE_UNKNOWN, \
                       ADB_CTL_GET_SERIAL_NUMBER, \
                       METHOD_BUFFERED, \
                       FILE_READ_ACCESS)

///@}

/** Structure AdbQueryEndpointInformation formats input for
  ADB_IOCTL_GET_ENDPOINT_INFORMATION IOCTL request
*/
struct AdbQueryEndpointInformation {
  /// Zero-based endpoint index for which information is queried.
  /// See ADB_QUERY_BULK_xxx_ENDPOINT_INDEX for shortcuts.
  UCHAR endpoint_index;
};

/** Structure AdbBulkTransfer formats parameters for ADB_CTL_BULK_READ and
  ADB_CTL_BULK_WRITE IOCTL requests.
*/
struct AdbBulkTransfer {
  /// Time in milliseconds to complete this request
  ULONG time_out;

  /// Size of the data to transfer. This parameter is used only for
  /// ADB_CTL_BULK_WRITE request. For ADB_CTL_BULK_READ requests transfer
  /// size is defined by the output buffer size.
  ULONG transfer_size;

  /// Initializes statically allocated structure
  __forceinline AdbBulkTransfer() {
    time_out = 0;
    transfer_size = 0;
    for_x64 = 0;
  }

  /// Provides access to protected write_buffer field
  void* GetWriteBuffer() {
    return write_buffer;
  }

  /// Provides access to protected write_buffer field
  const void* GetWriteBuffer() const {
    return write_buffer;
  }

  /// Sets write_buffer field.
  void SetWriteBuffer(void* buffer) {
    // For 32-bit we must zero out high 32 bit of the address, so 64-bit
    // driver will see valid address when accessing 64-bit write_buffer.
    for_x64 = 0;
    write_buffer = buffer;
  }

protected:
  /// Pointer to the actual buffer for ADB_CTL_BULK_WRITE request. This field
  /// is not used in ADB_CTL_BULK_READ request. Note that in order to support
  /// compatibility between 32-bit and 64-bit versions of both, driver and
  /// application we must sizeof this field to the max pointer sizeof (which
  /// is 64 bit in our case). The idea is that if IOCTL was issued by a 64-bit
  /// process to a 64-bit driver, write_buffer will be valid 64-bit pointer to
  /// the write buffer. Same is true for 32-bit app talking to 32-bit driver.
  /// If, however, a 32-bit app is talking to 64-bit driver, then write_buffer
  /// initialized by 32-bit app will contain 32-bit address, which will be
  /// correctly picked up ("extended") by 64-bit driver. Since when setting
  /// this field by a 32-bit app requires some extra work (see SetWriteBuffer)
  /// we hide this field, making it accessible only throug the accessor
  /// methods (Get/SetWriteBuffer).
  union {
    void* write_buffer;
    __int64 for_x64;
  };
};

#endif  // ANDROID_USB_API_ADB_API_LEGACY_H_

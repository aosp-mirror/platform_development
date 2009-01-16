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

#ifndef ANDROID_USB_API_ADB_API_EXTRA_H__
#define ANDROID_USB_API_ADB_API_EXTRA_H__
/** \file
  This file consists of public API declarations that are also used by the
  driver and as such cannot be declared in adb_api.h
*/

/** AdbEndpointType enumerates endpoint types. It enum is taken from
  WDF_USB_PIPE_TYPE enum found in WDK.
*/
typedef enum _AdbEndpointType {
    AdbEndpointTypeInvalid = 0,
    AdbEndpointTypeControl,
    AdbEndpointTypeIsochronous,
    AdbEndpointTypeBulk,
    AdbEndpointTypeInterrupt,
} AdbEndpointType;

/** Structure AdbEndpointInformation describes an endpoint. It is
  based on WDF_USB_PIPE_INFORMATION structure found in WDK.
*/
typedef struct _AdbEndpointInformation {
  /// Maximum packet size this endpoint is capable of
  unsigned long max_packet_size;

  // Maximum size of one transfer which should be sent to the host controller
  unsigned long max_transfer_size;

  // The type of the endpoint
  AdbEndpointType endpoint_type;

  /// Raw endpoint address of the device as described by its descriptor
  unsigned char endpoint_address;

  /// Polling interval
  unsigned char polling_interval;

  /// Which alternate setting this structure is relevant for
  unsigned char setting_index;
} AdbEndpointInformation;

/// Shortcut to default write bulk endpoint in zero-based endpoint index API
#define ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX  0xFC

/// Shortcut to default read bulk endpoint in zero-based endpoint index API
#define ADB_QUERY_BULK_READ_ENDPOINT_INDEX  0xFE

// {F72FE0D4-CBCB-407d-8814-9ED673D0DD6B}
/// Our USB class id that driver uses to register our device
#define ANDROID_USB_CLASS_ID \
{0xf72fe0d4, 0xcbcb, 0x407d, {0x88, 0x14, 0x9e, 0xd6, 0x73, 0xd0, 0xdd, 0x6b}};

/// Defines vendor ID for the device
#define DEVICE_VENDOR_ID            0x0BB4

/// Defines product ID for the device with single interface.
#define DEVICE_SINGLE_PRODUCT_ID    0x0C01

/// Defines product ID for the composite device.
#define DEVICE_COMPOSITE_PRODUCT_ID 0x0C02

/// Defines interface ID for the device.
#define DEVICE_INTERFACE_ID         0x01

/// Defines vendor ID for the device
#define DEVICE_EMULATOR_VENDOR_ID   0x18D1

/// Defines product ID for a SoftUSB device simulator that is used to test
/// the driver in isolation from hardware.
#define DEVICE_EMULATOR_PROD_ID     0xDDDD

#endif  // ANDROID_USB_API_ADB_API_EXTRA_H__

/*
 * Copyright (C) 2008 The Android Open Source Project
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

// android_usb_test.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <stdio.h>
#include <conio.h>

#define MAX_PAYLOAD 4096

#define A_SYNC 0x434e5953
#define A_CNXN 0x4e584e43
#define A_OPEN 0x4e45504f
#define A_OKAY 0x59414b4f
#define A_CLSE 0x45534c43
#define A_WRTE 0x45545257

#define A_VERSION 0x01000000

struct message {
    unsigned int command;       /* command identifier constant      */
    unsigned int arg0;          /* first argument                   */
    unsigned int arg1;          /* second argument                  */
    unsigned int data_length;   /* length of payload (0 is allowed) */
    unsigned int data_crc32;    /* crc32 of data payload            */
    unsigned int magic;         /* command ^ 0xffffffff             */
};

USB_DEVICE_DESCRIPTOR test_dev_desc = {
  sizeof(USB_DEVICE_DESCRIPTOR),
  1,      // bDescriptorType
  0x200,  // bcdUSB
  0xFF,   // bDeviceClass
  0xFF,   // bDeviceSubClass
  0xFF,   // bDeviceProtocol
  64,     // bMaxPacketSize
  0x18D1, // idVendor
  0xDDDD, // idProduct
  0x100,  // bcdDevice
  1,      // iManufacturer
  2,      // iProduct
  3,      // iSerialNumber
  1       // bNumConfigurations
};

USB_CONFIGURATION_DESCRIPTOR test_config_desc = {
  9,      // bLength
  2,      // bDescriptorType
  32,     // wTotalLength
  1,      // bNumInterfaces
  1,      // bConfigurationValue
  4,      // iConfiguration
  64,     // bmAttributes
  50      // MaxPower
};

USB_INTERFACE_DESCRIPTOR test_interface_desc = {
  9,      // bLength
  4,      // bDescriptorType
  0,      // bInterfaceNumber
  0,      // bAlternateSetting
  2,      // bNumEndpoints
  0xFF,   // bInterfaceClass
  0xFF,   // bInterfaceSubClass
  0xFF,   // bInterfaceProtocol
  5       // iInterface
};

AdbEndpointInformation test_pipe_00 = {
  1024,       // MaximumPacketSize
  0xFFFFFFFF, // MaximumTransferSize
  static_cast<AdbEndpointType>(3),          // PipeType
  0x81,       // EndpointAddress
  0,          // Interval
  0           // SettingIndex
};

AdbEndpointInformation test_pipe_01 = {
  1024,       // MaximumPacketSize
  0xFFFFFFFF, // MaximumTransferSize
  static_cast<AdbEndpointType>(3),          // PipeType
  0x02,       // EndpointAddress
  0,          // Interval
  0           // SettingIndex
};

AdbEndpointInformation* test_read_pipe = &test_pipe_00;
AdbEndpointInformation* test_write_pipe = &test_pipe_01;

const UCHAR test_read_pipe_index = 0;
const UCHAR test_write_pipe_index = 1;

bool device_active = false;

GUID adb_class_id = ANDROID_USB_CLASS_ID;
const wchar_t test_interface_name[] = L"\\\\?\\usb#vid_18d1&pid_dddd#123456789abcdef#{F72FE0D4-CBCB-407d-8814-9ED673D0DD6B}";
bool RunInterfaceEnumTest();
bool RunInterfaceEnumTest(bool exclude_not_present,
                          bool exclude_removed,
                          bool active_only);
bool RunInterfaceCreateTest();
bool RunEndpointInfoTest();
bool RunEndpointInfoTest(ADBAPIHANDLE adb_interface, UCHAR index);
bool RunEndpointOpenTest();
bool RunEndpointOpenTest(ADBAPIHANDLE adb_interface, UCHAR index);
bool RunEndpointIoTest(ULONG time_out_base);
bool RunTimeoutsTest();
bool RunGeneralTests();
bool DeviceHandShake();
bool CheckEndpointInfo(UCHAR index, AdbEndpointInformation* info);

int _tmain(int argc, _TCHAR* argv[]) {
  argc = argc;
  argv = argv;

  // General startup tests.
  if (!RunGeneralTests())
    return 1;

	return 0;
}

bool RunGeneralTests() {
  // Test interface enum
  if (!RunInterfaceEnumTest())
    return false;

  // Test interface create
  if (!RunInterfaceCreateTest())
    return false;

  // Test endpoint information
  if (!RunEndpointInfoTest())
    return false;

  // Test endpoint open
  if (!RunEndpointOpenTest())
    return false;

  // Test timeout i/o
  if (!RunTimeoutsTest())
    return false;

  // Test read / write (no timeouts)
  if (!RunEndpointIoTest(0))
    return false;

  // Test read / write (OK timeouts)
  if (!RunEndpointIoTest(10))
    return false;

  return true;
/*
  if (!DeviceHandShake())
    return false;

  return true;
*/
}

bool DeviceHandShake() {
  printf("\n\n===== Running DeviceHandShake... ");

  // Get interface
  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_COMPOSITE_PRODUCT_ID,
                                                  DEVICE_INTERFACE_ID);
  if (NULL == adb_interface) {
    adb_interface = AdbCreateInterface(adb_class_id,
                                       DEVICE_VENDOR_ID,
                                       DEVICE_SINGLE_PRODUCT_ID,
                                       0xFF);
  }

  if (NULL == adb_interface) {
    printf("\n      AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  char interf_name[1024];
  unsigned long name_size = sizeof(interf_name);

  if (!AdbGetInterfaceName(adb_interface, interf_name, &name_size, true)) {
    printf("\n      AdbGetInterfaceName returned error %u", GetLastError());
    AdbCloseHandle(adb_interface);
    return false;
  }

  printf("\n      Interface name is %s", interf_name);

  char* ser_num = NULL;
  name_size = 0;
  if (!AdbGetSerialNumber(adb_interface, ser_num, &name_size, true)) {
    ser_num = reinterpret_cast<char*>(malloc(name_size));
    if (NULL != ser_num) {
      if (!AdbGetSerialNumber(adb_interface, ser_num, &name_size, true)) {
        printf("\n      AdbGetSerialNumber returned error %u", GetLastError());
        AdbCloseHandle(adb_interface);
        return false;
      }
      printf("\n      Interface serial number is %s", ser_num);
      free(ser_num);
    }
  } else {
    printf("\nAdbGetSerialNumber(adb_interface, ser_num, &name_size, true)");
  }

  // Get default read endpoint
  ADBAPIHANDLE adb_read = AdbOpenDefaultBulkReadEndpoint(adb_interface,
                                                         AdbOpenAccessTypeReadWrite,
                                                         AdbOpenSharingModeReadWrite);
  if (NULL == adb_read) {
    printf("\n      AdbOpenDefaultBulkReadEndpoint returned error %u", GetLastError());
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Get default write endpoint
  ADBAPIHANDLE adb_write = AdbOpenDefaultBulkWriteEndpoint(adb_interface,
                                                           AdbOpenAccessTypeReadWrite,
                                                           AdbOpenSharingModeReadWrite);
  if (NULL == adb_write) {
    printf("\n      AdbOpenDefaultBulkWriteEndpoint returned error %u", GetLastError());
    AdbCloseHandle(adb_read);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Send connect message
  message msg_send;
  msg_send.command = A_CNXN;
  msg_send.arg0 = A_VERSION;
  msg_send.arg1 = MAX_PAYLOAD;
  msg_send.data_length = 0;
  msg_send.data_crc32 = 0;
  msg_send.magic = msg_send.command ^ 0xffffffff;

  ULONG written_bytes = 0;
  bool write_res = AdbWriteEndpointSync(adb_write, &msg_send, sizeof(msg_send), &written_bytes, 0);
  if (!write_res) {
    printf("\n       AdbWriteEndpointSync returned error %u", GetLastError());
    AdbCloseHandle(adb_write);
    AdbCloseHandle(adb_read);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Receive handshake
  message msg_rcv;
  ULONG read_bytes = 0;
  bool read_res = AdbReadEndpointSync(adb_read, &msg_rcv, sizeof(msg_rcv), &read_bytes, 0);
  if (!read_res) {
    printf("\n       AdbReadEndpointSync returned error %u", GetLastError());
    AdbCloseHandle(adb_write);
    AdbCloseHandle(adb_read);
    AdbCloseHandle(adb_interface);
    return false;
  }

  printf("\n      Read handshake: %u bytes received", read_bytes);
  char* cmd_ansi = reinterpret_cast<char*>(&msg_rcv.command);
  printf("\n         command     = %08X (%c%c%c%c)", msg_rcv.command,
         cmd_ansi[0], cmd_ansi[1], cmd_ansi[2], cmd_ansi[3]);
  printf("\n         arg0        = %08X", msg_rcv.arg0);
  printf("\n         arg1        = %08X", msg_rcv.arg1);
  printf("\n         data_length = %u", msg_rcv.data_length);
  printf("\n         data_crc32  = %08X", msg_rcv.data_crc32);
  printf("\n         magic       = %08X", msg_rcv.magic);

  if (0 != msg_rcv.data_length) {
    char* buf = reinterpret_cast<char*>(malloc(msg_rcv.data_length));
    read_res = AdbReadEndpointSync(adb_read, buf, msg_rcv.data_length, &read_bytes, 0);
    if (!read_res) {
      printf("\n       AdbReadEndpointSync (data) returned error %u", GetLastError());
      free(buf);
      AdbCloseHandle(adb_write);
      AdbCloseHandle(adb_read);
      AdbCloseHandle(adb_interface);
      return false;
    }

    for (ULONG n = 0; n < read_bytes; n++) {
      if (0 == (n % 16))
        printf("\n          ");
      printf("%02X ", buf[n]);
    }

    printf("\n          %s", buf);

    delete buf;
  }

  printf("\nPress any key to close handles...");
  getch();
  AdbCloseHandle(adb_write);
  AdbCloseHandle(adb_read);
  AdbCloseHandle(adb_interface);

  return true;
}

bool RunInterfaceEnumTest() {
  if (!RunInterfaceEnumTest(true, true, true))
    return false;

  if (!RunInterfaceEnumTest(false, false, false))
    return false;

  if (device_active) {
    return true;
  } else {
    // Device has not found in the list of active devices
    printf("\nPlease start the USB device emulator to run the tests");
    return false;
  }
}

bool RunInterfaceEnumTest(bool exclude_not_present,
                          bool exclude_removed,
                          bool active_only) {
  printf("\n\n=== Running RunInterfaceEnumTest(%s, %s, %s)... ",
         exclude_not_present ? "true" : "false",
         exclude_removed ? "true" : "false",
         active_only ? "true" : "false");

  ADBAPIHANDLE adb_handle =
    AdbEnumInterfaces(adb_class_id, exclude_not_present, exclude_removed, active_only);
  if (NULL == adb_handle) {
    printf("\n     Unable to AdbEnumInterfaces. Error %u", GetLastError());
    return false;
  }

  bool res;

  do {
    AdbInterfaceInfo* info = NULL;
    ULONG size = 0;

    res = AdbNextInterface(adb_handle, NULL, &size);
    // We expect 'false' and GetLastError() being either ERROR_NO_MORE_ITEMS
    // or ERROR_INSUFFICIENT_BUFFER
    if (res || ((ERROR_INSUFFICIENT_BUFFER != GetLastError()) &&
                (ERROR_NO_MORE_ITEMS != GetLastError()))) {
      printf("\n    Unexpected AdbNextInterface(NULL) result. Res = %u, Error = %u",
             res, GetLastError());
      AdbCloseHandle(adb_handle);
      return false;
    }

    if (ERROR_INSUFFICIENT_BUFFER == GetLastError()) {
      info = reinterpret_cast<AdbInterfaceInfo*>(malloc(size));
      // Try one byte less than required length
      size--;
      res = AdbNextInterface(adb_handle, info, &size);
      if (res || (ERROR_INSUFFICIENT_BUFFER != GetLastError())) {
        printf("\n    Unexpected AdbNextInterface(small) result. Res = %u, Error = %u",
               res, GetLastError());
        free(info);
        AdbCloseHandle(adb_handle);
        return false;
      }

      size++;
      res = AdbNextInterface(adb_handle, info, &size);

      if (res) {
        if (exclude_not_present && active_only &&
            (0 == wcsicmp(info->device_name, test_interface_name))) {
          device_active = true;
        }
      } else {
        printf("\n    AdbNextInterface failed: %u", GetLastError());
        free(info);
        AdbCloseHandle(adb_handle);
        return false;
      }

      free(info);
    } else {
      res = false;
    }
  } while (res);

  res = AdbCloseHandle(adb_handle);
  if (!res) {
    printf("\n    Unable to AdbCloseHandle:  %u", GetLastError());
    return false;
  }

  // Closing closed handle
  res = AdbCloseHandle(adb_handle);
  if (res || (ERROR_INVALID_HANDLE != GetLastError())) {
    printf("\n    Unexpected AdbCloseHandle(closed) result. Ret = %u, Error = %u",
           res, GetLastError());
    return false;
  }

  printf(" SUCCESS.");
  return true;
}

bool RunInterfaceCreateTest() {
  printf("\n\n=== Running RunInterfaceCreateTest()... ");

  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_EMULATOR_PROD_ID,
                                                  0xFF);
  if (NULL == adb_interface) {
    printf("\n    AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  // Gather information
  USB_DEVICE_DESCRIPTOR dev_desc;
  USB_CONFIGURATION_DESCRIPTOR config_desc;
  USB_INTERFACE_DESCRIPTOR interface_desc;

  bool res = AdbGetUsbDeviceDescriptor(adb_interface, &dev_desc);
  if (!res) {
    printf("\n    AdbGetUsbDeviceDescriptor error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  res = AdbGetUsbConfigurationDescriptor(adb_interface, &config_desc);
  if (!res) {
    printf("\n    AdbGetUsbDeviceDescriptor error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  res = AdbGetUsbInterfaceDescriptor(adb_interface, &interface_desc);
  if (!res) {
    printf("\n    AdbGetUsbDeviceDescriptor error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  wchar_t* wide_buffer = NULL;
  char* char_buffer = NULL;
  ULONG buffer_size = 0;

  res = AdbGetInterfaceName(adb_interface, wide_buffer, &buffer_size, false);
  if (!res) {
    if (ERROR_INSUFFICIENT_BUFFER != GetLastError()) {
      printf("\n    Unable to AdbGetInterfaceName(NULL). Error %u", GetLastError());
      AdbCloseHandle(adb_interface);
      return false;
    }
    wide_buffer = reinterpret_cast<wchar_t*>(malloc(buffer_size * sizeof(wchar_t)));
    res = AdbGetInterfaceName(adb_interface, wide_buffer, &buffer_size, false);
    if (!res) {
      printf("\n    Unable to AdbGetInterfaceName(%u). Error %u", buffer_size, GetLastError());
      AdbCloseHandle(adb_interface);
      return false;
    }
  }

  res = AdbGetInterfaceName(adb_interface, char_buffer, &buffer_size, true);
  if (!res) {
    if (ERROR_INSUFFICIENT_BUFFER != GetLastError()) {
      printf("\n    Unable to AdbGetInterfaceName(NULL). Error %u", GetLastError());
      AdbCloseHandle(adb_interface);
      return false;
    }
    char_buffer = reinterpret_cast<char*>(malloc(buffer_size * sizeof(char)));
    res = AdbGetInterfaceName(adb_interface, char_buffer, &buffer_size, true);
    if (!res) {
      printf("\n    Unable to AdbGetInterfaceName(%u). Error %u", buffer_size, GetLastError());
      AdbCloseHandle(adb_interface);
      return false;
    }
  }

  res = AdbCloseHandle(adb_interface);
  if (!res) {
    res = AdbCloseHandle(adb_interface);
    if (!res)
    printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  res = AdbCloseHandle(adb_interface);
  if (res || (ERROR_INVALID_HANDLE != GetLastError())) {
    printf("\n    Unexpected AdbCloseHandle(closed) result. Ret = %u, Error = %u",
           res, GetLastError());
    return false;
  }

  if (0 != memcmp(&dev_desc, &test_dev_desc, sizeof(USB_DEVICE_DESCRIPTOR))) {
    printf("\n    Wrong USB_DEVICE_DESCRIPTOR");
    return false;
  }

  if (0 != memcmp(&config_desc, &test_config_desc, sizeof(USB_CONFIGURATION_DESCRIPTOR))) {
    printf("\n    Wrong USB_CONFIGURATION_DESCRIPTOR");
    return false;
  }

  if (0 != memcmp(&interface_desc, &test_interface_desc, sizeof(USB_INTERFACE_DESCRIPTOR))) {
    printf("\n    Wrong USB_INTERFACE_DESCRIPTOR");
    return false;
  }

  printf(" SUCCESS.");
  return true;
}

bool RunEndpointInfoTest() {
  printf("\n\n=== Running RunEndpointInfoTest()");
  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_EMULATOR_PROD_ID,
                                                  0xFF);
  if (NULL == adb_interface) {
    printf("\n    AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  USB_INTERFACE_DESCRIPTOR interface_desc;
  BOOL res = AdbGetUsbInterfaceDescriptor(adb_interface, &interface_desc);
  if (!res) {
    printf("\n    AdbGetUsbDeviceDescriptor error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  if (0 != memcmp(&interface_desc, &test_interface_desc, sizeof(USB_INTERFACE_DESCRIPTOR))) {
    printf("\n    Wrong USB_INTERFACE_DESCRIPTOR");
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  for (UCHAR index = 0; index < interface_desc.bNumEndpoints; index++) {
    if (!RunEndpointInfoTest(adb_interface, index)) {
      res = AdbCloseHandle(adb_interface);
      if (!res)
        printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
      return false;
    }
  }

  if (RunEndpointInfoTest(adb_interface, index)) {
    printf("\n    Unexpected success of RunEndpointInfoTest(%u - invalid index)", index);
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  if (!RunEndpointInfoTest(adb_interface, ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX))
    return false;

  if (!RunEndpointInfoTest(adb_interface, ADB_QUERY_BULK_READ_ENDPOINT_INDEX))
    return false;

  res = AdbCloseHandle(adb_interface);
  if (!res) {
    printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  return true;
}

bool RunEndpointInfoTest(ADBAPIHANDLE adb_interface, UCHAR index) {
  printf("\n======= Running RunEndpointInfoTest(%X)... ", index);

  AdbEndpointInformation info;

  if (!AdbGetEndpointInformation(adb_interface, index, &info)) {
    if ((index < 2) || (ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == index) ||
        (ADB_QUERY_BULK_READ_ENDPOINT_INDEX == index)) {
      printf("\n        AdbGetEndpointInformation(u) failed: %u", index, GetLastError());
    }
    return false;
  }

  if (!CheckEndpointInfo(index, &info)) {
    printf("\n        Wrong AdbEndpointInformation(%X)", index);
    return false;
  }

  printf(" SUCCESS.");
  return true;
}

bool RunEndpointOpenTest() {
  printf("\n\n=== Running RunEndpointOpenTest()... ");
  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_EMULATOR_PROD_ID,
                                                  0xFF);
  if (NULL == adb_interface) {
    printf("\n    AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  USB_INTERFACE_DESCRIPTOR interface_desc;
  BOOL res = AdbGetUsbInterfaceDescriptor(adb_interface, &interface_desc);
  if (!res) {
    printf("\n    AdbGetUsbDeviceDescriptor error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  ADBAPIHANDLE adb_endpoint = AdbOpenDefaultBulkReadEndpoint(adb_interface,
                                                             AdbOpenAccessTypeReadWrite,
                                                             AdbOpenSharingModeReadWrite);
  if (NULL == adb_endpoint) {
    printf("\n    AdbOpenDefaultBulkReadEndpoint error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  // Make sure that we can't write to it
  ULONG transfer = 0;
  res = AdbWriteEndpointSync(adb_endpoint,
                         &adb_endpoint,
                         sizeof(adb_endpoint),
                         &transfer,
                         0);
  if (res || (ERROR_ACCESS_DENIED != GetLastError())) {
    printf("\n    AdbWriteEndpoint failure: Ret = %u, error = %u", res, GetLastError());
    AdbCloseHandle(adb_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  AdbCloseHandle(adb_endpoint);

  adb_endpoint = AdbOpenDefaultBulkWriteEndpoint(adb_interface,
                                                 AdbOpenAccessTypeReadWrite,
                                                 AdbOpenSharingModeReadWrite);
  if (NULL == adb_endpoint) {
    printf("\n    AdbOpenDefaultBulkWriteEndpoint error %u", GetLastError());
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  // Make sure we cannot read from it
  ULONG to_read;
  res = AdbReadEndpointSync(adb_endpoint,
                        &to_read,
                        sizeof(to_read),
                        &transfer,
                        0);
  if (res || (ERROR_ACCESS_DENIED != GetLastError())) {
    printf("\n    AdbReadEndpoint failure: Ret = %u, error = %u", res, GetLastError());
    AdbCloseHandle(adb_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  AdbCloseHandle(adb_endpoint);

  for (UCHAR index = 0; index < interface_desc.bNumEndpoints; index++) {
    if (!RunEndpointOpenTest(adb_interface, index)) {
      res = AdbCloseHandle(adb_interface);
      if (!res)
        printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
      return false;
    }
  }

  if (RunEndpointOpenTest(adb_interface, index)) {
    printf("\nRunEndpointOpenTest failed: succeeded on invalid EP %u", index);
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  if (!RunEndpointOpenTest(adb_interface, ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX)) {
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  if (!RunEndpointOpenTest(adb_interface, ADB_QUERY_BULK_READ_ENDPOINT_INDEX)) {
    res = AdbCloseHandle(adb_interface);
    if (!res)
      printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  res = AdbCloseHandle(adb_interface);
  if (!res) {
    printf("\n    Unable to AdbCloseHandle. Error %u", GetLastError());
    return false;
  }

  return true;
}

bool RunEndpointOpenTest(ADBAPIHANDLE adb_interface, UCHAR index) {
  printf("\n======= Running RunEndpointOpenTest(%X)... ", index);
  ADBAPIHANDLE adb_endpoint = AdbOpenEndpoint(adb_interface,
                                              index,
                                              AdbOpenAccessTypeReadWrite,
                                              AdbOpenSharingModeReadWrite);
  if (NULL == adb_endpoint) {
    if ((index < 2) || (ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == index) ||
        (ADB_QUERY_BULK_READ_ENDPOINT_INDEX == index)) {
      printf("\n        AdbOpenEndpoint(%X) error %u", index, GetLastError());
    }
    return false;
  }

  ADBAPIHANDLE parent = AdbGetEndpointInterface(adb_endpoint);
  if (parent != adb_interface) {
    printf("\n        AdbGetEndpointInterface(%X) failure: expected %p returned %p, error %u",
           index, adb_interface, parent, GetLastError());
    AdbCloseHandle(adb_endpoint);
    return false;
  }

  AdbEndpointInformation info;
  if (!AdbQueryInformationEndpoint(adb_endpoint, &info)) {
    printf("\n    Unable to AdbGetEndpointInformationForHandle(%X): %u",
           index, GetLastError());
    AdbCloseHandle(adb_endpoint);
    return false;
  }

  if (!CheckEndpointInfo(index, &info)) {
    printf("\n        Wrong AdbEndpointInformation(%X)", index);
    AdbCloseHandle(adb_endpoint);
    return false;
  }

  AdbCloseHandle(adb_endpoint);

  printf(" SUCCESS");
  return true;
}

bool RunEndpointIoTest(ULONG time_out_base) {
  printf("\n\n=== Running RunEndpointIoTest(%u)... ", time_out_base);
  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_EMULATOR_PROD_ID,
                                                  0xFF);
  if (NULL == adb_interface) {
    printf("\n    AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  ADBAPIHANDLE adb_read_endpoint =
    AdbOpenDefaultBulkReadEndpoint(adb_interface,
                                   AdbOpenAccessTypeReadWrite,
                                   AdbOpenSharingModeReadWrite);
  if (NULL == adb_read_endpoint) {
    printf("\n    AdbOpenDefaultBulkReadEndpoint error %u", GetLastError());
    AdbCloseHandle(adb_interface);
    return false;
  }

  ADBAPIHANDLE adb_write_endpoint =
    AdbOpenDefaultBulkWriteEndpoint(adb_interface,
                                    AdbOpenAccessTypeReadWrite,
                                    AdbOpenSharingModeReadWrite);
  if (NULL == adb_write_endpoint) {
    printf("\n    AdbOpenDefaultBulkWriteEndpoint error %u", GetLastError());
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  AdbEndpointInformation read_info;
  AdbEndpointInformation write_info;

  if (!AdbQueryInformationEndpoint(adb_read_endpoint, &read_info)) {
    printf("\n    AdbQueryInformationEndpoint(read) error %u", GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  if (!AdbQueryInformationEndpoint(adb_write_endpoint, &write_info)) {
    printf("\n    AdbQueryInformationEndpoint(write) error %u", GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  char read_buf[40960];
  char write_buf[40960];
  ULONG written, read;
  ULONG small_block = 101;
  ULONG partial_small_block = small_block - 10;
  ULONG large_block = write_info.max_packet_size * 3 + 3;

  bool wr_res;
  bool rd_res;

  // Simple synchronous write / read of a small block
  memset(write_buf, '0', small_block);
  wr_res = AdbWriteEndpointSync(adb_write_endpoint, write_buf, small_block, &written, time_out_base * small_block);
  if (!wr_res || (written != small_block)) {
    printf("\n    AdbWriteEndpointSync(%u) failure (%u). Written %u. Error %u",
           small_block, wr_res, written, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbReadEndpointSync(adb_read_endpoint, read_buf, small_block, &read, time_out_base * small_block);
  if (!rd_res || (small_block != read)) {
    printf("\n    AdbReadEndpointSync(%u) failure (%u). Read %u. Error %u",
           small_block, rd_res, read, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  if (0 != memcmp(read_buf, write_buf, read)) {
    printf("\n    Simple sync r/w %u data wrong.", small_block);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Simple synchronous write / read of a large block
  memset(write_buf, '1', large_block);
  wr_res = AdbWriteEndpointSync(adb_write_endpoint, write_buf, large_block, &written, time_out_base * large_block);
  if (!wr_res || (written != large_block)) {
    printf("\n    AdbWriteEndpointSync(%u) failure (%u). Written %u. Error %u",
           large_block, wr_res, written, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbReadEndpointSync(adb_read_endpoint, read_buf, large_block, &read, time_out_base * large_block);
  if (!rd_res || (large_block != read)) {
    printf("\n    AdbReadEndpointSync(%u) failure (%u). Read %u. Error %u",
           large_block, rd_res, read, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  if (0 != memcmp(read_buf, write_buf, read)) {
    printf("\n    Simple sync r/w %u data wrong.", large_block);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Simple synchronous write / partial read of a small block
  memset(write_buf, 'u', small_block);
  wr_res = AdbWriteEndpointSync(adb_write_endpoint, write_buf, partial_small_block, &written, time_out_base * small_block);
  if (!wr_res || (written != partial_small_block)) {
    printf("\n    AdbWriteEndpointSync(%u) failure (%u). Written %u. Error %u",
           partial_small_block, wr_res, written, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbReadEndpointSync(adb_read_endpoint, read_buf, small_block, &read, time_out_base * small_block);
  if (!rd_res || (partial_small_block != read)) {
    printf("\n    AdbReadEndpointSync(%u) failure (%u). Read %u. Error %u",
           partial_small_block, rd_res, read, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  if (0 != memcmp(read_buf, write_buf, read)) {
    printf("\n    Simple sync r/w %u data wrong.", small_block);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Simple Aynchronous write / read
  memset(write_buf, 'A', small_block);

  ADBAPIHANDLE adb_w_complete = 
    AdbWriteEndpointAsync(adb_write_endpoint, write_buf, small_block, &written, time_out_base * small_block, NULL);

  if (NULL == adb_w_complete) {
    printf("\n    AdbWriteEndpointAsync(%u) error %u",
           small_block, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  wr_res = AdbGetOvelappedIoResult(adb_w_complete, NULL, &written, true);
  if (!wr_res || (small_block != written)) {
    printf("\n    AdbGetOvelappedIoResult(write %u) failure (%u). Error %u, written %u",
           small_block, wr_res, GetLastError(), written);
    AdbCloseHandle(adb_w_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_w_complete);

  ADBAPIHANDLE adb_r_complete = 
    AdbReadEndpointAsync(adb_read_endpoint, read_buf, small_block, &read, time_out_base * small_block, NULL);
  if (NULL == adb_r_complete) {
    printf("\n    AdbReadEndpointAsync(%u) error %u", small_block, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbGetOvelappedIoResult(adb_r_complete, NULL, &read, true);
  if (!rd_res || (read != small_block)) {
    printf("\n    AdbGetOvelappedIoResult(read %u) failure (%u). Error %u, read %u",
           small_block, rd_res, GetLastError(), read);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_r_complete);

  if (0 != memcmp(read_buf, write_buf, read)) {
    printf("\n    Simple async r/w %u data wrong", small_block);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // Large Aynchronous write / read
  memset(write_buf, 'B', large_block);

  adb_w_complete = 
    AdbWriteEndpointAsync(adb_write_endpoint, write_buf, large_block, &written, time_out_base * large_block, NULL);

  if (NULL == adb_w_complete) {
    printf("\n    AdbWriteEndpointAsync(%u) error %u",
           large_block, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  wr_res = AdbGetOvelappedIoResult(adb_w_complete, NULL, &written, true);
  if (!wr_res || (large_block != written)) {
    printf("\n    AdbGetOvelappedIoResult(write %u) failure (%u). Error %u, written %u",
           large_block, wr_res, GetLastError(), written);
    AdbCloseHandle(adb_w_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_w_complete);

  adb_r_complete = 
    AdbReadEndpointAsync(adb_read_endpoint, read_buf, large_block, &read, time_out_base * large_block, NULL);
  if (NULL == adb_r_complete) {
    printf("\n    AdbReadEndpointAsync(%u) error %u", large_block, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbGetOvelappedIoResult(adb_r_complete, NULL, &read, true);
  if (!rd_res || (read != large_block)) {
    printf("\n    AdbGetOvelappedIoResult(read %u) failure (%u). Error %u, read %u",
           large_block, rd_res, GetLastError(), read);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_r_complete);

  if (0 != memcmp(read_buf, write_buf, read)) {
    printf("\n    Simple async r/w %u data wrong", large_block);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  // We disable this test because our new read model is no longer accumulative
#if 0
  // One async read, many async writes
  ULONG total = write_info.max_packet_size * 5 + 3;
  ULONG block1 = write_info.max_packet_size + 1;
  ULONG block2 = write_info.max_packet_size * 3 + 7;
  ULONG block3 = total - block2 - block1;
  memset(write_buf, 'a', block1);
  memset(write_buf + block1, 'b', block2);
  memset(write_buf + block1 + block2, 'c', block3);

  adb_r_complete = 
    AdbReadEndpointAsync(adb_read_endpoint, read_buf, total, &read, time_out_base * total, NULL);
  if (NULL == adb_r_complete) {
    printf("\n    AdbReadEndpointAsync(%u) error %u", total, GetLastError());
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbGetOvelappedIoResult(adb_r_complete, NULL, &read, false);
  if (rd_res || (GetLastError() != ERROR_IO_INCOMPLETE)) {
    printf("\n    AdbGetOvelappedIoResult(read %u) failure (%u). Error %u, read %u",
           total, rd_res, GetLastError(), read);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  ADBAPIHANDLE adb_w_complete1 = 
    AdbWriteEndpointAsync(adb_write_endpoint, write_buf, block1, &written, time_out_base * block1, NULL);
  if (NULL == adb_w_complete1) {
    printf("\n    Multiwrite block 1 AdbWriteEndpointAsync(%u) error %u",
           block1, GetLastError());
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  ADBAPIHANDLE adb_w_complete2 = 
    AdbWriteEndpointAsync(adb_write_endpoint, write_buf + block1, block2, &written, time_out_base * block2, NULL);
  if (NULL == adb_w_complete2) {
    printf("\n    Multiwrite block 2 AdbWriteEndpointAsync(%u) error %u",
           block1, GetLastError());
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  ADBAPIHANDLE adb_w_complete3 = 
    AdbWriteEndpointAsync(adb_write_endpoint, write_buf + block1 + block2, block3, &written, time_out_base * block3, NULL);
  if (NULL == adb_w_complete3) {
    printf("\n    Multiwrite block 3 AdbWriteEndpointAsync(%u) error %u",
           block1, GetLastError());
    AdbCloseHandle(adb_w_complete2);
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  rd_res = AdbGetOvelappedIoResult(adb_r_complete, NULL, &read, true);
  if (!rd_res || (read != total)) {
    printf("\n    AdbGetOvelappedIoResult(read %u) failure (%u). Error %u, read %u",
           total, rd_res, GetLastError(), read);
    AdbCloseHandle(adb_w_complete3);
    AdbCloseHandle(adb_w_complete2);
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_r_complete);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_r_complete);

  wr_res = AdbGetOvelappedIoResult(adb_w_complete3, NULL, &written, true);
  if (!wr_res || (block3 != written)) {
    printf("\n    Multiwrite block 3 AdbGetOvelappedIoResult(write %u) failure (%u). Error %u, written %u",
           block3, wr_res, GetLastError(), written);
    AdbCloseHandle(adb_w_complete3);
    AdbCloseHandle(adb_w_complete2);
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_w_complete3);

  wr_res = AdbGetOvelappedIoResult(adb_w_complete2, NULL, &written, true);
  if (!wr_res || (block2 != written)) {
    printf("\n    Multiwrite block 2 AdbGetOvelappedIoResult(write %u) failure (%u). Error %u, written %u",
           block2, wr_res, GetLastError(), written);
    AdbCloseHandle(adb_w_complete2);
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_w_complete2);

  wr_res = AdbGetOvelappedIoResult(adb_w_complete1, NULL, &written, true);
  if (!wr_res || (block1 != written)) {
    printf("\n    Multiwrite block 1 AdbGetOvelappedIoResult(write %u) failure (%u). Error %u, written %u",
           block1, wr_res, GetLastError(), written);
    AdbCloseHandle(adb_w_complete1);
    AdbCloseHandle(adb_write_endpoint);
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }
  AdbCloseHandle(adb_w_complete1);
#endif  // 0

#if 0
  // Async writes are not syncronized
  if (0 != memcmp(read_buf, write_buf, block1)) {
    printf("\n   First block wrong");
  } else {
    if (0 != memcmp(read_buf + block1, write_buf + block1, block2)) {
      printf("\n   Second block wrong");
    } else {
      if (0 != memcmp(read_buf + block1 + block2, write_buf + block1 + block2, block3)) {
        printf("\n   Second block wrong");
      }
    }
  }
#endif  // 0

  AdbCloseHandle(adb_write_endpoint);
  AdbCloseHandle(adb_read_endpoint);
  AdbCloseHandle(adb_interface);

  printf(" SUCCESS.");
  return true;
}

bool RunTimeoutsTest() {
  printf("\n\n=== Running RunTimeoutsTest... ");
  ADBAPIHANDLE adb_interface = AdbCreateInterface(adb_class_id,
                                                  DEVICE_VENDOR_ID,
                                                  DEVICE_EMULATOR_PROD_ID,
                                                  0xFF);
  if (NULL == adb_interface) {
    printf("\n    AdbCreateInterface returned error %u", GetLastError());
    return false;
  }

  ADBAPIHANDLE adb_read_endpoint =
    AdbOpenDefaultBulkReadEndpoint(adb_interface,
                                   AdbOpenAccessTypeReadWrite,
                                   AdbOpenSharingModeReadWrite);
  if (NULL == adb_read_endpoint) {
    printf("\n    AdbOpenDefaultBulkReadEndpoint error %u", GetLastError());
    AdbCloseHandle(adb_interface);
    return false;
  }

  ADBAPIHANDLE adb_write_endpoint =
    AdbOpenDefaultBulkWriteEndpoint(adb_interface,
                                    AdbOpenAccessTypeReadWrite,
                                    AdbOpenSharingModeReadWrite);
  if (NULL == adb_write_endpoint) {
    printf("\n    AdbOpenDefaultBulkWriteEndpoint error %u", GetLastError());
    AdbCloseHandle(adb_read_endpoint);
    AdbCloseHandle(adb_interface);
    return false;
  }

  char read_buf[40960];
  char write_buf[40960];
  ULONG written, read;
  ULONG small_block = 60;

  // Test virtually no timeouts
  for (int n = 0; n < 8; n++) {
    memset(write_buf, 'S', small_block);
    bool wr_res = AdbWriteEndpointSync(adb_write_endpoint, write_buf, small_block, &written, 0xFFFFFFF);
    if (!wr_res || (written != small_block)) {
      printf("\n    AdbWriteEndpointSync(%u) failure (%u). Written %u. Error %u",
            small_block, wr_res, written, GetLastError());
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }

    bool rd_res = AdbReadEndpointSync(adb_read_endpoint, read_buf, small_block, &read, 0xFFFFFFF);
    if (!rd_res || (small_block != read)) {
      printf("\n    AdbReadEndpointSync(%u) failure (%u). Read %u. Error %u",
            small_block, rd_res, read, GetLastError());
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }

    if (0 != memcmp(read_buf, write_buf, read)) {
      printf("\n    Simple sync r/w %u data wrong.", small_block);
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }
  }
/*
  ULONG large_block = 2048;

  // Test rediculously small timeouts
  for (n = 0; n < 20; n++) {
    memset(write_buf, 'L', large_block);
    bool wr_res = AdbWriteEndpointSync(adb_write_endpoint, write_buf, large_block, &written, 1);
    if (!wr_res || (written != small_block)) {
      printf("\n    AdbWriteEndpointSync(%u) failure (%u). Written %u. Error %u",
            large_block, wr_res, written, GetLastError());
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }

    bool rd_res = AdbReadEndpointSync(adb_read_endpoint, read_buf, large_block, &read, 1);
    if (!rd_res || (small_block != read)) {
      printf("\n    AdbReadEndpointSync(%u) failure (%u). Read %u. Error %u",
            large_block, rd_res, read, GetLastError());
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }

    if (0 != memcmp(read_buf, write_buf, read)) {
      printf("\n    Simple sync r/w %u data wrong.", small_block);
      AdbCloseHandle(adb_write_endpoint);
      AdbCloseHandle(adb_read_endpoint);
      AdbCloseHandle(adb_interface);
      return false;
    }
  }
*/
  AdbCloseHandle(adb_write_endpoint);
  AdbCloseHandle(adb_read_endpoint);
  AdbCloseHandle(adb_interface);

  printf(" SUCCESS.");
  return true;
}

bool CheckEndpointInfo(UCHAR index, AdbEndpointInformation* info) {
  AdbEndpointInformation* cmp;

  switch (index) {
    case test_read_pipe_index:
    case ADB_QUERY_BULK_READ_ENDPOINT_INDEX:
      cmp = test_read_pipe;
      break;

    default:
      cmp = test_write_pipe;
      break;
  };

  if ((info->max_packet_size != cmp->max_packet_size) ||
      (info->endpoint_address != cmp->endpoint_address) ||
      (info->polling_interval != cmp->polling_interval) ||
      (info->setting_index != cmp->setting_index) ||
      (info->endpoint_type != cmp->endpoint_type) ||
      (info->max_transfer_size != cmp->max_transfer_size)) {
    return false;
  }

  return true;
}

/*
  printf("\n***** USB_DEVICE_DESCRIPTOR");
  printf("\n      bDescriptorType    = %u", dev_desc.bDescriptorType);
  printf("\n      bcdUSB             = x%02X", dev_desc.bcdUSB);
  printf("\n      bDeviceClass       = x%02X", dev_desc.bDeviceClass);
  printf("\n      bDeviceSubClass    = x%02X", dev_desc.bDeviceSubClass);
  printf("\n      bDeviceProtocol    = x%02X", dev_desc.bDeviceProtocol);
  printf("\n      bMaxPacketSize     = %u", dev_desc.bMaxPacketSize0);
  printf("\n      idVendor           = x%04X", dev_desc.idVendor);
  printf("\n      idProduct          = x%04X", dev_desc.idProduct);
  printf("\n      bcdDevice          = x%02X", dev_desc.bcdDevice);
  printf("\n      iManufacturer      = %u", dev_desc.iManufacturer);
  printf("\n      iProduct           = %u", dev_desc.iProduct);
  printf("\n      iSerialNumber      = %u", dev_desc.iSerialNumber);
  printf("\n      bNumConfigurations = %u", dev_desc.bNumConfigurations);

  printf("\n\n***** USB_CONFIGURATION_DESCRIPTOR");
  printf("\n      bDescriptorType     = %u", config_desc.bDescriptorType);
  printf("\n      wTotalLength        = %u", config_desc.wTotalLength);
  printf("\n      bNumInterfaces      = %u", config_desc.bNumInterfaces);
  printf("\n      bConfigurationValue = %u", config_desc.bConfigurationValue);
  printf("\n      iConfiguration      = %u", config_desc.iConfiguration);
  printf("\n      bmAttributes        = %u", config_desc.bmAttributes);
  printf("\n      MaxPower            = %u", config_desc.MaxPower);

  printf("\n\n***** USB_INTERFACE_DESCRIPTOR");
  printf("\n      bLength            = %u", interface_desc.bLength);
  printf("\n      bDescriptorType    = %u", interface_desc.bDescriptorType);
  printf("\n      bInterfaceNumber   = %u", interface_desc.bInterfaceNumber);
  printf("\n      bAlternateSetting  = %u", interface_desc.bAlternateSetting);
  printf("\n      bNumEndpoints      = %u", interface_desc.bNumEndpoints);
  printf("\n      bInterfaceClass    = x%02X", interface_desc.bInterfaceClass);
  printf("\n      bInterfaceSubClass = x%02X", interface_desc.bInterfaceSubClass);
  printf("\n      bInterfaceProtocol = x%02X", interface_desc.bInterfaceProtocol);
  printf("\n      iInterface         = %u", interface_desc.iInterface);

  printf("\n***** ENDPOINT[%X]", index);
  printf("\n      MaximumPacketSize   = %u", info.max_packet_size);
  printf("\n      EndpointAddress     = x%02X", info.endpoint_address);
  printf("\n      Interval            = %u", info.polling_interval);
  printf("\n      SettingIndex        = %u", info.setting_index);
  printf("\n      PipeType            = %u", info.endpoint_type);
  printf("\n      MaximumTransferSize = %u", info.max_transfer_size);
*/

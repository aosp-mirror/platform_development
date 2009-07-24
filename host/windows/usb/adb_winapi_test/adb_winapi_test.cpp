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

// This file contains implementation of a test application that tests
// functionality of AdbWinApi interface. In this test we will use AdbWinApi
// interface in order to enumerate USB interfaces for Android ADB class, and
// for each interface found we will test USB I/O on that interface by sending
// a simple "hand shake" message to the device connected via this interface.

#include "stdafx.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// Android ADB interface identifier
const GUID kAdbInterfaceId = ANDROID_USB_CLASS_ID;

// Number of interfaces detected in TestEnumInterfaces.
int interface_count = 0;

// Constants used to initialize a "handshake" message
#define MAX_PAYLOAD 4096
#define A_SYNC 0x434e5953
#define A_CNXN 0x4e584e43
#define A_OPEN 0x4e45504f
#define A_OKAY 0x59414b4f
#define A_CLSE 0x45534c43
#define A_WRTE 0x45545257
#define A_VERSION 0x01000000

// Formats message sent to USB device
struct message {
    unsigned int command;       /* command identifier constant      */
    unsigned int arg0;          /* first argument                   */
    unsigned int arg1;          /* second argument                  */
    unsigned int data_length;   /* length of payload (0 is allowed) */
    unsigned int data_crc32;    /* crc32 of data payload            */
    unsigned int magic;         /* command ^ 0xffffffff             */
};

//
// Test routines declarations.
//

// Tests interface enumeration.
bool TestEnumInterfaces();

// Tests all interfaces detected for our device class.
bool TestInterfaces();

// Tests interface addressed by the given device name.
bool TestInterface(const wchar_t* device_name);

// Tests interface opened with ADB API.
bool TestInterfaceHandle(ADBAPIHANDLE interface_handle);

// Sends a "handshake" message to the given interface.
bool DeviceHandShake(ADBAPIHANDLE adb_interface);
int __cdecl _tmain(int argc, TCHAR* argv[], TCHAR* envp[]) {
  // Test enum interfaces.
  if (!TestEnumInterfaces())
    return -1;

  if (0 == interface_count) {
    printf("\nNo ADB interfaces found. Make sure that device is "
           "connected to USB port and is powered on.");
    return 1;
  }

  // Test each interface found in the system
  if (!TestInterfaces())
    return -2;

  return 0;
}

bool TestEnumInterfaces() {
  // Enumerate interfaces
  ADBAPIHANDLE enum_handle =
    AdbEnumInterfaces(kAdbInterfaceId, true, true, true);
  if (NULL == enum_handle) {
    printf("\nEnum interfaces failure:");
    printf("\nUnable to enumerate ADB interfaces: %u", GetLastError());
    return false;
  }

  // Unite interface info structure and buffer big enough to contain the
  // largest structure.
  union {
    AdbInterfaceInfo interface_info;
    char buf[4096];
  };
  unsigned long buf_size = sizeof(buf);

  // Enumerate (and count) interfaces, printing information for each found
  // interface.
  interface_count = 0;
  while (AdbNextInterface(enum_handle, &interface_info, &buf_size)) {
    interface_count++;
    printf("\nFound interface %ws:", interface_info.device_name);
    if (interface_info.flags & SPINT_ACTIVE)
      printf(" ACTIVE");
    if (interface_info.flags & SPINT_DEFAULT)
      printf(" DEFAULT");
    if (interface_info.flags & SPINT_REMOVED)
      printf(" REMOVED");

    buf_size = sizeof(buf);;
  };

  AdbCloseHandle(enum_handle);
  return true;
}

bool TestInterfaces() {
  // Enumerate interfaces
  ADBAPIHANDLE enum_handle =
    AdbEnumInterfaces(kAdbInterfaceId, true, true, true);
  if (NULL == enum_handle) {
    printf("\nTest interfaces failure:");
    printf("\nUnable to enumerate ADB interfaces: %u", GetLastError());
    return false;
  }

  // Unite interface info structure and buffer big enough to contain the
  // largest structure.
  union {
    AdbInterfaceInfo interface_info;
    char buf[4096];
  };
  unsigned long buf_size = sizeof(buf);

  // Test each found interface
  while (AdbNextInterface(enum_handle, &interface_info, &buf_size)) {
    TestInterface(interface_info.device_name);
    buf_size = sizeof(buf);
  };

  AdbCloseHandle(enum_handle);

  // Create interface by VID/PID/MI
  ADBAPIHANDLE interface_handle =
      AdbCreateInterface(kAdbInterfaceId, DEVICE_VENDOR_ID,
                         DEVICE_COMPOSITE_PRODUCT_ID, DEVICE_INTERFACE_ID);
  if (NULL == interface_handle) {
    printf("\nUnable to create interface by VID/PID: %u", GetLastError());
    return false;
  }

  // Test it
  TestInterfaceHandle(interface_handle);
  AdbCloseHandle(interface_handle);
  return true;
}

bool TestInterface(const wchar_t* device_name) {
  printf("\n*** Test interface( %ws )", device_name);

  // Get ADB handle to the interface by its name
  ADBAPIHANDLE interface_handle = AdbCreateInterfaceByName(device_name);
  if (NULL == interface_handle) {
    printf(" FAILED:\nUnable to create interface by name: %u", GetLastError());
    return false;
  }

  // Test it
  TestInterfaceHandle(interface_handle);
  AdbCloseHandle(interface_handle);
  return true;
}

bool TestInterfaceHandle(ADBAPIHANDLE interface_handle) {
  // Get interface name.
  char intr_name[4096];
  unsigned long intr_name_size = sizeof(intr_name);
  if (AdbGetInterfaceName(interface_handle, intr_name, &intr_name_size, true)) {
    printf("\n+++ Interface name %s", intr_name);
  } else {
    printf("\n--- AdbGetInterfaceName failure %u", GetLastError());
    return false;
  }

  // Get device descriptor for the interface
  USB_DEVICE_DESCRIPTOR dev_desc;
  if (AdbGetUsbDeviceDescriptor(interface_handle, &dev_desc)) {
    printf("\n+++ Device descriptor:");
    printf("\n        bLength            = %u", dev_desc.bLength);
    printf("\n        bDescriptorType    = %u", dev_desc.bDescriptorType);
    printf("\n        bcdUSB             = %u", dev_desc.bcdUSB);
    printf("\n        bDeviceClass       = %u", dev_desc.bDeviceClass);
    printf("\n        bDeviceSubClass    = %u", dev_desc.bDeviceSubClass);
    printf("\n        bDeviceProtocol    = %u", dev_desc.bDeviceProtocol);
    printf("\n        bMaxPacketSize0    = %u", dev_desc.bMaxPacketSize0);
    printf("\n        idVendor           = %X", dev_desc.idVendor);
    printf("\n        idProduct          = %X", dev_desc.idProduct);
    printf("\n        bcdDevice          = %u", dev_desc.bcdDevice);
    printf("\n        iManufacturer      = %u", dev_desc.iManufacturer);
    printf("\n        iProduct           = %u", dev_desc.iProduct);
    printf("\n        iSerialNumber      = %u", dev_desc.iSerialNumber);
    printf("\n        bNumConfigurations = %u", dev_desc.bDescriptorType);
  } else {
    printf("\n--- AdbGetUsbDeviceDescriptor failure %u", GetLastError());
    return false;
  }

  // Get configuration descriptor for the interface
  USB_CONFIGURATION_DESCRIPTOR config_desc;
  if (AdbGetUsbConfigurationDescriptor(interface_handle, &config_desc)) {
    printf("\n+++ Configuration descriptor:");
    printf("\n        bLength             = %u", config_desc.bLength);
    printf("\n        bDescriptorType     = %u", config_desc.bDescriptorType);
    printf("\n        wTotalLength        = %u", config_desc.wTotalLength);
    printf("\n        bNumInterfaces      = %u", config_desc.bNumInterfaces);
    printf("\n        bConfigurationValue = %u", config_desc.bConfigurationValue);
    printf("\n        iConfiguration      = %u", config_desc.iConfiguration);
    printf("\n        bmAttributes        = %u", config_desc.bmAttributes);
    printf("\n        MaxPower            = %u", config_desc.MaxPower);
  } else {
    printf("\n--- AdbGetUsbConfigurationDescriptor failure %u", GetLastError());
    return false;
  }

  // Get device serial number
  char ser_num[1024];
  unsigned long ser_num_size = sizeof(ser_num);
  if (AdbGetSerialNumber(interface_handle, ser_num, &ser_num_size, true)) {
    printf("\n+++ Serial number: %s", ser_num);
  } else {
    printf("\n--- AdbGetSerialNumber failure %u", GetLastError());
    return false;
  }

  // Get interface descriptor
  USB_INTERFACE_DESCRIPTOR intr_desc;
  if (AdbGetUsbInterfaceDescriptor(interface_handle, &intr_desc)) {
    printf("\n+++ Interface descriptor:");
    printf("\n        bDescriptorType    = %u", intr_desc.bDescriptorType);
    printf("\n        bInterfaceNumber   = %u", intr_desc.bInterfaceNumber);
    printf("\n        bAlternateSetting  = %u", intr_desc.bAlternateSetting);
    printf("\n        bNumEndpoints      = %u", intr_desc.bNumEndpoints);
    printf("\n        bInterfaceClass    = %u", intr_desc.bInterfaceClass);
    printf("\n        bInterfaceSubClass = %u", intr_desc.bInterfaceSubClass);
    printf("\n        bInterfaceProtocol = %u", intr_desc.bInterfaceProtocol);
    printf("\n        iInterface         = %u", intr_desc.iInterface);
  } else {
    printf("\n--- AdbGetUsbInterfaceDescriptor failure %u", GetLastError());
    return false;
  }

  // Enumerate interface's endpoints
  AdbEndpointInformation pipe_info;
  for (UCHAR pipe = 0; pipe < intr_desc.bNumEndpoints; pipe++) {
    if (AdbGetEndpointInformation(interface_handle, pipe, &pipe_info)) {
      printf("\n      PIPE %u info:", pipe);
      printf("\n          max_packet_size   = %u", pipe_info.max_packet_size);
      printf("\n          max_transfer_size = %u", pipe_info.max_transfer_size);
      printf("\n          endpoint_type     = %u", pipe_info.endpoint_type);
      printf("\n          endpoint_address  = %02X", pipe_info.endpoint_address);
      printf("\n          polling_interval  = %u", pipe_info.polling_interval);
      printf("\n          setting_index     = %u", pipe_info.setting_index);
    } else {
      printf("\n--- AdbGetEndpointInformation(%u) failure %u", pipe, GetLastError());
      return false;
    }
  }

  // Get default bulk read endpoint info
  if (AdbGetDefaultBulkReadEndpointInformation(interface_handle, &pipe_info)) {
    printf("\n      Default Bulk Read Pipe info:");
    printf("\n          max_packet_size   = %u", pipe_info.max_packet_size);
    printf("\n          max_transfer_size = %u", pipe_info.max_transfer_size);
    printf("\n          endpoint_type     = %u", pipe_info.endpoint_type);
    printf("\n          endpoint_address  = %02X", pipe_info.endpoint_address);
    printf("\n          polling_interval  = %u", pipe_info.polling_interval);
    printf("\n          setting_index     = %u", pipe_info.setting_index);
  } else {
    printf("\n--- AdbGetDefaultBulkReadEndpointInformation failure %u", GetLastError());
    return false;
  }

  // Get default bulk write endpoint info
  if (AdbGetDefaultBulkWriteEndpointInformation(interface_handle, &pipe_info)) {
    printf("\n      Default Bulk Write Pipe info:");
    printf("\n          max_packet_size   = %u", pipe_info.max_packet_size);
    printf("\n          max_transfer_size = %u", pipe_info.max_transfer_size);
    printf("\n          endpoint_type     = %u", pipe_info.endpoint_type);
    printf("\n          endpoint_address  = %02X", pipe_info.endpoint_address);
    printf("\n          polling_interval  = %u", pipe_info.polling_interval);
    printf("\n          setting_index     = %u", pipe_info.setting_index);
  } else {
    printf("\n--- AdbGetDefaultBulkWriteEndpointInformation failure %u", GetLastError());
    return false;
  }

  // Test a handshake on that interface
  DeviceHandShake(interface_handle);

  return true;
}

bool DeviceHandShake(ADBAPIHANDLE adb_interface) {
  // Get interface name
  char interf_name[512];
  unsigned long name_size = sizeof(interf_name);
  if (!AdbGetInterfaceName(adb_interface, interf_name, &name_size, true)) {
    printf("\nDeviceHandShake: AdbGetInterfaceName returned error %u",
           GetLastError());
    return false;
  }

  printf("\n\nDeviceHandShake on %s", interf_name);

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
      printf("\nInterface serial number is %s", ser_num);
      free(ser_num);
    }
  }

  // Get default read endpoint
  ADBAPIHANDLE adb_read = AdbOpenDefaultBulkReadEndpoint(adb_interface,
                                                         AdbOpenAccessTypeReadWrite,
                                                         AdbOpenSharingModeReadWrite);
  if (NULL == adb_read) {
    printf("\n      AdbOpenDefaultBulkReadEndpoint returned error %u", GetLastError());
    return false;
  }

  // Get default write endpoint
  ADBAPIHANDLE adb_write = AdbOpenDefaultBulkWriteEndpoint(adb_interface,
                                                           AdbOpenAccessTypeReadWrite,
                                                           AdbOpenSharingModeReadWrite);
  if (NULL == adb_write) {
    printf("\n      AdbOpenDefaultBulkWriteEndpoint returned error %u", GetLastError());
    AdbCloseHandle(adb_read);
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
  bool write_res = AdbWriteEndpointSync(adb_write, &msg_send, sizeof(msg_send), &written_bytes, 500);
  if (!write_res) {
    printf("\n       AdbWriteEndpointSync returned error %u", GetLastError());
    AdbCloseHandle(adb_write);
    AdbCloseHandle(adb_read);
    return false;
  }

  // Receive handshake
  message msg_rcv;
  ULONG read_bytes = 0;
  bool read_res = AdbReadEndpointSync(adb_read, &msg_rcv, sizeof(msg_rcv), &read_bytes, 512);
  if (!read_res) {
    printf("\n       AdbReadEndpointSync returned error %u", GetLastError());
    AdbCloseHandle(adb_write);
    AdbCloseHandle(adb_read);
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
    read_res = AdbReadEndpointSync(adb_read, buf, msg_rcv.data_length, &read_bytes, 512);
    if (!read_res) {
      printf("\n       AdbReadEndpointSync (data) returned error %u", GetLastError());
      free(buf);
      AdbCloseHandle(adb_write);
      AdbCloseHandle(adb_read);
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

  AdbCloseHandle(adb_write);
  AdbCloseHandle(adb_read);

  return true;
}

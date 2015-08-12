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
#define A_AUTH 0x48545541
#define A_VERSION 0x01000000

// AUTH packets first argument
#define ADB_AUTH_TOKEN         1
#define ADB_AUTH_SIGNATURE     2
#define ADB_AUTH_RSAPUBLICKEY  3

// Interface descriptor constants for ADB interface
#define ADB_CLASS              0xff
#define ADB_SUBCLASS           0x42
#define ADB_PROTOCOL           0x1

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

// Test AdbCloseHandle race condition.
bool TestCloseRaceCondition();

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

  // Test for AdbCloseHandle race condition
  if (!TestCloseRaceCondition())
    return -3;

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

    buf_size = sizeof(buf);
  }

  bool ret = true;
  if (GetLastError() != ERROR_NO_MORE_ITEMS) {
    printf("\n--- AdbNextInterface failure %u", GetLastError());
    ret = false;
  }

  if (!AdbCloseHandle(enum_handle)) {
    printf("\n--- AdbCloseHandle failure %u", GetLastError());
    ret = false;
  }

  return ret;
}

bool TestInterfaces() {
  bool ret = true;

  // Enumerate interfaces
  ADBAPIHANDLE enum_handle =
    AdbEnumInterfaces(kAdbInterfaceId, true, true, true);
  if (NULL == enum_handle) {
    printf("\nTest interfaces failure:");
    printf("\nUnable to enumerate ADB interfaces: %u", GetLastError());
    ret = false;
  } else {
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
    }

    if (GetLastError() != ERROR_NO_MORE_ITEMS) {
      printf("\n--- AdbNextInterface failure %u", GetLastError());
      ret = false;
    }

    if (!AdbCloseHandle(enum_handle)) {
      printf("\n--- AdbCloseHandle failure %u", GetLastError());
      ret = false;
    }
  }

  return ret;
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
  if (!AdbCloseHandle(interface_handle)) {
    printf("\n--- AdbCloseHandle failure %u", GetLastError());
    return false;
  }

  return true;
}

bool TestInterfaceName(ADBAPIHANDLE interface_handle) {
  bool ret = true;
  unsigned long intr_name_size = 0;
  char* buf = NULL;

  if (AdbGetInterfaceName(interface_handle, NULL, &intr_name_size, true)) {
    printf("\n--- AdbGetInterfaceName unexpectedly succeeded %u",
           GetLastError());
    ret = false;
    goto exit;
  }
  if (GetLastError() != ERROR_INSUFFICIENT_BUFFER) {
    printf("\n--- AdbGetInterfaceName failure %u", GetLastError());
    ret = false;
    goto exit;
  }
  if (intr_name_size == 0) {
    printf("\n--- AdbGetInterfaceName returned name size of zero");
    ret = false;
    goto exit;
  }

  const size_t buf_size = intr_name_size + 16; // extra in case of overwrite
  buf = reinterpret_cast<char*>(malloc(buf_size));
  if (buf == NULL) {
    printf("\n--- could not malloc %d bytes, errno %u", buf_size, errno);
    ret = false;
    goto exit;
  }
  const char buf_fill = (unsigned char)0xFF;
  memset(buf, buf_fill, buf_size);

  if (!AdbGetInterfaceName(interface_handle, buf, &intr_name_size, true)) {
    printf("\n--- AdbGetInterfaceName failure %u", GetLastError());
    ret = false;
    goto exit;
  }
  if (buf[intr_name_size - 1] != '\0') {
    printf("\n--- AdbGetInterfaceName returned non-NULL terminated string");
    ret = false;
    goto exit;
  }
  for (size_t i = intr_name_size; i < buf_size; ++i) {
    if (buf[i] != buf_fill) {
      printf("\n--- AdbGetInterfaceName overwrote past the end of the buffer at"
             " index %u with 0x%02X", i, (unsigned char)buf[i]);
      ret = false;
      goto exit;
    }
  }

  printf("\n+++ Interface name %s", buf);

exit:
  free(buf);

  return ret;
}

void DumpEndpointInformation(const AdbEndpointInformation* pipe_info) {
  printf("\n          max_packet_size   = %u", pipe_info->max_packet_size);
  printf("\n          max_transfer_size = %u", pipe_info->max_transfer_size);
  printf("\n          endpoint_type     = %u", pipe_info->endpoint_type);
  const char* endpoint_type_desc = NULL;
  switch (pipe_info->endpoint_type) {
#define CASE_TYPE(type) case type: endpoint_type_desc = #type; break
    CASE_TYPE(AdbEndpointTypeInvalid);
    CASE_TYPE(AdbEndpointTypeControl);
    CASE_TYPE(AdbEndpointTypeIsochronous);
    CASE_TYPE(AdbEndpointTypeBulk);
    CASE_TYPE(AdbEndpointTypeInterrupt);
#undef CASE_TYPE
  }
  if (endpoint_type_desc != NULL) {
    printf(" (%s)", endpoint_type_desc);
  }
  printf("\n          endpoint_address  = %02X", pipe_info->endpoint_address);
  printf("\n          polling_interval  = %u", pipe_info->polling_interval);
  printf("\n          setting_index     = %u", pipe_info->setting_index);
}

bool TestInterfaceHandle(ADBAPIHANDLE interface_handle) {
  // Get interface name.
  if (!TestInterfaceName(interface_handle)) {
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
    printf("\n        bNumConfigurations = %u", dev_desc.bNumConfigurations);
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
    if (intr_desc.bInterfaceClass == ADB_CLASS) {
      printf(" (ADB_CLASS)");
    }
    printf("\n        bInterfaceSubClass = %u", intr_desc.bInterfaceSubClass);
    if (intr_desc.bInterfaceSubClass == ADB_SUBCLASS) {
      printf(" (ADB_SUBCLASS)");
    }
    printf("\n        bInterfaceProtocol = %u", intr_desc.bInterfaceProtocol);
    if (intr_desc.bInterfaceProtocol == ADB_PROTOCOL) {
      printf(" (ADB_PROTOCOL)");
    }
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
      DumpEndpointInformation(&pipe_info);
    } else {
      printf("\n--- AdbGetEndpointInformation(%u) failure %u", pipe,
             GetLastError());
      return false;
    }
  }

  // Get default bulk read endpoint info
  if (AdbGetDefaultBulkReadEndpointInformation(interface_handle, &pipe_info)) {
    printf("\n      Default Bulk Read Pipe info:");
    DumpEndpointInformation(&pipe_info);
  } else {
    printf("\n--- AdbGetDefaultBulkReadEndpointInformation failure %u",
           GetLastError());
    return false;
  }

  // Get default bulk write endpoint info
  if (AdbGetDefaultBulkWriteEndpointInformation(interface_handle, &pipe_info)) {
    printf("\n      Default Bulk Write Pipe info:");
    DumpEndpointInformation(&pipe_info);
  } else {
    printf("\n--- AdbGetDefaultBulkWriteEndpointInformation failure %u",
           GetLastError());
    return false;
  }

  // Test a handshake on that interface
  DeviceHandShake(interface_handle);

  return true;
}

void HexDump(const void* data, const size_t read_bytes) {
  const unsigned char* buf = reinterpret_cast<const unsigned char*>(data);
  const size_t line_length = 16;
  for (size_t n = 0; n < read_bytes; n += line_length) {
    const unsigned char* line = &buf[n];
    const size_t max_line = min(line_length, read_bytes - n);

    printf("\n          ");
    for (size_t i = 0; i < line_length; ++i) {
      if (i >= max_line) {
        printf("   ");
      } else {
        printf("%02X ", line[i]);
      }
    }
    printf(" ");
    for (size_t i = 0; i < max_line; ++i) {
      if (isprint(line[i])) {
        printf("%c", line[i]);
      } else {
        printf(".");
      }
    }
  }
}

void DumpMessageArg0(unsigned int command, unsigned int arg0) {
  if (command == A_AUTH) {
    const char* desc = NULL;
    switch (arg0) {
#define CASE_ARG0(arg) case arg: desc = # arg; break
      CASE_ARG0(ADB_AUTH_TOKEN);
      CASE_ARG0(ADB_AUTH_SIGNATURE);
      CASE_ARG0(ADB_AUTH_RSAPUBLICKEY);
#undef CASE_ARG0
    }
    if (desc != NULL) {
      printf(" (%s)", desc);
    }
  }
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
  DumpMessageArg0(msg_rcv.command, msg_rcv.arg0);
  printf("\n         arg1        = %08X", msg_rcv.arg1);
  printf("\n         data_length = %u", msg_rcv.data_length);
  printf("\n         data_crc32  = %08X", msg_rcv.data_crc32);
  printf("\n         magic       = %08X", msg_rcv.magic);
  printf(" (%s)", (msg_rcv.magic == (msg_rcv.command ^ 0xffffffff)) ?
           "valid" : "invalid");

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

    HexDump(buf, read_bytes);

    free(buf);
  }

  if (!AdbCloseHandle(adb_write)) {
    printf("\n--- AdbCloseHandle failure %u", GetLastError());
  }
  if (!AdbCloseHandle(adb_read)) {
    printf("\n--- AdbCloseHandle failure %u", GetLastError());
  }

  return true;
}

// Randomly delay the current thread.
class RandomDelayer {
public:
  // Prepare for a call to Delay() by getting random data. This call might grab
  // locks, causing serialization, so this should be called before
  // time-sensitive code.
  void SeedRandom() {
    r_ = rand();
  }

  // Randomly delay the current thread based on a previous call to SeedRandom().
  void Delay() {
    switch (r_ % 5) {
      case 0:
        Sleep(0); // Give up time slice to another read-to-run thread.
        break;
      case 1:
        // Try to sleep for 1 ms, but probably more based on OS scheduler
        // minimum granularity.
        Sleep(1);
        break;
      case 2:
        // Yield to another thread ready-to-run on the current processor.
        SwitchToThread();
        break;
      case 3:
        // Busy-wait for a random amount of time.
        for (int i = 0; i < r_; ++i) {
          GetLastError();
        }
        break;
      case 4:
        break; // Do nothing, no delay.
    }
  }

private:
  int r_;
};

volatile ADBAPIHANDLE g_read_handle;
volatile ADBAPIHANDLE g_interface_handle;
volatile bool g_stop_close_race_thread;

unsigned __stdcall CloseRaceThread(void*) {
  RandomDelayer r;

  while (!g_stop_close_race_thread) {
    r.SeedRandom();

    // Do volatile reads of both globals
    ADBAPIHANDLE read_handle = g_read_handle;
    ADBAPIHANDLE interface_handle = g_interface_handle;

    // If we got both handles, close them and clear the globals
    if (read_handle != NULL && interface_handle != NULL) {
      // Delay random amount before calling the API that conflicts with
      // Adb{Read,Write}EndpointSync().
      r.Delay();

      if (!AdbCloseHandle(read_handle)) {
        printf("\nAdbCloseHandle(read) failure: %u", GetLastError());
      }
      if (!AdbCloseHandle(interface_handle)) {
        printf("\nAdbCloseHandle(interface) failure: %u", GetLastError());
      }

      // Clear globals so that read thread is free to set them.
      g_read_handle = NULL;
      g_interface_handle = NULL;
    }
  }
  return 0;
}

#define EXPECTED_ERROR_LIST(FOR_EACH) \
  FOR_EACH(ERROR_INVALID_HANDLE) \
  FOR_EACH(ERROR_HANDLES_CLOSED) \
  FOR_EACH(ERROR_OPERATION_ABORTED)

#define MAKE_ARRAY_ITEM(x) x,
const DWORD g_expected_errors[] = {
  EXPECTED_ERROR_LIST(MAKE_ARRAY_ITEM)
};
#undef MAKE_ARRAY_ITEM

#define MAKE_STRING_ITEM(x) #x,
const char* g_expected_error_strings[] = {
  EXPECTED_ERROR_LIST(MAKE_STRING_ITEM)
};
#undef MAKE_STRING_ITEM

std::string get_error_description(const DWORD err) {
  const DWORD* end = g_expected_errors + ARRAYSIZE(g_expected_errors);
  const DWORD* found = std::find(g_expected_errors, end, err);
  if (found != end) {
    return g_expected_error_strings[found - g_expected_errors];
  } else {
    char buf[64];
    _snprintf(buf, sizeof(buf), "%u", err);
    return std::string(buf);
  }
}

bool is_expected_error(const DWORD err) {
  const DWORD* end = g_expected_errors + ARRAYSIZE(g_expected_errors);
  return std::find(g_expected_errors, end, err) != end;
}

// Test to reproduce https://code.google.com/p/android/issues/detail?id=161890
bool TestCloseRaceCondition() {
  const DWORD test_duration_sec = 10;
  printf("\nTesting close race condition for %u seconds... ",
         test_duration_sec);

  ADBAPIHANDLE enum_handle =
    AdbEnumInterfaces(kAdbInterfaceId, true, true, true);
  if (NULL == enum_handle) {
    printf("\nUnable to enumerate ADB interfaces: %u", GetLastError());
    return false;
  }

  union {
    AdbInterfaceInfo interface_info;
    char buf[4096];
  };
  unsigned long buf_size = sizeof(buf);

  // Get the first interface
  if (!AdbNextInterface(enum_handle, &interface_info, &buf_size)) {
    printf("\n--- AdbNextInterface failure %u", GetLastError());
    return false;
  }

  if (!AdbCloseHandle(enum_handle)) {
    printf("\nAdbCloseHandle(enum_handle) failure: %u", GetLastError());
  }

  HANDLE thread_handle = reinterpret_cast<HANDLE>(
    _beginthreadex(NULL, 0, CloseRaceThread, NULL, 0, NULL));
  if (thread_handle == NULL) {
    printf("\n--- _beginthreadex failure %u", errno);
    return false;
  }

  // Run the test for 10 seconds. It usually reproduces the crash in 1 second.
  const DWORD tick_start = GetTickCount();
  const DWORD test_duration_ticks = test_duration_sec * 1000;
  RandomDelayer r;

  std::map<DWORD, size_t> read_errors;

  while (GetTickCount() < tick_start + test_duration_ticks) {
    // Busy-wait until close thread has cleared the handles, so that we don't
    // leak handles during the test.
    while (g_read_handle != NULL) {}
    while (g_interface_handle != NULL) {}

    ADBAPIHANDLE interface_handle = AdbCreateInterfaceByName(
      interface_info.device_name);
    if (interface_handle == NULL) {
      // Not really expected to encounter an error here.
      printf("\n--- AdbCreateInterfaceByName failure %u", GetLastError());
      continue; // try again
    }
    ADBAPIHANDLE read_handle = AdbOpenDefaultBulkReadEndpoint(
      interface_handle, AdbOpenAccessTypeReadWrite,
      AdbOpenSharingModeReadWrite);
    if (read_handle == NULL) {
      // Not really expected to encounter an error here, so report, cleanup,
      // and retry.
      printf("\n--- AdbOpenDefaultBulkReadEndpoint failure %u", GetLastError());
      AdbCloseHandle(interface_handle);
      continue;
    }

    r.SeedRandom();

    // Set handles to allow other thread to close them.
    g_read_handle = read_handle;
    g_interface_handle = interface_handle;

    // Delay random amount before calling the API that conflicts with
    // AdbCloseHandle().
    r.Delay();

    message msg_rcv;
    ULONG read_bytes = 0;

    while (AdbReadEndpointSync(read_handle, &msg_rcv, sizeof(msg_rcv),
                               &read_bytes, 0 /* infinite timeout */)) {
      // Keep reading until a crash or we're broken out of the read
      // (with an error) by the CloseRaceThread.
    }
    read_errors[GetLastError()]++;
  }

  g_stop_close_race_thread = true;
  if (WaitForSingleObject(thread_handle, INFINITE) != WAIT_OBJECT_0) {
    printf("\n--- WaitForSingleObject failure %u", GetLastError());
  }
  if (!CloseHandle(thread_handle)) {
    printf("\n--- CloseHandle failure %u", GetLastError());
  }

  // The expected errors are the errors that would be encountered if the code
  // had all the major concurrent interleavings. So the test only passes if
  // we encountered all the expected errors, and thus stress tested all the
  // possible major concurrent interleavings.
  bool pass = true;
  for (size_t i = 0; i < ARRAYSIZE(g_expected_errors); ++i) {
    // If we didn't encounter the expected error code, then the test failed.
    if (read_errors.count(g_expected_errors[i]) == 0) {
      pass = false;
      break;
    }
  }

  if (pass) {
    printf("passed");
  } else {
    printf("failed.");
    printf("\nPerhaps you just need to run the test longer or again.");
  }

  printf("\nRead Error Code\t\tCount");
  printf("\n=============================");

  for (std::map<DWORD, size_t>::iterator it = read_errors.begin();
       it != read_errors.end(); ++it) {
    printf("\n%s\t%u%s", get_error_description(it->first).c_str(), it->second,
           is_expected_error(it->first) ? " (expected)" : "");
  }

  for (size_t i = 0; i < ARRAYSIZE(g_expected_errors); ++i) {
    if (read_errors.count(g_expected_errors[i]) == 0) {
      printf("\n%s\t%u (was not encountered, but was expected)",
             get_error_description(g_expected_errors[i]).c_str(), 0);
    }
  }

  return pass;
}

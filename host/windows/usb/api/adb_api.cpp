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
  This file consists of implementation of rotines that are exported
  from this DLL.
*/

#include "stdafx.h"
#include "adb_api.h"
#include "adb_object_handle.h"
#include "adb_interface_enum.h"
#include "adb_interface.h"
#include "adb_legacy_interface.h"
#include "adb_endpoint_object.h"
#include "adb_io_completion.h"
#include "adb_helper_routines.h"
#include "adb_winusb_api.h"

/** \brief Points to InstantiateWinUsbInterface exported from AdbWinUsbApi.dll.

  This variable is initialized with the actual address in DllMain routine for
  this DLL on DLL_PROCESS_ATTACH event.
  @see PFN_INSTWINUSBINTERFACE for more information.
*/
PFN_INSTWINUSBINTERFACE InstantiateWinUsbInterface = NULL;

ADBAPIHANDLE __cdecl AdbEnumInterfaces(GUID class_id,
                               bool exclude_not_present,
                               bool exclude_removed,
                               bool active_only) {
  AdbInterfaceEnumObject* enum_obj = NULL;
  ADBAPIHANDLE ret = NULL;

  try {
    // Instantiate and initialize enum object
    enum_obj = new AdbInterfaceEnumObject();

    if (enum_obj->InitializeEnum(class_id,
                                 exclude_not_present,
                                 exclude_removed,
                                 active_only)) {
      // After successful initialization we can create handle.
      ret = enum_obj->CreateHandle();
    }
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
  }

  if (NULL != enum_obj)
    enum_obj->Release();

  return ret;
}

bool __cdecl AdbNextInterface(ADBAPIHANDLE adb_handle,
                      AdbInterfaceInfo* info,
                      unsigned long* size) {
  if (NULL == size) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  // Lookup AdbInterfaceEnumObject object for the handle
  AdbInterfaceEnumObject* adb_ienum_object =
    LookupObject<AdbInterfaceEnumObject>(adb_handle);
  if (NULL == adb_ienum_object)
    return false;

  // Everything is verified. Pass it down to the object
  bool ret = adb_ienum_object->Next(info, size);

  adb_ienum_object->Release();

  return ret;
}

bool __cdecl AdbResetInterfaceEnum(ADBAPIHANDLE adb_handle) {
  // Lookup AdbInterfaceEnumObject object for the handle
  AdbInterfaceEnumObject* adb_ienum_object =
    LookupObject<AdbInterfaceEnumObject>(adb_handle);
  if (NULL == adb_ienum_object)
    return false;

  // Everything is verified. Pass it down to the object
  bool ret = adb_ienum_object->Reset();

  adb_ienum_object->Release();

  return ret;
}

ADBAPIHANDLE __cdecl AdbCreateInterfaceByName(
    const wchar_t* interface_name) {
  AdbInterfaceObject* obj = NULL;
  ADBAPIHANDLE ret = NULL;

  try {
    // Instantiate interface object, depending on the USB driver type.
    if (IsLegacyInterface(interface_name)) {
      // We have legacy USB driver underneath us.
      obj = new AdbLegacyInterfaceObject(interface_name);
    } else {
      // We have WinUsb driver underneath us. Make sure that AdbWinUsbApi.dll
      // is loaded and its InstantiateWinUsbInterface routine address has
      // been cached.
      if (NULL != InstantiateWinUsbInterface) {
        obj = InstantiateWinUsbInterface(interface_name);
        if (NULL == obj) {
          return NULL;
        }
      } else {
        return NULL;
      }
    }

    // Create handle for it
    ret = obj->CreateHandle();
  } catch (...) {
    SetLastError(ERROR_OUTOFMEMORY);
  }

  if (NULL != obj)
    obj->Release();

  return ret;
}

ADBAPIHANDLE __cdecl AdbCreateInterface(GUID class_id,
                                unsigned short vendor_id,
                                unsigned short product_id,
                                unsigned char interface_id) {
  // Enumerate all active interfaces for the given class
  AdbEnumInterfaceArray interfaces;

  if (!EnumerateDeviceInterfaces(class_id,
                                 DIGCF_DEVICEINTERFACE | DIGCF_PRESENT,
                                 true,
                                 true,
                                 &interfaces)) {
    return NULL;
  }

  if (interfaces.empty()) {
    SetLastError(ERROR_DEVICE_NOT_AVAILABLE);
    return NULL;
  }

  // Now iterate over active interfaces looking for the name match.
  // The name is formatted as such:
  // "\\\\?\\usb#vid_xxxx&pid_xxxx&mi_xx#123456789abcdef#{XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}"
  // where
  //    vid_xxxx is for the vendor id (xxxx are hex for the given vendor id),
  //    pid_xxxx is for the product id (xxxx are hex for the given product id)
  //    mi_xx is for the interface id  (xx are hex for the given interface id)
  // EnumerateDeviceInterfaces will guarantee that returned interface names
  // will have our class id at the end of the name (those last XXXes in the
  // format). So, we only need to match the beginning of the name
  wchar_t match_name[64];
  if (0xFF == interface_id) {
    // No interface id for the name.
    swprintf(match_name, L"\\\\?\\usb#vid_%04x&pid_%04x#",
             vendor_id, product_id);
  } else {
    // With interface id for the name.
    swprintf(match_name, L"\\\\?\\usb#vid_%04x&pid_%04x&mi_%02x#",
             vendor_id, product_id, interface_id);
  }
  size_t match_len = wcslen(match_name);

  for (AdbEnumInterfaceArray::iterator it = interfaces.begin();
       it != interfaces.end(); it++) {
    const AdbInstanceEnumEntry& next_interface = *it;
    if (0 == _wcsnicmp(match_name,
                      next_interface.device_name().c_str(),
                      match_len)) {
      // Found requested interface among active interfaces.
      return AdbCreateInterfaceByName(next_interface.device_name().c_str());
    }
  }

  SetLastError(ERROR_DEVICE_NOT_AVAILABLE);
  return NULL;
}

bool __cdecl AdbGetInterfaceName(ADBAPIHANDLE adb_interface,
                         void* buffer,
                         unsigned long* buffer_char_size,
                         bool ansi) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch call to the found object
    bool ret = adb_object->GetInterfaceName(buffer, buffer_char_size, ansi);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetSerialNumber(ADBAPIHANDLE adb_interface,
                        void* buffer,
                        unsigned long* buffer_char_size,
                        bool ansi) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch call to the found object
    bool ret = adb_object->GetSerialNumber(buffer, buffer_char_size, ansi);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetUsbDeviceDescriptor(ADBAPIHANDLE adb_interface,
                               USB_DEVICE_DESCRIPTOR* desc) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    bool ret = adb_object->GetUsbDeviceDescriptor(desc);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetUsbConfigurationDescriptor(ADBAPIHANDLE adb_interface,
                                      USB_CONFIGURATION_DESCRIPTOR* desc) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    bool ret = adb_object->GetUsbConfigurationDescriptor(desc);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetUsbInterfaceDescriptor(ADBAPIHANDLE adb_interface,
                                  USB_INTERFACE_DESCRIPTOR* desc) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    bool ret = adb_object->GetUsbInterfaceDescriptor(desc);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetEndpointInformation(ADBAPIHANDLE adb_interface,
                               UCHAR endpoint_index,
                               AdbEndpointInformation* info) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    bool ret = adb_object->GetEndpointInformation(endpoint_index, info);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetDefaultBulkReadEndpointInformation(ADBAPIHANDLE adb_interface,
                                              AdbEndpointInformation* info) {
  return AdbGetEndpointInformation(adb_interface,
                                   ADB_QUERY_BULK_READ_ENDPOINT_INDEX,
                                   info);
}

bool __cdecl AdbGetDefaultBulkWriteEndpointInformation(ADBAPIHANDLE adb_interface,
                                               AdbEndpointInformation* info) {
  return AdbGetEndpointInformation(adb_interface,
                                   ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX,
                                   info);
}

ADBAPIHANDLE __cdecl AdbOpenEndpoint(ADBAPIHANDLE adb_interface,
                             unsigned char endpoint_index,
                             AdbOpenAccessType access_type,
                             AdbOpenSharingMode sharing_mode) {
  // Lookup interface object for the handle
  AdbInterfaceObject* adb_object =
    LookupObject<AdbInterfaceObject>(adb_interface);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    ADBAPIHANDLE ret =
      adb_object->OpenEndpoint(endpoint_index, access_type, sharing_mode);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return NULL;
  }
}

ADBAPIHANDLE __cdecl AdbOpenDefaultBulkReadEndpoint(ADBAPIHANDLE adb_interface,
                                            AdbOpenAccessType access_type,
                                            AdbOpenSharingMode sharing_mode) {
  return AdbOpenEndpoint(adb_interface,
                         ADB_QUERY_BULK_READ_ENDPOINT_INDEX,
                         access_type,
                         sharing_mode);
}

ADBAPIHANDLE __cdecl AdbOpenDefaultBulkWriteEndpoint(ADBAPIHANDLE adb_interface,
                                             AdbOpenAccessType access_type,
                                             AdbOpenSharingMode sharing_mode) {
  return AdbOpenEndpoint(adb_interface,
                         ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX,
                         access_type,
                         sharing_mode);
}

ADBAPIHANDLE __cdecl AdbGetEndpointInterface(ADBAPIHANDLE adb_endpoint) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    ADBAPIHANDLE ret = adb_object->GetParentInterfaceHandle();
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return NULL;
  }
}

bool __cdecl AdbQueryInformationEndpoint(ADBAPIHANDLE adb_endpoint,
                                 AdbEndpointInformation* info) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    bool ret = adb_object->GetEndpointInformation(info);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

ADBAPIHANDLE __cdecl AdbReadEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                  void* buffer,
                                  unsigned long bytes_to_read,
                                  unsigned long* bytes_read,
                                  unsigned long time_out,
                                  HANDLE event_handle) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    ADBAPIHANDLE ret = adb_object->AsyncRead(buffer,
                                             bytes_to_read,
                                             bytes_read,
                                             event_handle,
                                             time_out);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return NULL;
  }
}

ADBAPIHANDLE __cdecl AdbWriteEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                   void* buffer,
                                   unsigned long bytes_to_write,
                                   unsigned long* bytes_written,
                                   unsigned long time_out,
                                   HANDLE event_handle) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    ADBAPIHANDLE ret = adb_object->AsyncWrite(buffer,
                                              bytes_to_write,
                                              bytes_written,
                                              event_handle,
                                              time_out);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbReadEndpointSync(ADBAPIHANDLE adb_endpoint,
                         void* buffer,
                         unsigned long bytes_to_read,
                         unsigned long* bytes_read,
                         unsigned long time_out) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    bool ret =
      adb_object->SyncRead(buffer, bytes_to_read, bytes_read, time_out);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return NULL;
  }
}

bool __cdecl AdbWriteEndpointSync(ADBAPIHANDLE adb_endpoint,
                          void* buffer,
                          unsigned long bytes_to_write,
                          unsigned long* bytes_written,
                          unsigned long time_out) {
  // Lookup endpoint object for the handle
  AdbEndpointObject* adb_object =
    LookupObject<AdbEndpointObject>(adb_endpoint);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    bool ret =
      adb_object->SyncWrite(buffer, bytes_to_write, bytes_written, time_out);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbGetOvelappedIoResult(ADBAPIHANDLE adb_io_completion,
                             LPOVERLAPPED overlapped,
                             unsigned long* bytes_transferred,
                             bool wait) {
  // Lookup endpoint object for the handle
  AdbIOCompletion* adb_object =
    LookupObject<AdbIOCompletion>(adb_io_completion);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    bool ret =
      adb_object->GetOvelappedIoResult(overlapped, bytes_transferred, wait);
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

bool __cdecl AdbHasOvelappedIoComplated(ADBAPIHANDLE adb_io_completion) {
  // Lookup endpoint object for the handle
  AdbIOCompletion* adb_object =
    LookupObject<AdbIOCompletion>(adb_io_completion);

  if (NULL != adb_object) {
    // Dispatch the call to the found object
    bool ret =
      adb_object->IsCompleted();
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return true;
  }
}

bool __cdecl AdbCloseHandle(ADBAPIHANDLE adb_handle) {
  // Lookup object for the handle
  AdbObjectHandle* adb_object = AdbObjectHandle::Lookup(adb_handle);

  if (NULL != adb_object) {
    // Dispatch close to the found object
    bool ret = adb_object->CloseHandle();
    adb_object->Release();
    return ret;
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }
}

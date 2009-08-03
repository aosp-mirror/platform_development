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
  This file consists of implementation of class AdbEndpointObject that
  encapsulates a handle opened to an endpoint on our device.
*/

#include "stdafx.h"
#include "adb_endpoint_object.h"

AdbEndpointObject::AdbEndpointObject(AdbInterfaceObject* parent_interf,
                                     UCHAR endpoint_id,
                                     UCHAR endpoint_index)
    : AdbObjectHandle(AdbObjectTypeEndpoint),
      parent_interface_(parent_interf),
      endpoint_id_(endpoint_id),
      endpoint_index_(endpoint_index) {
  if (NULL != parent_interface_)
    parent_interface_->AddRef();
}

AdbEndpointObject::~AdbEndpointObject() {
  if (NULL != parent_interface_)
    parent_interface_->Release();
}

bool AdbEndpointObject::GetEndpointInformation(AdbEndpointInformation* info) {
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  return parent_interface()->GetEndpointInformation(endpoint_index(), info);
}

ADBAPIHANDLE AdbEndpointObject::AsyncRead(void* buffer,
                                    ULONG bytes_to_read,
                                    ULONG* bytes_read,
                                    HANDLE event_handle,
                                    ULONG time_out) {
  return CommonAsyncReadWrite(true,
                              buffer,
                              bytes_to_read,
                              bytes_read,
                              event_handle,
                              time_out);
}

ADBAPIHANDLE AdbEndpointObject::AsyncWrite(void* buffer,
                                     ULONG bytes_to_write,
                                     ULONG* bytes_written,
                                     HANDLE event_handle,
                                     ULONG time_out) {
  return CommonAsyncReadWrite(false,
                              buffer,
                              bytes_to_write,
                              bytes_written,
                              event_handle,
                              time_out);
}

bool AdbEndpointObject::SyncRead(void* buffer,
                           ULONG bytes_to_read,
                           ULONG* bytes_read,
                           ULONG time_out) {
  return CommonSyncReadWrite(true,
                             buffer,
                             bytes_to_read,
                             bytes_read,
                             time_out);
}

bool AdbEndpointObject::SyncWrite(void* buffer,
                            ULONG bytes_to_write,
                            ULONG* bytes_written,
                            ULONG time_out) {
  return CommonSyncReadWrite(false,
                             buffer,
                             bytes_to_write,
                             bytes_written,
                             time_out);
}

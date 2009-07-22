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
  This file consists of implementation of class AdbIOObject that
  encapsulates an interface on our USB device.
*/

#include "stdafx.h"
#include "adb_endpoint_object.h"
#include "adb_io_completion.h"
#include "adb_helper_routines.h"

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

ADBAPIHANDLE AdbEndpointObject::CommonAsyncReadWrite(bool is_read,
                                                     void* buffer,
                                                     ULONG bytes_to_transfer,
                                                     ULONG* bytes_transferred,
                                                     HANDLE event_handle,
                                                     ULONG time_out) {
  if (!SetTimeout(time_out))
    return false;

  // Create completion i/o object
  AdbIOCompletion* adb_io_completion = NULL;

  try {
    adb_io_completion = new AdbIOCompletion(this,
                                            bytes_to_transfer,
                                            event_handle);
  } catch (... ) {
    SetLastError(ERROR_OUTOFMEMORY);
    return NULL;
  }

  // Create a handle for it
  ADBAPIHANDLE ret = adb_io_completion->CreateHandle();
  ULONG transferred = 0;
  if (NULL != ret) {
    BOOL res = TRUE;
    // Go the read / write file way
    res = is_read ?
        WinUsb_ReadPipe(parent_interface()->winusb_handle(),
                        endpoint_id(),
                        reinterpret_cast<PUCHAR>(buffer),
                        bytes_to_transfer,
                        &transferred,
                        adb_io_completion->overlapped()) :
        WinUsb_WritePipe(parent_interface()->winusb_handle(),
                         endpoint_id(),
                         reinterpret_cast<PUCHAR>(buffer),
                         bytes_to_transfer,
                         &transferred,
                         adb_io_completion->overlapped());

    if (NULL != bytes_transferred)
      *bytes_transferred = transferred;

    ULONG error = GetLastError();
    if (!res && (ERROR_IO_PENDING != error)) {
      // I/O failed immediatelly. We need to close i/o completion object
      // before we return NULL to the caller.
      adb_io_completion->CloseHandle();
      ret = NULL;
      SetLastError(error);
    }
  }

  // Offseting 'new'
  adb_io_completion->Release();

  return ret;
}

bool AdbEndpointObject::CommonSyncReadWrite(bool is_read,
                                            void* buffer,
                                            ULONG bytes_to_transfer,
                                            ULONG* bytes_transferred,
                                            ULONG time_out) {
  if (!SetTimeout(time_out))
    return false;

  // This is synchronous I/O. Since we always open I/O items for
  // overlapped I/O we're obligated to always provide OVERLAPPED
  // structure to read / write routines. Prepare it now.
  OVERLAPPED overlapped;
  ZeroMemory(&overlapped, sizeof(overlapped));
  overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

  BOOL ret = TRUE;
  ULONG transferred = 0;
  // Go the read / write file way
  ret = is_read ?
        WinUsb_ReadPipe(parent_interface()->winusb_handle(),
                        endpoint_id(),
                        reinterpret_cast<PUCHAR>(buffer),
                        bytes_to_transfer,
                        &transferred,
                        &overlapped) :
        WinUsb_WritePipe(parent_interface()->winusb_handle(),
                         endpoint_id(),
                         reinterpret_cast<PUCHAR>(buffer),
                         bytes_to_transfer,
                         &transferred,
                         &overlapped);

  // Lets see the result
  if (!ret && (ERROR_IO_PENDING != GetLastError())) {
    // I/O failed.
    if (NULL != overlapped.hEvent)
      ::CloseHandle(overlapped.hEvent);
    return false;
  }

  // Lets wait till I/O completes
  ret = WinUsb_GetOverlappedResult(parent_interface()->winusb_handle(), &overlapped,
                                   &transferred, TRUE);
  if (ret && (NULL != bytes_transferred)) {
    *bytes_transferred = transferred;
  }

  if (NULL != overlapped.hEvent)
    ::CloseHandle(overlapped.hEvent);

  return ret ? true : false;
}

bool AdbEndpointObject::SetTimeout(ULONG timeout) {
  if (!WinUsb_SetPipePolicy(parent_interface()->winusb_handle(),
                            endpoint_id(), PIPE_TRANSFER_TIMEOUT,
                            sizeof(ULONG), &timeout)) {
    return false;
  }

  return true;
}

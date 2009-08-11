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
  This file consists of implementation of class AdbWinUsbEndpointObject that
  encapsulates a handle opened to a WinUsb endpoint on our device.
*/

#include "stdafx.h"
#include "adb_winusb_endpoint_object.h"
#include "adb_winusb_io_completion.h"

AdbWinUsbEndpointObject::AdbWinUsbEndpointObject(
    AdbWinUsbInterfaceObject* parent_interf,
    UCHAR endpoint_id,
    UCHAR endpoint_index)
    : AdbEndpointObject(parent_interf, endpoint_id, endpoint_index) {
}

AdbWinUsbEndpointObject::~AdbWinUsbEndpointObject() {
}

LONG AdbWinUsbEndpointObject::Release() {
  ATLASSERT(ref_count_ > 0);
  LONG ret = InterlockedDecrement(&ref_count_);
  ATLASSERT(ret >= 0);
  if (0 == ret) {
    LastReferenceReleased();
    delete this;
  }
  return ret;
}

ADBAPIHANDLE AdbWinUsbEndpointObject::CommonAsyncReadWrite(
    bool is_read,
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
    adb_io_completion = new AdbWinUsbIOCompletion(this,
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
        WinUsb_ReadPipe(parent_winusb_interface()->winusb_handle(),
                        endpoint_id(),
                        reinterpret_cast<PUCHAR>(buffer),
                        bytes_to_transfer,
                        &transferred,
                        adb_io_completion->overlapped()) :
        WinUsb_WritePipe(parent_winusb_interface()->winusb_handle(),
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

bool AdbWinUsbEndpointObject::CommonSyncReadWrite(bool is_read,
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
        WinUsb_ReadPipe(parent_winusb_interface()->winusb_handle(),
                        endpoint_id(),
                        reinterpret_cast<PUCHAR>(buffer),
                        bytes_to_transfer,
                        &transferred,
                        &overlapped) :
        WinUsb_WritePipe(parent_winusb_interface()->winusb_handle(),
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
  ret = WinUsb_GetOverlappedResult(parent_winusb_interface()->winusb_handle(), &overlapped,
                                   &transferred, TRUE);
  if (ret && (NULL != bytes_transferred)) {
    *bytes_transferred = transferred;
  }

  if (NULL != overlapped.hEvent)
    ::CloseHandle(overlapped.hEvent);

  return ret ? true : false;
}

bool AdbWinUsbEndpointObject::SetTimeout(ULONG timeout) {
  if (!WinUsb_SetPipePolicy(parent_winusb_interface()->winusb_handle(),
                            endpoint_id(), PIPE_TRANSFER_TIMEOUT,
                            sizeof(ULONG), &timeout)) {
    return false;
  }

  return true;
}

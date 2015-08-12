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
    : AdbEndpointObject(parent_interf, endpoint_id, endpoint_index),
    lock_(), is_closing_(false), pending_io_count_(0) {
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

bool AdbWinUsbEndpointObject::CloseHandle() {
  // This method only returns once all pending IOs are aborted and after
  // preventing future pending IOs. This means that once CloseHandle()
  // returns, threads using this object won't be using
  // parent_winusb_interface()->winusb_handle(), so it can then be safely
  // released.
  lock_.Lock();
  if (!is_closing_) {
    // Set flag to prevent new I/Os from starting up.
    is_closing_ = true;
  }

  // While there are pending IOs, keep aborting the pipe. We have to do this
  // repeatedly because pending_ios_ is incremented before the IO has actually
  // started, and abort (probably) only works if the IO has been started.
  while (pending_io_count_ > 0) {
    lock_.Unlock();

    // It has been noticed that on Windows 7, if you only call
    // WinUsb_AbortPipe(), without first calling WinUsb_ResetPipe(), the call
    // to WinUsb_AbortPipe() hangs.
    if (!WinUsb_ResetPipe(parent_winusb_interface()->winusb_handle(),
                          endpoint_id()) ||
        !WinUsb_AbortPipe(parent_winusb_interface()->winusb_handle(),
                          endpoint_id())) {
      // Reset or Abort failed for unexpected reason. We might not be able to
      // abort pending IOs, so we shouldn't keep polling pending_io_count_ or
      // else we might hang forever waiting for the IOs to abort. In this
      // situation it is preferable to risk a race condition (which may or may
      // not crash) and just break now.
      lock_.Lock();
      break;
    }

    // Give the IO threads time to break out of I/O calls and decrement
    // pending_io_count_. They should finish up pretty quick. The amount of time
    // "wasted" here (as opposed to if we did synchronization with an event)
    // doesn't really matter since this is an uncommon corner-case.
    Sleep(16);  // 16 ms, old default OS scheduler granularity

    lock_.Lock();
  }

  lock_.Unlock();

  return AdbEndpointObject::CloseHandle();
}

ADBAPIHANDLE AdbWinUsbEndpointObject::CommonAsyncReadWrite(
    bool is_read,
    void* buffer,
    ULONG bytes_to_transfer,
    ULONG* bytes_transferred,
    HANDLE event_handle,
    ULONG time_out) {
  // TODO: Do synchronization with is_closing_ and pending_io_count_ like
  // CommonSyncReadWrite(). This is not yet implemented because there are no
  // callers to Adb{Read,Write}EndpointAsync() in AOSP, and hence no testing.
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
  lock_.Lock();
  if (is_closing_) {
    lock_.Unlock();
    // AdbCloseHandle() is in progress, so don't start up any new IOs.
    SetLastError(ERROR_HANDLES_CLOSED);
    return false;
  } else {
    // Not closing down, so record the fact that we're doing IO. This will
    // prevent CloseHandle() from returning until our IO completes or it aborts
    // our IO.
    ++pending_io_count_;
    lock_.Unlock();
  }

  // Because we've incremented pending_ios_, do the matching decrement when this
  // object goes out of scope.
  DecrementPendingIO dec(this);

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

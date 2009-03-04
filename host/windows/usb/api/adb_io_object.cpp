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
  This file consists of implementation of class AdbIOObject that encapsulates
  an item on our device that is opened for read / write / IOCTL I/O.
*/

#include "stdafx.h"
#include "adb_io_object.h"
#include "adb_io_completion.h"
#include "adb_helper_routines.h"

AdbIOObject::AdbIOObject(AdbInterfaceObject* parent_interf,
                         AdbObjectType obj_type)
    : AdbObjectHandle(obj_type),
      usb_handle_(INVALID_HANDLE_VALUE),
      parent_interface_(parent_interf) {
  ATLASSERT(NULL != parent_interf);
  parent_interf->AddRef();
}

AdbIOObject::~AdbIOObject() {
  if (INVALID_HANDLE_VALUE != usb_handle_)
    ::CloseHandle(usb_handle_);
  parent_interface_->Release();
}

ADBAPIHANDLE AdbIOObject::CreateHandle(const wchar_t* item_path,
                                       AdbOpenAccessType access_type,
                                       AdbOpenSharingMode share_mode) {
  // Make sure that we don't have USB handle here
  if (IsUsbOpened()) {
    SetLastError(ERROR_GEN_FAILURE);
    return NULL;
  }

  // Convert access / share parameters into CreateFile - compatible
  ULONG desired_access;
  ULONG desired_sharing;

  if (!GetSDKComplientParam(access_type, share_mode,
                            &desired_access, &desired_sharing)) {
    return NULL;
  }

  // Open USB handle
  usb_handle_ = CreateFile(item_path,
                           desired_access,
                           share_mode,
                           NULL,
                           OPEN_EXISTING,
                           FILE_FLAG_OVERLAPPED,  // Always overlapped!
                           NULL);
  if (INVALID_HANDLE_VALUE == usb_handle_)
    return NULL;

  // Create ADB handle
  ADBAPIHANDLE ret = AdbObjectHandle::CreateHandle();

  if (NULL == ret) {
    // If creation of ADB handle failed we have to close USB handle too.
    ULONG error = GetLastError();
    ::CloseHandle(usb_handle());
    usb_handle_ = INVALID_HANDLE_VALUE;
    SetLastError(error);
  }

  return ret;
}

bool AdbIOObject::CloseHandle() {
  // Lets close USB item first
  if (IsUsbOpened()) {
    ::CloseHandle(usb_handle());
    usb_handle_ = INVALID_HANDLE_VALUE;
  }

  return AdbObjectHandle::CloseHandle();
}

ADBAPIHANDLE AdbIOObject::AsyncRead(void* buffer,
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

ADBAPIHANDLE AdbIOObject::AsyncWrite(void* buffer,
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

bool AdbIOObject::SyncRead(void* buffer,
                           ULONG bytes_to_read,
                           ULONG* bytes_read,
                           ULONG time_out) {
  return CommonSyncReadWrite(true,
                             buffer,
                             bytes_to_read,
                             bytes_read,
                             time_out);
}

bool AdbIOObject::SyncWrite(void* buffer,
                            ULONG bytes_to_write,
                            ULONG* bytes_written,
                            ULONG time_out) {
  return CommonSyncReadWrite(false,
                             buffer,
                             bytes_to_write,
                             bytes_written,
                             time_out);
}

ADBAPIHANDLE AdbIOObject::CommonAsyncReadWrite(bool is_read,
                                               void* buffer,
                                               ULONG bytes_to_transfer,
                                               ULONG* bytes_transferred,
                                               HANDLE event_handle,
                                               ULONG time_out) {
  if (NULL != bytes_transferred)
    *bytes_transferred = 0;

  if (!IsOpened() || !IsUsbOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  bool is_ioctl_write = is_read ? false : (0 != time_out);

  // Create completion i/o object
  AdbIOCompletion* adb_io_completion = NULL;

  try {
    adb_io_completion = new AdbIOCompletion(this,
                                            is_ioctl_write,
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
    if (0 == time_out) {
      // Go the read / write file way
      res = is_read ? ReadFile(usb_handle(),
                               buffer,
                               bytes_to_transfer,
                               &transferred,
                               adb_io_completion->overlapped()) :
                      WriteFile(usb_handle(),
                                buffer,
                                bytes_to_transfer,
                                &transferred,
                                adb_io_completion->overlapped());
    } else {
      // Go IOCTL way
      AdbBulkTransfer transfer_param;
      transfer_param.time_out = time_out;
      transfer_param.transfer_size = is_read ? 0 : bytes_to_transfer;
      transfer_param.SetWriteBuffer(is_read ? NULL : buffer);

      res = DeviceIoControl(usb_handle(),
        is_read ? ADB_IOCTL_BULK_READ : ADB_IOCTL_BULK_WRITE,
        &transfer_param, sizeof(transfer_param),
        is_read ? buffer : adb_io_completion->transferred_bytes_ptr(),
        is_read ? bytes_to_transfer : sizeof(ULONG),
        &transferred,
        adb_io_completion->overlapped());
    }

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

bool AdbIOObject::CommonSyncReadWrite(bool is_read,
                                      void* buffer,
                                      ULONG bytes_to_transfer,
                                      ULONG* bytes_transferred,
                                      ULONG time_out) {
  if (NULL != bytes_transferred)
    *bytes_transferred = 0;

  if (!IsOpened() || !IsUsbOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  bool is_ioctl_write = is_read ? false : (0 != time_out);

  // This is synchronous I/O. Since we always open I/O items for
  // overlapped I/O we're obligated to always provide OVERLAPPED
  // structure to read / write routines. Prepare it now.
  OVERLAPPED overlapped;
  ZeroMemory(&overlapped, sizeof(overlapped));

  BOOL ret = TRUE;
  ULONG ioctl_write_transferred = 0;
  if (0 == time_out) {
    // Go the read / write file way
    ret = is_read ?
      ReadFile(usb_handle(), buffer, bytes_to_transfer, bytes_transferred, &overlapped) :
      WriteFile(usb_handle(), buffer, bytes_to_transfer, bytes_transferred, &overlapped);
  } else {
    // Go IOCTL way
    AdbBulkTransfer transfer_param;
    transfer_param.time_out = time_out;
    transfer_param.transfer_size = is_read ? 0 : bytes_to_transfer;
    transfer_param.SetWriteBuffer(is_read ? NULL : buffer);

    ULONG tmp;
    ret = DeviceIoControl(usb_handle(),
      is_read ? ADB_IOCTL_BULK_READ : ADB_IOCTL_BULK_WRITE,
      &transfer_param, sizeof(transfer_param),
      is_read ? buffer : &ioctl_write_transferred,
      is_read ? bytes_to_transfer : sizeof(ULONG),
      &tmp,
      &overlapped);
  }

  // Lets see the result
  if (!ret && (ERROR_IO_PENDING != GetLastError())) {
    // I/O failed.
    return false;
  }

  // Lets wait till I/O completes
  ULONG transferred = 0;
  ret = GetOverlappedResult(usb_handle(), &overlapped, &transferred, TRUE);
  if (ret && (NULL != bytes_transferred)) {
    *bytes_transferred = is_ioctl_write ? ioctl_write_transferred :
                                          transferred;
  }

  return ret ? true : false;
}

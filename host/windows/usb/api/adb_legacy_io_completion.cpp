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
  This file consists of implementation of class AdbLegacyIOCompletion that
  encapsulates a wrapper around OVERLAPPED Win32 structure returned from
  asynchronous I/O requests issued via legacy USB API.
*/

#include "stdafx.h"
#include "adb_legacy_io_completion.h"

AdbLegacyIOCompletion::AdbLegacyIOCompletion(
    AdbLegacyEndpointObject* parent_io_obj,
    ULONG expected_trans_size,
    HANDLE event_hndl,
    bool is_write_ctl)
    : AdbIOCompletion(parent_io_obj, expected_trans_size, event_hndl),
      transferred_bytes_(0),
      is_write_ioctl_(is_write_ctl) {
}

AdbLegacyIOCompletion::~AdbLegacyIOCompletion() {
}

bool AdbLegacyIOCompletion::GetOvelappedIoResult(LPOVERLAPPED ovl_data,
                                                 ULONG* bytes_transferred,
                                                 bool wait) {
  if (NULL != bytes_transferred) {
    *bytes_transferred = 0;
  }

  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  ULONG transfer;
  bool ret = GetOverlappedResult(parent_legacy_io_object()->usb_handle(),
                                 overlapped(),
                                 &transfer,
                                 wait) ? true :
                                         false;

  // TODO: This is bizzare but I've seen it happening
  // that GetOverlappedResult with wait set to true returns "prematurely",
  // with wrong transferred bytes value and GetLastError reporting
  // ERROR_IO_PENDING. So, lets give it an up to a 20 ms loop!
  ULONG error = GetLastError();

  if (wait && ret && (0 == transfer) && (0 != expected_transfer_size_) &&
      ((ERROR_IO_INCOMPLETE == error) || (ERROR_IO_PENDING == error))) {
    for (int trying = 0; trying < 10; trying++) {
      Sleep(2);
      ret = GetOverlappedResult(parent_legacy_io_object()->usb_handle(),
                                overlapped(),
                                &transfer,
                                wait) ? true :
                                        false;
      error = GetLastError();
      if (!ret || (0 != transfer) ||
          ((ERROR_IO_INCOMPLETE != error) && (ERROR_IO_PENDING != error))) {
        break;
      }
    }
  }

  if (NULL != ovl_data) {
    CopyMemory(ovl_data, overlapped(), sizeof(OVERLAPPED));
  }

  if (NULL != bytes_transferred) {
    *bytes_transferred = is_write_ioctl() ? transferred_bytes_ : transfer;
  }

  return ret;
}

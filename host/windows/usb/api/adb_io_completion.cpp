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
  This file consists of implementation of class AdbIOCompletion that
  encapsulates a generic wrapper around OVERLAPPED Win32 structure
  returned from asynchronous I/O requests.
*/

#include "stdafx.h"
#include "adb_io_completion.h"

AdbIOCompletion::AdbIOCompletion(AdbEndpointObject* parent_io_obj,
                                 ULONG expected_trans_size,
                                 HANDLE event_hndl)
    : AdbObjectHandle(AdbObjectTypeIoCompletion),
      expected_transfer_size_(expected_trans_size),
      parent_io_object_(parent_io_obj) {
  ATLASSERT(NULL != parent_io_obj);
  parent_io_obj->AddRef();
  ZeroMemory(&overlapped_, sizeof(overlapped_));
  overlapped_.hEvent = event_hndl;
}

AdbIOCompletion::~AdbIOCompletion() {
  parent_io_object_->Release();
}

bool AdbIOCompletion::IsCompleted() {
  SetLastError(NO_ERROR);
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return true;
  }

  return HasOverlappedIoCompleted(overlapped()) ? true : false;
}

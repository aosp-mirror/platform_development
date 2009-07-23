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
  This file consists of implementation of AdbInterfaceEnumObject class that
  encapsulates enumerator of USB interfaces available through this API.
*/

#include "stdafx.h"
#include "adb_api.h"
#include "adb_interface_enum.h"
#include "adb_helper_routines.h"

AdbInterfaceEnumObject::AdbInterfaceEnumObject()
    : AdbObjectHandle(AdbObjectTypeInterfaceEnumerator) {
  current_interface_ = interfaces_.begin();
}

AdbInterfaceEnumObject::~AdbInterfaceEnumObject() {
}

bool AdbInterfaceEnumObject::InitializeEnum(GUID class_id,
                                            bool exclude_not_present,
                                            bool exclude_removed,
                                            bool active_only) {
  // Calc flags for SetupDiGetClassDevs
  DWORD flags = DIGCF_DEVICEINTERFACE;
  if (exclude_not_present)
    flags |= DIGCF_PRESENT;

  // Do the enum
  bool ret = EnumerateDeviceInterfaces(class_id,
                                       flags,
                                       exclude_removed,
                                       active_only,
                                       &interfaces_);

  // If enum was successfull set current enum pointer
  // to the beginning of the array
  if (ret)
    current_interface_ = interfaces_.begin();

  return ret;
}

bool AdbInterfaceEnumObject::Next(AdbInterfaceInfo* info, ULONG* size) {
  // Make sure that it's opened
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  ATLASSERT(NULL != size);
  if (NULL == size) {
    SetLastError(ERROR_INVALID_PARAMETER);
    return false;
  }

  // Lets see if enum is over
  if (interfaces_.end() == current_interface_) {
    SetLastError(ERROR_NO_MORE_ITEMS);
    return false;
  }

  AdbInstanceEnumEntry& entry = *current_interface_;

  // Big enough?
  if ((NULL == info) || (*size < entry.GetFlatSize())) {
    *size = entry.GetFlatSize();
    SetLastError(ERROR_INSUFFICIENT_BUFFER);
    return false;
  }

  // All checks passed
  entry.Save(info);
  current_interface_++;
  return true;
}

bool AdbInterfaceEnumObject::Reset() {
  // Make sure that it's opened
  if (!IsOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  current_interface_ = interfaces_.begin();

  return true;
}

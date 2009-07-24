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
  This file consists of implementation of class AdbLegacyInterfaceObject
  that encapsulates an interface on our USB device that is accessible
  via WinUsb API.
*/

#include "stdafx.h"
#include "adb_legacy_interface.h"
#include "adb_endpoint_object.h"

AdbLegacyInterfaceObject::AdbLegacyInterfaceObject(const wchar_t* interf_name)
  : AdbInterfaceObject(interf_name) {
}

AdbLegacyInterfaceObject::~AdbLegacyInterfaceObject() {
}

ADBAPIHANDLE AdbLegacyInterfaceObject::CreateHandle() {
  SetLastError(ERROR_CALL_NOT_IMPLEMENTED);
  return NULL;
}

bool AdbLegacyInterfaceObject::CloseHandle() {
  SetLastError(ERROR_CALL_NOT_IMPLEMENTED);
  return false;
}

bool AdbLegacyInterfaceObject::GetSerialNumber(void* buffer,
                                               unsigned long* buffer_char_size,
                                               bool ansi) {
  SetLastError(ERROR_CALL_NOT_IMPLEMENTED);
  return false;
}

bool AdbLegacyInterfaceObject::GetEndpointInformation(
    UCHAR endpoint_index,
    AdbEndpointInformation* info) {
  SetLastError(ERROR_CALL_NOT_IMPLEMENTED);
  return false;
}

ADBAPIHANDLE AdbLegacyInterfaceObject::OpenEndpoint(
    UCHAR endpoint_index,
    AdbOpenAccessType access_type,
    AdbOpenSharingMode sharing_mode) {
  SetLastError(ERROR_CALL_NOT_IMPLEMENTED);
  return NULL;
}

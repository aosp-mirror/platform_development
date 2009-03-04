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

AdbEndpointObject::AdbEndpointObject(AdbInterfaceObject* parent_interf)
    : AdbIOObject(parent_interf, AdbObjectTypeEndpoint) {
}

AdbEndpointObject::~AdbEndpointObject() {
}

bool AdbEndpointObject::IsObjectOfType(AdbObjectType obj_type) const {
  return ((obj_type == AdbObjectTypeEndpoint) ||
          (obj_type == AdbObjectTypeIo));
}

bool AdbEndpointObject::GetEndpointInformation(AdbEndpointInformation* info) {
  if (!IsOpened() || !IsUsbOpened()) {
    SetLastError(ERROR_INVALID_HANDLE);
    return false;
  }

  // Send IOCTL
  DWORD ret_bytes = 0;
  BOOL ret = DeviceIoControl(usb_handle(),
                             ADB_IOCTL_GET_ENDPOINT_INFORMATION,
                             NULL, 0,
                             info, sizeof(AdbEndpointInformation),
                             &ret_bytes,
                             NULL);
  ATLASSERT(!ret || (sizeof(AdbEndpointInformation) == ret_bytes));

  return ret ? true : false;
}

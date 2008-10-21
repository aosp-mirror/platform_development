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
  This file consists of implementation of class AndroidUsbFileObject that
  encapsulates a common extension for all KMDF file object types.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_file_object.h"

#pragma data_seg()
#pragma code_seg("PAGE")

AndroidUsbFileObject::AndroidUsbFileObject(AndroidUsbFileObjectType fo_type,
                                           AndroidUsbDeviceObject* dev_obj,
                                           WDFFILEOBJECT wdf_fo)
    : AndroidUsbWdfObject(AndroidUsbWdfObjectTypeFile),
      file_type_(fo_type),
      device_object_(dev_obj) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(NULL != dev_obj);
  ASSERT(fo_type < AndroidUsbFileObjectTypeMax);
  ASSERT(NULL != wdf_fo);
  set_wdf_object(wdf_fo);
}

#pragma code_seg()

AndroidUsbFileObject::~AndroidUsbFileObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
}

#pragma code_seg("PAGE")

NTSTATUS AndroidUsbFileObject::Initialize() {
  ASSERT_IRQL_LOW();
  ASSERT(NULL != wdf_file());
  if (NULL == wdf_file())
    return STATUS_INTERNAL_ERROR;
  
  // Register context for this file object
  return InitializeContext();
}

#pragma code_seg()

void AndroidUsbFileObject::OnEvtIoRead(WDFREQUEST request,
                                       size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(WdfRequestGetFileObject(request) == wdf_file());
  // Complete zero reads with success
  if (0 == length) {
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, 0);
    return;
  }

  WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
}

void AndroidUsbFileObject::OnEvtIoWrite(WDFREQUEST request,
                                        size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(WdfRequestGetFileObject(request) == wdf_file());
  // Complete zero writes with success
  if (0 == length) {
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, 0);
    return;
  }

  WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
}

void AndroidUsbFileObject::OnEvtIoDeviceControl(WDFREQUEST request,
                                                size_t output_buf_len,
                                                size_t input_buf_len,
                                                ULONG ioctl_code) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(WdfRequestGetFileObject(request) == wdf_file());

  WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
}

#pragma data_seg()
#pragma code_seg()

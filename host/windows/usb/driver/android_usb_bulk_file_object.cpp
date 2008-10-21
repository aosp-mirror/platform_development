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
  This file consists of implementation of class AndroidUsbBulkPipeFileObject
  that encapsulates extension to a bulk pipe file objects.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_bulk_file_object.h"

#pragma data_seg()
#pragma code_seg("PAGE")

AndroidUsbBulkPipeFileObject::AndroidUsbBulkPipeFileObject(
    AndroidUsbDeviceObject* dev_obj,
    WDFFILEOBJECT wdf_fo,
    WDFUSBPIPE wdf_pipe_obj)
    : AndroidUsbPipeFileObject(dev_obj, wdf_fo, wdf_pipe_obj) {
  ASSERT_IRQL_PASSIVE();

#if DBG
  WDF_USB_PIPE_INFORMATION pipe_info;
  WDF_USB_PIPE_INFORMATION_INIT(&pipe_info);
  WdfUsbTargetPipeGetInformation(wdf_pipe_obj, &pipe_info);
  ASSERT(WdfUsbPipeTypeBulk == pipe_info.PipeType);
#endif  // DBG
}

#pragma code_seg()

AndroidUsbBulkPipeFileObject::~AndroidUsbBulkPipeFileObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
}

#pragma data_seg()
#pragma code_seg()

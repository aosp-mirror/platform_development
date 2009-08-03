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

#ifndef ANDROID_USB_BULK_PIPE_FILE_OBJECT_H__
#define ANDROID_USB_BULK_PIPE_FILE_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbBulkPipeFileObject
  that encapsulates extension to a bulk pipe file objects.
*/

#include "android_usb_pipe_file_object.h"

/** AndroidUsbBulkPipeFileObject class encapsulates extension to a KMDF file
  object that represent opened bulk pipe. Instances of this class must be
  allocated from NonPagedPool.
*/
class AndroidUsbBulkPipeFileObject : public AndroidUsbPipeFileObject {
 public:
  /** \brief Constructs the object.

    This method must be called at low IRQL.
    @param dev_obj[in] Our device object for which this file has been created
    @param wdf_fo[in] KMDF file object this extension wraps
    @param wdf_pipe_obj[in] KMDF pipe for this file
  */
  AndroidUsbBulkPipeFileObject(AndroidUsbDeviceObject* dev_obj,
                               WDFFILEOBJECT wdf_fo,
                               WDFUSBPIPE wdf_pipe_obj);

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
   virtual ~AndroidUsbBulkPipeFileObject();
};

#endif  // ANDROID_USB_BULK_PIPE_FILE_OBJECT_H__

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

#ifndef ANDROID_USB_DEVICE_FILE_OBJECT_H__
#define ANDROID_USB_DEVICE_FILE_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbDeviceFileObject that
  encapsulates an extension for a KMDF file object that represent opened
  device.
*/

#include "android_usb_file_object.h"

/** AndroidUsbDeviceFileObject class encapsulates an extension for a KMDF
  file object that represent opened device. Instances of this class must be
  allocated from NonPagedPool.
*/
class AndroidUsbDeviceFileObject : public AndroidUsbFileObject  {
 public:
  /** \brief Constructs the object.

    This method must be called at low IRQL.
    @param dev_obj[in] Our device object for which this file has been created
    @param wdf_fo[in] KMDF file object this extension wraps
  */
  AndroidUsbDeviceFileObject(AndroidUsbDeviceObject* dev_obj,
                             WDFFILEOBJECT wdf_fo);

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
   virtual ~AndroidUsbDeviceFileObject();

  /** \brief IOCTL event handler

    This method is called when a device control request comes to the file
    object this extension wraps. We override this method to handle the
    following IOCTL requests:
      1. ADB_CTL_GET_USB_DEVICE_DESCRIPTOR
      2. ADB_CTL_GET_USB_CONFIGURATION_DESCRIPTOR
      3. ADB_CTL_GET_USB_INTERFACE_DESCRIPTOR
      4. ADB_CTL_GET_ENDPOINT_INFORMATION
    This callback can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
    @param ioctl_code[in] The driver-defined or system-defined I/O control code
           that is associated with the request.
    @return Successful status or an appropriate error code
  */
  virtual void OnEvtIoDeviceControl(WDFREQUEST request,
                                    size_t output_buf_len,
                                    size_t input_buf_len,
                                    ULONG ioctl_code);
};

#endif  // ANDROID_USB_DEVICE_FILE_OBJECT_H__

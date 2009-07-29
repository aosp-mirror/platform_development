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
  This file consists of implementation of class AndroidUsbDeviceFileObject
  that encapsulates an extension for a KMDF file object that represent
  opened device.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_device_file_object.h"

#pragma data_seg()
#pragma code_seg("PAGE")

AndroidUsbDeviceFileObject::AndroidUsbDeviceFileObject(
    AndroidUsbDeviceObject* dev_obj,
    WDFFILEOBJECT wdf_fo)
    : AndroidUsbFileObject(AndroidUsbFileObjectTypeDevice, dev_obj, wdf_fo) {
  ASSERT_IRQL_PASSIVE();
}

#pragma code_seg()

AndroidUsbDeviceFileObject::~AndroidUsbDeviceFileObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
}

void AndroidUsbDeviceFileObject::OnEvtIoDeviceControl(WDFREQUEST request,
                                                      size_t output_buf_len,
                                                      size_t input_buf_len,
                                                      ULONG ioctl_code) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  switch (ioctl_code) {
    case ADB_IOCTL_GET_USB_DEVICE_DESCRIPTOR:
      device_object()->OnGetUsbDeviceDescriptorCtl(request, output_buf_len);
      break;

    case ADB_IOCTL_GET_USB_CONFIGURATION_DESCRIPTOR:
      device_object()->OnGetUsbConfigDescriptorCtl(request, output_buf_len);
      break;

    case ADB_IOCTL_GET_USB_INTERFACE_DESCRIPTOR:
      device_object()->OnGetUsbInterfaceDescriptorCtl(request, output_buf_len);
      break;

    case ADB_IOCTL_GET_ENDPOINT_INFORMATION:
      device_object()->OnGetEndpointInformationCtl(request,
                                                   input_buf_len,
                                                   output_buf_len);
      break;

    case ADB_IOCTL_GET_SERIAL_NUMBER:
      device_object()->OnGetSerialNumberCtl(request, output_buf_len);
      break;

    default:
      AndroidUsbFileObject::OnEvtIoDeviceControl(request,
                                                 output_buf_len,
                                                 input_buf_len,
                                                 ioctl_code);
      break;
  }
}

#pragma data_seg()
#pragma code_seg()

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

#ifndef ANDROID_USB_FILE_OBJECT_H__
#define ANDROID_USB_FILE_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbFileObject that
  encapsulates a common extension for all KMDF file object types.
*/

#include "android_usb_wdf_object.h"
#include "android_usb_device_object.h"

/** Enumerator AndroidUsbFileObjectType defines possible types for our file
  object extension.
*/
enum AndroidUsbFileObjectType {
  /// File extends device FO
  AndroidUsbFileObjectTypeDevice,

  // File extends a pipe FO
  AndroidUsbFileObjectTypePipe,

  AndroidUsbFileObjectTypeMax,
};

/** AndroidUsbFileObject class encapsulates a common extension for all KMDF
  file object types. Instances of this class must be allocated from
  NonPagedPool.
*/
class AndroidUsbFileObject : public AndroidUsbWdfObject {
 public:
  /** \brief Constructs the object.

    This method must be called at low IRQL.
    @param fo_type[in] Type of the file object that this object extends
    @param dev_obj[in] Our device object for which this file has been created
    @param wdf_fo[in] KMDF file object for this extension 
  */
  AndroidUsbFileObject(AndroidUsbFileObjectType fo_type,
                       AndroidUsbDeviceObject* dev_obj,
                       WDFFILEOBJECT wdf_fo);

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
   virtual ~AndroidUsbFileObject();

  /** \brief Initializes the object

    This method verifies that instance has been created and calls base class's
    InitializeContext method to register itself with the wrapped FO. All
    derived classes must call this method when initializing.
    This method must be called at low IRQL.
    @return STATUS_SUCCESS on success or an appropriate error code
  */
  virtual NTSTATUS Initialize();

  /** \brief Read event handler

    This method is called when a read request comes to the file object this
    class extends.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be read.
    @return Successful status or an appropriate error code
  */
  virtual void OnEvtIoRead(WDFREQUEST request, size_t length);

  /** \brief Write event handler

    This method is called when a write request comes to the file object this
    class extends.
    This callback can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be written.
    @return Successful status or an appropriate error code
  */
  virtual void OnEvtIoWrite(WDFREQUEST request, size_t length);

  /** \brief IOCTL event handler

    This method is called when a device control request comes to the file
    object this class extends.
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

 public:
  /// Gets KMDF file handle for this extension
  __forceinline WDFFILEOBJECT wdf_file() const {
    return reinterpret_cast<WDFFILEOBJECT>(wdf_object());
  }

  /// Gets device object that owns this file
  __forceinline AndroidUsbDeviceObject* device_object() const {
    return device_object_;
  }

  /// Gets type of the file object that this extension wraps
  __forceinline AndroidUsbFileObjectType file_type() const {
    return file_type_;
  }

  /// Gets WDF device handle for device that owns this file
  __forceinline WDFDEVICE wdf_device() const {
    ASSERT(NULL != device_object());
    return (NULL != device_object()) ? device_object()->wdf_device() :
                                       NULL;
  }

  /// Gets target (PDO) device handle for the device that owns this file
  __forceinline WDFUSBDEVICE wdf_target_device() const {
    ASSERT(NULL != device_object());
    return (NULL != device_object()) ? device_object()->wdf_target_device() :
                                       NULL;
  }

 protected:
  /// Device object that owns this file
  AndroidUsbDeviceObject*   device_object_;

  /// Type of the file object that this extension wraps
  AndroidUsbFileObjectType  file_type_;
};

/** \brief Gets file KMDF object extension for the given KMDF file object

  This method can be called at any IRQL
  @param wdf_fo[in] KMDF file handle describing file object
  @return Instance of AndroidUsbFileObject associated with this object or NULL
          if association is not found.
*/
__forceinline AndroidUsbFileObject* GetAndroidUsbFileObjectFromHandle(
    WDFFILEOBJECT wdf_fo) {
  AndroidUsbWdfObject* wdf_object_ext =
    GetAndroidUsbWdfObjectFromHandle(wdf_fo);
  ASSERT(NULL != wdf_object_ext);
  if (NULL != wdf_object_ext) {
    ASSERT(wdf_object_ext->Is(AndroidUsbWdfObjectTypeFile));
    if (wdf_object_ext->Is(AndroidUsbWdfObjectTypeFile))
      return reinterpret_cast<AndroidUsbFileObject*>(wdf_object_ext);
  }
  return NULL;
}

/** \brief Gets file KMDF file object extension for the given request

  This method can be called at any IRQL
  @param request[in] KMDF request object
  @return Instance of AndroidUsbFileObject associated with this request or NULL
          if association is not found.
*/
__forceinline AndroidUsbFileObject* GetAndroidUsbFileObjectForRequest(
    WDFREQUEST request) {
  return GetAndroidUsbFileObjectFromHandle(WdfRequestGetFileObject(request));
}

#endif  // ANDROID_USB_FILE_OBJECT_H__

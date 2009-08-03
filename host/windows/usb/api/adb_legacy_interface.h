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

#ifndef ANDROID_USB_API_ADB_LEGACY_INTERFACE_H__
#define ANDROID_USB_API_ADB_LEGACY_INTERFACE_H__
/** \file
  This file consists of declaration of class AdbLegacyInterfaceObject
  that encapsulates an interface on our USB device that is accessible
  via custom USB driver.
*/

#include "adb_interface.h"

/** \brief Encapsulates an interface on our USB device that is accessible
  via custom USB driver.
*/
class AdbLegacyInterfaceObject : public AdbInterfaceObject {
 public:
  /** \brief Constructs the object.

    @param[in] interf_name Name of the interface
  */
  explicit AdbLegacyInterfaceObject(const wchar_t* interf_name);

 protected:
  /** \brief Destructs the object.

   We hide destructor in order to prevent ourseves from accidentaly allocating
   instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbLegacyInterfaceObject();

  //
  // Virtual overrides
  //

 public:
  /** \brief Creates handle to this object.

    In this call a handle for this object is generated and object is added
    to the AdbObjectHandleMap. We override this method in order to initialize
    access to the custom driver.
    @return A handle to this object on success or NULL on an error.
            If NULL is returned GetLastError() provides extended error
            information. ERROR_GEN_FAILURE is set if an attempt was
            made to create already opened object.
  */
  virtual ADBAPIHANDLE CreateHandle();

  //
  // Abstract overrides
  //

 public:
  /** \brief Gets serial number for interface's device.

    @param[out] buffer Buffer for the serail number string. Can be NULL in
           which case buffer_char_size will contain number of characters
           required for the string.
    @param[in,out] buffer_char_size On the way in supplies size (in characters)
           of the buffer. On the way out, if method failed and GetLastError
           reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
           required for the name.
    @param[in] ansi If true the name will be returned as single character
           string. Otherwise name will be returned as wide character string.
    @return true on success, false on failure. If false is returned
            GetLastError() provides extended error information.
  */
  virtual bool GetSerialNumber(void* buffer,
                               unsigned long* buffer_char_size,
                               bool ansi);

  /** \brief Gets information about an endpoint on this interface.

    @param[in] endpoint_index Zero-based endpoint index. There are two
           shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
           and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide infor about
           (default?) bulk write and read endpoints respectively.
    @param[out] info Upon successful completion will have endpoint information.
    @return true on success, false on failure. If false is returned
            GetLastError() provides extended error information.
  */
  virtual bool GetEndpointInformation(UCHAR endpoint_index,
                                      AdbEndpointInformation* info);

  /** \brief Opens an endpoint on this interface.

    @param[in] endpoint_index Zero-based endpoint index. There are two
           shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
           and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide infor about
           (default?) bulk write and read endpoints respectively.
    @param[in] access_type Desired access type. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always read / write access.
    @param[in] sharing_mode Desired share mode. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always shared for read / write.
    @return Handle to the opened endpoint object or NULL on failure.
            If NULL is returned GetLastError() provides extended information
            about the error that occurred.
  */
  virtual ADBAPIHANDLE OpenEndpoint(UCHAR endpoint_index,
                                    AdbOpenAccessType access_type,
                                    AdbOpenSharingMode sharing_mode);

  //
  // Internal operations
  //

 protected:
  /** \brief Opens an endpoint on this interface.

    @param[in] endpoint_name Endpoint file name.
    @param[in] endpoint_id Endpoint (pipe) address on the device.
    @param[in] endpoint_index Zero-based endpoint index.
    @param[in] access_type Desired access type. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always read / write access.
    @param[in] sharing_mode Desired share mode. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always shared for read / write.
    @return Handle to the opened endpoint object or NULL on failure.
            If NULL is returned GetLastError() provides extended information
            about the error that occurred.
  */
  ADBAPIHANDLE OpenEndpoint(const wchar_t* endpoint_name,
                            UCHAR endpoint_id,
                            UCHAR endpoint_index,
                            AdbOpenAccessType access_type,
                            AdbOpenSharingMode sharing_mode);

  /** \brief Caches device descriptor for the USB device associated with
    this interface.

    This method is called from CreateHandle method to cache some interface
    information.
    @param[in] usb_device_handle Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbDeviceDescriptor(HANDLE usb_device_handle);

  /** \brief Caches descriptor for the selected USB device configuration.

    This method is called from CreateHandle method to cache some interface
    information.
    @param[in] usb_device_handle Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbConfigurationDescriptor(HANDLE usb_device_handle);

  /** \brief Caches descriptor for this interface.

    This method is called from CreateHandle method to cache some interface
    information.
    @param[in] usb_device_handle Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbInterfaceDescriptor(HANDLE usb_device_handle);

 protected:
  /// Index for the default bulk read endpoint
  UCHAR                         def_read_endpoint_;

  /// ID for the default bulk read endpoint
  UCHAR                         read_endpoint_id_;

  /// Index for the default bulk write endpoint
  UCHAR                         def_write_endpoint_;

  /// ID for the default bulk write endpoint
  UCHAR                         write_endpoint_id_;
};

#endif  // ANDROID_USB_API_ADB_LEGACY_INTERFACE_H__

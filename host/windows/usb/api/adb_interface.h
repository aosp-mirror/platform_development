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

#ifndef ANDROID_USB_API_ADB_INTERFACE_H__
#define ANDROID_USB_API_ADB_INTERFACE_H__
/** \file
  This file consists of declaration of class AdbInterfaceObject that
  encapsulates an interface on our USB device.
*/

#include "adb_object_handle.h"

/** Class AdbInterfaceObject encapsulates an interface on our USB device.
*/
class AdbInterfaceObject : public AdbObjectHandle {
 public:
  /** \brief Constructs the object
    
    @param interf_name[in] Name of the interface
  */
  explicit AdbInterfaceObject(const wchar_t* interf_name);

 protected:
  /** \brief Destructs the object.

   We hide destructor in order to prevent ourseves from accidentaly allocating
   instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbInterfaceObject();

 public:
  /** \brief Creates handle to this object

    In this call a handle for this object is generated and object is added
    to the AdbObjectHandleMap. We override this method in order to verify that
    interface indeed exists and gather device, interface and pipe properties.
    If this step succeeds then and only then AdbObjectHandle::CreateHandle
    will be called.
    @return A handle to this object on success or NULL on an error.
            If NULL is returned GetLastError() provides extended error
            information. ERROR_GEN_FAILURE is set if an attempt was
            made to create already opened object.
  */
  virtual ADBAPIHANDLE CreateHandle();

  /** \brief Gets interface device name.

    @param buffer[out] Buffer for the name. Can be NULL in which case
           buffer_char_size will contain number of characters required to fit
           the name.
    @param buffer_char_size[in/out] On the way in supplies size (in characters)
           of the buffer. On the way out if method failed and GetLastError
           reports ERROR_INSUFFICIENT_BUFFER will contain number of characters
           required to fit the name.
    @param ansi[in] If true the name will be returned as single character
           string. Otherwise name will be returned as wide character string.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetInterfaceName(void* buffer,
                        unsigned long* buffer_char_size,
                        bool ansi);

  /** \brief Gets serial number for interface's device.

    @param buffer[out] Buffer for the serail number string. Can be NULL in
           which case buffer_char_size will contain number of characters
           required for the string.
    @param buffer_char_size[in/out] On the way in supplies size (in characters)
           of the buffer. On the way out, if method failed and GetLastError
           reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
           required for the name.
    @param ansi[in] If 'true' the name will be returned as single character
           string. Otherwise name will be returned as wide character string.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetSerialNumber(void* buffer,
                       unsigned long* buffer_char_size,
                       bool ansi);

  /** \brief Gets device descriptor for the USB device associated with
    this interface.

    @param desc[out] Upon successful completion will have usb device
           descriptor.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetUsbDeviceDescriptor(USB_DEVICE_DESCRIPTOR* desc);

  /** \brief Gets descriptor for the selected USB device configuration.

    @param desc[out] Upon successful completion will have usb device
           configuration descriptor.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetUsbConfigurationDescriptor(USB_CONFIGURATION_DESCRIPTOR* desc);

  /** \brief Gets descriptor for this interface.

    @param desc[out] Upon successful completion will have interface
           descriptor.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetUsbInterfaceDescriptor(USB_INTERFACE_DESCRIPTOR* desc);

  /** \brief Gets information about an endpoint on this interface.

    @param endpoint_index[in] Zero-based endpoint index. There are two
           shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
           and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide infor about
           (default?) bulk write and read endpoints respectively.
    @param info[out] Upon successful completion will have endpoint information.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetEndpointInformation(UCHAR endpoint_index, AdbEndpointInformation* info);

  /** \brief Opens an endpoint on this interface.

    @param endpoint_index[in] Zero-based endpoint index. There are two
           shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
           and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide infor about
           (default?) bulk write and read endpoints respectively.
    @param access_type[in] Desired access type. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always read / write access.
    @param sharing_mode[in] Desired share mode. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always shared for read / write.
    @return Handle to the opened endpoint object or NULL on failure.
            If NULL is returned GetLastError() provides extended information
            about the error that occurred.
  */
  ADBAPIHANDLE OpenEndpoint(UCHAR endpoint_index,
                            AdbOpenAccessType access_type,
                            AdbOpenSharingMode sharing_mode);

  /** \brief Opens an endpoint on this interface.

    @param endpoint_name[in] Endpoint file name.
    @param access_type[in] Desired access type. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always read / write access.
    @param sharing_mode[in] Desired share mode. In the current implementation
           this parameter has no effect on the way endpoint is opened. It's
           always shared for read / write.
    @return Handle to the opened endpoint object or NULL on failure.
            If NULL is returned GetLastError() provides extended information
            about the error that occurred.
  */
  ADBAPIHANDLE OpenEndpoint(const wchar_t* endpoint_name,
                            AdbOpenAccessType access_type,
                            AdbOpenSharingMode sharing_mode);

 private:
  /** \brief Caches device descriptor for the USB device associated with
    this interface.

    This method is called from CreateHandle method to cache some interface
    information.
    @param usb_device_handle[in] Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbDeviceDescriptor(HANDLE usb_device_handle);

  /** \brief Caches descriptor for the selected USB device configuration.

    This method is called from CreateHandle method to cache some interface
    information.
    @param usb_device_handle[in] Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbConfigurationDescriptor(HANDLE usb_device_handle);

  /** \brief Caches descriptor for this interface.

    This method is called from CreateHandle method to cache some interface
    information.
    @param usb_device_handle[in] Handle to USB device.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool CacheUsbInterfaceDescriptor(HANDLE usb_device_handle);

 public:
  /// Gets name of the USB interface (device name) for this object
  const std::wstring& interface_name() const {
    return interface_name_;
  }

  // This is a helper for extracting object from the AdbObjectHandleMap
  static AdbObjectType Type() {
    return AdbObjectTypeInterface;
  }

  /// Gets cached usb device descriptor
  const USB_DEVICE_DESCRIPTOR* usb_device_descriptor() const {
    return &usb_device_descriptor_;
  }

  /// Gets cached usb configuration descriptor
  const USB_CONFIGURATION_DESCRIPTOR* usb_config_descriptor() const {
    return &usb_config_descriptor_;
  }

  /// Gets cached usb interface descriptor
  const USB_INTERFACE_DESCRIPTOR* usb_interface_descriptor() const {
    return &usb_interface_descriptor_;
  }

private:
  /// Name of the USB interface (device name) for this object
  std::wstring                  interface_name_;

  /// Cached usb device descriptor
  USB_DEVICE_DESCRIPTOR         usb_device_descriptor_;

  /// Cached usb configuration descriptor
  USB_CONFIGURATION_DESCRIPTOR  usb_config_descriptor_;

  /// Cached usb interface descriptor
  USB_INTERFACE_DESCRIPTOR      usb_interface_descriptor_;
};

#endif  // ANDROID_USB_API_ADB_INTERFACE_H__

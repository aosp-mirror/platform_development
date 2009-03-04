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

#ifndef ANDROID_USB_DEVICE_OBJECT_H__
#define ANDROID_USB_DEVICE_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbDeviceObject that
  encapsulates an extension for KMDF device (FDO) object.
*/

#include "android_usb_wdf_object.h"

// Forward declaration for file object extension
class AndroidUsbFileObject;

/** AndroidUsbDeviceObject class encapsulates an extension for KMDF FDO device
  object. Instances of this class must be allocated from NonPagedPool.
*/
class AndroidUsbDeviceObject : public AndroidUsbWdfObject {
 public:
  /** \brief Constructs the object.

    This method must be called at low IRQL.
  */
  AndroidUsbDeviceObject();

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
   ~AndroidUsbDeviceObject();

 public:
  /** \brief Creates and initializes FDO device object extension

    This method is called from driver's OnAddDevice method in response to
    AddDevice call from the PnP manager
    @param device_init[in] A pointer to a framework-allocated WDFDEVICE_INIT
           structure.
    @return If the routine succeeds, it returns STATUS_SUCCESS. Otherwise,
            it returns one of the error status values defined in ntstatus.h.
  */
  NTSTATUS CreateFDODevice(PWDFDEVICE_INIT device_init);

  /** \brief Resets target device

    When executing this method instance of this class may be deleted!
    This method must be called at PASSIVE IRQL.
    @return STATUS_SUCCESS or an appropriate error code
  */
  NTSTATUS ResetDevice();

 private:
  /** \name Device event handlers and callbacks
  */
  ///@{

  /** \brief Handler for PnP prepare hardware event

    This method performs any operations that are needed to make a device
    accessible to the driver. The framework calls this callback after the PnP
    manager has assigned hardware resources to the device and after the device
    has entered its uninitialized D0 state. This callback is called before
    calling the driver's EvtDeviceD0Entry callback function.
    This method is called at PASSIVE IRQL.
    @param resources_raw[in] A handle to a framework resource-list object that
           identifies the raw hardware resources that the PnP manager has
           assigned to the device.
    @param resources_translated[in] A handle to a framework resource-list
           object that identifies the translated hardware resources that the
           PnP manager has assigned to the device. 
    @return Successful status or an appropriate error code
  */
  NTSTATUS OnEvtDevicePrepareHardware(WDFCMRESLIST resources_raw,
                                      WDFCMRESLIST resources_translated);

  /** \brief Handler for PnP release hardware event

    This method performs operations that  that are needed when a device is no
    longer accessible. Framework calls the callback function if the device is
    being removed, or if the PnP manager is attempting to redistribute hardware
    resources. The framework calls the EvtDeviceReleaseHardware callback
    function after the driver's device has been shut off, the PnP manager has
    reclaimed the hardware resources that it assigned to the device, and the
    device is no longer accessible. (The PCI configuration state is still
    accessible.) Typically, a EvtDeviceReleaseHardware callback function unmaps
    memory that the driver's EvtDevicePrepareHardware callback function mapped.
    Usually, all other hardware shutdown operations should take place in the
    driver's EvtDeviceD0Exit callback function.
    This method is called at PASSIVE IRQL.
    @param wdf_device[in] A handle to a framework device object. 
    @param resources_translated[in] A handle to a framework resource-list
           object that identifies the translated hardware resources that the
           PnP manager has assigned to the device. 
    @return Successful status or an appropriate error code
  */
  NTSTATUS OnEvtDeviceReleaseHardware(WDFCMRESLIST resources_translated);

  /** \brief Handler for create file event (request)

    This method performs operations that are needed when an application
    requests access to an item within this device path (including device
    itself). This method is called synchronously, in the context of the
    user thread that opens the item.
    This method is called at PASSIVE IRQL.
    @param request[in] A handle to a framework request object that represents
           a file creation request.
    @param wdf_fo[in] A handle to a framework file object that describes a
           file that is being created with this request.
    @return Successful status or an appropriate error code
  */
  void OnEvtDeviceFileCreate(WDFREQUEST request, WDFFILEOBJECT wdf_fo);

  /** \brief Entry point for PnP prepare hardware event

    This callback performs any operations that are needed to make a device
    accessible to the driver. The framework calls this callback after the PnP
    manager has assigned hardware resources to the device and after the device
    has entered its uninitialized D0 state. This callback is called before
    calling the driver's EvtDeviceD0Entry callback function.
    This callback is called at PASSIVE IRQL.
    @param wdf_dev[in] A handle to a framework device object. 
    @param resources_raw[in] A handle to a framework resource-list object that
           identifies the raw hardware resources that the PnP manager has
           assigned to the device.
    @param resources_translated[in] A handle to a framework resource-list
           object that identifies the translated hardware resources that the
           PnP manager has assigned to the device. 
    @return Successful status or an appropriate error code
  */
  static NTSTATUS EvtDevicePrepareHardwareEntry(WDFDEVICE wdf_dev,
                                                WDFCMRESLIST resources_raw,
                                                WDFCMRESLIST resources_translated);

  /** \brief Entry point for PnP release hardware event

    This callback performs operations that  that are needed when a device is no
    longer accessible. Framework calls the callback function if the device is
    being removed, or if the PnP manager is attempting to redistribute hardware
    resources. The framework calls the EvtDeviceReleaseHardware callback
    function after the driver's device has been shut off, the PnP manager has
    reclaimed the hardware resources that it assigned to the device, and the
    device is no longer accessible. (The PCI configuration state is still
    accessible.) Typically, a EvtDeviceReleaseHardware callback function unmaps
    memory that the driver's EvtDevicePrepareHardware callback function mapped.
    Usually, all other hardware shutdown operations should take place in the
    driver's EvtDeviceD0Exit callback function.
    This callback is called at PASSIVE IRQL.
    @param wdf_dev[in] A handle to a framework device object. 
    @param resources_translated[in] A handle to a framework resource-list
           object that identifies the translated hardware resources that the
           PnP manager has assigned to the device. 
    @return Successful status or an appropriate error code
  */
  static NTSTATUS EvtDeviceReleaseHardwareEntry(WDFDEVICE wdf_dev,
                                                WDFCMRESLIST resources_translated);

  /** \brief Entry point for create file event (request)

    This callback performs operations that that are needed when an application
    requests access to a device. The framework calls a driver's
    EvtDeviceFileCreate callback function when a user application or another
    driver opens the device (or file on this device) to perform an I/O
    operation, such as reading or writing a file. This callback function is
    called synchronously, in the context of the user thread that opens the
    device.
    This callback is called at PASSIVE IRQL.
    @param wdf_dev[in] A handle to a framework device object. 
    @param request[in] A handle to a framework request object that represents
           a file creation request.
    @param wdf_fo[in] A handle to a framework file object that describes a
           file that is being created with this request.
    @return Successful status or an appropriate error code
  */
  static void EvtDeviceFileCreateEntry(WDFDEVICE wdf_dev,
                                       WDFREQUEST request,
                                       WDFFILEOBJECT wdf_fo);

  ///@}

 private:
  /** \name I/O request event handlers and callbacks
  */
  ///@{

  /** \brief Read event handler

    This method is called when a read request comes to a file object opened
    on this device.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be read.
  */
  void OnEvtIoRead(WDFREQUEST request, size_t length);

  /** \brief Write event handler

    This method is called when a write request comes to a file object opened
    on this device.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be written.
  */
  void OnEvtIoWrite(WDFREQUEST request, size_t length);

  /** \brief IOCTL event handler

    This method is called when a device control request comes to a file object
    opened on this device.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
    @param ioctl_code[in] The driver-defined or system-defined I/O control code
           that is associated with the request.
  */
  void OnEvtIoDeviceControl(WDFREQUEST request,
                            size_t output_buf_len,
                            size_t input_buf_len,
                            ULONG ioctl_code);

  /** \brief Entry point for read event

    This callback is called when a read request comes to a file object opened
    on this device.
    This callback can be called IRQL <= DISPATCH_LEVEL.
    @param queue[in] A handle to the framework queue object that is associated
           with the I/O request.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be read.
  */
  static void EvtIoReadEntry(WDFQUEUE queue,
                             WDFREQUEST request,
                             size_t length);

  /** \brief Entry point for write event

    This callback is called when a write request comes to a file object opened
    on this device.
    This callback can be called IRQL <= DISPATCH_LEVEL.
    @param queue[in] A handle to the framework queue object that is associated
           with the I/O request.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be written.
  */
  static void EvtIoWriteEntry(WDFQUEUE queue,
                              WDFREQUEST request,
                              size_t length);

  /** \brief Entry point for device IOCTL event

    This callback is called when a device control request comes to a file
    object opened on this device.
    This callback can be called IRQL <= DISPATCH_LEVEL.
    @param queue[in] A handle to the framework queue object that is associated
           with the I/O request.
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
    @param ioctl_code[in] The driver-defined or system-defined I/O control code
           that is associated with the request.
  */
  static void EvtIoDeviceControlEntry(WDFQUEUE queue,
                                      WDFREQUEST request,
                                      size_t output_buf_len,
                                      size_t input_buf_len,
                                      ULONG ioctl_code);

  ///@}

 public:
  /** \name Device level I/O request handlers
  */
  ///@{

  /** \brief Gets USB device descriptor

    This method can be called at IRQL <= DISPATCH_LEVEL
    @param request[in] A handle to a framework request object for this IOCTL.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  void OnGetUsbDeviceDescriptorCtl(WDFREQUEST request, size_t output_buf_len);

  /** \brief Gets USB configuration descriptor for the selected configuration.

    This method can be called at IRQL <= DISPATCH_LEVEL
    @param request[in] A handle to a framework request object for this IOCTL.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  void OnGetUsbConfigDescriptorCtl(WDFREQUEST request, size_t output_buf_len);

  /** \brief Gets USB configuration descriptor for the selected interface.

    This method can be called at IRQL <= DISPATCH_LEVEL
    @param request[in] A handle to a framework request object for this IOCTL.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  void OnGetUsbInterfaceDescriptorCtl(WDFREQUEST request, size_t output_buf_len);

  /** \brief Gets information about an endpoint.

    This method can be called at IRQL <= DISPATCH_LEVEL
    @param request[in] A handle to a framework request object for this IOCTL.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  void OnGetEndpointInformationCtl(WDFREQUEST request,
                                   size_t input_buf_len,
                                   size_t output_buf_len);

  /** \brief Gets device serial number.

    Serial number is returned in form of zero-terminated string that in the
    output buffer. This method must be called at low IRQL.
    @param request[in] A handle to a framework request object for this IOCTL.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  void OnGetSerialNumberCtl(WDFREQUEST request, size_t output_buf_len);

  ///@}

 private:
  /** \name Internal methods
  */
  ///@{

  /** \brief Creates default request queue for this device.

    In KMDF all I/O requests are coming through the queue object. So, in order
    to enable our device to receive I/O requests we must create a queue for it.
    This method is called at PASSIVE IRQL.
    @return STATUS_SUCCESS or an appropriate error code.
  */
  NTSTATUS CreateDefaultQueue();

  /** \brief Configures our device.

    This method is called from the prepare hardware handler after underlying
    FDO device has been created.
    This method is called at PASSSIVE IRQL.
    @return STATUS_SUCCESS or an appropriate error code.
  */
  NTSTATUS ConfigureDevice();

  /** \brief Selects interfaces on our device.

    This method is called from the prepare hardware handler after underlying
    FDO device has been created and configured.
    This method is called at PASSSIVE IRQL.
    @return STATUS_SUCCESS or an appropriate error code.
  */
  NTSTATUS SelectInterfaces();
  
  /** \brief Gets pipe index from a file name

    This method is called from OnEvtDeviceFileCreate to determine index of
    the pipe this file is addressing.
    This method is called at PASSIVE IRQL.
    @param file_path[in] Path to the file that being opened.
    @return Pipe index or INVALID_UCHAR if index cannot be calculated.
  */
  UCHAR GetPipeIndexFromFileName(PUNICODE_STRING file_path);

  /** \brief Creates file object extension for a pipe

    This method is called from OnEvtDeviceFileCreate to create an appropriate
    file object extension for a particular pipe type.
    This method is called at PASSIVE IRQL.
    @param wdf_fo[in] KMDF file to extend.
    @param wdf_pipe_obj[in] KMDF pipe for this extension
    @param pipe_info[in] Pipe information
    @param wdf_file_ext[out] Upon successfull completion will receive instance
           of the extension.
    @return STATUS_SUCCESS or an appropriate error code
  */
  NTSTATUS CreatePipeFileObjectExt(WDFFILEOBJECT wdf_fo,
                                   WDFUSBPIPE wdf_pipe_obj,
                                   const WDF_USB_PIPE_INFORMATION* pipe_info,
                                   AndroidUsbFileObject** wdf_file_ext);

  ///@}

 private:
  /** \name Debugging support
  */
  ///@{

#if DBG
  /// Prints USB_DEVICE_DESCRIPTOR to debug output
  void PrintUsbDeviceDescriptor(const USB_DEVICE_DESCRIPTOR* desc);

  /// Prints WDF_USB_DEVICE_INFORMATION to debug output
  void PrintUsbTargedDeviceInformation(const WDF_USB_DEVICE_INFORMATION* info);

  /// Prints USB_CONFIGURATION_DESCRIPTOR to debug output
  void PrintConfigDescriptor(const USB_CONFIGURATION_DESCRIPTOR* desc,
                             ULONG size);

  /// Prints WDF_USB_DEVICE_SELECT_CONFIG_PARAMS to debug output
  void PrintSelectedConfig(const WDF_USB_DEVICE_SELECT_CONFIG_PARAMS* config);

  /// Prints USB_INTERFACE_DESCRIPTOR to debug output
  void PrintInterfaceDescriptor(const USB_INTERFACE_DESCRIPTOR* desc);

  /// Prints WDF_USB_PIPE_INFORMATION to debug output
  void PrintPipeInformation(const WDF_USB_PIPE_INFORMATION* info,
                            UCHAR pipe_index);

#endif  // DBG

  ///@}

 public:
  /// Gets WDF device handle for this device
  __forceinline WDFDEVICE wdf_device() const {
    return reinterpret_cast<WDFDEVICE>(wdf_object());
  }

  /// Gets target USB device descriptor
  __forceinline const USB_DEVICE_DESCRIPTOR* usb_device_descriptor() const {
    return &usb_device_descriptor_;
  }

  /// Gets target USB device information
  __forceinline const WDF_USB_DEVICE_INFORMATION* usb_device_info() const {
    return &usb_device_info_;
  }

  /// Gets selected interface descriptor
  __forceinline const USB_INTERFACE_DESCRIPTOR* interface_descriptor() const {
    return &interface_descriptor_;
  }

  /// Gets target (PDO) device handle
  __forceinline WDFUSBDEVICE wdf_target_device() const {
    return wdf_target_device_;
  }

  /// Checks if target device has been created
  __forceinline bool IsTaretDeviceCreated() const {
    return (NULL != wdf_target_device());
  }

  /// Gets USB configuration descriptor
  __forceinline const USB_CONFIGURATION_DESCRIPTOR* configuration_descriptor() const {
    return configuration_descriptor_;
  }

  /// Checks if device has been configured
  __forceinline bool IsDeviceConfigured() const {
    return (NULL != configuration_descriptor());
  }

  /// Gets number of interfaces for this device
  __forceinline UCHAR GetInterfaceCount() const {
    ASSERT(IsDeviceConfigured());
    return IsDeviceConfigured() ? configuration_descriptor()->bNumInterfaces : 0;
  }

  /// Checks if this is "single interface" device
  __forceinline bool IsSingleInterfaceDevice() const {
    return (1 == GetInterfaceCount());
  }

  /// Gets USB interface selected on this device
  __forceinline WDFUSBINTERFACE wdf_usb_interface() const {
    return wdf_usb_interface_;
  }

  /// Checks if an interface has been selected on this device
  __forceinline bool IsInterfaceSelected() const {
    return (NULL != wdf_usb_interface());
  }

  /// Gets number of pipes configured on this device
  __forceinline UCHAR configured_pipes_num() const {
    return configured_pipes_num_;
  }

  /// Gets index of the bulk read pipe
  __forceinline UCHAR bulk_read_pipe_index() const {
    return bulk_read_pipe_index_;
  }

  /// Gets index of the bulk write pipe
  __forceinline UCHAR bulk_write_pipe_index() const {
    return bulk_write_pipe_index_;
  }

  /// Checks if this is a high speed device
  __forceinline bool IsHighSpeed() const {
    return (0 != (usb_device_info()->Traits & WDF_USB_DEVICE_TRAIT_AT_HIGH_SPEED));
  }

  /// Checks if bulk read pipe index is known
  __forceinline bool IsBulkReadPipeKnown() const {
    return (INVALID_UCHAR != bulk_read_pipe_index());
  }

  /// Checks if bulk write pipe index is known
  __forceinline bool IsBulkWritePipeKnown() const {
    return (INVALID_UCHAR != bulk_write_pipe_index());
  }

  /// Gets device serial number string. Note that string may be
  /// not zero-terminated. Use serial_number_len() to get actual
  /// length of this string.
  __forceinline const WCHAR* serial_number() const {
    ASSERT(NULL != serial_number_handle_);
    return (NULL != serial_number_handle_) ?
      reinterpret_cast<const WCHAR*>
        (WdfMemoryGetBuffer(serial_number_handle_, NULL)) :
      NULL;
  }

  /// Gets length (in bytes) of device serial number string
  __forceinline USHORT serial_number_char_len() const {
    return serial_number_char_len_;
  }

  /// Gets length (in bytes) of device serial number string
  __forceinline USHORT serial_number_byte_len() const {
    return serial_number_char_len() * sizeof(WCHAR);
  }

 protected:
  /// Target USB device descriptor
  USB_DEVICE_DESCRIPTOR         usb_device_descriptor_;

  /// Target USB device information
  WDF_USB_DEVICE_INFORMATION    usb_device_info_;

  /// Selected interface descriptor
  USB_INTERFACE_DESCRIPTOR      interface_descriptor_;

  /// USB configuration descriptor
  PUSB_CONFIGURATION_DESCRIPTOR configuration_descriptor_;

  /// Target (PDO?) device handle
  WDFUSBDEVICE                  wdf_target_device_;

  /// USB interface selected on this device
  WDFUSBINTERFACE               wdf_usb_interface_;

  /// Device serial number
  WDFMEMORY                     serial_number_handle_;

  /// Device serial number string length
  USHORT                        serial_number_char_len_;

  /// Number of pipes configured on this device
  UCHAR                         configured_pipes_num_;

  /// Index of the bulk read pipe
  UCHAR                         bulk_read_pipe_index_;

  /// Index of the bulk write pipe
  UCHAR                         bulk_write_pipe_index_;
};

/** \brief Gets device KMDF object extension for the given KMDF object

  @param wdf_dev[in] KMDF handle describing device object
  @return Instance of AndroidUsbDeviceObject associated with KMDF object or
          NULL if association is not found.
*/
__forceinline AndroidUsbDeviceObject* GetAndroidUsbDeviceObjectFromHandle(
    WDFDEVICE wdf_dev) {
  AndroidUsbWdfObject* wdf_object_ext =
    GetAndroidUsbWdfObjectFromHandle(wdf_dev);
  ASSERT((NULL != wdf_object_ext) &&
         wdf_object_ext->Is(AndroidUsbWdfObjectTypeDevice));
  if ((NULL != wdf_object_ext) &&
      wdf_object_ext->Is(AndroidUsbWdfObjectTypeDevice)) {
    return reinterpret_cast<AndroidUsbDeviceObject*>(wdf_object_ext);
  }
  return NULL;
}

#endif  // ANDROID_USB_DEVICE_OBJECT_H__

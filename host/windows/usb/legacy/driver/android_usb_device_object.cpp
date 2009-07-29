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
  This file consists of implementation of class AndroidUsbDeviceObject that
  encapsulates an extension for KMDF device (FDO) object.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_device_object.h"
#include "android_usb_file_object.h"
#include "android_usb_device_file_object.h"
#include "android_usb_pipe_file_object.h"
#include "android_usb_bulk_file_object.h"
#include "android_usb_interrupt_file_object.h"

#pragma data_seg()

/// Buffer for bulk read pipe name
const WCHAR bulk_read_pipe_str[] = L"\\" DEVICE_BULK_READ_PIPE_NAME;

/// Unicode string for bulk read pipe name
UNICODE_STRING bulk_read_pipe_name = {
  sizeof(bulk_read_pipe_str) - sizeof(WCHAR),
  sizeof(bulk_read_pipe_str) - sizeof(WCHAR),
  const_cast<PWSTR>(bulk_read_pipe_str)
};

/// Buffer for bulk write pipe name
const WCHAR bulk_write_pipe_str[] = L"\\" DEVICE_BULK_WRITE_PIPE_NAME;

/// Unicode string for bulk write pipe name
UNICODE_STRING bulk_write_pipe_name = {
  sizeof(bulk_write_pipe_str) - sizeof(WCHAR),
  sizeof(bulk_write_pipe_str) - sizeof(WCHAR),
  const_cast<PWSTR>(bulk_write_pipe_str)
};

/// Buffer for an index-based pipe name prefix
const WCHAR index_pipe_prefix_str[] = L"\\" DEVICE_PIPE_NAME_PREFIX;

/// Unicode string for index-based pipe name prefix
UNICODE_STRING index_pipe_prefix = {
  sizeof(index_pipe_prefix_str) - sizeof(WCHAR),
  sizeof(index_pipe_prefix_str) - sizeof(WCHAR),
  const_cast<PWSTR>(index_pipe_prefix_str)
};

/// GUID that sets class ID for our device
const GUID android_guid = ANDROID_USB_CLASS_ID;

#pragma code_seg("PAGE")

AndroidUsbDeviceObject::AndroidUsbDeviceObject()
    : AndroidUsbWdfObject(AndroidUsbWdfObjectTypeDevice),
      wdf_target_device_(NULL),
      wdf_usb_interface_(NULL),
      serial_number_handle_(NULL),
      serial_number_char_len_(0),
      configured_pipes_num_(0),
      bulk_read_pipe_index_(INVALID_UCHAR),
      bulk_write_pipe_index_(INVALID_UCHAR),
      configuration_descriptor_(NULL) {
  ASSERT_IRQL_PASSIVE();
}

#pragma code_seg()

AndroidUsbDeviceObject::~AndroidUsbDeviceObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  if (NULL != serial_number_handle_)
    WdfObjectDelete(serial_number_handle_);
}

#pragma code_seg("PAGE")

NTSTATUS AndroidUsbDeviceObject::CreateFDODevice(PWDFDEVICE_INIT device_init) {
  ASSERT_IRQL_PASSIVE();

  ASSERT(!IsTaretDeviceCreated());
  if (IsTaretDeviceCreated())
    return STATUS_INTERNAL_ERROR;

  // Initialize our object attributes first
  WDF_OBJECT_ATTRIBUTES device_attr;
  NTSTATUS status = InitObjectAttributes(&device_attr, NULL);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Initialize the pnp_power_callbacks structure.  Callback events for PnP
  // and Power are specified here. If we don't supply any callbacks, the
  // KMDF will take appropriate default actions for an FDO device object.
  // EvtDevicePrepareHardware and EvtDeviceReleaseHardware are major entry
  // points for initializing / cleaning up our device. Probably, we can leave
  // the rest to the framework.
  WDF_PNPPOWER_EVENT_CALLBACKS pnp_power_callbacks;
  WDF_PNPPOWER_EVENT_CALLBACKS_INIT(&pnp_power_callbacks);
  pnp_power_callbacks.EvtDevicePrepareHardware =
    EvtDevicePrepareHardwareEntry;
  pnp_power_callbacks.EvtDeviceReleaseHardware =
    EvtDeviceReleaseHardwareEntry;
  WdfDeviceInitSetPnpPowerEventCallbacks(device_init, &pnp_power_callbacks);

  // Initialize the request attributes to specify the context size and type
  // for every request created by framework for this device.
  WDF_OBJECT_ATTRIBUTES request_attr;
  WDF_OBJECT_ATTRIBUTES_INIT(&request_attr);
  WDF_OBJECT_ATTRIBUTES_SET_CONTEXT_TYPE(&request_attr, AndroidUsbWdfRequestContext);
  WdfDeviceInitSetRequestAttributes(device_init, &request_attr);

  // Initialize WDF_FILEOBJECT_CONFIG_INIT struct to tell the KMDF that we are
  // interested in handling Create requests that get genereated when an
  // application or another kernel component opens a handle through the device.
  // We are not interested in receiving cleanup / close IRPs at this point.
  WDF_FILEOBJECT_CONFIG file_config;
  WDF_OBJECT_ATTRIBUTES file_attr;
  WDF_FILEOBJECT_CONFIG_INIT(&file_config,
                             EvtDeviceFileCreateEntry,
                             WDF_NO_EVENT_CALLBACK,
                             WDF_NO_EVENT_CALLBACK);
  WDF_OBJECT_ATTRIBUTES_INIT(&file_attr);
  WDF_OBJECT_ATTRIBUTES_SET_CONTEXT_TYPE(&file_attr,
                                         AndroidUsbWdfObjectContext);
  file_attr.EvtCleanupCallback = AndroidUsbWdfObject::EvtCleanupCallbackEntry;
  file_attr.EvtDestroyCallback = AndroidUsbWdfObject::EvtDestroyCallbackEntry;
  // We will provide our own synchronization for file access
  file_attr.SynchronizationScope = WdfSynchronizationScopeNone;
  WdfDeviceInitSetFileObjectConfig(device_init, &file_config, &file_attr);

  // I/O type is buffered by default. It could be very inefficient if we have
  // large reads / writes through our device.
  WdfDeviceInitSetIoType(device_init, WdfDeviceIoDirect);

  // DeviceInit is completely initialized. So call the framework
  // to create the device and attach it to the lower stack.
  WDFDEVICE wdf_dev = NULL;
  status = WdfDeviceCreate(&device_init, &device_attr, &wdf_dev);
  ASSERT(NT_SUCCESS(status) && (NULL != wdf_dev));
  if (!NT_SUCCESS(status))
    return status;

  // Save handle to the created device
  set_wdf_object(wdf_dev);

  // Tell the framework to set the SurpriseRemovalOK in the DeviceCaps so
  // that we don't get the popup in usermode (on Win2K) when we surprise
  // remove the device.
  WDF_DEVICE_PNP_CAPABILITIES pnp_caps;
  WDF_DEVICE_PNP_CAPABILITIES_INIT(&pnp_caps);
  pnp_caps.SurpriseRemovalOK = WdfTrue;
  WdfDeviceSetPnpCapabilities(wdf_device(), &pnp_caps);

  // Create our default queue object for this device to start receiving I/O
  status = CreateDefaultQueue();
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Register a device interface so that app can find our device and talk to it.
  status = WdfDeviceCreateDeviceInterface(wdf_device(), &android_guid, NULL);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Initialize our extension to that device. We will do this at the very end
  // so we know that we successfully passed entire device create chain when
  // we are called with other callbacks to that device.
  status = InitializeContext();
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  return STATUS_SUCCESS;
}

NTSTATUS AndroidUsbDeviceObject::ResetDevice() {
  ASSERT_IRQL_PASSIVE();

  if (!IsTaretDeviceCreated())
    return STATUS_SUCCESS;

  // Reset the device
  NTSTATUS status =
    status = WdfUsbTargetDeviceResetPortSynchronously(wdf_target_device());

  // !!!!! Note that after the call to WdfUsbTargetDeviceResetPortSynchronously
  // this object may be no longer valid !!!!!

  if (!NT_SUCCESS(status))
    GoogleDbgPrint("\n!!!!! AndroidUsbDeviceObject::ResetDevice failed %X", status);

  return status;
}

NTSTATUS AndroidUsbDeviceObject::OnEvtDevicePrepareHardware(
    WDFCMRESLIST resources_raw,
    WDFCMRESLIST resources_translated) {
  ASSERT_IRQL_PASSIVE();

  // Create a USB device handle so that we can communicate with the underlying
  // USB stack. The wdf_target_device_ handle is used to query, configure, and
  // manage all aspects of the USB device. These aspects include device
  // properties, bus properties, and I/O creation and synchronization. This
  // call gets the device and configuration descriptors and stores them in
  // wdf_target_device_ object.
  NTSTATUS status = WdfUsbTargetDeviceCreate(wdf_device(),
                                             WDF_NO_OBJECT_ATTRIBUTES,
                                             &wdf_target_device_);
  ASSERT(NT_SUCCESS(status) && (NULL != wdf_target_device_));
  if (!NT_SUCCESS(status))
    return status;

  // Retrieve USBD version information, port driver capabilites and device
  // capabilites such as speed, power, etc.
  WDF_USB_DEVICE_INFORMATION_INIT(&usb_device_info_);
  status = WdfUsbTargetDeviceRetrieveInformation(wdf_target_device(),
                                                 &usb_device_info_);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  WdfUsbTargetDeviceGetDeviceDescriptor(wdf_target_device(),
                                        &usb_device_descriptor_);
#if DBG
  PrintUsbTargedDeviceInformation(usb_device_info());
  PrintUsbDeviceDescriptor(&usb_device_descriptor_);
#endif  // DBG

  // Save device serial number
  status =
    WdfUsbTargetDeviceAllocAndQueryString(wdf_target_device(),
                                          WDF_NO_OBJECT_ATTRIBUTES,
                                          &serial_number_handle_,
                                          &serial_number_char_len_,
                                          usb_device_descriptor_.iSerialNumber,
                                          0x0409);  // English (US)
  if (!NT_SUCCESS(status))
    return status;

#if DBG
  UNICODE_STRING ser_num;
  ser_num.Length = serial_number_byte_len();
  ser_num.MaximumLength = ser_num.Length;
  ser_num.Buffer = const_cast<WCHAR*>
    (serial_number());
  GoogleDbgPrint("\n*** Device serial number %wZ", &ser_num);
#endif  // DBG

  // Configure our device now
  status = ConfigureDevice();
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Select device interfaces
  status = SelectInterfaces();
  if (!NT_SUCCESS(status))
    return status;

  return status;
}

NTSTATUS AndroidUsbDeviceObject::OnEvtDeviceReleaseHardware(
    WDFCMRESLIST resources_translated) {
  ASSERT_IRQL_PASSIVE();

  // It's possible that Preparehardware failed half way thru. So make
  // sure the target device exists.
  if (!IsTaretDeviceCreated())
    return STATUS_SUCCESS;

  // Cancel all the currently queued I/O. This is better than sending an
  // explicit USB abort request down because release hardware gets
  // called even when the device surprise-removed.
  WdfIoTargetStop(WdfUsbTargetDeviceGetIoTarget(wdf_target_device()),
                  WdfIoTargetCancelSentIo);

  // Unselect all selected configurations
  WDF_USB_DEVICE_SELECT_CONFIG_PARAMS config_params;
  WDF_USB_DEVICE_SELECT_CONFIG_PARAMS_INIT_DECONFIG(&config_params);

  NTSTATUS status = WdfUsbTargetDeviceSelectConfig(wdf_target_device(),
                                                   WDF_NO_OBJECT_ATTRIBUTES,
                                                   &config_params);
  ASSERT(NT_SUCCESS(status) || (STATUS_DEVICE_NOT_CONNECTED == status));
  return status;
}

void AndroidUsbDeviceObject::OnEvtDeviceFileCreate(WDFREQUEST request,
                                                   WDFFILEOBJECT wdf_fo) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(IsInterfaceSelected());
  if (!IsInterfaceSelected()) {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_STATE);
    return;
  }

  PUNICODE_STRING file_name = WdfFileObjectGetFileName(wdf_fo);
  ASSERT(NULL != file_name);
  if (NULL == file_name) {
    WdfRequestComplete(request, STATUS_OBJECT_NAME_INVALID);
    return;
  }

  WDFUSBPIPE wdf_pipe_obj = NULL;
  WDF_USB_PIPE_INFORMATION pipe_info;

  // TODO: Share / access check here?

  // Lets see if this is a device open
  if (0 != file_name->Length) {
    // This is a pipe open. Lets retrieve pipe index from the name
    UCHAR pipe_index = GetPipeIndexFromFileName(file_name);
    if (INVALID_UCHAR == pipe_index) {
      GoogleDbgPrint("\n!!!!! There is no pipe index for file %wZ", file_name);
      WdfRequestComplete(request, STATUS_OBJECT_NAME_INVALID);
      return;
    }

    // Make sure that pipe index doesn't exceed number of pipes
    if (pipe_index >= configured_pipes_num()) {
      WdfRequestComplete(request, STATUS_OBJECT_NAME_NOT_FOUND);
      return;
    }

    // Retrieve the pipe along with the pipe info
    WDF_USB_PIPE_INFORMATION_INIT(&pipe_info);
    wdf_pipe_obj = WdfUsbInterfaceGetConfiguredPipe(wdf_usb_interface(),
                                                    pipe_index,
                                                    &pipe_info);
    if (NULL == wdf_pipe_obj) {
      GoogleDbgPrint("\n!!!!! There is no pipe for index %u for file %wZ",
                     pipe_index, file_name);
      WdfRequestComplete(request, STATUS_OBJECT_NAME_NOT_FOUND);
      return;
    }
  }

  // If we're here this must be either device open or pipe open
  ASSERT((NULL != wdf_pipe_obj) || (0 == file_name->Length));

  // Create our file object extension for this file
  AndroidUsbFileObject* wdf_file_ext = NULL;
  NTSTATUS status;

  if (0 == file_name->Length) {
    // This is a device FO. Create wrapper for device FO
    ASSERT(NULL == wdf_pipe_obj);
    wdf_file_ext = new(NonPagedPool, GANDR_POOL_TAG_DEVICE_FO)
      AndroidUsbDeviceFileObject(this, wdf_fo);
    ASSERT(NULL != wdf_file_ext);
    if (NULL == wdf_file_ext) {
      WdfRequestComplete(request, STATUS_INSUFFICIENT_RESOURCES);
      return;
    }

    // Initialize extension
    status = wdf_file_ext->Initialize();
    if (!NT_SUCCESS(status)) {
      delete wdf_file_ext;
      WdfRequestComplete(request, status);
      return;
    }
  } else {
    // This is a pipe file. Create and initialize appropriate extension for it.
    status =
      CreatePipeFileObjectExt(wdf_fo, wdf_pipe_obj, &pipe_info, &wdf_file_ext);
    ASSERT((NULL != wdf_file_ext) || !NT_SUCCESS(status));
    if (!NT_SUCCESS(status)) {
      WdfRequestComplete(request, status);
      return;
    }
  }
  ASSERT(GetAndroidUsbFileObjectFromHandle(wdf_fo) == wdf_file_ext);
  WdfRequestComplete(request, STATUS_SUCCESS);
}

NTSTATUS AndroidUsbDeviceObject::EvtDevicePrepareHardwareEntry(
    WDFDEVICE wdf_dev,
    WDFCMRESLIST resources_raw,
    WDFCMRESLIST resources_translated) {
  ASSERT_IRQL_PASSIVE();

  // Get our wrapper for the device and redirect event to its handler
  AndroidUsbDeviceObject* wdf_device_ext =
    GetAndroidUsbDeviceObjectFromHandle(wdf_dev);
  ASSERT(NULL != wdf_device_ext);
  return (NULL != wdf_device_ext) ?
    wdf_device_ext->OnEvtDevicePrepareHardware(resources_raw,
                                               resources_translated) :
    STATUS_INVALID_DEVICE_REQUEST;
}

NTSTATUS AndroidUsbDeviceObject::EvtDeviceReleaseHardwareEntry(
    WDFDEVICE wdf_dev,
    WDFCMRESLIST resources_translated) {
  ASSERT_IRQL_PASSIVE();

  // Get our wrapper for the device and redirect event to its handler
  AndroidUsbDeviceObject* wdf_device_ext =
    GetAndroidUsbDeviceObjectFromHandle(wdf_dev);
  ASSERT(NULL != wdf_device_ext);
  return (NULL != wdf_device_ext) ?
    wdf_device_ext->OnEvtDeviceReleaseHardware(resources_translated) :
    STATUS_INVALID_DEVICE_REQUEST;
}

void AndroidUsbDeviceObject::EvtDeviceFileCreateEntry(
    WDFDEVICE wdf_dev,
    WDFREQUEST request,
    WDFFILEOBJECT wdf_fo) {
  ASSERT_IRQL_PASSIVE();

  ASSERT(NULL != wdf_fo);
  if (NULL == wdf_fo) {
    WdfRequestComplete(request, STATUS_INVALID_PARAMETER);
    return;
  }

  // Get our wrapper for the device and redirect event to its handler
  AndroidUsbDeviceObject* wdf_device_ext =
    GetAndroidUsbDeviceObjectFromHandle(wdf_dev);
  ASSERT(NULL != wdf_device_ext);
  if (NULL != wdf_device_ext) {
    wdf_device_ext->OnEvtDeviceFileCreate(request, wdf_fo);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

#pragma code_seg()

void AndroidUsbDeviceObject::OnEvtIoRead(WDFREQUEST request,
                                         size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(IsInterfaceSelected());
  if (!IsInterfaceSelected()) {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_STATE);
    return;
  }

  // Get our file extension and dispatch this event to its handler
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->OnEvtIoRead(request, length);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::OnEvtIoWrite(WDFREQUEST request,
                                          size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(IsInterfaceSelected());
  if (!IsInterfaceSelected()) {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_STATE);
    return;
  }

  // Get our file extension and dispatch this event to its handler
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->OnEvtIoWrite(request, length);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::OnEvtIoDeviceControl(WDFREQUEST request,
                                                  size_t output_buf_len,
                                                  size_t input_buf_len,
                                                  ULONG ioctl_code) {
  ASSERT_IRQL_LOW_OR_DISPATCH();
  ASSERT(IsInterfaceSelected());
  if (!IsInterfaceSelected()) {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_STATE);
    return;
  }

  // Get our file extension and dispatch this event to its handler
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->OnEvtIoDeviceControl(request,
                                       output_buf_len,
                                       input_buf_len,
                                       ioctl_code);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::EvtIoReadEntry(WDFQUEUE queue,
                                            WDFREQUEST request,
                                            size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Get our file extension and dispatch this event to the appropriate handler
  // inside our device extension.
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->device_object()->OnEvtIoRead(request, length);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::EvtIoWriteEntry(WDFQUEUE queue,
                                            WDFREQUEST request,
                                            size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Get our file extension and dispatch this event to the appropriate handler
  // inside our device extension.
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->device_object()->OnEvtIoWrite(request, length);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::EvtIoDeviceControlEntry(WDFQUEUE queue,
                                                    WDFREQUEST request,
                                                    size_t output_buf_len,
                                                    size_t input_buf_len,
                                                    ULONG ioctl_code) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Get our file extension and dispatch this event to the appropriate handler
  // inside our device extension.
  AndroidUsbFileObject* wdf_file_ext =
    GetAndroidUsbFileObjectForRequest(request);
  ASSERT(NULL != wdf_file_ext);
  if (NULL != wdf_file_ext) {
    wdf_file_ext->device_object()->OnEvtIoDeviceControl(request,
                                                        output_buf_len,
                                                        input_buf_len,
                                                        ioctl_code);
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::OnGetUsbDeviceDescriptorCtl(WDFREQUEST request,
                                                         size_t output_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Check the buffer first
  if (output_buf_len >= sizeof(USB_DEVICE_DESCRIPTOR)) {
    // Get the output buffer
    NTSTATUS status;
    void* ret_info = OutAddress(request, &status);
    ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
    if (NT_SUCCESS(status)) {
      // Copy requested info into output buffer and complete request
      RtlCopyMemory(ret_info,
                    usb_device_descriptor(),
                    sizeof(USB_DEVICE_DESCRIPTOR));

      WdfRequestCompleteWithInformation(request,
                                        STATUS_SUCCESS,
                                        sizeof(USB_DEVICE_DESCRIPTOR));
    } else {
      WdfRequestComplete(request, status);
    }
  } else {
    WdfRequestCompleteWithInformation(request,
                                      STATUS_BUFFER_TOO_SMALL,
                                      sizeof(USB_DEVICE_DESCRIPTOR));
  }
}

void AndroidUsbDeviceObject::OnGetUsbConfigDescriptorCtl(WDFREQUEST request,
                                                         size_t output_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  if (NULL != configuration_descriptor()) {
    // Check the buffer first
    if (output_buf_len >= sizeof(USB_CONFIGURATION_DESCRIPTOR)) {
      // Get the output buffer
      NTSTATUS status;
      void* ret_info = OutAddress(request, &status);
      ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
      if (NT_SUCCESS(status)) {
        // Copy requested info into output buffer and complete request
        RtlCopyMemory(ret_info,
                      configuration_descriptor(),
                      sizeof(USB_CONFIGURATION_DESCRIPTOR));

        WdfRequestCompleteWithInformation(request,
                                          STATUS_SUCCESS,
                                          sizeof(USB_CONFIGURATION_DESCRIPTOR));
      } else {
        WdfRequestComplete(request, status);
      }
    } else {
      WdfRequestCompleteWithInformation(request,
                                        STATUS_BUFFER_TOO_SMALL,
                                        sizeof(USB_CONFIGURATION_DESCRIPTOR));
    }
  } else {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_REQUEST);
  }
}

void AndroidUsbDeviceObject::OnGetUsbInterfaceDescriptorCtl(WDFREQUEST request,
                                                            size_t output_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Check the buffer first
  if (output_buf_len >= sizeof(USB_INTERFACE_DESCRIPTOR)) {
    // Get the output buffer
    NTSTATUS status;
    void* ret_info = OutAddress(request, &status);
    ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
    if (NT_SUCCESS(status)) {
      // Copy requested info into output buffer and complete request
      RtlCopyMemory(ret_info,
                    interface_descriptor(),
                    sizeof(USB_INTERFACE_DESCRIPTOR));

      WdfRequestCompleteWithInformation(request,
                                        STATUS_SUCCESS,
                                        sizeof(USB_INTERFACE_DESCRIPTOR));
    } else {
      WdfRequestComplete(request, status);
    }
  } else {
    WdfRequestCompleteWithInformation(request,
                                      STATUS_BUFFER_TOO_SMALL,
                                      sizeof(USB_INTERFACE_DESCRIPTOR));
  }
}

void AndroidUsbDeviceObject::OnGetEndpointInformationCtl(
    WDFREQUEST request,
    size_t input_buf_len,
    size_t output_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Check the buffers first
  if (input_buf_len < sizeof(AdbQueryEndpointInformation)) {
    WdfRequestComplete(request, STATUS_INVALID_BUFFER_SIZE);
    return;
  }

  if (output_buf_len < sizeof(AdbEndpointInformation)) {
    WdfRequestCompleteWithInformation(request,
                                      STATUS_BUFFER_TOO_SMALL,
                                      sizeof(AdbEndpointInformation));
    return;
  }

  // Get the output buffer
  NTSTATUS status;
  AdbEndpointInformation* ret_info = reinterpret_cast<AdbEndpointInformation*>
    (OutAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Get the input buffer
  AdbQueryEndpointInformation* in = reinterpret_cast<AdbQueryEndpointInformation*>
    (InAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != in));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Lets see what exactly is queried
  UCHAR endpoint_index = in->endpoint_index;
  if (ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX == endpoint_index)
    endpoint_index = bulk_write_pipe_index();
  else if (ADB_QUERY_BULK_READ_ENDPOINT_INDEX == endpoint_index)
    endpoint_index = bulk_read_pipe_index();

  // Make sure index is valid and within interface range
  if ((INVALID_UCHAR == endpoint_index) ||
      (endpoint_index >= configured_pipes_num())) {
    WdfRequestComplete(request, STATUS_NOT_FOUND);
    return;
  }

  // Get endpoint information
  WDF_USB_PIPE_INFORMATION pipe_info;
  WDF_USB_PIPE_INFORMATION_INIT(&pipe_info);
  WDFUSBPIPE wdf_pipe_obj =
      WdfUsbInterfaceGetConfiguredPipe(wdf_usb_interface(), endpoint_index, &pipe_info);
  if (NULL == wdf_pipe_obj) {
    WdfRequestComplete(request, STATUS_NOT_FOUND);
    return;
  }

  // Copy endpoint info to the output
  ret_info->max_packet_size = pipe_info.MaximumPacketSize;
  ret_info->endpoint_address = pipe_info.EndpointAddress;
  ret_info->polling_interval = pipe_info.Interval;
  ret_info->setting_index = pipe_info.SettingIndex;
  ret_info->endpoint_type = static_cast<AdbEndpointType>(pipe_info.PipeType);
  ret_info->max_transfer_size = pipe_info.MaximumTransferSize;

  WdfRequestCompleteWithInformation(request,
                                    STATUS_SUCCESS,
                                    sizeof(AdbEndpointInformation));
}

void AndroidUsbDeviceObject::OnGetSerialNumberCtl(WDFREQUEST request,
                                                  size_t output_buf_len) {
  ASSERT_IRQL_LOW();

  if (NULL == serial_number()) {
    // There is no serial number saved for this device!
    WdfRequestComplete(request, STATUS_INTERNAL_ERROR);
    return;
  }

  size_t expected_len = serial_number_byte_len() + sizeof(WCHAR);

  // Check the buffer first
  if (output_buf_len >= expected_len) {
    // Get the output buffer
    NTSTATUS status;
    WCHAR* ret_info = reinterpret_cast<WCHAR*>(OutAddress(request, &status));
    ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
    if (NT_SUCCESS(status)) {
      // Copy serial number
      RtlCopyMemory(ret_info, serial_number(), serial_number_byte_len());
      ret_info[serial_number_char_len()] = L'\0';
      WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, expected_len);
    } else {
      WdfRequestComplete(request, status);
    }
  } else {
    WdfRequestCompleteWithInformation(request,
                                      STATUS_BUFFER_TOO_SMALL,
                                      sizeof(expected_len));
  }
}

#pragma code_seg("PAGE")

NTSTATUS AndroidUsbDeviceObject::CreateDefaultQueue() {
  ASSERT_IRQL_PASSIVE();

  // Register I/O callbacks to tell the framework that we are interested
  // in handling WdfRequestTypeRead, WdfRequestTypeWrite, and
  // WdfRequestTypeDeviceControl requests. WdfIoQueueDispatchParallel means
  // that we are capable of handling all the I/O request simultaneously and we
  // are responsible for protecting data that could be accessed by these
  // callbacks simultaneously. This queue will be, by default, automanaged by
  // the framework with respect to PnP and Power events. That is, framework
  // will take care of queuing, failing, dispatching incoming requests based
  // on the current PnP / Power state of the device. We also need to register
  // a EvtIoStop handler so that we can acknowledge requests that are pending
  // at the target driver.
  WDF_IO_QUEUE_CONFIG io_queue_config;
  WDF_IO_QUEUE_CONFIG_INIT_DEFAULT_QUEUE(&io_queue_config,
                                         WdfIoQueueDispatchParallel);

  io_queue_config.EvtIoDeviceControl = EvtIoDeviceControlEntry;
  io_queue_config.EvtIoRead = EvtIoReadEntry;
  io_queue_config.EvtIoWrite = EvtIoWriteEntry;
  io_queue_config.AllowZeroLengthRequests = TRUE;
  // By default KMDF will take care of the power management of this queue
  io_queue_config.PowerManaged = WdfUseDefault;

  // Create queue object
  WDFQUEUE wdf_queue_obj = NULL;
  NTSTATUS status = WdfIoQueueCreate(wdf_device(),
                                     &io_queue_config,
                                     WDF_NO_OBJECT_ATTRIBUTES,
                                     &wdf_queue_obj);
  ASSERT(NT_SUCCESS(status) && (NULL != wdf_queue_obj));
  if (!NT_SUCCESS(status))
    return status;
  return STATUS_SUCCESS;
}

NTSTATUS AndroidUsbDeviceObject::ConfigureDevice() {
  ASSERT_IRQL_PASSIVE();

  ASSERT(IsTaretDeviceCreated());
  if (!IsTaretDeviceCreated())
    return STATUS_INTERNAL_ERROR;

  // In order to get the configuration descriptor we must first query for its
  // size (by supplying NULL for the descriptor's address), allocate enough
  // memory and then retrieve the descriptor.
  USHORT size = 0;

  // Query descriptor size first
  NTSTATUS status =
    WdfUsbTargetDeviceRetrieveConfigDescriptor(wdf_target_device(),
                                               WDF_NO_HANDLE,
                                               &size);
  ASSERT((status == STATUS_BUFFER_TOO_SMALL) || !NT_SUCCESS(status));
  if (status != STATUS_BUFFER_TOO_SMALL)
    return status;

  // Create a memory object and specify our device as the parent so that
  // it will be freed automatically along with our device.
  WDFMEMORY memory = NULL;
  WDF_OBJECT_ATTRIBUTES attributes;
  WDF_OBJECT_ATTRIBUTES_INIT(&attributes);
  attributes.ParentObject = wdf_device();
  status = WdfMemoryCreate(&attributes,
                           NonPagedPool,
                           GANDR_POOL_TAG_DEV_CFG_DESC,
                           size,
                           &memory,
                           reinterpret_cast<PVOID*>(&configuration_descriptor_));
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Now retrieve configuration descriptor
  status =
    WdfUsbTargetDeviceRetrieveConfigDescriptor(wdf_target_device(),
                                               configuration_descriptor_,
                                               &size);
  ASSERT(NT_SUCCESS(status) && (NULL != configuration_descriptor_));
  if (!NT_SUCCESS(status))
    return status;

#if DBG
  PrintConfigDescriptor(configuration_descriptor(), size);
#endif  // DBG

  return status;
}

NTSTATUS AndroidUsbDeviceObject::SelectInterfaces() {
  ASSERT_IRQL_PASSIVE();

  ASSERT(IsDeviceConfigured());
  if (!IsDeviceConfigured())
    return STATUS_INTERNAL_ERROR;

  WDF_USB_DEVICE_SELECT_CONFIG_PARAMS config_params;
  PWDF_USB_INTERFACE_SETTING_PAIR pairs = NULL;
  // TODO: We need to find a way (possibly by looking at each
  // interface descriptor) to get index of the ADB interface in multiinterface
  // configuration.
  UCHAR adb_interface_index = 0;

  if (IsSingleInterfaceDevice()) {
    // Our device has only one interface, so we don't have to bother with
    // multiple interfaces at all.
    GoogleDbgPrint("\n********** Device reports single interface");
    // Select single interface configuration
    WDF_USB_DEVICE_SELECT_CONFIG_PARAMS_INIT_SINGLE_INTERFACE(&config_params);
  } else {
    // Configure multiple interfaces
    ULONG num_interf = GetInterfaceCount();
    GoogleDbgPrint("\n********** Device reports %u interfaces",
             num_interf);

    // Allocate pairs for each interface
    pairs = new(PagedPool, GANDR_POOL_TAG_INTERF_PAIRS)
              WDF_USB_INTERFACE_SETTING_PAIR[num_interf];
    ASSERT(NULL != pairs);
    if (NULL == pairs)
      return STATUS_INSUFFICIENT_RESOURCES;

    adb_interface_index = 1;
    // Initialize each interface pair
    for (UCHAR pair = 0; pair < num_interf; pair++) {
      pairs[pair].SettingIndex = 0;
      pairs[pair].UsbInterface =
        WdfUsbTargetDeviceGetInterface(wdf_target_device(), pair);
      ASSERT(NULL != pairs[pair].UsbInterface);
      if (NULL == pairs[pair].UsbInterface) {
        delete[] pairs;
        return STATUS_INTERNAL_ERROR;
      }
    }

    // Select multiinterface configuration
    WDF_USB_DEVICE_SELECT_CONFIG_PARAMS_INIT_MULTIPLE_INTERFACES(&config_params,
                                                                 (UCHAR)num_interf,
                                                                 pairs);
  }

  NTSTATUS status =
    WdfUsbTargetDeviceSelectConfig(wdf_target_device(),
                                   WDF_NO_OBJECT_ATTRIBUTES,
                                   &config_params);
  if (NULL != pairs)
    delete[] pairs;

  // ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

#if DBG
  PrintSelectedConfig(&config_params);
#endif  // DBG

  wdf_usb_interface_ =
    WdfUsbTargetDeviceGetInterface(wdf_target_device(), adb_interface_index);
  ASSERT(NULL != wdf_usb_interface_);
  if (NULL == wdf_usb_interface_)
    return STATUS_INTERNAL_ERROR;

  configured_pipes_num_ = WdfUsbInterfaceGetNumEndpoints(wdf_usb_interface(), 0);
  ASSERT(0 != configured_pipes_num_);

  // Cache selected interface descriptor
  BYTE setting_index =
    WdfUsbInterfaceGetConfiguredSettingIndex(wdf_usb_interface());

  WdfUsbInterfaceGetDescriptor(wdf_usb_interface(),
                               setting_index,
                               &interface_descriptor_);

#if DBG
  PrintInterfaceDescriptor(interface_descriptor());
#endif  // DBG

  // Iterate over pipes, decoding and saving info about bulk r/w pipes for
  // easier and faster addressing later on when they get opened
  for (UCHAR pipe = 0; pipe < configured_pipes_num(); pipe++) {
    WDF_USB_PIPE_INFORMATION pipe_info;
    WDF_USB_PIPE_INFORMATION_INIT(&pipe_info);
    WDFUSBPIPE wdf_pipe_obj =
      WdfUsbInterfaceGetConfiguredPipe(wdf_usb_interface(), pipe, &pipe_info);
    ASSERT(NULL != wdf_pipe_obj);
    if (NULL != wdf_pipe_obj) {
      if ((WdfUsbPipeTypeBulk  == pipe_info.PipeType) &&
          WDF_USB_PIPE_DIRECTION_IN(pipe_info.EndpointAddress)) {
        // This is a bulk read pipe
        ASSERT(!IsBulkReadPipeKnown());
        bulk_read_pipe_index_ = pipe;
      } else {
        ASSERT(!IsBulkWritePipeKnown());
        bulk_write_pipe_index_ = pipe;
      }
    }
#if DBG
    PrintPipeInformation(&pipe_info, pipe);
#endif  // DBG
  }

  // At the end we must have calculated indexes for both,
  // bulk read and write pipes
  ASSERT(!NT_SUCCESS(status) || (IsBulkReadPipeKnown() &&
                                 IsBulkWritePipeKnown()));

  return status;
}

UCHAR AndroidUsbDeviceObject::GetPipeIndexFromFileName(
    PUNICODE_STRING file_path) {
  ASSERT_IRQL_PASSIVE();
  ASSERT((NULL != file_path) && (0 != file_path->Length) && (NULL != file_path->Buffer));
  if ((NULL == file_path) ||
      (0 == file_path->Length) ||
      (NULL == file_path->Buffer)) {
    return INVALID_UCHAR;
  }

  // Lets check for explicit r/w pipe names
  if (0 == RtlCompareUnicodeString(file_path, &bulk_read_pipe_name, TRUE))
    return bulk_read_pipe_index();
  if (0 == RtlCompareUnicodeString(file_path, &bulk_write_pipe_name, TRUE))
    return bulk_write_pipe_index();

  // Lets check path format
  if (file_path->Length <= index_pipe_prefix.Length) {
    GoogleDbgPrint("\n!!!!! Bad format for pipe name: %wZ", file_path);
    return INVALID_UCHAR;
  }

  // Now when whe know that file_path->Length is sufficient lets match this
  // path with the prefix
  UNICODE_STRING prefix_match = *file_path;
  prefix_match.Length = index_pipe_prefix.Length;
  prefix_match.MaximumLength = prefix_match.Length;

  if (0 != RtlCompareUnicodeString(&prefix_match, &index_pipe_prefix, TRUE)) {
    GoogleDbgPrint("\n!!!!! Bad format for pipe name: %wZ", file_path);
    return INVALID_UCHAR;
  }

  // Prefix matches. Make sure that remaining chars are all decimal digits.
  // Pipe index begins right after the prefix ends.
  const ULONG index_begins_at = WcharLen(index_pipe_prefix.Length);
  const ULONG name_len = WcharLen(file_path->Length);
  for (ULONG index = index_begins_at; index < name_len; index++) {
    if ((file_path->Buffer[index] > L'9') ||
        (file_path->Buffer[index] < L'0')) {
      GoogleDbgPrint("\n!!!!! Bad format for pipe name: %wZ", file_path);
      return INVALID_UCHAR;
    }
  }

  // Parse the pipe#
  ULONG uval = 0;
  ULONG umultiplier = 1;

  // traversing least to most significant digits.
  for (ULONG index = name_len - 1; index >= index_begins_at; index--) {
    uval += (umultiplier * static_cast<ULONG>(file_path->Buffer[index] - L'0'));
    umultiplier *= 10;
  }

  return static_cast<UCHAR>(uval);
}

NTSTATUS AndroidUsbDeviceObject::CreatePipeFileObjectExt(
    WDFFILEOBJECT wdf_fo,
    WDFUSBPIPE wdf_pipe_obj,
    const WDF_USB_PIPE_INFORMATION* pipe_info,
    AndroidUsbFileObject** wdf_file_ext) {
  ASSERT_IRQL_PASSIVE();
  ASSERT((NULL != wdf_fo) && (NULL != wdf_pipe_obj) && (NULL != pipe_info) && (NULL != wdf_file_ext));
  if ((NULL == wdf_fo) || (NULL == wdf_pipe_obj) || (NULL == pipe_info) || (NULL == wdf_file_ext)) {
    return STATUS_INTERNAL_ERROR;
  }
  *wdf_file_ext = NULL;

  AndroidUsbPipeFileObject* wdf_pipe_file_ext = NULL;

  // We support only WdfUsbPipeTypeBulk and WdfUsbPipeTypeInterrupt files
  // at this point.
  switch (pipe_info->PipeType) {
    case WdfUsbPipeTypeBulk:
      wdf_pipe_file_ext = new(NonPagedPool, GANDR_POOL_TAG_BULK_FILE)
            AndroidUsbBulkPipeFileObject(this, wdf_fo, wdf_pipe_obj);
      break;

    case WdfUsbPipeTypeInterrupt:
      wdf_pipe_file_ext = new(NonPagedPool, GANDR_POOL_TAG_INTERRUPT_FILE)
          AndroidUsbInterruptPipeFileObject(this, wdf_fo, wdf_pipe_obj);
      break;;

    case WdfUsbPipeTypeIsochronous:
    case WdfUsbPipeTypeControl:
    case WdfUsbPipeTypeInvalid:
    default:
      return STATUS_OBJECT_TYPE_MISMATCH;
  }

  // If we reached here instance of a file wrapper must be created.
  ASSERT(NULL != wdf_pipe_file_ext);
  if (NULL == wdf_pipe_file_ext)
    return STATUS_INSUFFICIENT_RESOURCES;
    
  // Initialize the wrapper.
  NTSTATUS status = wdf_pipe_file_ext->InitializePipe(pipe_info);
  ASSERT(NT_SUCCESS(status));
  if (NT_SUCCESS(status)) {
    *wdf_file_ext = wdf_pipe_file_ext;
  } else {
    delete wdf_pipe_file_ext;
  }

  return STATUS_SUCCESS;
}

#if DBG
#pragma code_seg()

void AndroidUsbDeviceObject::PrintUsbDeviceDescriptor(
    const USB_DEVICE_DESCRIPTOR* desc) {
  GoogleDbgPrint("\n***** USB_DEVICE_DESCRIPTOR %p for device %p", desc, this);
  GoogleDbgPrint("\n      bDescriptorType    = %u", desc->bDescriptorType);
  GoogleDbgPrint("\n      bcdUSB             = x%02X", desc->bcdUSB);
  GoogleDbgPrint("\n      bDeviceClass       = x%02X", desc->bDeviceClass);
  GoogleDbgPrint("\n      bDeviceSubClass    = x%02X", desc->bDeviceSubClass);
  GoogleDbgPrint("\n      bDeviceProtocol    = x%02X", desc->bDeviceProtocol);
  GoogleDbgPrint("\n      bMaxPacketSize     = %u", desc->bMaxPacketSize0);
  GoogleDbgPrint("\n      idVendor           = x%04X", desc->idVendor);
  GoogleDbgPrint("\n      idProduct          = x%04X", desc->idProduct);
  GoogleDbgPrint("\n      bcdDevice          = x%02X", desc->bcdDevice);
  GoogleDbgPrint("\n      iManufacturer      = %u", desc->iManufacturer);
  GoogleDbgPrint("\n      iProduct           = %u", desc->iProduct);
  GoogleDbgPrint("\n      iSerialNumber      = %u", desc->iSerialNumber);
  GoogleDbgPrint("\n      bNumConfigurations = %u", desc->bNumConfigurations);
}

void AndroidUsbDeviceObject::PrintUsbTargedDeviceInformation(
    const WDF_USB_DEVICE_INFORMATION* info) {
  GoogleDbgPrint("\n***** WDF_USB_DEVICE_INFORMATION %p for device %p", info, this);
  GoogleDbgPrint("\n      HcdPortCapabilities               = x%08X", info->HcdPortCapabilities);
  GoogleDbgPrint("\n      Traits                            = x%08X", info->Traits);
  GoogleDbgPrint("\n      VersionInfo.USBDI_Version         = x%08X",
           info->UsbdVersionInformation.USBDI_Version);
  GoogleDbgPrint("\n      VersionInfo.Supported_USB_Version = x%08X",
           info->UsbdVersionInformation.Supported_USB_Version);
}

void AndroidUsbDeviceObject::PrintConfigDescriptor(
    const USB_CONFIGURATION_DESCRIPTOR* desc,
    ULONG size) {
  GoogleDbgPrint("\n***** USB_CONFIGURATION_DESCRIPTOR %p for device %p size %u",
           desc, this, size);
  GoogleDbgPrint("\n      bDescriptorType     = %u", desc->bDescriptorType);
  GoogleDbgPrint("\n      wTotalLength        = %u", desc->wTotalLength);
  GoogleDbgPrint("\n      bNumInterfaces      = %u", desc->bNumInterfaces);
  GoogleDbgPrint("\n      bConfigurationValue = %u", desc->bConfigurationValue);
  GoogleDbgPrint("\n      iConfiguration      = %u", desc->iConfiguration);
  GoogleDbgPrint("\n      bmAttributes        = %u", desc->bmAttributes);
  GoogleDbgPrint("\n      MaxPower            = %u", desc->MaxPower);
}

void AndroidUsbDeviceObject::PrintSelectedConfig(
    const WDF_USB_DEVICE_SELECT_CONFIG_PARAMS* config) {
  GoogleDbgPrint("\n***** WDF_USB_DEVICE_SELECT_CONFIG_PARAMS %p for device %p", config, this);
  GoogleDbgPrint("\n      Type = %u", config->Type);
  switch (config->Type) {
    case WdfUsbTargetDeviceSelectConfigTypeSingleInterface:
      GoogleDbgPrint("\n      SingleInterface:");
      GoogleDbgPrint("\n         NumberConfiguredPipes  = %u",
               config->Types.SingleInterface.NumberConfiguredPipes);
      GoogleDbgPrint("\n         ConfiguredUsbInterface = %p",
               config->Types.SingleInterface.ConfiguredUsbInterface);
      break;

    case WdfUsbTargetDeviceSelectConfigTypeMultiInterface:
      GoogleDbgPrint("\n      MultiInterface:");
      GoogleDbgPrint("\n         NumberInterfaces              = %u",
               config->Types.MultiInterface.NumberInterfaces);
      GoogleDbgPrint("\n         NumberOfConfiguredInterfaces  = %u",
               config->Types.MultiInterface.NumberOfConfiguredInterfaces);
      GoogleDbgPrint("\n         Pairs                         = %p",
               config->Types.MultiInterface.Pairs);
      break;

    case WdfUsbTargetDeviceSelectConfigTypeInterfacesDescriptor:
      GoogleDbgPrint("\n      Descriptor:");
      GoogleDbgPrint("\n         NumInterfaceDescriptors = %u",
               config->Types.Descriptor.NumInterfaceDescriptors);
      GoogleDbgPrint("\n         ConfigurationDescriptor = %p",
               config->Types.Descriptor.ConfigurationDescriptor);
      GoogleDbgPrint("\n         InterfaceDescriptors    = %p",
               config->Types.Descriptor.InterfaceDescriptors);
      break;

    case WdfUsbTargetDeviceSelectConfigTypeUrb:
      GoogleDbgPrint("\n      Urb:");
      GoogleDbgPrint("\n         Urb = %p",
               config->Types.Urb.Urb);
      break;

    case WdfUsbTargetDeviceSelectConfigTypeInterfacesPairs:
    case WdfUsbTargetDeviceSelectConfigTypeInvalid:
    case WdfUsbTargetDeviceSelectConfigTypeDeconfig:
    default:
      GoogleDbgPrint("\n      Config type is unknown or invalid or not printable.");
      break;
  }
}

void AndroidUsbDeviceObject::PrintInterfaceDescriptor(
    const USB_INTERFACE_DESCRIPTOR* desc) {
  GoogleDbgPrint("\n***** USB_INTERFACE_DESCRIPTOR %p for device %p",
           desc, this);
  GoogleDbgPrint("\n      bLength            = %u", desc->bLength);
  GoogleDbgPrint("\n      bDescriptorType    = %u", desc->bDescriptorType);
  GoogleDbgPrint("\n      bInterfaceNumber   = %u", desc->bInterfaceNumber);
  GoogleDbgPrint("\n      bAlternateSetting  = %u", desc->bAlternateSetting);
  GoogleDbgPrint("\n      bNumEndpoints      = %u", desc->bNumEndpoints);
  GoogleDbgPrint("\n      bInterfaceClass    = x%02X", desc->bInterfaceClass);
  GoogleDbgPrint("\n      bInterfaceSubClass = x%02X", desc->bInterfaceSubClass);
  GoogleDbgPrint("\n      bInterfaceProtocol = x%02X", desc->bInterfaceProtocol);
  GoogleDbgPrint("\n      iInterface         = %u", desc->iInterface);
}

void AndroidUsbDeviceObject::PrintPipeInformation(
    const WDF_USB_PIPE_INFORMATION* info,
    UCHAR pipe_index) {
  GoogleDbgPrint("\n***** WDF_USB_PIPE_INFORMATION[%u] %p for device %p",
           pipe_index, info, this);
  GoogleDbgPrint("\n      Size                = %u", info->Size);
  GoogleDbgPrint("\n      MaximumPacketSize   = %u", info->MaximumPacketSize);
  GoogleDbgPrint("\n      EndpointAddress     = x%02X", info->EndpointAddress);
  GoogleDbgPrint("\n      Interval            = %u", info->Interval);
  GoogleDbgPrint("\n      SettingIndex        = %u", info->SettingIndex);
  GoogleDbgPrint("\n      PipeType            = %u", info->PipeType);
  GoogleDbgPrint("\n      MaximumTransferSize = %u", info->MaximumTransferSize);
}

#endif  // DBG

#pragma data_seg()
#pragma code_seg()

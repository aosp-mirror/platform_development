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
  This file consists of implementation of class AndroidUsbPipeFileObject that
  encapsulates a common extension for pipe file objects.
*/
#pragma data_seg()
#pragma code_seg()

#include "precomp.h"
#include "android_usb_pipe_file_object.h"

#pragma data_seg()
#pragma code_seg("PAGE")

AndroidUsbPipeFileObject::AndroidUsbPipeFileObject(
    AndroidUsbDeviceObject* dev_obj,
    WDFFILEOBJECT wdf_fo,
    WDFUSBPIPE wdf_pipe_obj)
    : AndroidUsbFileObject(AndroidUsbFileObjectTypePipe, dev_obj, wdf_fo),
      wdf_pipe_(wdf_pipe_obj) {
  ASSERT_IRQL_PASSIVE();
  ASSERT(NULL != wdf_pipe_obj);
}

#pragma code_seg()

AndroidUsbPipeFileObject::~AndroidUsbPipeFileObject() {
  ASSERT_IRQL_LOW_OR_DISPATCH();
}

#pragma code_seg("PAGE")

NTSTATUS AndroidUsbPipeFileObject::InitializePipe(
    const WDF_USB_PIPE_INFORMATION* pipe_info) {
  ASSERT_IRQL_LOW();
  ASSERT(IsPipeAttached());
  if (!IsPipeAttached())
    return STATUS_INTERNAL_ERROR;

  // Initialize base class
  NTSTATUS status = AndroidUsbFileObject::Initialize();
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status))
    return status;

  // Save pipe information
  pipe_information_ = *pipe_info;

  // We will provide size check ourselves (less surprizes always better)
  WdfUsbTargetPipeSetNoMaximumPacketSizeCheck(wdf_pipe());

  GoogleDbgPrint("\n===== File %p for %s pipe. max_transfer_size = %X, max_packet_size = %X",
                 this, is_input_pipe() ? "read" : "write",
                 max_transfer_size(), max_packet_size());
  return STATUS_SUCCESS;
}

#pragma code_seg()

void AndroidUsbPipeFileObject::OnEvtIoRead(WDFREQUEST request,
                                           size_t length) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Make sure that this is an input pipe
  if (is_output_pipe()) {
    GoogleDbgPrint("\n!!!! Attempt to read from output pipe %p", this);
    WdfRequestComplete(request, STATUS_ACCESS_DENIED);
    return;
  }

  // Make sure zero length I/O doesn't go through
  if (0 == length) {
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, 0);
    return;
  }

  // Get MDL for this request.
  PMDL request_mdl = NULL;
  NTSTATUS status = WdfRequestRetrieveOutputWdmMdl(request, &request_mdl);
  ASSERT(NT_SUCCESS(status) && (NULL != request_mdl));
  if (NT_SUCCESS(status)) {
    CommonBulkReadWrite(request,
                        request_mdl,
                        static_cast<ULONG>(length),
                        true,
                        0,
                        false);
  } else {
    WdfRequestComplete(request, status);
  }
}

void AndroidUsbPipeFileObject::OnEvtIoWrite(WDFREQUEST request,
                                            size_t length) {

  // Make sure that this is an output pipe
  if (is_input_pipe()) {
    GoogleDbgPrint("\n!!!! Attempt to write to input pipe %p", this);
    WdfRequestComplete(request, STATUS_ACCESS_DENIED);
    return;
  }

  // Make sure zero length I/O doesn't go through
  if (0 == length) {
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, 0);
    return;
  }

  // Get MDL for this request.
  PMDL request_mdl = NULL;
  NTSTATUS status = WdfRequestRetrieveInputWdmMdl(request, &request_mdl);
  ASSERT(NT_SUCCESS(status) && (NULL != request_mdl));
  if (NT_SUCCESS(status)) {
    CommonBulkReadWrite(request,
                        request_mdl,
                        static_cast<ULONG>(length),
                        false,
                        0,
                        false);
  } else {
    WdfRequestComplete(request, status);
  }
}

void AndroidUsbPipeFileObject::OnEvtIoDeviceControl(WDFREQUEST request,
                                                    size_t output_buf_len,
                                                    size_t input_buf_len,
                                                    ULONG ioctl_code) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  switch (ioctl_code) {
    case ADB_IOCTL_GET_ENDPOINT_INFORMATION:
      OnCtlGetEndpointInformation(request, output_buf_len);
      break;

    case ADB_IOCTL_BULK_READ:
      OnCtlBulkRead(request, output_buf_len, input_buf_len);
      break;

    case ADB_IOCTL_BULK_WRITE:
      OnCtlBulkWrite(request, output_buf_len, input_buf_len);
      break;

    default:
      AndroidUsbFileObject::OnEvtIoDeviceControl(request,
                                                 output_buf_len,
                                                 input_buf_len,
                                                 ioctl_code);
      break;
  }
}

void AndroidUsbPipeFileObject::OnCtlGetEndpointInformation(
    WDFREQUEST request,
    size_t output_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Verify output buffer
  if (output_buf_len < sizeof(AdbEndpointInformation)) {
    WdfRequestCompleteWithInformation(request,
                                      STATUS_BUFFER_TOO_SMALL,
                                      sizeof(AdbEndpointInformation));
    return;
  }

  // Get the output buffer
  NTSTATUS status;
  AdbEndpointInformation* ret_info =
    reinterpret_cast<AdbEndpointInformation*>(OutAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != ret_info));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Copy endpoint info to the output
  ret_info->max_packet_size = pipe_information_.MaximumPacketSize;
  ret_info->endpoint_address = pipe_information_.EndpointAddress;
  ret_info->polling_interval = pipe_information_.Interval;
  ret_info->setting_index = pipe_information_.SettingIndex;
  ret_info->endpoint_type =
    static_cast<AdbEndpointType>(pipe_information_.PipeType);
  ret_info->max_transfer_size = pipe_information_.MaximumTransferSize;

  WdfRequestCompleteWithInformation(request,
                                    STATUS_SUCCESS,
                                    sizeof(AdbEndpointInformation));
}

void AndroidUsbPipeFileObject::OnCtlBulkRead(WDFREQUEST request,
                                             size_t output_buf_len,
                                             size_t input_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Make sure that this is an input pipe
  if (is_output_pipe()) {
    GoogleDbgPrint("\n!!!! Attempt to IOCTL read from output pipe %p", this);
    WdfRequestComplete(request, STATUS_ACCESS_DENIED);
    return;
  }

  // Make sure zero length I/O doesn't go through
  if (0 == output_buf_len) {
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, 0);
    return;
  }

  // Verify buffers
  ASSERT(input_buf_len >= sizeof(AdbBulkTransfer));
  if (input_buf_len < sizeof(AdbBulkTransfer)) {
    WdfRequestComplete(request, STATUS_INVALID_BUFFER_SIZE);
    return;
  }

  // Get the input buffer
  NTSTATUS status;
  AdbBulkTransfer* transfer_param =
    reinterpret_cast<AdbBulkTransfer*>(InAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != transfer_param));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Get MDL for this request.
  PMDL request_mdl = NULL;
  status = WdfRequestRetrieveOutputWdmMdl(request, &request_mdl);
  ASSERT(NT_SUCCESS(status) && (NULL != request_mdl));
  if (NT_SUCCESS(status)) {
    // Perform the read
    CommonBulkReadWrite(request,
                        request_mdl,
                        static_cast<ULONG>(output_buf_len),
                        true,
                        transfer_param->time_out,
                        true);
  } else {
    WdfRequestComplete(request, status);
  }
}

void AndroidUsbPipeFileObject::OnCtlBulkWrite(WDFREQUEST request,
                                              size_t output_buf_len,
                                              size_t input_buf_len) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Make sure that this is an output pipe
  if (is_input_pipe()) {
    GoogleDbgPrint("\n!!!! Attempt to IOCTL write to input pipe %p", this);
    WdfRequestComplete(request, STATUS_ACCESS_DENIED);
    return;
  }

  // Verify buffers
  ASSERT(input_buf_len >= sizeof(AdbBulkTransfer));
  // Output buffer points to ULONG that receives number of transferred bytes
  ASSERT(output_buf_len >= sizeof(ULONG));
  if ((input_buf_len < sizeof(AdbBulkTransfer)) ||
      (output_buf_len < sizeof(ULONG))) {
    WdfRequestComplete(request, STATUS_INVALID_BUFFER_SIZE);
    return;
  }

  // Get the input buffer
  NTSTATUS status = STATUS_SUCCESS;
  AdbBulkTransfer* transfer_param =
    reinterpret_cast<AdbBulkTransfer*>(InAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != transfer_param));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Get the output buffer
  ULONG* ret_transfer =
    reinterpret_cast<ULONG*>(OutAddress(request, &status));
  ASSERT(NT_SUCCESS(status) && (NULL != ret_transfer));
  if (!NT_SUCCESS(status)) {
    WdfRequestComplete(request, status);
    return;
  }

  // Cache these param to prevent us from sudden change after we've chacked it.
  // This is common practice in protecting ourselves from malicious code:
  // 1. Never trust anything that comes from the User Mode.
  // 2. Never assume that anything that User Mode buffer has will remain
  // unchanged.
  void* transfer_buffer = transfer_param->GetWriteBuffer();
  ULONG transfer_size = transfer_param->transfer_size;

  // Make sure zero length I/O doesn't go through
  if (0 == transfer_size) {
    *ret_transfer = 0;
    WdfRequestCompleteWithInformation(request, STATUS_SUCCESS, sizeof(ULONG));
    return;
  }

  // Make sure that buffer is not NULL
  ASSERT(NULL != transfer_buffer);
  if (NULL == transfer_buffer) {
    WdfRequestComplete(request, STATUS_INVALID_PARAMETER);
    return;
  }

  // At this point we are ready to build MDL for the user buffer.
  PMDL write_mdl =
    IoAllocateMdl(transfer_buffer, transfer_size, FALSE, FALSE, NULL);
  ASSERT(NULL != write_mdl);
  if (NULL == write_mdl) {
    WdfRequestComplete(request, STATUS_INSUFFICIENT_RESOURCES);
    return;
  }

  // Now we need to probe/lock this mdl
  __try {
    MmProbeAndLockPages(write_mdl,
                        WdfRequestGetRequestorMode(request),
                        IoReadAccess);
    status = STATUS_SUCCESS;
  } __except (EXCEPTION_EXECUTE_HANDLER) {
    status = GetExceptionCode();
    ASSERTMSG("\n!!!!! AndroidUsbPipeFileObject::OnCtlBulkWrite exception",
              false);
  }

  if (!NT_SUCCESS(status)) {
    IoFreeMdl(write_mdl);
    WdfRequestComplete(request, status);
    return;
  }

  // Perform the write
  status = CommonBulkReadWrite(request,
                               write_mdl,
                               transfer_size,
                               false,
                               transfer_param->time_out,
                               true);
  if (!NT_SUCCESS(status)) {
    // If CommonBulkReadWrite failed we need to unlock and free MDL here
    MmUnlockPages(write_mdl);
    IoFreeMdl(write_mdl);
  }
}

NTSTATUS AndroidUsbPipeFileObject::CommonBulkReadWrite(
    WDFREQUEST request,
    PMDL transfer_mdl,
    ULONG length,
    bool is_read,
    ULONG time_out,
    bool is_ioctl) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  ASSERT(IsPipeAttached());
  if (!IsPipeAttached()) {
    WdfRequestComplete(request, STATUS_INVALID_DEVICE_STATE);
    return STATUS_INVALID_DEVICE_STATE;
  }

  // Quick access check. Might be redundant though...
  ASSERT((is_read && is_input_pipe()) || (!is_read && is_output_pipe()));
  if ((is_read && is_output_pipe()) || (!is_read && is_input_pipe())) {
    WdfRequestComplete(request, STATUS_ACCESS_DENIED);
    return STATUS_ACCESS_DENIED;
  }

  // Set URB flags
  ULONG urb_flags = USBD_SHORT_TRANSFER_OK | (is_read ?
                                                USBD_TRANSFER_DIRECTION_IN :
                                                USBD_TRANSFER_DIRECTION_OUT);

  // Calculate transfer length for this stage.
  ULONG stage_len =
    (length > GetTransferGranularity()) ? GetTransferGranularity() : length;

  // Get virtual address that we're gonna use in the transfer.
  // We rely here on the fact that we're in the context of the calling thread.
  void* virtual_address = MmGetMdlVirtualAddress(transfer_mdl);

  // Allocate our private MDL for this address which we will use for the transfer
  PMDL new_mdl = IoAllocateMdl(virtual_address, length, FALSE, FALSE, NULL);
  ASSERT(NULL != new_mdl);
  if (NULL == new_mdl) {
    WdfRequestComplete(request, STATUS_INSUFFICIENT_RESOURCES);
    return STATUS_INSUFFICIENT_RESOURCES;
  }

  // Map the portion of user buffer that we're going to transfer at this stage
  // to our mdl.
  IoBuildPartialMdl(transfer_mdl, new_mdl, virtual_address, stage_len);

  // Allocate memory for URB and associate it with this request
  WDF_OBJECT_ATTRIBUTES mem_attrib;
  WDF_OBJECT_ATTRIBUTES_INIT(&mem_attrib);
  mem_attrib.ParentObject = request;

  WDFMEMORY urb_mem = NULL;
  PURB urb = NULL;
  NTSTATUS status =
    WdfMemoryCreate(&mem_attrib,
                    NonPagedPool,
                    GANDR_POOL_TAG_BULKRW_URB,
                    sizeof(struct _URB_BULK_OR_INTERRUPT_TRANSFER),
                    &urb_mem,
                    reinterpret_cast<PVOID*>(&urb));
  ASSERT(NT_SUCCESS(status) && (NULL != urb));
  if (!NT_SUCCESS(status)) {
    IoFreeMdl(new_mdl);
    WdfRequestComplete(request, STATUS_INSUFFICIENT_RESOURCES);
    return STATUS_INSUFFICIENT_RESOURCES;
  }

  // Get USB pipe handle for our pipe and initialize transfer request for it
  USBD_PIPE_HANDLE usbd_pipe_hndl = usbd_pipe();
  ASSERT(NULL != usbd_pipe_hndl);
  if (NULL == usbd_pipe_hndl) {
    IoFreeMdl(new_mdl);
    WdfRequestComplete(request, STATUS_INTERNAL_ERROR);
    return STATUS_INTERNAL_ERROR;
  }

  // Initialize URB with request information
  UsbBuildInterruptOrBulkTransferRequest(
    urb,
    sizeof(struct _URB_BULK_OR_INTERRUPT_TRANSFER),
    usbd_pipe_hndl,
    NULL,
    new_mdl,
    stage_len,
    urb_flags,
    NULL);

  // Build transfer request
  status = WdfUsbTargetPipeFormatRequestForUrb(wdf_pipe(),
                                               request,
                                               urb_mem,
                                               NULL);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status)) {
    IoFreeMdl(new_mdl);
    WdfRequestComplete(request, status);
    return status;
  }

  // Initialize our request context.
  AndroidUsbWdfRequestContext* context =
    GetAndroidUsbWdfRequestContext(request);
  ASSERT(NULL != context);
  if (NULL == context) {
    IoFreeMdl(new_mdl);
    WdfRequestComplete(request, STATUS_INTERNAL_ERROR);
    return STATUS_INTERNAL_ERROR;
  }

  context->object_type = AndroidUsbWdfObjectTypeRequest;
  context->urb_mem = urb_mem;
  context->transfer_mdl = transfer_mdl;
  context->mdl = new_mdl;
  context->length = length;
  context->transfer_size = stage_len;
  context->num_xfer = 0;
  context->virtual_address = virtual_address;
  context->is_read = is_read;
  context->initial_time_out = time_out;
  context->is_ioctl = is_ioctl;

  // Set our completion routine
  WdfRequestSetCompletionRoutine(request,
                                 CommonReadWriteCompletionEntry,
                                 this);

  // Init send options (our timeout goes here)
  WDF_REQUEST_SEND_OPTIONS send_options;
  if (0 != time_out) {
    WDF_REQUEST_SEND_OPTIONS_INIT(&send_options, WDF_REQUEST_SEND_OPTION_TIMEOUT);
    WDF_REQUEST_SEND_OPTIONS_SET_TIMEOUT(&send_options, WDF_REL_TIMEOUT_IN_MS(time_out));
  }

  // Timestamp first WdfRequestSend
  KeQuerySystemTime(&context->sent_at);

  // Send request asynchronously.
  if (WdfRequestSend(request, wdf_pipe_io_target(),
                     (0 == time_out) ? WDF_NO_SEND_OPTIONS : &send_options)) {
    return STATUS_SUCCESS;
  }

  // Something went wrong here
  status = WdfRequestGetStatus(request);
  ASSERT(!NT_SUCCESS(status));
  GoogleDbgPrint("\n!!!!! CommonBulkReadWrite: WdfRequestGetStatus (is_read = %u) failed: %08X",
           is_read, status);
  WdfRequestCompleteWithInformation(request, status, 0);

  return status;
}

void AndroidUsbPipeFileObject::OnCommonReadWriteCompletion(
    WDFREQUEST request,
    PWDF_REQUEST_COMPLETION_PARAMS completion_params,
    AndroidUsbWdfRequestContext* context) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  NTSTATUS status = completion_params->IoStatus.Status;
  if (!NT_SUCCESS(status)){
    GoogleDbgPrint("\n========== Request completed with failure: %X", status);
    IoFreeMdl(context->mdl);
    // If this was IOCTL-originated write we must unlock and free
    // our transfer MDL.
    if (context->is_ioctl && !context->is_read) {
      MmUnlockPages(context->transfer_mdl);
      IoFreeMdl(context->transfer_mdl);
    }
    WdfRequestComplete(request, status);
    return;
  }

  // Get our URB buffer
  PURB urb
    = reinterpret_cast<PURB>(WdfMemoryGetBuffer(context->urb_mem, NULL));
  ASSERT(NULL != urb);

  // Lets see how much has been transfered and update our counters accordingly
  ULONG bytes_transfered =
    urb->UrbBulkOrInterruptTransfer.TransferBufferLength;
  // We expect writes to transfer entire packet
  ASSERT((bytes_transfered == context->transfer_size) || context->is_read);
  context->num_xfer += bytes_transfered;
  context->length -= bytes_transfered;

  // Is there anything left to transfer? Now, by the protocol we should
  // successfuly complete partial reads, instead of waiting on full set
  // of requested bytes being accumulated in the read buffer.
  if ((0 == context->length) || context->is_read) {
    status = STATUS_SUCCESS;

    // This was the last transfer
    if (context->is_ioctl && !context->is_read) {
      // For IOCTL-originated writes we have to return transfer size through
      // the IOCTL's output buffer.
      ULONG* ret_transfer =
        reinterpret_cast<ULONG*>(OutAddress(request, NULL));
      ASSERT(NULL != ret_transfer);
      if (NULL != ret_transfer)
        *ret_transfer = context->num_xfer;
      WdfRequestSetInformation(request, sizeof(ULONG));

      // We also must unlock / free transfer MDL
      MmUnlockPages(context->transfer_mdl);
      IoFreeMdl(context->transfer_mdl);
    } else {
      // For other requests we report transfer size through the request I/O
      // completion status.
      WdfRequestSetInformation(request, context->num_xfer);
    }
    IoFreeMdl(context->mdl);
    WdfRequestComplete(request, status);
    return;
  }

  // There are something left for the transfer. Prepare for it.
  // Required to free any mapping made on the partial MDL and
  // reset internal MDL state.
  MmPrepareMdlForReuse(context->mdl);

  // Update our virtual address
  context->virtual_address = 
    reinterpret_cast<char*>(context->virtual_address) + bytes_transfered;

  // Calculate size of this transfer
  ULONG stage_len =
    (context->length > GetTransferGranularity()) ? GetTransferGranularity() :
                                                   context->length;

  IoBuildPartialMdl(context->transfer_mdl,
                    context->mdl,
                    context->virtual_address,
                    stage_len);

  // Reinitialize the urb and context
  urb->UrbBulkOrInterruptTransfer.TransferBufferLength = stage_len;
  context->transfer_size = stage_len;

  // Format the request to send a URB to a USB pipe.
  status = WdfUsbTargetPipeFormatRequestForUrb(wdf_pipe(),
                                               request,
                                               context->urb_mem,
                                               NULL);
  ASSERT(NT_SUCCESS(status));
  if (!NT_SUCCESS(status)) {
    if (context->is_ioctl && !context->is_read) {
      MmUnlockPages(context->transfer_mdl);
      IoFreeMdl(context->transfer_mdl);
    }
    IoFreeMdl(context->mdl);
    WdfRequestComplete(request, status);
    return;
  }

  // Reset the completion routine
  WdfRequestSetCompletionRoutine(request,
                                 CommonReadWriteCompletionEntry,
                                 this);

  // Send the request asynchronously.
  if (!WdfRequestSend(request, wdf_pipe_io_target(), WDF_NO_SEND_OPTIONS)) {
    if (context->is_ioctl && !context->is_read) {
      MmUnlockPages(context->transfer_mdl);
      IoFreeMdl(context->transfer_mdl);
    }
    status = WdfRequestGetStatus(request);
    IoFreeMdl(context->mdl);
    WdfRequestComplete(request, status);
  }
}

NTSTATUS AndroidUsbPipeFileObject::ResetPipe() {
  ASSERT_IRQL_PASSIVE();

  // This routine synchronously submits a URB_FUNCTION_RESET_PIPE
  // request down the stack.
  NTSTATUS status = WdfUsbTargetPipeAbortSynchronously(wdf_pipe(),
                                                       WDF_NO_HANDLE,
                                                       NULL);
  if (NT_SUCCESS(status)) {
    status = WdfUsbTargetPipeResetSynchronously(wdf_pipe(),
                                                WDF_NO_HANDLE,
                                                NULL);
    if (!NT_SUCCESS(status))
      GoogleDbgPrint("\n!!!!! AndroidUsbPipeFileObject::ResetPipe failed %X", status);
  } else {
      GoogleDbgPrint("\n!!!!! WdfUsbTargetPipeAbortSynchronously failed %X", status);
  }

  return status;
}

NTSTATUS AndroidUsbPipeFileObject::QueueResetPipePassiveCallback() {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  // Initialize workitem
  WDF_OBJECT_ATTRIBUTES attr;
  WDF_OBJECT_ATTRIBUTES_INIT(&attr);
  WDF_OBJECT_ATTRIBUTES_SET_CONTEXT_TYPE(&attr, AndroidUsbWorkitemContext);
  attr.ParentObject = wdf_device();

  WDFWORKITEM wdf_work_item = NULL;
  WDF_WORKITEM_CONFIG workitem_config;
  WDF_WORKITEM_CONFIG_INIT(&workitem_config, ResetPipePassiveCallbackEntry);
  NTSTATUS status = WdfWorkItemCreate(&workitem_config,
                                      &attr,
                                      &wdf_work_item);
  ASSERT(NT_SUCCESS(status) && (NULL != wdf_work_item));
  if (!NT_SUCCESS(status))
    return status;

  // Initialize our extension to work item
  AndroidUsbWorkitemContext* context =
    GetAndroidUsbWorkitemContext(wdf_work_item);
  ASSERT(NULL != context);
  if (NULL == context) {
    WdfObjectDelete(wdf_work_item);
    return STATUS_INTERNAL_ERROR;
  }

  context->object_type = AndroidUsbWdfObjectTypeWorkitem;
  context->pipe_file_ext = this;

  // Enqueue this work item.
  WdfWorkItemEnqueue(wdf_work_item);

  return STATUS_SUCCESS;
}

void AndroidUsbPipeFileObject::CommonReadWriteCompletionEntry(
    WDFREQUEST request,
    WDFIOTARGET wdf_target,
    PWDF_REQUEST_COMPLETION_PARAMS completion_params,
    WDFCONTEXT completion_context) {
  ASSERT_IRQL_LOW_OR_DISPATCH();

  AndroidUsbWdfRequestContext*
    context = GetAndroidUsbWdfRequestContext(request);
  ASSERT((NULL != context) && (AndroidUsbWdfObjectTypeRequest == context->object_type));

  AndroidUsbPipeFileObject* pipe_file_ext =
    reinterpret_cast<AndroidUsbPipeFileObject*>(completion_context);
  ASSERT((NULL != pipe_file_ext) &&
         (pipe_file_ext->wdf_pipe() == (WDFUSBPIPE)wdf_target));

  pipe_file_ext->OnCommonReadWriteCompletion(request,
                                             completion_params,
                                             context);
}

void AndroidUsbPipeFileObject::ResetPipePassiveCallbackEntry(
    WDFWORKITEM wdf_work_item) {
  ASSERT_IRQL_PASSIVE();

  AndroidUsbWorkitemContext* context =
    GetAndroidUsbWorkitemContext(wdf_work_item);
  ASSERT((NULL != context) &&
         (AndroidUsbWdfObjectTypeWorkitem == context->object_type));
  if ((NULL == context) ||
      (AndroidUsbWdfObjectTypeWorkitem != context->object_type)) {
    WdfObjectDelete(wdf_work_item);
    return;
  }

  // In the sample they reset the device if pipe reset failed
  AndroidUsbDeviceObject* wdf_device_ext =
    context->pipe_file_ext->device_object();

  NTSTATUS status = context->pipe_file_ext->ResetPipe();
  if (!NT_SUCCESS(status))
    status = wdf_device_ext->ResetDevice();
  
  WdfObjectDelete(wdf_work_item);
}

#pragma data_seg()
#pragma code_seg()

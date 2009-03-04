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

#ifndef ANDROID_USB_PIPE_FILE_OBJECT_H__
#define ANDROID_USB_PIPE_FILE_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbPipeFileObject that
  encapsulates a common extension for pipe file objects.
*/

#include "android_usb_file_object.h"

/** AndroidUsbPipeFileObject class encapsulates extension for a KMDF file
  object that represents opened pipe. Instances of this class must be
  allocated from NonPagedPool.
*/
class AndroidUsbPipeFileObject : public AndroidUsbFileObject {
 public:
  /** \brief Constructs the object.

    This method must be called at low IRQL.
    @param dev_obj[in] Our device object for which this file has been created
    @param wdf_fo[in] KMDF file object this extension wraps
    @param wdf_pipe_obj[in] KMDF pipe for this file
  */
  AndroidUsbPipeFileObject(AndroidUsbDeviceObject* dev_obj,
                           WDFFILEOBJECT wdf_fo,
                           WDFUSBPIPE wdf_pipe_obj);

  /** \brief Destructs the object.

    This method can be called at any IRQL.
  */
   virtual ~AndroidUsbPipeFileObject();

  /** \brief Initializes the pipe file object extension

    This method internally calls AndroidUsbFileObject::Initialize()
    This method must be called at low IRQL
    @param pipe_info[in] Pipe information
    @return STATUS_SUCCESS or an appropriate error code
  */
  virtual NTSTATUS InitializePipe(const WDF_USB_PIPE_INFORMATION* pipe_info);

  /** \brief Read event handler

    This method is called when a read request comes to the file object this
    extension wraps. This method is an override.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be read.
    @return Successful status or an appropriate error code
  */
  virtual void OnEvtIoRead(WDFREQUEST request, size_t length);

  /** \brief Write event handler

    This method is called when a write request comes to the file object this
    extension wraps. This method is an override.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param length[in] The number of bytes to be written.
    @return Successful status or an appropriate error code
  */
  virtual void OnEvtIoWrite(WDFREQUEST request, size_t length);

  /** \brief IOCTL event handler

    This method is called when a device control request comes to the file
    object this extension wraps. We hanlde the following IOCTLs here:
    1. ADB_CTL_GET_ENDPOINT_INFORMATION
    2. ADB_CTL_BULK_READ
    3. ADB_CTL_BULK_WRITE
    This method can be called IRQL <= DISPATCH_LEVEL.
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

 protected:
  /** \brief Handler for ADB_CTL_GET_ENDPOINT_INFORMATION IOCTL request
    
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
  */
  virtual void OnCtlGetEndpointInformation(WDFREQUEST request,
                                           size_t output_buf_len);

  /** \brief Handler for ADB_CTL_BULK_READ IOCTL request
    
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
  */
  virtual void OnCtlBulkRead(WDFREQUEST request,
                             size_t output_buf_len,
                             size_t input_buf_len);

  /** \brief Handler for ADB_CTL_BULK_WRITE IOCTL request
    
    @param request[in] A handle to a framework request object.
    @param output_buf_len[in] The length, in bytes, of the request's output
           buffer, if an output buffer is available.
    @param input_buf_len[in] The length, in bytes, of the request's input
           buffer, if an input buffer is available.
  */
  virtual void OnCtlBulkWrite(WDFREQUEST request,
                              size_t output_buf_len,
                              size_t input_buf_len);

  /** \brief Performs common bulk read / write on the pipe

    This method is called from bulk and interrupt pipe file extensions to
    perform read to / write from the pipe this file represents. Typicaly,
    this method is called from OnEvtIoRead / OnEvtIoWrite /
    OnEvtIoDeviceControl methods. One very special case for this method is
    IOCTL-originated write request. If this is IOCTL-originated write request
    we can't report transfer size through the request's status block. Instead,
    for IOCTL-originated writes, the output buffer must a) exist and b) point
    to an ULONG that will receive size of the transfer. Besides, for this type
    of writes we create / lock write buffer MDL ourselves so we need to unlock
    and free it in the completion routine.
    This method can be called at IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object.
    @param transfer_mdl[in] MDL for the transferring buffer. The MDL must be
           locked prior to this call.
    @param length[in] The number of bytes to be read / written. If this method
           is actually IOCTL originated write request this parameter must be
           taken from AdbBulkTransfer.transfer_size by the caller of this
           method. AdbBulkTransfer is available at the beginning of the input
           buffer for bulk read / write IOCTLs.
    @param is_read[in] If true this is a read operation, otherwise it's write
           operation.
    @param time_out[in] Number of milliseconds for this request to complete.
           If this parameter is zero there will be no timeout associated with
           the request. Otherwise, if request doesn't complete within the given
           timeframe it will be cancelled.
    @param is_ioctl[in] If 'true' this method has been called from IOCTL
           handler. Otherwise it has been called from read / write handler. If
           this is IOCTL-originated write request we need to report bytes
           transferred through the IOCTL's output buffer.
    This method can be called IRQL <= DISPATCH_LEVEL.
    @return STATUS_SUCCESS or an appropriate error code
  */
  virtual NTSTATUS CommonBulkReadWrite(WDFREQUEST request,
                                       PMDL transfer_mdl,
                                       ULONG length,
                                       bool is_read,
                                       ULONG time_out,
                                       bool is_ioctl);

  /** \brief Handles request completion for CommonBulkReadWrite

    This method is called from CommonReadWriteCompletionEntry.
    This method can be called at IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object that is being
           completed.
    @param params[in] A pointer to a WDF_REQUEST_COMPLETION_PARAMS structure
           that contains information about the completed request.
    @param context[in] Context associated with this request in
           CommonBulkReadWrite
    This method can be called IRQL <= DISPATCH_LEVEL.
  */
  virtual void OnCommonReadWriteCompletion(WDFREQUEST request,
                                           PWDF_REQUEST_COMPLETION_PARAMS completion_params,
                                           AndroidUsbWdfRequestContext* context);

  /** \brief Resets pipe associated with this file

    After reseting the pipe this object might be destroyed.
    This method must be called at PASSIVE IRQL.
    @param read_device_on_failure[in] If true and reset pipe has failed this
           method will attempt to reset the device.
    @return STATUS_SUCCESS on success or an appropriate error code
  */
  virtual NTSTATUS ResetPipe();

  /** \brief Queues a workitem to launch pipe reset at PASSIVE IRQL

    This method can be called at IRQL <= DISPATCH_LEVEL.
    @return STATUS_SUCCESS or an appropriate error code.
  */
  virtual NTSTATUS QueueResetPipePassiveCallback();

 private:
  /** \brief Request completion routine for CommonBulkReadWrite

    This method can be called at IRQL <= DISPATCH_LEVEL.
    @param request[in] A handle to a framework request object that is being
           completed.
    @param wdf_target[in] A handle to an I/O target object that represents the
           I/O target that completed the request. In this case this is a pipe.
    @param params[in] A pointer to a WDF_REQUEST_COMPLETION_PARAMS structure
           that contains information about the completed request.
    @param completion_context[in] A handle to driver-supplied context
           information, which the driver specified in a previous call to 
           WdfRequestSetCompletionRoutine. In our case this is a pointer
           to this class instance that issued the request.
    This method can be called IRQL <= DISPATCH_LEVEL.
  */
  static void CommonReadWriteCompletionEntry(WDFREQUEST request,
                                             WDFIOTARGET wdf_target,
                                             PWDF_REQUEST_COMPLETION_PARAMS params,
                                             WDFCONTEXT completion_context);

  /** \brief Entry point for pipe reset workitem callback

    This method is called at PASSIVE IRQL
    @param wdf_work_item[in] A handle to a framework work item object.
  */
  static void ResetPipePassiveCallbackEntry(WDFWORKITEM wdf_work_item);

 public:
  /// Gets KMDF pipe handle for this file
  __forceinline WDFUSBPIPE wdf_pipe() const {
    return wdf_pipe_;
  }

  /// Gets maximum transfer size for this pipe
  __forceinline ULONG max_transfer_size() const {
    ASSERT(0 != pipe_information_.MaximumTransferSize);
    return pipe_information_.MaximumTransferSize;
  }

  /// Gets maximum packet size this pipe is capable of
  __forceinline ULONG max_packet_size() const {
    ASSERT(0 != pipe_information_.MaximumPacketSize);
    return pipe_information_.MaximumPacketSize;
  }

  /// Gets transfer granularity
  // TODO: It looks like device USB is capable of handling
  // packets with size greater than pipe_information_.MaximumPacketSize!
  // So, looks like we are not bound by this parameter in this driver.
  __forceinline ULONG GetTransferGranularity() const {
    return max_transfer_size();
  }

  /// Checks if this is an input pipe
  __forceinline bool is_input_pipe() const {
    return WDF_USB_PIPE_DIRECTION_IN(pipe_information_.EndpointAddress) ?
          true : false;
  }

  /// Checks if this is an output pipe
  __forceinline bool is_output_pipe() const {
    return WDF_USB_PIPE_DIRECTION_OUT(pipe_information_.EndpointAddress) ?
          true : false;
  }

  /// Checks if pipe is attached to this file
  __forceinline bool IsPipeAttached() const {
    return (NULL != wdf_pipe());
  }

  /// Gets USBD pipe handle
  // TODO: Can we cache this?
  __forceinline USBD_PIPE_HANDLE usbd_pipe() const {
    ASSERT(IsPipeAttached());
    return (IsPipeAttached()) ? WdfUsbTargetPipeWdmGetPipeHandle(wdf_pipe()) :
                                NULL;
  }

  /// Gets I/O target handle for this pipe
  // TODO: Can we cache this?
  __forceinline WDFIOTARGET wdf_pipe_io_target() const {
    ASSERT(IsPipeAttached());
    return (IsPipeAttached()) ? WdfUsbTargetPipeGetIoTarget(wdf_pipe()) :
                                NULL;
  }

 protected:
  /// Cached pipe information
  WDF_USB_PIPE_INFORMATION  pipe_information_;

  /// KMDF pipe handle for this file
  WDFUSBPIPE                wdf_pipe_;
};

#endif  // ANDROID_USB_PIPE_FILE_OBJECT_H__

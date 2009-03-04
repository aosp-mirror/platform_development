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

#ifndef ANDROID_USB_API_ADB_IO_OBJECT_H__
#define ANDROID_USB_API_ADB_IO_OBJECT_H__
/** \file
  This file consists of declaration of class AdbIOObject that encapsulates an
  item on our device that is opened for read / write / IOCTL I/O.
*/

#include "adb_interface.h"

/** Class AdbIOObject encapsulates an item on our device that is opened for
  read / write / IOCTL I/O.. All I/O items (currently only endpoints) are
  always opened for overlapped I/O (i.e. FILE_OVERLAPPED flag is set in
  create attributes). This way each object of the derived class automatically
  supports both, synchronous as well as asynchronous I/O. Since async I/O
  requires "giving out" some I/O context, we have to provide async I/O caller
  with some safe handle to this context. This is done wia allocating
  AdbIOCompletion object that holds async I/O context and returning handle to
  this object to the caller of async I/O.
*/
class AdbIOObject : public AdbObjectHandle {
 public:
  /** \brief Constructs the object
    
    @param interface[in] Parent interface for this object. Interface will be
           referenced in this object's constructur and released in the
           destructor.
    @param obj_type[in] Object type from AdbObjectType enum
  */
  AdbIOObject(AdbInterfaceObject* parent_interf, AdbObjectType obj_type);

 protected:
  /** \brief Destructs the object.

    parent_interface_ will be dereferenced here.
    We hide destructor in order to prevent ourseves from accidentaly allocating
    instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbIOObject();

 public:
  /** \brief Opens USB item and creates a handle to this object

    We combine in this method ADB handle association and opening required
    object on our USB device. The sequence is to open USB item first and if
    (and only if) this open succeedes we proceed to creating ADB handle by
    calling AdbObjectHandle::CreateHandle(). We always open USB handle for
    overlapped I/O.
    @param item_path[in] Path to the item on our USB device.
    @param access_type[in] Desired access type. In the current implementation
          this parameter has no effect on the way item is opened. It's
          always read / write access.
    @param sharing_mode[in] Desired share mode. In the current implementation
          this parameter has no effect on the way item is opened. It's
          always shared for read / write.
    @return A handle to this object on success or NULL on an error.
            If NULL is returned GetLastError() provides extended error
            information. ERROR_GEN_FAILURE is set if an attempt was
            made to create already opened object.
  */
  virtual ADBAPIHANDLE CreateHandle(const wchar_t* item_path,
                                    AdbOpenAccessType access_type,
                                    AdbOpenSharingMode share_mode);

  /** \brief This method is called when handle to this object gets closed

    We overwrite this method in order to close USB handle along with this
    object handle.
    @return 'true' on success or 'false' if object is already closed. If
            'false' is returned GetLastError() provides extended error
            information.
  */
  virtual bool CloseHandle();

  /** \brief Reads from opened I/O object asynchronously

    @param buffer[out] Pointer to the buffer that receives the data.
    @param bytes_to_read[in] Number of bytes to be read.
    @param bytes_read[out] Number of bytes read. Can be NULL.
    @param event_handle[in] Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return A handle to IO completion object or NULL on failure. If NULL is
            returned GetLastError() provides extended error information.
  */
  virtual ADBAPIHANDLE AsyncRead(void* buffer,
                                 ULONG bytes_to_read,
                                 ULONG* bytes_read,
                                 HANDLE event_handle,
                                 ULONG time_out);

  /** \brief Writes to opened I/O object asynchronously

    @param buffer[in] Pointer to the buffer containing the data to be written.
    @param bytes_to_write[in] Number of bytes to be written.
    @param bytes_written[out] Number of bytes written. Can be NULL.
    @param event_handle[in] Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return A handle to IO completion object or NULL on failure. If NULL is
            returned GetLastError() provides extended error information.
  */
  virtual ADBAPIHANDLE AsyncWrite(void* buffer,
                                  ULONG bytes_to_write,
                                  ULONG* bytes_written,
                                  HANDLE event_handle,
                                  ULONG time_out);

  /** \brief Reads from opened I/O object synchronously

    @param buffer[out] Pointer to the buffer that receives the data.
    @param bytes_to_read[in] Number of bytes to be read.
    @param bytes_read[out] Number of bytes read. Can be NULL.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return 'true' on success and 'false' on failure. If 'false' is
            returned GetLastError() provides extended error information.
  */
  virtual bool SyncRead(void* buffer,
                        ULONG bytes_to_read,
                        ULONG* bytes_read,
                        ULONG time_out);

  /** \brief Writes to opened I/O object synchronously

    @param buffer[in] Pointer to the buffer containing the data to be written.
    @param bytes_to_write[in] Number of bytes to be written.
    @param bytes_written[out] Number of bytes written. Can be NULL.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return 'true' on success and 'false' on failure. If 'false' is
            returned GetLastError() provides extended error information.
  */
  virtual bool SyncWrite(void* buffer,
                         ULONG bytes_to_write,
                         ULONG* bytes_written,
                         ULONG time_out);

 protected:
  /** \brief Common code for async read / write

    @param is_read[in] Read or write selector.
    @param buffer[in,out] Pointer to the buffer for read / write.
    @param bytes_to_transfer[in] Number of bytes to be read / written.
    @param bytes_transferred[out] Number of bytes read / written. Can be NULL.
    @param event_handle[in] Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return A handle to IO completion object or NULL on failure. If NULL is
            returned GetLastError() provides extended error information.
  */
  virtual ADBAPIHANDLE CommonAsyncReadWrite(bool is_read,
                                            void* buffer,
                                            ULONG bytes_to_transfer,
                                            ULONG* bytes_transferred,
                                            HANDLE event_handle,
                                            ULONG time_out);

  /** \brief Common code for sync read / write

    @param is_read[in] Read or write selector.
    @param buffer[in,out] Pointer to the buffer for read / write.
    @param bytes_to_transfer[in] Number of bytes to be read / written.
    @param bytes_transferred[out] Number of bytes read / written. Can be NULL.
    @param time_out[in] A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  virtual bool CommonSyncReadWrite(bool is_read,
                                   void* buffer,
                                   ULONG bytes_to_transfer,
                                   ULONG* bytes_transferred,
                                   ULONG time_out);

 public:
  /// Gets parent interface 
  AdbInterfaceObject* parent_interface() const {
    return parent_interface_;
  }

  /// Gets parent interface handle
  ADBAPIHANDLE GetParentInterfaceHandle() const {
    return (NULL != parent_interface()) ? parent_interface()->adb_handle() :
                                          NULL;
  }

  /// Gets handle to an item opened on our USB device
  HANDLE usb_handle() const {
    return usb_handle_;
  }

  /// Checks if USB item is opened
  bool IsUsbOpened() const {
    return (INVALID_HANDLE_VALUE != usb_handle());
  }

  // This is a helper for extracting object from the AdbObjectHandleMap
  static AdbObjectType Type() {
    return AdbObjectTypeIo;
  }

 protected:
  /// Parent interface
  AdbInterfaceObject* parent_interface_;

  /// Handle to an item opened on our USB device
  HANDLE              usb_handle_;
};

#endif  // ANDROID_USB_API_ADB_IO_OBJECT_H__

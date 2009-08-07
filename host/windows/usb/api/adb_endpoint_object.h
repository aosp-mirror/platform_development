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

#ifndef ANDROID_USB_API_ADB_ENDPOINT_OBJECT_H__
#define ANDROID_USB_API_ADB_ENDPOINT_OBJECT_H__
/** \file
  This file consists of declaration of class AdbEndpointObject that
  encapsulates a handle opened to an endpoint on our device.
*/

#include "adb_interface.h"

/** Class AdbEndpointObject encapsulates a handle opened to an endpoint on
  our device.

  This class implement functionality that is common for both, WinUsb and
  legacy APIs.
*/
class ADBWIN_API_CLASS AdbEndpointObject : public AdbObjectHandle {
 public:
  /** \brief Constructs the object
    
    @param[in] interface Parent interface for this object. Interface will be
           referenced in this object's constructur and released in the
           destructor.
    @param[in] endpoint_id Endpoint ID (endpoint address) on the device.
    @param[in] endpoint_index Zero-based endpoint index in the interface's
          array of endpoints.
  */
  AdbEndpointObject(AdbInterfaceObject* parent_interf,
                    UCHAR endpoint_id,
                    UCHAR endpoint_index);

 protected:
  /** \brief Destructs the object.

    We hide destructor in order to prevent ourseves from accidentaly allocating
    instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbEndpointObject();

  //
  // Abstract
  //

 protected:
  /** \brief Common code for async read / write

    @param[in] is_read Read or write selector.
    @param[in,out] buffer Pointer to the buffer for read / write.
    @param[in] bytes_to_transfer Number of bytes to be read / written.
    @param[out] bytes_transferred Number of bytes read / written. Can be NULL.
    @param[in] event_handle Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
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
                                            ULONG time_out) = 0;

  /** \brief Common code for sync read / write

    @param[in] is_read Read or write selector.
    @param[in,out] buffer Pointer to the buffer for read / write.
    @param[in] bytes_to_transfer Number of bytes to be read / written.
    @param[out] bytes_transferred Number of bytes read / written. Can be NULL.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return true on success, false on failure. If false is returned
            GetLastError() provides extended error information.
  */
  virtual bool CommonSyncReadWrite(bool is_read,
                                   void* buffer,
                                   ULONG bytes_to_transfer,
                                   ULONG* bytes_transferred,
                                   ULONG time_out) = 0;

  //
  // Operations
  //

 public:
  /** \brief Gets information about this endpoint.

    @param[out] info Upon successful completion will have endpoint information.
    @return true on success, false on failure. If false is returned
            GetLastError() provides extended error information.
  */
  virtual bool GetEndpointInformation(AdbEndpointInformation* info);

  /** \brief Reads from opened I/O object asynchronously

    @param[out] buffer Pointer to the buffer that receives the data.
    @param[in] bytes_to_read Number of bytes to be read.
    @param[out] bytes_read Number of bytes read. Can be NULL.
    @param[in] event_handle Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
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

    @param[in] buffer Pointer to the buffer containing the data to be written.
    @param[in] bytes_to_write Number of bytes to be written.
    @param[out] bytes_written Number of bytes written. Can be NULL.
    @param[in] event_handle Event handle that should be signaled when async I/O
           completes. Can be NULL. If it's not NULL this handle will be used to
           initialize OVERLAPPED structure for this I/O.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
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

    @param[out] buffer Pointer to the buffer that receives the data.
    @param[in] bytes_to_read Number of bytes to be read.
    @param[out] bytes_read Number of bytes read. Can be NULL.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return true on success and false on failure. If false is
            returned GetLastError() provides extended error information.
  */
  virtual bool SyncRead(void* buffer,
                        ULONG bytes_to_read,
                        ULONG* bytes_read,
                        ULONG time_out);

  /** \brief Writes to opened I/O object synchronously

    @param[in] buffer Pointer to the buffer containing the data to be written.
    @param[in] bytes_to_write Number of bytes to be written.
    @param[out] bytes_written Number of bytes written. Can be NULL.
    @param[in] time_out A timeout (in milliseconds) required for this I/O to
           complete. Zero value in this parameter means that there is no
           timeout set for this I/O.
    @return true on success and false on failure. If false is
            returned GetLastError() provides extended error information.
  */
  virtual bool SyncWrite(void* buffer,
                         ULONG bytes_to_write,
                         ULONG* bytes_written,
                         ULONG time_out);

 public:
  /// This is a helper for extracting object from the AdbObjectHandleMap
  static AdbObjectType Type() {
    return AdbObjectTypeEndpoint;
  }

  /// Gets parent interface 
  AdbInterfaceObject* parent_interface() const {
    return parent_interface_;
  }
  /// Gets this endpoint ID
  UCHAR endpoint_id() const {
    return endpoint_id_;
  }

  /// Gets this endpoint index on the interface
  UCHAR endpoint_index() const {
    return endpoint_index_;
  }

  /// Gets parent interface handle
  ADBAPIHANDLE GetParentInterfaceHandle() const {
    return (NULL != parent_interface()) ? parent_interface()->adb_handle() :
                                          NULL;
  }

 protected:
  /// Parent interface
  AdbInterfaceObject* parent_interface_;

  /// This endpoint id
  UCHAR               endpoint_id_;

  /// This endpoint index on the interface
  UCHAR               endpoint_index_;
};

#endif  // ANDROID_USB_API_ADB_ENDPOINT_OBJECT_H__

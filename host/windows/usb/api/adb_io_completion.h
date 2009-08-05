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

#ifndef ANDROID_USB_API_ADB_IO_COMPLETION_H__
#define ANDROID_USB_API_ADB_IO_COMPLETION_H__
/** \file
  This file consists of declaration of class AdbIOCompletion that encapsulates
  a generic wrapper around OVERLAPPED Win32 structure returned from
  asynchronous I/O requests.
*/

#include "adb_endpoint_object.h"

/** \brief Encapsulates encapsulates a generic wrapper around OVERLAPPED Win32
  structure returned from asynchronous I/O requests.

  This is an abstract class that implements functionality common for I/O
  performed via WinUsb as well as legacy driver APIs. A handle to this object
  is returned to the caller of each successful asynchronous I/O request. Just
  like all other handles this handle must be closed after it's no longer
  needed.
*/
class ADBWIN_API_CLASS AdbIOCompletion : public AdbObjectHandle {
 public:
  /** \brief Constructs the object
    
    @param[in] parent_io_obj Parent I/O object that created this instance.
           Parent object will be referenced in this object's constructor and
           released in the destructor.
    @param[in] expected_trans_size Number of bytes expected to be transferred
          with the I/O.
    @param[in] event_hndl Event handle that should be signaled when I/O
           completes. Can be NULL. If it's not NULL this handle will be
           used to initialize OVERLAPPED structure for this object.
  */
  AdbIOCompletion(AdbEndpointObject* parent_io_obj,
                  ULONG expected_trans_size,
                  HANDLE event_hndl);

 protected:
  /** \brief Destructs the object.

    We hide destructor in order to prevent ourseves from accidentaly allocating
    instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbIOCompletion();

  //
  // Abstract
  //

 public:
  /** \brief Gets overlapped I/O result

    @param[out] ovl_data Buffer for the copy of this object's OVERLAPPED
           structure. Can be NULL.
    @param[out] bytes_transferred Pointer to a variable that receives the
           number of bytes that were actually transferred by a read or write
           operation. See SDK doc on GetOvelappedResult for more information.
           Unlike regular GetOvelappedResult call this parameter can be NULL.
    @param[in] wait If this parameter is true, the method does not return
           until the operation has been completed. If this parameter is false
           and the operation is still pending, the method returns false and
           the GetLastError function returns ERROR_IO_INCOMPLETE.
    @return true if I/O has been completed or false on failure or if request
           is not yet completed. If false is returned GetLastError() provides
           extended error information. If GetLastError returns
           ERROR_IO_INCOMPLETE it means that I/O is not yet completed.
  */
  virtual bool GetOvelappedIoResult(LPOVERLAPPED ovl_data,
                                    ULONG* bytes_transferred,
                                    bool wait) = 0;

  //
  // Operations
  //

 public:
  /** \brief Checks if I/O that this object represents has completed.

    @return true if I/O has been completed or false if it's still
            incomplete. Regardless of the returned value, caller should
            check GetLastError to validate that handle was OK.
  */
  virtual bool IsCompleted();

 public:
  /// Gets overlapped structure for this I/O
  LPOVERLAPPED overlapped() {
    return &overlapped_;
  }

  /// Gets parent object
  AdbEndpointObject* parent_io_object() const {
    return parent_io_object_;
  }

  /// Gets parent object handle
  ADBAPIHANDLE GetParentObjectHandle() const {
    return (NULL != parent_io_object()) ? parent_io_object()->adb_handle() :
                                          NULL;
  }

  // This is a helper for extracting object from the AdbObjectHandleMap
  static AdbObjectType Type() {
    return AdbObjectTypeIoCompletion;
  }

 protected:
  /// Overlapped structure for this I/O
  OVERLAPPED          overlapped_;

  /// Parent I/O object
  AdbEndpointObject*  parent_io_object_;

  /// Expected number of bytes transferred in thi I/O
  ULONG               expected_transfer_size_;
};

#endif  // ANDROID_USB_API_ADB_IO_COMPLETION_H__

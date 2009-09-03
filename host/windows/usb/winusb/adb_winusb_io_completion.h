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

#ifndef ANDROID_USB_API_ADB_WINUSB_IO_COMPLETION_H__
#define ANDROID_USB_API_ADB_WINUSB_IO_COMPLETION_H__
/** \file
  This file consists of declaration of class AdbWinUsbIOCompletion that
  encapsulates a wrapper around OVERLAPPED Win32 structure returned from
  asynchronous I/O requests issued via WinUsb API.
*/

#include "..\api\adb_io_completion.h"
#include "adb_winusb_endpoint_object.h"

/** \brief Encapsulates encapsulates a wrapper around OVERLAPPED Win32
  structure returned from asynchronous I/O requests issued via WinUsb API.

  A handle to this object is returned to the caller of each successful
  asynchronous I/O request. Just like all other handles this handle
  must be closed after it's no longer needed.
*/
class AdbWinUsbIOCompletion : public AdbIOCompletion {
 public:
  /** \brief Constructs the object

    @param[in] parent_io_obj Parent WinUsb I/O object that created this
           instance.
    @param[in] expected_trans_size Number of bytes expected to be transferred
          with the I/O.
    @param[in] event_hndl Event handle that should be signaled when I/O
           completes. Can be NULL. If it's not NULL this handle will be
           used to initialize OVERLAPPED structure for this object.
  */
  AdbWinUsbIOCompletion(AdbWinUsbEndpointObject* parent_io_obj,
                        ULONG expected_trans_size,
                        HANDLE event_hndl);

 protected:
  /** \brief Destructs the object.

    We hide destructor in order to prevent ourseves from accidentaly allocating
    instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbWinUsbIOCompletion();

  //
  // Virtual overrides
  //

 public:
  /** \brief Releases the object.

    If refcount drops to zero as the result of this release, the object is
    destroyed in this method. As a general rule, objects must not be touched
    after this method returns even if returned value is not zero. We override
    this method in order to make sure that objects of this class are deleted
    in contect of the DLL they were created in. The problem is that since
    objects of this class were created in context of AdbWinUsbApi module, they
    are allocated from the heap assigned to that module. Now, if these objects
    are deleted outside of AdbWinUsbApi module, this will lead to the heap
    corruption in the module that deleted these objects. Since all objects of
    this class are deleted in the Release method only, by overriding it we make
    sure that we free memory in the context of the module where it was
    allocated.
    @return Value of the reference counter after object is released in this
            method.
  */
  virtual LONG Release();

  //
  // Abstract overrides
  //

 public:
  /** \brief Gets overlapped I/O result

    This method uses WinUsb_GetOverlappedResult to get results of the
    overlapped I/O operation.
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
                                    bool wait);

 public:
  /// Gets WinUsb parent object
  AdbWinUsbEndpointObject* parent_winusb_io_object() const {
    return reinterpret_cast<AdbWinUsbEndpointObject*>(parent_io_object());
  }
};

#endif  // ANDROID_USB_API_ADB_WINUSB_IO_COMPLETION_H__

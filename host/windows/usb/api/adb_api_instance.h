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

#ifndef ANDROID_USB_API_ADB_API_INSTANCE_H__
#define ANDROID_USB_API_ADB_API_INSTANCE_H__
/** \file
  This file consists of declaration of class AdbApiInstance that is a main
  API object representing a device interface that is in the interest of
  the API client. All device (interface) related operations go through this
  class first.
*/

#include "adb_api.h"
#include "adb_api_private_defines.h"

/** Class AdbApiInstance is the main API interbal object representing a device
  interface that is in the interest of the API client. All device (interface)
  related operations go through this class first. So, before doing anything
  meaningfull with the API a client must first create instance of the API
  via CreateAdbApiInstance, select a device interface for that instance and
  then do everything else.
  Objects of this class are globally stored in the map that matches
  ADBAPIINSTANCEHANDLE to the corresponded object.
  This class is self-referenced with the following reference model:
  1. When object of this class is created and added to the map, its recount
     is set to 1.
  2. Every time the client makes an API call that uses ADBAPIINSTANCEHANDLE
     a corresponded AdbApiInstance object is looked up in the table and its
     refcount is incremented. Upon return from the API call that incremented
     the refcount refcount gets decremented.
  3. When the client closes ADBAPIINSTANCEHANDLE via DeleteAdbApiInstance call
     corresponded object gets deleted from the map and its refcount is
     decremented.
  So, at the end, this object destroys itself when refcount drops to zero.
*/
class AdbApiInstance {
 public:
  /** \brief Constructs the object
    
    @param handle[in] Instance handle associated with this object
  */
  AdbApiInstance();

 private:
  /// Destructs the object
  ~AdbApiInstance();

  /** \brief
    This method is called when last reference to this object has been released

    In this method object is uninitialized and deleted (that is "delete this"
    is called).
  */
  void LastReferenceReleased();

 public:
   /// Gets name of the USB interface (device name) for this instance
   const std::wstring& interface_name() const {
     return interface_name_;
   }

   /// References the object and returns number of references
   LONG AddRef() {
     return InterlockedIncrement(&ref_count_);
   }

   /** \brief Dereferences the object and returns number of references

    Object may be deleted in this method, so you cannot touch it after
    this method returns, even if returned value is not zero, because object
    can be deleted in another thread.
   */
   LONG Release() {
     LONG ret = InterlockedDecrement(&ref_count_);
     if (0 == ret)
       LastReferenceReleased();

     return ret;
   }

   /// Checks if instance has been initialized
   bool IsInitialized() const {
     return !interface_name_.empty();
   }

private:
  /// Name of the USB interface (device name) for this instance
  std::wstring          interface_name_;

  /// Instance handle for this object
  ADBAPIINSTANCEHANDLE  instance_handle_;

  /// Reference counter for this instance
  LONG                  ref_count_;
};

/// Defines map that matches ADBAPIINSTANCEHANDLE with AdbApiInstance object
typedef std::map< ADBAPIINSTANCEHANDLE, AdbApiInstance* > AdbApiInstanceMap;

#endif  // ANDROID_USB_API_ADB_API_INSTANCE_H__

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

#ifndef ANDROID_USB_API_ADB_OBJECT_HANDLE_H__
#define ANDROID_USB_API_ADB_OBJECT_HANDLE_H__
/** \file
  This file consists of declaration of a class AdbObjectHandle that
  encapsulates an internal API object that is visible to the outside
  of the API through a handle.
*/

#include "adb_api.h"
#include "adb_api_private_defines.h"

/** \brief Defines types of internal API objects
*/
enum AdbObjectType {
  /// Object is AdbInterfaceEnumObject.
  AdbObjectTypeInterfaceEnumerator,

  /// Object is AdbInterfaceObject.
  AdbObjectTypeInterface,

  /// Object is AdbEndpointObject.
  AdbObjectTypeEndpoint,

  /// Object is AdbIOCompletion.
  AdbObjectTypeIoCompletion,

  AdbObjectTypeMax
};

/** \brief Encapsulates an internal API basic object that is visible to the
  outside of the API through a handle.
  
  In order to prevent crashes when API client tries to access an object through
  an invalid or already closed handle, we keep track of all opened handles in
  AdbObjectHandleMap that maps association between valid ADBAPIHANDLE and
  an object that this handle represents. All objects that are exposed to the
  outside of API via ADBAPIHANDLE are self-destructing referenced objects.
  The reference model for these objects is as such:
  1. When CreateHandle() method is called on an object, a handle (ADBAPIHANDLE
     that is) is assigned to it, a pair <handle, object> is added to the global
     AdbObjectHandleMap instance, object is referenced and then handle is
     returned to the API client.
  2. Every time API is called with a handle, a lookup is performed in 
     AdbObjectHandleMap to find an object that is associated with the handle.
     If object is not found then ERROR_INVALID_HANDLE is immediatelly returned
     (via SetLastError() call). If object is found then it is referenced and
     API call is dispatched to appropriate method of the found object. Upon
     return from this method, just before returning from the API call, object
     is dereferenced back to match lookup reference.
  3. When object handle gets closed, assuming object is found in the map, that
     <handle, object> pair is deleted from the map and object's refcount is
     decremented to match refcount increment performed when object has been
     added to the map.
  4. When object's refcount drops to zero, the object commits suicide by
     calling "delete this".
  All API objects that have handles that are sent back to API client must be
  derived from this class.
*/
class ADBWIN_API_CLASS AdbObjectHandle {
 public:
  /** \brief Constructs the object

    Refernce counter is set to 1 in the constructor.
    @param[in] obj_type Object type from AdbObjectType enum
  */
  explicit AdbObjectHandle(AdbObjectType obj_type);

 protected:
  /** \brief Destructs the object.

   We hide destructor in order to prevent ourseves from accidentaly allocating
   instances on the stack. If such attempt occurs, compiler will error.
  */
  virtual ~AdbObjectHandle();

 public:
  /** \brief References the object.

    @return Value of the reference counter after object is referenced in this
            method.
  */
  virtual LONG AddRef();

  /** \brief Releases the object.

    If refcount drops to zero as the result of this release, the object is
    destroyed in this method. As a general rule, objects must not be touched
    after this method returns even if returned value is not zero.
    @return Value of the reference counter after object is released in this
            method.
  */
  virtual LONG Release();

  /** \brief Creates handle to this object.

    In this call a handle for this object is generated and object is added
    to the AdbObjectHandleMap.
    @return A handle to this object on success or NULL on an error.
            If NULL is returned GetLastError() provides extended error
            information. ERROR_GEN_FAILURE is set if an attempt was
            made to create already opened object.
  */
  virtual ADBAPIHANDLE CreateHandle();

  /** \brief This method is called when handle to this object gets closed.

    In this call object is deleted from the AdbObjectHandleMap.
    @return true on success or false if object is already closed. If
            false is returned GetLastError() provides extended error
            information.
  */
  virtual bool CloseHandle();

  /** \brief Checks if this object is of the given type.

    @param[in] obj_type One of the AdbObjectType types to check
    @return true is this object type matches obj_type, or false otherwise.
  */
  virtual bool IsObjectOfType(AdbObjectType obj_type) const;

  /** \brief Looks up AdbObjectHandle instance associated with the given handle
    in the AdbObjectHandleMap.

    This method increments reference counter for the returned found object.
    @param[in] adb_handle ADB handle to the object
    @return API object associated with the handle or NULL if object is not
            found. If NULL is returned GetLastError() provides extended error
            information.
  */
  static AdbObjectHandle* Lookup(ADBAPIHANDLE adb_handle);

 protected:
  /** \brief Called when last reference to this object is released.

    Derived object should override this method to perform cleanup that is not
    suitable for destructors.
  */
  virtual void LastReferenceReleased();

 public:
  /// Gets ADB handle associated with this object
  ADBAPIHANDLE adb_handle() const {
    return adb_handle_;
  }

  /// Gets type of this object
  AdbObjectType object_type() const {
    return object_type_;
  }

  /// Checks if object is still opened. Note that it is not guaranteed that
  /// object remains opened when this method returns.
  bool IsOpened() const {
    return (NULL != adb_handle());
  }

 protected:
  /// API handle associated with this object
  ADBAPIHANDLE  adb_handle_;

  /// Type of this object
  AdbObjectType object_type_;

  /// This object's reference counter
  LONG          ref_count_;
};

/// Maps ADBAPIHANDLE to associated AdbObjectHandle object
typedef std::map< ADBAPIHANDLE, AdbObjectHandle* > AdbObjectHandleMap;

/** \brief Template routine that unifies extracting of objects of different
  types from the AdbObjectHandleMap

  @param[in] adb_handle API handle for the object
  @return Object associated with the handle or NULL on error. If NULL is
          returned GetLastError() provides extended error information.
*/
template<class obj_class>
obj_class* LookupObject(ADBAPIHANDLE adb_handle) {
  // Lookup object for the handle in the map
  AdbObjectHandle* adb_object = AdbObjectHandle::Lookup(adb_handle);
  if (NULL != adb_object) {
    // Make sure it's of the correct type
    if (!adb_object->IsObjectOfType(obj_class::Type())) {
      adb_object->Release();
      adb_object = NULL;
      SetLastError(ERROR_INVALID_HANDLE);
    }
  } else {
    SetLastError(ERROR_INVALID_HANDLE);
  }
  return (adb_object != NULL) ? reinterpret_cast<obj_class*>(adb_object) :
                                NULL;
}

#endif  // ANDROID_USB_API_ADB_OBJECT_HANDLE_H__

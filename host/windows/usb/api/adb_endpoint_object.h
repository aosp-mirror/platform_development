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
  This file consists of declaration of class AdbIOObject that encapsulates a
  handle opened to an endpoint on our device.
*/

#include "adb_io_object.h"

/** Class AdbEndpointObject encapsulates a handle opened to an endpoint on
  our device.
*/
class AdbEndpointObject : public AdbIOObject {
 public:
  /** \brief Constructs the object
    
    @param interface[in] Parent interface for this object. Interface will be
           referenced in this object's constructur and released in the
           destructor.
    @param obj_type[in] Object type from AdbObjectType enum
  */
  AdbEndpointObject(AdbInterfaceObject* parent_interf);

 protected:
  /** \brief Destructs the object.

    parent_interface_ will be dereferenced here.
    We hide destructor in order to prevent ourseves from accidentaly allocating
    instances on the stack. If such attemp occur, compiler will error.
  */
  virtual ~AdbEndpointObject();

 public:
  /** \brief Gets information about this endpoint.

    @param info[out] Upon successful completion will have endpoint information.
    @return 'true' on success, 'false' on failure. If 'false' is returned
            GetLastError() provides extended error information.
  */
  bool GetEndpointInformation(AdbEndpointInformation* info);

  /** \brief Checks if this object is of the given type

    @param obj_type[in] One of the AdbObjectType types to check
    @return 'true' is this object type matches obj_type and 'false' otherwise.
  */
  virtual bool IsObjectOfType(AdbObjectType obj_type) const;

  // This is a helper for extracting object from the AdbObjectHandleMap
  static AdbObjectType Type() {
    return AdbObjectTypeEndpoint;
  }
};

#endif  // ANDROID_USB_API_ADB_ENDPOINT_OBJECT_H__

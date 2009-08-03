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

#ifndef ANDROID_USB_ADB_API_PRIVATE_DEFINES_H__
#define ANDROID_USB_ADB_API_PRIVATE_DEFINES_H__
/** \file
  This file consists of private definitions used inside the API
*/

#include "adb_api.h"

/** \brief Encapsulates an entry in the array of enumerated interfaces.
*/
class AdbInstanceEnumEntry {
 public:
  /** \brief Constructs an empty object.
  */
  AdbInstanceEnumEntry()
      : flags_(0) {
    ZeroMemory(&class_id_, sizeof(class_id_));
  }

  /** \brief Copy constructor
  */
  AdbInstanceEnumEntry(const AdbInstanceEnumEntry& proto) {
    Set(proto.device_name().c_str(), proto.class_id(), proto.flags());
  }

  /** \brief Constructs the object with parameters.
  */
  AdbInstanceEnumEntry(const wchar_t* dev_name, GUID cls_id, DWORD flgs) {
    Set(dev_name, cls_id, flgs);
  }

  /** \brief Destructs the object.
  */
  ~AdbInstanceEnumEntry() {
  }

  /// Operator =
  AdbInstanceEnumEntry& operator=(const AdbInstanceEnumEntry& proto) {
    Set(proto.device_name().c_str(), proto.class_id(), proto.flags());
    return *this;
  }

  /// Initializes instance with parameters
  void Set(const wchar_t* dev_name, GUID cls_id, DWORD flgs) {
    device_name_ = dev_name;
    class_id_ = cls_id;
    flags_ = flgs;
  }

  /// Calculates memory size needed to save this entry into AdbInterfaceInfo
  /// structure
  ULONG GetFlatSize() const {
    return static_cast<ULONG>(FIELD_OFFSET(AdbInterfaceInfo, device_name) +
                              (device_name_.length() + 1) * sizeof(wchar_t));
  }

  /** \brief Saves this entry into AdbInterfaceInfo structure.

    @param[in] info Buffer to save this entry to. Must be big enough to fit it.
           Use GetFlatSize() method to get buffer size needed for that.

  */
  void Save(AdbInterfaceInfo* info) const {
    info->class_id = class_id();
    info->flags = flags();
    wcscpy(info->device_name, device_name().c_str());
  }

  /// Gets interface's device name
  const std::wstring& device_name() const {
    return device_name_;
  }

  /// Gets inteface's class id
  GUID class_id() const {
    return class_id_;
  }

  /// Gets interface flags
  DWORD flags() const {
    return flags_;
  }

 private:
  /// Inteface's class id (see SP_DEVICE_INTERFACE_DATA)
  GUID          class_id_;

  /// Interface's device name
  std::wstring  device_name_;

  /// Interface flags (see SP_DEVICE_INTERFACE_DATA)
  DWORD         flags_;
};

/// Defines array of enumerated interface entries
typedef std::vector< AdbInstanceEnumEntry > AdbEnumInterfaceArray;

#endif  // ANDROID_USB_ADB_API_PRIVATE_DEFINES_H__

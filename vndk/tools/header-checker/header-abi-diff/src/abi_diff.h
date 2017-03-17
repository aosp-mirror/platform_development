// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "abi_diff_wrappers.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#include "proto/abi_diff.pb.h"
#pragma clang diagnostic pop

#include <memory>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>


class HeaderAbiDiff {
 public:
  enum Status {
    COMPATIBLE = 1 << 0,
    EXTENSION = 1 << 1,
    INCOMPATIBLE = 1 << 2,
  };


  HeaderAbiDiff(const std::string &old_dump, const std::string &new_dump,
                const std::string &compatibility_report)
      : old_dump_(old_dump), new_dump_(new_dump), cr_(compatibility_report) { }

  Status GenerateCompatibilityReport();

 private:
  Status CompareTUs(const abi_dump::TranslationUnit &old_tu,
                    const abi_dump::TranslationUnit &new_tu);
  // Collect* methods fill in the diff_tu.
  template <typename T, typename TDiff>
  static Status Collect(
      google::protobuf::RepeatedPtrField<T> *elements_added,
      google::protobuf::RepeatedPtrField<T> *elements_removed,
      google::protobuf::RepeatedPtrField<TDiff> *elements_diff,
      const google::protobuf::RepeatedPtrField<T> &old_srcs,
      const google::protobuf::RepeatedPtrField<T> &new_srcs);

  template <typename T>
  static inline void AddToMap(std::map<std::string, const T *> *dst,
                              const google::protobuf::RepeatedPtrField<T> &src);

  template <typename T>
  static bool PopulateRemovedElements(
      google::protobuf::RepeatedPtrField<T> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map);

  template <typename T, typename TDiff>
  static bool PopulateCommonElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map);

  template <typename T, typename TDiff>
  static bool DumpDiffElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      std::vector<std::pair<const T *, const T *>> &pairs);

  template <typename T>
  static bool DumpLoneElements(google::protobuf::RepeatedPtrField<T> *dst,
                               std::vector<const T *> &elements);

 private:
  const std::string &old_dump_;
  const std::string &new_dump_;
  const std::string &cr_;
};

typedef HeaderAbiDiff::Status Status;

template <typename T>
inline void HeaderAbiDiff::AddToMap(
    std::map<std::string, const T *> *dst,
    const google::protobuf::RepeatedPtrField<T> &src) {
  for (auto &&element : src) {
    dst->insert(std::make_pair(element.basic_abi().linker_set_key(), &element));
  }
}

static inline Status operator|(Status f, Status s) {
  return static_cast<Status>(
      static_cast<std::underlying_type<Status>::type>(f) |
      static_cast<std::underlying_type<Status>::type>(s));
}

static inline Status operator&(Status f, Status s) {
  return static_cast<Status>(
      static_cast<std::underlying_type<Status>::type>(f) &
      static_cast<std::underlying_type<Status>::type>(s));
}

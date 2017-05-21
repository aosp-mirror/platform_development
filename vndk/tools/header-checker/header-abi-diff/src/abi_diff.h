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


typedef abi_diff::CompatibilityStatus CompatibilityStatus;

class HeaderAbiDiff {
 public:
  HeaderAbiDiff(const std::string &lib_name, const std::string &arch,
                const std::string &old_dump, const std::string &new_dump,
                const std::string &compatibility_report,
                const std::set<std::string> &ignored_symbols)
      : lib_name_(lib_name), arch_(arch), old_dump_(old_dump),
        new_dump_(new_dump), cr_(compatibility_report),
      ignored_symbols_(ignored_symbols) { }

  CompatibilityStatus GenerateCompatibilityReport();

 private:
  CompatibilityStatus CompareTUs(const abi_dump::TranslationUnit &old_tu,
                                 const abi_dump::TranslationUnit &new_tu);
  // Collect* methods fill in the diff_tu.
  template <typename T, typename TDiff>
  static CompatibilityStatus Collect(
      google::protobuf::RepeatedPtrField<T> *elements_added,
      google::protobuf::RepeatedPtrField<T> *elements_removed,
      google::protobuf::RepeatedPtrField<TDiff> *elements_diff,
      const google::protobuf::RepeatedPtrField<T> &old_srcs,
      const google::protobuf::RepeatedPtrField<T> &new_srcs,
      const std::set<std::string> &ignored_symbols);

  template <typename T>
  static inline void AddToMap(std::map<std::string, const T *> *dst,
                              const google::protobuf::RepeatedPtrField<T> &src);

  template <typename T>
  static bool PopulateRemovedElements(
      google::protobuf::RepeatedPtrField<T> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map,
      const std::set<std::string> &ignored_symbols);

  template <typename T, typename TDiff>
  static bool PopulateCommonElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map,
      const std::set<std::string> &ignored_symbols);

  template <typename T, typename TDiff>
  static bool DumpDiffElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      std::vector<std::pair<const T *, const T *>> &pairs,
      const std::set<std::string> &ignored_symbols);

  template <typename T>
  static bool DumpLoneElements(google::protobuf::RepeatedPtrField<T> *dst,
                               std::vector<const T *> &elements,
                               const std::set<std::string> &ignored_symbols);

 private:
  const std::string &lib_name_;
  const std::string &arch_;
  const std::string &old_dump_;
  const std::string &new_dump_;
  const std::string &cr_;
  const std::set<std::string> &ignored_symbols_;
};

template <typename T>
inline void HeaderAbiDiff::AddToMap(
    std::map<std::string, const T *> *dst,
    const google::protobuf::RepeatedPtrField<T> &src) {
  for (auto &&element : src) {
    dst->insert(std::make_pair(element.basic_abi().linker_set_key(), &element));
  }
}

static inline CompatibilityStatus operator|(CompatibilityStatus f,
                                            CompatibilityStatus s) {
  return static_cast<CompatibilityStatus>(
      static_cast<std::underlying_type<CompatibilityStatus>::type>(f) |
      static_cast<std::underlying_type<CompatibilityStatus>::type>(s));
}

static inline CompatibilityStatus operator&(
    CompatibilityStatus f, CompatibilityStatus s) {
  return static_cast<CompatibilityStatus>(
      static_cast<std::underlying_type<CompatibilityStatus>::type>(f) &
      static_cast<std::underlying_type<CompatibilityStatus>::type>(s));
}

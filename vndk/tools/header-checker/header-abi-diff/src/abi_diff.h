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
  Status CollectRecords(abi_diff::TranslationUnitDiff *abi_diff,
                        const abi_dump::TranslationUnit &old_tu,
                        const abi_dump::TranslationUnit &new_tu);

  Status CollectFunctions(abi_diff::TranslationUnitDiff *abi_diff,
                          const abi_dump::TranslationUnit &old_tu,
                          const abi_dump::TranslationUnit &new_tu);

  Status CollectEnums(abi_diff::TranslationUnitDiff *abi_diff,
                      const abi_dump::TranslationUnit &old_tu,
                      const abi_dump::TranslationUnit &new_tu);


  template <typename T>
  inline void AddToMap(std::map<std::string, const T *> *dst,
                       const google::protobuf::RepeatedPtrField<T> &src);

  template <typename T>
  bool PopulateRemovedElements(
      google::protobuf::RepeatedPtrField<T> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map) const;

  template <typename T, typename TDiff>
  bool PopulateCommonElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map) const;

  template <typename T, typename TDiff>
  bool DumpDiffElements(
      google::protobuf::RepeatedPtrField<TDiff> *dst,
      std::vector<std::pair<const T *, const T *>> &pairs) const;

  template <typename T>
  bool DumpLoneElements(google::protobuf::RepeatedPtrField<T> *dst,
                        std::vector<const T *> &elements) const;

 private:
  const std::string &old_dump_;
  const std::string &new_dump_;
  const std::string &cr_;

  // HashMaps for the old tu abis
  std::map<std::string, const abi_dump::RecordDecl *> old_dump_records_;
  std::map<std::string, const abi_dump::FunctionDecl *> old_dump_functions_;
  std::map<std::string, const abi_dump::EnumDecl *> old_dump_enums_;

  // HashMaps for the new tu abis
  std::map<std::string, const abi_dump::RecordDecl *> new_dump_records_;
  std::map<std::string, const abi_dump::FunctionDecl *> new_dump_functions_;
  std::map<std::string, const abi_dump::EnumDecl *> new_dump_enums_;
};

typedef HeaderAbiDiff::Status Status;

template <typename T>
inline void HeaderAbiDiff::AddToMap(
    std::map<std::string, const T *> *dst,
    const google::protobuf::RepeatedPtrField<T> &src) {
  for (auto &&element : src) {
    dst->insert(std::make_pair(element.linker_set_key(), &element));
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

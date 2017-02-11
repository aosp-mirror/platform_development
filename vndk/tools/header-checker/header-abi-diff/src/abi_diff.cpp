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

#include "abi_diff.h"

#include <llvm/Support/raw_ostream.h>

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <memory>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include <stdlib.h>

Status HeaderAbiDiff::GenerateCompatibilityReport() {
  abi_dump::TranslationUnit old_tu;
  abi_dump::TranslationUnit new_tu;
  std::ifstream old_input(old_dump_);
  std::ifstream new_input(new_dump_);
  google::protobuf::io::IstreamInputStream text_iso(&old_input);
  google::protobuf::io::IstreamInputStream text_isn(&new_input);

  if (!google::protobuf::TextFormat::Parse(&text_iso, &old_tu) ||
      !google::protobuf::TextFormat::Parse(&text_isn, &new_tu)) {
    llvm::errs() << "Failed to Parse Input\n";
    ::exit(1);
  }
   return CompareTUs(old_tu, new_tu);
}

Status HeaderAbiDiff::CompareTUs(const abi_dump::TranslationUnit &old_tu,
                                 const abi_dump::TranslationUnit &new_tu) {
  abi_diff::TranslationUnitDiff diff_tu;
  Status record_Status = CollectRecords(&diff_tu, old_tu, new_tu);
  Status function_Status = CollectFunctions(&diff_tu, old_tu, new_tu);
  Status enum_Status = CollectEnums(&diff_tu, old_tu, new_tu);

  Status combined_Status = record_Status | function_Status | enum_Status;

  std::ofstream text_output(cr_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);

  if(!google::protobuf::TextFormat::Print(diff_tu, &text_os)) {
    llvm::errs() << "Unable to dump report\n";
    ::exit(1);
  }
  if (combined_Status & INCOMPATIBLE) {
    return INCOMPATIBLE;
  }
  if (combined_Status & EXTENSION) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

Status HeaderAbiDiff::CollectRecords(abi_diff::TranslationUnitDiff *diff_tu,
                                     const abi_dump::TranslationUnit &old_tu,
                                     const abi_dump::TranslationUnit &new_tu) {
  AddToMap(&old_dump_records_, old_tu.records());
  AddToMap(&new_dump_records_, new_tu.records());

  if (!PopulateRemovedElements(diff_tu->mutable_records_removed(),
                               old_dump_records_, new_dump_records_) ||
      !PopulateRemovedElements(diff_tu->mutable_records_removed(),
                               new_dump_records_, old_dump_records_) ||
      !PopulateCommonElements(diff_tu->mutable_records_diff(),old_dump_records_,
                              new_dump_records_)) {
    llvm::errs() << "Populating records in report failed\n";
    ::exit(1);
  }
  if (diff_tu->records_diff().size() || diff_tu->records_removed().size()) {
    return INCOMPATIBLE;
  }
  if (diff_tu->records_added().size()) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

Status HeaderAbiDiff::CollectFunctions(
    abi_diff::TranslationUnitDiff *diff_tu,
    const abi_dump::TranslationUnit &old_tu,
    const abi_dump::TranslationUnit &new_tu) {
  AddToMap(&old_dump_functions_, old_tu.functions());
  AddToMap(&new_dump_functions_, new_tu.functions());

  if (!PopulateRemovedElements(diff_tu->mutable_functions_removed(),
                               old_dump_functions_, new_dump_functions_) ||
      !PopulateRemovedElements(diff_tu->mutable_functions_added(),
                               new_dump_functions_, old_dump_functions_)) {
    llvm::errs() << "Populating functions in report failed\n";
    ::exit(1);
  }
  if (diff_tu->functions_removed().size()) {
    return INCOMPATIBLE;
  }
  if (diff_tu->functions_added().size()) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

Status HeaderAbiDiff::CollectEnums(abi_diff::TranslationUnitDiff *diff_tu,
                                   const abi_dump::TranslationUnit &old_tu,
                                   const abi_dump::TranslationUnit &new_tu) {
  AddToMap(&old_dump_enums_, old_tu.enums());
  AddToMap(&new_dump_enums_, new_tu.enums());

  if (!PopulateRemovedElements(diff_tu->mutable_enums_removed(),
                               old_dump_enums_, new_dump_enums_) ||
      !PopulateRemovedElements(diff_tu->mutable_enums_added(), new_dump_enums_,
                               old_dump_enums_) ||
      !PopulateCommonElements(diff_tu->mutable_enums_diff(),old_dump_enums_,
                              new_dump_enums_)) {
    llvm::errs() << "Populating enums in report failed\n";
    ::exit(1);
  }
  if (diff_tu->enums_removed().size() || diff_tu->enums_diff().size()) {
    return INCOMPATIBLE;
  }
  if (diff_tu->enums_added().size()) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

template <typename T>
bool HeaderAbiDiff::PopulateRemovedElements(
    google::protobuf::RepeatedPtrField<T> *dst,
    const std::map<std::string, const T*> &old_elements_map,
    const std::map<std::string, const T*> &new_elements_map) const {

  std::vector<const T *> removed_elements;
  for (auto &&map_element : old_elements_map) {
      const T *element = map_element.second;
      auto new_element = new_elements_map.find(element->linker_set_key());
      if (new_element == new_elements_map.end()) {
        removed_elements.emplace_back(element);
      }
  }
  if (!DumpLoneElements(dst, removed_elements)) {
    llvm::errs() << "Dumping added / removed element to report failed\n";
    return false;
  }
  return true;
}

template <typename T, typename TDiff>
bool HeaderAbiDiff::PopulateCommonElements(
    google::protobuf::RepeatedPtrField<TDiff> *dst,
    const std::map<std::string, const T *> &old_elements_map,
    const std::map<std::string, const T *> &new_elements_map) const {
  std::vector<std::pair<const T *, const T *>> common_elements;
  typename std::map<std::string, const T *>::const_iterator old_element =
      old_elements_map.begin();
  typename std::map<std::string, const T *>::const_iterator new_element =
      new_elements_map.begin();
  while (old_element != old_elements_map.end() &&
         new_element != new_elements_map.end()) {
    if (old_element->first == new_element->first) {
      common_elements.emplace_back(std::make_pair(
          old_element->second, new_element->second));
      old_element++;
      new_element++;
      continue;
    }
    if (old_element->first < new_element->first) {
      old_element++;
    } else {
      new_element++;
    }
  }
  if (!DumpDiffElements(dst, common_elements)) {
    llvm::errs() << "Dumping difference in common element to report failed\n";
    return false;
  }
  return true;
}

template <typename T>
bool HeaderAbiDiff::DumpLoneElements(google::protobuf::RepeatedPtrField<T> *dst,
                                     std::vector<const T *> &elements) const {
  for (auto &&element : elements) {
    T *added_element = dst->Add();
    if (!added_element) {
      llvm::errs() << "Adding element diff failed\n";
      return false;
    }
    *added_element = *element;
  }
  return true;
}

template <typename T, typename TDiff>
bool HeaderAbiDiff::DumpDiffElements(
    google::protobuf::RepeatedPtrField<TDiff>  *dst,
    std::vector<std::pair<const T *,const T *>> &pairs) const {
  for (auto &&pair : pairs) {
    const T *old_element = pair.first;
    const T *new_element = pair.second;
    // Not having inheritance from protobuf messages makes this
    // restrictive code.
    abi_diff_wrappers::DiffWrapper<T, TDiff> diff_wrapper(old_element,
                                                          new_element);
    std::unique_ptr<TDiff> decl_diff_ptr = diff_wrapper.Get();
    if (!decl_diff_ptr) {
      continue;
    }
    TDiff *added_element_diff = dst->Add();
    if (!added_element_diff) {
      llvm::errs() << "Adding element diff failed\n";
      return false;
    }
    *added_element_diff = *decl_diff_ptr;
  }
  return true;
}

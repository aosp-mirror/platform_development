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
  std::unique_ptr<abi_diff::TranslationUnitDiff> diff_tu(
      new abi_diff::TranslationUnitDiff);

  Status record_status = Collect<abi_dump::RecordDecl>(
      diff_tu->mutable_records_added(), diff_tu->mutable_records_removed(),
      diff_tu->mutable_records_diff(), old_tu.records(), new_tu.records());

  Status function_status = Collect<abi_dump::FunctionDecl>(
      diff_tu->mutable_functions_added(), diff_tu->mutable_functions_removed(),
      diff_tu->mutable_functions_diff(), old_tu.functions(),
      new_tu.functions());

  Status enum_status = Collect<abi_dump::EnumDecl>(
      diff_tu->mutable_enums_added(), diff_tu->mutable_enums_removed(),
      diff_tu->mutable_enums_diff(), old_tu.enums(), new_tu.enums());

  Status global_var_status = Collect<abi_dump::GlobalVarDecl>(
      diff_tu->mutable_global_vars_added(),
      diff_tu->mutable_global_vars_removed(),
      diff_tu->mutable_global_vars_diff(), old_tu.global_vars(),
      new_tu.global_vars());

  Status combined_status =
      record_status | function_status | enum_status | global_var_status;

  std::ofstream text_output(cr_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);

  if(!google::protobuf::TextFormat::Print(*diff_tu, &text_os)) {
    llvm::errs() << "Unable to dump report\n";
    ::exit(1);
  }
  if (combined_status & INCOMPATIBLE) {
    return INCOMPATIBLE;
  }
  if (combined_status & EXTENSION) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

template <typename T, typename TDiff>
Status HeaderAbiDiff::Collect(
    google::protobuf::RepeatedPtrField<T> *elements_added,
    google::protobuf::RepeatedPtrField<T> *elements_removed,
    google::protobuf::RepeatedPtrField<TDiff> *elements_diff,
    const google::protobuf::RepeatedPtrField<T> &old_srcs,
    const google::protobuf::RepeatedPtrField<T> &new_srcs) {
  assert(elements_added != nullptr);
  assert(elements_removed != nullptr);
  assert(elements_diff != nullptr);

  std::map<std::string, const T*> old_elements_map;
  std::map<std::string, const T*> new_elements_map;
  AddToMap(&old_elements_map, old_srcs);
  AddToMap(&new_elements_map, new_srcs);

  if (!PopulateRemovedElements(elements_removed, old_elements_map,
                               new_elements_map) ||
      !PopulateRemovedElements(elements_added, new_elements_map,
                               old_elements_map) ||
      !PopulateCommonElements(elements_diff, old_elements_map,
                              new_elements_map)) {
    llvm::errs() << "Populating functions in report failed\n";
    ::exit(1);
  }
  if (elements_diff->size() || elements_removed->size()) {
    return INCOMPATIBLE;
  }
  if (elements_added->size()) {
    return EXTENSION;
  }
  return COMPATIBLE;
}

template <typename T>
bool HeaderAbiDiff::PopulateRemovedElements(
    google::protobuf::RepeatedPtrField<T> *dst,
    const std::map<std::string, const T*> &old_elements_map,
    const std::map<std::string, const T*> &new_elements_map) {

  std::vector<const T *> removed_elements;
  for (auto &&map_element : old_elements_map) {
      const T *element = map_element.second;
      auto new_element =
          new_elements_map.find(element->basic_abi().linker_set_key());
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
    const std::map<std::string, const T *> &new_elements_map) {
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
                                     std::vector<const T *> &elements) {
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
    std::vector<std::pair<const T *,const T *>> &pairs) {
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

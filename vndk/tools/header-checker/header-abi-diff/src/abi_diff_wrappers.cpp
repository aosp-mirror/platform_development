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

#include <llvm/Support/raw_ostream.h>

using abi_diff::RecordDeclDiff;
using abi_diff::FieldDeclDiff;
using abi_diff::CXXBaseSpecifierDiff;
using abi_diff::EnumDeclDiff;
using abi_diff::EnumFieldDeclDiff;
using abi_dump::RecordDecl;
using abi_dump::EnumDecl;

namespace abi_diff_wrappers {

// This function fills in a *Diff Message's repeated field. For eg:
// RecordDeclDiff's CXXBaseSpecifierDiff fields and well as FieldDeclDiff
// fields.
template <typename T, typename TDiff>
template <typename Element, typename ElementDiff>
bool DiffWrapperBase<T, TDiff>::GetElementDiffs(
    google::protobuf::RepeatedPtrField<ElementDiff> *dst,
    const google::protobuf::RepeatedPtrField<Element> &old_elements,
    const google::protobuf::RepeatedPtrField<Element> &new_elements) {
  bool differs = false;
  assert (dst != nullptr);
  int i = 0;
  int j = 0;
  while (i < old_elements.size() && j < new_elements.size()) {
    const Element &old_element = old_elements.Get(i);
    const Element &new_element = new_elements.Get(i);
    if (old_element.linker_set_key() != new_element.linker_set_key()) {
      ElementDiff *diff = dst->Add();
      Element *old_elementp = nullptr;
      Element *new_elementp = nullptr;
      if (!diff || !(old_elementp = diff->mutable_old()) ||
          !(new_elementp = diff->mutable_new_())) {
        llvm::errs() << "Failed to add diff element\n";
        ::exit(1);
      }
      *old_elementp = old_element;
      *new_elementp = new_element;
      diff->set_index(i);
      differs = true;
    }
    i++;
    j++;
  }
  if (old_elements.size() != new_elements.size()) {
    GetExtraElementDiffs(dst, i, j, old_elements, new_elements);
    differs = true;
  }
  return differs;
}

template <typename T, typename TDiff>
template <typename Element, typename ElementDiff>
void DiffWrapperBase<T, TDiff>::GetExtraElementDiffs(
    google::protobuf::RepeatedPtrField<ElementDiff> *dst, int i, int j,
    const google::protobuf::RepeatedPtrField<Element> &old_elements,
    const google::protobuf::RepeatedPtrField<Element> &new_elements) {
 assert(dst != nullptr);
 while (i < old_elements.size()) {
    const Element &old_element = old_elements.Get(i);
    ElementDiff *diff = dst->Add();
    Element *old_elementp = nullptr;
    if (!diff || !(old_elementp = diff->mutable_old())) {
      llvm::errs() << "Failed to add diff element\n";
      ::exit(1);
    }
    *old_elementp = old_element;
    diff->set_index(i);
    i++;
 }
 while (j < new_elements.size()) {
    const Element &new_element = new_elements.Get(j);
    ElementDiff *diff = dst->Add();
    Element *new_elementp = nullptr;
    if (!diff || !(new_elementp = diff->mutable_new_())) {
      llvm::errs() << "Failed to add diff element\n";
      ::exit(1);
    }
    *new_elementp = new_element;
    diff->set_index(j);
    j++;
 }
}

template <>
std::unique_ptr<RecordDeclDiff> DiffWrapper<RecordDecl, RecordDeclDiff>::Get() {
  std::unique_ptr<RecordDeclDiff> record_diff(new RecordDeclDiff());
  assert(oldp_->fully_qualified_name() == newp_->fully_qualified_name());
  record_diff->set_name(oldp_->fully_qualified_name());
  google::protobuf::RepeatedPtrField<FieldDeclDiff> *fdiffs =
      record_diff->mutable_field_diffs();
  google::protobuf::RepeatedPtrField<CXXBaseSpecifierDiff> *bdiffs =
      record_diff->mutable_base_diffs();
  assert(fdiffs != nullptr && bdiffs != nullptr);
  // Template Information isn't diffed since the linker_set_key includes the
  // mangled name which includes template information.
  if (GetElementDiffs(fdiffs, oldp_->fields(), newp_->fields()) ||
      GetElementDiffs(bdiffs, oldp_->base_specifiers(),
                      newp_->base_specifiers())) {
    return record_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<EnumDeclDiff> DiffWrapper<EnumDecl, EnumDeclDiff>::Get() {
  std::unique_ptr<EnumDeclDiff> enum_diff(new EnumDeclDiff());
  assert(oldp_->enum_name() == newp_->enum_name());
  google::protobuf::RepeatedPtrField<EnumFieldDeclDiff> *fdiffs =
      enum_diff->mutable_field_diffs();
  assert(fdiffs != nullptr);
  if (GetElementDiffs(fdiffs, oldp_->enum_fields(), newp_->enum_fields()) ||
      (oldp_->enum_type() != newp_->enum_type())) {
    return enum_diff;
  }
  return nullptr;
}

} // abi_diff_wrappers

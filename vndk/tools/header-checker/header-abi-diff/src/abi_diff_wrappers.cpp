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
using abi_diff::RecordFieldDeclDiff;
using abi_diff::CXXBaseSpecifierDiff;
using abi_diff::CXXVTableDiff;
using abi_diff::EnumDeclDiff;
using abi_diff::ReturnTypeDiff;
using abi_diff::FunctionDeclDiff;
using abi_diff::EnumDeclDiff;
using abi_diff::EnumFieldDeclDiff;
using abi_diff::GlobalVarDeclDiff;
using abi_dump::RecordDecl;
using abi_dump::RecordFieldDecl;
using abi_dump::EnumDecl;
using abi_dump::EnumFieldDecl;
using abi_dump::FunctionDecl;
using abi_dump::ParamDecl;
using abi_dump::VTableComponent;
using abi_dump::CXXBaseSpecifier;
using abi_dump::GlobalVarDecl;

namespace abi_diff_wrappers {

static bool IsAccessDownGraded(abi_dump::AccessSpecifier old_access,
                               abi_dump::AccessSpecifier new_access) {
  bool access_downgraded = false;
  switch (old_access) {
    case abi_dump::AccessSpecifier::protected_access:
      if (new_access == abi_dump::AccessSpecifier::private_access) {
        access_downgraded = true;
      }
      break;
    case abi_dump::AccessSpecifier::public_access:
      if (new_access != abi_dump::AccessSpecifier::public_access) {
        access_downgraded = true;
      }
      break;
    default:
      break;
  }
  return access_downgraded;
}

static bool DiffBasicTypeAbi(const abi_dump::BasicTypeAbi &old_abi,
                             const abi_dump::BasicTypeAbi &new_abi) {
  bool name_comparison = (old_abi.name() != new_abi.name());
  bool size_comparison = (old_abi.size() != new_abi.size());
  bool alignment_comparison = (old_abi.alignment() != new_abi.alignment());
  return name_comparison || size_comparison || alignment_comparison;
}

template <typename T>
static bool Diff(const T &old_element, const T &new_element) {
  // Can be specialized for future changes in the format.
  return DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                          new_element.basic_abi().type_abi()) ||
      IsAccessDownGraded(old_element.basic_abi().access(),
                         new_element.basic_abi().access());
}

template <>
bool Diff<EnumFieldDecl>(const EnumFieldDecl &old_element,
                         const EnumFieldDecl &new_element) {
  // Can be specialized for future changes in the format.
  return DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                          new_element.basic_abi().type_abi()) ||
      (old_element.enum_field_value() != new_element.enum_field_value());
}

template <>
bool Diff<CXXBaseSpecifier>(const CXXBaseSpecifier &old_element,
                            const CXXBaseSpecifier &new_element) {
  // Can be specialized for future changes in the format.
  return (DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                           new_element.basic_abi().type_abi()) ||
      old_element.basic_abi().access() != new_element.basic_abi().access() ||
      old_element.is_virtual() != new_element.is_virtual());
}

template <>
bool Diff<VTableComponent>(const VTableComponent &old_element,
                           const VTableComponent &new_element) {
  bool kind_comparison = old_element.kind() != new_element.kind();
  bool mangled_name_comparison = old_element.mangled_component_name() !=
      new_element.mangled_component_name();
  bool value_comparison = old_element.value() != new_element.value();
  return kind_comparison || mangled_name_comparison || value_comparison;
}

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
  assert(dst != nullptr);
  int i = 0;
  int j = 0;
  while (i < old_elements.size() && j < new_elements.size()) {
    const Element &old_element = old_elements.Get(i);
    const Element &new_element = new_elements.Get(i);

    if (Diff(old_element, new_element)) {
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
  assert(oldp_->basic_abi().name() == newp_->basic_abi().name());
  record_diff->set_name(oldp_->basic_abi().name());
  google::protobuf::RepeatedPtrField<RecordFieldDeclDiff> *fdiffs =
      record_diff->mutable_field_diffs();
  google::protobuf::RepeatedPtrField<CXXBaseSpecifierDiff> *bdiffs =
      record_diff->mutable_base_diffs();
  google::protobuf::RepeatedPtrField<CXXVTableDiff> *vtdiffs =
      record_diff->mutable_vtable_diffs();
  assert(fdiffs != nullptr && bdiffs != nullptr);
  // Template Information isn't diffed since the linker_set_key includes the
  // mangled name which includes template information.
  if (GetElementDiffs(fdiffs, oldp_->fields(), newp_->fields()) ||
      GetElementDiffs(bdiffs, oldp_->base_specifiers(),
                      newp_->base_specifiers()) ||
      GetElementDiffs(vtdiffs, oldp_->vtable_layout().vtable_components(),
                      newp_->vtable_layout().vtable_components())) {
    return record_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<EnumDeclDiff> DiffWrapper<EnumDecl, EnumDeclDiff>::Get() {
  std::unique_ptr<EnumDeclDiff> enum_diff(new EnumDeclDiff());
  assert(oldp_->basic_abi().name() == newp_->basic_abi().name());
  google::protobuf::RepeatedPtrField<EnumFieldDeclDiff> *fdiffs =
      enum_diff->mutable_field_diffs();
  assert(fdiffs != nullptr);
  if (GetElementDiffs(fdiffs, oldp_->enum_fields(), newp_->enum_fields())) {
    return enum_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<FunctionDeclDiff>
DiffWrapper<FunctionDecl, FunctionDeclDiff>::Get() {
  std::unique_ptr<FunctionDeclDiff> func_diff(new FunctionDeclDiff());
  if (DiffBasicTypeAbi(oldp_->basic_abi().type_abi(),
                       newp_->basic_abi().type_abi()) ||
      IsAccessDownGraded(oldp_->basic_abi().access(),
                         newp_->basic_abi().access())) {
    assert(func_diff->mutable_return_type_diffs() != nullptr &&
           func_diff->mutable_return_type_diffs()->mutable_old() != nullptr &&
           func_diff->mutable_return_type_diffs()->mutable_new_() != nullptr);
    *(func_diff->mutable_return_type_diffs()->mutable_old()) =
        oldp_->basic_abi();
    *(func_diff->mutable_return_type_diffs()->mutable_new_()) =
        newp_->basic_abi();
    return func_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<GlobalVarDeclDiff>
DiffWrapper<GlobalVarDecl, GlobalVarDeclDiff>::Get() {
  std::unique_ptr<GlobalVarDeclDiff> global_var_diff(new GlobalVarDeclDiff());
  if (DiffBasicTypeAbi(oldp_->basic_abi().type_abi(),
                       newp_->basic_abi().type_abi()) ||
      IsAccessDownGraded(oldp_->basic_abi().access(),
                         newp_->basic_abi().access())) {
    assert(global_var_diff->mutable_old() != nullptr);
    assert(global_var_diff->mutable_new_() != nullptr);
    *(global_var_diff->mutable_old()) = oldp_->basic_abi();
    *(global_var_diff->mutable_new_()) = newp_->basic_abi();
    return global_var_diff;
  }
  return nullptr;
}

} // abi_diff_wrappers

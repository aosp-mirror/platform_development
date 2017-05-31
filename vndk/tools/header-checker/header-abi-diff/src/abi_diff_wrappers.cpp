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

#include<header_abi_util.h>

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
using abi_diff::ParamDeclDiff;
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
using abi_dump::BasicNamedAndTypedDecl;

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

static std::string CpptoCAdjustment(const std::string &type) {
  std::string adjusted_type_name =
      abi_util::FindAndReplace(type, "\\bstruct ", "");

  return adjusted_type_name;
}

static bool CompareTypeNames(const abi_dump::BasicTypeAbi &old_abi,
                             const abi_dump::BasicTypeAbi &new_abi) {
  // Strip of leading 'struct' keyword from type names
  std::string old_type = old_abi.name();
  std::string new_type = new_abi.name();
  old_type = CpptoCAdjustment(old_type);
  new_type = CpptoCAdjustment(new_type);
  // TODO: Add checks for C++ built-in types vs C corresponding types.
  return old_type != new_type;
}

static bool DiffBasicTypeAbi(const abi_dump::BasicTypeAbi &old_abi,
                             const abi_dump::BasicTypeAbi &new_abi) {
  // We need to add a layer of indirection to account for issues when C and C++
  // are mixed. For example some types like wchar_t are in-built types for C++
  // but not for C. Another example would be clang reporting C structures
  // without the leading "struct" keyword when headers defining them are
  // included in C++ files.
  bool name_comparison = CompareTypeNames(old_abi, new_abi);
  bool size_comparison = (old_abi.size() != new_abi.size());
  bool alignment_comparison = (old_abi.alignment() != new_abi.alignment());
  return name_comparison || size_comparison || alignment_comparison;
}

template <typename T>
static bool Diff(const T &old_element, const T &new_element) {
  // Can be specialized for future changes in the format.
  return DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                          new_element.basic_abi().type_abi()) ||
      (old_element.basic_abi().name() != new_element.basic_abi().name()) ||
      IsAccessDownGraded(old_element.basic_abi().access(),
                         new_element.basic_abi().access());
}

template <>
bool Diff<EnumFieldDecl>(const EnumFieldDecl &old_element,
                         const EnumFieldDecl &new_element) {
  // Can be specialized for future changes in the format.
  return DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                          new_element.basic_abi().type_abi()) ||
      (old_element.enum_field_value() != new_element.enum_field_value()) ||
      (old_element.basic_abi().name() != new_element.basic_abi().name());
}

template <>
bool Diff<ParamDecl>(const ParamDecl &old_element,
                     const ParamDecl &new_element) {
  // Can be specialized for future changes in the format.
  return DiffBasicTypeAbi(old_element.basic_abi().type_abi(),
                          new_element.basic_abi().type_abi());
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

static bool DiffBasicNamedAndTypedDecl(BasicNamedAndTypedDecl *type_diff_old,
                                       BasicNamedAndTypedDecl *type_diff_new,
                                       const BasicNamedAndTypedDecl &old,
                                       const BasicNamedAndTypedDecl &new_) {
  assert(type_diff_old != nullptr);
  assert(type_diff_new != nullptr);
  if (DiffBasicTypeAbi(old.type_abi(), new_.type_abi()) ||
      IsAccessDownGraded(old.access(), new_.access())) {
    *(type_diff_old) = old;
    *(type_diff_new) = new_;
    return true;
  }
  return false;
}

template <>
std::unique_ptr<RecordDeclDiff>
DiffWrapper<RecordDecl, RecordDeclDiff>::Get() {
  std::unique_ptr<RecordDeclDiff> record_diff(new RecordDeclDiff());
  assert(oldp_->mangled_record_name() ==
         newp_->mangled_record_name());
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
                      newp_->vtable_layout().vtable_components()) ||
      DiffBasicNamedAndTypedDecl(
          record_diff->mutable_type_diff()->mutable_old(),
          record_diff->mutable_type_diff()->mutable_new_(),
          oldp_->basic_abi(), newp_->basic_abi())) {
    return record_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<EnumDeclDiff>
DiffWrapper<EnumDecl, EnumDeclDiff>::Get() {
  std::unique_ptr<EnumDeclDiff> enum_diff(new EnumDeclDiff());
  assert(oldp_->basic_abi().linker_set_key() ==
         newp_->basic_abi().linker_set_key());
  google::protobuf::RepeatedPtrField<EnumFieldDeclDiff> *fdiffs =
      enum_diff->mutable_field_diffs();
  assert(fdiffs != nullptr);
  enum_diff->set_name(oldp_->basic_abi().name());
  if (GetElementDiffs(fdiffs, oldp_->enum_fields(), newp_->enum_fields()) ||
      DiffBasicNamedAndTypedDecl(
          enum_diff->mutable_type_diff()->mutable_old(),
          enum_diff->mutable_type_diff()->mutable_new_(),
          oldp_->basic_abi(), newp_->basic_abi())) {
    return enum_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<FunctionDeclDiff>
DiffWrapper<FunctionDecl, FunctionDeclDiff>::Get() {
  std::unique_ptr<FunctionDeclDiff> func_diff(new FunctionDeclDiff());
  google::protobuf::RepeatedPtrField<ParamDeclDiff> *pdiffs =
      func_diff->mutable_param_diffs();
  assert(func_diff->mutable_return_type_diffs() != nullptr);
  func_diff->set_name(oldp_->basic_abi().linker_set_key());
  if (DiffBasicNamedAndTypedDecl(
          func_diff->mutable_return_type_diffs()->mutable_old(),
          func_diff->mutable_return_type_diffs()->mutable_new_(),
          oldp_->basic_abi(), newp_->basic_abi()) ||
      GetElementDiffs(pdiffs, oldp_->parameters(), newp_->parameters())) {
    return func_diff;
  }
  return nullptr;
}

template <>
std::unique_ptr<GlobalVarDeclDiff>
DiffWrapper<GlobalVarDecl, GlobalVarDeclDiff>::Get() {
  std::unique_ptr<GlobalVarDeclDiff> global_var_diff(new GlobalVarDeclDiff());
  assert(global_var_diff->mutable_type_diff() != nullptr);
  if (DiffBasicNamedAndTypedDecl(
          global_var_diff->mutable_type_diff()->mutable_old(),
          global_var_diff->mutable_type_diff()->mutable_new_(),
          oldp_->basic_abi(), newp_->basic_abi())) {
    return global_var_diff;
  }
  return nullptr;
}

} // abi_diff_wrappers

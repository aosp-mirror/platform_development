// Copyright (C) 2019 The Android Open Source Project
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

#include "repr/protobuf/converter.h"

#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <iostream>
#include <memory>
#include <string>


namespace header_checker {
namespace repr {


bool IRDiffToProtobufConverter::AddTypeInfoDiff(
    abi_diff::TypeInfoDiff *type_info_diff_protobuf,
    const TypeDiffIR *type_diff_ir) {
  abi_diff::TypeInfo *old_type_info_protobuf =
      type_info_diff_protobuf->mutable_old_type_info();
  abi_diff::TypeInfo *new_type_info_protobuf =
      type_info_diff_protobuf->mutable_new_type_info();
  if (old_type_info_protobuf == nullptr || new_type_info_protobuf == nullptr) {
    return false;
  }
  const std::pair<uint64_t, uint64_t> &sizes = type_diff_ir->GetSizes();
  const std::pair<uint32_t, uint32_t> &alignments =
      type_diff_ir->GetAlignments();
  old_type_info_protobuf->set_size(sizes.first);
  new_type_info_protobuf->set_size(sizes.second);

  old_type_info_protobuf->set_alignment(alignments.first);
  new_type_info_protobuf->set_alignment(alignments.second);
  return true;
}

bool IRDiffToProtobufConverter::AddVTableLayoutDiff(
    abi_diff::VTableLayoutDiff *vtable_layout_diff_protobuf,
    const VTableLayoutDiffIR *vtable_layout_diff_ir) {
  abi_dump:: VTableLayout *old_vtable =
      vtable_layout_diff_protobuf->mutable_old_vtable();
  abi_dump:: VTableLayout *new_vtable =
      vtable_layout_diff_protobuf->mutable_new_vtable();
  if (old_vtable == nullptr || new_vtable == nullptr ||
      !SetIRToProtobufVTableLayout(old_vtable,
                                   vtable_layout_diff_ir->GetOldVTable()) ||
      !SetIRToProtobufVTableLayout(new_vtable,
                                   vtable_layout_diff_ir->GetNewVTable())) {
    return false;
  }
  return true;
}

template <typename T>
static bool CopyBaseSpecifiersDiffIRToProtobuf(
    google::protobuf::RepeatedPtrField<T> *dst,
    const std::vector<CXXBaseSpecifierIR> &bases_ir) {
  for (auto &&base_ir : bases_ir) {
    T *added_base = dst->Add();
    if (!SetIRToProtobufBaseSpecifier(added_base, base_ir)) {
      return false;
    }
  }
  return true;
}

bool IRDiffToProtobufConverter::AddBaseSpecifierDiffs(
    abi_diff::CXXBaseSpecifierDiff *base_specifiers_diff_protobuf,
    const CXXBaseSpecifierDiffIR *base_specifiers_diff_ir) {
  if (!CopyBaseSpecifiersDiffIRToProtobuf(
          base_specifiers_diff_protobuf->mutable_old_bases(),
          base_specifiers_diff_ir->GetOldBases()) ||
      !CopyBaseSpecifiersDiffIRToProtobuf(
          base_specifiers_diff_protobuf->mutable_new_bases(),
          base_specifiers_diff_ir->GetNewBases())) {
    return false;
  }
  return true;
}

bool IRDiffToProtobufConverter::AddRecordFields(
    abi_diff::RecordTypeDiff *record_diff_protobuf,
    const std::vector<const RecordFieldIR *> &record_fields_ir,
    bool field_removed) {
  for (auto &&record_field_ir : record_fields_ir) {
    abi_dump::RecordFieldDecl *field = nullptr;
    if (field_removed) {
      field = record_diff_protobuf->add_fields_removed();
    } else {
      field = record_diff_protobuf->add_fields_added();
    }
    if (field == nullptr) {
      return false;
    }
    SetIRToProtobufRecordField(field, record_field_ir);
  }
  return true;
}

bool IRDiffToProtobufConverter::AddRecordFieldDiffs(
    abi_diff::RecordTypeDiff *record_diff_protobuf,
    const std::vector<RecordFieldDiffIR> &record_field_diffs_ir) {
  for (auto &&record_field_diff_ir : record_field_diffs_ir) {
    abi_diff::RecordFieldDeclDiff *record_field_diff =
        record_diff_protobuf->add_fields_diff();
    if (record_field_diff == nullptr) {
      return false;
    }
    abi_dump::RecordFieldDecl *old_field =
        record_field_diff->mutable_old_field();
    abi_dump::RecordFieldDecl *new_field =
        record_field_diff->mutable_new_field();
    if (old_field == nullptr || new_field == nullptr) {
      return false;
    }
    SetIRToProtobufRecordField(old_field,
                               record_field_diff_ir.GetOldField());
    SetIRToProtobufRecordField(new_field,
                               record_field_diff_ir.GetNewField());
  }
  return true;
}

abi_diff::RecordTypeDiff IRDiffToProtobufConverter::ConvertRecordTypeDiffIR(
    const RecordTypeDiffIR *record_type_diff_ir) {
  abi_diff::RecordTypeDiff record_type_diff_protobuf;
  record_type_diff_protobuf.set_name(record_type_diff_ir->GetName());
  const TypeDiffIR *type_diff_ir = record_type_diff_ir->GetTypeDiff();
  // If a type_info diff exists
  if (type_diff_ir != nullptr) {
    abi_diff::TypeInfoDiff *type_info_diff =
        record_type_diff_protobuf.mutable_type_info_diff();
    if (!AddTypeInfoDiff(type_info_diff, type_diff_ir)) {
      llvm::errs() << "RecordType could not be converted\n";
      ::exit(1);
    }
  }
  // If vtables differ.
  const VTableLayoutDiffIR *vtable_layout_diff_ir =
      record_type_diff_ir->GetVTableLayoutDiff();
  if (vtable_layout_diff_ir != nullptr) {
    abi_diff::VTableLayoutDiff *vtable_layout_diff_protobuf =
        record_type_diff_protobuf.mutable_vtable_layout_diff();
    if (!AddVTableLayoutDiff(vtable_layout_diff_protobuf,
                             vtable_layout_diff_ir)) {
      llvm::errs() << "VTable layout diff could not be added\n";
      ::exit(1);
    }
  }
  // If base specifiers differ.
  const CXXBaseSpecifierDiffIR *base_specifier_diff_ir =
      record_type_diff_ir->GetBaseSpecifiers();
  if (base_specifier_diff_ir != nullptr) {
    abi_diff::CXXBaseSpecifierDiff *base_specifier_diff_protobuf =
        record_type_diff_protobuf.mutable_bases_diff();
    if (!AddBaseSpecifierDiffs(base_specifier_diff_protobuf,
                               base_specifier_diff_ir)) {
      llvm::errs() << "Base Specifier diff could not be added\n";
      ::exit(1);
    }
  }
  // Field diffs
  if (!AddRecordFields(&record_type_diff_protobuf,
                       record_type_diff_ir->GetFieldsRemoved(), true) ||
      !AddRecordFields(&record_type_diff_protobuf,
                       record_type_diff_ir->GetFieldsAdded(), false) ||
      !AddRecordFieldDiffs(&record_type_diff_protobuf,
                           record_type_diff_ir->GetFieldDiffs())) {
    llvm::errs() << "Record Field diff could not be added\n";
    ::exit(1);
  }
  return record_type_diff_protobuf;
}

bool IRDiffToProtobufConverter::AddEnumUnderlyingTypeDiff(
    abi_diff::UnderlyingTypeDiff *underlying_type_diff_protobuf,
    const std::pair<std::string, std::string> *underlying_type_diff_ir) {
  if (underlying_type_diff_protobuf == nullptr) {
    return false;
  }
  underlying_type_diff_protobuf->set_old_type(underlying_type_diff_ir->first);
  underlying_type_diff_protobuf->set_new_type(underlying_type_diff_ir->second);
  return true;
}

static bool AddEnumFields(
    google::protobuf::RepeatedPtrField<abi_dump::EnumFieldDecl> *dst,
    const std::vector<const EnumFieldIR *> &enum_fields) {
  for (auto &&enum_field : enum_fields) {
    abi_dump::EnumFieldDecl *added_enum_field = dst->Add();
    if (!SetIRToProtobufEnumField(added_enum_field, enum_field)) {
      return false;
    }
  }
  return true;
}

static bool AddEnumFieldDiffs(
    google::protobuf::RepeatedPtrField<abi_diff::EnumFieldDeclDiff> *dst,
    const std::vector<EnumFieldDiffIR> &fields_diff_ir) {
  for (auto &&field_diff_ir : fields_diff_ir) {
    abi_diff::EnumFieldDeclDiff *field_diff_protobuf = dst->Add();
    if (field_diff_protobuf == nullptr) {
      return false;
    }
    if (!SetIRToProtobufEnumField(field_diff_protobuf->mutable_old_field(),
                                  field_diff_ir.GetOldField()) ||
        !SetIRToProtobufEnumField(field_diff_protobuf->mutable_new_field(),
                                  field_diff_ir.GetNewField())) {
      return false;
    }
  }
  return true;
}

abi_diff::EnumTypeDiff IRDiffToProtobufConverter::ConvertEnumTypeDiffIR(
    const EnumTypeDiffIR *enum_type_diff_ir) {
  abi_diff::EnumTypeDiff enum_type_diff_protobuf;
  enum_type_diff_protobuf.set_name(enum_type_diff_ir->GetName());
  const std::pair<std::string, std::string> *underlying_type_diff =
      enum_type_diff_ir->GetUnderlyingTypeDiff();
  if ((underlying_type_diff != nullptr &&
       !AddEnumUnderlyingTypeDiff(
           enum_type_diff_protobuf.mutable_underlying_type_diff(),
           underlying_type_diff)) ||
      !AddEnumFields(enum_type_diff_protobuf.mutable_fields_removed(),
                     enum_type_diff_ir->GetFieldsRemoved()) ||
      !AddEnumFields(enum_type_diff_protobuf.mutable_fields_added(),
                     enum_type_diff_ir->GetFieldsAdded()) ||
      !AddEnumFieldDiffs(enum_type_diff_protobuf.mutable_fields_diff(),
                         enum_type_diff_ir->GetFieldsDiff())) {
    llvm::errs() << "Enum field diff could not be added\n";
    ::exit(1);
  }
  return enum_type_diff_protobuf;
}

abi_diff::GlobalVarDeclDiff IRDiffToProtobufConverter::ConvertGlobalVarDiffIR(
    const GlobalVarDiffIR *global_var_diff_ir) {
  abi_diff::GlobalVarDeclDiff global_var_diff;
  global_var_diff.set_name(global_var_diff_ir->GetName());
  abi_dump::GlobalVarDecl *old_global_var = global_var_diff.mutable_old();
  abi_dump::GlobalVarDecl *new_global_var = global_var_diff.mutable_new_();
  if (old_global_var == nullptr || new_global_var == nullptr) {
    llvm::errs() << "Globar Var diff could not be added\n";
    ::exit(1);
  }
  *old_global_var =
      IRToProtobufConverter::ConvertGlobalVarIR(
          global_var_diff_ir->GetOldGlobalVar());
  *new_global_var =
      IRToProtobufConverter::ConvertGlobalVarIR(
          global_var_diff_ir->GetNewGlobalVar());
  return global_var_diff;
}

abi_diff::FunctionDeclDiff IRDiffToProtobufConverter::ConvertFunctionDiffIR(
    const FunctionDiffIR *function_diff_ir) {
  abi_diff::FunctionDeclDiff function_diff;
  function_diff.set_name(function_diff_ir->GetName());
  abi_dump::FunctionDecl *old_function = function_diff.mutable_old();
  abi_dump::FunctionDecl *new_function = function_diff.mutable_new_();
  if (old_function == nullptr || new_function == nullptr) {
    llvm::errs() << "Function diff could not be added\n";
    ::exit(1);
  }
  *old_function = IRToProtobufConverter::ConvertFunctionIR(
      function_diff_ir->GetOldFunction());
  *new_function = IRToProtobufConverter::ConvertFunctionIR(
      function_diff_ir->GetNewFunction());
  return function_diff;
}


}  // namespace repr
}  // namespace header_checker

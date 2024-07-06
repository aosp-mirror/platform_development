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

#include "repr/protobuf/ir_diff_dumper.h"

#include "repr/ir_diff_representation.h"
#include "repr/protobuf/api.h"
#include "repr/protobuf/converter.h"

#include <fstream>
#include <memory>
#include <string>

#include <llvm/Support/raw_ostream.h>

#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/text_format.h>


namespace header_checker {
namespace repr {

static bool AddTypeInfoDiff(abi_diff::TypeInfoDiff *type_info_diff_protobuf,
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

static bool AddVTableLayoutDiff(
    abi_diff::VTableLayoutDiff *vtable_layout_diff_protobuf,
    const VTableLayoutDiffIR *vtable_layout_diff_ir) {
  abi_dump::VTableLayout *old_vtable =
      vtable_layout_diff_protobuf->mutable_old_vtable();
  abi_dump::VTableLayout *new_vtable =
      vtable_layout_diff_protobuf->mutable_new_vtable();
  if (old_vtable == nullptr || new_vtable == nullptr ||
      !ConvertVTableLayoutIR(old_vtable,
                             vtable_layout_diff_ir->GetOldVTable()) ||
      !ConvertVTableLayoutIR(new_vtable,
                             vtable_layout_diff_ir->GetNewVTable())) {
    return false;
  }
  return true;
}

static bool CopyBaseSpecifiersDiffIRToProtobuf(
    google::protobuf::RepeatedPtrField<abi_dump::CXXBaseSpecifier> *dst,
    const std::vector<CXXBaseSpecifierIR> &bases_ir) {
  for (auto &&base_ir : bases_ir) {
    abi_dump::CXXBaseSpecifier *added_base = dst->Add();
    if (!ConvertCXXBaseSpecifierIR(added_base, base_ir)) {
      return false;
    }
  }
  return true;
}

static bool AddBaseSpecifierDiffs(
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

static bool AddRecordFields(
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
    if (!ConvertRecordFieldIR(field, record_field_ir)) {
      return false;
    }
  }
  return true;
}

static bool AddRecordFieldDiffs(
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
    ConvertRecordFieldIR(old_field, record_field_diff_ir.GetOldField());
    ConvertRecordFieldIR(new_field, record_field_diff_ir.GetNewField());
  }
  return true;
}

static abi_diff::RecordTypeDiff ConvertRecordTypeDiffIR(
    const RecordTypeDiffIR *record_type_diff_ir) {
  abi_diff::RecordTypeDiff record_type_diff_protobuf;
  record_type_diff_protobuf.set_name(record_type_diff_ir->GetName());
  record_type_diff_protobuf.set_linker_set_key(
      record_type_diff_ir->GetLinkerSetKey());
  // If a type_info diff exists
  const TypeDiffIR *type_diff_ir = record_type_diff_ir->GetTypeDiff();
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

static bool AddEnumUnderlyingTypeDiff(
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
    if (!ConvertEnumFieldIR(added_enum_field, enum_field)) {
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
    if (!ConvertEnumFieldIR(field_diff_protobuf->mutable_old_field(),
                            field_diff_ir.GetOldField()) ||
        !ConvertEnumFieldIR(field_diff_protobuf->mutable_new_field(),
                            field_diff_ir.GetNewField())) {
      return false;
    }
  }
  return true;
}

static abi_diff::EnumTypeDiff ConvertEnumTypeDiffIR(
    const EnumTypeDiffIR *enum_type_diff_ir) {
  abi_diff::EnumTypeDiff enum_type_diff_protobuf;
  enum_type_diff_protobuf.set_name(enum_type_diff_ir->GetName());
  enum_type_diff_protobuf.set_linker_set_key(
      enum_type_diff_ir->GetLinkerSetKey());
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

static abi_diff::GlobalVarDeclDiff ConvertGlobalVarDiffIR(
    const GlobalVarDiffIR *global_var_diff_ir) {
  abi_diff::GlobalVarDeclDiff global_var_diff;
  global_var_diff.set_name(global_var_diff_ir->GetName());
  abi_dump::GlobalVarDecl *old_global_var = global_var_diff.mutable_old();
  abi_dump::GlobalVarDecl *new_global_var = global_var_diff.mutable_new_();
  if (old_global_var == nullptr || new_global_var == nullptr) {
    llvm::errs() << "Globar Var diff could not be added\n";
    ::exit(1);
  }
  *old_global_var = ConvertGlobalVarIR(global_var_diff_ir->GetOldGlobalVar());
  *new_global_var = ConvertGlobalVarIR(global_var_diff_ir->GetNewGlobalVar());
  return global_var_diff;
}

static abi_diff::FunctionDeclDiff ConvertFunctionDiffIR(
    const FunctionDiffIR *function_diff_ir) {
  abi_diff::FunctionDeclDiff function_diff;
  function_diff.set_name(function_diff_ir->GetName());
  abi_dump::FunctionDecl *old_function = function_diff.mutable_old();
  abi_dump::FunctionDecl *new_function = function_diff.mutable_new_();
  if (old_function == nullptr || new_function == nullptr) {
    llvm::errs() << "Function diff could not be added\n";
    ::exit(1);
  }
  *old_function = ConvertFunctionIR(function_diff_ir->GetOldFunction());
  *new_function = ConvertFunctionIR(function_diff_ir->GetNewFunction());
  return function_diff;
}

void ProtobufIRDiffDumper::AddLibNameIR(const std::string &name) {
  diff_tu_->set_lib_name(name);
}

void ProtobufIRDiffDumper::AddArchIR(const std::string &arch) {
  diff_tu_->set_arch(arch);
}

CompatibilityStatusIR ProtobufIRDiffDumper::GetCompatibilityStatusIR() {
  if (diff_tu_->functions_removed().size() != 0 ||
      diff_tu_->global_vars_removed().size() != 0 ||
      diff_tu_->function_diffs().size() != 0 ||
      diff_tu_->global_var_diffs().size() != 0 ||
      diff_tu_->enum_type_diffs().size() != 0 ||
      diff_tu_->record_type_diffs().size() != 0) {
    return CompatibilityStatusIR::Incompatible;
  }

  CompatibilityStatusIR combined_status = CompatibilityStatusIR::Compatible;

  if (diff_tu_->enum_type_extension_diffs().size() != 0 ||
      diff_tu_->functions_added().size() != 0 ||
      diff_tu_->global_vars_added().size() != 0 ||
      diff_tu_->record_type_extension_diffs().size() != 0 ||
      diff_tu_->function_extension_diffs().size() != 0) {
    combined_status = combined_status | CompatibilityStatusIR::Extension;
  }

  if (diff_tu_->unreferenced_enum_type_diffs().size() != 0 ||
      diff_tu_->unreferenced_enum_type_extension_diffs().size() != 0 ||
      diff_tu_->unreferenced_enum_types_added().size() != 0 ||
      diff_tu_->unreferenced_enum_types_removed().size() != 0 ||
      diff_tu_->unreferenced_record_type_diffs().size() != 0 ||
      diff_tu_->unreferenced_record_type_extension_diffs().size() != 0 ||
      diff_tu_->unreferenced_record_types_added().size() != 0 ||
      diff_tu_->unreferenced_record_types_removed().size() != 0) {
    combined_status =
        combined_status | CompatibilityStatusIR::UnreferencedChanges;
  }

  if (diff_tu_->removed_elf_functions().size() != 0 ||
      diff_tu_->removed_elf_objects().size() != 0) {
    combined_status = combined_status | CompatibilityStatusIR::ElfIncompatible;
  }

  return combined_status;
}

void ProtobufIRDiffDumper::AddCompatibilityStatusIR(
    CompatibilityStatusIR status) {
  diff_tu_->set_compatibility_status(CompatibilityStatusIRToProtobuf(status));
}

bool ProtobufIRDiffDumper::AddDiffMessageIR(const DiffMessageIR *message,
                                            const std::string &type_stack,
                                            DiffKind diff_kind) {
  switch (message->Kind()) {
    case RecordTypeKind:
      return AddRecordTypeDiffIR(
          static_cast<const RecordTypeDiffIR *>(message), type_stack,
          diff_kind);
    case EnumTypeKind:
      return AddEnumTypeDiffIR(
          static_cast<const EnumTypeDiffIR *>(message), type_stack, diff_kind);
    case GlobalVarKind:
      return AddGlobalVarDiffIR(
          static_cast<const GlobalVarDiffIR*>(message), type_stack, diff_kind);
    case FunctionKind:
      return AddFunctionDiffIR(
          static_cast<const FunctionDiffIR*>(message), type_stack, diff_kind);
    default:
      break;
  }
  llvm::errs() << "Dump Diff attempted on something not a user defined type / "
               << "function / global variable\n";
  return false;
}

bool ProtobufIRDiffDumper::AddLinkableMessageIR(
    const LinkableMessageIR *message, DiffKind diff_kind) {
  switch (message->GetKind()) {
    case RecordTypeKind:
      return AddLoneRecordTypeDiffIR(
          static_cast<const RecordTypeIR *>(message), diff_kind);
    case EnumTypeKind:
      return AddLoneEnumTypeDiffIR(
          static_cast<const EnumTypeIR *>(message), diff_kind);
    case GlobalVarKind:
      return AddLoneGlobalVarDiffIR(
          static_cast<const GlobalVarIR*>(message), diff_kind);
    case FunctionKind:
      return AddLoneFunctionDiffIR(
          static_cast<const FunctionIR*>(message), diff_kind);
    default:
      break;
  }
  llvm::errs() << "Dump Diff attempted on something not a user defined type / "
               << "function / global variable\n";
  return false;
}

bool ProtobufIRDiffDumper::AddElfSymbolMessageIR(const ElfSymbolIR *elf_symbol,
                                                 DiffKind diff_kind) {
  switch (elf_symbol->GetKind()) {
    case ElfSymbolIR::ElfFunctionKind:
      return AddElfFunctionIR(static_cast<const ElfFunctionIR *>(elf_symbol),
                              diff_kind);
      break;
    case ElfSymbolIR::ElfObjectKind:
      return AddElfObjectIR(static_cast<const ElfObjectIR *>(elf_symbol),
                            diff_kind);
      break;
  }
  // Any other kind is invalid
  return false;
}

bool ProtobufIRDiffDumper::AddElfFunctionIR(
    const ElfFunctionIR *elf_function_ir, DiffKind diff_kind) {
  abi_dump::ElfFunction *added_elf_function = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      added_elf_function = diff_tu_->add_removed_elf_functions();
      break;
    case DiffKind::Added:
      added_elf_function = diff_tu_->add_added_elf_functions();
      break;
    default:
      llvm::errs() << "Invalid call to AddElfFunctionIR\n";
      return false;
  }
  if (added_elf_function == nullptr) {
    return false;
  }
  *added_elf_function = ConvertElfFunctionIR(elf_function_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddElfObjectIR(
    const ElfObjectIR *elf_object_ir, DiffKind diff_kind) {
  abi_dump::ElfObject *added_elf_object = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      added_elf_object = diff_tu_->add_removed_elf_objects();
      break;
    case DiffKind::Added:
      added_elf_object = diff_tu_->add_added_elf_objects();
      break;
    default:
      llvm::errs() << "Invalid call to AddElfObjectIR\n";
      return false;
  }
  if (added_elf_object == nullptr) {
    return false;
  }
  *added_elf_object = ConvertElfObjectIR(elf_object_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddLoneRecordTypeDiffIR(
    const RecordTypeIR *record_type_ir, DiffKind diff_kind) {
  abi_dump::RecordType *added_record_type = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      // Referenced record types do not get reported as added / removed,
      // the diff shows up in the parent type / function/ global variable
      // referencing the record.
      added_record_type = diff_tu_->add_unreferenced_record_types_removed();
      break;
    case DiffKind::Added:
      added_record_type = diff_tu_->add_unreferenced_record_types_added();
      break;
    default:
      llvm::errs() << "Invalid call to AddLoneRecordTypeDiffIR\n";
      return false;
  }
  if (added_record_type == nullptr) {
    return false;
  }
  *added_record_type = ConvertRecordTypeIR(record_type_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddLoneFunctionDiffIR(
    const FunctionIR *function_ir, DiffKind diff_kind) {
  abi_dump::FunctionDecl *added_function = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      added_function = diff_tu_->add_functions_removed();
      break;
    case DiffKind::Added:
      added_function = diff_tu_->add_functions_added();
      break;
    default:
      llvm::errs() << "Invalid call to AddLoneFunctionDiffIR\n";
      return false;
  }
  *added_function = ConvertFunctionIR(function_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddLoneEnumTypeDiffIR(
    const EnumTypeIR *enum_type_ir, DiffKind diff_kind) {
  abi_dump::EnumType *added_enum_type = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      // Referenced enum types do not get reported as added / removed,
      // the diff shows up in the parent type / function/ global variable
      // referencing the enum.
      added_enum_type = diff_tu_->add_unreferenced_enum_types_removed();
      break;
    case DiffKind::Added:
      added_enum_type = diff_tu_->add_unreferenced_enum_types_added();
      break;
    default:
      llvm::errs() << "Invalid call to AddLoneRecordTypeDiffIR\n";
      return false;
  }
  if (added_enum_type == nullptr) {
    return false;
  }
  *added_enum_type = ConvertEnumTypeIR(enum_type_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddLoneGlobalVarDiffIR(
    const GlobalVarIR *global_var_ir, DiffKind diff_kind) {
  abi_dump::GlobalVarDecl *added_global_var = nullptr;
  switch (diff_kind) {
    case DiffKind::Removed:
      added_global_var = diff_tu_->add_global_vars_removed();
      break;
    case DiffKind::Added:
      added_global_var = diff_tu_->add_global_vars_added();
      break;
    default:
      llvm::errs() << "Invalid call to AddLoneFunctionDiffIR\n";
      return false;
  }
  *added_global_var = ConvertGlobalVarIR(global_var_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddRecordTypeDiffIR(
    const RecordTypeDiffIR *record_diff_ir, const std::string &type_stack,
    DiffKind diff_kind) {
  abi_diff::RecordTypeDiff *added_record_type_diff = nullptr;
  bool is_extended = record_diff_ir->IsExtended();
  switch (diff_kind) {
    case DiffKind::Unreferenced:
      if (is_extended) {
        added_record_type_diff =
            diff_tu_->add_unreferenced_record_type_extension_diffs();
      } else {
        added_record_type_diff = diff_tu_->add_unreferenced_record_type_diffs();
      }
      break;
    case DiffKind::Referenced:
      if (is_extended) {
        added_record_type_diff = diff_tu_->add_record_type_extension_diffs();
      } else {
        added_record_type_diff = diff_tu_->add_record_type_diffs();
      }
      break;
    default:
      break;
  }
  if (!added_record_type_diff) {
    return false;
  }

  *added_record_type_diff = ConvertRecordTypeDiffIR(record_diff_ir);
  added_record_type_diff->set_type_stack(type_stack);
  return true;
}

bool ProtobufIRDiffDumper::AddFunctionDiffIR(
    const FunctionDiffIR *function_diff_ir, const std::string &type_stack,
    DiffKind diff_kind) {
  abi_diff::FunctionDeclDiff *added_function_diff =
      function_diff_ir->IsExtended() ? diff_tu_->add_function_extension_diffs()
                                     : diff_tu_->add_function_diffs();
  if (!added_function_diff) {
    return false;
  }
  *added_function_diff = ConvertFunctionDiffIR(function_diff_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddEnumTypeDiffIR(const EnumTypeDiffIR *enum_diff_ir,
                                             const std::string &type_stack,
                                             DiffKind diff_kind) {
  abi_diff::EnumTypeDiff *added_enum_type_diff = nullptr;
  switch (diff_kind) {
    case DiffKind::Unreferenced:
      if (enum_diff_ir->IsExtended()) {
        added_enum_type_diff =
            diff_tu_->add_unreferenced_enum_type_extension_diffs();
      } else {
        added_enum_type_diff =
            diff_tu_->add_unreferenced_enum_type_diffs();
      }
      break;
    case DiffKind::Referenced:
      if (enum_diff_ir->IsExtended()) {
        added_enum_type_diff =
            diff_tu_->add_enum_type_extension_diffs();
      } else {
        added_enum_type_diff =
            diff_tu_->add_enum_type_diffs();
      }
      break;
    default:
      break;
  }
  if (!added_enum_type_diff) {
    return false;
  }
  *added_enum_type_diff = ConvertEnumTypeDiffIR(enum_diff_ir);
  added_enum_type_diff->set_type_stack(type_stack);
  return true;
}

bool ProtobufIRDiffDumper::AddGlobalVarDiffIR(
    const GlobalVarDiffIR *global_var_diff_ir, const std::string &type_stack,
    DiffKind diff_kind) {
  abi_diff::GlobalVarDeclDiff *added_global_var_diff =
      diff_tu_->add_global_var_diffs();
  if (!added_global_var_diff) {
    return false;
  }
  *added_global_var_diff = ConvertGlobalVarDiffIR(global_var_diff_ir);
  return true;
}

bool ProtobufIRDiffDumper::Dump() {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  assert(diff_tu_.get() != nullptr);
  std::ofstream text_output(dump_path_);
  {
    google::protobuf::io::OstreamOutputStream text_os(&text_output);
    if (!google::protobuf::TextFormat::Print(*diff_tu_.get(), &text_os)) {
      return false;
    }
  }
  return text_output.flush().good();
}

std::unique_ptr<IRDiffDumper> CreateProtobufIRDiffDumper(
    const std::string &dump_path) {
  return std::make_unique<ProtobufIRDiffDumper>(dump_path);
}


}  // namespace repr
}  // namespace header_checker

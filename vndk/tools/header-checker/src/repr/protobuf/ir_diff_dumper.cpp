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
      diff_tu_->global_vars_added().size() != 0) {
    combined_status = combined_status | CompatibilityStatusIR::Extension;
  }

  if (diff_tu_->unreferenced_enum_type_diffs().size() != 0 ||
      diff_tu_->unreferenced_enum_types_removed().size() != 0 ||
      diff_tu_->unreferenced_record_types_removed().size() != 0 ||
      diff_tu_->unreferenced_record_type_diffs().size() != 0 ||
      diff_tu_->unreferenced_enum_type_extension_diffs().size() != 0 ||
      diff_tu_->unreferenced_record_types_added().size() != 0 ||
      diff_tu_->unreferenced_enum_types_added().size()) {
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
  *added_elf_function =
      IRToProtobufConverter::ConvertElfFunctionIR(elf_function_ir);
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
  *added_elf_object =
      IRToProtobufConverter::ConvertElfObjectIR(elf_object_ir);
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
  *added_record_type =
      IRToProtobufConverter::ConvertRecordTypeIR(record_type_ir);
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
  *added_function = IRToProtobufConverter::ConvertFunctionIR(function_ir);
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
  *added_enum_type = IRToProtobufConverter::ConvertEnumTypeIR(enum_type_ir);
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
  *added_global_var = IRToProtobufConverter::ConvertGlobalVarIR(global_var_ir);
  return true;
}

bool ProtobufIRDiffDumper::AddRecordTypeDiffIR(
    const RecordTypeDiffIR *record_diff_ir, const std::string &type_stack,
    DiffKind diff_kind) {
  abi_diff::RecordTypeDiff *added_record_type_diff = nullptr;
  switch (diff_kind) {
    case DiffKind::Unreferenced:
      added_record_type_diff = diff_tu_->add_unreferenced_record_type_diffs();
      break;
    case DiffKind::Referenced:
      added_record_type_diff = diff_tu_->add_record_type_diffs();
      break;
    default:
      break;
  }
  if (!added_record_type_diff) {
    return false;
  }

  *added_record_type_diff =
      IRDiffToProtobufConverter::ConvertRecordTypeDiffIR(record_diff_ir);
  added_record_type_diff->set_type_stack(type_stack);
  return true;
}

bool ProtobufIRDiffDumper::AddFunctionDiffIR(
    const FunctionDiffIR *function_diff_ir, const std::string &type_stack,
    DiffKind diff_kind) {
  abi_diff::FunctionDeclDiff *added_function_diff =
      diff_tu_->add_function_diffs();
  if (!added_function_diff) {
    return false;
  }
  *added_function_diff =
      IRDiffToProtobufConverter::ConvertFunctionDiffIR(function_diff_ir);
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
  *added_enum_type_diff =
      IRDiffToProtobufConverter::ConvertEnumTypeDiffIR(enum_diff_ir);
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
  *added_global_var_diff =
      IRDiffToProtobufConverter::ConvertGlobalVarDiffIR(global_var_diff_ir);
  return true;
}

bool ProtobufIRDiffDumper::Dump() {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  assert(diff_tu_.get() != nullptr);
  std::ofstream text_output(dump_path_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  return google::protobuf::TextFormat::Print(*diff_tu_.get(), &text_os);
}

std::unique_ptr<IRDiffDumper> CreateProtobufIRDiffDumper(
    const std::string &dump_path) {
  return std::make_unique<ProtobufIRDiffDumper>(dump_path);
}


}  // namespace repr
}  // namespace header_checker

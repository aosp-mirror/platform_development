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

#include "repr/ir_representation.h"

#include "repr/ir_reader.h"
#include "repr/ir_representation_internal.h"

#include <utility>


namespace header_checker {
namespace repr {


template <typename T, typename U>
static inline T CreateTemp(const U &lm) {
  // Cast source const reference to destination const reference and then create
  // a temporary copy.
  return static_cast<const T &>(lm);
}


bool ModuleIR::AddLinkableMessage(const LinkableMessageIR &lm) {
  switch (lm.GetKind()) {
    case RecordTypeKind:
      AddRecordType(CreateTemp<RecordTypeIR>(lm));
      return true;
    case EnumTypeKind:
      AddEnumType(CreateTemp<EnumTypeIR>(lm));
      return true;
    case PointerTypeKind:
      AddPointerType(CreateTemp<PointerTypeIR>(lm));
      return true;
    case QualifiedTypeKind:
      AddQualifiedType(CreateTemp<QualifiedTypeIR>(lm));
      return true;
    case ArrayTypeKind:
      AddArrayType(CreateTemp<ArrayTypeIR>(lm));
      return true;
    case LvalueReferenceTypeKind:
      AddLvalueReferenceType(CreateTemp<LvalueReferenceTypeIR>(lm));
      return true;
    case RvalueReferenceTypeKind:
      AddRvalueReferenceType(CreateTemp<RvalueReferenceTypeIR>(lm));
      return true;
    case BuiltinTypeKind:
      AddBuiltinType(CreateTemp<BuiltinTypeIR>(lm));
      return true;
    case FunctionTypeKind:
      AddFunctionType(CreateTemp<FunctionTypeIR>(lm));
      return true;
    case GlobalVarKind:
      AddGlobalVariable(CreateTemp<GlobalVarIR>(lm));
      return true;
    case FunctionKind:
      AddFunction(CreateTemp<FunctionIR>(lm));
      return true;
  }
  return false;
}


bool ModuleIR::AddElfSymbol(const ElfSymbolIR &elf_symbol) {
  switch (elf_symbol.GetKind()) {
    case ElfSymbolIR::ElfFunctionKind:
      AddElfFunction(CreateTemp<ElfFunctionIR>(elf_symbol));
      return true;
    case ElfSymbolIR::ElfObjectKind:
      AddElfObject(CreateTemp<ElfObjectIR>(elf_symbol));
      return true;
  }
  return false;
}


void ModuleIR::AddFunction(FunctionIR &&function) {
  if (!IsLinkableMessageInExportedHeaders(&function) ||
      !IsAvailable(function)) {
    return;
  }
  functions_.insert({function.GetLinkerSetKey(), std::move(function)});
}


void ModuleIR::AddGlobalVariable(GlobalVarIR &&global_var) {
  if (!IsLinkableMessageInExportedHeaders(&global_var) ||
      !IsAvailable(global_var)) {
    return;
  }
  global_variables_.insert(
      {global_var.GetLinkerSetKey(), std::move(global_var)});
}


void ModuleIR::FilterRecordFields(RecordTypeIR &record_type) const {
  if (!availability_.has_value()) {
    return;
  }
  std::vector<RecordFieldIR> new_fields;
  for (const RecordFieldIR &field : record_type.GetFields()) {
    if (IsAvailable(field)) {
      new_fields.emplace_back(field);
    }
  }
  record_type.SetRecordFields(std::move(new_fields));
}


void ModuleIR::AddRecordType(RecordTypeIR &&record_type) {
  if (!IsLinkableMessageInExportedHeaders(&record_type) ||
      !IsAvailable(record_type)) {
    return;
  }
  FilterRecordFields(record_type);
  auto it = AddToMapAndTypeGraph(
      std::move(record_type), &record_types_, &type_graph_);
  const std::string &key = GetODRListMapKey(&(it->second));
  AddToODRListMap(key, &(it->second), compilation_unit_path_);
}


void ModuleIR::AddFunctionType(FunctionTypeIR &&function_type) {
  if (!IsLinkableMessageInExportedHeaders(&function_type)) {
    return;
  }
  auto it = AddToMapAndTypeGraph(
      std::move(function_type), &function_types_, &type_graph_);
  const std::string &key = GetODRListMapKey(&(it->second));
  AddToODRListMap(key, &(it->second), compilation_unit_path_);
}


void ModuleIR::FilterEnumFields(EnumTypeIR &enum_type) const {
  if (!availability_.has_value()) {
    return;
  }
  std::vector<EnumFieldIR> new_fields;
  for (const EnumFieldIR &field : enum_type.GetFields()) {
    if (IsAvailable(field)) {
      new_fields.emplace_back(field);
    }
  }
  enum_type.SetFields(std::move(new_fields));
}


void ModuleIR::AddEnumType(EnumTypeIR &&enum_type) {
  if (!IsLinkableMessageInExportedHeaders(&enum_type) ||
      !IsAvailable(enum_type)) {
    return;
  }
  FilterEnumFields(enum_type);
  auto it = AddToMapAndTypeGraph(
      std::move(enum_type), &enum_types_, &type_graph_);
  const std::string &key = GetODRListMapKey(&(it->second));
  AddToODRListMap(key, (&it->second), compilation_unit_path_);
}


void ModuleIR::AddLvalueReferenceType(
    LvalueReferenceTypeIR &&lvalue_reference_type) {
  if (!IsLinkableMessageInExportedHeaders(&lvalue_reference_type)) {
    return;
  }
  AddToMapAndTypeGraph(std::move(lvalue_reference_type),
                       &lvalue_reference_types_, &type_graph_);
}


void ModuleIR::AddRvalueReferenceType(
    RvalueReferenceTypeIR &&rvalue_reference_type) {
  if (!IsLinkableMessageInExportedHeaders(&rvalue_reference_type)) {
    return;
  }
  AddToMapAndTypeGraph(std::move(rvalue_reference_type),
                       &rvalue_reference_types_, &type_graph_);
}


void ModuleIR::AddQualifiedType(QualifiedTypeIR &&qualified_type) {
  if (!IsLinkableMessageInExportedHeaders(&qualified_type)) {
    return;
  }
  AddToMapAndTypeGraph(std::move(qualified_type), &qualified_types_,
                       &type_graph_);
}


void ModuleIR::AddArrayType(ArrayTypeIR &&array_type) {
  if (!IsLinkableMessageInExportedHeaders(&array_type)) {
    return;
  }
  AddToMapAndTypeGraph(std::move(array_type), &array_types_, &type_graph_);
}


void ModuleIR::AddPointerType(PointerTypeIR &&pointer_type) {
  if (!IsLinkableMessageInExportedHeaders(&pointer_type)) {
    return;
  }
  AddToMapAndTypeGraph(std::move(pointer_type), &pointer_types_, &type_graph_);
}


void ModuleIR::AddBuiltinType(BuiltinTypeIR &&builtin_type) {
  AddToMapAndTypeGraph(std::move(builtin_type), &builtin_types_, &type_graph_);
}


void ModuleIR::AddElfFunction(ElfFunctionIR &&elf_function) {
  elf_functions_.insert(
      {elf_function.GetName(), std::move(elf_function)});
}


void ModuleIR::AddElfObject(ElfObjectIR &&elf_object) {
  elf_objects_.insert(
      {elf_object.GetName(), std::move(elf_object)});
}


std::string ModuleIR::GetCompilationUnitPath(const TypeIR *type_ir) const {
  std::string key;
  switch (type_ir->GetKind()) {
    case RecordTypeKind:
      key = GetODRListMapKey(static_cast<const RecordTypeIR *>(type_ir));
      break;
    case EnumTypeKind:
      key = GetODRListMapKey(static_cast<const EnumTypeIR *>(type_ir));
      break;
    case FunctionTypeKind:
      key = GetODRListMapKey(static_cast<const FunctionTypeIR *>(type_ir));
      break;
    default:
      return "";
  }
  auto it = odr_list_map_.find(key);
  if (it == odr_list_map_.end()) {
    return "";
  }
  for (const auto &definition : it->second) {
    if (definition.type_ir_ == type_ir) {
      return definition.compilation_unit_path_;
    }
  }
  return "";
}


bool ModuleIR::IsLinkableMessageInExportedHeaders(
    const LinkableMessageIR *linkable_message) const {
  if (exported_headers_ == nullptr || exported_headers_->empty()) {
    return true;
  }
  return exported_headers_->find(linkable_message->GetSourceFile()) !=
         exported_headers_->end();
}


bool ModuleIR::IsAvailable(const HasAvailabilityAttrs &decl_ir) const {
  if (!availability_.has_value()) {
    return true;
  }
  for (const AvailabilityAttrIR &attr : decl_ir.GetAvailabilityAttrs()) {
    if (attr.IsUnavailable()) {
      return false;
    }
    if (auto introduced = attr.GetIntroduced();
        introduced.has_value() && availability_.value() < introduced.value()) {
      return false;
    }
    if (auto obsoleted = attr.GetObsoleted();
        obsoleted.has_value() && availability_.value() >= obsoleted.value()) {
      return false;
    }
  }
  return true;
}


}  // namespace repr
}  // namespace header_checker

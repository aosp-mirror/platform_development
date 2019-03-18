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


void ModuleIR::AddFunction(FunctionIR &&function) {
  if (!IsLinkableMessageInExportedHeaders(&function)) {
    return;
  }
  functions_.insert({function.GetLinkerSetKey(), std::move(function)});
}


void ModuleIR::AddGlobalVariable(GlobalVarIR &&global_var) {
  if (!IsLinkableMessageInExportedHeaders(&global_var)) {
    return;
  }
  global_variables_.insert(
      {global_var.GetLinkerSetKey(), std::move(global_var)});
}


void ModuleIR::AddRecordType(RecordTypeIR &&record_type) {
  if (!IsLinkableMessageInExportedHeaders(&record_type)) {
    return;
  }
  auto it = AddToMapAndTypeGraph(
      std::move(record_type), &record_types_, &type_graph_);
  const std::string &key = GetODRListMapKey(&(it->second));
  AddToODRListMap(key, &(it->second));
}


void ModuleIR::AddFunctionType(FunctionTypeIR &&function_type) {
  if (!IsLinkableMessageInExportedHeaders(&function_type)) {
    return;
  }
  auto it = AddToMapAndTypeGraph(
      std::move(function_type), &function_types_, &type_graph_);
  const std::string &key = GetODRListMapKey(&(it->second));
  AddToODRListMap(key, &(it->second));
}


void ModuleIR::AddEnumType(EnumTypeIR &&enum_type) {
  if (!IsLinkableMessageInExportedHeaders(&enum_type)) {
    return;
  }
  auto it = AddToMapAndTypeGraph(
      std::move(enum_type), &enum_types_, &type_graph_);
  AddToODRListMap(it->second.GetUniqueId() + it->second.GetSourceFile(),
                  (&it->second));
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


bool ModuleIR::IsLinkableMessageInExportedHeaders(
    const LinkableMessageIR *linkable_message) const {
  if (exported_headers_ == nullptr || exported_headers_->empty()) {
    return true;
  }
  return exported_headers_->find(linkable_message->GetSourceFile()) !=
         exported_headers_->end();
}


}  // namespace repr
}  // namespace header_checker

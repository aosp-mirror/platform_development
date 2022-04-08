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

#include "repr/json/ir_dumper.h"

#include "repr/ir_dumper.h"
#include "repr/ir_reader.h"
#include "repr/ir_representation_internal.h"
#include "repr/json/api.h"
#include "repr/json/converter.h"

#include <json/reader.h>
#include <json/writer.h>

#include <llvm/Support/raw_ostream.h>

#include <cstdlib>
#include <fstream>
#include <sstream>
#include <string>


namespace header_checker {
namespace repr {


static void AddAccess(JsonObject &type_decl, AccessSpecifierIR value) {
  if (value != default_access_ir) {
    type_decl.Set("access",
                  FindInMap(access_ir_to_json, value,
                            "Failed to convert AccessSpecifierIR to JSON"));
  }
}

static void AddRecordKind(JsonObject &record_type,
                          RecordTypeIR::RecordKind value) {
  if (value != default_record_kind_ir) {
    record_type.Set("record_kind",
                    FindInMap(record_kind_ir_to_json, value,
                              "Failed to convert RecordKind to JSON"));
  }
}

static void AddVtableComponentKind(JsonObject &vtable_component,
                                   VTableComponentIR::Kind value) {
  if (value != default_vtable_component_kind_ir) {
    vtable_component.Set(
        "kind", FindInMap(vtable_component_kind_ir_to_json, value,
                          "Failed to convert VTableComponentIR::Kind to JSON"));
  }
}

static void AddElfSymbolBinding(JsonObject &elf_symbol,
                                ElfSymbolIR::ElfSymbolBinding value) {
  if (value != default_elf_symbol_binding_ir) {
    elf_symbol.Set("binding",
                   FindInMap(elf_symbol_binding_ir_to_json, value,
                             "Failed to convert ElfSymbolBinding to JSON"));
  }
}

void IRToJsonConverter::AddTemplateInfo(
    JsonObject &type_decl, const TemplatedArtifactIR *template_ir) {
  JsonArray args;
  for (auto &&template_element_ir : template_ir->GetTemplateElements()) {
    args.append(template_element_ir.GetReferencedType());
  }
  type_decl.Set("template_args", args);
}

void IRToJsonConverter::AddTypeInfo(JsonObject &type_decl,
                                    const TypeIR *type_ir) {
  type_decl.Set("linker_set_key", type_ir->GetLinkerSetKey());
  type_decl.Set("source_file", type_ir->GetSourceFile());
  type_decl.Set("name", type_ir->GetName());
  type_decl.Set("size", (uint64_t)type_ir->GetSize());
  type_decl.Set("alignment", (uint64_t)type_ir->GetAlignment());
  type_decl.Set("referenced_type", type_ir->GetReferencedType());
  type_decl.Set("self_type", type_ir->GetSelfType());
}

static JsonObject ConvertRecordFieldIR(const RecordFieldIR *record_field_ir) {
  JsonObject record_field;
  record_field.Set("field_name", record_field_ir->GetName());
  record_field.Set("referenced_type", record_field_ir->GetReferencedType());
  AddAccess(record_field, record_field_ir->GetAccess());
  record_field.Set("field_offset", (uint64_t)record_field_ir->GetOffset());
  return record_field;
}

void IRToJsonConverter::AddRecordFields(JsonObject &record_type,
                                        const RecordTypeIR *record_ir) {
  JsonArray fields;
  for (auto &&field_ir : record_ir->GetFields()) {
    fields.append(ConvertRecordFieldIR(&field_ir));
  }
  record_type.Set("fields", fields);
}

static JsonObject
ConvertBaseSpecifierIR(const CXXBaseSpecifierIR &base_specifier_ir) {
  JsonObject base_specifier;
  base_specifier.Set("referenced_type", base_specifier_ir.GetReferencedType());
  base_specifier.Set("is_virtual", base_specifier_ir.IsVirtual());
  AddAccess(base_specifier, base_specifier_ir.GetAccess());
  return base_specifier;
}

void IRToJsonConverter::AddBaseSpecifiers(JsonObject &record_type,
                                          const RecordTypeIR *record_ir) {
  JsonArray base_specifiers;
  for (auto &&base_ir : record_ir->GetBases()) {
    base_specifiers.append(ConvertBaseSpecifierIR(base_ir));
  }
  record_type.Set("base_specifiers", base_specifiers);
}

static JsonObject
ConvertVTableComponentIR(const VTableComponentIR &vtable_component_ir) {
  JsonObject vtable_component;
  AddVtableComponentKind(vtable_component, vtable_component_ir.GetKind());
  vtable_component.Set("component_value",
                       (int64_t)vtable_component_ir.GetValue());
  vtable_component.Set("mangled_component_name", vtable_component_ir.GetName());
  vtable_component.Set("is_pure", vtable_component_ir.GetIsPure());
  return vtable_component;
}

void IRToJsonConverter::AddVTableLayout(JsonObject &record_type,
                                        const RecordTypeIR *record_ir) {
  JsonArray vtable_components;
  for (auto &&vtable_component_ir :
       record_ir->GetVTableLayout().GetVTableComponents()) {
    vtable_components.append(ConvertVTableComponentIR(vtable_component_ir));
  }
  record_type.Set("vtable_components", vtable_components);
}

JsonObject IRToJsonConverter::ConvertRecordTypeIR(const RecordTypeIR *recordp) {
  JsonObject record_type;

  AddAccess(record_type, recordp->GetAccess());
  AddRecordKind(record_type, recordp->GetRecordKind());
  record_type.Set("is_anonymous", recordp->IsAnonymous());
  AddTypeInfo(record_type, recordp);
  AddRecordFields(record_type, recordp);
  AddBaseSpecifiers(record_type, recordp);
  AddVTableLayout(record_type, recordp);
  AddTemplateInfo(record_type, recordp);
  return record_type;
}

void IRToJsonConverter::AddFunctionParametersAndSetReturnType(
    JsonObject &function, const CFunctionLikeIR *cfunction_like_ir) {
  function.Set("return_type", cfunction_like_ir->GetReturnType());
  AddFunctionParameters(function, cfunction_like_ir);
}

void IRToJsonConverter::AddFunctionParameters(
    JsonObject &function, const CFunctionLikeIR *cfunction_like_ir) {
  JsonArray parameters;
  for (auto &&parameter_ir : cfunction_like_ir->GetParameters()) {
    JsonObject parameter;
    parameter.Set("referenced_type", parameter_ir.GetReferencedType());
    parameter.Set("default_arg", parameter_ir.GetIsDefault());
    parameter.Set("is_this_ptr", parameter_ir.GetIsThisPtr());
    parameters.append(parameter);
  }
  function.Set("parameters", parameters);
}

JsonObject
IRToJsonConverter::ConvertFunctionTypeIR(const FunctionTypeIR *function_typep) {
  JsonObject function_type;
  AddTypeInfo(function_type, function_typep);
  AddFunctionParametersAndSetReturnType(function_type, function_typep);
  return function_type;
}

JsonObject IRToJsonConverter::ConvertFunctionIR(const FunctionIR *functionp) {
  JsonObject function;
  AddAccess(function, functionp->GetAccess());
  function.Set("linker_set_key", functionp->GetLinkerSetKey());
  function.Set("source_file", functionp->GetSourceFile());
  function.Set("function_name", functionp->GetName());
  AddFunctionParametersAndSetReturnType(function, functionp);
  AddTemplateInfo(function, functionp);
  return function;
}

static JsonObject ConvertEnumFieldIR(const EnumFieldIR *enum_field_ir) {
  JsonObject enum_field;
  enum_field.Set("name", enum_field_ir->GetName());
  // Never omit enum values.
  enum_field["enum_field_value"] = Json::Int64(enum_field_ir->GetValue());
  return enum_field;
}

void IRToJsonConverter::AddEnumFields(JsonObject &enum_type,
                                      const EnumTypeIR *enum_ir) {
  JsonArray enum_fields;
  for (auto &&field : enum_ir->GetFields()) {
    enum_fields.append(ConvertEnumFieldIR(&field));
  }
  enum_type.Set("enum_fields", enum_fields);
}

JsonObject IRToJsonConverter::ConvertEnumTypeIR(const EnumTypeIR *enump) {
  JsonObject enum_type;
  AddAccess(enum_type, enump->GetAccess());
  enum_type.Set("underlying_type", enump->GetUnderlyingType());
  AddTypeInfo(enum_type, enump);
  AddEnumFields(enum_type, enump);
  return enum_type;
}

JsonObject
IRToJsonConverter::ConvertGlobalVarIR(const GlobalVarIR *global_varp) {
  JsonObject global_var;
  global_var.Set("referenced_type", global_varp->GetReferencedType());
  global_var.Set("source_file", global_varp->GetSourceFile());
  global_var.Set("name", global_varp->GetName());
  global_var.Set("linker_set_key", global_varp->GetLinkerSetKey());
  AddAccess(global_var, global_varp->GetAccess());
  return global_var;
}

JsonObject
IRToJsonConverter::ConvertPointerTypeIR(const PointerTypeIR *pointerp) {
  JsonObject pointer_type;
  AddTypeInfo(pointer_type, pointerp);
  return pointer_type;
}

JsonObject
IRToJsonConverter::ConvertQualifiedTypeIR(const QualifiedTypeIR *qualtypep) {
  JsonObject qualified_type;
  AddTypeInfo(qualified_type, qualtypep);
  qualified_type.Set("is_const", qualtypep->IsConst());
  qualified_type.Set("is_volatile", qualtypep->IsVolatile());
  qualified_type.Set("is_restricted", qualtypep->IsRestricted());
  return qualified_type;
}

JsonObject
IRToJsonConverter::ConvertBuiltinTypeIR(const BuiltinTypeIR *builtin_typep) {
  JsonObject builtin_type;
  builtin_type.Set("is_unsigned", builtin_typep->IsUnsigned());
  builtin_type.Set("is_integral", builtin_typep->IsIntegralType());
  AddTypeInfo(builtin_type, builtin_typep);
  return builtin_type;
}

JsonObject
IRToJsonConverter::ConvertArrayTypeIR(const ArrayTypeIR *array_typep) {
  JsonObject array_type;
  AddTypeInfo(array_type, array_typep);
  return array_type;
}

JsonObject IRToJsonConverter::ConvertLvalueReferenceTypeIR(
    const LvalueReferenceTypeIR *lvalue_reference_typep) {
  JsonObject lvalue_reference_type;
  AddTypeInfo(lvalue_reference_type, lvalue_reference_typep);
  return lvalue_reference_type;
}

JsonObject IRToJsonConverter::ConvertRvalueReferenceTypeIR(
    const RvalueReferenceTypeIR *rvalue_reference_typep) {
  JsonObject rvalue_reference_type;
  AddTypeInfo(rvalue_reference_type, rvalue_reference_typep);
  return rvalue_reference_type;
}

bool JsonIRDumper::AddLinkableMessageIR(const LinkableMessageIR *lm) {
  std::string key;
  JsonObject converted;
  // No RTTI
  switch (lm->GetKind()) {
  case RecordTypeKind:
    key = "record_types";
    converted = ConvertRecordTypeIR(static_cast<const RecordTypeIR *>(lm));
    break;
  case EnumTypeKind:
    key = "enum_types";
    converted = ConvertEnumTypeIR(static_cast<const EnumTypeIR *>(lm));
    break;
  case PointerTypeKind:
    key = "pointer_types";
    converted = ConvertPointerTypeIR(static_cast<const PointerTypeIR *>(lm));
    break;
  case QualifiedTypeKind:
    key = "qualified_types";
    converted =
        ConvertQualifiedTypeIR(static_cast<const QualifiedTypeIR *>(lm));
    break;
  case ArrayTypeKind:
    key = "array_types";
    converted = ConvertArrayTypeIR(static_cast<const ArrayTypeIR *>(lm));
    break;
  case LvalueReferenceTypeKind:
    key = "lvalue_reference_types";
    converted = ConvertLvalueReferenceTypeIR(
        static_cast<const LvalueReferenceTypeIR *>(lm));
    break;
  case RvalueReferenceTypeKind:
    key = "rvalue_reference_types";
    converted = ConvertRvalueReferenceTypeIR(
        static_cast<const RvalueReferenceTypeIR *>(lm));
    break;
  case BuiltinTypeKind:
    key = "builtin_types";
    converted = ConvertBuiltinTypeIR(static_cast<const BuiltinTypeIR *>(lm));
    break;
  case FunctionTypeKind:
    key = "function_types";
    converted = ConvertFunctionTypeIR(static_cast<const FunctionTypeIR *>(lm));
    break;
  case GlobalVarKind:
    key = "global_vars";
    converted = ConvertGlobalVarIR(static_cast<const GlobalVarIR *>(lm));
    break;
  case FunctionKind:
    key = "functions";
    converted = ConvertFunctionIR(static_cast<const FunctionIR *>(lm));
    break;
  default:
    return false;
  }
  translation_unit_[key].append(converted);
  return true;
}

bool JsonIRDumper::AddElfSymbolMessageIR(const ElfSymbolIR *elf_symbol_ir) {
  std::string key;
  switch (elf_symbol_ir->GetKind()) {
  case ElfSymbolIR::ElfFunctionKind:
    key = "elf_functions";
    break;
  case ElfSymbolIR::ElfObjectKind:
    key = "elf_objects";
    break;
  default:
    return false;
  }
  JsonObject elf_symbol;
  elf_symbol.Set("name", elf_symbol_ir->GetName());
  AddElfSymbolBinding(elf_symbol, elf_symbol_ir->GetBinding());
  translation_unit_[key].append(elf_symbol);
  return true;
}

static std::string DumpJson(const JsonObject &obj) {
  std::ostringstream output_stream;
  Json::StyledStreamWriter writer(/* indentation */ " ");
  writer.write(output_stream, obj);
  return output_stream.str();
}

static void WriteTailTrimmedLinesToFile(const std::string &path,
                                        const std::string &output_string) {
  std::ofstream output_file(path);
  size_t line_start = 0;
  while (line_start < output_string.size()) {
    size_t trailing_space_start = line_start;
    size_t index;
    for (index = line_start;
         index < output_string.size() && output_string[index] != '\n';
         index++) {
      if (output_string[index] != ' ') {
        trailing_space_start = index + 1;
      }
    }
    // Only write this line if this line contains non-whitespace characters.
    if (trailing_space_start != line_start) {
      output_file.write(output_string.data() + line_start,
                        trailing_space_start - line_start);
      output_file.write("\n", 1);
    }
    line_start = index + 1;
  }
}

bool JsonIRDumper::Dump(const ModuleIR &module) {
  DumpModule(module);
  std::string output_string = DumpJson(translation_unit_);
  WriteTailTrimmedLinesToFile(dump_path_, output_string);
  return true;
}

JsonIRDumper::JsonIRDumper(const std::string &dump_path)
    : IRDumper(dump_path), translation_unit_() {
  const std::string keys[] = {
    "record_types",
    "enum_types",
    "pointer_types",
    "lvalue_reference_types",
    "rvalue_reference_types",
    "builtin_types",
    "qualified_types",
    "array_types",
    "function_types",
    "functions",
    "global_vars",
    "elf_functions",
    "elf_objects",
  };
  for (auto key : keys) {
    translation_unit_[key] = JsonArray();
  }
}

std::unique_ptr<IRDumper> CreateJsonIRDumper(const std::string &dump_path) {
  return std::make_unique<JsonIRDumper>(dump_path);
}


}  // namespace repr
}  // header_checker

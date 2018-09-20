// Copyright (C) 2018 The Android Open Source Project
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

#include <ir_representation_json.h>

#include <json/writer.h>
#include <llvm/Support/raw_ostream.h>

#include <cstdlib>
#include <fstream>
#include <sstream>
#include <string>

namespace abi_util {

// Conversion between IR enums and JSON strings.
static const std::map<AccessSpecifierIR, std::string> access_ir_to_json{
  {AccessSpecifierIR::PublicAccess, "public"},
  {AccessSpecifierIR::ProtectedAccess, "protected"},
  {AccessSpecifierIR::PrivateAccess, "private"},
};

static const std::map<RecordTypeIR::RecordKind, std::string>
    record_kind_ir_to_json{
  {RecordTypeIR::RecordKind::struct_kind, "struct"},
  {RecordTypeIR::RecordKind::class_kind, "class"},
  {RecordTypeIR::RecordKind::union_kind, "union"},
};

static const std::map<VTableComponentIR::Kind, std::string>
    vtable_component_kind_ir_to_json{
  {VTableComponentIR::Kind::VCallOffset, "vcall_offset"},
  {VTableComponentIR::Kind::VBaseOffset, "vbase_offset"},
  {VTableComponentIR::Kind::OffsetToTop, "offset_to_top"},
  {VTableComponentIR::Kind::RTTI, "rtti"},
  {VTableComponentIR::Kind::FunctionPointer, "function_pointer"},
  {VTableComponentIR::Kind::CompleteDtorPointer, "complete_dtor_pointer"},
  {VTableComponentIR::Kind::DeletingDtorPointer, "deleting_dtor_pointer"},
  {VTableComponentIR::Kind::UnusedFunctionPointer, "unused_function_pointer"},
};

static const std::map<ElfSymbolIR::ElfSymbolBinding, std::string>
    elf_symbol_binding_ir_to_json{
  {ElfSymbolIR::ElfSymbolBinding::Weak, "weak"},
  {ElfSymbolIR::ElfSymbolBinding::Global, "global"},
};

// If m contains k, this function returns the value.
// Otherwise, it prints error_msg and exits.
template <typename K, typename V>
static inline const V &FindInMap(const std::map<K, V> &m, const K &k,
                                 const std::string &error_msg) {
  auto it = m.find(k);
  if (it == m.end()) {
    llvm::errs() << error_msg << "\n";
    ::exit(1);
  }
  return it->second;
}

static inline const std::string &AccessIRToJson(AccessSpecifierIR access) {
  return FindInMap(access_ir_to_json, access,
                   "Failed to convert AccessSpecifierIR to JSON");
}

static inline const std::string &
RecordKindIRToJson(RecordTypeIR::RecordKind kind) {
  return FindInMap(record_kind_ir_to_json, kind,
                   "Failed to convert RecordKind to JSON");
}

static inline const std::string &
VTableComponentKindIRToJson(VTableComponentIR::Kind kind) {
  return FindInMap(vtable_component_kind_ir_to_json, kind,
                   "Failed to convert VTableComponentIR::Kind to JSON");
}

static inline const std::string &
ElfSymbolBindingIRToJson(ElfSymbolIR::ElfSymbolBinding binding) {
  return FindInMap(elf_symbol_binding_ir_to_json, binding,
                   "Failed to convert ElfSymbolBinding to JSON");
}

void IRToJsonConverter::AddTemplateInfo(
    JsonObject &type_decl, const TemplatedArtifactIR *template_ir) {
  Json::Value &args = type_decl["template_args"];
  args = JsonArray();
  for (auto &&template_element_ir : template_ir->GetTemplateElements()) {
    args.append(template_element_ir.GetReferencedType());
  }
}

void IRToJsonConverter::AddTypeInfo(JsonObject &type_decl,
                                    const TypeIR *type_ir) {
  type_decl["linker_set_key"] = type_ir->GetLinkerSetKey();
  type_decl["source_file"] = type_ir->GetSourceFile();
  type_decl["name"] = type_ir->GetName();
  type_decl["size"] = Json::UInt64(type_ir->GetSize());
  type_decl["alignment"] = type_ir->GetAlignment();
  type_decl["referenced_type"] = type_ir->GetReferencedType();
  type_decl["self_type"] = type_ir->GetSelfType();
}

static JsonObject ConvertRecordFieldIR(const RecordFieldIR *record_field_ir) {
  JsonObject record_field;
  record_field["field_name"] = record_field_ir->GetName();
  record_field["referenced_type"] = record_field_ir->GetReferencedType();
  record_field["access"] = AccessIRToJson(record_field_ir->GetAccess());
  record_field["field_offset"] = Json::UInt64(record_field_ir->GetOffset());
  return record_field;
}

void IRToJsonConverter::AddRecordFields(JsonObject &record_type,
                                        const RecordTypeIR *record_ir) {
  Json::Value &fields = record_type["fields"];
  fields = JsonArray();
  for (auto &&field_ir : record_ir->GetFields()) {
    fields.append(ConvertRecordFieldIR(&field_ir));
  }
}

static JsonObject
ConvertBaseSpecifierIR(const CXXBaseSpecifierIR &base_specifier_ir) {
  JsonObject base_specifier;
  base_specifier["referenced_type"] = base_specifier_ir.GetReferencedType();
  base_specifier["is_virtual"] = base_specifier_ir.IsVirtual();
  base_specifier["access"] = AccessIRToJson(base_specifier_ir.GetAccess());
  return base_specifier;
}

void IRToJsonConverter::AddBaseSpecifiers(JsonObject &record_type,
                                          const RecordTypeIR *record_ir) {
  Json::Value &base_specifiers = record_type["base_specifiers"];
  base_specifiers = JsonArray();
  for (auto &&base_ir : record_ir->GetBases()) {
    base_specifiers.append(ConvertBaseSpecifierIR(base_ir));
  }
}

static JsonObject
ConvertVTableComponentIR(const VTableComponentIR &vtable_component_ir) {
  JsonObject vtable_component;
  vtable_component["kind"] =
      VTableComponentKindIRToJson(vtable_component_ir.GetKind());
  vtable_component["component_value"] =
      Json::Int64(vtable_component_ir.GetValue());
  vtable_component["mangled_component_name"] = vtable_component_ir.GetName();
  vtable_component["is_pure"] = vtable_component_ir.GetIsPure();
  return vtable_component;
}

void IRToJsonConverter::AddVTableLayout(JsonObject &record_type,
                                        const RecordTypeIR *record_ir) {
  Json::Value &vtable_components = record_type["vtable_components"];
  vtable_components = JsonArray();
  for (auto &&vtable_component_ir :
       record_ir->GetVTableLayout().GetVTableComponents()) {
    vtable_components.append(ConvertVTableComponentIR(vtable_component_ir));
  }
}

void IRToJsonConverter::AddTagTypeInfo(JsonObject &type_decl,
                                       const TagTypeIR *tag_type_ir) {
  type_decl["unique_id"] = tag_type_ir->GetUniqueId();
}

JsonObject IRToJsonConverter::ConvertRecordTypeIR(const RecordTypeIR *recordp) {
  JsonObject record_type;

  record_type["access"] = AccessIRToJson(recordp->GetAccess());
  record_type["record_kind"] = RecordKindIRToJson(recordp->GetRecordKind());
  record_type["is_anonymous"] = recordp->IsAnonymous();
  AddTypeInfo(record_type, recordp);
  AddRecordFields(record_type, recordp);
  AddBaseSpecifiers(record_type, recordp);
  AddVTableLayout(record_type, recordp);
  AddTagTypeInfo(record_type, recordp);
  AddTemplateInfo(record_type, recordp);
  return record_type;
}

void IRToJsonConverter::AddFunctionParametersAndSetReturnType(
    JsonObject &function, const CFunctionLikeIR *cfunction_like_ir) {
  function["return_type"] = cfunction_like_ir->GetReturnType();
  AddFunctionParameters(function, cfunction_like_ir);
}

void IRToJsonConverter::AddFunctionParameters(
    JsonObject &function, const CFunctionLikeIR *cfunction_like_ir) {
  Json::Value &parameters = function["parameters"];
  parameters = JsonArray();
  for (auto &&parameter_ir : cfunction_like_ir->GetParameters()) {
    Json::Value &parameter = parameters.append(JsonObject());
    parameter["referenced_type"] = parameter_ir.GetReferencedType();
    parameter["default_arg"] = parameter_ir.GetIsDefault();
    parameter["is_this_ptr"] = parameter_ir.GetIsThisPtr();
  }
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
  function["access"] = AccessIRToJson(functionp->GetAccess());
  function["linker_set_key"] = functionp->GetLinkerSetKey();
  function["source_file"] = functionp->GetSourceFile();
  function["function_name"] = functionp->GetName();
  AddFunctionParametersAndSetReturnType(function, functionp);
  AddTemplateInfo(function, functionp);
  return function;
}

static JsonObject ConvertEnumFieldIR(const EnumFieldIR *enum_field_ir) {
  JsonObject enum_field;
  enum_field["name"] = enum_field_ir->GetName();
  enum_field["enum_field_value"] = Json::Int64(enum_field_ir->GetValue());
  return enum_field;
}

void IRToJsonConverter::AddEnumFields(JsonObject &enum_type,
                                      const EnumTypeIR *enum_ir) {
  Json::Value &enum_fields = enum_type["enum_fields"];
  enum_fields = JsonArray();
  for (auto &&field : enum_ir->GetFields()) {
    enum_fields.append(ConvertEnumFieldIR(&field));
  }
}

JsonObject IRToJsonConverter::ConvertEnumTypeIR(const EnumTypeIR *enump) {
  JsonObject enum_type;
  enum_type["access"] = AccessIRToJson(enump->GetAccess());
  enum_type["underlying_type"] = enump->GetUnderlyingType();
  AddTypeInfo(enum_type, enump);
  AddEnumFields(enum_type, enump);
  AddTagTypeInfo(enum_type, enump);
  return enum_type;
}

JsonObject
IRToJsonConverter::ConvertGlobalVarIR(const GlobalVarIR *global_varp) {
  JsonObject global_var;
  global_var["referenced_type"] = global_varp->GetReferencedType();
  global_var["source_file"] = global_varp->GetSourceFile();
  global_var["name"] = global_varp->GetName();
  global_var["linker_set_key"] = global_varp->GetLinkerSetKey();
  global_var["access"] = AccessIRToJson(global_varp->GetAccess());
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
  qualified_type["is_const"] = qualtypep->IsConst();
  qualified_type["is_volatile"] = qualtypep->IsVolatile();
  qualified_type["is_restricted"] = qualtypep->IsRestricted();
  return qualified_type;
}

JsonObject
IRToJsonConverter::ConvertBuiltinTypeIR(const BuiltinTypeIR *builtin_typep) {
  JsonObject builtin_type;
  builtin_type["is_unsigned"] = builtin_typep->IsUnsigned();
  builtin_type["is_integral"] = builtin_typep->IsIntegralType();
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
  Json::Value &elf_symbol = translation_unit_[key].append(JsonObject());
  elf_symbol["name"] = elf_symbol_ir->GetName();
  elf_symbol["binding"] =
      ElfSymbolBindingIRToJson(elf_symbol_ir->GetBinding());
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

bool JsonIRDumper::Dump() {
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

} // namespace abi_util

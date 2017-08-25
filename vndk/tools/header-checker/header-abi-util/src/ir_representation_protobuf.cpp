// Copyright (C) 2017 The Android Open Source Project
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

#include <ir_representation_protobuf.h>

#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <iostream>
#include <string>
#include <memory>

namespace abi_util {

void ProtobufTextFormatToIRReader::ReadTypeInfo(
    const abi_dump::BasicNamedAndTypedDecl &type_info,
    TypeIR *typep) {
  typep->SetLinkerSetKey(type_info.linker_set_key());
  typep->SetName(type_info.linker_set_key());
  typep->SetSourceFile(type_info.source_file());
  typep->SetReferencedType(type_info.referenced_type());
  typep->SetSize(type_info.size());
  typep->SetAlignment(type_info.alignment());
}

bool ProtobufTextFormatToIRReader::ReadDump() {
  abi_dump::TranslationUnit tu;
  std::ifstream input(dump_path_);
  google::protobuf::io::IstreamInputStream text_is(&input);

  if (!google::protobuf::TextFormat::Parse(&text_is, &tu)) {
    llvm::errs() << "Failed to parse protobuf TextFormat file\n";
    return false;
  }

  functions_ = ReadFunctions(tu);
  global_variables_ = ReadGlobalVariables(tu);

  enum_types_ = ReadEnumTypes(tu);
  record_types_ = ReadRecordTypes(tu);
  array_types_ = ReadArrayTypes(tu);
  pointer_types_ = ReadPointerTypes(tu);
  qualified_types_ = ReadQualifiedTypes(tu);
  builtin_types_ = ReadBuiltinTypes(tu);
  lvalue_reference_types_ = ReadLvalueReferenceTypes(tu);
  rvalue_reference_types_ = ReadRvalueReferenceTypes(tu);

  elf_functions_ = ReadElfFunctions(tu);
  elf_objects_ = ReadElfObjects(tu);

  return true;
}

TemplateInfoIR ProtobufTextFormatToIRReader::TemplateInfoProtobufToIR(
    const abi_dump::TemplateInfo &template_info_protobuf) {
  TemplateInfoIR template_info_ir;
  for (auto &&template_element : template_info_protobuf.elements()) {
    TemplateElementIR template_element_ir(template_element.referenced_type());
    template_info_ir.AddTemplateElement(std::move(template_element_ir));
  }
  return template_info_ir;
}

FunctionIR ProtobufTextFormatToIRReader::FunctionProtobufToIR(
    const abi_dump::FunctionDecl &function_protobuf) {
  FunctionIR function_ir;
  function_ir.SetReturnType(function_protobuf.return_type());
  function_ir.SetLinkerSetKey(function_protobuf.linker_set_key());
  function_ir.SetName(function_protobuf.function_name());
  function_ir.SetAccess(AccessProtobufToIR(function_protobuf.access()));
  function_ir.SetSourceFile(function_protobuf.source_file());
  // Set parameters
  for (auto &&parameter: function_protobuf.parameters()) {
    ParamIR param_ir(parameter.referenced_type(), parameter.default_arg());
    function_ir.AddParameter(std::move(param_ir));
  }
  // Set Template info
  function_ir.SetTemplateInfo(
      TemplateInfoProtobufToIR(function_protobuf.template_info()));
  return function_ir;
}

VTableLayoutIR ProtobufTextFormatToIRReader::VTableLayoutProtobufToIR(
    const abi_dump::VTableLayout &vtable_layout_protobuf) {
  VTableLayoutIR vtable_layout_ir;
  for (auto &&vtable_component : vtable_layout_protobuf.vtable_components()) {
    VTableComponentIR vtable_component_ir(
        vtable_component.mangled_component_name(),
        VTableComponentKindProtobufToIR(vtable_component.kind()),
        vtable_component.component_value());
    vtable_layout_ir.AddVTableComponent(std::move(vtable_component_ir));
  }
  return vtable_layout_ir;
}

std::vector<RecordFieldIR>
ProtobufTextFormatToIRReader::RecordFieldsProtobufToIR(
    const google::protobuf::RepeatedPtrField<abi_dump::RecordFieldDecl> &rfp) {
  std::vector<RecordFieldIR> record_type_fields_ir;
  for (auto &&field : rfp) {
    RecordFieldIR record_field_ir(field.field_name(), field.referenced_type(),
                                  field.field_offset(),
                                  AccessProtobufToIR(field.access()));
    record_type_fields_ir.emplace_back(std::move(record_field_ir));
  }
  return record_type_fields_ir;
}

std::vector<CXXBaseSpecifierIR>
ProtobufTextFormatToIRReader::RecordCXXBaseSpecifiersProtobufToIR(
    const google::protobuf::RepeatedPtrField<abi_dump::CXXBaseSpecifier> &rbs) {
  std::vector<CXXBaseSpecifierIR> record_type_bases_ir;
  for (auto &&base : rbs) {
    CXXBaseSpecifierIR record_base_ir(
        base.referenced_type(), base.is_virtual(),
        AccessProtobufToIR(base.access()));
    record_type_bases_ir.emplace_back(std::move(record_base_ir));
  }
  return record_type_bases_ir;
}

RecordTypeIR ProtobufTextFormatToIRReader::RecordTypeProtobufToIR(
    const abi_dump::RecordType &record_type_protobuf) {
  RecordTypeIR record_type_ir;
  ReadTypeInfo(record_type_protobuf.type_info(), &record_type_ir);
  record_type_ir.SetTemplateInfo(
      TemplateInfoProtobufToIR(record_type_protobuf.template_info()));
  record_type_ir.SetAccess(AccessProtobufToIR(record_type_protobuf.access()));
  record_type_ir.SetVTableLayout(
      VTableLayoutProtobufToIR(record_type_protobuf.vtable_layout()));
  // Get fields
  record_type_ir.SetRecordFields(RecordFieldsProtobufToIR(
      record_type_protobuf.fields()));
  // Base Specifiers
  record_type_ir.SetCXXBaseSpecifiers(RecordCXXBaseSpecifiersProtobufToIR(
      record_type_protobuf.base_specifiers()));

  return record_type_ir;
}

std::vector<EnumFieldIR>
ProtobufTextFormatToIRReader::EnumFieldsProtobufToIR(
    const google::protobuf::RepeatedPtrField<abi_dump::EnumFieldDecl> &efp) {
  std::vector<EnumFieldIR> enum_type_fields_ir;
  for (auto &&field : efp) {
    EnumFieldIR enum_field_ir(field.name(), field.enum_field_value());
    enum_type_fields_ir.emplace_back(std::move(enum_field_ir));
  }
  return enum_type_fields_ir;
}

EnumTypeIR ProtobufTextFormatToIRReader::EnumTypeProtobufToIR(
    const abi_dump::EnumType &enum_type_protobuf) {
  EnumTypeIR enum_type_ir;
  ReadTypeInfo(enum_type_protobuf.type_info(), &enum_type_ir);
  enum_type_ir.SetUnderlyingType(enum_type_protobuf.underlying_type());
  enum_type_ir.SetAccess(AccessProtobufToIR(enum_type_protobuf.access()));
  enum_type_ir.SetFields(
      EnumFieldsProtobufToIR(enum_type_protobuf.enum_fields()));
  return enum_type_ir;
}

std::vector<GlobalVarIR> ProtobufTextFormatToIRReader::ReadGlobalVariables(
    const abi_dump::TranslationUnit &tu) {
  std::vector<GlobalVarIR> global_variables;
  for (auto &&global_variable_protobuf : tu.global_vars()) {
    GlobalVarIR global_variable_ir;
    global_variable_ir.SetName(global_variable_protobuf.name());
    global_variable_ir.SetSourceFile(global_variable_protobuf.source_file());
    global_variable_ir.SetReferencedType(
        global_variable_protobuf.referenced_type());
    global_variable_ir.SetLinkerSetKey(
        global_variable_protobuf.linker_set_key());
    global_variables.emplace_back(std::move(global_variable_ir));
  }
  return global_variables;
}

std::vector<PointerTypeIR> ProtobufTextFormatToIRReader::ReadPointerTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<PointerTypeIR> pointer_types;
  for (auto &&pointer_type_protobuf : tu.pointer_types()) {
    PointerTypeIR pointer_type_ir;
    ReadTypeInfo(pointer_type_protobuf.type_info(), &pointer_type_ir);
    pointer_types.emplace_back(std::move(pointer_type_ir));
  }
  return pointer_types;
}

std::vector<BuiltinTypeIR> ProtobufTextFormatToIRReader::ReadBuiltinTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<BuiltinTypeIR> builtin_types;
  for (auto &&builtin_type_protobuf : tu.builtin_types()) {
    BuiltinTypeIR builtin_type_ir;
    ReadTypeInfo(builtin_type_protobuf.type_info(), &builtin_type_ir);
    builtin_type_ir.SetSignedness(builtin_type_protobuf.is_unsigned());
    builtin_type_ir.SetIntegralType(builtin_type_protobuf.is_integral());
    builtin_types.emplace_back(std::move(builtin_type_ir));
  }
  return builtin_types;
}

std::vector<QualifiedTypeIR> ProtobufTextFormatToIRReader::ReadQualifiedTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<QualifiedTypeIR> qualified_types;
  for (auto &&qualified_type_protobuf : tu.qualified_types()) {
    QualifiedTypeIR qualified_type_ir;
    ReadTypeInfo(qualified_type_protobuf.type_info(), &qualified_type_ir);
    qualified_types.emplace_back(std::move(qualified_type_ir));
  }
  return qualified_types;
}

std::vector<ArrayTypeIR> ProtobufTextFormatToIRReader::ReadArrayTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<ArrayTypeIR> array_types;
  for (auto &&array_type_protobuf : tu.array_types()) {
    ArrayTypeIR array_type_ir;
    ReadTypeInfo(array_type_protobuf.type_info(), &array_type_ir);
    array_types.emplace_back(std::move(array_type_ir));
  }
  return array_types;
}

std::vector<LvalueReferenceTypeIR>
ProtobufTextFormatToIRReader::ReadLvalueReferenceTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<LvalueReferenceTypeIR> lvalue_reference_types;
  for (auto &&lvalue_reference_type_protobuf : tu.lvalue_reference_types()) {
    LvalueReferenceTypeIR lvalue_reference_type_ir;
    ReadTypeInfo(lvalue_reference_type_protobuf.type_info(),
                 &lvalue_reference_type_ir);
    lvalue_reference_types.emplace_back(std::move(lvalue_reference_type_ir));
  }
  return lvalue_reference_types;
}

std::vector<RvalueReferenceTypeIR>
ProtobufTextFormatToIRReader::ReadRvalueReferenceTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<RvalueReferenceTypeIR> rvalue_reference_types;
  for (auto &&rvalue_reference_type_protobuf : tu.rvalue_reference_types()) {
    RvalueReferenceTypeIR rvalue_reference_type_ir;
    ReadTypeInfo(rvalue_reference_type_protobuf.type_info(),
                 &rvalue_reference_type_ir);
    rvalue_reference_types.emplace_back(std::move(rvalue_reference_type_ir));
  }
  return rvalue_reference_types;
}

std::vector<FunctionIR> ProtobufTextFormatToIRReader::ReadFunctions(
    const abi_dump::TranslationUnit &tu) {
  std::vector<FunctionIR> functions;
  for (auto &&function_protobuf : tu.functions()) {
    FunctionIR function_ir = FunctionProtobufToIR(function_protobuf);
    functions.emplace_back(std::move(function_ir));
  }
  return functions;
}

std::vector<RecordTypeIR> ProtobufTextFormatToIRReader::ReadRecordTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<RecordTypeIR> record_types;
  for (auto &&record_type_protobuf : tu.record_types()) {
    RecordTypeIR record_type_ir = RecordTypeProtobufToIR(record_type_protobuf);
    record_types.emplace_back(std::move(record_type_ir));
  }
  return record_types;
}

std::vector<EnumTypeIR> ProtobufTextFormatToIRReader::ReadEnumTypes(
    const abi_dump::TranslationUnit &tu) {
  std::vector<EnumTypeIR> enum_types;
  for (auto &&enum_type_protobuf : tu.enum_types()) {
    EnumTypeIR enum_type_ir = EnumTypeProtobufToIR(enum_type_protobuf);
    enum_types.emplace_back(std::move(enum_type_ir));
  }
  return enum_types;
}

std::vector<ElfFunctionIR> ProtobufTextFormatToIRReader::ReadElfFunctions(
    const abi_dump::TranslationUnit &tu) {
  std::vector<ElfFunctionIR> elf_functions;
  for (auto &&elf_function : tu.elf_functions()) {
    elf_functions.emplace_back(ElfFunctionIR(elf_function.name()));
  }
  return elf_functions;
}

std::vector<ElfObjectIR> ProtobufTextFormatToIRReader::ReadElfObjects(
    const abi_dump::TranslationUnit &tu) {
  std::vector<ElfObjectIR> elf_objects;
  for (auto &&elf_object : tu.elf_objects()) {
    elf_objects.emplace_back(ElfObjectIR(elf_object.name()));
  }
  return elf_objects;
}

bool IRToProtobufConverter::AddTemplateInformation(
    abi_dump::TemplateInfo *ti, const abi_util::TemplatedArtifactIR *ta) {
  for (auto &&template_element : ta->GetTemplateElements()) {
    abi_dump::TemplateElement *added_element = ti->add_elements();
    if (!added_element) {
      llvm::errs() << "Failed to add template element\n";
      return false;
    }
    added_element->set_referenced_type(template_element.GetReferencedType());
  }
  return true;
}

bool IRToProtobufConverter::AddTypeInfo(
    abi_dump::BasicNamedAndTypedDecl *type_info,
    const TypeIR *typep) {
  if (!type_info || !typep) {
    llvm::errs() << "Typeinfo not valid\n";
    return false;
  }
  type_info->set_linker_set_key(typep->GetLinkerSetKey());
  type_info->set_source_file(typep->GetSourceFile());
  type_info->set_name(typep->GetName());
  type_info->set_size(typep->GetSize());
  type_info->set_alignment(typep->GetAlignment());
  type_info->set_referenced_type(typep->GetReferencedType());
  return true;
}

static void SetIRToProtobufRecordField(
    abi_dump::RecordFieldDecl *record_field_protobuf,
    const RecordFieldIR *record_field_ir) {
  record_field_protobuf->set_field_name(record_field_ir->GetName());
  record_field_protobuf->set_referenced_type(
      record_field_ir->GetReferencedType());
  record_field_protobuf->set_access(
      AccessIRToProtobuf(record_field_ir->GetAccess()));
  record_field_protobuf->set_field_offset(record_field_ir->GetOffset());
}

bool IRToProtobufConverter::AddRecordFields(
    abi_dump::RecordType *record_protobuf,
    const RecordTypeIR *record_ir) {
  // Iterate through the fields and create corresponding ones for the protobuf
  // record
  for (auto &&field_ir : record_ir->GetFields()) {
    abi_dump::RecordFieldDecl *added_field = record_protobuf->add_fields();
    if (!added_field) {
      llvm::errs() << "Couldn't add record field\n";
    }
    SetIRToProtobufRecordField(added_field, &field_ir);
  }
  return true;
}

static bool SetIRToProtobufBaseSpecifier(
    abi_dump::CXXBaseSpecifier *base_specifier_protobuf,
    const CXXBaseSpecifierIR &base_specifier_ir) {
  if (!base_specifier_protobuf) {
    llvm::errs() << "Protobuf base specifier not valid\n";
    return false;
  }
  base_specifier_protobuf->set_referenced_type(
      base_specifier_ir.GetReferencedType());
  base_specifier_protobuf->set_is_virtual(
      base_specifier_ir.IsVirtual());
  base_specifier_protobuf->set_access(
      AccessIRToProtobuf(base_specifier_ir.GetAccess()));
  return true;
}

bool IRToProtobufConverter::AddBaseSpecifiers(
    abi_dump::RecordType *record_protobuf, const RecordTypeIR *record_ir) {
  for (auto &&base_ir : record_ir->GetBases()) {
    abi_dump::CXXBaseSpecifier *added_base =
        record_protobuf->add_base_specifiers();
    if (!SetIRToProtobufBaseSpecifier(added_base, base_ir)) {
      return false;
    }
  }
  return true;
}

static bool SetIRToProtobufVTableLayout(
    abi_dump::VTableLayout *vtable_layout_protobuf,
    const VTableLayoutIR &vtable_layout_ir) {
  if (vtable_layout_protobuf == nullptr) {
    llvm::errs() << "vtable layout protobuf not valid\n";
    return false;
  }
  for (auto &&vtable_component_ir : vtable_layout_ir.GetVTableComponents()) {
    abi_dump::VTableComponent *added_vtable_component =
        vtable_layout_protobuf->add_vtable_components();
    if (!added_vtable_component) {
      llvm::errs() << "Couldn't add vtable component\n";
      return false;
    }
    added_vtable_component->set_kind(
        VTableComponentKindIRToProtobuf(vtable_component_ir.GetKind()));
    added_vtable_component->set_component_value(vtable_component_ir.GetValue());
    added_vtable_component->set_mangled_component_name(
        vtable_component_ir.GetName());
  }
  return true;
}

bool IRToProtobufConverter::AddVTableLayout(
    abi_dump::RecordType *record_protobuf,
    const RecordTypeIR *record_ir) {
  // If there are no entries in the vtable, just return.
  if (record_ir->GetVTableNumEntries() == 0) {
    return true;
  }
  const VTableLayoutIR &vtable_layout_ir = record_ir->GetVTableLayout();
  abi_dump::VTableLayout *vtable_layout_protobuf =
      record_protobuf->mutable_vtable_layout();
  if (!SetIRToProtobufVTableLayout(vtable_layout_protobuf, vtable_layout_ir)) {
    return false;
  }
  return true;
}

abi_dump::RecordType IRToProtobufConverter::ConvertRecordTypeIR(
    const RecordTypeIR *recordp) {
  abi_dump::RecordType added_record_type;
  added_record_type.set_access(AccessIRToProtobuf(recordp->GetAccess()));
  added_record_type.set_record_kind(
      RecordKindIRToProtobuf(recordp->GetRecordKind()));
  if (recordp->IsAnonymous()) {
    added_record_type.set_is_anonymous(true);
  }
  if (!AddTypeInfo(added_record_type.mutable_type_info(), recordp) ||
      !AddRecordFields(&added_record_type, recordp) ||
      !AddBaseSpecifiers(&added_record_type, recordp) ||
      !AddVTableLayout(&added_record_type, recordp) ||
      !(recordp->GetTemplateElements().size() ?
       AddTemplateInformation(added_record_type.mutable_template_info(),
                              recordp) : true)) {
    llvm::errs() << "Template information could not be added\n";
    ::exit(1);
  }
  return added_record_type;
}


abi_dump::ElfObject IRToProtobufConverter::ConvertElfObjectIR(
    const ElfObjectIR *elf_object_ir) {
  abi_dump::ElfObject elf_object_protobuf;
  elf_object_protobuf.set_name(elf_object_ir->GetName());
  return elf_object_protobuf;
}

abi_dump::ElfFunction IRToProtobufConverter::ConvertElfFunctionIR(
    const ElfFunctionIR *elf_function_ir) {
  abi_dump::ElfFunction elf_function_protobuf;
  elf_function_protobuf.set_name(elf_function_ir->GetName());
  return elf_function_protobuf;
}

bool IRToProtobufConverter::AddFunctionParameters(
    abi_dump::FunctionDecl *function_protobuf,
    const FunctionIR *function_ir) {
  for (auto &&parameter : function_ir->GetParameters()) {
    abi_dump::ParamDecl *added_parameter = function_protobuf->add_parameters();
    if (!added_parameter) {
      return false;
    }
    added_parameter->set_referenced_type(
        parameter.GetReferencedType());
    added_parameter->set_default_arg(parameter.GetIsDefault());
  }
  return true;
}

abi_dump::FunctionDecl IRToProtobufConverter::ConvertFunctionIR(
    const FunctionIR *functionp) {
  abi_dump::FunctionDecl added_function;
  added_function.set_access(AccessIRToProtobuf(functionp->GetAccess()));
  added_function.set_linker_set_key(functionp->GetLinkerSetKey());
  added_function.set_source_file(functionp->GetSourceFile());
  added_function.set_function_name(functionp->GetName());
  added_function.set_return_type(functionp->GetReturnType());
  if (!AddFunctionParameters(&added_function, functionp) ||
      !(functionp->GetTemplateElements().size() ?
      AddTemplateInformation(added_function.mutable_template_info(), functionp)
      : true)) {
    llvm::errs() << "Template information could not be added\n";
    ::exit(1);
  }
  return added_function;
}

static bool SetIRToProtobufEnumField(
    abi_dump::EnumFieldDecl *enum_field_protobuf,
    const EnumFieldIR *enum_field_ir) {
  if (enum_field_protobuf == nullptr) {
    return true;
  }
  enum_field_protobuf->set_name(enum_field_ir->GetName());
  enum_field_protobuf->set_enum_field_value(enum_field_ir->GetValue());
  return true;
}

bool IRToProtobufConverter::AddEnumFields(abi_dump::EnumType *enum_protobuf,
                                     const EnumTypeIR *enum_ir) {
  for (auto &&field : enum_ir->GetFields()) {
    abi_dump::EnumFieldDecl *enum_fieldp = enum_protobuf->add_enum_fields();
    if (!SetIRToProtobufEnumField(enum_fieldp, &field)) {
      return false;
    }
  }
  return true;
}


abi_dump::EnumType IRToProtobufConverter::ConvertEnumTypeIR(
    const EnumTypeIR *enump) {
  abi_dump::EnumType added_enum_type;
  added_enum_type.set_access(AccessIRToProtobuf(enump->GetAccess()));
  added_enum_type.set_underlying_type(enump->GetUnderlyingType());
  if (!AddTypeInfo(added_enum_type.mutable_type_info(), enump) ||
      !AddEnumFields(&added_enum_type, enump)) {
    llvm::errs() << "EnumTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_enum_type;
}

abi_dump::GlobalVarDecl IRToProtobufConverter::ConvertGlobalVarIR(
    const GlobalVarIR *global_varp) {
  abi_dump::GlobalVarDecl added_global_var;
  added_global_var.set_referenced_type(global_varp->GetReferencedType());
  added_global_var.set_source_file(global_varp->GetSourceFile());
  added_global_var.set_name(global_varp->GetName());
  added_global_var.set_linker_set_key(global_varp->GetLinkerSetKey());
  added_global_var.set_access(
      AccessIRToProtobuf(global_varp->GetAccess()));
  return added_global_var;
}

abi_dump::PointerType IRToProtobufConverter::ConvertPointerTypeIR(
    const PointerTypeIR *pointerp) {
  abi_dump::PointerType added_pointer_type;
  if (!AddTypeInfo(added_pointer_type.mutable_type_info(), pointerp)) {
    llvm::errs() << "PointerTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_pointer_type;
}

abi_dump::QualifiedType IRToProtobufConverter::ConvertQualifiedTypeIR(
    const QualifiedTypeIR *qualtypep) {
  abi_dump::QualifiedType added_qualified_type;
  if (!AddTypeInfo(added_qualified_type.mutable_type_info(), qualtypep)) {
    llvm::errs() << "QualifiedTypeIR could not be converted\n";
    ::exit(1);
  }
  added_qualified_type.set_is_const(qualtypep->IsConst());
  added_qualified_type.set_is_volatile(qualtypep->IsVolatile());
  added_qualified_type.set_is_restricted(qualtypep->IsRestricted());
  return added_qualified_type;
}

abi_dump::BuiltinType IRToProtobufConverter::ConvertBuiltinTypeIR(
    const BuiltinTypeIR *builtin_typep) {
  abi_dump::BuiltinType added_builtin_type;
  added_builtin_type.set_is_unsigned(builtin_typep->IsUnsigned());
  added_builtin_type.set_is_integral(builtin_typep->IsIntegralType());
  if (!AddTypeInfo(added_builtin_type.mutable_type_info(), builtin_typep)) {
    llvm::errs() << "BuiltinTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_builtin_type;
}

abi_dump::ArrayType IRToProtobufConverter::ConvertArrayTypeIR(
    const ArrayTypeIR *array_typep) {
  abi_dump::ArrayType added_array_type;
  if (!AddTypeInfo(added_array_type.mutable_type_info(), array_typep)) {
    llvm::errs() << "ArrayTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_array_type;
}

abi_dump::LvalueReferenceType
IRToProtobufConverter::ConvertLvalueReferenceTypeIR(
    const LvalueReferenceTypeIR *lvalue_reference_typep) {
  abi_dump::LvalueReferenceType added_lvalue_reference_type;
  if (!AddTypeInfo(added_lvalue_reference_type.mutable_type_info(),
                   lvalue_reference_typep)) {
    llvm::errs() << "LvalueReferenceTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_lvalue_reference_type;
}

abi_dump::RvalueReferenceType
IRToProtobufConverter::ConvertRvalueReferenceTypeIR(
    const RvalueReferenceTypeIR *rvalue_reference_typep) {
  abi_dump::RvalueReferenceType added_rvalue_reference_type;
  if (!AddTypeInfo(added_rvalue_reference_type.mutable_type_info(),
                   rvalue_reference_typep)) {
    llvm::errs() << "RvalueReferenceTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_rvalue_reference_type;
}


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

bool IRDiffToProtobufConverter::AddRecordFieldsRemoved(
    abi_diff::RecordTypeDiff *record_diff_protobuf,
    const std::vector<const RecordFieldIR *> &record_fields_removed_ir) {
  for (auto &&record_field_ir : record_fields_removed_ir) {
    abi_dump::RecordFieldDecl *field_removed =
        record_diff_protobuf->add_fields_removed();
    if (field_removed == nullptr) {
      return false;
    }
    SetIRToProtobufRecordField(field_removed, record_field_ir);
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
  if ( base_specifier_diff_ir != nullptr) {
    abi_diff::CXXBaseSpecifierDiff *base_specifier_diff_protobuf =
        record_type_diff_protobuf.mutable_bases_diff();
    if (!AddBaseSpecifierDiffs(base_specifier_diff_protobuf,
                               base_specifier_diff_ir)) {
      llvm::errs() << "Base Specifier diff could not be added\n";
      ::exit(1);
    }
  }
  // Field diffs
  if (!AddRecordFieldsRemoved(&record_type_diff_protobuf,
                               record_type_diff_ir->GetFieldsRemoved()) ||
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
  *old_function =
      IRToProtobufConverter::ConvertFunctionIR(
          function_diff_ir->GetOldFunction());
  *new_function =
      IRToProtobufConverter::ConvertFunctionIR(
          function_diff_ir->GetNewFunction());
  return function_diff;
}

bool ProtobufIRDumper::AddLinkableMessageIR (const LinkableMessageIR *lm) {
  // No RTTI
  switch (lm->GetKind()) {
    case RecordTypeKind:
      return AddRecordTypeIR(static_cast<const RecordTypeIR *>(lm));
    case EnumTypeKind:
      return AddEnumTypeIR(static_cast<const EnumTypeIR *>(lm));
    case PointerTypeKind:
      return AddPointerTypeIR(static_cast<const PointerTypeIR *>(lm));
    case QualifiedTypeKind:
      return AddQualifiedTypeIR(static_cast<const QualifiedTypeIR *>(lm));
    case ArrayTypeKind:
      return AddArrayTypeIR(static_cast<const ArrayTypeIR *>(lm));
    case LvalueReferenceTypeKind:
      return AddLvalueReferenceTypeIR(
          static_cast<const LvalueReferenceTypeIR *>(lm));
    case RvalueReferenceTypeKind:
      return AddRvalueReferenceTypeIR(
          static_cast<const RvalueReferenceTypeIR*>(lm));
    case BuiltinTypeKind:
      return AddBuiltinTypeIR(static_cast<const BuiltinTypeIR*>(lm));
    case GlobalVarKind:
      return AddGlobalVarIR(static_cast<const GlobalVarIR*>(lm));
    case FunctionKind:
      return AddFunctionIR(static_cast<const FunctionIR*>(lm));
  }
  return false;
}

bool ProtobufIRDumper::AddRecordTypeIR(const RecordTypeIR *recordp) {
  abi_dump::RecordType *added_record_type = tu_ptr_->add_record_types();
  if (!added_record_type) {
    return false;
  }
  *added_record_type = ConvertRecordTypeIR(recordp);
  return true;
}

bool ProtobufIRDumper::AddFunctionIR(const FunctionIR *functionp) {
  abi_dump::FunctionDecl *added_function = tu_ptr_->add_functions();
  if (!added_function) {
    return false;
  }
  *added_function = ConvertFunctionIR(functionp);
  return true;
}

bool ProtobufIRDumper::AddEnumTypeIR(const EnumTypeIR *enump) {
  abi_dump::EnumType *added_enum_type = tu_ptr_->add_enum_types();
  if (!added_enum_type) {
    return false;
  }
  *added_enum_type = ConvertEnumTypeIR(enump);
  return true;
}

bool ProtobufIRDumper::AddGlobalVarIR(const GlobalVarIR *global_varp) {
  abi_dump::GlobalVarDecl *added_global_var = tu_ptr_->add_global_vars();
  if (!added_global_var) {
    return false;
  }
  *added_global_var = ConvertGlobalVarIR(global_varp);
  return true;
}

bool ProtobufIRDumper::AddPointerTypeIR(const PointerTypeIR *pointerp) {
  abi_dump::PointerType *added_pointer_type = tu_ptr_->add_pointer_types();
  if (!added_pointer_type) {
    return false;
  }
  *added_pointer_type = ConvertPointerTypeIR(pointerp);
  return true;
}

bool ProtobufIRDumper::AddQualifiedTypeIR(const QualifiedTypeIR *qualtypep) {
  abi_dump::QualifiedType *added_qualified_type =
      tu_ptr_->add_qualified_types();
  if (!added_qualified_type) {
    return false;
  }
  *added_qualified_type = ConvertQualifiedTypeIR(qualtypep);
  return true;
}

bool ProtobufIRDumper::AddBuiltinTypeIR(const BuiltinTypeIR *builtin_typep) {
  abi_dump::BuiltinType *added_builtin_type =
      tu_ptr_->add_builtin_types();
  if (!added_builtin_type) {
    return false;
  }
  *added_builtin_type = ConvertBuiltinTypeIR(builtin_typep);
  return true;
}

bool ProtobufIRDumper::AddArrayTypeIR(const ArrayTypeIR *array_typep) {
  abi_dump::ArrayType *added_array_type =
      tu_ptr_->add_array_types();
  if (!added_array_type) {
    return false;
  }
  *added_array_type = ConvertArrayTypeIR(array_typep);
  return true;
}

bool ProtobufIRDumper::AddLvalueReferenceTypeIR(
    const LvalueReferenceTypeIR *lvalue_reference_typep) {
  abi_dump::LvalueReferenceType *added_lvalue_reference_type =
      tu_ptr_->add_lvalue_reference_types();
  if (!added_lvalue_reference_type) {
    return false;
  }
  *added_lvalue_reference_type =
      ConvertLvalueReferenceTypeIR(lvalue_reference_typep);
  return true;
}

bool ProtobufIRDumper::AddRvalueReferenceTypeIR(
    const RvalueReferenceTypeIR *rvalue_reference_typep) {
  abi_dump::RvalueReferenceType *added_rvalue_reference_type =
      tu_ptr_->add_rvalue_reference_types();
  if (!added_rvalue_reference_type) {
    return false;
  }
  *added_rvalue_reference_type =
      ConvertRvalueReferenceTypeIR(rvalue_reference_typep);
  return true;
}

bool ProtobufIRDumper::Dump() {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  assert( tu_ptr_.get() != nullptr);
  std::ofstream text_output(dump_path_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  return google::protobuf::TextFormat::Print(*tu_ptr_.get(), &text_os);
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

  if(diff_tu_->removed_elf_functions().size() != 0 ||
     diff_tu_->removed_elf_objects().size() != 0) {
    return CompatibilityStatusIR::ElfIncompatible;
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

  return combined_status;
}

void ProtobufIRDiffDumper::AddCompatibilityStatusIR(
    CompatibilityStatusIR status) {
  diff_tu_->set_compatibility_status(CompatibilityStatusIRToProtobuf(status));
}

bool ProtobufIRDiffDumper::AddDiffMessageIR(
    const DiffMessageIR *message,
    const std::string &type_stack,
    DiffKind diff_kind) {
  switch (message->Kind()) {
    case RecordTypeKind:
      return AddRecordTypeDiffIR(
          static_cast<const RecordTypeDiffIR *>(message),
          type_stack, diff_kind);
    case EnumTypeKind:
      return AddEnumTypeDiffIR(
          static_cast<const EnumTypeDiffIR *>(message),
          type_stack, diff_kind);
    case GlobalVarKind:
      return AddGlobalVarDiffIR(
          static_cast<const GlobalVarDiffIR*>(message),
          type_stack, diff_kind);
    case FunctionKind:
      return AddFunctionDiffIR(
          static_cast<const FunctionDiffIR*>(message),
          type_stack, diff_kind);
    default:
      break;
  }
  llvm::errs() << "Dump Diff attempted on something not a user defined type" <<
                   "/ function / global variable\n";
  return false;
}

bool ProtobufIRDiffDumper::AddLinkableMessageIR(
    const LinkableMessageIR *message,
    DiffKind diff_kind) {
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
  llvm::errs() << "Dump Diff attempted on something not a user defined type" <<
                   "/ function / global variable\n";
  return false;
}

bool ProtobufIRDiffDumper::AddElfSymbolMessageIR (const ElfSymbolIR *elf_symbol,
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
  switch(diff_kind) {
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
  switch(diff_kind) {
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
    const RecordTypeIR *record_type_ir,
    DiffKind diff_kind) {
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
    const FunctionIR *function_ir,
    DiffKind diff_kind) {
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
    const RecordTypeDiffIR *record_diff_ir,
    const std::string &type_stack,
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
    const GlobalVarDiffIR *global_var_diff_ir,
    const std::string &type_stack,
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

} //abi_util

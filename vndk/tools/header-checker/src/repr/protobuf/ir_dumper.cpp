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

#include "repr/protobuf/ir_dumper.h"

#include "repr/protobuf/abi_dump.h"
#include "repr/protobuf/api.h"
#include "repr/protobuf/converter.h"

#include <fstream>
#include <memory>

#include <llvm/Support/raw_ostream.h>

#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/text_format.h>


namespace header_checker {
namespace repr {

static bool AddTemplateInformation(abi_dump::TemplateInfo *ti,
                                   const TemplatedArtifactIR *ta) {
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

static bool AddTypeInfo(abi_dump::BasicNamedAndTypedDecl *type_info,
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
  type_info->set_self_type(typep->GetSelfType());
  return true;
}

template <typename HasAvailabilityAttrsMessage>
void AddAvailabilityAttrs(HasAvailabilityAttrsMessage *decl_protobuf,
                          const HasAvailabilityAttrs *decl_ir) {
  for (const AvailabilityAttrIR &attr : decl_ir->GetAvailabilityAttrs()) {
    abi_dump::AvailabilityAttr *attr_protobuf =
        decl_protobuf->add_availability_attrs();
    if (auto introduced = attr.GetIntroduced(); introduced.has_value()) {
      attr_protobuf->set_introduced_major(introduced.value());
    }
    if (auto deprecated = attr.GetDeprecated(); deprecated.has_value()) {
      attr_protobuf->set_deprecated_major(deprecated.value());
    }
    if (auto obsoleted = attr.GetObsoleted(); obsoleted.has_value()) {
      attr_protobuf->set_obsoleted_major(obsoleted.value());
    }
    if (attr.IsUnavailable()) {
      attr_protobuf->set_unavailable(true);
    }
  }
}

bool ConvertRecordFieldIR(abi_dump::RecordFieldDecl *record_field_protobuf,
                          const RecordFieldIR *record_field_ir) {
  if (!record_field_protobuf) {
    llvm::errs() << "Invalid record field\n";
    return false;
  }
  record_field_protobuf->set_field_name(record_field_ir->GetName());
  record_field_protobuf->set_referenced_type(
      record_field_ir->GetReferencedType());
  record_field_protobuf->set_access(
      AccessIRToProtobuf(record_field_ir->GetAccess()));
  record_field_protobuf->set_field_offset(record_field_ir->GetOffset());
  if (record_field_ir->IsBitField()) {
    record_field_protobuf->set_is_bit_field(true);
    record_field_protobuf->set_bit_width(record_field_ir->GetBitWidth());
  }
  AddAvailabilityAttrs(record_field_protobuf, record_field_ir);
  return true;
}

static bool AddRecordFields(abi_dump::RecordType *record_protobuf,
                            const RecordTypeIR *record_ir) {
  for (auto &&field_ir : record_ir->GetFields()) {
    abi_dump::RecordFieldDecl *added_field = record_protobuf->add_fields();
    if (!ConvertRecordFieldIR(added_field, &field_ir)) {
      return false;
    }
  }
  return true;
}

bool ConvertCXXBaseSpecifierIR(
    abi_dump::CXXBaseSpecifier *base_specifier_protobuf,
    const CXXBaseSpecifierIR &base_specifier_ir) {
  if (!base_specifier_protobuf) {
    llvm::errs() << "Protobuf base specifier not valid\n";
    return false;
  }
  base_specifier_protobuf->set_referenced_type(
      base_specifier_ir.GetReferencedType());
  base_specifier_protobuf->set_is_virtual(base_specifier_ir.IsVirtual());
  base_specifier_protobuf->set_access(
      AccessIRToProtobuf(base_specifier_ir.GetAccess()));
  return true;
}

static bool AddBaseSpecifiers(abi_dump::RecordType *record_protobuf,
                              const RecordTypeIR *record_ir) {
  for (auto &&base_ir : record_ir->GetBases()) {
    abi_dump::CXXBaseSpecifier *added_base =
        record_protobuf->add_base_specifiers();
    if (!ConvertCXXBaseSpecifierIR(added_base, base_ir)) {
      return false;
    }
  }
  return true;
}

bool ConvertVTableLayoutIR(abi_dump::VTableLayout *vtable_layout_protobuf,
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
    added_vtable_component->set_is_pure(vtable_component_ir.GetIsPure());
  }
  return true;
}

static bool AddVTableLayout(abi_dump::RecordType *record_protobuf,
                            const RecordTypeIR *record_ir) {
  // If there are no entries in the vtable, just return.
  if (record_ir->GetVTableNumEntries() == 0) {
    return true;
  }
  const VTableLayoutIR &vtable_layout_ir = record_ir->GetVTableLayout();
  abi_dump::VTableLayout *vtable_layout_protobuf =
      record_protobuf->mutable_vtable_layout();
  if (!ConvertVTableLayoutIR(vtable_layout_protobuf, vtable_layout_ir)) {
    return false;
  }
  return true;
}

abi_dump::RecordType ConvertRecordTypeIR(const RecordTypeIR *recordp) {
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
  AddAvailabilityAttrs(&added_record_type, recordp);
  return added_record_type;
}

abi_dump::ElfObject ConvertElfObjectIR(const ElfObjectIR *elf_object_ir) {
  abi_dump::ElfObject elf_object_protobuf;
  elf_object_protobuf.set_name(elf_object_ir->GetName());
  elf_object_protobuf.set_binding(
      ElfSymbolBindingIRToProtobuf(elf_object_ir->GetBinding()));
  return elf_object_protobuf;
}

abi_dump::ElfFunction ConvertElfFunctionIR(
    const ElfFunctionIR *elf_function_ir) {
  abi_dump::ElfFunction elf_function_protobuf;
  elf_function_protobuf.set_name(elf_function_ir->GetName());
  elf_function_protobuf.set_binding(
      ElfSymbolBindingIRToProtobuf(elf_function_ir->GetBinding()));
  return elf_function_protobuf;
}

template <typename CFunctionLikeMessage>
static bool AddFunctionParametersAndSetReturnType(
    CFunctionLikeMessage *function_like_protobuf,
    const CFunctionLikeIR *cfunction_like_ir) {
  function_like_protobuf->set_return_type(cfunction_like_ir->GetReturnType());
  return AddFunctionParameters(function_like_protobuf, cfunction_like_ir);
}

template <typename CFunctionLikeMessage>
static bool AddFunctionParameters(CFunctionLikeMessage *function_like_protobuf,
                                  const CFunctionLikeIR *cfunction_like_ir) {
  for (auto &&parameter : cfunction_like_ir->GetParameters()) {
    abi_dump::ParamDecl *added_parameter =
        function_like_protobuf->add_parameters();
    if (!added_parameter) {
      return false;
    }
    added_parameter->set_referenced_type(
        parameter.GetReferencedType());
    added_parameter->set_default_arg(parameter.GetIsDefault());
    added_parameter->set_is_this_ptr(parameter.GetIsThisPtr());
  }
  return true;
}

static abi_dump::FunctionType ConvertFunctionTypeIR(
    const FunctionTypeIR *function_typep) {
  abi_dump::FunctionType added_function_type;
  if (!AddTypeInfo(added_function_type.mutable_type_info(), function_typep) ||
      !AddFunctionParametersAndSetReturnType(&added_function_type,
                                             function_typep)) {
    llvm::errs() << "Could not convert FunctionTypeIR to protobuf\n";
    ::exit(1);
  }
  return added_function_type;
}

abi_dump::FunctionDecl ConvertFunctionIR(const FunctionIR *functionp) {
  abi_dump::FunctionDecl added_function;
  added_function.set_access(AccessIRToProtobuf(functionp->GetAccess()));
  added_function.set_linker_set_key(functionp->GetLinkerSetKey());
  added_function.set_source_file(functionp->GetSourceFile());
  added_function.set_function_name(functionp->GetName());
  if (!AddFunctionParametersAndSetReturnType(&added_function, functionp) ||
      !(functionp->GetTemplateElements().size() ?
      AddTemplateInformation(added_function.mutable_template_info(), functionp)
      : true)) {
    llvm::errs() << "Template information could not be added\n";
    ::exit(1);
  }
  AddAvailabilityAttrs(&added_function, functionp);
  return added_function;
}

bool ConvertEnumFieldIR(abi_dump::EnumFieldDecl *enum_field_protobuf,
                        const EnumFieldIR *enum_field_ir) {
  if (enum_field_protobuf == nullptr) {
    llvm::errs() << "Invalid enum field\n";
    return false;
  }
  enum_field_protobuf->set_name(enum_field_ir->GetName());
  // The "enum_field_value" in the .proto is a signed 64-bit integer. An
  // unsigned integer >= (1 << 63) is represented with a negative integer in the
  // dump file. Despite the wrong representation, the diff result isn't affected
  // because every integer has a unique representation.
  enum_field_protobuf->set_enum_field_value(enum_field_ir->GetSignedValue());
  AddAvailabilityAttrs(enum_field_protobuf, enum_field_ir);
  return true;
}

static bool AddEnumFields(abi_dump::EnumType *enum_protobuf,
                          const EnumTypeIR *enum_ir) {
  for (auto &&field : enum_ir->GetFields()) {
    abi_dump::EnumFieldDecl *enum_fieldp = enum_protobuf->add_enum_fields();
    if (!ConvertEnumFieldIR(enum_fieldp, &field)) {
      return false;
    }
  }
  return true;
}

abi_dump::EnumType ConvertEnumTypeIR(const EnumTypeIR *enump) {
  abi_dump::EnumType added_enum_type;
  added_enum_type.set_access(AccessIRToProtobuf(enump->GetAccess()));
  added_enum_type.set_underlying_type(enump->GetUnderlyingType());
  if (!AddTypeInfo(added_enum_type.mutable_type_info(), enump) ||
      !AddEnumFields(&added_enum_type, enump)) {
    llvm::errs() << "EnumTypeIR could not be converted\n";
    ::exit(1);
  }
  AddAvailabilityAttrs(&added_enum_type, enump);
  return added_enum_type;
}

abi_dump::GlobalVarDecl ConvertGlobalVarIR(const GlobalVarIR *global_varp) {
  abi_dump::GlobalVarDecl added_global_var;
  added_global_var.set_referenced_type(global_varp->GetReferencedType());
  added_global_var.set_source_file(global_varp->GetSourceFile());
  added_global_var.set_name(global_varp->GetName());
  added_global_var.set_linker_set_key(global_varp->GetLinkerSetKey());
  added_global_var.set_access(
      AccessIRToProtobuf(global_varp->GetAccess()));
  AddAvailabilityAttrs(&added_global_var, global_varp);
  return added_global_var;
}

static abi_dump::PointerType ConvertPointerTypeIR(
    const PointerTypeIR *pointerp) {
  abi_dump::PointerType added_pointer_type;
  if (!AddTypeInfo(added_pointer_type.mutable_type_info(), pointerp)) {
    llvm::errs() << "PointerTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_pointer_type;
}

static abi_dump::QualifiedType ConvertQualifiedTypeIR(
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

static abi_dump::BuiltinType ConvertBuiltinTypeIR(
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

static abi_dump::ArrayType ConvertArrayTypeIR(const ArrayTypeIR *array_typep) {
  abi_dump::ArrayType added_array_type;
  added_array_type.set_is_of_unknown_bound(array_typep->IsOfUnknownBound());
  if (!AddTypeInfo(added_array_type.mutable_type_info(), array_typep)) {
    llvm::errs() << "ArrayTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_array_type;
}

static abi_dump::LvalueReferenceType ConvertLvalueReferenceTypeIR(
    const LvalueReferenceTypeIR *lvalue_reference_typep) {
  abi_dump::LvalueReferenceType added_lvalue_reference_type;
  if (!AddTypeInfo(added_lvalue_reference_type.mutable_type_info(),
                   lvalue_reference_typep)) {
    llvm::errs() << "LvalueReferenceTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_lvalue_reference_type;
}

static abi_dump::RvalueReferenceType ConvertRvalueReferenceTypeIR(
    const RvalueReferenceTypeIR *rvalue_reference_typep) {
  abi_dump::RvalueReferenceType added_rvalue_reference_type;
  if (!AddTypeInfo(added_rvalue_reference_type.mutable_type_info(),
                   rvalue_reference_typep)) {
    llvm::errs() << "RvalueReferenceTypeIR could not be converted\n";
    ::exit(1);
  }
  return added_rvalue_reference_type;
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
    case FunctionTypeKind:
      return AddFunctionTypeIR(static_cast<const FunctionTypeIR*>(lm));
    case GlobalVarKind:
      return AddGlobalVarIR(static_cast<const GlobalVarIR*>(lm));
    case FunctionKind:
      return AddFunctionIR(static_cast<const FunctionIR*>(lm));
  }
  return false;
}

bool ProtobufIRDumper::AddElfFunctionIR(const ElfFunctionIR *elf_function) {
  abi_dump::ElfFunction *added_elf_function = tu_ptr_->add_elf_functions();
  if (!added_elf_function) {
    return false;
  }
  *added_elf_function = ConvertElfFunctionIR(elf_function);
  return true;
}

bool ProtobufIRDumper::AddElfObjectIR(const ElfObjectIR *elf_object) {
  abi_dump::ElfObject *added_elf_object = tu_ptr_->add_elf_objects();
  if (!added_elf_object) {
    return false;
  }
  *added_elf_object = ConvertElfObjectIR(elf_object);
  return true;
}

bool ProtobufIRDumper::AddElfSymbolMessageIR(const ElfSymbolIR *em) {
  switch (em->GetKind()) {
    case ElfSymbolIR::ElfFunctionKind:
      return AddElfFunctionIR(static_cast<const ElfFunctionIR *>(em));
    case ElfSymbolIR::ElfObjectKind:
      return AddElfObjectIR(static_cast<const ElfObjectIR *>(em));
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

bool ProtobufIRDumper::AddFunctionTypeIR(const FunctionTypeIR *function_typep) {
  abi_dump::FunctionType *added_function_type = tu_ptr_->add_function_types();
  if (!added_function_type) {
    return false;
  }
  *added_function_type = ConvertFunctionTypeIR(function_typep);
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

bool ProtobufIRDumper::Dump(const ModuleIR &module) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  DumpModule(module);
  assert( tu_ptr_.get() != nullptr);
  std::ofstream text_output(dump_path_);
  {
    google::protobuf::io::OstreamOutputStream text_os(&text_output);
    if (!google::protobuf::TextFormat::Print(*tu_ptr_.get(), &text_os)) {
      return false;
    }
  }
  return text_output.flush().good();
}

std::unique_ptr<IRDumper> CreateProtobufIRDumper(const std::string &dump_path) {
  return std::make_unique<ProtobufIRDumper>(dump_path);
}


}  // namespace repr
}  // namespace header_checker

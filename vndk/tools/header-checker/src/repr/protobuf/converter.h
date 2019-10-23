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

#ifndef HEADER_CHECKER_PROTOBUF_CONVERTER_H_
#define HEADER_CHECKER_PROTOBUF_CONVERTER_H_

#include "repr/ir_diff_representation.h"
#include "repr/ir_representation.h"
#include "repr/protobuf/abi_diff.h"
#include "repr/protobuf/abi_dump.h"

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace repr {


inline abi_diff::CompatibilityStatus CompatibilityStatusIRToProtobuf(
    CompatibilityStatusIR status) {
  switch (status) {
    case CompatibilityStatusIR::Incompatible:
      return abi_diff::CompatibilityStatus::INCOMPATIBLE;
    case CompatibilityStatusIR::Extension:
      return abi_diff::CompatibilityStatus::EXTENSION;
    default:
      break;
  }
  return abi_diff::CompatibilityStatus::COMPATIBLE;
}

inline abi_dump::ElfSymbolBinding ElfSymbolBindingIRToProtobuf(
    ElfSymbolIR::ElfSymbolBinding binding) {
  switch (binding) {
    case ElfSymbolIR::ElfSymbolBinding::Global:
      return abi_dump::ElfSymbolBinding::Global;
    case ElfSymbolIR::ElfSymbolBinding::Weak:
      return abi_dump::ElfSymbolBinding::Weak;
  }
  // We skip symbols of all other Bindings
  // TODO: Add all bindings, don't leave out info
  assert(0);
}

inline ElfSymbolIR::ElfSymbolBinding ElfSymbolBindingProtobufToIR(
    abi_dump::ElfSymbolBinding binding) {
  switch (binding) {
    case abi_dump::ElfSymbolBinding::Global:
      return ElfSymbolIR::ElfSymbolBinding::Global;
    case abi_dump::ElfSymbolBinding::Weak:
      return ElfSymbolIR::ElfSymbolBinding::Weak;
  }
  // We skip symbols of all other Bindings
  assert(0);
}

inline abi_dump::AccessSpecifier AccessIRToProtobuf(AccessSpecifierIR access) {
  switch (access) {
    case AccessSpecifierIR::ProtectedAccess:
      return abi_dump::AccessSpecifier::protected_access;
    case AccessSpecifierIR::PrivateAccess:
      return abi_dump::AccessSpecifier::private_access;
    default:
      return abi_dump::AccessSpecifier::public_access;
  }
  return abi_dump::AccessSpecifier::public_access;
}

inline AccessSpecifierIR AccessProtobufToIR(
    abi_dump::AccessSpecifier access) {
  switch (access) {
    case abi_dump::AccessSpecifier::protected_access:
      return AccessSpecifierIR::ProtectedAccess;
    case abi_dump::AccessSpecifier::private_access:
      return AccessSpecifierIR::PrivateAccess;
    default:
      return AccessSpecifierIR::PublicAccess;
  }
  return AccessSpecifierIR::PublicAccess;
}

inline abi_dump::RecordKind RecordKindIRToProtobuf(
    RecordTypeIR::RecordKind kind) {
  switch (kind) {
    case RecordTypeIR::RecordKind::struct_kind:
      return abi_dump::RecordKind::struct_kind;

    case RecordTypeIR::RecordKind::class_kind:
      return abi_dump::RecordKind::class_kind;

    case RecordTypeIR::RecordKind::union_kind:
      return abi_dump::RecordKind::union_kind;

    default:
      return abi_dump::RecordKind::struct_kind;
  }
  // Should not be reached
  assert(false);
}

inline RecordTypeIR::RecordKind RecordKindProtobufToIR(
    abi_dump::RecordKind kind) {
  switch (kind) {
    case abi_dump::RecordKind::struct_kind:
      return RecordTypeIR::struct_kind;

    case abi_dump::RecordKind::class_kind:
      return RecordTypeIR::class_kind;

    case abi_dump::RecordKind::union_kind:
      return RecordTypeIR::union_kind;

    default:
      return RecordTypeIR::struct_kind;
  }
  // Should not be reached
  assert(false);
}

inline abi_dump::VTableComponent::Kind VTableComponentKindIRToProtobuf(
    VTableComponentIR::Kind kind) {
  switch (kind) {
    case VTableComponentIR::Kind::VCallOffset:
      return abi_dump::VTableComponent_Kind_VCallOffset;

    case VTableComponentIR::Kind::VBaseOffset:
      return abi_dump::VTableComponent_Kind_VBaseOffset;

    case VTableComponentIR::Kind::OffsetToTop:
      return abi_dump::VTableComponent_Kind_OffsetToTop;

    case VTableComponentIR::Kind::RTTI:
      return abi_dump::VTableComponent_Kind_RTTI;

    case VTableComponentIR::Kind::FunctionPointer:
      return abi_dump::VTableComponent_Kind_FunctionPointer;

    case VTableComponentIR::Kind::CompleteDtorPointer:
      return abi_dump::VTableComponent_Kind_CompleteDtorPointer;

    case VTableComponentIR::Kind::DeletingDtorPointer:
      return abi_dump::VTableComponent_Kind_DeletingDtorPointer;

    default:
      return abi_dump::VTableComponent_Kind_UnusedFunctionPointer;
  }
  // Should not be reached
  assert(false);
}

inline VTableComponentIR::Kind VTableComponentKindProtobufToIR(
    abi_dump::VTableComponent_Kind kind) {
  switch (kind) {
    case abi_dump::VTableComponent_Kind_VCallOffset:
      return VTableComponentIR::Kind::VCallOffset;

    case abi_dump::VTableComponent_Kind_VBaseOffset:
      return VTableComponentIR::Kind::VBaseOffset;

    case abi_dump::VTableComponent_Kind_OffsetToTop:
      return VTableComponentIR::Kind::OffsetToTop;

    case abi_dump::VTableComponent_Kind_RTTI:
      return VTableComponentIR::Kind::RTTI;

    case abi_dump::VTableComponent_Kind_FunctionPointer:
      return VTableComponentIR::Kind::FunctionPointer;

    case abi_dump::VTableComponent_Kind_CompleteDtorPointer:
      return VTableComponentIR::Kind::CompleteDtorPointer;

    case abi_dump::VTableComponent_Kind_DeletingDtorPointer:
      return VTableComponentIR::Kind::DeletingDtorPointer;

    default:
      return VTableComponentIR::Kind::UnusedFunctionPointer;
  }
  // Should not be reached
  assert(false);
}

class IRToProtobufConverter {
 private:
  static bool AddTemplateInformation(
      abi_dump::TemplateInfo *ti, const TemplatedArtifactIR *ta);

  static bool AddTypeInfo(
      abi_dump::BasicNamedAndTypedDecl *type_info, const TypeIR *typep);

  static bool AddRecordFields(
      abi_dump::RecordType *record_protobuf, const RecordTypeIR *record_ir);

  static bool AddBaseSpecifiers(
      abi_dump::RecordType *record_protobuf, const RecordTypeIR *record_ir);

  static bool AddVTableLayout(
      abi_dump::RecordType *record_protobuf, const RecordTypeIR *record_ir);

  static bool AddEnumFields(abi_dump::EnumType *enum_protobuf,
                            const EnumTypeIR *enum_ir);

 public:
  static abi_dump::EnumType ConvertEnumTypeIR(const EnumTypeIR *enump);

  static abi_dump::RecordType ConvertRecordTypeIR(const RecordTypeIR *recordp);

  static abi_dump::FunctionType ConvertFunctionTypeIR (
      const FunctionTypeIR *function_typep);

  template <typename CFunctionLikeMessage>
  static bool AddFunctionParametersAndSetReturnType(
      CFunctionLikeMessage *function_like_protobuf,
      const CFunctionLikeIR *cfunction_like_ir);

  template <typename CFunctionLikeMessage>
  static bool AddFunctionParameters(CFunctionLikeMessage *function_protobuf,
                                    const CFunctionLikeIR *cfunction_like_ir);

  static abi_dump::FunctionDecl ConvertFunctionIR(const FunctionIR *functionp);

  static abi_dump::GlobalVarDecl ConvertGlobalVarIR(
      const GlobalVarIR *global_varp);

  static abi_dump::PointerType ConvertPointerTypeIR(
      const PointerTypeIR *pointerp);

  static abi_dump::QualifiedType ConvertQualifiedTypeIR(
      const QualifiedTypeIR *qualtypep);

  static abi_dump::BuiltinType ConvertBuiltinTypeIR(
      const BuiltinTypeIR *builtin_typep);

  static abi_dump::ArrayType ConvertArrayTypeIR(
      const ArrayTypeIR *array_typep);

  static abi_dump::LvalueReferenceType ConvertLvalueReferenceTypeIR(
      const LvalueReferenceTypeIR *lvalue_reference_typep);

  static abi_dump::RvalueReferenceType ConvertRvalueReferenceTypeIR(
      const RvalueReferenceTypeIR *rvalue_reference_typep);

  static abi_dump::ElfFunction ConvertElfFunctionIR(
      const ElfFunctionIR *elf_function_ir);

  static abi_dump::ElfObject ConvertElfObjectIR(
      const ElfObjectIR *elf_object_ir);
};

class IRDiffToProtobufConverter {
 private:
  static bool AddTypeInfoDiff(
      abi_diff::TypeInfoDiff *type_info_diff_protobuf,
      const TypeDiffIR *type_diff_ir);

  static bool AddVTableLayoutDiff(
      abi_diff::VTableLayoutDiff *vtable_layout_diff_protobuf,
      const VTableLayoutDiffIR *vtable_layout_diff_ir);

  static bool AddBaseSpecifierDiffs(
      abi_diff::CXXBaseSpecifierDiff *base_specifier_diff_protobuf,
      const CXXBaseSpecifierDiffIR *base_specifier_diff_ir);

  static bool AddRecordFields(
      abi_diff::RecordTypeDiff *record_diff_protobuf,
      const std::vector<const RecordFieldIR *> &record_fields_removed_ir,
      bool removed);

  static bool AddRecordFieldDiffs(
      abi_diff::RecordTypeDiff *record_diff_protobuf,
      const std::vector<RecordFieldDiffIR> &record_field_diff_ir);

  static bool AddEnumUnderlyingTypeDiff(
      abi_diff::UnderlyingTypeDiff *underlying_type_diff_protobuf,
      const std::pair<std::string, std::string> *underlying_type_diff_ir);

 public:
  static abi_diff::RecordTypeDiff ConvertRecordTypeDiffIR(
      const RecordTypeDiffIR *record_type_diffp);

  static abi_diff::EnumTypeDiff ConvertEnumTypeDiffIR(
      const EnumTypeDiffIR *enum_type_diffp);

  static abi_diff::FunctionDeclDiff ConvertFunctionDiffIR(
      const FunctionDiffIR *function_diffp);

  static abi_diff::GlobalVarDeclDiff ConvertGlobalVarDiffIR(
      const GlobalVarDiffIR *global_var_diffp);
};

inline void SetIRToProtobufRecordField(
    abi_dump::RecordFieldDecl *record_field_protobuf,
    const RecordFieldIR *record_field_ir) {
  record_field_protobuf->set_field_name(record_field_ir->GetName());
  record_field_protobuf->set_referenced_type(
      record_field_ir->GetReferencedType());
  record_field_protobuf->set_access(
      AccessIRToProtobuf(record_field_ir->GetAccess()));
  record_field_protobuf->set_field_offset(record_field_ir->GetOffset());
}

inline bool SetIRToProtobufBaseSpecifier(
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

inline bool SetIRToProtobufVTableLayout(
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
    added_vtable_component->set_is_pure(vtable_component_ir.GetIsPure());
  }
  return true;
}

inline bool SetIRToProtobufEnumField(
    abi_dump::EnumFieldDecl *enum_field_protobuf,
    const EnumFieldIR *enum_field_ir) {
  if (enum_field_protobuf == nullptr) {
    return true;
  }
  enum_field_protobuf->set_name(enum_field_ir->GetName());
  enum_field_protobuf->set_enum_field_value(enum_field_ir->GetValue());
  return true;
}


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_PROTOBUF_CONVERTER_H_

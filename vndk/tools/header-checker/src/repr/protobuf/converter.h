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

// Convert IR to the messages defined in abi_dump.proto.
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
  static bool ConvertRecordFieldIR(
      abi_dump::RecordFieldDecl *record_field_protobuf,
      const RecordFieldIR *record_field_ir);

  static bool ConvertCXXBaseSpecifierIR(
      abi_dump::CXXBaseSpecifier *base_specifier_protobuf,
      const CXXBaseSpecifierIR &base_specifier_ir);

  static bool ConvertVTableLayoutIR(
      abi_dump::VTableLayout *vtable_layout_protobuf,
      const VTableLayoutIR &vtable_layout_ir);

  static bool ConvertEnumFieldIR(abi_dump::EnumFieldDecl *enum_field_protobuf,
                                 const EnumFieldIR *enum_field_ir);

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


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_PROTOBUF_CONVERTER_H_

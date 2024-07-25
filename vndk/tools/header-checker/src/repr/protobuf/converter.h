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
  if (status & (CompatibilityStatusIR::Incompatible |
                CompatibilityStatusIR::ElfIncompatible |
                CompatibilityStatusIR::UnreferencedChanges)) {
    return abi_diff::CompatibilityStatus::INCOMPATIBLE;
  }
  if (status & (CompatibilityStatusIR::Extension |
                CompatibilityStatusIR::ElfExtension)) {
    return abi_diff::CompatibilityStatus::EXTENSION;
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

// ProtobufIRDiffDumper calls the following functions.
bool ConvertVTableLayoutIR(abi_dump::VTableLayout *vtable_layout_protobuf,
                           const VTableLayoutIR &vtable_layout_ir);

bool ConvertCXXBaseSpecifierIR(
    abi_dump::CXXBaseSpecifier *base_specifier_protobuf,
    const CXXBaseSpecifierIR &base_specifier_ir);

bool ConvertRecordFieldIR(abi_dump::RecordFieldDecl *record_field_protobuf,
                          const RecordFieldIR *record_field_ir);

bool ConvertEnumFieldIR(abi_dump::EnumFieldDecl *enum_field_protobuf,
                        const EnumFieldIR *enum_field_ir);

abi_dump::FunctionDecl ConvertFunctionIR(const FunctionIR *functionp);

abi_dump::GlobalVarDecl ConvertGlobalVarIR(const GlobalVarIR *global_varp);

abi_dump::ElfFunction ConvertElfFunctionIR(
    const ElfFunctionIR *elf_function_ir);

abi_dump::ElfObject ConvertElfObjectIR(const ElfObjectIR *elf_object_ir);

abi_dump::RecordType ConvertRecordTypeIR(const RecordTypeIR *recordp);

abi_dump::EnumType ConvertEnumTypeIR(const EnumTypeIR *enump);


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_PROTOBUF_CONVERTER_H_

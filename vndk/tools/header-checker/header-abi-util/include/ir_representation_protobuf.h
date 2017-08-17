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
#ifndef IR_PROTOBUF_
#define IR_PROTOBUF_

#include <ir_representation.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#include "proto/abi_diff.pb.h"
#pragma clang diagnostic pop

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>


// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.
namespace abi_util {

inline abi_diff::CompatibilityStatus CompatibilityStatusIRToProtobuf(
    CompatibilityStatusIR status) {
  switch(status) {
    case CompatibilityStatusIR::Incompatible:
      return abi_diff::CompatibilityStatus::INCOMPATIBLE;
    case CompatibilityStatusIR::Extension:
      return abi_diff::CompatibilityStatus::EXTENSION;
    default:
      break;
  }
  return abi_diff::CompatibilityStatus::COMPATIBLE;
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
    abi_dump::TemplateInfo *ti, const abi_util::TemplatedArtifactIR *ta);

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

  static bool AddFunctionParameters(abi_dump::FunctionDecl *function_protobuf,
                                    const FunctionIR *function_ir);

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
  static bool AddTypeInfoDiff(abi_diff::TypeInfoDiff *type_info_diff_protobuf,
                              const TypeDiffIR *type_diff_ir);

  static bool AddVTableLayoutDiff(
    abi_diff::VTableLayoutDiff *vtable_layout_diff_protobuf,
    const VTableLayoutDiffIR *vtable_layout_diff_ir);

  static bool AddBaseSpecifierDiffs(
    abi_diff::CXXBaseSpecifierDiff *base_specifier_diff_protobuf,
    const CXXBaseSpecifierDiffIR *base_specifier_diff_ir);

  static bool AddRecordFieldsRemoved(
    abi_diff::RecordTypeDiff *record_diff_protobuf,
    const std::vector<const RecordFieldIR *> &record_fields_removed_ir);

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

class ProtobufIRDumper : public IRDumper, public IRToProtobufConverter {
 private:
  // Types
  bool AddRecordTypeIR(const RecordTypeIR *);

  bool AddEnumTypeIR(const EnumTypeIR *);

  bool AddPointerTypeIR(const PointerTypeIR *);

  bool AddQualifiedTypeIR(const QualifiedTypeIR *);

  bool AddLvalueReferenceTypeIR(const LvalueReferenceTypeIR *);

  bool AddRvalueReferenceTypeIR(const RvalueReferenceTypeIR *);

  bool AddArrayTypeIR(const ArrayTypeIR *);

  bool AddBuiltinTypeIR(const BuiltinTypeIR *);

  // Functions and global variables.
  bool AddFunctionIR(const FunctionIR *);

  bool AddGlobalVarIR(const GlobalVarIR *);

 public:
  ProtobufIRDumper(const std::string &dump_path)
      : IRDumper(dump_path), tu_ptr_(new abi_dump::TranslationUnit()) { }

  bool AddLinkableMessageIR(const LinkableMessageIR *);

  bool Dump() override;

  ~ProtobufIRDumper() override { }

 private:
  std::unique_ptr<abi_dump::TranslationUnit> tu_ptr_;
};


class ProtobufTextFormatToIRReader : public TextFormatToIRReader {
 public:

  virtual bool ReadDump() override;

  ProtobufTextFormatToIRReader(const std::string &dump_path)
      : TextFormatToIRReader(dump_path) { }

 private:
  std::vector<FunctionIR> ReadFunctions(
       const abi_dump::TranslationUnit &tu);

  std::vector<GlobalVarIR> ReadGlobalVariables(
       const abi_dump::TranslationUnit &tu);

  std::vector<EnumTypeIR> ReadEnumTypes(const abi_dump::TranslationUnit &tu);

  std::vector<RecordTypeIR> ReadRecordTypes(
      const abi_dump::TranslationUnit &tu);

  std::vector<PointerTypeIR> ReadPointerTypes(
       const abi_dump::TranslationUnit &tu);

  std::vector<BuiltinTypeIR> ReadBuiltinTypes(
       const abi_dump::TranslationUnit &tu);

  std::vector<QualifiedTypeIR> ReadQualifiedTypes(
       const abi_dump::TranslationUnit &tu);

  std::vector<ArrayTypeIR> ReadArrayTypes(const abi_dump::TranslationUnit &tu);

  std::vector<LvalueReferenceTypeIR> ReadLvalueReferenceTypes(
       const abi_dump::TranslationUnit &tu);

  std::vector<RvalueReferenceTypeIR> ReadRvalueReferenceTypes(
       const abi_dump::TranslationUnit &tu);

  std::vector<ElfFunctionIR> ReadElfFunctions (
      const abi_dump::TranslationUnit &tu);

  std::vector<ElfObjectIR> ReadElfObjects (const abi_dump::TranslationUnit &tu);

  void ReadTypeInfo(const abi_dump::BasicNamedAndTypedDecl &type_info,
                    TypeIR *typep);

  FunctionIR FunctionProtobufToIR(const abi_dump::FunctionDecl &);

  RecordTypeIR RecordTypeProtobufToIR(
       const abi_dump::RecordType &record_type_protobuf);

  std::vector<RecordFieldIR> RecordFieldsProtobufToIR(
    const google::protobuf::RepeatedPtrField<abi_dump::RecordFieldDecl> &rfp);

  std::vector<CXXBaseSpecifierIR> RecordCXXBaseSpecifiersProtobufToIR(
    const google::protobuf::RepeatedPtrField<abi_dump::CXXBaseSpecifier> &rbs);

  std::vector<EnumFieldIR> EnumFieldsProtobufToIR(
       const google::protobuf::RepeatedPtrField<abi_dump::EnumFieldDecl> &efp);

  EnumTypeIR EnumTypeProtobufToIR(
       const abi_dump::EnumType &enum_type_protobuf);

  VTableLayoutIR VTableLayoutProtobufToIR(
    const abi_dump::VTableLayout &vtable_layout_protobuf);

  TemplateInfoIR TemplateInfoProtobufToIR(
       const abi_dump::TemplateInfo &template_info_protobuf);
};

class ProtobufIRDiffDumper : public IRDiffDumper {
 public:
  ProtobufIRDiffDumper(const std::string &dump_path)
      : IRDiffDumper(dump_path),
        diff_tu_(new abi_diff::TranslationUnitDiff()) { }

  bool AddDiffMessageIR(const DiffMessageIR *, const std::string &type_stack,
                        DiffKind diff_kind) override;

  bool AddLinkableMessageIR(const LinkableMessageIR *,
                            DiffKind diff_kind) override;

  bool AddElfSymbolMessageIR(const ElfSymbolIR *, DiffKind diff_kind) override;

  void AddLibNameIR(const std::string &name) override;

  void AddArchIR(const std::string &arch) override;

  void AddCompatibilityStatusIR(CompatibilityStatusIR status) override;

  bool Dump() override;

   CompatibilityStatusIR GetCompatibilityStatusIR() override;

   ~ProtobufIRDiffDumper() override { }

 private:
  // User defined types.
  bool AddRecordTypeDiffIR(const RecordTypeDiffIR *,
                           const std::string &type_stack, DiffKind diff_kind);

  bool AddEnumTypeDiffIR(const EnumTypeDiffIR *,
                         const std::string &type_stack, DiffKind diff_kind);

  // Functions and global variables.
  bool AddFunctionDiffIR(const FunctionDiffIR *,
                         const std::string &type_stack, DiffKind diff_kind);

  bool AddGlobalVarDiffIR(const GlobalVarDiffIR *,
                          const std::string &type_stack, DiffKind diff_kind);

  bool AddLoneRecordTypeDiffIR(const RecordTypeIR *, DiffKind diff_kind);

  bool AddLoneEnumTypeDiffIR(const EnumTypeIR *, DiffKind diff_kind);

  // Functions and global variables.
  bool AddLoneFunctionDiffIR(const FunctionIR *, DiffKind diff_kind);

  bool AddLoneGlobalVarDiffIR(const GlobalVarIR *, DiffKind diff_kind);

  bool AddElfObjectIR(const ElfObjectIR *elf_object_ir, DiffKind diff_kind);

  bool AddElfFunctionIR(const ElfFunctionIR *elf_function_ir,
                        DiffKind diff_kind);

 protected:
  std::unique_ptr<abi_diff::TranslationUnitDiff> diff_tu_;
};

} // abi_util

#endif // IR_PROTOBUF_

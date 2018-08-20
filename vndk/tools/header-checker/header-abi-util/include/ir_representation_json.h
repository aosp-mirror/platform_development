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
#ifndef IR_JSON_
#define IR_JSON_

#include <ir_representation.h>

#include <json/value.h>

// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.
namespace abi_util {

enum AccessSpecifier {
  public_access = 1,
  private_access = 2,
  protected_access = 3,
};

enum RecordKind {
  struct_kind = 1,
  class_kind = 2,
  union_kind = 3,
};

enum VTableComponentKind {
  VCallOffset = 0,
  VBaseOffset = 1,
  OffsetToTop = 2,
  RTTI = 3,
  FunctionPointer = 4,
  CompleteDtorPointer = 5,
  DeletingDtorPointer = 6,
  UnusedFunctionPointer = 7,
};

inline AccessSpecifier AccessIRToJson(AccessSpecifierIR access) {
  switch (access) {
  case AccessSpecifierIR::PublicAccess:
    return AccessSpecifier::public_access;

  case AccessSpecifierIR::ProtectedAccess:
    return AccessSpecifier::protected_access;

  case AccessSpecifierIR::PrivateAccess:
    return AccessSpecifier::private_access;

  default:
    return AccessSpecifier::public_access;
  }
  // Should not be reached
  assert(false);
}

inline AccessSpecifierIR AccessJsonToIR(AccessSpecifier access) {
  switch (access) {
  case AccessSpecifier::public_access:
    return AccessSpecifierIR::PublicAccess;

  case AccessSpecifier::protected_access:
    return AccessSpecifierIR::ProtectedAccess;

  case AccessSpecifier::private_access:
    return AccessSpecifierIR::PrivateAccess;

  default:
    return AccessSpecifierIR::PublicAccess;
  }
  // Should not be reached
  assert(false);
}

inline RecordKind RecordKindIRToJson(RecordTypeIR::RecordKind kind) {
  switch (kind) {
  case RecordTypeIR::RecordKind::struct_kind:
    return RecordKind::struct_kind;

  case RecordTypeIR::RecordKind::class_kind:
    return RecordKind::class_kind;

  case RecordTypeIR::RecordKind::union_kind:
    return RecordKind::union_kind;

  default:
    return RecordKind::struct_kind;
  }
  // Should not be reached
  assert(false);
}

inline RecordTypeIR::RecordKind RecordKindJsonToIR(RecordKind kind) {
  switch (kind) {
  case RecordKind::struct_kind:
    return RecordTypeIR::struct_kind;

  case RecordKind::class_kind:
    return RecordTypeIR::class_kind;

  case RecordKind::union_kind:
    return RecordTypeIR::union_kind;

  default:
    return RecordTypeIR::struct_kind;
  }
  // Should not be reached
  assert(false);
}

inline VTableComponentKind
VTableComponentKindIRToJson(VTableComponentIR::Kind kind) {
  switch (kind) {
  case VTableComponentIR::Kind::VCallOffset:
    return VTableComponentKind::VCallOffset;

  case VTableComponentIR::Kind::VBaseOffset:
    return VTableComponentKind::VBaseOffset;

  case VTableComponentIR::Kind::OffsetToTop:
    return VTableComponentKind::OffsetToTop;

  case VTableComponentIR::Kind::RTTI:
    return VTableComponentKind::RTTI;

  case VTableComponentIR::Kind::FunctionPointer:
    return VTableComponentKind::FunctionPointer;

  case VTableComponentIR::Kind::CompleteDtorPointer:
    return VTableComponentKind::CompleteDtorPointer;

  case VTableComponentIR::Kind::DeletingDtorPointer:
    return VTableComponentKind::DeletingDtorPointer;

  case VTableComponentIR::Kind::UnusedFunctionPointer:
    return VTableComponentKind::UnusedFunctionPointer;

  default:
    return VTableComponentKind::UnusedFunctionPointer;
  }
  // Should not be reached
  assert(false);
}

inline VTableComponentIR::Kind
VTableComponentKindJsonToIR(VTableComponentKind kind) {
  switch (kind) {
  case VTableComponentKind::VCallOffset:
    return VTableComponentIR::Kind::VCallOffset;

  case VTableComponentKind::VBaseOffset:
    return VTableComponentIR::Kind::VBaseOffset;

  case VTableComponentKind::OffsetToTop:
    return VTableComponentIR::Kind::OffsetToTop;

  case VTableComponentKind::RTTI:
    return VTableComponentIR::Kind::RTTI;

  case VTableComponentKind::FunctionPointer:
    return VTableComponentIR::Kind::FunctionPointer;

  case VTableComponentKind::CompleteDtorPointer:
    return VTableComponentIR::Kind::CompleteDtorPointer;

  case VTableComponentKind::DeletingDtorPointer:
    return VTableComponentIR::Kind::DeletingDtorPointer;

  case VTableComponentKind::UnusedFunctionPointer:
    return VTableComponentIR::Kind::UnusedFunctionPointer;

  default:
    return VTableComponentIR::Kind::UnusedFunctionPointer;
  }
  // Should not be reached
  assert(false);
}

// Classes that wrap constructor of Json::Value.
class JsonArray : public Json::Value {
 public:
  JsonArray() : Json::Value(Json::ValueType::arrayValue) {}
};

class JsonObject : public Json::Value {
 public:
  JsonObject() : Json::Value(Json::ValueType::objectValue) {}
};

class IRToJsonConverter {
 private:
  static void AddTemplateInfo(JsonObject &type_decl,
                              const abi_util::TemplatedArtifactIR *template_ir);

  // BasicNamedAndTypedDecl
  static void AddTypeInfo(JsonObject &type_decl, const TypeIR *type_ir);

  static void AddRecordFields(JsonObject &record_type,
                              const RecordTypeIR *record_ir);

  static void AddBaseSpecifiers(JsonObject &record_type,
                                const RecordTypeIR *record_ir);

  static void AddVTableLayout(JsonObject &record_type,
                              const RecordTypeIR *record_ir);

  static void AddTagTypeInfo(JsonObject &tag_type,
                             const TagTypeIR *tag_type_ir);

  static void AddEnumFields(JsonObject &enum_type, const EnumTypeIR *enum_ir);

 public:
  static JsonObject ConvertEnumTypeIR(const EnumTypeIR *enump);

  static JsonObject ConvertRecordTypeIR(const RecordTypeIR *recordp);

  static JsonObject ConvertFunctionTypeIR(const FunctionTypeIR *function_typep);

  static void AddFunctionParametersAndSetReturnType(
      JsonObject &function, const CFunctionLikeIR *cfunction_like_ir);

  static void AddFunctionParameters(JsonObject &function,
                                    const CFunctionLikeIR *cfunction_like_ir);

  static JsonObject ConvertFunctionIR(const FunctionIR *functionp);

  static JsonObject ConvertGlobalVarIR(const GlobalVarIR *global_varp);

  static JsonObject ConvertPointerTypeIR(const PointerTypeIR *pointerp);

  static JsonObject ConvertQualifiedTypeIR(const QualifiedTypeIR *qualtypep);

  static JsonObject ConvertBuiltinTypeIR(const BuiltinTypeIR *builtin_typep);

  static JsonObject ConvertArrayTypeIR(const ArrayTypeIR *array_typep);

  static JsonObject ConvertLvalueReferenceTypeIR(
      const LvalueReferenceTypeIR *lvalue_reference_typep);

  static JsonObject ConvertRvalueReferenceTypeIR(
      const RvalueReferenceTypeIR *rvalue_reference_typep);

  static JsonObject ConvertElfFunctionIR(const ElfFunctionIR *elf_function_ir);

  static JsonObject ConvertElfObjectIR(const ElfObjectIR *elf_object_ir);
};

class JsonIRDumper : public IRDumper, public IRToJsonConverter {
 public:
  JsonIRDumper(const std::string &dump_path);

  bool AddLinkableMessageIR(const LinkableMessageIR *) override;

  bool AddElfSymbolMessageIR(const ElfSymbolIR *) override;

  bool Dump() override;

  ~JsonIRDumper() override {}

 private:
  JsonObject translation_unit_;
};

} // namespace abi_util

#endif // IR_JSON_

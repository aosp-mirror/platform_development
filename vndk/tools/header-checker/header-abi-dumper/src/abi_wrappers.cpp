// Copyright (C) 2016 The Android Open Source Project
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

#include "abi_wrappers.h"

#include <header_abi_util.h>

#include <limits.h>
#include <stdlib.h>
#include <clang/Tooling/Core/QualTypeNames.h>
#include <clang/Index/CodegenNameGenerator.h>

#include <string>

using namespace abi_wrapper;

ABIWrapper::ABIWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *cip,
    std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper,
    std::map<const clang::Decl *, std::string> &decl_to_source_cache)
  : cip_(cip),
    mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp),
    type_cache_(type_cache),
    ir_dumper_(ir_dumper),
    decl_to_source_file_cache_(decl_to_source_cache) { }

std::string ABIWrapper::GetCachedDeclSourceFile(
    const clang::Decl *decl, const clang::CompilerInstance *cip) {
  assert(decl != nullptr);
  auto result = decl_to_source_file_cache_.find(decl);
  if (result == decl_to_source_file_cache_.end()) {
    return GetDeclSourceFile(decl, cip);
  }
  return result->second;
}

std::string ABIWrapper::GetDeclSourceFile(const clang::Decl *decl,
                                          const clang::CompilerInstance *cip) {
  clang::SourceManager &sm = cip->getSourceManager();
  clang::SourceLocation location = decl->getLocation();
  // We need to use the expansion location to identify whether we should recurse
  // into the AST Node or not. For eg: macros specifying LinkageSpecDecl can
  // have their spelling location defined somewhere outside a source / header
  // file belonging to a library. This should not allow the AST node to be
  // skipped. Its expansion location will still be the source-file / header
  // belonging to the library.
  clang::SourceLocation expansion_location = sm.getExpansionLoc(location);
  llvm::StringRef file_name = sm.getFilename(expansion_location);
  return abi_util::RealPath(file_name.str());
}

static abi_util::AccessSpecifierIR AccessClangToIR(
    const clang::AccessSpecifier sp)  {
  switch (sp) {
    case clang::AS_private: {
      return abi_util::AccessSpecifierIR::PrivateAccess;
      break;
    }
    case clang::AS_protected: {
      return abi_util::AccessSpecifierIR::ProtectedAccess;
      break;
    }
    default: {
      return abi_util::AccessSpecifierIR::PublicAccess;
      break;
    }
  }
}

// Get type 'referenced' by qual_type. Referenced type implies, in order:
// 1) Strip off all qualifiers if qual_type has CVR qualifiers.
// 2) Strip off a pointer level if qual_type is a pointer.
// 3) Strip off the reference if qual_type is a reference.
// Note: qual_type is expected to be a canonical type.
clang::QualType ABIWrapper::GetReferencedType(const clang::QualType qual_type) {
  const clang::Type *type_ptr = qual_type.getTypePtr();
  if (qual_type.hasLocalQualifiers()) {
    return qual_type.getLocalUnqualifiedType();
  }
  if (type_ptr->isPointerType()) {
    return type_ptr->getPointeeType();
  }
  if (type_ptr->isArrayType()) {
    return
        type_ptr->getArrayElementTypeNoTypeQual()->getCanonicalTypeInternal();
  }
  return qual_type.getNonReferenceType();
}

bool ABIWrapper::CreateExtendedType(
    clang::QualType qual_type,
    abi_util::TypeIR *typep) {
  std::string type_name = QualTypeToString(qual_type);
  if (!type_cache_->insert(type_name).second) {
    return true;
  }
  const clang::QualType canonical_type = qual_type.getCanonicalType();
  return CreateBasicNamedAndTypedDecl(canonical_type, typep, "");
}

//This overload takes in a qualtype and adds its information to the abi-dump on
//its own.
bool ABIWrapper::CreateBasicNamedAndTypedDecl(clang::QualType qual_type,
                                              const std::string &source_file) {
  std::string type_name = QualTypeToString(qual_type);
  const clang::QualType canonical_type = qual_type.getCanonicalType();
  const clang::Type *base_type = canonical_type.getTypePtr();
  bool is_ptr = base_type->isPointerType();
  bool is_reference = base_type->isReferenceType();
  bool is_array = base_type->isArrayType();
  bool is_builtin = base_type->isBuiltinType();
  bool has_referenced_type =
      is_ptr || is_reference || is_array ||
      canonical_type.hasLocalQualifiers() || is_builtin;
  if (!has_referenced_type || !type_cache_->insert(type_name).second) {
    return true;
  }
  // Do something similar to what is being done right now. Create an object
  // extending Type and return a pointer to that and pass it to CreateBasic...
  // CreateBasic...(qualtype, Type *) fills in size, alignemnt etc.
  std::unique_ptr<abi_util::TypeIR> typep = SetTypeKind(canonical_type,
                                                        source_file);
  if (!base_type->isVoidType() && !typep) {
    return false;
  }
  return CreateBasicNamedAndTypedDecl(canonical_type, typep.get(),
                                      source_file) &&
      ir_dumper_->AddLinkableMessageIR(typep.get());
}

// CreateBasicNamedAndTypedDecl creates a BasicNamedAndTypedDecl : that'll
// include all the generic information a basic type will have:
// abi_dump::BasicNamedAndTypedDecl. Other methods fill in more specific
// information, eg: RecordDecl, EnumDecl.
bool ABIWrapper::CreateBasicNamedAndTypedDecl(
    clang::QualType canonical_type,
    abi_util::TypeIR *typep, const std::string &source_file) {
  // Cannot determine the size and alignment for template parameter dependent
  // types as well as incomplete types.
  const clang::Type *base_type = canonical_type.getTypePtr();
  assert(base_type != nullptr);
  clang::Type::TypeClass type_class = base_type->getTypeClass();
  // Temporary Hack for auto type sizes. Not determinable.
  if ((type_class != clang::Type::Auto) && !base_type->isIncompleteType() &&
      !(base_type->isDependentType())) {
    std::pair<clang::CharUnits, clang::CharUnits> size_and_alignment =
    ast_contextp_->getTypeInfoInChars(canonical_type);
    size_t size = size_and_alignment.first.getQuantity();
    size_t alignment = size_and_alignment.second.getQuantity();
    typep->SetSize(size);
    typep->SetAlignment(alignment);
  }
  typep->SetName(QualTypeToString(canonical_type));
  typep->SetLinkerSetKey(QualTypeToString(canonical_type));
  // default values are false, we don't set them since explicitly doing that
  // makes the abi dumps more verbose.
  // This type has a reference type if its a pointer / reference OR it has CVR
  // qualifiers.
  clang::QualType referenced_type = GetReferencedType(canonical_type);
  typep->SetReferencedType(QualTypeToString(referenced_type));
  // Create the type for referenced type.
  return CreateBasicNamedAndTypedDecl(referenced_type, source_file);
}

std::string ABIWrapper::GetTypeLinkageName(const clang::Type *typep)  {
  assert(typep != nullptr);
  clang::QualType qt = typep->getCanonicalTypeInternal();
  return QualTypeToString(qt);
}

std::unique_ptr<abi_util::TypeIR> ABIWrapper::SetTypeKind(
    const clang::QualType canonical_type, const std::string &source_file) {
  if (canonical_type.hasLocalQualifiers()) {
    auto qual_type_ir =
        std::make_unique<abi_util::QualifiedTypeIR>();
    qual_type_ir->SetConstness(canonical_type.isConstQualified());
    qual_type_ir->SetRestrictedness(canonical_type.isRestrictQualified());
    qual_type_ir->SetVolatility(canonical_type.isVolatileQualified());
    qual_type_ir->SetSourceFile(source_file);
    return qual_type_ir;
  }
  const clang::Type *type_ptr = canonical_type.getTypePtr();
  if (type_ptr->isPointerType()) {
    auto pointer_type_ir = std::make_unique<abi_util::PointerTypeIR>();
    pointer_type_ir->SetSourceFile(source_file);
    return pointer_type_ir;
  }
  if (type_ptr->isLValueReferenceType()) {
    auto lvalue_reference_type_ir =
        std::make_unique<abi_util::LvalueReferenceTypeIR>();
    lvalue_reference_type_ir->SetSourceFile(source_file);
    return lvalue_reference_type_ir;
  }
  if (type_ptr->isRValueReferenceType()) {
   auto rvalue_reference_type_ir =
       std::make_unique<abi_util::RvalueReferenceTypeIR>();
   rvalue_reference_type_ir->SetSourceFile(source_file);
   return rvalue_reference_type_ir;
  }
  if (type_ptr->isArrayType()) {
    auto array_type_ir = std::make_unique<abi_util::ArrayTypeIR>();
    array_type_ir->SetSourceFile(source_file);
    return array_type_ir;
  }
  if (type_ptr->isEnumeralType()) {
    return std::make_unique<abi_util::EnumTypeIR>();
  }
  if (type_ptr->isRecordType()) {
    return std::make_unique<abi_util::RecordTypeIR>();
  }
  if (type_ptr->isBuiltinType()) {
    auto builtin_type_ir = std::make_unique<abi_util::BuiltinTypeIR>();
    builtin_type_ir->SetSignedness(type_ptr->isUnsignedIntegerType());
    builtin_type_ir->SetIntegralType(type_ptr->isIntegralType(*ast_contextp_));
    return builtin_type_ir;
  }
  return nullptr;
}

std::string ABIWrapper::GetMangledNameDecl(
    const clang::NamedDecl *decl, clang::MangleContext *mangle_contextp) {
  if (!mangle_contextp->shouldMangleDeclName(decl)) {
    clang::IdentifierInfo *identifier = decl->getIdentifier();
    return identifier ? identifier->getName() : "";
  }
  std::string mangled_name;
  llvm::raw_string_ostream ostream(mangled_name);
  mangle_contextp->mangleName(decl, ostream);
  ostream.flush();
  return mangled_name;
}

std::string ABIWrapper::GetTagDeclQualifiedName(
    const clang::TagDecl *decl) {
  if (decl->getTypedefNameForAnonDecl()) {
    return decl->getTypedefNameForAnonDecl()->getQualifiedNameAsString();
  }
  return decl->getQualifiedNameAsString();
}

bool ABIWrapper::SetupTemplateArguments(
    const clang::TemplateArgumentList *tl,
    abi_util::TemplatedArtifactIR *ta,
    const std::string &source_file) {
  abi_util::TemplateInfoIR template_info;
  for (int i = 0; i < tl->size(); i++) {
    const clang::TemplateArgument &arg = (*tl)[i];
    //TODO: More comprehensive checking needed.
    if (arg.getKind() != clang::TemplateArgument::Type) {
      continue;
    }
    clang::QualType type = arg.getAsType();
    template_info.AddTemplateElement(
        abi_util::TemplateElementIR(QualTypeToString(type)));
    if (!CreateBasicNamedAndTypedDecl(type, source_file)) {
      return false;
    }
  }
  ta->SetTemplateInfo(std::move(template_info));
  return true;
}

static const clang::EnumDecl *GetAnonymousEnum(
    const clang::QualType qual_type) {
  const clang::Type *type_ptr = qual_type.getTypePtr();
  assert(type_ptr != nullptr);
  const clang::TagDecl *tag_decl = type_ptr->getAsTagDecl();
  if (!tag_decl) {
    return nullptr;
  }
  const clang::EnumDecl *enum_decl = llvm::dyn_cast<clang::EnumDecl>(tag_decl);
  if (!enum_decl || enum_decl->hasNameForLinkage()) {
    return nullptr;
  }
  return enum_decl;
}

std::string ABIWrapper::QualTypeToString(
    const clang::QualType &sweet_qt) {
  const clang::QualType salty_qt = sweet_qt.getCanonicalType();
  // clang::TypeName::getFullyQualifiedName removes the part of the type related
  // to it being a template parameter. Don't use it for dependent types.
  if (salty_qt.getTypePtr()->isDependentType()) {
    return salty_qt.getAsString();
  }
  return clang::TypeName::getFullyQualifiedName(salty_qt, *ast_contextp_);
}

FunctionDeclWrapper::FunctionDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::FunctionDecl *decl,
    std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper,
    std::map<const clang::Decl *, std::string> &decl_to_source_cache)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p,
               type_cache, ir_dumper, decl_to_source_cache),
    function_decl_(decl) { }

bool FunctionDeclWrapper::SetupThisParameter(abi_util::FunctionIR *functionp,
                                             const std::string &source_file) {
  const clang::CXXMethodDecl *cxx_method_decl =
      llvm::dyn_cast<clang::CXXMethodDecl>(function_decl_);
  // No this pointer for static methods.
  if (!cxx_method_decl || cxx_method_decl->isStatic()) {
    return true;
  }
  clang::QualType this_type = cxx_method_decl->getThisType(*ast_contextp_);
  return SetupFunctionParameter(functionp, this_type, false, source_file);
}

bool FunctionDeclWrapper::SetupFunctionParameter(
    abi_util::FunctionIR *functionp, const clang::QualType qual_type,
    bool has_default_arg, const std::string &source_file) {
  if (!CreateBasicNamedAndTypedDecl(qual_type, source_file)) {
    return false;
  }
  functionp->AddParameter(abi_util::ParamIR(QualTypeToString(qual_type),
                                            has_default_arg));
  return true;
}

bool FunctionDeclWrapper::SetupFunctionParameters(
    abi_util::FunctionIR *functionp,
    const std::string &source_file) {
  clang::FunctionDecl::param_const_iterator param_it =
      function_decl_->param_begin();
  // If this is a CXXMethodDecl, we need to add the "this" pointer.
  if (!SetupThisParameter(functionp, source_file)) {
    return false;
  }

  while (param_it != function_decl_->param_end()) {
    // The linker set key is blank since that shows up in the mangled name.
    bool has_default_arg = (*param_it)->hasDefaultArg();
    clang::QualType param_qt = (*param_it)->getType();
    if (!SetupFunctionParameter(functionp, param_qt, has_default_arg,
                                source_file)) {
      return false;
    }
    param_it++;
  }
  return true;
}

bool FunctionDeclWrapper::SetupFunction(abi_util::FunctionIR *functionp,
                                        const std::string &source_file)  {
  // Go through all the parameters in the method and add them to the fields.
  // Also get the fully qualfied name.
  functionp->SetSourceFile(source_file);
  functionp->SetName(function_decl_->getQualifiedNameAsString());
  clang::QualType return_type = function_decl_->getReturnType();

  functionp->SetReturnType(QualTypeToString(return_type));
  functionp->SetAccess(AccessClangToIR(function_decl_->getAccess()));
  return CreateBasicNamedAndTypedDecl(return_type, source_file) &&
      SetupFunctionParameters(functionp, source_file) &&
      SetupTemplateInfo(functionp, source_file);
}

bool FunctionDeclWrapper::SetupTemplateInfo(
    abi_util::FunctionIR *functionp,
    const std::string &source_file)  {
  switch (function_decl_->getTemplatedKind()) {
    case clang::FunctionDecl::TK_FunctionTemplateSpecialization: {
      const clang::TemplateArgumentList *arg_list =
          function_decl_->getTemplateSpecializationArgs();
      if (arg_list && !SetupTemplateArguments(arg_list, functionp,
                                              source_file)) {
        return false;
      }
      break;
    }
    default: {
      break;
    }
  }
  return true;
}

std::unique_ptr<abi_util::FunctionIR> FunctionDeclWrapper::GetFunctionDecl() {
  auto abi_decl = std::make_unique<abi_util::FunctionIR>();
  std::string source_file = GetCachedDeclSourceFile(function_decl_, cip_);
  if (!SetupFunction(abi_decl.get(), source_file)) {
    return nullptr;
  }
  return abi_decl;
}

RecordDeclWrapper::RecordDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::RecordDecl *decl,
    std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper,
    std::map<const clang::Decl *, std::string> &decl_to_source_cache,
    const std::string &previous_record_stages)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p,
               type_cache, ir_dumper, decl_to_source_cache),
    record_decl_(decl), previous_record_stages_(previous_record_stages) { }

bool RecordDeclWrapper::CreateAnonymousRecord(
    const clang::RecordDecl *record_decl, const std::string &linker_set_key) {
  RecordDeclWrapper record_decl_wrapper(mangle_contextp_, ast_contextp_, cip_,
                                        record_decl, type_cache_, ir_dumper_,
                                        decl_to_source_file_cache_,
                                        linker_set_key);
  return record_decl_wrapper.GetRecordDecl();
}

static const clang::RecordDecl *GetAnonymousRecord(clang::QualType type) {
  const clang::Type *type_ptr = type.getTypePtr();
  assert(type_ptr != nullptr);
  if (!type_ptr->isRecordType()) {
    return nullptr;
  }
  const clang::TagDecl *tag_decl = type_ptr->getAsTagDecl();
  if (!tag_decl) {
    return nullptr;
  }
  const clang::RecordDecl *record_decl =
      llvm::dyn_cast<clang::RecordDecl>(tag_decl);

  if (record_decl != nullptr && (!record_decl->hasNameForLinkage() ||
      record_decl->isAnonymousStructOrUnion())) {
    return record_decl;
  }
  return nullptr;
}

bool RecordDeclWrapper::SetupRecordFields(abi_util::RecordTypeIR *recordp,
                                          const std::string &source_file) {
  clang::RecordDecl::field_iterator field = record_decl_->field_begin();
  uint32_t field_index = 0;
  const clang::ASTRecordLayout &record_layout =
      ast_contextp_->getASTRecordLayout(record_decl_);
  while (field != record_decl_->field_end()) {
    clang::QualType field_type = field->getType();
    if (!CreateBasicNamedAndTypedDecl(field_type, source_file)) {
      llvm::errs() << "Creation of Type failed\n";
      return false;
    }
    std::string field_type_str = QualTypeToString(field_type);
    std::string field_name = field->getName();
    // Handle anoymous structs / unions as fields.
    if (const clang::RecordDecl *anon_record_decl =
        GetAnonymousRecord(field_type)) {
      // We need to create a unique linker set key for anonymous structs / enums
      // since clang just names them with fully_qualified_scope::anonymous.
      field_type_str =
          previous_record_stages_ + "::(anonymous)" +
          std::to_string(field_index);
      if (!CreateAnonymousRecord(anon_record_decl, field_type_str)) {
      return false;
      }
    } else if (const clang::EnumDecl *enum_decl =
               GetAnonymousEnum(field_type)) {
      // Handle anonymous enums.
      field_type_str = QualTypeToString(enum_decl->getIntegerType());
    }
    uint64_t field_offset = record_layout.getFieldOffset(field_index);
    recordp->AddRecordField(abi_util::RecordFieldIR(
        field_name, field_type_str, field_offset,
        AccessClangToIR(field->getAccess())));
    field++;
    field_index++;
  }
  return true;
}

bool RecordDeclWrapper::SetupCXXBases(
    abi_util::RecordTypeIR *cxxp,
    const clang::CXXRecordDecl *cxx_record_decl) {
  if (!cxx_record_decl || !cxxp) {
    return false;
  }
  clang::CXXRecordDecl::base_class_const_iterator base_class =
      cxx_record_decl->bases_begin();
  while (base_class != cxx_record_decl->bases_end()) {
    std::string name = QualTypeToString(base_class->getType());
    bool is_virtual = base_class->isVirtual();
    abi_util::AccessSpecifierIR access =
        AccessClangToIR(base_class->getAccessSpecifier());
    cxxp->AddCXXBaseSpecifier(abi_util::CXXBaseSpecifierIR(name, is_virtual,
                                                           access));
    base_class++;
  }
  return true;
}

bool RecordDeclWrapper::SetupRecordVTable(
    abi_util::RecordTypeIR *record_declp,
    const clang::CXXRecordDecl *cxx_record_decl) {
  if (!cxx_record_decl || !record_declp) {
    return false;
  }
  clang::VTableContextBase *base_vtable_contextp =
      ast_contextp_->getVTableContext();
  const clang::Type *typep = cxx_record_decl->getTypeForDecl();
  if (!base_vtable_contextp || !typep) {
    return false;
  }
  // Skip Microsoft ABI.
  clang::ItaniumVTableContext *itanium_vtable_contextp =
        llvm::dyn_cast<clang::ItaniumVTableContext>(base_vtable_contextp);
  if (!itanium_vtable_contextp || !cxx_record_decl->isPolymorphic() ||
      typep->isDependentType() || typep->isIncompleteType()) {
    return true;
  }
  const clang::VTableLayout &vtable_layout =
      itanium_vtable_contextp->getVTableLayout(cxx_record_decl);
  abi_util::VTableLayoutIR vtable_ir_layout;
  for (const auto &vtable_component : vtable_layout.vtable_components()) {
    abi_util::VTableComponentIR added_component=
        SetupRecordVTableComponent(vtable_component);
    vtable_ir_layout.AddVTableComponent(std::move(added_component));
  }
  record_declp->SetVTableLayout(std::move(vtable_ir_layout));
  return true;
}

abi_util::VTableComponentIR RecordDeclWrapper::SetupRecordVTableComponent(
    const clang::VTableComponent &vtable_component) {
  abi_util::VTableComponentIR::Kind kind =
      abi_util::VTableComponentIR::Kind::RTTI;
  std::string mangled_component_name = "";
  llvm::raw_string_ostream ostream(mangled_component_name);
  int64_t value = 0;
  clang::VTableComponent::Kind clang_component_kind =
      vtable_component.getKind();
  switch (clang_component_kind) {
    case clang::VTableComponent::CK_VCallOffset:
      kind = abi_util::VTableComponentIR::Kind::VCallOffset;
      value = vtable_component.getVCallOffset().getQuantity();
      break;
    case clang::VTableComponent::CK_VBaseOffset:
      kind = abi_util::VTableComponentIR::Kind::VBaseOffset;
      value = vtable_component.getVBaseOffset().getQuantity();
      break;
    case clang::VTableComponent::CK_OffsetToTop:
      kind = abi_util::VTableComponentIR::Kind::OffsetToTop;
      value = vtable_component.getOffsetToTop().getQuantity();
      break;
    case clang::VTableComponent::CK_RTTI:
      {
        kind = abi_util::VTableComponentIR::Kind::RTTI;
        const clang::CXXRecordDecl *rtti_decl =
            vtable_component.getRTTIDecl();
        assert(rtti_decl != nullptr);
        mangled_component_name =
            ABIWrapper::GetTypeLinkageName(rtti_decl->getTypeForDecl());
      }
      break;
    case clang::VTableComponent::CK_FunctionPointer:
    case clang::VTableComponent::CK_CompleteDtorPointer:
    case clang::VTableComponent::CK_DeletingDtorPointer:
    case clang::VTableComponent::CK_UnusedFunctionPointer:
      {
        const clang::CXXMethodDecl *method_decl =
            vtable_component.getFunctionDecl();
        assert(method_decl != nullptr);
        switch (clang_component_kind) {
          case clang::VTableComponent::CK_FunctionPointer:
            kind = abi_util::VTableComponentIR::Kind::FunctionPointer;
            mangled_component_name = GetMangledNameDecl(method_decl,
                                                        mangle_contextp_);
              break;
          case clang::VTableComponent::CK_CompleteDtorPointer:
            kind = abi_util::VTableComponentIR::Kind::CompleteDtorPointer;
            mangle_contextp_->mangleCXXDtor(
                vtable_component.getDestructorDecl(),
                clang::CXXDtorType::Dtor_Complete, ostream);
            ostream.flush();
            break;
          case clang::VTableComponent::CK_DeletingDtorPointer:
            kind = abi_util::VTableComponentIR::Kind::DeletingDtorPointer;
            mangle_contextp_->mangleCXXDtor(
                vtable_component.getDestructorDecl(),
                clang::CXXDtorType::Dtor_Deleting, ostream);
            ostream.flush();
            break;
          case clang::VTableComponent::CK_UnusedFunctionPointer:
            kind = abi_util::VTableComponentIR::Kind::UnusedFunctionPointer;
            break;
          default:
            break;
        }
      }
      break;
    default:
      break;
  }
  return abi_util::VTableComponentIR(mangled_component_name, kind, value);
}

bool RecordDeclWrapper::SetupTemplateInfo(
    abi_util::RecordTypeIR *record_declp,
    const clang::CXXRecordDecl *cxx_record_decl,
    const std::string &source_file) {
  assert(cxx_record_decl != nullptr);
  const clang::ClassTemplateSpecializationDecl *specialization_decl =
      clang::dyn_cast<clang::ClassTemplateSpecializationDecl>(cxx_record_decl);
  if (specialization_decl) {
    const clang::TemplateArgumentList *arg_list =
        &specialization_decl->getTemplateArgs();
    if (arg_list &&
        !SetupTemplateArguments(arg_list, record_declp, source_file)) {
      return false;
    }
  }
  return true;
}

bool RecordDeclWrapper::SetupRecordInfo(abi_util::RecordTypeIR *record_declp,
                                        const std::string &source_file) {
  if (!record_declp) {
    return false;
  }

  if (record_decl_->isStruct()) {
    record_declp->SetRecordKind(
        abi_util::RecordTypeIR::RecordKind::struct_kind);
  } else if (record_decl_->isClass()) {
    record_declp->SetRecordKind(
        abi_util::RecordTypeIR::RecordKind::class_kind);
  } else {
    record_declp->SetRecordKind(
        abi_util::RecordTypeIR::RecordKind::union_kind);
  }

  const clang::Type *basic_type = nullptr;
  if (!(basic_type = record_decl_->getTypeForDecl())) {
    return false;
  }
  clang::QualType qual_type = basic_type->getCanonicalTypeInternal();
  if (!CreateExtendedType(qual_type, record_declp)) {
    return false;
  }
  std::string record_qual_type_str = QualTypeToString(qual_type);
  record_declp->SetSourceFile(source_file);
  if (!record_decl_->hasNameForLinkage() ||
      record_decl_->isAnonymousStructOrUnion()) {
    record_declp->SetLinkerSetKey(previous_record_stages_);
    record_declp->SetAnonymity(true);
  } else {
    previous_record_stages_ = record_qual_type_str;
    record_declp->SetLinkerSetKey(record_qual_type_str);
  }
  record_declp->SetAccess(AccessClangToIR(record_decl_->getAccess()));
  return SetupRecordFields(record_declp, source_file) &&
      SetupCXXRecordInfo(record_declp, source_file);
}

bool RecordDeclWrapper::SetupCXXRecordInfo(
    abi_util::RecordTypeIR *record_declp, const std::string &source_file) {
  const clang::CXXRecordDecl *cxx_record_decl =
      clang::dyn_cast<clang::CXXRecordDecl>(record_decl_);
  if (!cxx_record_decl) {
    return true;
  }
  return SetupTemplateInfo(record_declp, cxx_record_decl, source_file) &&
      SetupCXXBases(record_declp, cxx_record_decl) &&
      SetupRecordVTable(record_declp, cxx_record_decl);
}

bool RecordDeclWrapper::GetRecordDecl() {
  auto abi_decl = std::make_unique<abi_util::RecordTypeIR>();
  std::string source_file = GetCachedDeclSourceFile(record_decl_, cip_);
  if (!SetupRecordInfo(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up CXX Bases / Template Info failed\n";
    return false;
  }
  return ir_dumper_->AddLinkableMessageIR(abi_decl.get());
}

EnumDeclWrapper::EnumDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::EnumDecl *decl,
    std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper,
    std::map<const clang::Decl *, std::string> &decl_to_source_cache)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p,
               type_cache, ir_dumper, decl_to_source_cache),
    enum_decl_(decl) { }

bool EnumDeclWrapper::SetupEnumFields(abi_util::EnumTypeIR *enump) {
  if (!enump) {
    return false;
  }
  clang::EnumDecl::enumerator_iterator enum_it = enum_decl_->enumerator_begin();
  while (enum_it != enum_decl_->enumerator_end()) {
    std::string name = enum_it->getQualifiedNameAsString();
    uint64_t field_value = enum_it->getInitVal().getExtValue();
    enump->AddEnumField(abi_util::EnumFieldIR(name, field_value));
    enum_it++;
  }
  return true;
}

bool EnumDeclWrapper::SetupEnum(abi_util::EnumTypeIR *enum_type,
                                const std::string &source_file) {
  std::string enum_name = GetTagDeclQualifiedName(enum_decl_);
  clang::QualType enum_qual_type =
      enum_decl_->getTypeForDecl()->getCanonicalTypeInternal();
  if (!CreateExtendedType(enum_qual_type, enum_type)) {
    return false;
  }
  enum_type->SetSourceFile(source_file);
  enum_type->SetUnderlyingType(QualTypeToString(enum_decl_->getIntegerType()));
  enum_type->SetAccess(AccessClangToIR(enum_decl_->getAccess()));
  return SetupEnumFields(enum_type) &&
      CreateBasicNamedAndTypedDecl(enum_decl_->getIntegerType(), "");
}

bool EnumDeclWrapper::GetEnumDecl() {
  auto abi_decl = std::make_unique<abi_util::EnumTypeIR>();
  std::string source_file = GetCachedDeclSourceFile(enum_decl_, cip_);

  if (!SetupEnum(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up Enum failed\n";
    return false;
  }
  return ir_dumper_->AddLinkableMessageIR(abi_decl.get());
}

GlobalVarDeclWrapper::GlobalVarDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::VarDecl *decl,std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper,
    std::map<const clang::Decl *, std::string> &decl_to_source_cache)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, type_cache,
               ir_dumper, decl_to_source_cache),
    global_var_decl_(decl) { }

bool GlobalVarDeclWrapper::SetupGlobalVar(
    abi_util::GlobalVarIR *global_varp,
    const std::string &source_file) {
  // Temporary fix : clang segfaults on trying to mangle global variable which
  // is a dependent sized array type.
  std::string mangled_name =
      GetMangledNameDecl(global_var_decl_, mangle_contextp_);
  if (!CreateBasicNamedAndTypedDecl(global_var_decl_->getType(), source_file)) {
    return false;
  }
  global_varp->SetSourceFile(source_file);
  global_varp->SetName(global_var_decl_->getQualifiedNameAsString());
  global_varp->SetLinkerSetKey(mangled_name);
  global_varp->SetReferencedType(
      QualTypeToString(global_var_decl_->getType()));
  return true;
}

bool GlobalVarDeclWrapper::GetGlobalVarDecl() {
  auto abi_decl = std::make_unique<abi_util::GlobalVarIR>();
  std::string source_file = GetCachedDeclSourceFile(global_var_decl_, cip_);
  return SetupGlobalVar(abi_decl.get(), source_file) &&
      ir_dumper_->AddLinkableMessageIR(abi_decl.get());
}

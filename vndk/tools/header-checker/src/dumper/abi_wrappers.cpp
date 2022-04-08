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

#include "dumper/abi_wrappers.h"

#include "repr/ir_reader.h"
#include "utils/header_abi_util.h"

#include <clang/AST/QualTypeNames.h>

#include <regex>
#include <string>

#include <assert.h>
#include <limits.h>
#include <stdlib.h>


namespace header_checker {
namespace dumper {


//------------------------------------------------------------------------------
// Helper Function
//------------------------------------------------------------------------------

static repr::AccessSpecifierIR AccessClangToIR(
    const clang::AccessSpecifier sp) {
  switch (sp) {
    case clang::AS_private: {
      return repr::AccessSpecifierIR::PrivateAccess;
    }
    case clang::AS_protected: {
      return repr::AccessSpecifierIR::ProtectedAccess;
    }
    default: {
      return repr::AccessSpecifierIR::PublicAccess;
    }
  }
}


//------------------------------------------------------------------------------
// ABI Wrapper
//------------------------------------------------------------------------------

ABIWrapper::ABIWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *cip,
    repr::ModuleIR *module,
    ASTCaches *ast_caches)
    : cip_(cip),
      mangle_contextp_(mangle_contextp),
      ast_contextp_(ast_contextp),
      module_(module),
      ast_caches_(ast_caches) {}

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
  return utils::RealPath(file_name.str());
}

std::string ABIWrapper::GetCachedDeclSourceFile(
    const clang::Decl *decl, const clang::CompilerInstance *cip) {
  assert(decl != nullptr);
  auto result = ast_caches_->decl_to_source_file_cache_.find(decl);
  if (result == ast_caches_->decl_to_source_file_cache_.end()) {
    return GetDeclSourceFile(decl, cip);
  }
  return result->second;
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

bool ABIWrapper::SetupTemplateArguments(const clang::TemplateArgumentList *tl,
                                        repr::TemplatedArtifactIR *ta,
                                        const std::string &source_file) {
  repr::TemplateInfoIR template_info;
  for (int i = 0; i < tl->size(); i++) {
    const clang::TemplateArgument &arg = (*tl)[i];
    // TODO: More comprehensive checking needed.
    if (arg.getKind() != clang::TemplateArgument::Type) {
      continue;
    }
    clang::QualType type = arg.getAsType();
    template_info.AddTemplateElement(
        repr::TemplateElementIR(GetTypeUniqueId(type)));
    if (!CreateBasicNamedAndTypedDecl(type, source_file)) {
      llvm::errs() << "Setting up template arguments failed\n";
      return false;
    }
  }
  ta->SetTemplateInfo(std::move(template_info));
  return true;
}

bool ABIWrapper::SetupFunctionParameter(
    repr::CFunctionLikeIR *functionp, const clang::QualType qual_type,
    bool has_default_arg, const std::string &source_file, bool is_this_ptr) {
  if (!CreateBasicNamedAndTypedDecl(qual_type, source_file)) {
    llvm::errs() << "Setting up function parameter failed\n";
    return false;
  }
  functionp->AddParameter(repr::ParamIR(
      GetTypeUniqueId(qual_type), has_default_arg, is_this_ptr));
  return true;
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

  if (record_decl != nullptr &&
      (!record_decl->hasNameForLinkage() ||
       record_decl->isAnonymousStructOrUnion())) {
    return record_decl;
  }
  return nullptr;
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

static bool IsReferencingType(clang::QualType qual_type) {
  const clang::QualType canonical_type = qual_type.getCanonicalType();
  const clang::Type *base_type = canonical_type.getTypePtr();
  bool is_ptr = base_type->isPointerType();
  bool is_reference = base_type->isReferenceType();
  bool is_array = base_type->isArrayType();
  return is_array || is_ptr || is_reference || qual_type.hasLocalQualifiers();
}

// Get type 'referenced' by qual_type. Referenced type implies, in order:
// 1) Strip off all qualifiers if qual_type has CVR qualifiers.
// 2) Strip off a pointer level if qual_type is a pointer.
// 3) Strip off the reference if qual_type is a reference.
// Note: qual_type is expected to be a canonical type.
static clang::QualType GetReferencedType(const clang::QualType qual_type) {
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

bool ABIWrapper::CreateAnonymousRecord(const clang::RecordDecl *record_decl) {
  RecordDeclWrapper record_decl_wrapper(mangle_contextp_, ast_contextp_, cip_,
                                        record_decl, module_, ast_caches_);
  return record_decl_wrapper.GetRecordDecl();
}

bool ABIWrapper::CreateExtendedType(clang::QualType qual_type,
                                    repr::TypeIR *typep) {
  const clang::QualType canonical_type = qual_type.getCanonicalType();
  // The source file is going to be set later anyway.
  return CreateBasicNamedAndTypedDecl(canonical_type, typep, "");
}

// A mangled anonymous enum name ends with $_<number> or Ut<number>_ where the
// number may be inconsistent between translation units. This function replaces
// the name with $ followed by the lexicographically smallest field name.
static std::string GetAnonymousEnumUniqueId(llvm::StringRef mangled_name,
                                            const clang::EnumDecl *enum_decl) {
  // Get the type name from the mangled name.
  const std::string mangled_name_str = mangled_name;
  std::smatch match_result;
  std::string old_suffix;
  std::string nested_name_suffix;
  if (std::regex_search(mangled_name_str, match_result,
                        std::regex(R"((\$_\d+)(E?)$)"))) {
    const std::ssub_match &old_name = match_result[1];
    old_suffix = std::to_string(old_name.length()) + match_result[0].str();
    nested_name_suffix = match_result[2].str();
    if (!mangled_name.endswith(old_suffix)) {
      llvm::errs() << "Unexpected length of anonymous enum type name: "
                   << mangled_name << "\n";
      ::exit(1);
    }
  } else if (std::regex_search(mangled_name_str, match_result,
                               std::regex(R"(Ut\d*_(E?)$)"))) {
    old_suffix = match_result[0].str();
    nested_name_suffix = match_result[1].str();
  } else {
    llvm::errs() << "Cannot parse anonymous enum name: " << mangled_name
                 << "\n";
    ::exit(1);
  }

  // Find the smallest enumerator name.
  std::string smallest_enum_name;
  for (auto enum_it : enum_decl->enumerators()) {
    std::string enum_name = enum_it->getNameAsString();
    if (smallest_enum_name.empty() || smallest_enum_name > enum_name) {
      smallest_enum_name = enum_name;
    }
  }
  smallest_enum_name = "$" + smallest_enum_name;
  std::string new_suffix = std::to_string(smallest_enum_name.length()) +
                           smallest_enum_name + nested_name_suffix;

  return mangled_name.drop_back(old_suffix.length()).str() + new_suffix;
}

std::string ABIWrapper::GetTypeUniqueId(clang::QualType qual_type) {
  const clang::Type *canonical_type = qual_type.getCanonicalType().getTypePtr();
  assert(canonical_type != nullptr);

  llvm::SmallString<256> uid;
  llvm::raw_svector_ostream out(uid);
  mangle_contextp_->mangleCXXRTTI(qual_type, out);

  if (const clang::EnumDecl *enum_decl = GetAnonymousEnum(qual_type)) {
    return GetAnonymousEnumUniqueId(uid.str(), enum_decl);
  }

  return uid.str();
}

// CreateBasicNamedAndTypedDecl creates a BasicNamedAndTypedDecl which will
// include all the generic information of a basic type. Other methods will
// create more specific information, e.g. RecordDecl, EnumDecl.
bool ABIWrapper::CreateBasicNamedAndTypedDecl(
    clang::QualType canonical_type, repr::TypeIR *typep,
    const std::string &source_file) {
  // Cannot determine the size and alignment for template parameter dependent
  // types as well as incomplete types.
  const clang::Type *base_type = canonical_type.getTypePtr();
  assert(base_type != nullptr);
  clang::Type::TypeClass type_class = base_type->getTypeClass();

  // Set the size and alignment of the type.
  // Temporary hack: Skip the auto types, incomplete types and dependent types.
  if (type_class != clang::Type::Auto && !base_type->isIncompleteType() &&
      !base_type->isDependentType()) {
    std::pair<clang::CharUnits, clang::CharUnits> size_and_alignment =
        ast_contextp_->getTypeInfoInChars(canonical_type);
    typep->SetSize(size_and_alignment.first.getQuantity());
    typep->SetAlignment(size_and_alignment.second.getQuantity());
  }

  std::string human_name = QualTypeToString(canonical_type);
  std::string mangled_name = GetTypeUniqueId(canonical_type);
  typep->SetName(human_name);
  typep->SetLinkerSetKey(mangled_name);

  // This type has a reference type if its a pointer / reference OR it has CVR
  // qualifiers.
  clang::QualType referenced_type = GetReferencedType(canonical_type);
  typep->SetReferencedType(GetTypeUniqueId(referenced_type));

  typep->SetSelfType(mangled_name);

  // Create the type for referenced type.
  return CreateBasicNamedAndTypedDecl(referenced_type, source_file);
}

// This overload takes in a qualtype and adds its information to the abi-dump on
// its own.
bool ABIWrapper::CreateBasicNamedAndTypedDecl(clang::QualType qual_type,
                                              const std::string &source_file) {
  const clang::QualType canonical_type = qual_type.getCanonicalType();
  const clang::Type *base_type = canonical_type.getTypePtr();
  bool is_builtin = base_type->isBuiltinType();
  bool should_continue_with_recursive_type_creation =
      IsReferencingType(canonical_type) || is_builtin ||
      base_type->isFunctionType() ||
      (GetAnonymousRecord(canonical_type) != nullptr);
  if (!should_continue_with_recursive_type_creation ||
      !ast_caches_->converted_qual_types_.insert(qual_type).second) {
    return true;
  }

  // Do something similar to what is being done right now. Create an object
  // extending Type and return a pointer to that and pass it to CreateBasic...
  // CreateBasic...(qualtype, Type *) fills in size, alignemnt etc.
  auto type_and_status = SetTypeKind(canonical_type, source_file);
  std::unique_ptr<repr::TypeIR> typep = std::move(type_and_status.typep_);
  if (!base_type->isVoidType() && type_and_status.should_create_type_ &&
      !typep) {
    llvm::errs() << "nullptr with valid type while creating basic type\n";
    return false;
  }

  if (!type_and_status.should_create_type_) {
    return true;
  }

  return (CreateBasicNamedAndTypedDecl(
              canonical_type, typep.get(), source_file) &&
          module_->AddLinkableMessage(*typep));
}

// This method returns a TypeAndCreationStatus object. This object contains a
// type and information to tell the clients of this method whether the caller
// should continue creating the type.
TypeAndCreationStatus ABIWrapper::SetTypeKind(
    const clang::QualType canonical_type, const std::string &source_file) {
  if (canonical_type.hasLocalQualifiers()) {
    auto qual_type_ir =
        std::make_unique<repr::QualifiedTypeIR>();
    qual_type_ir->SetConstness(canonical_type.isConstQualified());
    qual_type_ir->SetRestrictedness(canonical_type.isRestrictQualified());
    qual_type_ir->SetVolatility(canonical_type.isVolatileQualified());
    qual_type_ir->SetSourceFile(source_file);
    return TypeAndCreationStatus(std::move(qual_type_ir));
  }
  const clang::Type *type_ptr = canonical_type.getTypePtr();
  if (type_ptr->isPointerType()) {
    auto pointer_type_ir = std::make_unique<repr::PointerTypeIR>();
    pointer_type_ir->SetSourceFile(source_file);
    return TypeAndCreationStatus(std::move(pointer_type_ir));
  }
  if (type_ptr->isLValueReferenceType()) {
    auto lvalue_reference_type_ir =
        std::make_unique<repr::LvalueReferenceTypeIR>();
    lvalue_reference_type_ir->SetSourceFile(source_file);
    return TypeAndCreationStatus(std::move(lvalue_reference_type_ir));
  }
  if (type_ptr->isRValueReferenceType()) {
    auto rvalue_reference_type_ir =
        std::make_unique<repr::RvalueReferenceTypeIR>();
    rvalue_reference_type_ir->SetSourceFile(source_file);
    return TypeAndCreationStatus(std::move(rvalue_reference_type_ir));
  }
  if (type_ptr->isArrayType()) {
    auto array_type_ir = std::make_unique<repr::ArrayTypeIR>();
    array_type_ir->SetSourceFile(source_file);
    return TypeAndCreationStatus(std::move(array_type_ir));
  }
  if (type_ptr->isEnumeralType()) {
    return TypeAndCreationStatus(std::make_unique<repr::EnumTypeIR>());
  }
  if (type_ptr->isBuiltinType()) {
    auto builtin_type_ir = std::make_unique<repr::BuiltinTypeIR>();
    builtin_type_ir->SetSignedness(type_ptr->isUnsignedIntegerType());
    builtin_type_ir->SetIntegralType(type_ptr->isIntegralType(*ast_contextp_));
    return TypeAndCreationStatus(std::move(builtin_type_ir));
  }
  if (auto &&func_type_ptr =
          llvm::dyn_cast<const clang::FunctionType>(type_ptr)) {
    FunctionTypeWrapper function_type_wrapper(mangle_contextp_, ast_contextp_,
                                              cip_, func_type_ptr, module_,
                                              ast_caches_, source_file);
    if (!function_type_wrapper.GetFunctionType()) {
      llvm::errs() << "FunctionType could not be created\n";
      ::exit(1);
    }
  }
  if (type_ptr->isRecordType()) {
    // If this record is anonymous, create it.
    const clang::RecordDecl *anon_record = GetAnonymousRecord(canonical_type);
    // Avoid constructing RecordDeclWrapper with invalid record, which results
    // in segmentation fault.
    if (anon_record && !anon_record->isInvalidDecl() &&
        !CreateAnonymousRecord(anon_record)) {
      llvm::errs() << "Anonymous record could not be created\n";
      ::exit(1);
    }
  }
  return TypeAndCreationStatus(nullptr, false);
}

std::string ABIWrapper::QualTypeToString(const clang::QualType &sweet_qt) {
  const clang::QualType salty_qt = sweet_qt.getCanonicalType();
  // clang::TypeName::getFullyQualifiedName removes the part of the type related
  // to it being a template parameter. Don't use it for dependent types.
  if (salty_qt.getTypePtr()->isDependentType()) {
    return salty_qt.getAsString();
  }
  return clang::TypeName::getFullyQualifiedName(
      salty_qt, *ast_contextp_, ast_contextp_->getPrintingPolicy());
}


//------------------------------------------------------------------------------
// Function Type Wrapper
//------------------------------------------------------------------------------

FunctionTypeWrapper::FunctionTypeWrapper(
    clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::FunctionType *function_type, repr::ModuleIR *module,
    ASTCaches *ast_caches, const std::string &source_file)
    : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, module,
                 ast_caches),
      function_type_(function_type),
      source_file_(source_file) {}

bool FunctionTypeWrapper::SetupFunctionType(
    repr::FunctionTypeIR *function_type_ir) {
  // Add ReturnType
  function_type_ir->SetReturnType(
      GetTypeUniqueId(function_type_->getReturnType()));
  function_type_ir->SetSourceFile(source_file_);
  const clang::FunctionProtoType *function_pt =
      llvm::dyn_cast<clang::FunctionProtoType>(function_type_);
  if (!function_pt) {
    return true;
  }
  for (unsigned i = 0, e = function_pt->getNumParams(); i != e; ++i) {
    clang::QualType param_type = function_pt->getParamType(i);
    if (!SetupFunctionParameter(function_type_ir, param_type, false,
                                source_file_)) {
      return false;
    }
  }
  return true;
}

bool FunctionTypeWrapper::GetFunctionType() {
  auto abi_decl = std::make_unique<repr::FunctionTypeIR>();
  clang::QualType canonical_type = function_type_->getCanonicalTypeInternal();
  if (!CreateBasicNamedAndTypedDecl(canonical_type, abi_decl.get(), "")) {
    llvm::errs() << "Couldn't create (function type) extended type\n";
    return false;
  }
  return SetupFunctionType(abi_decl.get()) &&
      module_->AddLinkableMessage(*abi_decl);
}


//------------------------------------------------------------------------------
// Function Decl Wrapper
//------------------------------------------------------------------------------

FunctionDeclWrapper::FunctionDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::FunctionDecl *decl,
    repr::ModuleIR *module,
    ASTCaches *ast_caches)
    : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, module,
                 ast_caches),
      function_decl_(decl) {}

bool FunctionDeclWrapper::SetupThisParameter(repr::FunctionIR *functionp,
                                             const std::string &source_file) {
  const clang::CXXMethodDecl *cxx_method_decl =
      llvm::dyn_cast<clang::CXXMethodDecl>(function_decl_);
  // No this pointer for static methods.
  if (!cxx_method_decl || cxx_method_decl->isStatic()) {
    return true;
  }
  clang::QualType this_type = cxx_method_decl->getThisType();
  return SetupFunctionParameter(functionp, this_type, false, source_file, true);
}

bool FunctionDeclWrapper::SetupFunctionParameters(
    repr::FunctionIR *functionp,
    const std::string &source_file) {
  clang::FunctionDecl::param_const_iterator param_it =
      function_decl_->param_begin();
  // If this is a CXXMethodDecl, we need to add the "this" pointer.
  if (!SetupThisParameter(functionp, source_file)) {
    llvm::errs() << "Setting up 'this' parameter failed\n";
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

bool FunctionDeclWrapper::SetupFunction(repr::FunctionIR *functionp,
                                        const std::string &source_file) {
  // Go through all the parameters in the method and add them to the fields.
  // Also get the fully qualfied name.
  // TODO: Change this to get the complete function signature
  functionp->SetName(function_decl_->getQualifiedNameAsString());
  functionp->SetSourceFile(source_file);
  clang::QualType return_type = function_decl_->getReturnType();

  functionp->SetReturnType(GetTypeUniqueId(return_type));
  functionp->SetAccess(AccessClangToIR(function_decl_->getAccess()));
  return CreateBasicNamedAndTypedDecl(return_type, source_file) &&
      SetupFunctionParameters(functionp, source_file) &&
      SetupTemplateInfo(functionp, source_file);
}

bool FunctionDeclWrapper::SetupTemplateInfo(repr::FunctionIR *functionp,
                                            const std::string &source_file) {
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

std::unique_ptr<repr::FunctionIR> FunctionDeclWrapper::GetFunctionDecl() {
  auto abi_decl = std::make_unique<repr::FunctionIR>();
  std::string source_file = GetCachedDeclSourceFile(function_decl_, cip_);
  if (!SetupFunction(abi_decl.get(), source_file)) {
    return nullptr;
  }
  return abi_decl;
}


//------------------------------------------------------------------------------
// Record Decl Wrapper
//------------------------------------------------------------------------------

RecordDeclWrapper::RecordDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::RecordDecl *decl, repr::ModuleIR *module,
    ASTCaches *ast_caches)
    : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, module,
                 ast_caches),
      record_decl_(decl) {}

bool RecordDeclWrapper::SetupRecordFields(repr::RecordTypeIR *recordp,
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
    std::string field_name = field->getName();
    uint64_t field_offset = record_layout.getFieldOffset(field_index);
    recordp->AddRecordField(repr::RecordFieldIR(
        field_name, GetTypeUniqueId(field_type), field_offset,
        AccessClangToIR(field->getAccess())));
    field++;
    field_index++;
  }
  return true;
}

bool RecordDeclWrapper::SetupCXXBases(
    repr::RecordTypeIR *cxxp, const clang::CXXRecordDecl *cxx_record_decl) {
  if (!cxx_record_decl || !cxxp) {
    return false;
  }
  clang::CXXRecordDecl::base_class_const_iterator base_class =
      cxx_record_decl->bases_begin();
  while (base_class != cxx_record_decl->bases_end()) {
    bool is_virtual = base_class->isVirtual();
    repr::AccessSpecifierIR access =
        AccessClangToIR(base_class->getAccessSpecifier());
    cxxp->AddCXXBaseSpecifier(repr::CXXBaseSpecifierIR(
        GetTypeUniqueId(base_class->getType()), is_virtual, access));
    base_class++;
  }
  return true;
}

typedef std::map<uint64_t, clang::ThunkInfo> ThunkMap;

bool RecordDeclWrapper::SetupRecordVTable(
    repr::RecordTypeIR *record_declp,
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
  llvm::ArrayRef<clang::VTableLayout::VTableThunkTy> thunks =
      vtable_layout.vtable_thunks();
  ThunkMap thunk_map(thunks.begin(), thunks.end());
  repr::VTableLayoutIR vtable_ir_layout;

  uint64_t index = 0;
  for (auto vtable_component : vtable_layout.vtable_components()) {
    clang::ThunkInfo thunk_info;
    ThunkMap::iterator it = thunk_map.find(index);
    if (it != thunk_map.end()) {
      thunk_info = it->second;
    }
    repr::VTableComponentIR added_component =
        SetupRecordVTableComponent(vtable_component, thunk_info);
    vtable_ir_layout.AddVTableComponent(std::move(added_component));
    index++;
  }
  record_declp->SetVTableLayout(std::move(vtable_ir_layout));
  return true;
}

repr::VTableComponentIR RecordDeclWrapper::SetupRecordVTableComponent(
    const clang::VTableComponent &vtable_component,
    const clang::ThunkInfo &thunk_info) {
  repr::VTableComponentIR::Kind kind =
      repr::VTableComponentIR::Kind::RTTI;
  std::string mangled_component_name = "";
  llvm::raw_string_ostream ostream(mangled_component_name);
  int64_t value = 0;
  clang::VTableComponent::Kind clang_component_kind =
      vtable_component.getKind();
  bool is_pure = false;

  switch (clang_component_kind) {
    case clang::VTableComponent::CK_VCallOffset:
      kind = repr::VTableComponentIR::Kind::VCallOffset;
      value = vtable_component.getVCallOffset().getQuantity();
      break;
    case clang::VTableComponent::CK_VBaseOffset:
      kind = repr::VTableComponentIR::Kind::VBaseOffset;
      value = vtable_component.getVBaseOffset().getQuantity();
      break;
    case clang::VTableComponent::CK_OffsetToTop:
      kind = repr::VTableComponentIR::Kind::OffsetToTop;
      value = vtable_component.getOffsetToTop().getQuantity();
      break;
    case clang::VTableComponent::CK_RTTI:
      {
        kind = repr::VTableComponentIR::Kind::RTTI;
        const clang::CXXRecordDecl *rtti_decl =
            vtable_component.getRTTIDecl();
        assert(rtti_decl != nullptr);
        mangled_component_name = GetMangledRTTI(rtti_decl);
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
        is_pure = method_decl->isPure();
        switch (clang_component_kind) {
          case clang::VTableComponent::CK_FunctionPointer:
            kind = repr::VTableComponentIR::Kind::FunctionPointer;
            if (thunk_info.isEmpty()) {
              mangle_contextp_->mangleName(method_decl, ostream);
            } else {
              mangle_contextp_->mangleThunk(method_decl, thunk_info, ostream);
            }
            ostream.flush();
            break;
          case clang::VTableComponent::CK_CompleteDtorPointer:
          case clang::VTableComponent::CK_DeletingDtorPointer:
            {
              clang::CXXDtorType dtor_type;
              if (clang_component_kind ==
                  clang::VTableComponent::CK_CompleteDtorPointer) {
                dtor_type = clang::CXXDtorType::Dtor_Complete;
                kind = repr::VTableComponentIR::Kind::CompleteDtorPointer;
              } else {
                dtor_type = clang::CXXDtorType::Dtor_Deleting;
                kind = repr::VTableComponentIR::Kind::DeletingDtorPointer;
              }

              if (thunk_info.isEmpty()) {
                mangle_contextp_->mangleCXXDtor(
                    vtable_component.getDestructorDecl(), dtor_type, ostream);
              } else {
                mangle_contextp_->mangleCXXDtorThunk(
                    vtable_component.getDestructorDecl(), dtor_type,
                    thunk_info.This, ostream);
              }
              ostream.flush();
            }
            break;
          case clang::VTableComponent::CK_UnusedFunctionPointer:
            kind = repr::VTableComponentIR::Kind::UnusedFunctionPointer;
            break;
          default:
            break;
        }
      }
      break;
    default:
      break;
  }
  return repr::VTableComponentIR(mangled_component_name, kind, value,
                                     is_pure);
}

bool RecordDeclWrapper::SetupTemplateInfo(
    repr::RecordTypeIR *record_declp,
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

bool RecordDeclWrapper::SetupRecordInfo(repr::RecordTypeIR *record_declp,
                                        const std::string &source_file) {
  if (!record_declp) {
    return false;
  }
  if (record_decl_->isStruct()) {
    record_declp->SetRecordKind(
        repr::RecordTypeIR::RecordKind::struct_kind);
  } else if (record_decl_->isClass()) {
    record_declp->SetRecordKind(
        repr::RecordTypeIR::RecordKind::class_kind);
  } else {
    record_declp->SetRecordKind(
        repr::RecordTypeIR::RecordKind::union_kind);
  }

  const clang::Type *basic_type = nullptr;
  if (!(basic_type = record_decl_->getTypeForDecl())) {
    return false;
  }
  clang::QualType qual_type = basic_type->getCanonicalTypeInternal();
  if (!CreateExtendedType(qual_type, record_declp)) {
    return false;
  }
  record_declp->SetSourceFile(source_file);
  if (!record_decl_->hasNameForLinkage() ||
      record_decl_->isAnonymousStructOrUnion()) {
    record_declp->SetAnonymity(true);
  }
  record_declp->SetAccess(AccessClangToIR(record_decl_->getAccess()));
  return SetupRecordFields(record_declp, source_file) &&
      SetupCXXRecordInfo(record_declp, source_file);
}

bool RecordDeclWrapper::SetupCXXRecordInfo(repr::RecordTypeIR *record_declp,
                                           const std::string &source_file) {
  const clang::CXXRecordDecl *cxx_record_decl =
      clang::dyn_cast<clang::CXXRecordDecl>(record_decl_);
  if (!cxx_record_decl) {
    return true;
  }
  return SetupTemplateInfo(record_declp, cxx_record_decl, source_file) &&
      SetupCXXBases(record_declp, cxx_record_decl) &&
      SetupRecordVTable(record_declp, cxx_record_decl);
}

// TODO: Can we use clang's ODR hash to do faster ODR checking?
bool RecordDeclWrapper::GetRecordDecl() {
  auto abi_decl = std::make_unique<repr::RecordTypeIR>();
  std::string source_file = GetCachedDeclSourceFile(record_decl_, cip_);
  if (!SetupRecordInfo(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up CXX Bases / Template Info failed\n";
    return false;
  }
  if ((abi_decl->GetReferencedType() == "") ||
      (abi_decl->GetSelfType() == "")) {
    // The only way to have an empty referenced / self type is when the type was
    // cached, don't add the record.
    return true;
  }
  return module_->AddLinkableMessage(*abi_decl);
}

std::string RecordDeclWrapper::GetMangledRTTI(
    const clang::CXXRecordDecl *cxx_record_decl) {
  clang::QualType qual_type =
      cxx_record_decl->getTypeForDecl()->getCanonicalTypeInternal();
  llvm::SmallString<256> uid;
  llvm::raw_svector_ostream out(uid);
  mangle_contextp_->mangleCXXRTTI(qual_type, out);
  return uid.str();
}


//------------------------------------------------------------------------------
// Enum Decl Wrapper
//------------------------------------------------------------------------------

EnumDeclWrapper::EnumDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::EnumDecl *decl, repr::ModuleIR *module,
    ASTCaches *ast_caches)
    : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, module,
                 ast_caches),
      enum_decl_(decl) {}

bool EnumDeclWrapper::SetupEnumFields(repr::EnumTypeIR *enump) {
  if (!enump) {
    return false;
  }
  clang::EnumDecl::enumerator_iterator enum_it = enum_decl_->enumerator_begin();
  while (enum_it != enum_decl_->enumerator_end()) {
    std::string name = enum_it->getQualifiedNameAsString();
    uint64_t field_value = enum_it->getInitVal().getExtValue();
    enump->AddEnumField(repr::EnumFieldIR(name, field_value));
    enum_it++;
  }
  return true;
}

bool EnumDeclWrapper::SetupEnum(repr::EnumTypeIR *enum_type,
                                const std::string &source_file) {
  clang::QualType enum_qual_type =
      enum_decl_->getTypeForDecl()->getCanonicalTypeInternal();
  if (!CreateExtendedType(enum_qual_type, enum_type)) {
    return false;
  }
  enum_type->SetSourceFile(source_file);
  enum_type->SetUnderlyingType(GetTypeUniqueId(enum_decl_->getIntegerType()));
  enum_type->SetAccess(AccessClangToIR(enum_decl_->getAccess()));
  return SetupEnumFields(enum_type) &&
      CreateBasicNamedAndTypedDecl(enum_decl_->getIntegerType(), "");
}

bool EnumDeclWrapper::GetEnumDecl() {
  auto abi_decl = std::make_unique<repr::EnumTypeIR>();
  std::string source_file = GetCachedDeclSourceFile(enum_decl_, cip_);

  if (!SetupEnum(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up Enum failed\n";
    return false;
  }
  return module_->AddLinkableMessage(*abi_decl);
}


//------------------------------------------------------------------------------
// Global Decl Wrapper
//------------------------------------------------------------------------------

GlobalVarDeclWrapper::GlobalVarDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::VarDecl *decl, repr::ModuleIR *module,
    ASTCaches *ast_caches)
    : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p, module,
                 ast_caches),
      global_var_decl_(decl) {}

bool GlobalVarDeclWrapper::SetupGlobalVar(repr::GlobalVarIR *global_varp,
                                          const std::string &source_file) {
  // Temporary fix: clang segfaults on trying to mangle global variable which
  // is a dependent sized array type.
  std::string mangled_name =
      GetMangledNameDecl(global_var_decl_, mangle_contextp_);
  if (!CreateBasicNamedAndTypedDecl(global_var_decl_->getType(), source_file)) {
    return false;
  }
  global_varp->SetSourceFile(source_file);
  global_varp->SetName(global_var_decl_->getQualifiedNameAsString());
  global_varp->SetLinkerSetKey(mangled_name);
  global_varp->SetAccess(AccessClangToIR(global_var_decl_->getAccess()));
  global_varp->SetReferencedType(GetTypeUniqueId(global_var_decl_->getType()));
  return true;
}

bool GlobalVarDeclWrapper::GetGlobalVarDecl() {
  auto abi_decl = std::make_unique<repr::GlobalVarIR>();
  std::string source_file = GetCachedDeclSourceFile(global_var_decl_, cip_);
  return SetupGlobalVar(abi_decl.get(), source_file) &&
      module_->AddLinkableMessage(*abi_decl);
}


}  // dumper
}  // header_checker

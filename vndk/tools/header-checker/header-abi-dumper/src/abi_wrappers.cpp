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
    const clang::CompilerInstance *cip)
  : cip_(cip),
    mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp) { }

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
  std::string file_name_adjusted = "";
  char file_abs_path[PATH_MAX];
  if (realpath(file_name.str().c_str(), file_abs_path) == nullptr) {
    return "";
  }
  return file_abs_path;
}

abi_dump::AccessSpecifier ABIWrapper::AccessClangToDump(
    const clang::AccessSpecifier sp) const {
  switch (sp) {
    case clang::AS_private: {
      return abi_dump::AccessSpecifier::private_access;
      break;
    }
    case clang::AS_protected: {
      return abi_dump::AccessSpecifier::protected_access;
      break;
    }
    default: {
      return abi_dump::AccessSpecifier::public_access;
      break;
    }
  }
}

// Dumping the size and alignment is optional. This is since clang can lazily
// instantiate records as incomplete and therefore their sizes 'may' not be
// computable. b/62307940
bool ABIWrapper::SetupBasicTypeAbi(abi_dump::BasicTypeAbi *type_abi,
                                   const clang::QualType type,
                                   bool dump_size) const {
  if (!type_abi) {
    return false;
  }
  const clang::QualType canonical_type = type.getCanonicalType();
  type_abi->set_name(QualTypeToString(canonical_type));
  // Cannot determine the size and alignment for template parameter dependent
  // types as well as incomplete types.
  const clang::Type *base_type = canonical_type.getTypePtr();
  clang::Type::TypeClass type_class = base_type->getTypeClass();
  // Temporary Hack for auto type sizes. Not determinable.
  if (dump_size && base_type && !(base_type->isDependentType()) &&
      !(base_type->isIncompleteType()) && (type_class != clang::Type::Auto)) {
    std::pair<clang::CharUnits, clang::CharUnits> size_and_alignment =
    ast_contextp_->getTypeInfoInChars(canonical_type);
    int64_t size = size_and_alignment.first.getQuantity();
    int64_t alignment = size_and_alignment.second.getQuantity();
    type_abi->set_size(size);
    type_abi->set_alignment(alignment);
  }
  return true;
}

bool ABIWrapper::SetupBasicNamedAndTypedDecl(
    abi_dump::BasicNamedAndTypedDecl *basic_named_and_typed_decl,
    const clang::QualType type, const std::string &name,
    const clang::AccessSpecifier &access, std::string key,
    bool dump_size) const {
  if (!basic_named_and_typed_decl) {
    return false;
  }
  abi_dump::AccessSpecifier access_dump = AccessClangToDump(access);
  basic_named_and_typed_decl->set_name(name);
  basic_named_and_typed_decl->set_access(access_dump);
  if (key != "") {
    basic_named_and_typed_decl->set_linker_set_key(key);
  }
  return SetupBasicTypeAbi(basic_named_and_typed_decl->mutable_type_abi(),
                           type, dump_size);
}

static bool ShouldDumpSize(clang::QualType qt) {
  const clang::Type *type_ptr = qt.getTypePtr();
  assert(type_ptr != nullptr);
  if (type_ptr->isBuiltinType() || type_ptr->isPointerType()) {
    return true;
  }
  return false;
}

std::string ABIWrapper::GetTypeLinkageName(const clang::Type *typep) const {
  assert(typep != nullptr);
  clang::QualType qt = typep->getCanonicalTypeInternal();
  return QualTypeToString(qt);
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

bool ABIWrapper::SetupTemplateParamNames(
    abi_dump::TemplateInfo *tinfo,
    clang::TemplateParameterList *pl) const {
  if (tinfo->elements_size() > 0) {
    return true;
  }

  clang::TemplateParameterList::iterator template_it = pl->begin();
  while (template_it != pl->end()) {
    abi_dump::TemplateElement *template_parameterp =
        tinfo->add_elements();
    if (!template_parameterp) {
      return false;
    }
    abi_dump::TemplateElement::BasicTemplateElementAbi *basic_abi =
        template_parameterp->mutable_basic_abi();
    if (!basic_abi) {
      return false;
    }
    std::string name = (*template_it)->getName();
    basic_abi->set_name(name);
    // TODO : Default arg ?
    basic_abi->set_linker_set_key(name);
    template_it++;
  }
  return true;
}

std::string ABIWrapper::GetTagDeclQualifiedName(
    const clang::TagDecl *decl) const {
  if (decl->getTypedefNameForAnonDecl()) {
    return decl->getTypedefNameForAnonDecl()->getQualifiedNameAsString();
  }
  return decl->getQualifiedNameAsString();
}

bool ABIWrapper::SetupTemplateArguments(
    abi_dump::TemplateInfo *tinfo,
    const clang::TemplateArgumentList *tl) const {
  for (int i = 0; i < tl->size(); i++) {
    const clang::TemplateArgument &arg = (*tl)[i];
    //TODO: More comprehensive checking needed.
    if (arg.getKind() != clang::TemplateArgument::Type) {
      continue;
    }
    clang::QualType type = arg.getAsType();
    abi_dump::TemplateElement *template_parameterp =
        tinfo->add_elements();
    if (!template_parameterp) {
      return false;
    }
    abi_dump::TemplateElement::BasicTemplateElementAbi *basic_abi =
        template_parameterp->mutable_basic_abi();
    if (!basic_abi || !SetupBasicTypeAbi(basic_abi->mutable_type_abi(), type,
                                         false)) {
      return false;
    }
    // TODO : default arg
    basic_abi->set_linker_set_key(QualTypeToString(type));
  }
  return true;
}

std::string ABIWrapper::QualTypeToString(
    const clang::QualType &sweet_qt) const {
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
    const clang::FunctionDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    function_decl_(decl) { }

bool FunctionDeclWrapper::SetupFunctionParameters(
    abi_dump::FunctionDecl *functionp) const {
  clang::FunctionDecl::param_const_iterator param_it =
      function_decl_->param_begin();
  while (param_it != function_decl_->param_end()) {
    abi_dump::ParamDecl *function_fieldp = functionp->add_parameters();
    if (!function_fieldp) {
      llvm::errs() << "Couldn't add parameter to method. Aborting\n";
      return false;
    }
    // The linker set key is blank since that shows up in the mangled name.
    bool has_default_arg = (*param_it)->hasDefaultArg();
    clang::QualType param_qt = (*param_it)->getType();
    bool should_dump_size = ShouldDumpSize(param_qt);
    if (!SetupBasicNamedAndTypedDecl(
        function_fieldp->mutable_basic_abi(),
        (*param_it)->getType(), (*param_it)->getName(),
        (*param_it)->getAccess(), has_default_arg ? "true" : "false",
        should_dump_size)) {
      return false;
    }
    function_fieldp->set_default_arg(has_default_arg);
    param_it++;
  }
  return true;
}

bool FunctionDeclWrapper::SetupFunction(abi_dump::FunctionDecl *functionp,
                                        const std::string &source_file) const {
  // Go through all the parameters in the method and add them to the fields.
  // Also get the fully qualfied name.
  functionp->set_source_file(source_file);
  // Combine the function name and return type to form a NamedAndTypedDecl
  clang::QualType return_type = function_decl_->getReturnType();
  bool should_dump_size = ShouldDumpSize(return_type);
  return SetupBasicNamedAndTypedDecl(
      functionp->mutable_basic_abi(),
      return_type, function_decl_->getQualifiedNameAsString(),
      function_decl_->getAccess(), "", should_dump_size) &&
      SetupTemplateInfo(functionp) && SetupFunctionParameters(functionp);
}

bool FunctionDeclWrapper::SetupTemplateInfo(
    abi_dump::FunctionDecl *functionp) const {
  switch (function_decl_->getTemplatedKind()) {
    case clang::FunctionDecl::TK_FunctionTemplate: {
      clang::FunctionTemplateDecl *template_decl =
          function_decl_->getDescribedFunctionTemplate();
      if (template_decl) {
        clang::TemplateParameterList *template_parameter_list =
            template_decl->getTemplateParameters();
        if (template_parameter_list &&
            !SetupTemplateParamNames(functionp->mutable_template_info(),
                                     template_parameter_list)) {
          return false;
        }
      }
      break;
    }
    case clang::FunctionDecl::TK_FunctionTemplateSpecialization: {
      const clang::TemplateArgumentList *arg_list =
          function_decl_->getTemplateSpecializationArgs();
      if (arg_list &&
          !SetupTemplateArguments(functionp->mutable_template_info(),
                                  arg_list)) {
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

std::unique_ptr<abi_dump::FunctionDecl>
FunctionDeclWrapper::GetFunctionDecl() const {
  std::unique_ptr<abi_dump::FunctionDecl> abi_decl(
      new abi_dump::FunctionDecl());
  std::string source_file = GetDeclSourceFile(function_decl_, cip_);
  if (!SetupFunction(abi_decl.get(), source_file)) {
    return nullptr;
  }
  return abi_decl;
}

RecordDeclWrapper::RecordDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::RecordDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    record_decl_(decl) { }

bool RecordDeclWrapper::SetupRecordFields(abi_dump::RecordDecl *recordp) const {
  clang::RecordDecl::field_iterator field = record_decl_->field_begin();
  while (field != record_decl_->field_end()) {
    abi_dump::RecordFieldDecl *record_fieldp = recordp->add_fields();
    if (!record_fieldp) {
      llvm::errs() << " Couldn't add record field: " << field->getName()
                   << " to reference dump\n";
      return false;
    }
    if (!SetupBasicNamedAndTypedDecl(record_fieldp->mutable_basic_abi(),
                                     field->getType(), field->getName(),
                                     field->getAccess(), "", true)) {
      return false;
    }
    field++;
  }
  return true;
}

bool RecordDeclWrapper::SetupCXXBases(
    abi_dump::RecordDecl *cxxp,
    const clang::CXXRecordDecl *cxx_record_decl) const {
  assert(cxx_record_decl != nullptr);
  clang::CXXRecordDecl::base_class_const_iterator base_class =
      cxx_record_decl->bases_begin();
  while (base_class != cxx_record_decl->bases_end()) {
    abi_dump::CXXBaseSpecifier *base_specifierp = cxxp->add_base_specifiers();
    if (!base_specifierp) {
      llvm::errs() << " Couldn't add base specifier to reference dump\n";
      return false;
    }
    std::string name = QualTypeToString(base_class->getType());
    bool is_virtual = base_class->isVirtual();
    if (!SetupBasicNamedAndTypedDecl(base_specifierp->mutable_basic_abi(),
                                     base_class->getType(),
                                     "", base_class->getAccessSpecifier(),
                                     "", false)) {
      return false;
    }
    base_specifierp->set_is_virtual(is_virtual);
    base_class++;
  }
  return true;
}

bool RecordDeclWrapper::SetupRecordVTable(
    abi_dump::RecordDecl *record_declp,
    const clang::CXXRecordDecl *cxx_record_decl) const {
  assert(cxx_record_decl != nullptr);
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
  abi_dump::VTableLayout *vtablep = record_declp->mutable_vtable_layout();
  if (!vtablep) {
    return false;
  }
  for (const auto &vtable_component : vtable_layout.vtable_components()) {
    abi_dump::VTableComponent *added_vtable_component =
        vtablep->add_vtable_components();
    if (!added_vtable_component ||
        !SetupRecordVTableComponent(added_vtable_component, vtable_component)) {
      return false;
    }
  }
  return true;
}

bool RecordDeclWrapper::SetupRecordVTableComponent(
    abi_dump::VTableComponent *added_vtable_component,
    const clang::VTableComponent &vtable_component) const {
  assert(added_vtable_component != nullptr);
  abi_dump::VTableComponent_Kind kind = abi_dump::VTableComponent_Kind_RTTI;
  std::string mangled_component_name = "";
  llvm::raw_string_ostream ostream(mangled_component_name);
  int64_t value = 0;
  clang::VTableComponent::Kind clang_component_kind =
      vtable_component.getKind();
    switch (clang_component_kind) {
      case clang::VTableComponent::CK_VCallOffset:
        kind =  abi_dump::VTableComponent_Kind_VCallOffset;
        value = vtable_component.getVCallOffset().getQuantity();
        break;
      case clang::VTableComponent::CK_VBaseOffset:
        kind =  abi_dump::VTableComponent_Kind_VBaseOffset;
        value = vtable_component.getVBaseOffset().getQuantity();
        break;
      case clang::VTableComponent::CK_OffsetToTop:
        kind =  abi_dump::VTableComponent_Kind_OffsetToTop;
        value = vtable_component.getOffsetToTop().getQuantity();
        break;
      case clang::VTableComponent::CK_RTTI:
        {
          kind =  abi_dump::VTableComponent_Kind_RTTI;
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
              kind =  abi_dump::VTableComponent_Kind_FunctionPointer;
              mangled_component_name = GetMangledNameDecl(method_decl,
                                                          mangle_contextp_);
              break;
            case clang::VTableComponent::CK_CompleteDtorPointer:
              kind =  abi_dump::VTableComponent_Kind_CompleteDtorPointer;
              mangle_contextp_->mangleCXXDtor(
                  vtable_component.getDestructorDecl(),
                  clang::CXXDtorType::Dtor_Complete, ostream);
              ostream.flush();

              break;
            case clang::VTableComponent::CK_DeletingDtorPointer:
              kind =  abi_dump::VTableComponent_Kind_DeletingDtorPointer;
              mangle_contextp_->mangleCXXDtor(
                  vtable_component.getDestructorDecl(),
                  clang::CXXDtorType::Dtor_Deleting, ostream);
              ostream.flush();
              break;
            case clang::VTableComponent::CK_UnusedFunctionPointer:
              kind =  abi_dump::VTableComponent_Kind_UnusedFunctionPointer;
            default:
              break;
          }
        }
        break;
      default:
        return false;
    }
  added_vtable_component->set_kind(kind);
  added_vtable_component->set_value(value);
  added_vtable_component->set_mangled_component_name(mangled_component_name);
  return true;
}

bool RecordDeclWrapper::SetupTemplateInfo(
    abi_dump::RecordDecl *record_declp,
    const clang::CXXRecordDecl *cxx_record_decl) const {
  assert(cxx_record_decl != nullptr);
  if (cxx_record_decl->isTemplateDecl()) {
    clang::ClassTemplateDecl *template_decl =
        cxx_record_decl->getDescribedClassTemplate();
    if (template_decl) {
      clang::TemplateParameterList *template_parameter_list =
          template_decl->getTemplateParameters();
      if (template_parameter_list &&
          !SetupTemplateParamNames(record_declp->mutable_template_info(),
                                   template_parameter_list)) {
        return false;
      }
    }
  } else {
    const clang::ClassTemplateSpecializationDecl *specialization_decl =
        clang::dyn_cast<clang::ClassTemplateSpecializationDecl>(
            cxx_record_decl);
    if(specialization_decl) {
      const clang::TemplateArgumentList *arg_list =
          &specialization_decl->getTemplateArgs();
      if (arg_list &&
          !SetupTemplateArguments(record_declp->mutable_template_info(),
                                  arg_list)) {
        return false;
      }
    }
  }
  return true;
}

bool RecordDeclWrapper::SetupRecordInfo(abi_dump::RecordDecl *record_declp,
                                        const std::string &source_file) const {
  std::string qualified_name = GetTagDeclQualifiedName(record_decl_);
  const clang::Type *basic_type = nullptr;
  if (!(basic_type = record_decl_->getTypeForDecl())) {
    return false;
  }
  std::string mangled_name = ABIWrapper::GetTypeLinkageName(basic_type);
  clang::QualType type = basic_type->getCanonicalTypeInternal();
  std::string linker_key = (mangled_name == "") ? qualified_name : mangled_name;
  if (!SetupBasicNamedAndTypedDecl(record_declp->mutable_basic_abi(),
                                   type, qualified_name,
                                   record_decl_->getAccess(), linker_key,
                                   true)) {
    return false;
  }
  record_declp->set_mangled_record_name(mangled_name);
  record_declp->set_source_file(source_file);
  return true;
}

bool RecordDeclWrapper::SetupCXXRecordInfo(
    abi_dump::RecordDecl *record_declp) const {
  const clang::CXXRecordDecl *cxx_record_decl =
      clang::dyn_cast<clang::CXXRecordDecl>(record_decl_);
  if (!cxx_record_decl) {
    return true;
  }
  return SetupTemplateInfo(record_declp, cxx_record_decl) &&
      SetupCXXBases(record_declp, cxx_record_decl) &&
      SetupRecordVTable(record_declp, cxx_record_decl);
}

std::unique_ptr<abi_dump::RecordDecl> RecordDeclWrapper::GetRecordDecl() const {
  std::unique_ptr<abi_dump::RecordDecl> abi_decl(new abi_dump::RecordDecl());
  std::string source_file = GetDeclSourceFile(record_decl_, cip_);
  abi_dump::RecordDecl *record_declp = abi_decl.get();
  if (!SetupRecordInfo(record_declp, source_file) ||
      !SetupRecordFields(record_declp) ||
      !SetupCXXRecordInfo(abi_decl.get())) {
    llvm::errs() << "Setting up CXX Bases / Template Info failed\n";
    return nullptr;
  }
  return abi_decl;
}

EnumDeclWrapper::EnumDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::EnumDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    enum_decl_(decl) { }

bool EnumDeclWrapper::SetupEnumFields(abi_dump::EnumDecl *enump) const {
  clang::EnumDecl::enumerator_iterator enum_it = enum_decl_->enumerator_begin();
  while (enum_it != enum_decl_->enumerator_end()) {
    abi_dump::EnumFieldDecl *enum_fieldp = enump->add_enum_fields();
    std::string name = enum_it->getQualifiedNameAsString();
    uint64_t field_value = enum_it->getInitVal().getExtValue();
    if (!enum_fieldp ||
        !SetupBasicNamedAndTypedDecl(enum_fieldp->mutable_basic_abi(),
                                     enum_it->getType(), name,
                                     enum_it->getAccess(),
                                     std::to_string(field_value), true)) {
      return false;
    }
    enum_fieldp->set_enum_field_value(field_value);
    enum_it++;
  }
  return true;
}

bool EnumDeclWrapper::SetupEnum(abi_dump::EnumDecl *enump,
                                const std::string &source_file) const {
  std::string enum_name = GetTagDeclQualifiedName(enum_decl_);
  clang::QualType enum_type = enum_decl_->getIntegerType();
  if (!SetupBasicNamedAndTypedDecl(enump->mutable_basic_abi(), enum_type,
                                   enum_name, enum_decl_->getAccess(),
                                   enum_name, true) ||
      !SetupEnumFields(enump)) {
    return false;
  }
  enump->set_source_file(source_file);
  return true;
}

std::unique_ptr<abi_dump::EnumDecl> EnumDeclWrapper::GetEnumDecl() const {
  std::unique_ptr<abi_dump::EnumDecl> abi_decl(new abi_dump::EnumDecl());
  std::string source_file = GetDeclSourceFile(enum_decl_, cip_);

  if (!SetupEnum(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up Enum fields failed\n";
    return nullptr;
  }
  return abi_decl;
}

GlobalVarDeclWrapper::GlobalVarDeclWrapper(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::VarDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    global_var_decl_(decl) { }

bool GlobalVarDeclWrapper::SetupGlobalVar(
    abi_dump::GlobalVarDecl *global_varp,
    const std::string &source_file) const {
  // Temporary fix : clang segfaults on trying to mangle global variable which
  // is a dependent sized array type.
  std::string qualified_name = global_var_decl_->getQualifiedNameAsString();
  std::string mangled_name =
      GetMangledNameDecl(global_var_decl_, mangle_contextp_);
  if (!SetupBasicNamedAndTypedDecl(
      global_varp->mutable_basic_abi(),global_var_decl_->getType(),
      qualified_name, global_var_decl_->getAccess(),
      mangled_name, true)) {
    return false;
  }
  global_varp->set_source_file(source_file);
  return true;
}

std::unique_ptr<abi_dump::GlobalVarDecl>
GlobalVarDeclWrapper::GetGlobalVarDecl() const {
  std::unique_ptr<abi_dump::GlobalVarDecl>
      abi_decl(new abi_dump::GlobalVarDecl);
  std::string source_file = GetDeclSourceFile(global_var_decl_, cip_);
  if (!SetupGlobalVar(abi_decl.get(), source_file)) {
    return nullptr;
  }
  return abi_decl;
}

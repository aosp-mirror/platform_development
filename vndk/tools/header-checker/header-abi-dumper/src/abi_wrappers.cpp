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

#include <clang/Tooling/Core/QualTypeNames.h>

#include <string>

using namespace abi_wrapper;

ABIWrapper::ABIWrapper(
    clang::MangleContext *mangle_contextp,
    const clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p)
  : mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp),
    cip_(compiler_instance_p) { }

std::string ABIWrapper::GetDeclSourceFile(const clang::NamedDecl *decl) const {
  clang::SourceManager &sm = cip_->getSourceManager();
  clang::SourceLocation location = decl->getLocation();
  llvm::StringRef file_name = sm.getFilename(location);
  llvm::SmallString<128> abs_path(file_name.str());
  if (llvm::sys::fs::make_absolute(abs_path)) {
    return "";
  }
  return abs_path.str();
}

std::string ABIWrapper::AccessToString(const clang::AccessSpecifier sp) const {
  std::string str = "public";
  switch (sp) {
    case clang::AS_private: {
      str = "private";
      break;
    }
    case clang::AS_protected: {
      str = "protected";
      break;
    }
    default: {
      break;
    }
  }
  return str;
}

std::string ABIWrapper::GetMangledNameDecl(const clang::NamedDecl *decl) const {
  std::string mangled_or_demangled_name = decl->getName();
  if (mangle_contextp_->shouldMangleDeclName(decl)) {
    llvm::raw_string_ostream ostream(mangled_or_demangled_name);
    mangle_contextp_->mangleName(decl, ostream);
    ostream.flush();
  }
  return mangled_or_demangled_name;
}

bool ABIWrapper::SetupTemplateParamNames(
    abi_dump::TemplateInfo *tinfo,
    clang::TemplateParameterList *pl) const {
  if (tinfo->template_parameters_size() > 0) {
    return true;
  }

  clang::TemplateParameterList::iterator template_it = pl->begin();
  while (template_it != pl->end()) {
    abi_dump::FieldDecl *template_parameterp = tinfo->add_template_parameters();
    if (!template_parameterp) {
      return false;
    }
    template_parameterp->set_field_name((*template_it)->getName());
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
    std::string type = " ";
    if(arg.getKind() == clang::TemplateArgument::Type) {
      type = QualTypeToString(arg.getAsType());
    }
    abi_dump::FieldDecl *template_parameterp =
        tinfo->add_template_parameters();
    if (!template_parameterp) {
      return false;
    }
    template_parameterp->set_field_type((type));
  }
  return true;
}

std::string ABIWrapper::QualTypeToString(
    const clang::QualType &sweet_qt) const {
  const clang::QualType salty_qt = sweet_qt.getCanonicalType();
  return clang::TypeName::getFullyQualifiedName(salty_qt, *ast_contextp_);
}

FunctionDeclWrapper::FunctionDeclWrapper(
    clang::MangleContext *mangle_contextp,
    const clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::FunctionDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    function_decl_(decl) { }

bool FunctionDeclWrapper::SetupFunction(abi_dump::FunctionDecl *functionp,
                                        const std::string &source_file) const {
  // Go through all the parameters in the method and add them to the fields.
  // Also get the fully qualfied name and mangled name and store them.
  std::string mangled_name = GetMangledNameDecl(function_decl_);
  functionp->set_function_name(function_decl_->getQualifiedNameAsString());
  functionp->set_mangled_function_name(mangled_name);
  functionp->set_linker_set_key(mangled_name);
  functionp->set_source_file(source_file);
  functionp->set_return_type(QualTypeToString(function_decl_->getReturnType()));

  clang::FunctionDecl::param_const_iterator param_it =
      function_decl_->param_begin();
  while (param_it != function_decl_->param_end()) {
    abi_dump::FieldDecl *function_fieldp = functionp->add_parameters();
    if (!function_fieldp) {
      llvm::errs() << "Couldn't add parameter to method. Aborting\n";
      return false;
    }
    function_fieldp->set_field_name((*param_it)->getName());
    function_fieldp->set_default_arg((*param_it)->hasDefaultArg());
    function_fieldp->set_field_type(QualTypeToString((*param_it)->getType()));
    param_it++;
  }
  functionp->set_access(AccessToString(function_decl_->getAccess()));
  functionp->set_template_kind(function_decl_->getTemplatedKind());
  if(!SetupTemplateInfo(functionp)) {
    return false;
  }
  return true;
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
  std::string source_file = GetDeclSourceFile(function_decl_);
  if (!SetupFunction(abi_decl.get(), source_file)) {
    return nullptr;
  }
  return abi_decl;
}

RecordDeclWrapper::RecordDeclWrapper(
    clang::MangleContext *mangle_contextp,
    const clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::RecordDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    record_decl_(decl) { }

bool RecordDeclWrapper::SetupRecordFields(
    abi_dump::RecordDecl *recordp,
    const std::string &source_file) const {
  clang::RecordDecl::field_iterator field = record_decl_->field_begin();
  while (field != record_decl_->field_end()) {
    abi_dump::FieldDecl *record_fieldp = recordp->add_fields();
    if (!record_fieldp) {
      llvm::errs() << " Couldn't add record field: " << field->getName()
                   << " to reference dump\n";
      return false;
    }
    std::string name = field->getName();
    std::string type = QualTypeToString(field->getType());
    std::string access = AccessToString(field->getAccess());
    record_fieldp->set_field_name(name);
    record_fieldp->set_field_type(type);
    record_fieldp->set_access(access);
    record_fieldp->set_linker_set_key(name + type + access);
    field++;
  }
  return true;
}

bool RecordDeclWrapper::SetupCXXBases(abi_dump::RecordDecl *cxxp) const {
  const clang::CXXRecordDecl *cxx_record_decl =
      clang::dyn_cast<clang::CXXRecordDecl>(record_decl_);
  if (!cxx_record_decl) {
    return true;
  }

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
    char is_virtual_c = is_virtual ? 't' : 'f';
    std::string access = AccessToString(base_class->getAccessSpecifier());
    base_specifierp->set_fully_qualified_name(name);
    base_specifierp->set_is_virtual(is_virtual);
    base_specifierp->set_access(access);
    base_specifierp->set_linker_set_key(name + is_virtual_c + access);
    base_class++;
  }
  return true;
}

bool RecordDeclWrapper::SetupTemplateInfo(
    abi_dump::RecordDecl *record_declp) const {
 const clang::CXXRecordDecl *cxx_record_decl =
      clang::dyn_cast<clang::CXXRecordDecl>(record_decl_);
  if (!cxx_record_decl) {
    return true;
  }

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

void RecordDeclWrapper::SetupRecordInfo(abi_dump::RecordDecl *record_declp,
                                        const std::string &source_file) const {
  std::string qualified_name = GetTagDeclQualifiedName(record_decl_);
  std::string mangled_name = GetMangledNameDecl(record_decl_);
  std::string linker_key = (mangled_name == "") ? qualified_name : mangled_name;
  record_declp->set_fully_qualified_name(qualified_name);
  record_declp->set_mangled_record_name(mangled_name);
  record_declp->set_linker_set_key(linker_key);
  record_declp->set_source_file(source_file);
  record_declp->set_access(AccessToString(record_decl_->getAccess()));
}

std::unique_ptr<abi_dump::RecordDecl> RecordDeclWrapper::GetRecordDecl() const {
  std::unique_ptr<abi_dump::RecordDecl> abi_decl(new abi_dump::RecordDecl());
  std::string source_file = GetDeclSourceFile(record_decl_);
  SetupRecordInfo(abi_decl.get(), source_file);
  if (!SetupRecordFields(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up Record Fields failed\n";
    return nullptr;
  }

  if (!SetupCXXBases(abi_decl.get()) || !SetupTemplateInfo(abi_decl.get())) {
    llvm::errs() << "Setting up CXX Bases / Template Info failed\n";
    return nullptr;
  }
  return abi_decl;
}

EnumDeclWrapper::EnumDeclWrapper(
    clang::MangleContext *mangle_contextp,
    const clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const clang::EnumDecl *decl)
  : ABIWrapper(mangle_contextp, ast_contextp, compiler_instance_p),
    enum_decl_(decl) { }

bool EnumDeclWrapper::SetupEnum(abi_dump::EnumDecl *enump,
                                const std::string &source_file) const {
  // Enum's name.
  std::string enum_name = GetTagDeclQualifiedName(enum_decl_);
  std::string enum_type = QualTypeToString(enum_decl_->getIntegerType());
  enump->set_enum_name(enum_name);
  // Enum's base integer type.
  enump->set_enum_type(enum_type);
  enump->set_linker_set_key(enum_name + enum_type);
  clang::EnumDecl::enumerator_iterator enum_it = enum_decl_->enumerator_begin();
  while (enum_it != enum_decl_->enumerator_end()) {
    abi_dump::EnumFieldDecl *enum_fieldp = enump->add_enum_fields();
    if (!enum_fieldp) {
      return false;
    }
    enum_fieldp->set_enum_field_name(enum_it->getQualifiedNameAsString());
    enum_fieldp->set_enum_field_value(enum_it->getInitVal().getExtValue());
    enum_it++;
  }
  return true;
}

std::unique_ptr<abi_dump::EnumDecl> EnumDeclWrapper::GetEnumDecl() const {
  std::unique_ptr<abi_dump::EnumDecl> abi_decl(new abi_dump::EnumDecl());
  std::string source_file = GetDeclSourceFile(enum_decl_);

  if (!SetupEnum(abi_decl.get(), source_file)) {
    llvm::errs() << "Setting up Enum fields failed\n";
    return nullptr;
  }
  return abi_decl;
}

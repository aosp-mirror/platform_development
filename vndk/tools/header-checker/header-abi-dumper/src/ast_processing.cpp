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

#include "ast_processing.h"

#include <clang/Lex/Token.h>
#include <clang/Tooling/Core/QualTypeNames.h>

#include <google/protobuf/text_format.h>

#include <fstream>
#include <iostream>
#include <string>

HeaderASTVisitor::HeaderASTVisitor(
    abi_dump::TranslationUnit *tu_ptr,
    clang::MangleContext *mangle_contextp,
    const clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p)
  : tu_ptr_(tu_ptr),
    mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp),
    cip_(compiler_instance_p) { }

bool HeaderASTVisitor::VisitRecordDecl(const clang::RecordDecl *decl) {
  abi_dump::RecordDecl *record_decl = tu_ptr_->add_classes();
  SetupClassFields(record_decl, decl);
  return true;
}

bool HeaderASTVisitor::VisitFunctionDecl(const clang::FunctionDecl *decl) {
  abi_dump::FunctionDecl *function_decl = tu_ptr_->add_functions();
  // FIXME: Use return value.
  SetupFunction(function_decl, decl);
  return true;
}

HeaderASTConsumer::HeaderASTConsumer(
    const std::string &file_name,
    clang::CompilerInstance *compiler_instancep,
    const std::string &out_dump_name)
  : file_name_(file_name),
    cip_(compiler_instancep),
    out_dump_name_(out_dump_name) { }

void HeaderASTConsumer::HandleTranslationUnit(clang::ASTContext &ctx) {
  clang::TranslationUnitDecl* translation_unit = ctx.getTranslationUnitDecl();
  std::unique_ptr<clang::MangleContext> mangle_contextp(
      ctx.createMangleContext());
  abi_dump::TranslationUnit tu;
  HeaderASTVisitor v(&tu, mangle_contextp.get(), &ctx, cip_);
  v.TraverseDecl(translation_unit);
  std::ofstream text_output(out_dump_name_ + ".txt");
  std::fstream binary_output(
      (out_dump_name_).c_str(),
      std::ios::out | std::ios::trunc | std::ios::binary);
  std::string str_out;
  google::protobuf::TextFormat::PrintToString(tu, &str_out);
  text_output << str_out;
  if (!tu.SerializeToOstream(&binary_output)) {
    llvm::errs() << "Serialization to ostream failed\n";
  }
}

void HeaderASTConsumer::HandleVTable(clang::CXXRecordDecl *crd) {
  llvm::errs() << "HandleVTable: " << crd->getName() << "\n";
}

std::string HeaderASTVisitor::GetDeclSourceFile(const clang::NamedDecl *decl) {
  clang::SourceManager &SM = cip_->getSourceManager();
  clang::SourceLocation location = decl->getLocation();
  llvm::StringRef file_name= SM.getFilename(location);
  return file_name.str();
}

std::string HeaderASTVisitor::GetMangledNameDecl(const clang::NamedDecl *decl) {
  std::string mangled_or_demangled_name = decl->getName();
  if (mangle_contextp_->shouldMangleDeclName(decl)) {
    llvm::raw_string_ostream ostream(mangled_or_demangled_name);
    mangle_contextp_->mangleName(decl, ostream);
    ostream.flush();
  }
  return mangled_or_demangled_name;
}

bool HeaderASTVisitor::SetupFunction(abi_dump::FunctionDecl *functionp,
                                     const clang::FunctionDecl *decl) {
  // Go through all the parameters in the method and add them to the fields.
  // Also get the fully qualfied name and mangled name and store them.
  functionp->set_function_name(decl->getQualifiedNameAsString());
  functionp->set_mangled_function_name(GetMangledNameDecl(decl));
  functionp->set_source_file(GetDeclSourceFile(decl));
  clang::QualType return_type =
      decl->getReturnType().getDesugaredType(*ast_contextp_);
  functionp->set_return_type(
      clang::TypeName::getFullyQualifiedName(return_type, *ast_contextp_));
  clang::FunctionDecl::param_const_iterator param_it = decl->param_begin();
  while (param_it != decl->param_end()) {
    abi_dump::FieldDecl *function_fieldp = functionp->add_parameters();
    if (!function_fieldp) {
      llvm::errs() << "Couldn't add parameter to method. Aborting";
      return false;
    }
    function_fieldp->set_field_name((*param_it)->getName());
    clang::QualType field_type =
      (*param_it)->getType().getDesugaredType(*ast_contextp_);

    function_fieldp->set_field_type(
        clang::TypeName::getFullyQualifiedName(field_type, *ast_contextp_));

    param_it++;
  }
  return true;
}

bool HeaderASTVisitor::SetupClassFields(abi_dump::RecordDecl *classp,
                                        const clang::RecordDecl *decl) {
  classp->set_fully_qualified_name(decl->getQualifiedNameAsString());
  classp->set_source_file(GetDeclSourceFile(decl));
  classp->set_entity_type("class");
  clang::RecordDecl::field_iterator field = decl->field_begin();
  while (field != decl->field_end()) {
    abi_dump::FieldDecl *class_fieldp = classp->add_fields();
    if (!class_fieldp) {
      llvm::errs() << " Couldn't add class field: " << field->getName()
                   << " to reference dump\n";
      return false;
    }
    class_fieldp->set_field_name(field->getName());
    //FIXME: This needs to change. Resolve typedef, class name, built-in etc.
    clang::QualType field_type =
        field->getType().getDesugaredType(*ast_contextp_);
    class_fieldp->set_field_type(
        clang::TypeName::getFullyQualifiedName(field_type, *ast_contextp_));
    field++;
  }
  return true;
}

void HeaderASTPPCallbacks::MacroDefined(const clang::Token &macro_name_tok,
                                        const clang::MacroDirective *) {
  assert(macro_name_tok.isAnyIdentifier());
}

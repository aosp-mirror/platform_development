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
#include "abi_wrappers.h"

#include <clang/Lex/Token.h>
#include <clang/Tooling/Core/QualTypeNames.h>

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <fstream>
#include <iostream>
#include <string>

using abi_wrapper::ABIWrapper;
using abi_wrapper::FunctionDeclWrapper;
using abi_wrapper::RecordDeclWrapper;
using abi_wrapper::EnumDeclWrapper;
using abi_wrapper::GlobalVarDeclWrapper;

HeaderASTVisitor::HeaderASTVisitor(
    abi_dump::TranslationUnit *tu_ptr,
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const std::string &current_file_name,
    const std::set<std::string> &exported_headers,
    const clang::Decl *tu_decl)
  : tu_ptr_(tu_ptr),
    mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp),
    cip_(compiler_instance_p),
    current_file_name_(current_file_name),
    exported_headers_(exported_headers),
    tu_decl_(tu_decl) { }

bool HeaderASTVisitor::VisitRecordDecl(const clang::RecordDecl *decl) {
  // Skip forward declaration.
  if (!decl->isThisDeclarationADefinition()) {
    return true;
  }
  RecordDeclWrapper record_decl_wrapper(
      mangle_contextp_, ast_contextp_, cip_, decl);
  std::unique_ptr<abi_dump::RecordDecl> wrapped_record_decl =
      record_decl_wrapper.GetRecordDecl();
  if (!wrapped_record_decl) {
    llvm::errs() << "Getting Record Decl failed\n";
    return false;
  }
  abi_dump::RecordDecl *added_record_declp = tu_ptr_->add_records();
  if (!added_record_declp) {
    return false;
  }
  *added_record_declp = *wrapped_record_decl;
  return true;
}

bool HeaderASTVisitor::VisitEnumDecl(const clang::EnumDecl *decl) {
  if (!decl->isThisDeclarationADefinition()) {
    return true;
  }
  EnumDeclWrapper enum_decl_wrapper(
      mangle_contextp_, ast_contextp_, cip_, decl);
  std::unique_ptr<abi_dump::EnumDecl> wrapped_enum_decl =
      enum_decl_wrapper.GetEnumDecl();
  if (!wrapped_enum_decl) {
    llvm::errs() << "Getting Enum Decl failed\n";
    return false;
  }
  abi_dump::EnumDecl *added_enum_declp = tu_ptr_->add_enums();
  if (!added_enum_declp) {
    return false;
  }
  *added_enum_declp = *wrapped_enum_decl;
  return true;
}

bool HeaderASTVisitor::VisitFunctionDecl(const clang::FunctionDecl *decl) {
  FunctionDeclWrapper function_decl_wrapper(mangle_contextp_, ast_contextp_,
                                            cip_, decl);
  std::unique_ptr<abi_dump::FunctionDecl> wrapped_function_decl =
      function_decl_wrapper.GetFunctionDecl();
  if (!wrapped_function_decl) {
    llvm::errs() << "Getting Function Decl failed\n";
    return false;
  }
  abi_dump::FunctionDecl *added_function_declp = tu_ptr_->add_functions();
  if (!added_function_declp) {
    return false;
  }
  *added_function_declp = *wrapped_function_decl;
  return true;
}

bool HeaderASTVisitor::VisitVarDecl(const clang::VarDecl *decl) {
  if(!decl->hasGlobalStorage()) {
    // Non global / static variable declarations don't need to be dumped.
    return true;
  }
  GlobalVarDeclWrapper global_var_decl_wrapper(mangle_contextp_, ast_contextp_,
                                               cip_, decl);
  std::unique_ptr<abi_dump::GlobalVarDecl> wrapped_global_var_decl =
      global_var_decl_wrapper.GetGlobalVarDecl();
  if (!wrapped_global_var_decl) {
    llvm::errs() << "Getting Global Var Decl failed\n";
    return false;
  }
  abi_dump::GlobalVarDecl *added_global_var_declp = tu_ptr_->add_global_vars();
  if (!added_global_var_declp) {
    return false;
  }
  *added_global_var_declp = *wrapped_global_var_decl;
  return true;
}

// We don't need to recurse into Declarations which are not exported.
bool HeaderASTVisitor::TraverseDecl(clang::Decl *decl) {
  if (!decl) {
    return true;
  }
  std::string source_file = ABIWrapper::GetDeclSourceFile(decl, cip_);
  if ((decl != tu_decl_) &&
      (exported_headers_.find(source_file) == exported_headers_.end())) {
    return true;
  }
  return RecursiveASTVisitor<HeaderASTVisitor>::TraverseDecl(decl);
}

HeaderASTConsumer::HeaderASTConsumer(
    const std::string &file_name,
    clang::CompilerInstance *compiler_instancep,
    const std::string &out_dump_name,
    const std::set<std::string> &exported_headers)
  : file_name_(file_name),
    cip_(compiler_instancep),
    out_dump_name_(out_dump_name),
    exported_headers_(exported_headers) { }

void HeaderASTConsumer::HandleTranslationUnit(clang::ASTContext &ctx) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  std::ofstream text_output(out_dump_name_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  clang::TranslationUnitDecl *translation_unit = ctx.getTranslationUnitDecl();
  std::unique_ptr<clang::MangleContext> mangle_contextp(
      ctx.createMangleContext());
  abi_dump::TranslationUnit tu;
  std::string str_out;
  HeaderASTVisitor v(&tu, mangle_contextp.get(), &ctx, cip_, file_name_,
                     exported_headers_, translation_unit);
  if (!v.TraverseDecl(translation_unit) ||
      !google::protobuf::TextFormat::Print(tu, &text_os)) {
    llvm::errs() << "Serialization to ostream failed\n";
    ::exit(1);
  }
}

void HeaderASTConsumer::HandleVTable(clang::CXXRecordDecl *crd) {
  llvm::errs() << "HandleVTable: " << crd->getName() << "\n";
}

void HeaderASTPPCallbacks::MacroDefined(const clang::Token &macro_name_tok,
                                        const clang::MacroDirective *) {
  assert(macro_name_tok.getLength() != 0);
}

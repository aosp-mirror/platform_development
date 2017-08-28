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
#include <clang/Index/CodegenNameGenerator.h>

#include <fstream>
#include <iostream>
#include <string>

using abi_wrapper::ABIWrapper;
using abi_wrapper::FunctionDeclWrapper;
using abi_wrapper::RecordDeclWrapper;
using abi_wrapper::EnumDeclWrapper;
using abi_wrapper::GlobalVarDeclWrapper;

HeaderASTVisitor::HeaderASTVisitor(
    clang::MangleContext *mangle_contextp,
    clang::ASTContext *ast_contextp,
    const clang::CompilerInstance *compiler_instance_p,
    const std::string &current_file_name,
    const std::set<std::string> &exported_headers,
    const clang::Decl *tu_decl,
    std::set<std::string> *type_cache,
    abi_util::IRDumper *ir_dumper)
  : mangle_contextp_(mangle_contextp),
    ast_contextp_(ast_contextp),
    cip_(compiler_instance_p),
    current_file_name_(current_file_name),
    exported_headers_(exported_headers),
    tu_decl_(tu_decl),
    type_cache_(type_cache),
    ir_dumper_(ir_dumper) { }

bool HeaderASTVisitor::VisitRecordDecl(const clang::RecordDecl *decl) {
  // Skip forward declarations, dependent records. Also skip anonymous records
  // as they will be traversed through record fields.
  if (!decl->isThisDeclarationADefinition() ||
      decl->getTypeForDecl()->isDependentType() ||
      decl->isAnonymousStructOrUnion() ||
      !decl->hasNameForLinkage()) {
    return true;
  }
  RecordDeclWrapper record_decl_wrapper(
      mangle_contextp_, ast_contextp_, cip_, decl, type_cache_,
      ir_dumper_, decl_to_source_file_cache_, "");
  return record_decl_wrapper.GetRecordDecl();
}

bool HeaderASTVisitor::VisitEnumDecl(const clang::EnumDecl *decl) {
  if (!decl->isThisDeclarationADefinition() ||
      decl->getTypeForDecl()->isDependentType() ||
      !decl->hasNameForLinkage()) {
    return true;
  }
  EnumDeclWrapper enum_decl_wrapper(
      mangle_contextp_, ast_contextp_, cip_, decl, type_cache_,
      ir_dumper_, decl_to_source_file_cache_);
  return enum_decl_wrapper.GetEnumDecl();
 }

static bool MutateFunctionWithLinkageName(const abi_util::FunctionIR *function,
                                          abi_util::IRDumper *ir_dumper,
                                          std::string &linkage_name) {
  auto added_function = std::make_unique<abi_util::FunctionIR>();
  *added_function = *function;
  added_function->SetLinkerSetKey(linkage_name);
  return ir_dumper->AddLinkableMessageIR(added_function.get());
}

static bool AddMangledFunctions(const abi_util::FunctionIR *function,
                                abi_util:: IRDumper *ir_dumper,
                                std::vector<std::string> &manglings) {
  for (auto &&mangling : manglings) {
    if (!MutateFunctionWithLinkageName(function, ir_dumper, mangling)) {
      return false;
    }
  }
  return true;
}

static bool ShouldSkipFunctionDecl(const clang::FunctionDecl *decl) {
  if (const clang::CXXMethodDecl *method_decl =
      llvm::dyn_cast<clang::CXXMethodDecl>(decl)) {
    if (method_decl->getParent()->getTypeForDecl()->isDependentType()) {
      return true;
    }
  }
  clang::FunctionDecl::TemplatedKind tkind = decl->getTemplatedKind();
  switch (tkind) {
    case clang::FunctionDecl::TK_NonTemplate:
    case clang::FunctionDecl::TK_FunctionTemplateSpecialization:
    case clang::FunctionDecl::TK_MemberSpecialization:
      return false;
    default:
      return true;
  }
}

bool HeaderASTVisitor::VisitFunctionDecl(const clang::FunctionDecl *decl) {
  if (ShouldSkipFunctionDecl(decl)) {
    return true;
  }
  FunctionDeclWrapper function_decl_wrapper(mangle_contextp_, ast_contextp_,
                                            cip_, decl, type_cache_,
                                            ir_dumper_,
                                            decl_to_source_file_cache_);
  auto function_wrapper = function_decl_wrapper.GetFunctionDecl();
  // Destructors and Constructors can have more than 1 symbol generated from the
  // same Decl.
  clang::index::CodegenNameGenerator cg(*ast_contextp_);
  std::vector<std::string> manglings = cg.getAllManglings(decl);
  if (!manglings.empty()) {
    return AddMangledFunctions(function_wrapper.get(), ir_dumper_, manglings);
  }
  std::string linkage_name =
      ABIWrapper::GetMangledNameDecl(decl, mangle_contextp_);
  return MutateFunctionWithLinkageName(function_wrapper.get(), ir_dumper_,
                                       linkage_name);
}

bool HeaderASTVisitor::VisitVarDecl(const clang::VarDecl *decl) {
  if(!decl->hasGlobalStorage()||
     decl->getType().getTypePtr()->isDependentType()) {
    // Non global / static variable declarations don't need to be dumped.
    return true;
  }
  GlobalVarDeclWrapper global_var_decl_wrapper(mangle_contextp_, ast_contextp_,
                                               cip_, decl, type_cache_,
                                               ir_dumper_,
                                               decl_to_source_file_cache_);
  return  global_var_decl_wrapper.GetGlobalVarDecl();
}

static bool AreHeadersExported(const std::set<std::string> &exported_headers) {
  return !exported_headers.empty();
}

// We don't need to recurse into Declarations which are not exported.
bool HeaderASTVisitor::TraverseDecl(clang::Decl *decl) {
  if (!decl) {
    return true;
  }
  std::string source_file = ABIWrapper::GetDeclSourceFile(decl, cip_);
  decl_to_source_file_cache_.insert(std::make_pair(decl, source_file));
  // If no exported headers are specified we assume the whole AST is exported.
  if ((decl != tu_decl_) && AreHeadersExported(exported_headers_) &&
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
  clang::PrintingPolicy policy(ctx.getPrintingPolicy());
  // Suppress 'struct' keyword for C source files while getting QualType string
  // names to avoid inconsistency between C and C++ (for C++ files, this is true
  // by default)
  policy.SuppressTagKeyword = true;
  ctx.setPrintingPolicy(policy);
  clang::TranslationUnitDecl *translation_unit = ctx.getTranslationUnitDecl();
  std::unique_ptr<clang::MangleContext> mangle_contextp(
      ctx.createMangleContext());
  std::set<std::string> type_cache;
  std::unique_ptr<abi_util::IRDumper> ir_dumper =
      abi_util::IRDumper::CreateIRDumper("protobuf", out_dump_name_);
  HeaderASTVisitor v(mangle_contextp.get(), &ctx, cip_, file_name_,
                     exported_headers_, translation_unit, &type_cache,
                     ir_dumper.get());
  if (!v.TraverseDecl(translation_unit) || !ir_dumper->Dump()) {
    llvm::errs() << "Serialization to ostream failed\n";
    ::exit(1);
  }
}

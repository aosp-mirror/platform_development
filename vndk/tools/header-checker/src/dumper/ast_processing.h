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

#ifndef AST_PROCESSING_H_
#define AST_PROCESSING_H_

#include "dumper/ast_util.h"
#include "dumper/header_checker.h"

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Mangle.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Lex/PPCallbacks.h>

#include <set>


namespace header_checker {
namespace dumper {


class HeaderASTVisitor
    : public clang::RecursiveASTVisitor<HeaderASTVisitor> {
 public:
  HeaderASTVisitor(const HeaderCheckerOptions &options,
                   clang::MangleContext *mangle_contextp,
                   clang::ASTContext *ast_contextp,
                   const clang::CompilerInstance *compiler_instance_p,
                   const clang::Decl *tu_decl,
                   repr::ModuleIR *module,
                   ASTCaches *ast_caches);

  bool VisitRecordDecl(const clang::RecordDecl *decl);

  bool VisitFunctionDecl(const clang::FunctionDecl *decl);

  bool VisitEnumDecl(const clang::EnumDecl *decl);

  bool VisitVarDecl(const clang::VarDecl *decl);

  bool TraverseDecl(clang::Decl *decl);

  // Enable recursive traversal of template instantiations.
  bool shouldVisitTemplateInstantiations() const { return true; }

 private:
  bool ShouldSkipFunctionDecl(const clang::FunctionDecl *decl);

  const HeaderCheckerOptions &options_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
  const clang::CompilerInstance *cip_;
  // To optimize recursion into only exported abi.
  const clang::Decl *tu_decl_;
  repr::ModuleIR *module_;
  // We cache the source file an AST node corresponds to, to avoid repeated
  // calls to "realpath".
  ASTCaches *ast_caches_;
};


class HeaderASTConsumer : public clang::ASTConsumer {
 public:
  HeaderASTConsumer(clang::CompilerInstance *compiler_instancep,
                    HeaderCheckerOptions &options);

  void HandleTranslationUnit(clang::ASTContext &ctx) override;

 private:
  clang::CompilerInstance *cip_;
  HeaderCheckerOptions &options_;
};


}  // namespace dumper
}  // namespace header_checker


#endif  // AST_PROCESSING_H_

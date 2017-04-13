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

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#pragma clang diagnostic pop

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Mangle.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Lex/PPCallbacks.h>

#include <set>

class HeaderASTVisitor
    : public clang::RecursiveASTVisitor<HeaderASTVisitor> {
 public:
  HeaderASTVisitor(abi_dump::TranslationUnit *tu_ptr,
                   clang::MangleContext *mangle_contextp,
                   clang::ASTContext *ast_contextp,
                   const clang::CompilerInstance *compiler_instance_p,
                   const std::string &current_file_name,
                   const std::set<std::string> &exported_headers,
                   const clang::Decl *tu_decl);

  bool VisitRecordDecl(const clang::RecordDecl *decl);

  bool VisitFunctionDecl(const clang::FunctionDecl *decl);

  bool VisitEnumDecl(const clang::EnumDecl *decl);

  bool VisitVarDecl(const clang::VarDecl *decl);

  bool TraverseDecl(clang::Decl *decl);

  // Enable recursive traversal of template instantiations.
  bool shouldVisitTemplateInstantiations() const {
    return true;
  }

 private:
  abi_dump::TranslationUnit *tu_ptr_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
  const clang::CompilerInstance *cip_;
  const std::string current_file_name_;
  const std::set<std::string> &exported_headers_;
  // To optimize recursion into only exported abi.
  const clang::Decl *tu_decl_;
};

class HeaderASTConsumer : public clang::ASTConsumer {
 public:
  HeaderASTConsumer(const std::string &file_name,
                    clang::CompilerInstance *compiler_instancep,
                    const std::string &out_dump_name,
                    const std::set<std::string> &exported_headers);

  void HandleTranslationUnit(clang::ASTContext &ctx) override;

 private:
  std::string file_name_;
  clang::CompilerInstance *cip_;
  std::string out_dump_name_;
  std::set<std::string> exported_headers_;
};

class HeaderASTPPCallbacks : public clang::PPCallbacks {
 public:
  void MacroDefined(const clang::Token &macro_name_tok,
                    const clang::MacroDirective *) override;
};

#endif  // AST_PROCESSING_H_

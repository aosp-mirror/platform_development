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

#include "frontend_action.h"

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/MultiplexConsumer.h>
#include <clang/Lex/PPCallbacks.h>
#include <clang/Lex/Preprocessor.h>
#include <clang/Lex/Token.h>
#include <clang/Serialization/ASTWriter.h>
#include <llvm/ADT/STLExtras.h>
#include <llvm/Support/raw_ostream.h>

#include <memory>
#include <string>

static constexpr bool kLoadRefAsImplicitPCH = false;

class HeaderCheckVisitor
    : public clang::RecursiveASTVisitor<HeaderCheckVisitor> {
 public:
  bool VisitRecordDecl(const clang::RecordDecl *decl) {
    llvm::errs() << "struct: " << decl->getName() << "\n";
    return true;
  }

  bool VisitCXXRecordDecl(const clang::CXXRecordDecl *decl) {
    llvm::errs() << "class: " << decl->getName() << "\n";
    return true;
  }

  bool VisitFunctionDecl(const clang::FunctionDecl *decl) {
    llvm::errs() << "func: " << decl->getName() << "\n";
    return true;
  }
};

class HeaderCheckerConsumer : public clang::ASTConsumer {
 public:
  void HandleTranslationUnit(clang::ASTContext &ctx) override {
    llvm::errs() << "HandleTranslationUnit ------------------------------\n";
    clang::TranslationUnitDecl* translation_unit = ctx.getTranslationUnitDecl();
    HeaderCheckVisitor v;
    v.TraverseDecl(translation_unit);
  }

  void HandleVTable(clang::CXXRecordDecl *crd) override {
    llvm::errs() << "HandleVTable: " << crd->getName() << "\n";
  }
};

class HeaderCheckerPPCallbacks : public clang::PPCallbacks {
 private:
   llvm::StringRef ToString(const clang::Token &tok) {
    return tok.getIdentifierInfo()->getName();
  }

 public:
  void MacroDefined(const clang::Token &macro_name_tok,
                    const clang::MacroDirective *) override {
    assert(macro_name_tok.isAnyIdentifier());
    llvm::errs() << "defines: " << ToString(macro_name_tok) << "\n";
  }
};

HeaderCheckerFrontendAction::HeaderCheckerFrontendAction(
    const std::string &ref_dump_name, bool should_generate_ref_dump)
  : ref_dump_name_(ref_dump_name),
    should_generate_ref_dump_(should_generate_ref_dump) { }

static bool VisitRefDumpDecls(void *ctx, const clang::Decl *decl) {
  HeaderCheckVisitor v;
  v.TraverseDecl(const_cast<clang::Decl *>(decl));
  return true;
}

bool HeaderCheckerFrontendAction::BeginSourceFileAction(
    clang::CompilerInstance &ci, llvm::StringRef header_file) {

  // Load reference dump file.
  if (llvm::sys::fs::exists(ref_dump_name_)) {
    if (kLoadRefAsImplicitPCH) {
      ci.getPreprocessorOpts().ImplicitPCHInclude = ref_dump_name_;
    } else {
      clang::DiagnosticsEngine &diag = ci.getDiagnostics();

      diag.getClient()->BeginSourceFile(ci.getLangOpts(),
                                        &ci.getPreprocessor());

      // FIXME: Must replace getPCHContainerReader() with other ASTReader.
      ref_dump_ = clang::ASTUnit::LoadFromASTFile(
          ref_dump_name_, ci.getPCHContainerReader(), &diag,
          ci.getFileSystemOpts(), ci.getCodeGenOpts().DebugTypeExtRefs);

      diag.getClient()->EndSourceFile();

      if (ref_dump_) {
        llvm::errs() << "Loaded: " << ref_dump_name_ << " : "
                     << ref_dump_->top_level_size() << "\n";

        ref_dump_->visitLocalTopLevelDecls(nullptr, VisitRefDumpDecls);
        llvm::errs() << "----------------------------------------\n";
      }
    }
  }
  return true;
}

void HeaderCheckerFrontendAction::EndSourceFileAction() {
  ref_dump_.reset();
}

std::unique_ptr<clang::ASTConsumer>
HeaderCheckerFrontendAction::CreateASTConsumer(clang::CompilerInstance &ci,
                                               llvm::StringRef header_file) {
  // Add preprocessor callbacks.
  clang::Preprocessor &pp = ci.getPreprocessor();
  pp.addPPCallbacks(llvm::make_unique<HeaderCheckerPPCallbacks>());

  // Create AST consumers.
  std::vector<std::unique_ptr<clang::ASTConsumer>> consumers;
  consumers.push_back(llvm::make_unique<HeaderCheckerConsumer>());

  if (should_generate_ref_dump_) {
    std::string sysroot;
    llvm::raw_pwrite_stream *ref_dump_os = ci.createOutputFile(
        ref_dump_name_, true, false, header_file, "", true);
    if (!ref_dump_os) {
      llvm::errs() << "ERROR: Failed to create reference dump file: "
                   << ref_dump_name_ << "\n";
      return nullptr;
    }

    auto buffer = std::make_shared<clang::PCHBuffer>();
    consumers.push_back(
        llvm::make_unique<clang::PCHGenerator>(
            ci.getPreprocessor(), ref_dump_name_, nullptr, "",  buffer,
            ci.getFrontendOpts().ModuleFileExtensions, false, false));
    consumers.push_back(
        ci.getPCHContainerWriter().CreatePCHContainerGenerator(
            ci, header_file, ref_dump_name_, ref_dump_os, buffer));
  }

  return llvm::make_unique<clang::MultiplexConsumer>(std::move(consumers));
}

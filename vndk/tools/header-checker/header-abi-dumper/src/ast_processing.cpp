#include "ast_processing.h"

bool HeaderASTVisitor::VisitRecordDecl(const clang::RecordDecl *decl) {
  llvm::errs() << "struct: " << decl->getName() << "\n";
  return true;
}

bool HeaderASTVisitor::VisitCXXRecordDecl(const clang::CXXRecordDecl *decl) {
  llvm::errs() << "class: " << decl->getName() << "\n";
  return true;
}

bool HeaderASTVisitor::VisitFunctionDecl(const clang::FunctionDecl *decl) {
  llvm::errs() << "func: " << decl->getName() << "\n";
  return true;
}

void HeaderASTConsumer::HandleTranslationUnit(clang::ASTContext &ctx) {
  llvm::errs() << "HandleTranslationUnit ------------------------------\n";
  clang::TranslationUnitDecl* translation_unit = ctx.getTranslationUnitDecl();
  HeaderASTVisitor v;
  v.TraverseDecl(translation_unit);
}

void HeaderASTConsumer::HandleVTable(clang::CXXRecordDecl *crd) {
  llvm::errs() << "HandleVTable: " << crd->getName() << "\n";
}

llvm::StringRef HeaderASTPPCallbacks::ToString(const clang::Token &tok) {
  return tok.getIdentifierInfo()->getName();
}

void HeaderASTPPCallbacks::MacroDefined(const clang::Token &macro_name_tok,
    const clang::MacroDirective *) {
  assert(macro_name_tok.isAnyIdentifier());
  llvm::errs() << "defines: " << ToString(macro_name_tok) << "\n";
}

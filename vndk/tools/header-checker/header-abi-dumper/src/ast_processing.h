#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Lex/PPCallbacks.h>
#include <clang/Lex/Preprocessor.h>

class HeaderASTVisitor
    : public clang::RecursiveASTVisitor<HeaderASTVisitor> {
 public:
  bool VisitRecordDecl(const clang::RecordDecl *decl);
  bool VisitCXXRecordDecl(const clang::CXXRecordDecl *decl);
  bool VisitFunctionDecl(const clang::FunctionDecl *decl);
};

class HeaderASTConsumer : public clang::ASTConsumer {
 public:
  void HandleTranslationUnit(clang::ASTContext &ctx) override;
  void HandleVTable(clang::CXXRecordDecl *crd) override;
};

class HeaderASTPPCallbacks : public clang::PPCallbacks {
 private:
   llvm::StringRef ToString(const clang::Token &tok);

 public:
  void MacroDefined(const clang::Token &macro_name_tok,
                    const clang::MacroDirective *) override;
};

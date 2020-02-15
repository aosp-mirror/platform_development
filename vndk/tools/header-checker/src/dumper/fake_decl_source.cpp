// Copyright (C) 2018 The Android Open Source Project
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

#include "dumper/fake_decl_source.h"

#include <clang/Lex/Preprocessor.h>
#include <clang/Sema/Lookup.h>


namespace header_checker {
namespace dumper {


FakeDeclSource::FakeDeclSource(const clang::CompilerInstance &ci) : ci_(ci) {}

clang::CXXRecordDecl *
FakeDeclSource::CreateCXXRecordDecl(const clang::DeclarationName &name,
                                    clang::DeclContext *decl_context) {
  clang::CXXRecordDecl *cxx_record_decl = clang::CXXRecordDecl::Create(
      ci_.getASTContext(), clang::TTK_Struct, decl_context,
      clang::SourceLocation(), clang::SourceLocation(),
      name.getAsIdentifierInfo(), /* PrevDecl */ nullptr);
  cxx_record_decl->setInvalidDecl(true);

  return cxx_record_decl;
}

clang::ClassTemplateDecl *
FakeDeclSource::CreateClassTemplateDecl(clang::CXXRecordDecl *cxx_record_decl,
                                        clang::DeclContext *decl_context) {
  clang::ASTContext &ast = ci_.getASTContext();

  // Declare `template<typename ...T> struct RecordName` in decl_context.
  clang::TemplateTypeParmDecl *parm = clang::TemplateTypeParmDecl::Create(
      ast, decl_context, clang::SourceLocation(), clang::SourceLocation(),
      /* Depth */ 0, /* Position */ 0, /* Id */ nullptr,
      /* Typename */ true, /* ParameterPack */ true);
  parm->setInvalidDecl(true);

  clang::NamedDecl *parm_array[1] = {parm};
  clang::TemplateParameterList *parm_list =
      clang::TemplateParameterList::Create(
          ast, clang::SourceLocation(), clang::SourceLocation(), parm_array,
          clang::SourceLocation(), /* RequiresClause */ nullptr);

  clang::ClassTemplateDecl *class_template_decl =
      clang::ClassTemplateDecl::Create(
          ast, decl_context, clang::SourceLocation(),
          cxx_record_decl->getDeclName(), parm_list, cxx_record_decl);

  cxx_record_decl->setDescribedClassTemplate(class_template_decl);
  class_template_decl->setInvalidDecl(true);

  return class_template_decl;
}

clang::NamespaceDecl *
FakeDeclSource::CreateNamespaceDecl(const clang::DeclarationName &name,
                                    clang::DeclContext *decl_context) {
  clang::NamespaceDecl *namespace_decl = clang::NamespaceDecl::Create(
      ci_.getASTContext(), decl_context, /* Inline */ false,
      clang::SourceLocation(), clang::SourceLocation(),
      name.getAsIdentifierInfo(), /* PrevDecl */ nullptr);
  namespace_decl->setInvalidDecl(true);

  return namespace_decl;
}

clang::NamedDecl *
FakeDeclSource::CreateDecl(clang::Sema::LookupNameKind kind,
                           const clang::DeclarationNameInfo &name_info,
                           clang::DeclContext *decl_context) {
  const clang::DeclarationName &name = name_info.getName();
  if (name.getNameKind() != clang::DeclarationName::Identifier) {
    return nullptr;
  }

  clang::NamedDecl *decl;
  switch (kind) {
  case clang::Sema::LookupOrdinaryName:
  case clang::Sema::LookupTagName: {
    clang::CXXRecordDecl *cxx_record_decl =
        CreateCXXRecordDecl(name, decl_context);
    // If `<` follows the type name, the type must be a template.
    // Otherwise, the compiler takes it as a syntax error.
    const clang::Token &next_token = ci_.getPreprocessor().LookAhead(0);
    if (next_token.is(clang::tok::less)) {
      decl = CreateClassTemplateDecl(cxx_record_decl, decl_context);
    } else {
      decl = cxx_record_decl;
    }
    break;
  }
  case clang::Sema::LookupNestedNameSpecifierName:
    decl = CreateNamespaceDecl(name, decl_context);
    break;
  default:
    decl = nullptr;
  }

  if (decl) {
    decl_context->addDecl(decl);
  }
  return decl;
}

clang::DeclContext *
FakeDeclSource::ResolveDeclContext(clang::DeclContext *member_context,
                                   clang::Scope *scope,
                                   clang::NestedNameSpecifier *nns) {
  if (member_context) {
    return member_context;
  }

  if (nns) {
    switch (nns->getKind()) {
    case clang::NestedNameSpecifier::Namespace:
      return nns->getAsNamespace();
    case clang::NestedNameSpecifier::NamespaceAlias:
      return nns->getAsNamespaceAlias()->getNamespace();
    case clang::NestedNameSpecifier::TypeSpec:
    case clang::NestedNameSpecifier::TypeSpecWithTemplate:
      return nns->getAsRecordDecl();
    case clang::NestedNameSpecifier::Global:
      return ci_.getASTContext().getTranslationUnitDecl();
    case clang::NestedNameSpecifier::Identifier:
    case clang::NestedNameSpecifier::Super:
      break;
    }
  }

  if (scope && scope->getEntity()) {
    return scope->getEntity();
  }

  return ci_.getASTContext().getTranslationUnitDecl();
}

clang::TypoCorrection FakeDeclSource::CorrectTypo(
    const clang::DeclarationNameInfo &typo, int lookup_kind,
    clang::Scope *scope, clang::CXXScopeSpec *scope_spec,
    clang::CorrectionCandidateCallback &ccc, clang::DeclContext *member_context,
    bool entering_context, const clang::ObjCObjectPointerType *opt) {
  // Skip function bodies.
  if (scope && scope->getFnParent()) {
    return clang::TypoCorrection();
  }

  clang::NestedNameSpecifier *nns = nullptr;
  if (scope_spec && !scope_spec->isEmpty()) {
    nns = scope_spec->getScopeRep();
  }

  clang::DeclContext *decl_context =
      ResolveDeclContext(member_context, scope, nns);

  clang::NamedDecl *decl =
      CreateDecl(clang::Sema::LookupNameKind(lookup_kind), typo, decl_context);
  if (decl == nullptr) {
    return clang::TypoCorrection();
  }

  return clang::TypoCorrection(decl, nns);
}

bool FakeDeclSource::LookupUnqualified(clang::LookupResult &result,
                                       clang::Scope *scope) {
  // The compiler looks for redeclaration when it parses a known name.
  if (result.isForRedeclaration()) {
    return false;
  }
  // Skip function bodies.
  if (scope && scope->getFnParent()) {
    return false;
  }

  clang::DeclContext *decl_context;
  if (scope && scope->getEntity()) {
    decl_context = scope->getEntity();
  } else {
    decl_context = ci_.getASTContext().getTranslationUnitDecl();
  }

  clang::NamedDecl *decl = CreateDecl(result.getLookupKind(),
                                      result.getLookupNameInfo(), decl_context);
  if (decl == nullptr) {
    return false;
  }

  result.addDecl(decl);
  result.resolveKind();
  return true;
}


}  // dumper
}  // header_checker

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

#ifndef HEADER_CHECKER_ABI_DUMPER_FAKE_DECL_SOURCE_H_
#define HEADER_CHECKER_ABI_DUMPER_FAKE_DECL_SOURCE_H_

#include <clang/Frontend/CompilerInstance.h>
#include <clang/Sema/ExternalSemaSource.h>
#include <clang/Sema/Sema.h>


namespace header_checker {
namespace dumper {


// This class creates fake declarations when the compiler queries for unknown
// types.
class FakeDeclSource : public clang::ExternalSemaSource {
 private:
  const clang::CompilerInstance &ci_;

  clang::CXXRecordDecl *CreateCXXRecordDecl(const clang::DeclarationName &name,
                                            clang::DeclContext *decl_context);

  clang::ClassTemplateDecl *
  CreateClassTemplateDecl(clang::CXXRecordDecl *cxx_record_decl,
                          clang::DeclContext *decl_context);

  clang::NamespaceDecl *CreateNamespaceDecl(const clang::DeclarationName &name,
                                            clang::DeclContext *decl_context);

  // This method creates a declaration in decl_context according to the lookup
  // name kind and the declaration name kind. If this method doesn't support the
  // kinds, it returns nullptr.
  clang::NamedDecl *CreateDecl(clang::Sema::LookupNameKind kind,
                               const clang::DeclarationNameInfo &name,
                               clang::DeclContext *decl_context);

  // Return the DeclContext for CorrectTypo to create a declaration in.
  clang::DeclContext *ResolveDeclContext(clang::DeclContext *member_context,
                                         clang::Scope *scope,
                                         clang::NestedNameSpecifier *nns);

 public:
  FakeDeclSource(const clang::CompilerInstance &ci);

  clang::TypoCorrection
  CorrectTypo(const clang::DeclarationNameInfo &typo, int lookup_kind,
              clang::Scope *scope, clang::CXXScopeSpec *scope_spec,
              clang::CorrectionCandidateCallback &ccc,
              clang::DeclContext *member_context, bool entering_context,
              const clang::ObjCObjectPointerType *opt) override;

  bool LookupUnqualified(clang::LookupResult &result,
                         clang::Scope *scope) override;
};


}  // namespace dumper
}  // namespace header_checker


#endif  // HEADER_CHECKER_ABI_DUMPER_FAKE_DECL_SOURCE_H_

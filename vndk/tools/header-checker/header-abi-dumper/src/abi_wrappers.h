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

#ifndef ABI_WRAPPERS_H_
#define ABI_WRAPPERS_H_

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#pragma clang diagnostic pop

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Mangle.h>
#include <clang/AST/VTableBuilder.h>
#include <clang/Frontend/CompilerInstance.h>

namespace abi_wrapper {
class ABIWrapper {
 public:
  ABIWrapper(clang::MangleContext *mangle_contextp,
             clang::ASTContext *ast_contextp,
             const clang::CompilerInstance *cip);

  static std::string GetDeclSourceFile(const clang::Decl *decl,
                                       const clang::CompilerInstance *cip);

  static std::string GetMangledNameDecl(const clang::NamedDecl *decl,
                                        clang::MangleContext *mangle_context);
 protected:
  abi_dump::AccessSpecifier AccessClangToDump(
      const clang::AccessSpecifier sp) const;

  bool SetupTemplateParamNames(abi_dump::TemplateInfo *tinfo,
                               clang::TemplateParameterList *pl) const;

  bool SetupTemplateArguments(abi_dump::TemplateInfo *tinfo,
                              const clang::TemplateArgumentList *tl) const;

  std::string QualTypeToString(const clang::QualType &sweet_qt) const;

  std::string GetTagDeclQualifiedName(const clang::TagDecl *decl) const;

  bool SetupBasicTypeAbi(abi_dump::BasicTypeAbi *type_abi,
                         const clang::QualType type, bool dump_size) const;

  bool SetupBasicNamedAndTypedDecl(
      abi_dump::BasicNamedAndTypedDecl *basic_named_and_typed_decl,
      const clang::QualType type, const std::string &name,
      const clang::AccessSpecifier &access, std::string key,
      bool dump_size) const;

  std::string GetTypeLinkageName(const clang::Type *typep) const;

protected:
  const clang::CompilerInstance *cip_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
};

class RecordDeclWrapper : public ABIWrapper {
 public:
  RecordDeclWrapper(clang::MangleContext *mangle_contextp,
                    clang::ASTContext *ast_contextp,
                    const clang::CompilerInstance *compiler_instance_p,
                    const clang::RecordDecl *decl);

  std::unique_ptr<abi_dump::RecordDecl> GetRecordDecl() const;

 private:
  const clang::RecordDecl *record_decl_;

 private:
  bool SetupRecordInfo(abi_dump::RecordDecl *record_declp,
                       const std::string &source_file) const;

  bool SetupRecordFields(abi_dump::RecordDecl *record_declp) const;

  bool SetupCXXBases(abi_dump::RecordDecl *cxxp,
                     const clang::CXXRecordDecl *cxx_record_decl) const;

  bool SetupTemplateInfo(abi_dump::RecordDecl *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl) const;

  bool SetupRecordVTable(abi_dump::RecordDecl *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl) const;
  bool SetupRecordVTableComponent(
      abi_dump::VTableComponent *added_vtable_component,
      const clang::VTableComponent &vtable_component) const;

  bool SetupCXXRecordInfo(abi_dump::RecordDecl *record_declp) const;
};

class FunctionDeclWrapper : public ABIWrapper {
 public:
  FunctionDeclWrapper(clang::MangleContext *mangle_contextp,
                      clang::ASTContext *ast_contextp,
                      const clang::CompilerInstance *compiler_instance_p,
                      const clang::FunctionDecl *decl);

  std::unique_ptr<abi_dump::FunctionDecl> GetFunctionDecl() const;

 private:
  const clang::FunctionDecl *function_decl_;

 private:
  bool SetupFunction(abi_dump::FunctionDecl *methodp,
                     const std::string &source_file) const;

  bool SetupTemplateInfo(abi_dump::FunctionDecl *functionp) const;

  bool SetupFunctionParameters(abi_dump::FunctionDecl *functionp) const;
};

class EnumDeclWrapper : public ABIWrapper {
 public:
  EnumDeclWrapper(clang::MangleContext *mangle_contextp,
                  clang::ASTContext *ast_contextp,
                  const clang::CompilerInstance *compiler_instance_p,
                  const clang::EnumDecl *decl);

  std::unique_ptr<abi_dump::EnumDecl> GetEnumDecl() const;

 private:
  const clang::EnumDecl *enum_decl_;

 private:
  bool SetupEnum(abi_dump::EnumDecl *enump,
                 const std::string &source_file) const;
  bool SetupEnumFields(abi_dump::EnumDecl *enump) const;
};

class GlobalVarDeclWrapper : public ABIWrapper {
 public:
  GlobalVarDeclWrapper(clang::MangleContext *mangle_contextp,
                  clang::ASTContext *ast_contextp,
                  const clang::CompilerInstance *compiler_instance_p,
                  const clang::VarDecl *decl);

  std::unique_ptr<abi_dump::GlobalVarDecl> GetGlobalVarDecl() const;

 private:
  const clang::VarDecl *global_var_decl_;
 private:
  bool SetupGlobalVar(abi_dump::GlobalVarDecl *global_varp,
                      const std::string &source_file) const;
};

} //end namespace abi_wrapper

#endif  // ABI_WRAPPERS_H_

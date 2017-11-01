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

#include <ir_representation.h>

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
             const clang::CompilerInstance *cip,
             std::set<std::string> *type_cache,
             abi_util::IRDumper *ir_dumper,
             std::map<const clang::Decl *, std::string> &decl_to_source_cache);

  static std::string GetDeclSourceFile(const clang::Decl *decl,
                                       const clang::CompilerInstance *cip);

  static std::string GetMangledNameDecl(const clang::NamedDecl *decl,
                                        clang::MangleContext *mangle_context);
 protected:
  abi_dump::AccessSpecifier AccessClangToDump(
      const clang::AccessSpecifier sp);
  std::string GetCachedDeclSourceFile(const clang::Decl *decl,
                                      const clang::CompilerInstance *cip);

  bool SetupTemplateArguments(const clang::TemplateArgumentList *tl,
                              abi_util::TemplatedArtifactIR *ta,
                              const std::string &source_file);

  std::string QualTypeToString(const clang::QualType &sweet_qt);

  std::string GetTagDeclQualifiedName(const clang::TagDecl *decl);

  bool CreateBasicNamedAndTypedDecl(clang::QualType,
                                    const std::string &source_file);
  bool CreateBasicNamedAndTypedDecl(
      clang::QualType canonical_type,
      abi_util::TypeIR *typep,
      const std::string &source_file);

  bool CreateExtendedType(
      clang::QualType canonical_type,
      abi_util::TypeIR *typep);

  clang::QualType GetReferencedType(const clang::QualType qual_type);

  std::string GetTypeLinkageName(const clang::Type *typep);

  std::unique_ptr<abi_util::TypeIR> SetTypeKind(const clang::QualType qtype,
                                                const std::string &source_file);


 protected:
  const clang::CompilerInstance *cip_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
  std::set<std::string> *type_cache_;
  abi_util::IRDumper *ir_dumper_;
  std::map<const clang::Decl *, std::string> &decl_to_source_file_cache_;
};

class RecordDeclWrapper : public ABIWrapper {
 public:
  RecordDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::RecordDecl *decl, std::set<std::string> *type_cache,
      abi_util::IRDumper *ir_dumper,
      std::map<const clang::Decl *, std::string> &decl_to_source_cache_,
      const std::string &previous_record_stages);

  bool GetRecordDecl();

 private:
  const clang::RecordDecl *record_decl_;
  std::string previous_record_stages_;

 private:
  bool SetupRecordInfo(abi_util::RecordTypeIR *type,
                       const std::string &source_file);

  bool SetupRecordFields(abi_util::RecordTypeIR *record_declp,
                         const std::string &source_file);

  bool SetupCXXBases(abi_util::RecordTypeIR *cxxp,
                     const clang::CXXRecordDecl *cxx_record_decl);

  bool SetupTemplateInfo(abi_util::RecordTypeIR *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl,
                         const std::string &source_file);

  bool SetupRecordVTable(abi_util::RecordTypeIR *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl);
  abi_util::VTableComponentIR SetupRecordVTableComponent(
      const clang::VTableComponent &vtable_component);

  bool SetupCXXRecordInfo(abi_util::RecordTypeIR *record_declp,
                          const std::string &source_file);

  bool CreateAnonymousRecord(
      const clang::RecordDecl *decl, const std::string &linker_set_key);
};

class FunctionDeclWrapper : public ABIWrapper {
 public:
  FunctionDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::FunctionDecl *decl, std::set<std::string> *type_cache,
      abi_util::IRDumper *ir_dumper,
      std::map<const clang::Decl *, std::string> &decl_to_source_cache_);

  std::unique_ptr<abi_util::FunctionIR> GetFunctionDecl();

 private:
  const clang::FunctionDecl *function_decl_;

 private:
  bool SetupFunction(abi_util::FunctionIR *methodp,
                     const std::string &source_file);

  bool SetupTemplateInfo(abi_util::FunctionIR *functionp,
                         const std::string &source_file);

  bool SetupFunctionParameters(abi_util::FunctionIR *functionp,
                               const std::string &source_file);

  bool SetupFunctionParameter(abi_util::FunctionIR *functionp,
                              const clang::QualType qual_type,
                              bool has_default_arg,
                              const std::string &source_file);

  bool SetupThisParameter(abi_util::FunctionIR *functionp,
                          const std::string &source_file);

};

class EnumDeclWrapper : public ABIWrapper {
 public:
  EnumDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::EnumDecl *decl,
      std::set<std::string> *type_cache, abi_util::IRDumper *ir_dumper,
      std::map<const clang::Decl *, std::string> &decl_to_source_cache_);

  bool GetEnumDecl();

 private:
  const clang::EnumDecl *enum_decl_;

 private:
  bool SetupEnum(abi_util::EnumTypeIR *type,
                 const std::string &source_file);

  bool SetupEnumFields(abi_util::EnumTypeIR *enump);
};

class GlobalVarDeclWrapper : public ABIWrapper {
 public:
  GlobalVarDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::VarDecl *decl,
      std::set<std::string> *type_cache, abi_util::IRDumper *ir_dumper,
      std::map<const clang::Decl *, std::string> &decl_to_source_cache_);

  bool GetGlobalVarDecl();

 private:
  const clang::VarDecl *global_var_decl_;
 private:
  bool SetupGlobalVar(abi_util::GlobalVarIR *global_varp,
                      const std::string &source_file);
};

} //end namespace abi_wrapper

#endif // ABI_WRAPPERS_H_

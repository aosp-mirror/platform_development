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

#include "ast_util.h"
#include <ir_representation.h>

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Mangle.h>
#include <clang/AST/VTableBuilder.h>
#include <clang/Frontend/CompilerInstance.h>

namespace abi_wrapper {

struct TypeAndCreationStatus {
  std::unique_ptr<abi_util::TypeIR> typep_;
  bool should_create_type_; // Whether the type is to be created.
  TypeAndCreationStatus(std::unique_ptr<abi_util::TypeIR> &&typep,
                        bool should_create_type = true)
      : typep_(std::move(typep)), should_create_type_(should_create_type) { }
};

class ABIWrapper {
 public:
  ABIWrapper(clang::MangleContext *mangle_contextp,
             clang::ASTContext *ast_contextp,
             const clang::CompilerInstance *cip,
             abi_util::IRDumper *ir_dumper,
             ast_util::ASTCaches *ast_caches);

  static std::string GetDeclSourceFile(const clang::Decl *decl,
                                       const clang::CompilerInstance *cip);

  static std::string GetMangledNameDecl(const clang::NamedDecl *decl,
                                        clang::MangleContext *mangle_context);

 protected:
  std::string GetCachedDeclSourceFile(const clang::Decl *decl,
                                      const clang::CompilerInstance *cip);

  std::string GetKeyForTypeId(clang::QualType qual_type);

  std::string TypeNameWithFinalDestination(clang::QualType qual_type);

  bool SetupTemplateArguments(const clang::TemplateArgumentList *tl,
                              abi_util::TemplatedArtifactIR *ta,
                              const std::string &source_file);

  bool SetupFunctionParameter(abi_util::CFunctionLikeIR *functionp,
                              const clang::QualType qual_type,
                              bool has_default_arg,
                              const std::string &source_file,
                              bool is_this_parameter = false);

  std::string QualTypeToString(const clang::QualType &sweet_qt);

  std::string GetTagDeclQualifiedName(const clang::TagDecl *decl);

  bool CreateBasicNamedAndTypedDecl(clang::QualType,
                                    const std::string &source_file);

  bool CreateBasicNamedAndTypedDecl(clang::QualType canonical_type,
                                    abi_util::TypeIR *typep,
                                    const std::string &source_file);

  bool CreateExtendedType(clang::QualType canonical_type,
                          abi_util::TypeIR *typep);

  bool CreateAnonymousRecord(const clang::RecordDecl *decl);

  std::string GetTypeLinkageName(const clang::Type *typep);

  TypeAndCreationStatus SetTypeKind(const clang::QualType qtype,
                                    const std::string &source_file);

  std::string GetTypeUniqueId(const clang::TagDecl *tag_decl);

 protected:
  const clang::CompilerInstance *cip_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
  abi_util::IRDumper *ir_dumper_;
  ast_util::ASTCaches *ast_caches_;
};

class RecordDeclWrapper : public ABIWrapper {
 public:
  RecordDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::RecordDecl *record_decl, abi_util::IRDumper *ir_dumper,
      ast_util::ASTCaches *ast_caches);

  bool GetRecordDecl();

 private:
  const clang::RecordDecl *record_decl_;

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

  std::string GetMangledRTTI(const clang::CXXRecordDecl *cxx_record_decl);

  abi_util::VTableComponentIR SetupRecordVTableComponent(
      const clang::VTableComponent &vtable_component);

  bool SetupCXXRecordInfo(abi_util::RecordTypeIR *record_declp,
                          const std::string &source_file);
};

class FunctionDeclWrapper : public ABIWrapper {
 public:
  FunctionDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::FunctionDecl *decl, abi_util::IRDumper *ir_dumper,
      ast_util::ASTCaches *ast_caches);

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

  bool SetupThisParameter(abi_util::FunctionIR *functionp,
                          const std::string &source_file);

};

class FunctionTypeWrapper : public ABIWrapper {
 private:
  bool SetupFunctionType(abi_util::FunctionTypeIR *function_type_ir);

 public:
  FunctionTypeWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::FunctionType *function_type, abi_util::IRDumper *ir_dumper,
      ast_util::ASTCaches *ast_caches, const std::string &source_file);

  bool GetFunctionType();

 private:
  const clang::FunctionType *function_type_= nullptr;
  const std::string &source_file_;
};

class EnumDeclWrapper : public ABIWrapper {
 public:
  EnumDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::EnumDecl *decl, abi_util::IRDumper *ir_dumper,
      ast_util::ASTCaches *ast_caches);

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
      const clang::VarDecl *decl, abi_util::IRDumper *ir_dumper,
      ast_util::ASTCaches *ast_caches);

  bool GetGlobalVarDecl();

 private:
  const clang::VarDecl *global_var_decl_;
 private:
  bool SetupGlobalVar(abi_util::GlobalVarIR *global_varp,
                      const std::string &source_file);
};

} //end namespace abi_wrapper

#endif // ABI_WRAPPERS_H_

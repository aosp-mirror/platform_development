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

#include "dumper/ast_util.h"
#include "repr/ir_representation.h"

#include <clang/AST/AST.h>
#include <clang/AST/ASTConsumer.h>
#include <clang/AST/Mangle.h>
#include <clang/AST/VTableBuilder.h>
#include <clang/Frontend/CompilerInstance.h>


namespace header_checker {
namespace dumper {


struct TypeAndCreationStatus {
  std::unique_ptr<repr::TypeIR> typep_;
  bool should_create_type_;  // Whether the type is to be created.
  TypeAndCreationStatus(std::unique_ptr<repr::TypeIR> &&typep,
                        bool should_create_type = true)
      : typep_(std::move(typep)), should_create_type_(should_create_type) {}
};


class ABIWrapper {
 public:
  ABIWrapper(clang::MangleContext *mangle_contextp,
             clang::ASTContext *ast_contextp,
             const clang::CompilerInstance *cip,
             repr::ModuleIR *module,
             ASTCaches *ast_caches);

 public:
  static std::string GetDeclSourceFile(const clang::Decl *decl,
                                       const clang::CompilerInstance *cip);

 protected:
  std::string GetCachedDeclSourceFile(const clang::Decl *decl,
                                      const clang::CompilerInstance *cip);

 public:
  static std::string GetMangledNameDecl(const clang::NamedDecl *decl,
                                        clang::MangleContext *mangle_context);

 protected:
  // Shared between FunctionDeclWrapper and RecordDeclWrapper.
  bool SetupTemplateArguments(const clang::TemplateArgumentList *tl,
                              repr::TemplatedArtifactIR *ta,
                              const std::string &source_file);

 protected:
  // Shared between FunctionTypeWrapper and FunctionDeclWrapper.
  bool SetupFunctionParameter(repr::CFunctionLikeIR *functionp,
                              const clang::QualType qual_type,
                              bool has_default_arg,
                              const std::string &source_file,
                              bool is_this_parameter = false);

 protected:
  // Type-related functions
  std::string GetTypeUniqueId(clang::QualType qual_type);

  bool CreateBasicNamedAndTypedDecl(clang::QualType,
                                    const std::string &source_file);

  bool CreateBasicNamedAndTypedDecl(clang::QualType canonical_type,
                                    repr::TypeIR *typep,
                                    const std::string &source_file);

  bool CreateExtendedType(clang::QualType canonical_type,
                          repr::TypeIR *typep);

 private:
  std::string QualTypeToString(const clang::QualType &sweet_qt);

  TypeAndCreationStatus SetTypeKind(const clang::QualType qtype,
                                    const std::string &source_file);

  bool CreateAnonymousRecord(const clang::RecordDecl *decl);

 protected:
  const clang::CompilerInstance *cip_;
  clang::MangleContext *mangle_contextp_;
  clang::ASTContext *ast_contextp_;
  repr::ModuleIR *module_;
  ASTCaches *ast_caches_;
};


class RecordDeclWrapper : public ABIWrapper {
 public:
  RecordDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::RecordDecl *record_decl, repr::ModuleIR *module,
      ASTCaches *ast_caches);

  bool GetRecordDecl();

 private:
  const clang::RecordDecl *record_decl_;

 private:
  bool SetupRecordInfo(repr::RecordTypeIR *type,
                       const std::string &source_file);

  bool SetupRecordFields(repr::RecordTypeIR *record_declp,
                         const std::string &source_file);

  bool SetupCXXBases(repr::RecordTypeIR *cxxp,
                     const clang::CXXRecordDecl *cxx_record_decl);

  bool SetupTemplateInfo(repr::RecordTypeIR *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl,
                         const std::string &source_file);

  bool SetupRecordVTable(repr::RecordTypeIR *record_declp,
                         const clang::CXXRecordDecl *cxx_record_decl);

  std::string GetMangledRTTI(const clang::CXXRecordDecl *cxx_record_decl);

  repr::VTableComponentIR
  SetupRecordVTableComponent(const clang::VTableComponent &vtable_component,
                             const clang::ThunkInfo &thunk_info);

  bool SetupCXXRecordInfo(repr::RecordTypeIR *record_declp,
                          const std::string &source_file);
};


class FunctionDeclWrapper : public ABIWrapper {
 public:
  FunctionDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::FunctionDecl *decl, repr::ModuleIR *module,
      ASTCaches *ast_caches);

  std::unique_ptr<repr::FunctionIR> GetFunctionDecl();

 private:
  const clang::FunctionDecl *function_decl_;

 private:
  bool SetupFunction(repr::FunctionIR *methodp,
                     const std::string &source_file);

  bool SetupTemplateInfo(repr::FunctionIR *functionp,
                         const std::string &source_file);

  bool SetupFunctionParameters(repr::FunctionIR *functionp,
                               const std::string &source_file);

  bool SetupThisParameter(repr::FunctionIR *functionp,
                          const std::string &source_file);
};


class FunctionTypeWrapper : public ABIWrapper {
 private:
  bool SetupFunctionType(repr::FunctionTypeIR *function_type_ir);

 public:
  FunctionTypeWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::FunctionType *function_type, repr::ModuleIR *module,
      ASTCaches *ast_caches, const std::string &source_file);

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
      const clang::EnumDecl *decl, repr::ModuleIR *module,
      ASTCaches *ast_caches);

  bool GetEnumDecl();

 private:
  const clang::EnumDecl *enum_decl_;

 private:
  bool SetupEnum(repr::EnumTypeIR *type,
                 const std::string &source_file);

  bool SetupEnumFields(repr::EnumTypeIR *enump);
};


class GlobalVarDeclWrapper : public ABIWrapper {
 public:
  GlobalVarDeclWrapper(
      clang::MangleContext *mangle_contextp, clang::ASTContext *ast_contextp,
      const clang::CompilerInstance *compiler_instance_p,
      const clang::VarDecl *decl, repr::ModuleIR *module,
      ASTCaches *ast_caches);

  bool GetGlobalVarDecl();

 private:
  const clang::VarDecl *global_var_decl_;

 private:
  bool SetupGlobalVar(repr::GlobalVarIR *global_varp,
                      const std::string &source_file);
};


}  // namespace dumper
}  // namespace header_checker


#endif  // ABI_WRAPPERS_H_

// Copyright (C) 2019 The Android Open Source Project
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

#ifndef HEADER_CHECKER_REPR_JSON_IR_DUMPER_H_
#define HEADER_CHECKER_REPR_JSON_IR_DUMPER_H_

#include "repr/ir_dumper.h"
#include "repr/ir_reader.h"
#include "repr/ir_representation.h"
#include "repr/json/converter.h"


namespace header_checker {
namespace repr {


class IRToJsonConverter {
 private:
  static void AddTemplateInfo(JsonObject &type_decl,
                              const TemplatedArtifactIR *template_ir);

  // BasicNamedAndTypedDecl
  static void AddTypeInfo(JsonObject &type_decl, const TypeIR *type_ir);

  static void AddRecordFields(JsonObject &record_type,
                              const RecordTypeIR *record_ir);

  static void AddBaseSpecifiers(JsonObject &record_type,
                                const RecordTypeIR *record_ir);

  static void AddVTableLayout(JsonObject &record_type,
                              const RecordTypeIR *record_ir);

  static void AddEnumFields(JsonObject &enum_type, const EnumTypeIR *enum_ir);

 public:
  static JsonObject ConvertEnumTypeIR(const EnumTypeIR *enump);

  static JsonObject ConvertRecordTypeIR(const RecordTypeIR *recordp);

  static JsonObject ConvertFunctionTypeIR(const FunctionTypeIR *function_typep);

  static void AddFunctionParametersAndSetReturnType(
      JsonObject &function, const CFunctionLikeIR *cfunction_like_ir);

  static void AddFunctionParameters(JsonObject &function,
                                    const CFunctionLikeIR *cfunction_like_ir);

  static JsonObject ConvertFunctionIR(const FunctionIR *functionp);

  static JsonObject ConvertGlobalVarIR(const GlobalVarIR *global_varp);

  static JsonObject ConvertPointerTypeIR(const PointerTypeIR *pointerp);

  static JsonObject ConvertQualifiedTypeIR(const QualifiedTypeIR *qualtypep);

  static JsonObject ConvertBuiltinTypeIR(const BuiltinTypeIR *builtin_typep);

  static JsonObject ConvertArrayTypeIR(const ArrayTypeIR *array_typep);

  static JsonObject ConvertLvalueReferenceTypeIR(
      const LvalueReferenceTypeIR *lvalue_reference_typep);

  static JsonObject ConvertRvalueReferenceTypeIR(
      const RvalueReferenceTypeIR *rvalue_reference_typep);
};

class JsonIRDumper : public IRDumper, public IRToJsonConverter {
 public:
  JsonIRDumper(const std::string &dump_path);

  ~JsonIRDumper() override {}

  bool Dump(const ModuleIR &module) override;

 private:
  bool AddLinkableMessageIR(const LinkableMessageIR *) override;

  bool AddElfSymbolMessageIR(const ElfSymbolIR *) override;

 private:
  JsonObject translation_unit_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_JSON_IR_DUMPER_H_

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

#ifndef HEADER_CHECKER_REPR_PROTOBUF_IR_READER_H_
#define HEADER_CHECKER_REPR_PROTOBUF_IR_READER_H_

#include "repr/ir_reader.h"
#include "repr/protobuf/abi_diff.h"
#include "repr/protobuf/abi_dump.h"

#include <set>
#include <string>
#include <vector>

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>


namespace header_checker {
namespace repr {


class ProtobufIRReader : public IRReader {
 private:
  template <typename T>
  using RepeatedPtrField = google::protobuf::RepeatedPtrField<T>;


 public:
  ProtobufIRReader(const std::set<std::string> *exported_headers)
      : IRReader(exported_headers) {}


 private:
  bool ReadDumpImpl(const std::string &dump_file) override;

  void ReadFunctions(const abi_dump::TranslationUnit &tu);

  void ReadGlobalVariables(const abi_dump::TranslationUnit &tu);

  void ReadEnumTypes(const abi_dump::TranslationUnit &tu);

  void ReadRecordTypes(const abi_dump::TranslationUnit &tu);

  void ReadFunctionTypes(const abi_dump::TranslationUnit &tu);

  void ReadPointerTypes(const abi_dump::TranslationUnit &tu);

  void ReadBuiltinTypes(const abi_dump::TranslationUnit &tu);

  void ReadQualifiedTypes(const abi_dump::TranslationUnit &tu);

  void ReadArrayTypes(const abi_dump::TranslationUnit &tu);

  void ReadLvalueReferenceTypes(const abi_dump::TranslationUnit &tu);

  void ReadRvalueReferenceTypes(const abi_dump::TranslationUnit &tu);

  void ReadElfFunctions(const abi_dump::TranslationUnit &tu);

  void ReadElfObjects(const abi_dump::TranslationUnit &tu);

  void ReadTypeInfo(const abi_dump::BasicNamedAndTypedDecl &type_info,
                    TypeIR *typep);

  FunctionIR FunctionProtobufToIR(const abi_dump::FunctionDecl &);

  FunctionTypeIR FunctionTypeProtobufToIR(
      const abi_dump::FunctionType &function_type_protobuf);

  RecordTypeIR RecordTypeProtobufToIR(
      const abi_dump::RecordType &record_type_protobuf);

  std::vector<RecordFieldIR> RecordFieldsProtobufToIR(
      const RepeatedPtrField<abi_dump::RecordFieldDecl> &rfp);

  std::vector<CXXBaseSpecifierIR> RecordCXXBaseSpecifiersProtobufToIR(
      const RepeatedPtrField<abi_dump::CXXBaseSpecifier> &rbs);

  std::vector<EnumFieldIR> EnumFieldsProtobufToIR(
      const RepeatedPtrField<abi_dump::EnumFieldDecl> &efp);

  EnumTypeIR EnumTypeProtobufToIR(
      const abi_dump::EnumType &enum_type_protobuf);

  VTableLayoutIR VTableLayoutProtobufToIR(
      const abi_dump::VTableLayout &vtable_layout_protobuf);

  TemplateInfoIR TemplateInfoProtobufToIR(
      const abi_dump::TemplateInfo &template_info_protobuf);
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_PROTOBUF_IR_READER_H_

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

#ifndef HEADER_CHECKER_REPR_PROTOBUF_IR_DUMPER_H_
#define HEADER_CHECKER_REPR_PROTOBUF_IR_DUMPER_H_

#include "repr/ir_dumper.h"
#include "repr/protobuf/abi_dump.h"
#include "repr/protobuf/converter.h"
#include "repr/protobuf/ir_dumper.h"


namespace header_checker {
namespace repr {


class ProtobufIRDumper : public IRDumper, public IRToProtobufConverter {
 private:
  // Types
  bool AddRecordTypeIR(const RecordTypeIR *);

  bool AddEnumTypeIR(const EnumTypeIR *);

  bool AddPointerTypeIR(const PointerTypeIR *);

  bool AddQualifiedTypeIR(const QualifiedTypeIR *);

  bool AddLvalueReferenceTypeIR(const LvalueReferenceTypeIR *);

  bool AddRvalueReferenceTypeIR(const RvalueReferenceTypeIR *);

  bool AddArrayTypeIR(const ArrayTypeIR *);

  bool AddBuiltinTypeIR(const BuiltinTypeIR *);

  bool AddFunctionTypeIR(const FunctionTypeIR *function_typep);

  // Functions and global variables.
  bool AddFunctionIR(const FunctionIR *);

  bool AddGlobalVarIR(const GlobalVarIR *);

  bool AddElfFunctionIR(const ElfFunctionIR *);

  bool AddElfObjectIR(const ElfObjectIR *);


 public:
  ProtobufIRDumper(const std::string &dump_path)
      : IRDumper(dump_path), tu_ptr_(new abi_dump::TranslationUnit()) {}

  ~ProtobufIRDumper() override {}

  bool Dump(const ModuleIR &module) override;


 private:
  bool AddLinkableMessageIR(const LinkableMessageIR *) override;

  bool AddElfSymbolMessageIR(const ElfSymbolIR *) override;


 private:
  std::unique_ptr<abi_dump::TranslationUnit> tu_ptr_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_PROTOBUF_IR_DUMPER_H_

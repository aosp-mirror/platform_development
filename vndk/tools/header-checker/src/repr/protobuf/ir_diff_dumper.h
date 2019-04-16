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

#ifndef HEADER_CHECKER_REPR_PROTOBUF_IR_DIFF_DUMPER_H_
#define HEADER_CHECKER_REPR_PROTOBUF_IR_DIFF_DUMPER_H_

#include "repr/ir_diff_dumper.h"
#include "repr/ir_diff_representation.h"
#include "repr/ir_representation.h"
#include "repr/protobuf/abi_diff.h"

#include <string>


namespace header_checker {
namespace repr {


class ProtobufIRDiffDumper : public IRDiffDumper {
 public:
  ProtobufIRDiffDumper(const std::string &dump_path)
      : IRDiffDumper(dump_path),
        diff_tu_(new abi_diff::TranslationUnitDiff()) {}

  ~ProtobufIRDiffDumper() override {}

  bool AddDiffMessageIR(const DiffMessageIR *, const std::string &type_stack,
                        DiffKind diff_kind) override;

  bool AddLinkableMessageIR(const LinkableMessageIR *,
                            DiffKind diff_kind) override;

  bool AddElfSymbolMessageIR(const ElfSymbolIR *, DiffKind diff_kind) override;

  void AddLibNameIR(const std::string &name) override;

  void AddArchIR(const std::string &arch) override;

  void AddCompatibilityStatusIR(CompatibilityStatusIR status) override;

  bool Dump() override;

  CompatibilityStatusIR GetCompatibilityStatusIR() override;


 private:
  bool AddRecordTypeDiffIR(const RecordTypeDiffIR *,
                           const std::string &type_stack, DiffKind diff_kind);

  bool AddEnumTypeDiffIR(const EnumTypeDiffIR *,
                         const std::string &type_stack, DiffKind diff_kind);

  bool AddFunctionDiffIR(const FunctionDiffIR *,
                         const std::string &type_stack, DiffKind diff_kind);

  bool AddGlobalVarDiffIR(const GlobalVarDiffIR *,
                          const std::string &type_stack, DiffKind diff_kind);


  bool AddLoneRecordTypeDiffIR(const RecordTypeIR *, DiffKind diff_kind);

  bool AddLoneEnumTypeDiffIR(const EnumTypeIR *, DiffKind diff_kind);

  bool AddLoneFunctionDiffIR(const FunctionIR *, DiffKind diff_kind);

  bool AddLoneGlobalVarDiffIR(const GlobalVarIR *, DiffKind diff_kind);


  bool AddElfObjectIR(const ElfObjectIR *elf_object_ir, DiffKind diff_kind);

  bool AddElfFunctionIR(const ElfFunctionIR *elf_function_ir,
                        DiffKind diff_kind);


 protected:
  std::unique_ptr<abi_diff::TranslationUnitDiff> diff_tu_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_PROTOBUF_PROTOBUF_IR_DIFF_DUMPER_H_

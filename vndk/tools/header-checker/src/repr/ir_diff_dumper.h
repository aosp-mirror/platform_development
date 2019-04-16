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

#ifndef HEADER_CHECKER_REPR_IR_DIFF_DUMPER_H_
#define HEADER_CHECKER_REPR_IR_DIFF_DUMPER_H_

#include "repr/ir_diff_representation.h"

#include <string>


namespace header_checker {
namespace repr {


class IRDiffDumper {
 public:
  typedef DiffMessageIR::DiffKind DiffKind;

 public:
  IRDiffDumper(const std::string &dump_path) : dump_path_(dump_path) {}

  virtual ~IRDiffDumper() {}

  virtual bool AddDiffMessageIR(const DiffMessageIR *,
                                const std::string &type_stack,
                                DiffKind diff_kind) = 0;

  virtual bool AddLinkableMessageIR(const LinkableMessageIR *,
                                    DiffKind diff_kind) = 0;

  virtual bool AddElfSymbolMessageIR(const ElfSymbolIR *,
                                     DiffKind diff_kind) = 0;

  virtual void AddLibNameIR(const std::string &name) = 0;

  virtual void AddArchIR(const std::string &arch) = 0;

  virtual void AddCompatibilityStatusIR(CompatibilityStatusIR status) = 0;

  virtual bool Dump() = 0;

  virtual CompatibilityStatusIR GetCompatibilityStatusIR() = 0;

  static std::unique_ptr<IRDiffDumper> CreateIRDiffDumper(
      TextFormatIR, const std::string &dump_path);

 protected:
  const std::string &dump_path_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_DIFF_DUMPER_H_

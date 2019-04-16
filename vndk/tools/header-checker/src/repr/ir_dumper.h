// Copyright (C) 2017 The Android Open Source Project
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

#ifndef HEADER_CHECKER_REPR_IR_DUMPER_H_
#define HEADER_CHECKER_REPR_IR_DUMPER_H_

#include "repr/ir_representation.h"

#include <string>


namespace header_checker {
namespace repr {


class IRDumper {
 public:
  IRDumper(const std::string &dump_path) : dump_path_(dump_path) {}

  virtual ~IRDumper() {}

  static std::unique_ptr<IRDumper> CreateIRDumper(
      TextFormatIR text_format, const std::string &dump_path);

  virtual bool Dump(const ModuleIR &module) = 0;

 protected:
  bool DumpModule(const ModuleIR &module);

  virtual bool AddLinkableMessageIR(const LinkableMessageIR *) = 0;

  virtual bool AddElfSymbolMessageIR(const ElfSymbolIR *) = 0;

 protected:
  const std::string &dump_path_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_DUMPER_H_

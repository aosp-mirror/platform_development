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
#include "repr/ir_representation.h"
#include "repr/json/converter.h"


namespace header_checker {
namespace repr {


class JsonIRDumper : public IRDumper {
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

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

#ifndef HEADER_CHECKER_REPR_IR_READER_H_
#define HEADER_CHECKER_REPR_IR_READER_H_

#include "repr/ir_representation.h"

#include <memory>
#include <string>


namespace header_checker {
namespace repr {


class IRReader {
 public:
  // Construct an IRReader that loads a dump file to module_ir.
  // If module_ir is nullptr, create a ModuleIR with the default constructor.
  static std::unique_ptr<IRReader> CreateIRReader(
      TextFormatIR text_format, std::unique_ptr<ModuleIR> module_ir = nullptr);

  IRReader(std::unique_ptr<ModuleIR> module_ir)
      : module_(std::move(module_ir)) {}

  virtual ~IRReader() {}

  bool ReadDump(const std::string &dump_file);

  const ModuleIR &GetModule() const { return *module_; }

 private:
  virtual bool ReadDumpImpl(const std::string &dump_file) = 0;

 protected:
  std::unique_ptr<ModuleIR> module_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_READER_H_

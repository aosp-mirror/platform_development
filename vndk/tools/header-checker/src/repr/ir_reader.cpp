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

#include "repr/ir_reader.h"

#include "repr/ir_representation.h"
#include "repr/json/api.h"
#include "repr/protobuf/api.h"

#include <list>
#include <memory>
#include <set>
#include <string>

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace repr {


std::unique_ptr<IRReader> IRReader::CreateIRReader(
    TextFormatIR text_format, std::unique_ptr<ModuleIR> module_ir) {
  if (module_ir == nullptr) {
    module_ir = std::make_unique<ModuleIR>();
  }
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return CreateProtobufIRReader(std::move(module_ir));
    case TextFormatIR::Json:
      return CreateJsonIRReader(std::move(module_ir));
    default:
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}

bool IRReader::ReadDump(const std::string &dump_file) {
  module_->SetCompilationUnitPath(dump_file);
  return ReadDumpImpl(dump_file);
}


}  // namespace repr
}  // header_checker

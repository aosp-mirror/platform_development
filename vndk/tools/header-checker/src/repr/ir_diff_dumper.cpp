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

#include "repr/ir_diff_dumper.h"

#include "repr/ir_representation.h"
#include "repr/protobuf/api.h"

#include <memory>
#include <string>

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace repr {


std::unique_ptr<IRDiffDumper> IRDiffDumper::CreateIRDiffDumper(
    TextFormatIR text_format, const std::string &dump_path) {
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return CreateProtobufIRDiffDumper(dump_path);
    default:
      // Nothing else is supported yet.
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}


}  // namespace repr
}  // header_checker

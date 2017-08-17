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

#include <ir_representation.h>
#include <ir_representation_protobuf.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#pragma clang diagnostic pop

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <llvm/Support/raw_ostream.h>

#include <string>
#include <memory>


namespace abi_util {

std::unique_ptr<IRDumper> IRDumper::CreateIRDumper(
    const std::string &type, const std::string &dump_path) {
  if (type == "protobuf") {
    return std::make_unique<ProtobufIRDumper>(dump_path);
  }
  // Nothing else is supported yet.
  llvm::errs() << type << " message format is not supported yet!\n";
  return nullptr;
}

std::unique_ptr<IRDiffDumper> IRDiffDumper::CreateIRDiffDumper(
    const std::string &type, const std::string &dump_path) {
  if (type == "protobuf") {
    return std::make_unique<ProtobufIRDiffDumper>(dump_path);
  }
  // Nothing else is supported yet.
  llvm::errs() << type << " message format is not supported yet!\n";
  return nullptr;
}

std::unique_ptr<TextFormatToIRReader>
TextFormatToIRReader::CreateTextFormatToIRReader(
    const std::string &type, const std::string &dump_path) {
  if (type == "protobuf") {
    return std::make_unique<ProtobufTextFormatToIRReader>(dump_path);
  }
  // Nothing else is supported yet.
  llvm::errs() << type << " message format is not supported yet!\n";
  return nullptr;
}

} // namespace abi_util


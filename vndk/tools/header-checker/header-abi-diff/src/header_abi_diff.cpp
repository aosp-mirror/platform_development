// Copyright (C) 2016 The Android Open Source Project
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

#include "abi_diff.h"

#include <llvm/Support/CommandLine.h>
#include <llvm/Support/raw_ostream.h>

static llvm::cl::OptionCategory header_checker_category(
    "header-abi-diff options");

static llvm::cl::opt<std::string> compatibility_report(
    "o", llvm::cl::desc("<compatibility report>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> new_dump(
    "new", llvm::cl::desc("<new dump>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> old_dump(
    "old", llvm::cl::desc("<old dump>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

int main(int argc, const char **argv) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-checker");
  uint8_t extension_or_incompatible = 0;
  HeaderAbiDiff judge(old_dump, new_dump, compatibility_report);
  switch (judge.GenerateCompatibilityReport()) {
    case HeaderAbiDiff::COMPATIBLE:
      break;
    case HeaderAbiDiff::EXTENSION:
      extension_or_incompatible = 1;
      break;
    default:
      extension_or_incompatible = 2;
      break;
  }
  if (extension_or_incompatible) {
    llvm::errs() << "******************************************************\n"
                 << "VNDK Abi Compliance breakage:"
                 << " Please check compatiblity report at : "
                 << compatibility_report << "\n"
                 << "*****************************************************\n";
  }
  return extension_or_incompatible;
}

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

#include "fixed_argv.h"
#include "frontend_action_factory.h"

#include <header_abi_util.h>

#include <clang/Frontend/FrontendActions.h>
#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/CompilationDatabase.h>
#include <clang/Tooling/Tooling.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/raw_ostream.h>

#include <memory>
#include <string>
#include <vector>

#include <stdlib.h>

static llvm::cl::OptionCategory header_checker_category(
    "header-checker options");

static llvm::cl::opt<std::string> header_file(
    llvm::cl::Positional, llvm::cl::desc("<source.cpp>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> out_dump(
    "o", llvm::cl::value_desc("out_dump"), llvm::cl::Required,
    llvm::cl::desc("Specify the reference dump file name"),
    llvm::cl::cat(header_checker_category));

static llvm::cl::list<std::string> exported_header_dirs(
    "I", llvm::cl::desc("<export_include_dirs>"), llvm::cl::Prefix,
    llvm::cl::ZeroOrMore, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> no_filter(
    "no-filter", llvm::cl::desc("Do not filter any abi"), llvm::cl::Optional,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<abi_util::TextFormatIR> text_format(
    "text-format", llvm::cl::desc("Specify text format of abi dump"),
    llvm::cl::values(clEnumValN(abi_util::TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValEnd),
    llvm::cl::init(abi_util::TextFormatIR::ProtobufTextFormat),
    llvm::cl::cat(header_checker_category));

// Hide irrelevant command line options defined in LLVM libraries.
static void HideIrrelevantCommandLineOptions() {
  llvm::StringMap<llvm::cl::Option *> &map = llvm::cl::getRegisteredOptions();
  for (llvm::StringMapEntry<llvm::cl::Option *> &p : map) {
    if (p.second->Category == &header_checker_category) {
      continue;
    }
    if (p.first().startswith("help")) {
      continue;
    }
    p.second->setHiddenFlag(llvm::cl::Hidden);
  }
}


int main(int argc, const char **argv) {
  HideIrrelevantCommandLineOptions();

  // Tweak argc and argv to workaround clang version mismatches.
  FixedArgv fixed_argv(argc, argv);
  FixedArgvRegistry::Apply(fixed_argv);

  // Create compilation database from command line arguments after "--".
  std::unique_ptr<clang::tooling::CompilationDatabase> compilations;

  {
    // loadFromCommandLine() may alter argc and argv, thus access fixed_argv
    // through FixedArgvAccess.
    FixedArgvAccess raw(fixed_argv);
    compilations.reset(
        clang::tooling::FixedCompilationDatabase::loadFromCommandLine(
            raw.argc_, raw.argv_));
  }

  // Parse the command line options.
  llvm::cl::ParseCommandLineOptions(
      fixed_argv.GetArgc(), fixed_argv.GetArgv(), "header-checker");

  // Input header file existential check.
  if (!llvm::sys::fs::exists(header_file)) {
    llvm::errs() << "ERROR: Header file \"" << header_file << "\" not found\n";
    ::exit(1);
  }

  // Check whether we can create compilation database and deduce compiler
  // options from command line options.
  if (!compilations) {
    llvm::errs() << "ERROR: Clang compilation options not specified.\n";
    ::exit(1);
  }

  std::set<std::string> exported_headers;
  if (!no_filter) {
    exported_headers =
        abi_util::CollectAllExportedHeaders(exported_header_dirs);
  }

  // Initialize clang tools and run front-end action.
  std::vector<std::string> header_files{ header_file };

  clang::tooling::ClangTool tool(*compilations, header_files);
  std::unique_ptr<clang::tooling::FrontendActionFactory> factory(
      new HeaderCheckerFrontendActionFactory(out_dump, exported_headers,
                                             text_format));

  return tool.run(factory.get());
}

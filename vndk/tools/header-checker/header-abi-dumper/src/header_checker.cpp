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

#include "frontend_action_factory.h"

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
    "I", llvm::cl::desc("<export_include_dirs>"), llvm::cl::ZeroOrMore,
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

  // FIXME: Clang FORTIFY requires a version of clang at least as new as
  // clang-3688880 (r285906). Since external/clang is currently r275480, we need
  // to disable FORTIFY for this tool to function correctly.
  std::vector<const char *> fixedArgv(argv, argv + argc);
  fixedArgv.push_back("-U_FORTIFY_SOURCE");
  int fixedArgc = fixedArgv.size();

  // Create compilation database from command line arguments after "--".
  std::unique_ptr<clang::tooling::CompilationDatabase> compilations(
      clang::tooling::FixedCompilationDatabase::loadFromCommandLine(
          fixedArgc, fixedArgv.data()));

  // Parse the command line options.
  // Note that loadFromCommandLine may alter fixedArgc, so we can't use
  // fixedArgv.size() here.
  llvm::cl::ParseCommandLineOptions(fixedArgc, fixedArgv.data(),
      "header-checker");

  // Input header file existential check.
  if (!llvm::sys::fs::exists(header_file)) {
    llvm::errs() << "ERROR: Header file \"" << header_file << "\" not found\n";
    ::exit(1);
  }

  //Existential checks for exported dirs.
  for (auto &&dir : exported_header_dirs) {
    if (!llvm::sys::fs::exists(dir)) {
      llvm::errs() << "ERROR: exported dir \"" << dir << "\" not found\n";
      ::exit(1);
    }
  }

  // Check whether we can create compilation database and deduce compiler
  // options from command line options.
  if (!compilations) {
    llvm::errs() << "ERROR: Clang compilation options not specified.\n";
    ::exit(1);
  }

  // Initialize clang tools and run front-end action.
  std::vector<std::string> header_files{ header_file };

  clang::tooling::ClangTool tool(*compilations, header_files);
  std::unique_ptr<clang::tooling::FrontendActionFactory> factory(
      new HeaderCheckerFrontendActionFactory(out_dump, exported_header_dirs));

  return tool.run(factory.get());
}

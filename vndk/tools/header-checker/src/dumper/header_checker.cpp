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

#include "dumper/header_checker.h"

#include "dumper/fixed_argv.h"
#include "dumper/frontend_action_factory.h"
#include "utils/command_line_utils.h"
#include "utils/header_abi_util.h"

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


using header_checker::dumper::FixedArgv;
using header_checker::dumper::FixedArgvAccess;
using header_checker::dumper::FixedArgvRegistry;
using header_checker::dumper::HeaderCheckerFrontendActionFactory;
using header_checker::dumper::HeaderCheckerOptions;
using header_checker::repr::TextFormatIR;
using header_checker::utils::CollectAllExportedHeaders;
using header_checker::utils::HideIrrelevantCommandLineOptions;
using header_checker::utils::RealPath;



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

static llvm::cl::opt<bool> suppress_errors(
    "suppress-errors",
    llvm::cl::desc("Suppress preprocess and semantic errors"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> dump_function_declarations(
    "dump-function-declarations",
    llvm::cl::desc("Output the functions declared but not defined in the input "
                   "file"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<TextFormatIR> output_format(
    "output-format", llvm::cl::desc("Specify format of output dump file"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(TextFormatIR::Json),
    llvm::cl::cat(header_checker_category));

int main(int argc, const char **argv) {
  HideIrrelevantCommandLineOptions(header_checker_category);

  // Tweak argc and argv to workaround clang version mismatches.
  FixedArgv fixed_argv(argc, argv);
  FixedArgvRegistry::Apply(fixed_argv);

  // Create compilation database from command line arguments after "--".
  std::string cmdline_error_msg;
  std::unique_ptr<clang::tooling::CompilationDatabase> compilations;
  {
    // loadFromCommandLine() may alter argc and argv, thus access fixed_argv
    // through FixedArgvAccess.
    FixedArgvAccess raw(fixed_argv);

    compilations =
        clang::tooling::FixedCompilationDatabase::loadFromCommandLine(
            raw.argc_, raw.argv_, cmdline_error_msg);
  }

  // Parse the command line options
  llvm::cl::ParseCommandLineOptions(
      fixed_argv.GetArgc(), fixed_argv.GetArgv(), "header-checker");

  // Print an error message if we failed to create the compilation database
  // from the command line arguments. This check is intentionally performed
  // after `llvm::cl::ParseCommandLineOptions()` so that `-help` can work
  // without `--`.
  if (!compilations) {
    if (cmdline_error_msg.empty()) {
      llvm::errs() << "ERROR: Failed to parse clang command line options\n";
    } else {
      llvm::errs() << "ERROR: " << cmdline_error_msg << "\n";
    }
    ::exit(1);
  }

  // Input header file existential check.
  if (!llvm::sys::fs::exists(header_file)) {
    llvm::errs() << "ERROR: Header file \"" << header_file << "\" not found\n";
    ::exit(1);
  }

  std::set<std::string> exported_headers;
  if (!no_filter) {
    exported_headers = CollectAllExportedHeaders(exported_header_dirs);
  }

  // Initialize clang tools and run front-end action.
  std::vector<std::string> header_files{ header_file };
  HeaderCheckerOptions options(RealPath(header_file), out_dump,
                               std::move(exported_headers), output_format,
                               dump_function_declarations, suppress_errors);

  clang::tooling::ClangTool tool(*compilations, header_files);
  std::unique_ptr<clang::tooling::FrontendActionFactory> factory(
      new HeaderCheckerFrontendActionFactory(options));
  return tool.run(factory.get());
}

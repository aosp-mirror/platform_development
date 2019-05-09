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

#include "diff/abi_diff.h"

#include "utils/config_file.h"
#include "utils/string_utils.h"

#include <llvm/ADT/SmallString.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>
#include <llvm/Support/raw_ostream.h>

#include <fstream>


using header_checker::diff::HeaderAbiDiff;
using header_checker::repr::CompatibilityStatusIR;
using header_checker::repr::DiffPolicyOptions;
using header_checker::repr::TextFormatIR;
using header_checker::utils::ConfigFile;
using header_checker::utils::ConfigParser;
using header_checker::utils::ParseBool;


static llvm::cl::OptionCategory header_checker_category(
    "header-abi-diff options");

static llvm::cl::opt<std::string> compatibility_report(
    "o", llvm::cl::desc("<compatibility report>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> lib_name(
    "lib", llvm::cl::desc("<lib name>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> arch(
    "arch", llvm::cl::desc("<arch>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> new_dump(
    "new", llvm::cl::desc("<new dump>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> old_dump(
    "old", llvm::cl::desc("<old dump>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<std::string> ignore_symbol_list(
    "ignore-symbols", llvm::cl::desc("ignore symbols"), llvm::cl::Optional,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> advice_only(
    "advice-only", llvm::cl::desc("Advisory mode only"), llvm::cl::Optional,
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> elf_unreferenced_symbol_errors(
    "elf-unreferenced-symbol-errors",
    llvm::cl::desc("Display erors on removal of elf symbols, unreferenced by"
                   "metadata in exported headers."),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> check_all_apis(
    "check-all-apis",
    llvm::cl::desc("All apis, whether referenced or not, by exported symbols in"
                   " the dynsym table of a shared library are checked"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> allow_extensions(
    "allow-extensions",
    llvm::cl::desc("Do not return a non zero status on extensions"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> allow_unreferenced_elf_symbol_changes(
    "allow-unreferenced-elf-symbol-changes",
    llvm::cl::desc("Do not return a non zero status on changes to elf symbols"
                   "not referenced by metadata in exported headers"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> allow_unreferenced_changes(
    "allow-unreferenced-changes",
    llvm::cl::desc("Do not return a non zero status on changes to data"
                   " structures which are not directly referenced by exported"
                   " APIs."),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> consider_opaque_types_different(
    "consider-opaque-types-different",
    llvm::cl::desc("Consider opaque types with different names as different. "
                   "This should not be used while comparing C++ library ABIs"),
    llvm::cl::Optional, llvm::cl::cat(header_checker_category));

static llvm::cl::opt<TextFormatIR> text_format_old(
    "input-format-old", llvm::cl::desc("Specify input format of old abi dump"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(TextFormatIR::Json),
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<TextFormatIR> text_format_new(
    "input-format-new", llvm::cl::desc("Specify input format of new abi dump"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(TextFormatIR::Json),
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<TextFormatIR> text_format_diff(
    "text-format-diff", llvm::cl::desc("Specify text format of abi-diff"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat")),
    llvm::cl::init(TextFormatIR::ProtobufTextFormat),
    llvm::cl::cat(header_checker_category));

static llvm::cl::opt<bool> allow_adding_removing_weak_symbols(
    "allow-adding-removing-weak-symbols",
    llvm::cl::desc("Do not treat addition or removal of weak symbols as "
                   "incompatible changes."),
    llvm::cl::init(false), llvm::cl::Optional,
    llvm::cl::cat(header_checker_category));

static std::set<std::string> LoadIgnoredSymbols(std::string &symbol_list_path) {
  std::ifstream symbol_ifstream(symbol_list_path);
  std::set<std::string> ignored_symbols;
  if (!symbol_ifstream) {
    llvm::errs() << "Failed to open file containing symbols to ignore\n";
    ::exit(1);
  }
  std::string line = "";
  while (std::getline(symbol_ifstream, line)) {
    ignored_symbols.insert(line);
  }
  return ignored_symbols;
}

static std::string GetConfigFilePath(const std::string &dump_file_path) {
  llvm::SmallString<128> config_file_path(dump_file_path);
  llvm::sys::path::remove_filename(config_file_path);
  llvm::sys::path::append(config_file_path, "config.ini");
  return config_file_path.str();
}

static void ReadConfigFile(const std::string &config_file_path) {
  ConfigFile cfg = ConfigParser::ParseFile(config_file_path);
  if (cfg.HasSection("global")) {
    for (auto &&[key, value] : cfg.GetSection("global")) {
      bool value_bool = ParseBool(value);
      if (key == "allow_adding_removing_weak_symbols") {
        allow_adding_removing_weak_symbols = value_bool;
      } else if (key == "advice_only") {
        advice_only = value_bool;
      } else if (key == "elf_unreferenced_symbol_errors") {
        elf_unreferenced_symbol_errors = value_bool;
      } else if (key == "check_all_apis") {
        check_all_apis = value_bool;
      } else if (key == "allow_extensions") {
        allow_extensions = value_bool;
      } else if (key == "allow_unreferenced_elf_symbol_changes") {
        allow_unreferenced_elf_symbol_changes = value_bool;
      } else if (key == "allow_unreferenced_changes") {
        allow_unreferenced_changes = value_bool;
      } else if (key == "consider_opaque_types_different") {
        consider_opaque_types_different = value_bool;
      }
    }
  }
}

static const char kWarn[] = "\033[36;1mwarning: \033[0m";
static const char kError[] = "\033[31;1merror: \033[0m";

bool ShouldEmitWarningMessage(CompatibilityStatusIR status) {
  return ((!allow_extensions &&
           (status & CompatibilityStatusIR::Extension)) ||
          (!allow_unreferenced_changes &&
           (status & CompatibilityStatusIR::UnreferencedChanges)) ||
          (!allow_unreferenced_elf_symbol_changes &&
           (status & CompatibilityStatusIR::ElfIncompatible)) ||
          (status & CompatibilityStatusIR::Incompatible));
}

int main(int argc, const char **argv) {
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-checker");

  ReadConfigFile(GetConfigFilePath(old_dump));

  std::set<std::string> ignored_symbols;
  if (llvm::sys::fs::exists(ignore_symbol_list)) {
    ignored_symbols = LoadIgnoredSymbols(ignore_symbol_list);
  }

  DiffPolicyOptions diff_policy_options(consider_opaque_types_different);

  HeaderAbiDiff judge(lib_name, arch, old_dump, new_dump, compatibility_report,
                      ignored_symbols, allow_adding_removing_weak_symbols,
                      diff_policy_options, check_all_apis, text_format_old,
                      text_format_new, text_format_diff);

  CompatibilityStatusIR status = judge.GenerateCompatibilityReport();

  std::string status_str = "";
  std::string unreferenced_change_str = "";
  std::string error_or_warning_str = kWarn;

  switch (status) {
    case CompatibilityStatusIR::Incompatible:
      error_or_warning_str = kError;
      status_str = "INCOMPATIBLE CHANGES";
      break;
    case CompatibilityStatusIR::ElfIncompatible:
      if (elf_unreferenced_symbol_errors) {
        error_or_warning_str = kError;
      }
      status_str = "ELF Symbols not referenced by exported headers removed";
      break;
    default:
      break;
  }
  if (status & CompatibilityStatusIR::Extension) {
    if (!allow_extensions) {
      error_or_warning_str = kError;
    }
    status_str = "EXTENDING CHANGES";
  }
  if (status & CompatibilityStatusIR::UnreferencedChanges) {
    unreferenced_change_str = ", changes in exported headers, which are";
    unreferenced_change_str += " not directly referenced by exported symbols.";
    unreferenced_change_str += " This MIGHT be an ABI breaking change due to";
    unreferenced_change_str += " internal typecasts.";
  }

  bool should_emit_warning_message = ShouldEmitWarningMessage(status);

  if (should_emit_warning_message) {
    llvm::errs() << "******************************************************\n"
                 << error_or_warning_str
                 << "VNDK library: "
                 << lib_name
                 << "'s ABI has "
                 << status_str
                 << unreferenced_change_str
                 << " Please check compatibility report at: "
                 << compatibility_report << "\n"
                 << "******************************************************\n";
  }

  if (!advice_only && should_emit_warning_message) {
    return status;
  }

  return CompatibilityStatusIR::Compatible;
}

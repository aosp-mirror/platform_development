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
using header_checker::utils::ConfigSection;


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
    llvm::cl::desc("This option is deprecated and has no effect."),
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

static llvm::cl::opt<std::string> target_version(
    "target-version",
    llvm::cl::desc(
      "Load the flags for <target version> and <lib name> from config.json in "
      "the old dump's parent directory."
    ),
    llvm::cl::init("current"), llvm::cl::Optional,
    llvm::cl::cat(header_checker_category));

static llvm::cl::list<std::string> ignore_linker_set_keys(
    "ignore-linker-set-key",
    llvm::cl::desc("Ignore a specific type or function in the comparison."),
    llvm::cl::ZeroOrMore, llvm::cl::cat(header_checker_category));

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
  llvm::sys::path::append(config_file_path, "config.json");
  return std::string(config_file_path);
}

static void UpdateFlags(const ConfigSection &section) {
  for (auto &&i : section.GetIgnoredLinkerSetKeys()) {
    ignore_linker_set_keys.push_back(i);
  }
  for (auto &&p : section) {
    auto &&key = p.first;
    bool value_bool = p.second;
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

static void ReadConfigFile(const std::string &config_file_path) {
  ConfigFile cfg;
  if (!cfg.Load(config_file_path)) {
    ::exit(1);
  }
  if (cfg.HasGlobalSection()) {
    UpdateFlags(cfg.GetGlobalSection());
  }
  if (cfg.HasSection(lib_name, target_version)) {
    UpdateFlags(cfg.GetSection(lib_name, target_version));
  }
}

static std::string GetErrorMessage(CompatibilityStatusIR status) {
  if (status & CompatibilityStatusIR::Incompatible) {
    return "INCOMPATIBLE CHANGES";
  }
  if (!allow_unreferenced_elf_symbol_changes &&
      (status & CompatibilityStatusIR::ElfIncompatible)) {
    return "ELF Symbols not referenced by exported headers removed";
  }
  if (!allow_extensions && (status & CompatibilityStatusIR::Extension)) {
    return "EXTENDING CHANGES";
  }
  if (!allow_unreferenced_changes &&
      (status & CompatibilityStatusIR::UnreferencedChanges)) {
    return "changes in exported headers, which are not directly referenced "
           "by exported symbols. This MIGHT be an ABI breaking change due to "
           "internal typecasts";
  }
  return "";
}

int main(int argc, const char **argv) {
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-checker");

  const std::string config_file_path = GetConfigFilePath(old_dump);
  if (llvm::sys::fs::exists(config_file_path)) {
    ReadConfigFile(config_file_path);
  }

  std::set<std::string> ignored_symbols;
  if (llvm::sys::fs::exists(ignore_symbol_list)) {
    ignored_symbols = LoadIgnoredSymbols(ignore_symbol_list);
  }

  std::set<std::string> ignored_linker_set_keys_set(
      ignore_linker_set_keys.begin(), ignore_linker_set_keys.end());

  DiffPolicyOptions diff_policy_options(consider_opaque_types_different);

  HeaderAbiDiff judge(lib_name, arch, old_dump, new_dump, compatibility_report,
                      ignored_symbols, ignored_linker_set_keys_set,
                      allow_adding_removing_weak_symbols, diff_policy_options,
                      check_all_apis, text_format_old, text_format_new,
                      text_format_diff);

  CompatibilityStatusIR status = judge.GenerateCompatibilityReport();

  std::string status_str = GetErrorMessage(status);
  if (!status_str.empty()) {
    llvm::errs() << "******************************************************\n"
                 << "\033[31;1merror: \033[0m"
                 << lib_name
                 << "'s ABI has "
                 << status_str
                 << ". Please check compatibility report at: "
                 << compatibility_report << "\n"
                 << "******************************************************\n";
  }

  return (advice_only || status_str.empty()) ? CompatibilityStatusIR::Compatible
                                             : status;
}

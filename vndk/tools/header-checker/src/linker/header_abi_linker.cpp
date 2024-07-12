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

#include "linker/module_merger.h"
#include "repr/ir_dumper.h"
#include "repr/ir_reader.h"
#include "repr/ir_representation.h"
#include "repr/symbol/so_file_parser.h"
#include "repr/symbol/version_script_parser.h"
#include "utils/command_line_utils.h"
#include "utils/source_path_utils.h"

#include <llvm/Support/CommandLine.h>
#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <functional>
#include <iostream>
#include <memory>
#include <optional>
#include <string>
#include <thread>
#include <vector>

#include <stdlib.h>


using namespace header_checker;
using header_checker::repr::ModeTagPolicy;
using header_checker::repr::TextFormatIR;
using header_checker::utils::CollectAllExportedHeaders;
using header_checker::utils::HideIrrelevantCommandLineOptions;
using header_checker::utils::ParseRootDirs;
using header_checker::utils::RootDir;


static llvm::cl::OptionCategory header_linker_category(
    "header-abi-linker options");

static llvm::cl::list<std::string> dump_files(
    llvm::cl::Positional, llvm::cl::desc("<dump-files>"), llvm::cl::ZeroOrMore,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> linked_dump(
    "o", llvm::cl::desc("<linked dump>"), llvm::cl::Required,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> exported_header_dirs(
    "I", llvm::cl::desc("<export_include_dirs>"), llvm::cl::Prefix,
    llvm::cl::ZeroOrMore, llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> root_dirs(
    "root-dir",
    llvm::cl::desc("Specify the directory that the paths in the dump files "
                   "are relative to. The format is <path>:<replacement> or "
                   "<path>. If this option is not specified, it defaults to "
                   "current working directory."),
    llvm::cl::ZeroOrMore, llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> version_script(
    "v", llvm::cl::desc("<version_script>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> excluded_symbol_versions(
    "exclude-symbol-version",
    llvm::cl::desc("Specify the glob patterns of the version blocks to be "
                   "excluded."),
    llvm::cl::Optional, llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> excluded_symbol_tags(
    "exclude-symbol-tag", llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> included_symbol_tags(
    "include-symbol-tag",
    llvm::cl::desc("Filter the symbols in the version script by mode tag, "
                   "such as llndk, apex, and systemapi. The format is "
                   "<tag>=<level> or <tag>. If this option is not specified, "
                   "all mode tags are included."),
    llvm::cl::Optional, llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> api(
    "api",
    llvm::cl::desc("Filter the symbols in the version script by comparing "
                   "\"introduced\" tags and the specified API level."),
    llvm::cl::Optional, llvm::cl::init("current"),
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> api_map(
    "api-map",
    llvm::cl::desc("Specify the path to the json file that maps codenames to "
                   "API levels."),
    llvm::cl::Optional, llvm::cl::cat(header_linker_category));

static llvm::cl::opt<ModeTagPolicy> symbol_tag_policy(
    "symbol-tag-policy",
    llvm::cl::desc("Specify how to match -include-symbol-tag."),
    llvm::cl::values(clEnumValN(ModeTagPolicy::MatchTagAndApi, "MatchTagAndApi",
                                "If a symbol has mode tags, match both the "
                                "mode tags and the \"introduced\" tag."),
                     clEnumValN(ModeTagPolicy::MatchTagOnly, "MatchTagOnly",
                                "If a symbol has mode tags, match the mode "
                                "tags and ignore the \"introduced\" tag.")),
    llvm::cl::init(ModeTagPolicy::MatchTagAndApi),
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> arch(
    "arch", llvm::cl::desc("<arch>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<bool> no_filter(
    "no-filter", llvm::cl::desc("Do not filter any abi"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> so_file(
    "so", llvm::cl::desc("<path to so file>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<TextFormatIR> input_format(
    "input-format", llvm::cl::desc("Specify format of input dump files"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(TextFormatIR::Json),
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<TextFormatIR> output_format(
    "output-format", llvm::cl::desc("Specify format of output dump file"),
    llvm::cl::values(clEnumValN(TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(TextFormatIR::Json),
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::size_t> sources_per_thread(
    "sources-per-thread",
    llvm::cl::desc("Specify number of input dump files each thread parses, for "
                   "debugging merging types"),
    llvm::cl::init(7), llvm::cl::Hidden);

class HeaderAbiLinker {
 public:
  HeaderAbiLinker(const std::vector<std::string> &dump_files,
                  const std::vector<std::string> &exported_header_dirs,
                  repr::VersionScriptParser &version_script_parser,
                  const std::string &version_script, const std::string &so_file,
                  const std::string &linked_dump)
      : dump_files_(dump_files),
        exported_header_dirs_(exported_header_dirs),
        version_script_parser_(version_script_parser),
        version_script_(version_script),
        so_file_(so_file),
        out_dump_name_(linked_dump) {}

  bool LinkAndDump();

 private:
  template <typename T>
  bool LinkDecl(repr::ModuleIR *dst,
                const repr::AbiElementMap<T> &src,
                const std::function<bool(const std::string &)> &symbol_filter);

  std::unique_ptr<linker::ModuleMerger> ReadInputDumpFiles();

  bool ReadExportedSymbols();

  bool ReadExportedSymbolsFromVersionScript();

  bool ReadExportedSymbolsFromSharedObjectFile();

  bool LinkTypes(const repr::ModuleIR &module, repr::ModuleIR *linked_module);

  bool LinkFunctions(const repr::ModuleIR &module,
                     repr::ModuleIR *linked_module);

  bool LinkGlobalVars(const repr::ModuleIR &module,
                      repr::ModuleIR *linked_module);

  bool LinkExportedSymbols(repr::ModuleIR *linked_module);

  bool LinkExportedSymbols(repr::ModuleIR *linked_module,
                           const repr::ExportedSymbolSet &exported_symbols);

  template <typename SymbolMap>
  bool LinkExportedSymbols(repr::ModuleIR *linked_module,
                           const SymbolMap &symbols);

  // Check whether a symbol name is considered as exported.  If both
  // `shared_object_symbols_` and `version_script_symbols_` exists, the symbol
  // name must pass the `HasSymbol()` test in both cases.
  bool IsSymbolExported(const std::string &name) const;

 private:
  const std::vector<std::string> &dump_files_;
  const std::vector<std::string> &exported_header_dirs_;
  repr::VersionScriptParser &version_script_parser_;
  const std::string &version_script_;
  const std::string &so_file_;
  const std::string &out_dump_name_;

  std::set<std::string> exported_headers_;

  // Exported symbols
  std::unique_ptr<repr::ExportedSymbolSet> shared_object_symbols_;

  std::unique_ptr<repr::ExportedSymbolSet> version_script_symbols_;
};

static void DeDuplicateAbiElementsThread(
    std::vector<std::string>::const_iterator dump_files_begin,
    std::vector<std::string>::const_iterator dump_files_end,
    const std::set<std::string> *exported_headers,
    linker::ModuleMerger *merger) {
  for (auto it = dump_files_begin; it != dump_files_end; it++) {
    std::unique_ptr<repr::IRReader> reader = repr::IRReader::CreateIRReader(
        input_format, std::make_unique<repr::ModuleIR>(exported_headers));
    if (reader == nullptr) {
      llvm::errs() << "Failed to create IRReader for " << input_format << "\n";
      ::exit(1);
    }
    if (!reader->ReadDump(*it)) {
      llvm::errs() << "ReadDump failed\n";
      ::exit(1);
    }
    merger->MergeGraphs(reader->GetModule());
  }
}

std::unique_ptr<linker::ModuleMerger> HeaderAbiLinker::ReadInputDumpFiles() {
  std::unique_ptr<linker::ModuleMerger> merger(
      new linker::ModuleMerger(&exported_headers_));
  std::size_t max_threads = std::thread::hardware_concurrency();
  std::size_t num_threads = std::max<std::size_t>(
      std::min(dump_files_.size() / sources_per_thread, max_threads), 1);
  std::vector<std::thread> threads;
  std::vector<linker::ModuleMerger> thread_mergers;
  thread_mergers.reserve(num_threads - 1);

  std::size_t dump_files_index = 0;
  std::size_t first_end_index = 0;
  for (std::size_t i = 0; i < num_threads; i++) {
    std::size_t cnt = dump_files_.size() / num_threads +
                      (i < dump_files_.size() % num_threads ? 1 : 0);
    if (i == 0) {
      first_end_index = cnt;
    } else {
      thread_mergers.emplace_back(&exported_headers_);
      threads.emplace_back(DeDuplicateAbiElementsThread,
                           dump_files_.begin() + dump_files_index,
                           dump_files_.begin() + dump_files_index + cnt,
                           &exported_headers_, &thread_mergers.back());
    }
    dump_files_index += cnt;
  }
  assert(dump_files_index == dump_files_.size());

  DeDuplicateAbiElementsThread(dump_files_.begin(),
                               dump_files_.begin() + first_end_index,
                               &exported_headers_, merger.get());

  for (std::size_t i = 0; i < threads.size(); i++) {
    threads[i].join();
    merger->MergeGraphs(thread_mergers[i].GetModule());
  }

  return merger;
}

bool HeaderAbiLinker::LinkAndDump() {
  // Extract exported functions and variables from a shared lib or a version
  // script.
  if (!ReadExportedSymbols()) {
    return false;
  }

  // Construct the list of exported headers for source location filtering.
  exported_headers_ = CollectAllExportedHeaders(exported_header_dirs_,
                                                ParseRootDirs(root_dirs));

  // Read all input ABI dumps.
  auto merger = ReadInputDumpFiles();

  const repr::ModuleIR &module = merger->GetModule();

  // Link input ABI dumps.
  std::unique_ptr<repr::ModuleIR> linked_module(
      new repr::ModuleIR(&exported_headers_));

  if (!LinkExportedSymbols(linked_module.get())) {
    return false;
  }

  if (!LinkTypes(module, linked_module.get()) ||
      !LinkFunctions(module, linked_module.get()) ||
      !LinkGlobalVars(module, linked_module.get())) {
    llvm::errs() << "Failed to link elements\n";
    return false;
  }

  // Dump the linked module.
  std::unique_ptr<repr::IRDumper> ir_dumper =
      repr::IRDumper::CreateIRDumper(output_format, out_dump_name_);
  assert(ir_dumper != nullptr);
  if (!ir_dumper->Dump(*linked_module)) {
    llvm::errs() << "Failed to serialize the linked output to ostream\n";
    return false;
  }

  return true;
}

template <typename T>
bool HeaderAbiLinker::LinkDecl(
    repr::ModuleIR *dst, const repr::AbiElementMap<T> &src,
    const std::function<bool(const std::string &)> &symbol_filter) {
  assert(dst != nullptr);
  for (auto &&element : src) {
    // If we are not using a version script and exported headers are available,
    // filter out unexported abi.
    std::string source_file = element.second.GetSourceFile();
    // Builtin types will not have source file information.
    if (!exported_headers_.empty() && !source_file.empty() &&
        exported_headers_.find(source_file) == exported_headers_.end()) {
      continue;
    }
    // Check for the existence of the element in version script / symbol file.
    if (!symbol_filter(element.first)) {
      continue;
    }
    if (!dst->AddLinkableMessage(element.second)) {
      llvm::errs() << "Failed to add element to linked dump\n";
      return false;
    }
  }
  return true;
}

bool HeaderAbiLinker::LinkTypes(const repr::ModuleIR &module,
                                repr::ModuleIR *linked_module) {
  auto no_filter = [](const std::string &symbol) { return true; };
  return LinkDecl(linked_module, module.GetRecordTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetEnumTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetFunctionTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetBuiltinTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetPointerTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetRvalueReferenceTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetLvalueReferenceTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetArrayTypes(), no_filter) &&
         LinkDecl(linked_module, module.GetQualifiedTypes(), no_filter);
}

bool HeaderAbiLinker::IsSymbolExported(const std::string &name) const {
  if (shared_object_symbols_ && !shared_object_symbols_->HasSymbol(name)) {
    return false;
  }
  if (version_script_symbols_ && !version_script_symbols_->HasSymbol(name)) {
    return false;
  }
  return true;
}

bool HeaderAbiLinker::LinkFunctions(const repr::ModuleIR &module,
                                    repr::ModuleIR *linked_module) {
  auto symbol_filter = [this](const std::string &linker_set_key) {
    return IsSymbolExported(linker_set_key);
  };
  return LinkDecl(linked_module, module.GetFunctions(), symbol_filter);
}

bool HeaderAbiLinker::LinkGlobalVars(const repr::ModuleIR &module,
                                     repr::ModuleIR *linked_module) {
  auto symbol_filter = [this](const std::string &linker_set_key) {
    return IsSymbolExported(linker_set_key);
  };
  return LinkDecl(linked_module, module.GetGlobalVariables(), symbol_filter);
}

template <typename SymbolMap>
bool HeaderAbiLinker::LinkExportedSymbols(repr::ModuleIR *dst,
                                          const SymbolMap &symbols) {
  for (auto &&symbol : symbols) {
    if (!IsSymbolExported(symbol.first)) {
      continue;
    }
    if (!dst->AddElfSymbol(symbol.second)) {
      return false;
    }
  }
  return true;
}

bool HeaderAbiLinker::LinkExportedSymbols(
    repr::ModuleIR *linked_module,
    const repr::ExportedSymbolSet &exported_symbols) {
  return (LinkExportedSymbols(linked_module, exported_symbols.GetFunctions()) &&
          LinkExportedSymbols(linked_module, exported_symbols.GetVars()));
}

bool HeaderAbiLinker::LinkExportedSymbols(repr::ModuleIR *linked_module) {
  if (shared_object_symbols_) {
    return LinkExportedSymbols(linked_module, *shared_object_symbols_);
  }

  if (version_script_symbols_) {
    return LinkExportedSymbols(linked_module, *version_script_symbols_);
  }

  return false;
}

bool HeaderAbiLinker::ReadExportedSymbols() {
  if (so_file_.empty() && version_script_.empty()) {
    llvm::errs() << "Either shared lib or version script must be specified.\n";
    return false;
  }

  if (!so_file_.empty()) {
    if (!ReadExportedSymbolsFromSharedObjectFile()) {
      llvm::errs() << "Failed to parse the shared library (.so file): "
                   << so_file_ << "\n";
      return false;
    }
  }

  if (!version_script_.empty()) {
    if (!ReadExportedSymbolsFromVersionScript()) {
      llvm::errs() << "Failed to parse the version script: " << version_script_
                   << "\n";
      return false;
    }
  }

  return true;
}

bool HeaderAbiLinker::ReadExportedSymbolsFromVersionScript() {
  std::ifstream stream(version_script_, std::ios_base::in);
  if (!stream) {
    llvm::errs() << "Failed to open version script file\n";
    return false;
  }

  version_script_symbols_ = version_script_parser_.Parse(stream);
  if (!version_script_symbols_) {
    llvm::errs() << "Failed to parse version script file\n";
    return false;
  }

  return true;
}

bool HeaderAbiLinker::ReadExportedSymbolsFromSharedObjectFile() {
  std::unique_ptr<repr::SoFileParser> so_parser =
      repr::SoFileParser::Create(so_file_);
  if (!so_parser) {
    return false;
  }

  shared_object_symbols_ = so_parser->Parse();
  if (!shared_object_symbols_) {
    llvm::errs() << "Failed to parse shared object file\n";
    return false;
  }

  return true;
}

static bool InitializeVersionScriptParser(repr::VersionScriptParser &parser) {
  utils::ApiLevelMap api_level_map;
  if (!api_map.empty()) {
    std::ifstream stream(api_map);
    if (!stream) {
      llvm::errs() << "Failed to open " << api_map << "\n";
      return false;
    }
    if (!api_level_map.Load(stream)) {
      llvm::errs() << "Failed to load " << api_map << "\n";
      return false;
    }
  }

  std::optional<utils::ApiLevel> api_level = api_level_map.Parse(api);
  if (!api_level) {
    llvm::errs() << "-api must be \"current\", an integer, or a codename in "
                    "-api-map\n";
    return false;
  }

  parser.SetArch(arch);
  parser.SetApiLevel(api_level.value());
  parser.SetApiLevelMap(api_level_map);
  for (auto &&version : excluded_symbol_versions) {
    parser.AddExcludedSymbolVersion(version);
  }
  for (auto &&tag : excluded_symbol_tags) {
    parser.AddExcludedSymbolTag(tag);
  }
  for (auto &&tag : included_symbol_tags) {
    if (!parser.AddModeTag(tag)) {
      llvm::errs() << "Failed to parse -include-symbol-tag " << tag << "\n";
      return false;
    }
  }
  parser.SetModeTagPolicy(symbol_tag_policy);

  return true;
}

int main(int argc, const char **argv) {
  HideIrrelevantCommandLineOptions(header_linker_category);
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-linker");

  if (so_file.empty() && version_script.empty()) {
    llvm::errs() << "One of -so or -v needs to be specified\n";
    return -1;
  }

  repr::VersionScriptParser version_script_parser;
  if (!InitializeVersionScriptParser(version_script_parser)) {
    return -1;
  }

  if (no_filter) {
    static_cast<std::vector<std::string> &>(exported_header_dirs).clear();
  }

  HeaderAbiLinker Linker(dump_files, exported_header_dirs,
                         version_script_parser, version_script, so_file,
                         linked_dump);

  if (!Linker.LinkAndDump()) {
    llvm::errs() << "Failed to link and dump elements\n";
    return -1;
  }

  return 0;
}

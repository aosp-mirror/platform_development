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

#include "header_abi_util.h"
#include "ir_representation.h"
#include "so_file_parser.h"
#include "version_script_parser.h"

#include <llvm/Support/CommandLine.h>
#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <functional>
#include <iostream>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include <stdlib.h>

static constexpr std::size_t kSourcesPerBatchThread = 7;

static llvm::cl::OptionCategory header_linker_category(
    "header-abi-linker options");

static llvm::cl::list<std::string> dump_files(
    llvm::cl::Positional, llvm::cl::desc("<dump-files>"), llvm::cl::Required,
    llvm::cl::cat(header_linker_category), llvm::cl::OneOrMore);

static llvm::cl::opt<std::string> linked_dump(
    "o", llvm::cl::desc("<linked dump>"), llvm::cl::Required,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> exported_header_dirs(
    "I", llvm::cl::desc("<export_include_dirs>"), llvm::cl::Prefix,
    llvm::cl::ZeroOrMore, llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> version_script(
    "v", llvm::cl::desc("<version_script>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> excluded_symbol_versions(
    "exclude-symbol-version", llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::list<std::string> excluded_symbol_tags(
    "exclude-symbol-tag", llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> api(
    "api", llvm::cl::desc("<api>"), llvm::cl::Optional,
    llvm::cl::init("current"),
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

static llvm::cl::opt<abi_util::TextFormatIR> input_format(
    "input-format", llvm::cl::desc("Specify format of input dump files"),
    llvm::cl::values(clEnumValN(abi_util::TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(abi_util::TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(abi_util::TextFormatIR::Json),
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<abi_util::TextFormatIR> output_format(
    "output-format", llvm::cl::desc("Specify format of output dump file"),
    llvm::cl::values(clEnumValN(abi_util::TextFormatIR::ProtobufTextFormat,
                                "ProtobufTextFormat", "ProtobufTextFormat"),
                     clEnumValN(abi_util::TextFormatIR::Json, "Json", "JSON")),
    llvm::cl::init(abi_util::TextFormatIR::Json),
    llvm::cl::cat(header_linker_category));

class HeaderAbiLinker {
 public:
  HeaderAbiLinker(
      const std::vector<std::string> &dump_files,
      const std::vector<std::string> &exported_header_dirs,
      const std::string &version_script,
      const std::string &so_file,
      const std::string &linked_dump,
      const std::string &arch,
      const std::string &api,
      const std::vector<std::string> &excluded_symbol_versions,
      const std::vector<std::string> &excluded_symbol_tags)
      : dump_files_(dump_files), exported_header_dirs_(exported_header_dirs),
        version_script_(version_script), so_file_(so_file),
        out_dump_name_(linked_dump), arch_(arch), api_(api),
        excluded_symbol_versions_(excluded_symbol_versions),
        excluded_symbol_tags_(excluded_symbol_tags) {}

  bool LinkAndDump();

 private:
  template <typename T>
  bool LinkDecl(abi_util::IRDumper *dst,
                const abi_util::AbiElementMap<T> &src,
                const std::function<bool(const std::string &)> &symbol_filter);

  std::unique_ptr<abi_util::TextFormatToIRReader> ReadInputDumpFiles();

  bool ReadExportedSymbols();

  bool ReadExportedSymbolsFromVersionScript();

  bool ReadExportedSymbolsFromSharedObjectFile();

  bool LinkTypes(const abi_util::TextFormatToIRReader *ir_reader,
                 abi_util::IRDumper *ir_dumper);

  bool LinkFunctions(const abi_util::TextFormatToIRReader *ir_reader,
                     abi_util::IRDumper *ir_dumper);

  bool LinkGlobalVars(const abi_util::TextFormatToIRReader *ir_reader,
                      abi_util::IRDumper *ir_dumper);

  bool LinkExportedSymbols(abi_util::IRDumper *ir_dumper);

  bool LinkExportedSymbols(abi_util::IRDumper *ir_dumper,
                           const abi_util::ExportedSymbolSet &exported_symbols);

  template <typename SymbolMap>
  bool LinkExportedSymbols(abi_util::IRDumper *ir_dumper,
                           const SymbolMap &symbols);

  // Check whether a symbol name is considered as exported.  If both
  // `shared_object_symbols_` and `version_script_symbols_` exists, the symbol
  // name must pass the `HasSymbol()` test in both cases.
  bool IsSymbolExported(const std::string &name) const;

 private:
  const std::vector<std::string> &dump_files_;
  const std::vector<std::string> &exported_header_dirs_;
  const std::string &version_script_;
  const std::string &so_file_;
  const std::string &out_dump_name_;
  const std::string &arch_;
  const std::string &api_;
  const std::vector<std::string> &excluded_symbol_versions_;
  const std::vector<std::string> &excluded_symbol_tags_;

  std::set<std::string> exported_headers_;

  // Exported symbols
  std::unique_ptr<abi_util::ExportedSymbolSet> shared_object_symbols_;
  std::unique_ptr<abi_util::ExportedSymbolSet> version_script_symbols_;
};

static void DeDuplicateAbiElementsThread(
    const std::vector<std::string> &dump_files,
    const std::set<std::string> *exported_headers,
    abi_util::TextFormatToIRReader *greader, std::mutex *greader_lock,
    std::atomic<std::size_t> *cnt) {
  std::unique_ptr<abi_util::TextFormatToIRReader> local_reader =
      abi_util::TextFormatToIRReader::CreateTextFormatToIRReader(
          input_format, exported_headers);

  auto begin_it = dump_files.begin();
  std::size_t num_sources = dump_files.size();
  while (1) {
    std::size_t i = cnt->fetch_add(kSourcesPerBatchThread);
    if (i >= num_sources) {
      break;
    }
    std::size_t end = std::min(i + kSourcesPerBatchThread, num_sources);
    for (auto it = begin_it; it != begin_it + end; it++) {
      std::unique_ptr<abi_util::TextFormatToIRReader> reader =
          abi_util::TextFormatToIRReader::CreateTextFormatToIRReader(
              input_format, exported_headers);
      assert(reader != nullptr);
      if (!reader->ReadDump(*it)) {
        llvm::errs() << "ReadDump failed\n";
        ::exit(1);
      }
      // This merge is needed since the iterators might not be contigous.
      local_reader->MergeGraphs(*reader);
    }
  }

  std::lock_guard<std::mutex> lock(*greader_lock);
  greader->MergeGraphs(*local_reader);
}

std::unique_ptr<abi_util::TextFormatToIRReader>
HeaderAbiLinker::ReadInputDumpFiles() {
  std::unique_ptr<abi_util::TextFormatToIRReader> greader =
      abi_util::TextFormatToIRReader::CreateTextFormatToIRReader(
          input_format, &exported_headers_);

  std::size_t max_threads = std::thread::hardware_concurrency();
  std::size_t num_threads = kSourcesPerBatchThread < dump_files_.size() ?
      std::min(dump_files_.size() / kSourcesPerBatchThread, max_threads) : 0;
  std::vector<std::thread> threads;
  std::atomic<std::size_t> cnt(0);
  std::mutex greader_lock;
  for (std::size_t i = 1; i < num_threads; i++) {
    threads.emplace_back(DeDuplicateAbiElementsThread, dump_files_,
                         &exported_headers_, greader.get(), &greader_lock,
                         &cnt);
  }
  DeDuplicateAbiElementsThread(dump_files_, &exported_headers_, greader.get(),
                               &greader_lock, &cnt);
  for (auto &thread : threads) {
    thread.join();
  }

  return greader;
}

bool HeaderAbiLinker::LinkAndDump() {
  // Extract exported functions and variables from a shared lib or a version
  // script.
  if (!ReadExportedSymbols()) {
    return false;
  }

  // Construct the list of exported headers for source location filtering.
  exported_headers_ =
      abi_util::CollectAllExportedHeaders(exported_header_dirs_);

  // Read all input ABI dumps.
  auto greader = ReadInputDumpFiles();

  // Link input ABI dumps.
  std::unique_ptr<abi_util::IRDumper> ir_dumper =
      abi_util::IRDumper::CreateIRDumper(output_format, out_dump_name_);
  assert(ir_dumper != nullptr);

  if (!LinkExportedSymbols(ir_dumper.get())) {
    return false;
  }

  if (!LinkTypes(greader.get(), ir_dumper.get()) ||
      !LinkFunctions(greader.get(), ir_dumper.get()) ||
      !LinkGlobalVars(greader.get(), ir_dumper.get())) {
    llvm::errs() << "Failed to link elements\n";
    return false;
  }

  if (!ir_dumper->Dump()) {
    llvm::errs() << "Failed to serialize the linked output to ostream\n";
    return false;
  }

  return true;
}

template <typename T>
bool HeaderAbiLinker::LinkDecl(
    abi_util::IRDumper *dst, const abi_util::AbiElementMap<T> &src,
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
    if (!dst->AddLinkableMessageIR(&(element.second))) {
      llvm::errs() << "Failed to add element to linked dump\n";
      return false;
    }
  }
  return true;
}

bool HeaderAbiLinker::LinkTypes(const abi_util::TextFormatToIRReader *reader,
                                abi_util::IRDumper *ir_dumper) {
  assert(reader != nullptr);
  auto no_filter = [](const std::string &symbol) { return true; };
  return LinkDecl(ir_dumper, reader->GetRecordTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetEnumTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetFunctionTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetBuiltinTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetPointerTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetRvalueReferenceTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetLvalueReferenceTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetArrayTypes(), no_filter) &&
         LinkDecl(ir_dumper, reader->GetQualifiedTypes(), no_filter);
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

bool HeaderAbiLinker::LinkFunctions(
    const abi_util::TextFormatToIRReader *reader,
    abi_util::IRDumper *ir_dumper) {
  assert(reader != nullptr);
  auto symbol_filter = [this](const std::string &linker_set_key) {
    return IsSymbolExported(linker_set_key);
  };
  return LinkDecl(ir_dumper, reader->GetFunctions(), symbol_filter);
}

bool HeaderAbiLinker::LinkGlobalVars(
    const abi_util::TextFormatToIRReader *reader,
    abi_util::IRDumper *ir_dumper) {
  assert(reader != nullptr);
  auto symbol_filter = [this](const std::string &linker_set_key) {
    return IsSymbolExported(linker_set_key);
  };
  return LinkDecl(ir_dumper, reader->GetGlobalVariables(), symbol_filter);
}

template <typename SymbolMap>
bool HeaderAbiLinker::LinkExportedSymbols(abi_util::IRDumper *dst,
                                          const SymbolMap &symbols) {
  for (auto &&symbol : symbols) {
    if (!IsSymbolExported(symbol.first)) {
      continue;
    }
    if (!dst->AddElfSymbolMessageIR(&(symbol.second))) {
      return false;
    }
  }
  return true;
}

bool HeaderAbiLinker::LinkExportedSymbols(
    abi_util::IRDumper *ir_dumper,
    const abi_util::ExportedSymbolSet &exported_symbols) {
  return (LinkExportedSymbols(ir_dumper, exported_symbols.GetFunctions()) &&
          LinkExportedSymbols(ir_dumper, exported_symbols.GetVars()));
}

bool HeaderAbiLinker::LinkExportedSymbols(abi_util::IRDumper *ir_dumper) {
  if (shared_object_symbols_) {
    return LinkExportedSymbols(ir_dumper, *shared_object_symbols_);
  }

  if (version_script_symbols_) {
    return LinkExportedSymbols(ir_dumper, *version_script_symbols_);
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
  std::optional<abi_util::ApiLevel> api_level = abi_util::ParseApiLevel(api_);
  if (!api_level) {
    llvm::errs() << "-api must be either \"current\" or an integer (e.g. 21)\n";
    return false;
  }

  std::ifstream stream(version_script_, std::ios_base::in);
  if (!stream) {
    llvm::errs() << "Failed to open version script file\n";
    return false;
  }

  abi_util::VersionScriptParser parser;
  parser.SetArch(arch_);
  parser.SetApiLevel(api_level.value());
  for (auto &&version : excluded_symbol_versions_) {
    parser.AddExcludedSymbolVersion(version);
  }
  for (auto &&tag : excluded_symbol_tags_) {
    parser.AddExcludedSymbolTag(tag);
  }

  version_script_symbols_ = parser.Parse(stream);
  if (!version_script_symbols_) {
    llvm::errs() << "Failed to parse version script file\n";
    return false;
  }

  return true;
}

bool HeaderAbiLinker::ReadExportedSymbolsFromSharedObjectFile() {
  std::unique_ptr<abi_util::SoFileParser> so_parser =
      abi_util::SoFileParser::Create(so_file_);
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

// Hide irrelevant command line options defined in LLVM libraries.
static void HideIrrelevantCommandLineOptions() {
  llvm::StringMap<llvm::cl::Option *> &map = llvm::cl::getRegisteredOptions();
  for (llvm::StringMapEntry<llvm::cl::Option *> &p : map) {
    if (p.second->Category == &header_linker_category) {
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
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-linker");

  if (so_file.empty() && version_script.empty()) {
    llvm::errs() << "One of -so or -v needs to be specified\n";
    return -1;
  }

  if (no_filter) {
    static_cast<std::vector<std::string> &>(exported_header_dirs).clear();
  }

  HeaderAbiLinker Linker(dump_files, exported_header_dirs, version_script,
                         so_file, linked_dump, arch, api,
                         excluded_symbol_versions,
                         excluded_symbol_tags);

  if (!Linker.LinkAndDump()) {
    llvm::errs() << "Failed to link and dump elements\n";
    return -1;
  }

  return 0;
}

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

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#pragma clang diagnostic pop

#include <header_abi_util.h>

#include <llvm/Support/CommandLine.h>
#include <llvm/Support/raw_ostream.h>

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <memory>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

#include <stdlib.h>

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

static llvm::cl::opt<std::string> api(
    "api", llvm::cl::desc("<api>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<std::string> arch(
    "arch", llvm::cl::desc("<arch>"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

static llvm::cl::opt<bool> no_filter(
    "no-filter", llvm::cl::desc("Do not filter any abi"), llvm::cl::Optional,
    llvm::cl::cat(header_linker_category));

class HeaderAbiLinker {
 public:
  HeaderAbiLinker(
      const std::vector<std::string> &dump_files,
      const std::vector<std::string> &exported_header_dirs,
      const std::string &version_script,
      const std::string &linked_dump,
      const std::string &arch,
      const std::string &api)
    : dump_files_(dump_files), exported_header_dirs_(exported_header_dirs),
    version_script_(version_script), out_dump_name_(linked_dump), arch_(arch),
    api_(api) {};

  bool LinkAndDump();

 private:
  bool LinkRecords(const abi_dump::TranslationUnit &dump_tu,
                   abi_dump::TranslationUnit *linked_tu);

  bool LinkFunctions(const abi_dump::TranslationUnit &dump_tu,
                     abi_dump::TranslationUnit *linked_tu);

  bool LinkEnums(const abi_dump::TranslationUnit &dump_tu,
                 abi_dump::TranslationUnit *linked_tu);

  bool LinkGlobalVars(const abi_dump::TranslationUnit &dump_tu,
                      abi_dump::TranslationUnit *linked_tu);

  template <typename T>
  inline bool LinkDecl(google::protobuf::RepeatedPtrField<T> *dst,
                       std::set<std::string> *link_set,
                       std::set<std::string> *regex_matched_link_set,
                       const std::regex *vs_regex,
                       const google::protobuf::RepeatedPtrField<T> &src,
                       bool use_version_script);

  bool ParseVersionScriptFiles();

 private:
  const std::vector<std::string> &dump_files_;
  const std::vector<std::string> &exported_header_dirs_;
  const std::string &version_script_;
  const std::string &out_dump_name_;
  const std::string &arch_;
  const std::string &api_;
  // TODO: Add to a map of std::sets instead.
  std::set<std::string> exported_headers_;
  std::set<std::string> record_decl_set_;
  std::set<std::string> function_decl_set_;
  std::set<std::string> enum_decl_set_;
  std::set<std::string> globvar_decl_set_;
  // Version Script Regex Matching.
  std::set<std::string> functions_regex_matched_set;
  std::regex functions_vs_regex_;
  // Version Script Regex Matching.
  std::set<std::string> globvars_regex_matched_set;
  std::regex globvars_vs_regex_;
};

bool HeaderAbiLinker::LinkAndDump() {
  abi_dump::TranslationUnit linked_tu;
  std::ofstream text_output(out_dump_name_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  // If a version script is available, we use that as a filter.
  if (version_script.empty()) {
    exported_headers_ =
        abi_util::CollectAllExportedHeaders(exported_header_dirs_);
  } else if (!ParseVersionScriptFiles()) {
    llvm::errs() << "Failed to parse stub files for exported symbols\n";
    return false;
  }

  for (auto &&i : dump_files_) {
    abi_dump::TranslationUnit dump_tu;
    std::ifstream input(i);
    google::protobuf::io::IstreamInputStream text_is(&input);
    if (!google::protobuf::TextFormat::Parse(&text_is, &dump_tu) ||
        !LinkRecords(dump_tu, &linked_tu) ||
        !LinkFunctions(dump_tu, &linked_tu) ||
        !LinkEnums(dump_tu, &linked_tu) ||
        !LinkGlobalVars(dump_tu, &linked_tu)) {
      llvm::errs() << "Failed to link elements\n";
      return false;
    }
  }

  if (!google::protobuf::TextFormat::Print(linked_tu, &text_os)) {
    llvm::errs() << "Serialization to ostream failed\n";
    return false;
  }
  return true;
}

static std::string GetSymbol(const abi_dump::RecordDecl &element) {
  return element.mangled_record_name();
}

static std::string GetSymbol(const abi_dump::FunctionDecl &element) {
  return element.mangled_function_name();
}

static std::string GetSymbol(const abi_dump::EnumDecl &element) {
  return element.basic_abi().linker_set_key();
}

static std::string GetSymbol(const abi_dump::GlobalVarDecl &element) {
  return element.basic_abi().linker_set_key();
}

static bool QueryRegexMatches(std::set<std::string> *regex_matched_link_set,
                              const std::regex *vs_regex,
                              const std::string &symbol) {
  assert(regex_matched_link_set != nullptr);
  assert(vs_regex != nullptr);
  if (regex_matched_link_set->find(symbol) != regex_matched_link_set->end()) {
    return false;
  }
  if (std::regex_search(symbol, *vs_regex)) {
    regex_matched_link_set->insert(symbol);
    return true;
  }
  return false;
}

static std::regex CreateRegexMatchExprFromSet(
    const std::set<std::string> &link_set) {
  std::string all_regex_match_str = "";
  std::set<std::string>::iterator it = link_set.begin();
  while (it != link_set.end()) {
    std::string regex_match_str_find_glob =
      abi_util::FindAndReplace(*it, "\\*", ".*");
    all_regex_match_str += "(\\b" + regex_match_str_find_glob + "\\b)";
    if (++it != link_set.end()) {
      all_regex_match_str += "|";
    }
  }
  if (all_regex_match_str == "") {
    return std::regex();
  }
  return std::regex(all_regex_match_str);
}

template <typename T>
inline bool HeaderAbiLinker::LinkDecl(
    google::protobuf::RepeatedPtrField<T> *dst,
    std::set<std::string> *link_set,
    std::set<std::string> *regex_matched_link_set, const std::regex *vs_regex,
    const google::protobuf::RepeatedPtrField<T> &src, bool use_version_script) {
  assert(dst != nullptr);
  assert(link_set != nullptr);
  for (auto &&element : src) {
    // If we are not using a version script and exported headers are available,
    // filter out unexported abi.
    if (!exported_headers_.empty() &&
        exported_headers_.find(element.source_file()) ==
        exported_headers_.end()) {
      continue;
    }
    // Check for the existence of the element in linked dump / symbol file.
    if (!use_version_script) {
        if (!link_set->insert(element.basic_abi().linker_set_key()).second) {
        continue;
        }
    } else {
      std::string element_str = GetSymbol(element);
      std::set<std::string>::iterator it =
          link_set->find(element_str);
      if (it == link_set->end()) {
        if (!QueryRegexMatches(regex_matched_link_set, vs_regex, element_str)) {
          continue;
        }
      } else {
        // We get a pre-filled link name set while using version script.
        link_set->erase(*it); // Avoid multiple instances of the same symbol.
      }
    }
    T *added_element = dst->Add();
    if (!added_element) {
      llvm::errs() << "Failed to add element to linked dump\n";
      return false;
    }
    *added_element = element;
  }
  return true;
}

bool HeaderAbiLinker::LinkRecords(const abi_dump::TranslationUnit &dump_tu,
                                  abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  // Even if version scripts are available we take in records, since the symbols
  // in the version script might reference a record exposed by the library.
  return LinkDecl(linked_tu->mutable_records(), &record_decl_set_, nullptr,
                  nullptr, dump_tu.records(), false);
}

bool HeaderAbiLinker::LinkFunctions(const abi_dump::TranslationUnit &dump_tu,
                                    abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  return LinkDecl(linked_tu->mutable_functions(), &function_decl_set_,
                  &functions_regex_matched_set, &functions_vs_regex_,
                  dump_tu.functions(), (!version_script_.empty()));
}

bool HeaderAbiLinker::LinkEnums(const abi_dump::TranslationUnit &dump_tu,
                                abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  // Even if version scripts are available we take in records, since the symbols
  // in the version script might reference an enum exposed by the library.
  return LinkDecl(linked_tu->mutable_enums(), &enum_decl_set_, nullptr,
                  nullptr, dump_tu.enums(), false);
}

bool HeaderAbiLinker::LinkGlobalVars(const abi_dump::TranslationUnit &dump_tu,
                                     abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  return LinkDecl(linked_tu->mutable_global_vars(), &globvar_decl_set_,
                  &globvars_regex_matched_set, &globvars_vs_regex_,
                  dump_tu.global_vars(), (!version_script.empty()));
}

bool HeaderAbiLinker::ParseVersionScriptFiles() {
  abi_util::VersionScriptParser version_script_parser(version_script_, arch_,
                                                      api_);
  if (!version_script_parser.Parse()) {
    return false;
  }
  function_decl_set_ = version_script_parser.GetFunctions();
  globvar_decl_set_ = version_script_parser.GetGlobVars();
  std::set<std::string> function_regexs =
      version_script_parser.GetFunctionRegexs();
  std::set<std::string> globvar_regexs =
      version_script_parser.GetGlobVarRegexs();
  functions_vs_regex_ = CreateRegexMatchExprFromSet(function_regexs);
  globvars_vs_regex_ = CreateRegexMatchExprFromSet(globvar_regexs);
  return true;
}

int main(int argc, const char **argv) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-linker");
  if (no_filter) {
    static_cast<std::vector<std::string> &>(exported_header_dirs).clear();
  }
  HeaderAbiLinker Linker(dump_files, exported_header_dirs,
                         version_script, linked_dump, arch, api);
  if (!Linker.LinkAndDump()) {
    return -1;
  }
  return 0;
}

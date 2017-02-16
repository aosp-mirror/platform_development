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

static llvm::cl::OptionCategory header_checker_category(
    "header-abi-linker options");

static llvm::cl::list<std::string> dump_files(
    llvm::cl::Positional, llvm::cl::desc("<dump-files>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category), llvm::cl::OneOrMore);

static llvm::cl::opt<std::string> linked_dump(
    "o", llvm::cl::desc("<linked dump>"), llvm::cl::Required,
    llvm::cl::cat(header_checker_category));

class HeaderAbiLinker {
 public:
  HeaderAbiLinker(
      const std::vector<std::string> &files,
      const std::string &linked_dump)
    : dump_files_(files), out_dump_name_(linked_dump) {};

  bool LinkAndDump();

 private:
  bool LinkRecords(const abi_dump::TranslationUnit &dump_tu,
                   abi_dump::TranslationUnit *linked_tu);

  bool LinkFunctions(const abi_dump::TranslationUnit &dump_tu,
                     abi_dump::TranslationUnit *linked_tu);

  bool LinkEnums(const abi_dump::TranslationUnit &dump_tu,
                 abi_dump::TranslationUnit *linked_tu);

  template <typename T>
  static inline bool LinkDecl(
    google::protobuf::RepeatedPtrField<T> *dst,
    std::set<std::string> *link_set,
    const google::protobuf::RepeatedPtrField<T> &src);

 private:
  const std::vector<std::string> &dump_files_;
  const std::string &out_dump_name_;
  std::set<std::string> record_decl_set_;
  std::set<std::string> function_decl_set_;
  std::set<std::string> enum_decl_set_;
};

bool HeaderAbiLinker::LinkAndDump() {
  abi_dump::TranslationUnit linked_tu;
  std::ofstream text_output(out_dump_name_);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  for (auto &&i : dump_files_) {
    abi_dump::TranslationUnit dump_tu;
    std::ifstream input(i);
    google::protobuf::io::IstreamInputStream text_is(&input);
    if (!google::protobuf::TextFormat::Parse(&text_is, &dump_tu) ||
        !LinkRecords(dump_tu, &linked_tu) ||
        !LinkFunctions(dump_tu, &linked_tu) ||
        !LinkEnums(dump_tu, &linked_tu)) {
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

template <typename T>
inline bool HeaderAbiLinker::LinkDecl(
    google::protobuf::RepeatedPtrField<T> *dst,
    std::set<std::string> *link_set,
    const google::protobuf::RepeatedPtrField<T> &src) {
  assert(dst != nullptr);
  assert(link_set != nullptr);
  for (auto &&element : src) {
    // The element already exists in the linked dump. Skip.
    if (!link_set->insert(element.linker_set_key()).second) {
      continue;
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
  return LinkDecl(linked_tu->mutable_records(), &record_decl_set_,
                  dump_tu.records());
}

bool HeaderAbiLinker::LinkFunctions(const abi_dump::TranslationUnit &dump_tu,
                                    abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  return LinkDecl(linked_tu->mutable_functions(), &function_decl_set_,
                  dump_tu.functions());
}

bool HeaderAbiLinker::LinkEnums(const abi_dump::TranslationUnit &dump_tu,
                                abi_dump::TranslationUnit *linked_tu) {
  assert(linked_tu != nullptr);
  return LinkDecl(linked_tu->mutable_enums(), &enum_decl_set_,
                  dump_tu.enums());
}

int main(int argc, const char **argv) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  llvm::cl::ParseCommandLineOptions(argc, argv, "header-checker");
  HeaderAbiLinker Linker(dump_files, linked_dump);
  if (!Linker.LinkAndDump()) {
    return -1;
  }

  return 0;
}

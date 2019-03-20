// Copyright (C) 2019 The Android Open Source Project
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

#include "repr/ir_dumper.h"

#include "repr/json/api.h"
#include "repr/protobuf/api.h"

#include <algorithm>
#include <memory>
#include <string>
#include <vector>

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace repr {


std::unique_ptr<IRDumper> IRDumper::CreateIRDumper(
    TextFormatIR text_format, const std::string &dump_path) {
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return CreateProtobufIRDumper(dump_path);
    case TextFormatIR::Json:
      return CreateJsonIRDumper(dump_path);
    default:
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}


// TODO: Replace AbiElementMap key with linker_set_key and use the natural
// ordering.
template <typename T>
static std::vector<const T *> SortAbiElements(const AbiElementMap<T> &m) {
  std::vector<const T *> xs;

  xs.reserve(m.size());
  for (auto &&item : m) {
    xs.push_back(&item.second);
  }

  auto &&compare = [](const T *lhs, const T *rhs) {
    return lhs->GetLinkerSetKey() < rhs->GetLinkerSetKey();
  };
  std::stable_sort(xs.begin(), xs.end(), compare);

  return xs;
}


bool IRDumper::DumpModule(const ModuleIR &module) {
  for (auto &&item : SortAbiElements(module.GetFunctions())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetGlobalVariables())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetRecordTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetFunctionTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetEnumTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetLvalueReferenceTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetRvalueReferenceTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetQualifiedTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetArrayTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetPointerTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : SortAbiElements(module.GetBuiltinTypes())) {
    AddLinkableMessageIR(item);
  }
  for (auto &&item : module.GetElfFunctions()) {
    AddElfSymbolMessageIR(&item.second);
  }
  for (auto &&item : module.GetElfObjects()) {
    AddElfSymbolMessageIR(&item.second);
  }
  return true;
}


}  // namespace repr
}  // header_checker

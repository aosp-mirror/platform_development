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

#include "repr/json/converter.h"


namespace header_checker {
namespace repr {


const JsonArray json_empty_array;
const JsonObject json_empty_object;
const Json::Value json_0(0);
const Json::Value json_false(false);
const Json::Value json_empty_string("");


const AccessSpecifierIR default_access_ir = AccessSpecifierIR::PublicAccess;

const RecordTypeIR::RecordKind default_record_kind_ir =
    RecordTypeIR::RecordKind::struct_kind;

const VTableComponentIR::Kind default_vtable_component_kind_ir =
    VTableComponentIR::Kind::FunctionPointer;

const ElfSymbolIR::ElfSymbolBinding default_elf_symbol_binding_ir =
    ElfSymbolIR::ElfSymbolBinding::Global;


void JsonObject::Set(const std::string &key, bool value) {
  SetOmissible(key, value, false);
}


void JsonObject::Set(const std::string &key, uint64_t value) {
  SetOmissible<Json::UInt64>(key, value, 0);
}


void JsonObject::Set(const std::string &key, int64_t value) {
  SetOmissible<Json::Int64>(key, value, 0);
}


void JsonObject::Set(const std::string &key, const std::string &value) {
  SetOmissible<const std::string &>(key, value, "");
}


void JsonObject::Set(const std::string &key, const JsonArray &value) {
  SetOmissible(key, value, json_empty_array);
}


}  // namespace repr
}  // header_checker

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

#ifndef HEADER_CHECKER_REPR_JSON_CONVERTER_H_
#define HEADER_CHECKER_REPR_JSON_CONVERTER_H_

#include "repr/ir_representation.h"

#include <json/value.h>

#include <map>
#include <string>

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace repr {


// These classes wrap constructors of Json::Value.
class JsonArray : public Json::Value {
 public:
  JsonArray() : Json::Value(Json::ValueType::arrayValue) {}
};


class JsonObject : public Json::Value {
 public:
  JsonObject() : Json::Value(Json::ValueType::objectValue) {}

  // This method inserts the key-value pair if the value is not equal to the
  // omissible value.
  // Omit false.
  void Set(const std::string &key, bool value);

  // Omit 0.
  void Set(const std::string &key, uint64_t value);

  // Omit 0.
  void Set(const std::string &key, int64_t value);

  // Omit "".
  void Set(const std::string &key, const std::string &value);

  // Omit [].
  void Set(const std::string &key, const JsonArray &value);

 private:
  template <typename T>
  inline void SetOmissible(const std::string &key, T value, T omissible_value) {
    if (value != omissible_value) {
      (*this)[key] = value;
    } else {
      removeMember(key);
    }
  }
};


extern const JsonArray json_empty_array;
extern const JsonObject json_empty_object;
extern const Json::Value json_0;
extern const Json::Value json_false;
extern const Json::Value json_empty_string;

extern const AccessSpecifierIR default_access_ir;
extern const RecordTypeIR::RecordKind default_record_kind_ir;
extern const VTableComponentIR::Kind default_vtable_component_kind_ir;
extern const ElfSymbolIR::ElfSymbolBinding default_elf_symbol_binding_ir;


// Conversion between IR enums and JSON strings.
static const std::map<AccessSpecifierIR, std::string> access_ir_to_json{
  {AccessSpecifierIR::PublicAccess, "public"},
  {AccessSpecifierIR::ProtectedAccess, "protected"},
  {AccessSpecifierIR::PrivateAccess, "private"},
};

static const std::map<RecordTypeIR::RecordKind, std::string>
    record_kind_ir_to_json{
  {RecordTypeIR::RecordKind::struct_kind, "struct"},
  {RecordTypeIR::RecordKind::class_kind, "class"},
  {RecordTypeIR::RecordKind::union_kind, "union"},
};

static const std::map<VTableComponentIR::Kind, std::string>
    vtable_component_kind_ir_to_json{
  {VTableComponentIR::Kind::VCallOffset, "vcall_offset"},
  {VTableComponentIR::Kind::VBaseOffset, "vbase_offset"},
  {VTableComponentIR::Kind::OffsetToTop, "offset_to_top"},
  {VTableComponentIR::Kind::RTTI, "rtti"},
  {VTableComponentIR::Kind::FunctionPointer, "function_pointer"},
  {VTableComponentIR::Kind::CompleteDtorPointer, "complete_dtor_pointer"},
  {VTableComponentIR::Kind::DeletingDtorPointer, "deleting_dtor_pointer"},
  {VTableComponentIR::Kind::UnusedFunctionPointer, "unused_function_pointer"},
};

static const std::map<ElfSymbolIR::ElfSymbolBinding, std::string>
    elf_symbol_binding_ir_to_json{
  {ElfSymbolIR::ElfSymbolBinding::Weak, "weak"},
  {ElfSymbolIR::ElfSymbolBinding::Global, "global"},
};

// If m contains k, this function returns the value.
// Otherwise, it prints error_msg and exits.
template <typename K, typename V>
static inline const V &FindInMap(const std::map<K, V> &m, const K &k,
                                 const std::string &error_msg) {
  auto it = m.find(k);
  if (it == m.end()) {
    llvm::errs() << error_msg << "\n";
    ::exit(1);
  }
  return it->second;
}


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_JSON_CONVERTER_H_

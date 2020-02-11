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

#ifndef HEADER_CHECKER_REPR_JSON_IR_READER_H_
#define HEADER_CHECKER_REPR_JSON_IR_READER_H_

#include "repr/ir_dumper.h"
#include "repr/ir_reader.h"
#include "repr/ir_representation.h"

#include <json/value.h>


namespace header_checker {
namespace repr {


template <typename T> class JsonArrayRef;

// This class loads values from a read-only JSON object.
class JsonObjectRef {
 public:
  // The constructor sets ok to false if json_value is not an object.
  JsonObjectRef(const Json::Value &json_value, bool &ok);

  // This method gets a value from the object and checks the type.
  // If the type mismatches, it sets ok_ to false and returns default value.
  // If the key doesn't exist, it doesn't change ok_ and returns default value.
  // Default to false.
  bool GetBool(const std::string &key) const;

  // Default to 0.
  int64_t GetInt(const std::string &key) const;

  // Default to 0.
  uint64_t GetUint(const std::string &key) const;

  // Default to "".
  std::string GetString(const std::string &key) const;

  // Default to {}.
  JsonObjectRef GetObject(const std::string &key) const;

  // Default to [].
  JsonArrayRef<JsonObjectRef> GetObjects(const std::string &key) const;

  JsonArrayRef<std::string> GetStrings(const std::string &key) const;

 private:
  typedef bool (Json::Value::*IsExpectedJsonType)() const;

  const Json::Value &Get(const std::string &key,
                         const Json::Value &default_value,
                         IsExpectedJsonType is_expected_type) const;

  const Json::Value &object_;
  bool &ok_;
};

// This class loads elements as type T from a read-only JSON array.
template <typename T> class JsonArrayRef {
 public:
  class Iterator {
   public:
    Iterator(const Json::Value &json_value, bool &ok, int index)
        : array_(json_value), ok_(ok), index_(index) {}

    Iterator &operator++() {
      ++index_;
      return *this;
    }

    bool operator!=(const Iterator &other) const {
      return index_ != other.index_;
    }

    T operator*() const;

   private:
    const Json::Value &array_;
    bool &ok_;
    int index_;
  };

  // The caller ensures json_value.isArray() == true.
  JsonArrayRef(const Json::Value &json_value, bool &ok)
      : array_(json_value), ok_(ok) {}

  Iterator begin() const { return Iterator(array_, ok_, 0); }

  Iterator end() const { return Iterator(array_, ok_, array_.size()); }

 private:
  const Json::Value &array_;
  bool &ok_;
};

template <>
JsonObjectRef JsonArrayRef<JsonObjectRef>::Iterator::operator*() const;

template <> std::string JsonArrayRef<std::string>::Iterator::operator*() const;

class JsonIRReader : public IRReader {
 public:
  JsonIRReader(const std::set<std::string> *exported_headers)
      : IRReader(exported_headers) {}

 private:
  bool ReadDumpImpl(const std::string &dump_file) override;

  void ReadFunctions(const JsonObjectRef &tu);

  void ReadGlobalVariables(const JsonObjectRef &tu);

  void ReadEnumTypes(const JsonObjectRef &tu);

  void ReadRecordTypes(const JsonObjectRef &tu);

  void ReadFunctionTypes(const JsonObjectRef &tu);

  void ReadPointerTypes(const JsonObjectRef &tu);

  void ReadBuiltinTypes(const JsonObjectRef &tu);

  void ReadQualifiedTypes(const JsonObjectRef &tu);

  void ReadArrayTypes(const JsonObjectRef &tu);

  void ReadLvalueReferenceTypes(const JsonObjectRef &tu);

  void ReadRvalueReferenceTypes(const JsonObjectRef &tu);

  void ReadElfFunctions(const JsonObjectRef &tu);

  void ReadElfObjects(const JsonObjectRef &tu);

  static void ReadTemplateInfo(const JsonObjectRef &type_decl,
                               TemplatedArtifactIR *template_ir);

  static void ReadTypeInfo(const JsonObjectRef &type_decl, TypeIR *type_ir);

  static void ReadRecordFields(const JsonObjectRef &record_type,
                               RecordTypeIR *record_ir);

  static void ReadBaseSpecifiers(const JsonObjectRef &record_type,
                                 RecordTypeIR *record_ir);

  static void ReadVTableLayout(const JsonObjectRef &record_type,
                               RecordTypeIR *record_ir);

  static void ReadEnumFields(const JsonObjectRef &enum_type,
                             EnumTypeIR *enum_ir);

  static void ReadFunctionParametersAndReturnType(const JsonObjectRef &function,
                                                  CFunctionLikeIR *function_ir);

  static FunctionIR FunctionJsonToIR(const JsonObjectRef &function);

  static FunctionTypeIR
  FunctionTypeJsonToIR(const JsonObjectRef &function_type);

  static RecordTypeIR RecordTypeJsonToIR(const JsonObjectRef &record_type);

  static EnumTypeIR EnumTypeJsonToIR(const JsonObjectRef &enum_type);
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_JSON_IR_READER_H_

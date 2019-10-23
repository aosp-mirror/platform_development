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

#ifndef HEADER_CHECKER_REPR_IR_REPRESENTATION_INTERNAL_H_
#define HEADER_CHECKER_REPR_IR_REPRESENTATION_INTERNAL_H_

#include "repr/ir_representation.h"

#include <string>


namespace header_checker {
namespace repr {


template <typename T>
inline std::string GetReferencedTypeMapKey(T &element) {
  return element.GetReferencedType();
}

template <>
inline std::string GetReferencedTypeMapKey<ArrayTypeIR>(ArrayTypeIR &element) {
  return element.GetReferencedType() + ":" + std::to_string(element.GetSize());
}

template <>
inline std::string GetReferencedTypeMapKey<BuiltinTypeIR>(
    BuiltinTypeIR &element) {
  return element.GetLinkerSetKey();
}

inline static std::string BoolToString(bool val) {
  return val ? "true" : "false";
}

template <>
inline std::string GetReferencedTypeMapKey<QualifiedTypeIR>(
    QualifiedTypeIR &element) {
  return element.GetReferencedType() + BoolToString(element.IsRestricted()) +
      BoolToString(element.IsVolatile()) + BoolToString(element.IsConst());
}

inline std::string GetODRListMapKey(const RecordTypeIR *record_type_ir) {
  if (record_type_ir->IsAnonymous()) {
    return record_type_ir->GetLinkerSetKey();
  }
  return record_type_ir->GetLinkerSetKey() + record_type_ir->GetSourceFile();
}

inline std::string GetODRListMapKey(const EnumTypeIR *enum_type_ir) {
  return enum_type_ir->GetLinkerSetKey() + enum_type_ir->GetSourceFile();
}

inline std::string GetODRListMapKey(const FunctionTypeIR *function_type_ir) {
  return function_type_ir->GetLinkerSetKey();
}

// The map that is being updated maps special_key -> Type / Function/ GlobVar
// This special key is needed to distinguish what is being referenced.
template <typename T>
typename AbiElementMap<T>::iterator AddToMapAndTypeGraph(
    T &&element, AbiElementMap<T> *map_to_update,
    AbiElementMap<const TypeIR *> *type_graph) {
  auto it = map_to_update->emplace(GetReferencedTypeMapKey(element),
                                   std::move(element));
  type_graph->emplace(it.first->second.GetSelfType(), &(it.first->second));
  return it.first;
}


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_REPRESENTATION_INTERNAL_H_

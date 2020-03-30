// Copyright (C) 2020 The Android Open Source Project
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

#include "repr/ir_representation.h"


namespace header_checker {
namespace repr {


class MergeStatus {
public:
  MergeStatus(bool was_newly_added, const std::string &type_id)
      : was_newly_added_(was_newly_added), type_id_(type_id) {}

  MergeStatus() {}

  // type_id_ always has the global_type_id corresponding to the type this
  // MergeStatus corresponds to. For
  // generic reference types (pointers, qual types, l(r)value references etc),
  // this will be a proactively added type_id, which will be added to the
  // parent  type_graph if the we decide to add the referencing type to the
  // parent post ODR checking.
  bool was_newly_added_ = false;

  std::string type_id_;
};


class ModuleMerger {
public:
  ModuleMerger(const std::set<std::string> *exported_headers)
      : module_(new ModuleIR(exported_headers)) {}

  const ModuleIR &GetModule() {
    return *module_;
  }

  void MergeGraphs(const ModuleIR &addend);

private:
  void MergeCFunctionLikeDeps(
      const ModuleIR &addend, CFunctionLikeIR *cfunction_like_ir,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeFunctionType(
      const FunctionTypeIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeEnumType(
      const EnumTypeIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeEnumDependencies(
      const ModuleIR &addend, EnumTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeRecordAndDependencies(
      const RecordTypeIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordDependencies(
      const ModuleIR &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordFields(
      const ModuleIR &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordCXXBases(
      const ModuleIR &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordTemplateElements(
      const ModuleIR &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariable(
      const GlobalVarIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariables(
      const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunctionDeps(
      FunctionIR *added_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunction(
      const FunctionIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  MergeStatus MergeReferencingTypeInternalAndUpdateParent(
      const ModuleIR &addend, const T *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *parent_map, const std::string &updated_self_type_id);

  MergeStatus MergeReferencingTypeInternal(
      const ModuleIR &addend, ReferencesOtherType *references_type,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeReferencingType(
      const ModuleIR &addend, const TypeIR *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  std::pair<MergeStatus, typename AbiElementMap<T>::iterator>
  UpdateUDTypeAccounting(
      const T *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *specific_type_map);

  MergeStatus MergeBuiltinType(
      const BuiltinTypeIR *builtin_type, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus LookupUserDefinedType(
      const TypeIR *ud_type, const ModuleIR &addend,
      const std::string &ud_type_unique_id,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map_);

  MergeStatus LookupType(
      const TypeIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeTypeInternal(
      const TypeIR *addend_node, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeType(
      const TypeIR *addend_type, const ModuleIR &addend,
      AbiElementMap<MergeStatus> *merged_types_cache);

private:
  std::unique_ptr<ModuleIR> module_;

  uint64_t max_type_id_ = 0;
};


}  // namespace repr
}  // namespace header_checker

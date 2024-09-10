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
namespace linker {


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
  ModuleMerger() : module_(std::make_unique<repr::ModuleIR>()) {}

  const repr::ModuleIR &GetModule() { return *module_; }

  void MergeGraphs(const repr::ModuleIR &addend);

private:
  void MergeCFunctionLikeDeps(
      const repr::ModuleIR &addend, repr::CFunctionLikeIR *cfunction_like_ir,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeFunctionType(
      const repr::FunctionTypeIR *addend_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus
  MergeEnumType(const repr::EnumTypeIR *addend_node,
                const repr::ModuleIR &addend,
                repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeEnumDependencies(
      const repr::ModuleIR &addend, repr::EnumTypeIR *added_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeRecordAndDependencies(
      const repr::RecordTypeIR *addend_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordDependencies(
      const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordFields(
      const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordCXXBases(
      const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordTemplateElements(
      const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariable(
      const repr::GlobalVarIR *addend_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariables(
      const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunctionDeps(
      repr::FunctionIR *added_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void
  MergeFunction(const repr::FunctionIR *addend_node,
                const repr::ModuleIR &addend,
                repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  MergeStatus MergeReferencingTypeInternalAndUpdateParent(
      const repr::ModuleIR &addend, const T *addend_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      repr::AbiElementMap<T> *parent_map,
      const std::string &updated_self_type_id);

  MergeStatus MergeReferencingTypeInternal(
      const repr::ModuleIR &addend, repr::ReferencesOtherType *references_type,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeReferencingType(
      const repr::ModuleIR &addend, const repr::TypeIR *addend_node,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  std::pair<MergeStatus, typename repr::AbiElementMap<T>::iterator>
  UpdateUDTypeAccounting(
      const T *addend_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      repr::AbiElementMap<T> *specific_type_map);

  MergeStatus MergeBuiltinType(
      const repr::BuiltinTypeIR *builtin_type, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus LookupUserDefinedType(
      const repr::TypeIR *ud_type, const repr::ModuleIR &addend,
      const std::string &ud_type_unique_id,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map_);

  MergeStatus
  LookupType(const repr::TypeIR *addend_node, const repr::ModuleIR &addend,
             repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeTypeInternal(
      const repr::TypeIR *addend_node, const repr::ModuleIR &addend,
      repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeType(const repr::TypeIR *addend_type,
                        const repr::ModuleIR &addend,
                        repr::AbiElementMap<MergeStatus> *merged_types_cache);

private:
  std::unique_ptr<repr::ModuleIR> module_;
};


}  // namespace linker
}  // namespace header_checker

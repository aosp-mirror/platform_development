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

#ifndef HEADER_CHECKER_REPR_IR_READER_H_
#define HEADER_CHECKER_REPR_IR_READER_H_

#include "repr/ir_representation.h"

#include <cstdint>
#include <list>
#include <memory>
#include <set>
#include <string>
#include <utility>


namespace header_checker {
namespace repr {


class TextFormatToIRReader {
 public:
  struct MergeStatus {
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

 public:
  TextFormatToIRReader(const std::set<std::string> *exported_headers)
      : module_(new ModuleIR(exported_headers)) {}

  virtual ~TextFormatToIRReader() {}

  const AbiElementMap<FunctionIR> &GetFunctions() const {
    return module_->functions_;
  }

  const AbiElementMap<GlobalVarIR> &GetGlobalVariables() const {
    return module_->global_variables_;
  }

  const AbiElementMap<RecordTypeIR> &GetRecordTypes() const {
    return module_->record_types_;
  }

  const AbiElementMap<FunctionTypeIR> &GetFunctionTypes() const {
    return module_->function_types_;
  }

  const AbiElementMap<EnumTypeIR> &GetEnumTypes() const {
    return module_->enum_types_;
  }

  const AbiElementMap<LvalueReferenceTypeIR> &GetLvalueReferenceTypes() const {
    return module_->lvalue_reference_types_;
  }

  const AbiElementMap<RvalueReferenceTypeIR> &GetRvalueReferenceTypes() const {
    return module_->rvalue_reference_types_;
  }

  const AbiElementMap<QualifiedTypeIR> &GetQualifiedTypes() const {
    return module_->qualified_types_;
  }

  const AbiElementMap<ArrayTypeIR> &GetArrayTypes() const {
    return module_->array_types_;
  }

  const AbiElementMap<PointerTypeIR> &GetPointerTypes() const {
    return module_->pointer_types_;
  }

  const AbiElementMap<BuiltinTypeIR> &GetBuiltinTypes() const {
    return module_->builtin_types_;
  }

  const AbiElementMap<ElfFunctionIR> &GetElfFunctions() const {
    return module_->elf_functions_;
  }

  const AbiElementMap<ElfObjectIR> &GetElfObjects() const {
    return module_->elf_objects_;
  }

  const AbiElementMap<const TypeIR *> &GetTypeGraph() const {
    return module_->type_graph_;
  }

  const AbiElementUnorderedMap<std::list<const TypeIR *>> &
  GetODRListMap() const {
    return module_->odr_list_map_;
  }

  virtual bool ReadDump(const std::string &dump_file) = 0;

  void Merge(TextFormatToIRReader &&addend) {
    MergeElements(&module_->functions_, std::move(addend.module_->functions_));
    MergeElements(&module_->global_variables_,
                  std::move(addend.module_->global_variables_));
    MergeElements(&module_->record_types_,
                  std::move(addend.module_->record_types_));
    MergeElements(&module_->enum_types_,
                  std::move(addend.module_->enum_types_));
    MergeElements(&module_->pointer_types_,
                  std::move(addend.module_->pointer_types_));
    MergeElements(&module_->lvalue_reference_types_,
                  std::move(addend.module_->lvalue_reference_types_));
    MergeElements(&module_->rvalue_reference_types_,
                  std::move(addend.module_->rvalue_reference_types_));
    MergeElements(&module_->array_types_,
                  std::move(addend.module_->array_types_));
    MergeElements(&module_->builtin_types_,
                  std::move(addend.module_->builtin_types_));
    MergeElements(&module_->qualified_types_,
                  std::move(addend.module_->qualified_types_));
  }

  void AddToODRListMap(const std::string &key, const TypeIR *value);

  template <typename T>
  MergeStatus MergeReferencingTypeInternalAndUpdateParent(
      const TextFormatToIRReader &addend, const T *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *parent_map, const std::string  &updated_self_type_id);

  MergeStatus DoesUDTypeODRViolationExist(
      const TypeIR *ud_type, const TextFormatToIRReader &addend,
      const std::string ud_type_unique_id,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map_);

  MergeStatus MergeReferencingTypeInternal(
      const TextFormatToIRReader &addend, ReferencesOtherType *references_type,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeReferencingType(
      const TextFormatToIRReader &addend, const TypeIR *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      const std::string  &updated_self_type_id);

  MergeStatus MergeGenericReferringType(
      const TextFormatToIRReader &addend, const TypeIR *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  std::pair<MergeStatus, typename AbiElementMap<T>::iterator>
  UpdateUDTypeAccounting(
      const T *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *specific_type_map);

  MergeStatus MergeTypeInternal(
      const TypeIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeCFunctionLikeDeps(
      const TextFormatToIRReader &addend, CFunctionLikeIR *cfunction_like_ir,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeFunctionType(
      const FunctionTypeIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeEnumType(
      const EnumTypeIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeEnumDependencies(
      const TextFormatToIRReader &addend, EnumTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeRecordAndDependencies(
      const RecordTypeIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordDependencies(
      const TextFormatToIRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordFields(
      const TextFormatToIRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordCXXBases(
      const TextFormatToIRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordTemplateElements(
      const TextFormatToIRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus IsBuiltinTypeNodePresent(
      const BuiltinTypeIR *builtin_type, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariable(
      const GlobalVarIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariables(
      const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus>  *local_to_global_type_id_map);

  void MergeFunctionDeps(
      FunctionIR *added_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunction(
      const FunctionIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGraphs(const TextFormatToIRReader &addend);

  void UpdateTextFormatToIRReaderTypeGraph(
      const TypeIR *addend_node, const std::string &added_type_id,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus IsTypeNodePresent(
      const TypeIR *addend_node, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeType(
      const TypeIR *addend_type, const TextFormatToIRReader &addend,
      AbiElementMap<MergeStatus> *merged_types_cache);

  std::string AllocateNewTypeId();

  static std::unique_ptr<TextFormatToIRReader> CreateTextFormatToIRReader(
      TextFormatIR text_format,
      const std::set<std::string> *exported_headers = nullptr);

 protected:
  template <typename Augend, typename Addend>
  inline void MergeElements(Augend *augend, Addend &&addend) {
    augend->insert(std::make_move_iterator(addend.begin()),
                   std::make_move_iterator(addend.end()));
  }

  bool IsLinkableMessageInExportedHeaders(
      const LinkableMessageIR *linkable_message) const;

  std::unique_ptr<ModuleIR> module_;

  uint64_t max_type_id_ = 0;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_READER_H_

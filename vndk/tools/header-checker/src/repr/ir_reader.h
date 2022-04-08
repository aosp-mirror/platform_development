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


class IRReader {
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
  static std::unique_ptr<IRReader> CreateIRReader(
      TextFormatIR text_format,
      const std::set<std::string> *exported_headers = nullptr);

  IRReader(const std::set<std::string> *exported_headers)
      : module_(new ModuleIR(exported_headers)) {}

  virtual ~IRReader() {}

  bool ReadDump(const std::string &dump_file);

  ModuleIR &GetModule() {
    return *module_;
  }

  std::unique_ptr<ModuleIR> TakeModule() {
    return std::move(module_);
  }

  void MergeGraphs(const IRReader &addend);

 private:
  virtual bool ReadDumpImpl(const std::string &dump_file) = 0;

  void MergeCFunctionLikeDeps(
      const IRReader &addend, CFunctionLikeIR *cfunction_like_ir,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeFunctionType(
      const FunctionTypeIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeEnumType(
      const EnumTypeIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeEnumDependencies(
      const IRReader &addend, EnumTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeRecordAndDependencies(
      const RecordTypeIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordDependencies(
      const IRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordFields(
      const IRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordCXXBases(
      const IRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeRecordTemplateElements(
      const IRReader &addend, RecordTypeIR *added_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariable(
      const GlobalVarIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeGlobalVariables(
      const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunctionDeps(
      FunctionIR *added_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  void MergeFunction(
      const FunctionIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  MergeStatus MergeReferencingTypeInternalAndUpdateParent(
      const IRReader &addend, const T *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *parent_map, const std::string &updated_self_type_id);

  MergeStatus MergeReferencingTypeInternal(
      const IRReader &addend, ReferencesOtherType *references_type,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeReferencingType(
      const IRReader &addend, const TypeIR *addend_node,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  template <typename T>
  std::pair<MergeStatus, typename AbiElementMap<T>::iterator>
  UpdateUDTypeAccounting(
      const T *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map,
      AbiElementMap<T> *specific_type_map);

  MergeStatus MergeBuiltinType(
      const BuiltinTypeIR *builtin_type, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus LookupUserDefinedType(
      const TypeIR *ud_type, const IRReader &addend,
      const std::string &ud_type_unique_id,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map_);

  MergeStatus LookupType(
      const TypeIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeTypeInternal(
      const TypeIR *addend_node, const IRReader &addend,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map);

  MergeStatus MergeType(
      const TypeIR *addend_type, const IRReader &addend,
      AbiElementMap<MergeStatus> *merged_types_cache);

  std::string AllocateNewTypeId(const std::string &addend_type_id,
                                const ModuleIR &addend_module);

 protected:
  std::unique_ptr<ModuleIR> module_;

  uint64_t max_type_id_ = 0;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_READER_H_

// Copyright (C) 2018 The Android Open Source Project
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

#ifndef ABI_DIFF_HELPERS_H_
#define ABI_DIFF_HELPERS_H_

#include "repr/ir_diff_dumper.h"
#include "repr/ir_diff_representation.h"
#include "repr/ir_reader.h"
#include "repr/ir_representation.h"

#include <deque>


namespace header_checker {
namespace repr {


// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.

using MergeStatus = IRReader::MergeStatus;

enum DiffStatus {
  // There was no diff found while comparing types.
  no_diff = 0,
  // There was a diff found and it should be added as a part of a diff message.
  direct_diff = 1,
  // There was a diff found, however it need not be added as a part of a diff
  // message, since it would have already been noted elsewhere.
  indirect_diff = 2,

  opaque_diff = 3,
};

static inline DiffStatus operator|(DiffStatus f, DiffStatus s) {
  return static_cast<DiffStatus>(
      static_cast<std::underlying_type<DiffStatus>::type>(f) |
      static_cast<std::underlying_type<DiffStatus>::type>(s));
}

static inline DiffStatus operator&(DiffStatus f, DiffStatus s) {
  return static_cast<DiffStatus>(
      static_cast<std::underlying_type<DiffStatus>::type>(f) &
      static_cast<std::underlying_type<DiffStatus>::type>(s));
}

template <typename T>
using DiffStatusPair = std::pair<DiffStatus, T>;

template <typename GenericField, typename GenericFieldDiff>
struct GenericFieldDiffInfo {
  DiffStatus diff_status_;
  std::vector<GenericFieldDiff> diffed_fields_;
  std::vector<const GenericField *> removed_fields_;
  std::vector<const GenericField *> added_fields_;
};

std::string Unwind(const std::deque<std::string> *type_queue);

struct DiffPolicyOptions {
  DiffPolicyOptions(bool consider_opaque_types_different)
      : consider_opaque_types_different_(consider_opaque_types_different) {}

  bool consider_opaque_types_different_;
};

class AbiDiffHelper {
 public:
  AbiDiffHelper(
      const AbiElementMap<const TypeIR *> &old_types,
      const AbiElementMap<const TypeIR *> &new_types,
      const DiffPolicyOptions &diff_policy_options,
      std::set<std::string> *type_cache,
      IRDiffDumper *ir_diff_dumper = nullptr,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map = nullptr)
      : old_types_(old_types), new_types_(new_types),
        diff_policy_options_(diff_policy_options), type_cache_(type_cache),
        ir_diff_dumper_(ir_diff_dumper),
        local_to_global_type_id_map_(local_to_global_type_id_map) {}

  DiffStatus CompareAndDumpTypeDiff(
      const std::string &old_type_str, const std::string &new_type_str,
      std::deque<std::string> *type_queue = nullptr,
      IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);

  DiffStatus CompareAndDumpTypeDiff(
      const TypeIR *old_type, const TypeIR *new_type,
      LinkableMessageKind kind,
      std::deque<std::string> *type_queue = nullptr,
      IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);


  DiffStatus CompareRecordTypes(const RecordTypeIR *old_type,
                                const RecordTypeIR *new_type,
                                std::deque<std::string> *type_queue,
                                IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareEnumTypes(const EnumTypeIR *old_type,
                              const EnumTypeIR *new_type,
                              std::deque<std::string> *type_queue,
                              IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareFunctionTypes(const FunctionTypeIR *old_type,
                                  const FunctionTypeIR *new_type,
                                  std::deque<std::string> *type_queue,
                                  DiffMessageIR::DiffKind diff_kind);

  DiffStatus CompareFunctionParameters(
      const std::vector<ParamIR> &old_parameters,
      const std::vector<ParamIR> &new_parameters,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareTemplateInfo(
      const std::vector<TemplateElementIR> &old_template_elements,
      const std::vector<TemplateElementIR> &new_template_elements,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);


 private:
  DiffStatus CompareQualifiedTypes(const QualifiedTypeIR *old_type,
                                   const QualifiedTypeIR *new_type,
                                   std::deque<std::string> *type_queue,
                                   IRDiffDumper::DiffKind diff_kind);

  DiffStatus ComparePointerTypes(const PointerTypeIR *old_type,
                                 const PointerTypeIR *new_type,
                                 std::deque<std::string> *type_queue,
                                 IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareLvalueReferenceTypes(
      const LvalueReferenceTypeIR *old_type,
      const LvalueReferenceTypeIR *new_type,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareRvalueReferenceTypes(
      const RvalueReferenceTypeIR *old_type,
      const RvalueReferenceTypeIR *new_type,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);


  DiffStatus CompareBuiltinTypes(const BuiltinTypeIR *old_type,
                                 const BuiltinTypeIR *new_type);

  static void CompareEnumFields(
      const std::vector<EnumFieldIR> &old_fields,
      const std::vector<EnumFieldIR> &new_fields,
      EnumTypeDiffIR *enum_type_diff_ir);


  void ReplaceRemovedFieldTypeIdsWithTypeNames(
      std::vector<RecordFieldIR *> *removed_fields);

  void ReplaceDiffedFieldTypeIdsWithTypeNames(
      RecordFieldDiffIR *diffed_field);

  std::vector<std::pair<RecordFieldIR, RecordFieldIR>>
  FixupDiffedFieldTypeIds(
      const std::vector<RecordFieldDiffIR> &field_diffs);

  DiffStatusPair<std::unique_ptr<RecordFieldDiffIR>>
  CompareCommonRecordFields(
      const RecordFieldIR *old_field,
      const RecordFieldIR *new_field,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);

  GenericFieldDiffInfo<RecordFieldIR, RecordFieldDiffIR>
      CompareRecordFields(
      const std::vector<RecordFieldIR> &old_fields,
      const std::vector<RecordFieldIR> &new_fields,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);

  bool CompareBaseSpecifiers(
      const std::vector<CXXBaseSpecifierIR> &old_base_specifiers,
      const std::vector<CXXBaseSpecifierIR> &new_base_specifiers,
      std::deque<std::string> *type_queue,
      IRDiffDumper::DiffKind diff_kind);

  bool CompareVTables(const RecordTypeIR *old_record,
                      const RecordTypeIR *new_record);

  bool CompareVTableComponents(
      const VTableComponentIR &old_component,
      const VTableComponentIR &new_component);

  bool CompareSizeAndAlignment(const TypeIR *old_ti,
                               const TypeIR *new_ti);

  template <typename DiffType, typename DiffElement>
  bool AddToDiff(DiffType *mutable_diff, const DiffElement *oldp,
                 const DiffElement *newp,
                 std::deque<std::string> *type_queue = nullptr);

 protected:
  const AbiElementMap<const TypeIR *> &old_types_;
  const AbiElementMap<const TypeIR *> &new_types_;
  const DiffPolicyOptions &diff_policy_options_;
  std::set<std::string> *type_cache_;
  IRDiffDumper *ir_diff_dumper_;
  AbiElementMap<MergeStatus> *local_to_global_type_id_map_;
};

void ReplaceTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph, LinkableMessageIR *lm);


}  // namespace repr
}  // namespace header_checker


#endif  // ABI_DIFF_HELPERS_H_

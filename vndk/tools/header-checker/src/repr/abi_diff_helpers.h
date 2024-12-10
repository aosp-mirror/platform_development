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
#include "repr/ir_representation.h"

#include <deque>
#include <set>


namespace header_checker {
namespace repr {


// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.

class DiffStatus {
 public:
  enum Status {
    kNoDiff = 0,
    // The diff has been added to the IRDiffDumper.
    kIndirectDiff = 1,
    // The diff has not been added to the IRDiffDumper, and the new ABI is
    // an extension to the old ABI.
    kDirectExt = 2,
    // The diff has not been added to the IRDiffDumper.
    kDirectDiff = 3,
  };

  // Allow implicit conversion.
  DiffStatus(Status status) : status_(status) {}

  bool HasDiff() const { return status_ != kNoDiff; }

  bool IsDirectDiff() const {
    return status_ == kDirectDiff || status_ == kDirectExt;
  }

  bool IsExtension() const { return status_ == kDirectExt; }

  DiffStatus &CombineWith(DiffStatus other) {
    status_ = std::max(status_, other.status_);
    return *this;
  }

 private:
  Status status_;
};

struct RecordFieldDiffResult {
  DiffStatus status = DiffStatus::kNoDiff;
  std::vector<RecordFieldDiffIR> diffed_fields;
  std::vector<const RecordFieldIR *> removed_fields;
  std::vector<const RecordFieldIR *> added_fields;
};

class TypeStackGuard {
 public:
  TypeStackGuard(std::deque<std::string> &type_stack,
                 const std::string &type_name)
      : type_stack_(type_stack) {
    type_stack_.push_back(type_name);
  }

  ~TypeStackGuard() { type_stack_.pop_back(); }

 private:
  std::deque<std::string> &type_stack_;
};

struct DiffPolicyOptions {
  bool consider_opaque_types_different = false;
  bool allow_adding_removing_referenced_apis = false;
};

class AbiDiffHelper {
 public:
  AbiDiffHelper(
      const AbiElementMap<const TypeIR *> &old_types,
      const AbiElementMap<const TypeIR *> &new_types,
      const DiffPolicyOptions &diff_policy_options,
      std::set<std::string> *type_cache,
      const std::set<std::string> &ignored_linker_set_keys,
      IRDiffDumper *ir_diff_dumper = nullptr)
      : old_types_(old_types), new_types_(new_types),
        diff_policy_options_(diff_policy_options), type_cache_(type_cache),
        ignored_linker_set_keys_(ignored_linker_set_keys),
        ir_diff_dumper_(ir_diff_dumper) {}

  // Concatenate the strings in type_stack.
  std::string UnwindTypeStack();

  bool AreOpaqueTypesEqual(const std::string &old_type_str,
                           const std::string &new_type_str) const;

  DiffStatus CompareAndDumpTypeDiff(
      const std::string &old_type_str, const std::string &new_type_str,
      IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);

  DiffStatus CompareAndDumpTypeDiff(
      const TypeIR *old_type, const TypeIR *new_type, LinkableMessageKind kind,
      IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);

  DiffStatus CompareRecordTypes(const RecordTypeIR *old_type,
                                const RecordTypeIR *new_type,
                                IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareEnumTypes(const EnumTypeIR *old_type,
                              const EnumTypeIR *new_type,
                              IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareFunctionTypes(const CFunctionLikeIR *old_type,
                                  const CFunctionLikeIR *new_type,
                                  DiffMessageIR::DiffKind diff_kind);

  DiffStatus CompareTemplateInfo(
      const std::vector<TemplateElementIR> &old_template_elements,
      const std::vector<TemplateElementIR> &new_template_elements,
      IRDiffDumper::DiffKind diff_kind);

 private:
  DiffStatus CompareQualifiedTypes(const QualifiedTypeIR *old_type,
                                   const QualifiedTypeIR *new_type,
                                   IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareArrayTypes(const ArrayTypeIR *old_type,
                               const ArrayTypeIR *new_type,
                               IRDiffDumper::DiffKind diff_kind);

  DiffStatus ComparePointerTypes(const PointerTypeIR *old_type,
                                 const PointerTypeIR *new_type,
                                 IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareLvalueReferenceTypes(const LvalueReferenceTypeIR *old_type,
                                         const LvalueReferenceTypeIR *new_type,
                                         IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareRvalueReferenceTypes(const RvalueReferenceTypeIR *old_type,
                                         const RvalueReferenceTypeIR *new_type,
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

  DiffStatus CompareAccess(AccessSpecifierIR old_access,
                           AccessSpecifierIR new_access);

  DiffStatus CompareCommonRecordFields(const RecordFieldIR *old_field,
                                       const RecordFieldIR *new_field,
                                       IRDiffDumper::DiffKind diff_kind);

  DiffStatus FilterOutRenamedRecordFields(
      DiffMessageIR::DiffKind diff_kind,
      std::vector<const RecordFieldIR *> &old_fields,
      std::vector<const RecordFieldIR *> &new_fields);

  RecordFieldDiffResult CompareRecordFields(
      const std::vector<RecordFieldIR> &old_fields,
      const std::vector<RecordFieldIR> &new_fields,
      IRDiffDumper::DiffKind diff_kind);

  bool CompareBaseSpecifiers(
      const std::vector<CXXBaseSpecifierIR> &old_base_specifiers,
      const std::vector<CXXBaseSpecifierIR> &new_base_specifiers,
      IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareFunctionParameters(
      const std::vector<ParamIR> &old_parameters,
      const std::vector<ParamIR> &new_parameters,
      IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareParameterTypes(const std::string &old_type_id,
                                   const std::string &new_type_id,
                                   IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareReturnTypes(const std::string &old_type_id,
                                const std::string &new_type_id,
                                IRDiffDumper::DiffKind diff_kind);

 protected:
  const AbiElementMap<const TypeIR *> &old_types_;
  const AbiElementMap<const TypeIR *> &new_types_;
  const DiffPolicyOptions &diff_policy_options_;
  std::set<std::string> *type_cache_;
  std::deque<std::string> type_stack_;
  const std::set<std::string> &ignored_linker_set_keys_;
  IRDiffDumper *ir_diff_dumper_;
};

void ReplaceTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph, LinkableMessageIR *lm);


}  // namespace repr
}  // namespace header_checker


#endif  // ABI_DIFF_HELPERS_H_

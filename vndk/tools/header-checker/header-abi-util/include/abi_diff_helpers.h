#ifndef ABI_DIFF_HELPERS
#define ABI_DIFF_HELPERS

#include <ir_representation.h>

#include <deque>

// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.
namespace abi_util {

using MergeStatus = TextFormatToIRReader::MergeStatus;

enum DiffStatus {
  // There was no diff found while comparing types.
  no_diff = 0,
  // There was a diff found and it should be added as a part of a diff message.
  direct_diff = 1,
  // There was a diff found, however it need not be added as a part of a diff
  // message, since it would have already been noted elsewhere.
  indirect_diff = 2,
};

static inline DiffStatus operator| (DiffStatus f,DiffStatus s) {
  return static_cast<DiffStatus>(
      static_cast<std::underlying_type<DiffStatus>::type>(f) |
      static_cast<std::underlying_type<DiffStatus>::type>(s));
}

static inline DiffStatus operator& (DiffStatus f, DiffStatus s) {
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

class AbiDiffHelper {
 public:
  AbiDiffHelper(
      const AbiElementMap<const abi_util::TypeIR *> &old_types,
      const AbiElementMap<const abi_util::TypeIR *> &new_types,
      std::set<std::string> *type_cache,
      abi_util::IRDiffDumper *ir_diff_dumper = nullptr,
      AbiElementMap<MergeStatus> *local_to_global_type_id_map = nullptr)
      : old_types_(old_types), new_types_(new_types),
        type_cache_(type_cache), ir_diff_dumper_(ir_diff_dumper),
        local_to_global_type_id_map_(local_to_global_type_id_map) { }

  DiffStatus CompareAndDumpTypeDiff(
      const std::string &old_type_str, const std::string &new_type_str,
      std::deque<std::string> *type_queue = nullptr,
      abi_util::IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);

  DiffStatus CompareAndDumpTypeDiff(
      const abi_util::TypeIR *old_type, const abi_util::TypeIR *new_type,
      abi_util::LinkableMessageKind kind,
      std::deque<std::string> *type_queue = nullptr,
      abi_util::IRDiffDumper::DiffKind diff_kind = DiffMessageIR::Unreferenced);


  DiffStatus CompareRecordTypes(const abi_util::RecordTypeIR *old_type,
                                const abi_util::RecordTypeIR *new_type,
                                std::deque<std::string> *type_queue,
                                abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareQualifiedTypes(const abi_util::QualifiedTypeIR *old_type,
                                   const abi_util::QualifiedTypeIR *new_type,
                                   std::deque<std::string> *type_queue,
                                   abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus ComparePointerTypes(const abi_util::PointerTypeIR *old_type,
                                 const abi_util::PointerTypeIR *new_type,
                                 std::deque<std::string> *type_queue,
                                 abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareLvalueReferenceTypes(
      const abi_util::LvalueReferenceTypeIR *old_type,
      const abi_util::LvalueReferenceTypeIR *new_type,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareRvalueReferenceTypes(
      const abi_util::RvalueReferenceTypeIR *old_type,
      const abi_util::RvalueReferenceTypeIR *new_type,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);


  DiffStatus CompareBuiltinTypes(const abi_util::BuiltinTypeIR *old_type,
                                 const abi_util::BuiltinTypeIR *new_type);
  static void CompareEnumFields(
    const std::vector<abi_util::EnumFieldIR> &old_fields,
    const std::vector<abi_util::EnumFieldIR> &new_fields,
    abi_util::EnumTypeDiffIR *enum_type_diff_ir);

  DiffStatus CompareEnumTypes(const abi_util::EnumTypeIR *old_type,
                              const abi_util::EnumTypeIR *new_type,
                              std::deque<std::string> *type_queue,
                              abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareFunctionTypes(const abi_util::FunctionTypeIR *old_type,
                                  const abi_util::FunctionTypeIR *new_type,
                                  std::deque<std::string> *type_queue,
                                  abi_util::DiffMessageIR::DiffKind diff_kind);

  void ReplaceRemovedFieldTypeIdsWithTypeNames(
    std::vector<abi_util::RecordFieldIR *> *removed_fields);

  void ReplaceDiffedFieldTypeIdsWithTypeNames(
      abi_util::RecordFieldDiffIR *diffed_field);

  std::vector<std::pair<abi_util::RecordFieldIR, abi_util::RecordFieldIR>>
  FixupDiffedFieldTypeIds(
      const std::vector<abi_util::RecordFieldDiffIR> &field_diffs);

  DiffStatusPair<std::unique_ptr<abi_util::RecordFieldDiffIR>>
  CompareCommonRecordFields(
    const abi_util::RecordFieldIR *old_field,
    const abi_util::RecordFieldIR *new_field,
    std::deque<std::string> *type_queue,
    abi_util::IRDiffDumper::DiffKind diff_kind);

  GenericFieldDiffInfo<abi_util::RecordFieldIR, abi_util::RecordFieldDiffIR>
      CompareRecordFields(
      const std::vector<abi_util::RecordFieldIR> &old_fields,
      const std::vector<abi_util::RecordFieldIR> &new_fields,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  DiffStatus CompareFunctionParameters(
      const std::vector<abi_util::ParamIR> &old_parameters,
      const std::vector<abi_util::ParamIR> &new_parameters,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  bool CompareBaseSpecifiers(
      const std::vector<abi_util::CXXBaseSpecifierIR> &old_base_specifiers,
      const std::vector<abi_util::CXXBaseSpecifierIR> &new_base_specifiers,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  bool CompareVTables(const abi_util::RecordTypeIR *old_record,
                      const abi_util::RecordTypeIR *new_record);

  bool CompareVTableComponents(
      const abi_util::VTableComponentIR &old_component,
      const abi_util::VTableComponentIR &new_component);

  DiffStatus CompareTemplateInfo(
      const std::vector<abi_util::TemplateElementIR> &old_template_elements,
      const std::vector<abi_util::TemplateElementIR> &new_template_elements,
      std::deque<std::string> *type_queue,
      abi_util::IRDiffDumper::DiffKind diff_kind);


  bool CompareSizeAndAlignment(const abi_util::TypeIR *old_ti,
                               const abi_util::TypeIR *new_ti);

  template <typename DiffType, typename DiffElement>
  bool AddToDiff(DiffType *mutable_diff, const DiffElement *oldp,
                 const DiffElement *newp,
                 std::deque<std::string> *type_queue = nullptr);
 protected:
  const AbiElementMap<const abi_util::TypeIR *> &old_types_;
  const AbiElementMap<const abi_util::TypeIR *> &new_types_;
  std::set<std::string> *type_cache_ = nullptr;
  abi_util::IRDiffDumper *ir_diff_dumper_ = nullptr;
  AbiElementMap<MergeStatus> *local_to_global_type_id_map_ = nullptr;
};

void ReplaceTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph, LinkableMessageIR *lm);

} // namespace abi_util
#endif

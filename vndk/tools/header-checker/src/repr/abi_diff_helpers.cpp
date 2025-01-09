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

#include "repr/abi_diff_helpers.h"

#include "utils/header_abi_util.h"

#include <android-base/strings.h>
#include <llvm/Support/raw_ostream.h>

#include <unordered_set>


namespace header_checker {
namespace repr {


static std::string ConvertTypeIdToString(
    const AbiElementMap<const TypeIR *> &type_graph,
    const std::string &type_id) {
  auto it = type_graph.find(type_id);
  if (it != type_graph.end()) {
    return it->second->GetName();
  }
  return "type-unexported";
}

template <typename Container>
static void ReplaceReferencesOtherTypeIdWithName(
    const AbiElementMap<const TypeIR *> &type_graph,
    Container &to_fix_elements) {
  for (auto &element : to_fix_elements) {
    element.SetReferencedType(
        ConvertTypeIdToString(type_graph, element.GetReferencedType()));
  }
}

static void ReplaceEnumTypeIRTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph, EnumTypeIR *enum_type_ir) {
  // Replace underlying type.
  enum_type_ir->SetUnderlyingType(
      ConvertTypeIdToString(type_graph, enum_type_ir->GetUnderlyingType()));
}

static void ReplaceRecordTypeIRTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph,
    RecordTypeIR *record_type_ir) {
  // Replace Fields
  ReplaceReferencesOtherTypeIdWithName(type_graph,
                                       record_type_ir->GetFields());
  // Replace template parameters
  ReplaceReferencesOtherTypeIdWithName(type_graph,
                                       record_type_ir->GetTemplateElements());
  // Replace bases
  ReplaceReferencesOtherTypeIdWithName(type_graph,
                                       record_type_ir->GetBases());
}

static void ReplaceGlobalVarTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph,
    GlobalVarIR *global_var_ir) {
  // Replace referenced type id.
  global_var_ir->SetReferencedType(
      ConvertTypeIdToString(type_graph, global_var_ir->GetReferencedType()));
}

static void ReplaceFunctionTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph, FunctionIR *function_ir) {
  // Replace return type
  function_ir->SetReturnType(
      ConvertTypeIdToString(type_graph, function_ir->GetReturnType()));
  // Replace function parameters
  ReplaceReferencesOtherTypeIdWithName(type_graph,
                                       function_ir->GetParameters());
  // Replace function template parameters
  ReplaceReferencesOtherTypeIdWithName(type_graph,
                                       function_ir->GetTemplateElements());
}

void ReplaceTypeIdsWithTypeNames(
    const AbiElementMap<const TypeIR *> &type_graph,
    LinkableMessageIR *lm) {
  switch (lm->GetKind()) {
    case FunctionKind:
      ReplaceFunctionTypeIdsWithTypeNames(
          type_graph, static_cast<FunctionIR *>(lm));
      break;
    case GlobalVarKind:
      ReplaceGlobalVarTypeIdsWithTypeNames(
          type_graph, static_cast<GlobalVarIR *>(lm));
      break;
    case RecordTypeKind:
      ReplaceRecordTypeIRTypeIdsWithTypeNames(
          type_graph, static_cast<RecordTypeIR *>(lm));
      break;
    case EnumTypeKind:
      ReplaceEnumTypeIRTypeIdsWithTypeNames(
          type_graph, static_cast<EnumTypeIR *>(lm));
      break;
    default:
      // This method should not be called on any other LinkableMessage
      assert(0);
  }
}

std::string AbiDiffHelper::UnwindTypeStack() {
  return android::base::Join(type_stack_, "-> ");
}

void AbiDiffHelper::CompareEnumFields(
    const std::vector<EnumFieldIR> &old_fields,
    const std::vector<EnumFieldIR> &new_fields,
    EnumTypeDiffIR *enum_type_diff_ir) {
  AbiElementMap<const EnumFieldIR *> old_fields_map;
  AbiElementMap<const EnumFieldIR *> new_fields_map;
  utils::AddToMap(&old_fields_map, old_fields,
                  [](const EnumFieldIR *f) {return f->GetName();},
                  [](const EnumFieldIR *f) {return f;});

  utils::AddToMap(&new_fields_map, new_fields,
                  [](const EnumFieldIR *f) {return f->GetName();},
                  [](const EnumFieldIR *f) {return f;});

  std::vector<const EnumFieldIR *> removed_fields =
      utils::FindRemovedElements(old_fields_map, new_fields_map);

  std::vector<const EnumFieldIR *> added_fields =
      utils::FindRemovedElements(new_fields_map, old_fields_map);

  enum_type_diff_ir->SetFieldsAdded(std::move(added_fields));

  enum_type_diff_ir->SetFieldsRemoved(std::move(removed_fields));

  std::vector<std::pair<const EnumFieldIR *,
                        const EnumFieldIR *>> cf =
      utils::FindCommonElements(old_fields_map, new_fields_map);
  std::vector<EnumFieldDiffIR> enum_field_diffs;
  for (auto &&common_fields : cf) {
    if (common_fields.first->GetSignedValue() !=
        common_fields.second->GetSignedValue()) {
      EnumFieldDiffIR enum_field_diff_ir(common_fields.first,
                                         common_fields.second);
      enum_field_diffs.emplace_back(std::move(enum_field_diff_ir));
    }
  }
  enum_type_diff_ir->SetFieldsDiff(std::move(enum_field_diffs));
}

DiffStatus AbiDiffHelper::CompareEnumTypes(const EnumTypeIR *old_type,
                                           const EnumTypeIR *new_type,
                                           DiffMessageIR::DiffKind diff_kind) {
  if (old_type->GetLinkerSetKey() != new_type->GetLinkerSetKey()) {
    return DiffStatus::kDirectDiff;
  }
  auto enum_type_diff_ir = std::make_unique<EnumTypeDiffIR>();
  enum_type_diff_ir->SetName(old_type->GetName());
  enum_type_diff_ir->SetLinkerSetKey(old_type->GetLinkerSetKey());
  const std::string &old_underlying_type =
      ConvertTypeIdToString(old_types_, old_type->GetUnderlyingType());
  const std::string &new_underlying_type =
      ConvertTypeIdToString(new_types_, new_type->GetUnderlyingType());
  if (old_underlying_type != new_underlying_type) {
    enum_type_diff_ir->SetUnderlyingTypeDiff(
        std::make_unique<std::pair<std::string, std::string>>(
            old_underlying_type, new_underlying_type));
  }
  CompareEnumFields(old_type->GetFields(), new_type->GetFields(),
                    enum_type_diff_ir.get());
  if ((enum_type_diff_ir->IsExtended() ||
       enum_type_diff_ir->IsIncompatible()) &&
      (ir_diff_dumper_ &&
       !ir_diff_dumper_->AddDiffMessageIR(enum_type_diff_ir.get(),
                                          UnwindTypeStack(), diff_kind))) {
    llvm::errs() << "AddDiffMessage on EnumTypeDiffIR failed\n";
    ::exit(1);
  }
  return DiffStatus::kNoDiff;
}

static std::string RemoveThunkInfoFromMangledName(const std::string &name) {
  if (name.find("_ZTv") != 0 && name.find("_ZTh") != 0 &&
      name.find("_ZTc") != 0) {
    return name;
  }
  size_t base_name_pos = name.find("N");
  if (base_name_pos == std::string::npos) {
    return name;
  }
  return "_Z" + name.substr(base_name_pos);
}

static bool CompareVTableComponents(const VTableComponentIR &old_component,
                                    const VTableComponentIR &new_component) {
  // Vtable components in prebuilts/abi-dumps/vndk/28 don't have thunk info.
  if (old_component.GetName() != new_component.GetName()) {
    if (RemoveThunkInfoFromMangledName(old_component.GetName()) ==
        RemoveThunkInfoFromMangledName(new_component.GetName())) {
      llvm::errs() << "WARNING: Ignore difference between "
                   << old_component.GetName() << " and "
                   << new_component.GetName() << "\n";
    } else {
      return false;
    }
  }
  return old_component.GetValue() == new_component.GetValue() &&
         old_component.GetKind() == new_component.GetKind();
}

static bool CompareVTables(
    const std::vector<VTableComponentIR> &old_components,
    const std::vector<VTableComponentIR> &new_components) {
  if (old_components.size() != new_components.size()) {
    return false;
  }
  for (size_t i = 0; i < old_components.size(); i++) {
    if (!CompareVTableComponents(old_components[i], new_components[i])) {
      return false;
    }
  }
  return true;
}

static inline bool IsVOffset(VTableComponentIR::Kind kind) {
  return kind == VTableComponentIR::VBaseOffset ||
         kind == VTableComponentIR::VCallOffset;
}

static inline bool IsFunctionPointer(VTableComponentIR::Kind kind) {
  return kind == VTableComponentIR::FunctionPointer ||
         kind == VTableComponentIR::CompleteDtorPointer ||
         kind == VTableComponentIR::DeletingDtorPointer;
}

// A Vtable consists of one or more sub-vtables. Each sub-vtable is a sequence
// of components in the following order:
//   Zero or more VCallOffset or VBaseOffset.
//   One OffsetToTop.
//   One RTTI.
//   Zero or more FunctionPointer, CompleteDtorPointer, or DeletingDtorPointer.
//
// An object's vtable pointer points to the next component of the RTTI
// component. Hence, new components can be appended or prepended to sub-vtables
// without breaking compatibility.
static bool IsVTableExtended(
    const std::vector<VTableComponentIR> &old_components,
    const std::vector<VTableComponentIR> &new_components) {
  const auto old_end = old_components.end();
  const auto new_end = new_components.end();
  auto old_it = old_components.begin();
  auto new_it = new_components.begin();
  bool is_extended = false;
  while (old_it != old_end) {
    const auto old_begin = old_it;
    const auto new_begin = new_it;
    // Iterate VCallOffset and VBaseOffset.
    while (old_it != old_end && IsVOffset(old_it->GetKind())) {
      old_it++;
    }
    while (new_it != new_end && IsVOffset(new_it->GetKind())) {
      new_it++;
    }
    // Compare VCallOffset and VBaseOffset.
    auto old_back_it = old_it;
    auto new_back_it = new_it;
    while (old_back_it != old_begin) {
      if (new_back_it == new_begin) {
        return false;
      }
      old_back_it--;
      new_back_it--;
      if (old_back_it->GetKind() != new_back_it->GetKind()) {
        return false;
      }
    }
    // The new sub-vtable has additional VOffsets at the beginning.
    if (new_back_it != new_begin) {
      is_extended = true;
    }
    // Compare OffsetToTop.
    if (old_it == old_end || new_it == new_end ||
        old_it->GetKind() != VTableComponentIR::OffsetToTop ||
        new_it->GetKind() != VTableComponentIR::OffsetToTop) {
      return false;
    }
    old_it++;
    new_it++;
    // Compare RTTI.
    if (old_it == old_end || new_it == new_end ||
        old_it->GetKind() != VTableComponentIR::RTTI ||
        new_it->GetKind() != VTableComponentIR::RTTI ||
        old_it->GetName() != new_it->GetName()) {
      return false;
    }
    old_it++;
    new_it++;
    // Compare function pointers.
    while (old_it != old_end && IsFunctionPointer(old_it->GetKind())) {
      if (new_it == new_end || old_it->GetKind() != new_it->GetKind() ||
          old_it->GetName() != new_it->GetName()) {
        return false;
      }
      old_it++;
      new_it++;
    }
    // The new sub-vtable has additional function pointers at the end.
    while (new_it != new_end && IsFunctionPointer(new_it->GetKind())) {
      is_extended = true;
      new_it++;
    }
  }
  return new_it == new_end ? is_extended : false;
}

bool AbiDiffHelper::AreOpaqueTypesEqual(const std::string &old_type_id,
                                        const std::string &new_type_id) const {
  // b/253095767: In T, some dump files contain opaque types whose IDs end with
  // "#ODR:" and the source paths. This function removes the suffixes before
  // comparing the type IDs.
  if (!diff_policy_options_.consider_opaque_types_different ||
      ExtractMultiDefinitionTypeId(old_type_id) ==
          ExtractMultiDefinitionTypeId(new_type_id)) {
    return true;
  }
  // __va_list is an opaque type defined by the compiler. ARM ABI requires
  // __va_list to be in std namespace. Its mangled name is _ZTISt9__va_list, but
  // some versions of clang produce _ZTI9__va_list. The names are equivalent.
  static const std::unordered_set<std::string> va_list_names{
      "_ZTI9__va_list", "_ZTISt9__va_list"};
  return va_list_names.count(old_type_id) && va_list_names.count(new_type_id);
}

static bool CompareSizeAndAlignment(const TypeIR *old_type,
                                    const TypeIR *new_type) {
  return old_type->GetSize() == new_type->GetSize() &&
      old_type->GetAlignment() == new_type->GetAlignment();
}

DiffStatus AbiDiffHelper::CompareAccess(AccessSpecifierIR old_access,
                                        AccessSpecifierIR new_access) {
  if (old_access == new_access) {
    return DiffStatus::kNoDiff;
  }
  if (old_access > new_access) {
    return DiffStatus::kDirectExt;
  }
  return DiffStatus::kDirectDiff;
}

// This function returns a map from field names to RecordFieldIR.
// It appends anonymous fields to anonymous_fields.
static AbiElementMap<const RecordFieldIR *> BuildRecordFieldNameMap(
    const std::vector<RecordFieldIR> &fields,
    std::vector<const RecordFieldIR *> &anonymous_fields) {
  AbiElementMap<const RecordFieldIR *> field_map;
  for (const RecordFieldIR &field : fields) {
    const std::string &name = field.GetName();
    if (name.empty()) {
      anonymous_fields.emplace_back(&field);
    } else {
      field_map.emplace(name, &field);
    }
  }
  return field_map;
}

DiffStatus AbiDiffHelper::CompareCommonRecordFields(
    const RecordFieldIR *old_field, const RecordFieldIR *new_field,
    DiffMessageIR::DiffKind diff_kind) {
  DiffStatus field_diff_status =
      CompareAndDumpTypeDiff(old_field->GetReferencedType(),
                             new_field->GetReferencedType(), diff_kind);
  // CompareAndDumpTypeDiff should not return kDirectExt.
  // In case it happens, report an incompatible diff for review.
  if (field_diff_status.IsExtension() ||
      old_field->GetOffset() != new_field->GetOffset() ||
      old_field->IsBitField() != new_field->IsBitField() ||
      old_field->GetBitWidth() != new_field->GetBitWidth()) {
    field_diff_status.CombineWith(DiffStatus::kDirectDiff);
  }
  field_diff_status.CombineWith(
      CompareAccess(old_field->GetAccess(), new_field->GetAccess()));
  return field_diff_status;
}

// FilterOutRenamedRecordFields calls this function to compare record fields in
// two dumps.
// If this function returns 0, the fields may be compatible.
// If it returns -1 or 1, the fields must be incompatible.
static int CompareRenamedRecordFields(const RecordFieldIR *old_field,
                                      const RecordFieldIR *new_field) {
  if (old_field->GetOffset() != new_field->GetOffset()) {
    return old_field->GetOffset() < new_field->GetOffset() ? -1 : 1;
  }
  if (old_field->IsBitField() != new_field->IsBitField()) {
    return old_field->IsBitField() < new_field->IsBitField() ? -1 : 1;
  }
  if (old_field->GetBitWidth() != new_field->GetBitWidth()) {
    return old_field->GetBitWidth() < new_field->GetBitWidth() ? -1 : 1;
  }
  // Skip GetReferencedType because the same type in old and new dumps may have
  // different IDs, especially in the cases of anonymous types and multiple
  // definitions.
  return 0;
}

// This function filters out the pairs of old and new fields that meet the
// following conditions:
//   The old field's (offset, bit width, type) is unique in old_fields.
//   The new field's (offset, bit width, type) is unique in new_fields.
//   The two fields have compatible attributes except the name.
//
// This function returns either kNoDiff or kIndirectDiff. It is the status of
// the field pairs that are filtered out.
DiffStatus AbiDiffHelper::FilterOutRenamedRecordFields(
    DiffMessageIR::DiffKind diff_kind,
    std::vector<const RecordFieldIR *> &old_fields,
    std::vector<const RecordFieldIR *> &new_fields) {
  DiffStatus diff_status = DiffStatus::kNoDiff;
  const auto old_end = old_fields.end();
  const auto new_end = new_fields.end();
  // Sort fields by (offset, bit width, type).
  auto is_less = [](const RecordFieldIR *first, const RecordFieldIR *second) {
    int result = CompareRenamedRecordFields(first, second);
    return result != 0
               ? result < 0
               : first->GetReferencedType() < second->GetReferencedType();
  };
  std::sort(old_fields.begin(), old_end, is_less);
  std::sort(new_fields.begin(), new_end, is_less);

  std::vector<const RecordFieldIR *> out_old_fields;
  std::vector<const RecordFieldIR *> out_new_fields;
  auto old_it = old_fields.begin();
  auto new_it = new_fields.begin();
  while (old_it != old_end && new_it != new_end) {
    int old_new_cmp = CompareRenamedRecordFields(*old_it, *new_it);
    auto next_old_it = std::next(old_it);
    while (next_old_it != old_end && !is_less(*old_it, *next_old_it)) {
      next_old_it++;
    }
    if (old_new_cmp < 0 || next_old_it - old_it > 1) {
      out_old_fields.insert(out_old_fields.end(), old_it, next_old_it);
      old_it = next_old_it;
      continue;
    }

    auto next_new_it = std::next(new_it);
    while (next_new_it != new_end && !is_less(*new_it, *next_new_it)) {
      next_new_it++;
    }
    if (old_new_cmp > 0 || next_new_it - new_it > 1) {
      out_new_fields.insert(out_new_fields.end(), new_it, next_new_it);
      new_it = next_new_it;
      continue;
    }

    DiffStatus field_diff_status =
        CompareCommonRecordFields(*old_it, *new_it, diff_kind);
    if (field_diff_status.IsDirectDiff()) {
      out_old_fields.emplace_back(*old_it);
      out_new_fields.emplace_back(*new_it);
    } else {
      diff_status.CombineWith(field_diff_status);
    }
    old_it = next_old_it;
    new_it = next_new_it;
  }
  out_old_fields.insert(out_old_fields.end(), old_it, old_end);
  out_new_fields.insert(out_new_fields.end(), new_it, new_end);

  old_fields = std::move(out_old_fields);
  new_fields = std::move(out_new_fields);
  return diff_status;
}

RecordFieldDiffResult AbiDiffHelper::CompareRecordFields(
    const std::vector<RecordFieldIR> &old_fields,
    const std::vector<RecordFieldIR> &new_fields,
    DiffMessageIR::DiffKind diff_kind) {
  RecordFieldDiffResult result;
  DiffStatus &diff_status = result.status;
  diff_status = DiffStatus::kNoDiff;
  AbiElementMap<const RecordFieldIR *> old_fields_map =
      BuildRecordFieldNameMap(old_fields, result.removed_fields);
  AbiElementMap<const RecordFieldIR *> new_fields_map =
      BuildRecordFieldNameMap(new_fields, result.added_fields);

  // Compare the anonymous fields and the fields whose names are not present in
  // both records.
  utils::InsertAll(result.removed_fields,
                   utils::FindRemovedElements(old_fields_map, new_fields_map));
  utils::InsertAll(result.added_fields,
                   utils::FindRemovedElements(new_fields_map, old_fields_map));
  diff_status.CombineWith(FilterOutRenamedRecordFields(
      diff_kind, result.removed_fields, result.added_fields));
  if (result.removed_fields.size() != 0) {
    diff_status.CombineWith(DiffStatus::kDirectDiff);
  }
  if (result.added_fields.size() != 0) {
    diff_status.CombineWith(DiffStatus::kDirectExt);
  }
  // Compare the fields whose names are present in both records.
  std::vector<std::pair<const RecordFieldIR *, const RecordFieldIR *>> cf =
      utils::FindCommonElements(old_fields_map, new_fields_map);
  for (auto &&common_fields : cf) {
    DiffStatus field_diff_status = CompareCommonRecordFields(
        common_fields.first, common_fields.second, diff_kind);
    diff_status.CombineWith(field_diff_status);
    if (field_diff_status.IsDirectDiff()) {
      result.diffed_fields.emplace_back(common_fields.first,
                                        common_fields.second);
    }
  }
  return result;
}

bool AbiDiffHelper::CompareBaseSpecifiers(
    const std::vector<CXXBaseSpecifierIR> &old_base_specifiers,
    const std::vector<CXXBaseSpecifierIR> &new_base_specifiers,
    DiffMessageIR::DiffKind diff_kind) {
  if (old_base_specifiers.size() != new_base_specifiers.size()) {
    return false;
  }
  int i = 0;
  while (i < old_base_specifiers.size()) {
    if (CompareAndDumpTypeDiff(old_base_specifiers.at(i).GetReferencedType(),
                               new_base_specifiers.at(i).GetReferencedType(),
                               diff_kind)
            .IsDirectDiff() ||
        (old_base_specifiers.at(i).GetAccess() !=
         new_base_specifiers.at(i).GetAccess())) {
      return false;
    }
    i++;
  }
  return true;
}

DiffStatus AbiDiffHelper::CompareTemplateInfo(
    const std::vector<TemplateElementIR> &old_template_elements,
    const std::vector<TemplateElementIR> &new_template_elements,
    DiffMessageIR::DiffKind diff_kind) {
  uint32_t old_template_size = old_template_elements.size();
  uint32_t i = 0;
  if (old_template_size != new_template_elements.size()) {
    return DiffStatus::kDirectDiff;
  }
  DiffStatus final_diff_status = DiffStatus::kNoDiff;
  while (i < old_template_size) {
    const TemplateElementIR &old_template_element =
        old_template_elements[i];
    const TemplateElementIR &new_template_element =
        new_template_elements[i];
    auto template_element_diff = CompareAndDumpTypeDiff(
        old_template_element.GetReferencedType(),
        new_template_element.GetReferencedType(), diff_kind);
    if (template_element_diff.HasDiff()) {
      final_diff_status.CombineWith(template_element_diff);
    }
    i++;
  }
  return final_diff_status;
}

template <typename DiffContainer, typename T>
static std::vector<DiffContainer> ConvertToDiffContainerVector(
    std::vector<std::pair<T, T>> &nc_vector) {
  std::vector<DiffContainer> cptr_vec;
  for (auto &e : nc_vector) {
    cptr_vec.emplace_back(&e.first, &e.second);
  }
  return cptr_vec;
}

template <typename T>
static std::vector<const T*> ConvertToConstPtrVector(
    std::vector<T> &nc_vector) {
  std::vector<const T*> cptr_vec;
  for (auto &e : nc_vector) {
    cptr_vec.emplace_back(&e);
  }
  return cptr_vec;
}

static std::vector<RecordFieldIR> FixupRemovedFieldTypeIds(
    const std::vector<const RecordFieldIR *> &removed_fields,
    const AbiElementMap<const TypeIR *> &old_types) {
  std::vector<RecordFieldIR> removed_fields_dup;
  for (auto &removed_field : removed_fields) {
    removed_fields_dup.emplace_back(*removed_field);
    RecordFieldIR &it = removed_fields_dup[removed_fields_dup.size() -1];
    it.SetReferencedType(
        ConvertTypeIdToString(old_types, it.GetReferencedType()));
  }
  return removed_fields_dup;
}

std::vector<std::pair<RecordFieldIR, RecordFieldIR>>
AbiDiffHelper::FixupDiffedFieldTypeIds(
    const std::vector<RecordFieldDiffIR> &field_diffs) {
  std::vector<std::pair<RecordFieldIR, RecordFieldIR>>
      diffed_fields_dup;
  for (auto &field_diff : field_diffs) {
    diffed_fields_dup.emplace_back(*(field_diff.old_field_),
                                   *(field_diff.new_field_));
    auto &it = diffed_fields_dup[diffed_fields_dup.size() - 1];
    RecordFieldIR &old_field = it.first;
    RecordFieldIR &new_field = it.second;
    old_field.SetReferencedType(
        ConvertTypeIdToString(old_types_, old_field.GetReferencedType()));
    new_field.SetReferencedType(
        ConvertTypeIdToString(new_types_, new_field.GetReferencedType()));
  }
  return diffed_fields_dup;
}

DiffStatus AbiDiffHelper::CompareFunctionTypes(
    const CFunctionLikeIR *old_type, const CFunctionLikeIR *new_type,
    DiffMessageIR::DiffKind diff_kind) {
  DiffStatus status = CompareFunctionParameters(
      old_type->GetParameters(), new_type->GetParameters(), diff_kind);
  status.CombineWith(CompareReturnTypes(old_type->GetReturnType(),
                                        new_type->GetReturnType(), diff_kind));
  return status;
}

DiffStatus AbiDiffHelper::CompareRecordTypes(
    const RecordTypeIR *old_type, const RecordTypeIR *new_type,
    DiffMessageIR::DiffKind diff_kind) {
  auto record_type_diff_ir = std::make_unique<RecordTypeDiffIR>();
  // Compare names.
  if (!old_type->IsAnonymous() && !new_type->IsAnonymous() &&
      old_type->GetLinkerSetKey() != new_type->GetLinkerSetKey()) {
    // Do not dump anything since the record types themselves are fundamentally
    // different.
    return DiffStatus::kDirectDiff;
  }
  DiffStatus final_diff_status = DiffStatus::kNoDiff;
  record_type_diff_ir->SetName(old_type->GetName());
  record_type_diff_ir->SetLinkerSetKey(old_type->GetLinkerSetKey());

  DiffStatus access_diff_status =
      CompareAccess(old_type->GetAccess(), new_type->GetAccess());
  final_diff_status.CombineWith(access_diff_status);
  if (access_diff_status.HasDiff()) {
    record_type_diff_ir->SetAccessDiff(
        std::make_unique<AccessSpecifierDiffIR>(
            old_type->GetAccess(), new_type->GetAccess()));
  }

  if (!CompareSizeAndAlignment(old_type, new_type)) {
    if (old_type->GetSize() < new_type->GetSize() &&
        old_type->GetAlignment() == new_type->GetAlignment()) {
      final_diff_status.CombineWith(DiffStatus::kDirectExt);
    } else {
      final_diff_status.CombineWith(DiffStatus::kDirectDiff);
    }
    record_type_diff_ir->SetTypeDiff(
        std::make_unique<TypeDiffIR>(
            std::make_pair(old_type->GetSize(), new_type->GetSize()),
            std::make_pair(old_type->GetAlignment(),
                           new_type->GetAlignment())));
  }

  const std::vector<VTableComponentIR> &old_vtable =
      old_type->GetVTableLayout().GetVTableComponents();
  const std::vector<VTableComponentIR> &new_vtable =
      new_type->GetVTableLayout().GetVTableComponents();
  if (!CompareVTables(old_vtable, new_vtable)) {
    if (IsVTableExtended(old_vtable, new_vtable)) {
      final_diff_status.CombineWith(DiffStatus::kDirectExt);
    } else {
      final_diff_status.CombineWith(DiffStatus::kDirectDiff);
    }
    record_type_diff_ir->SetVTableLayoutDiff(
        std::make_unique<VTableLayoutDiffIR>(
            old_type->GetVTableLayout(), new_type->GetVTableLayout()));
  }

  auto &old_fields_dup = old_type->GetFields();
  auto &new_fields_dup = new_type->GetFields();
  RecordFieldDiffResult field_status_and_diffs =
      CompareRecordFields(old_fields_dup, new_fields_dup, diff_kind);
  final_diff_status.CombineWith(field_status_and_diffs.status);

  std::vector<CXXBaseSpecifierIR> old_bases = old_type->GetBases();
  std::vector<CXXBaseSpecifierIR> new_bases = new_type->GetBases();
  if (!CompareBaseSpecifiers(old_bases, new_bases, diff_kind) &&
      ir_diff_dumper_) {
    final_diff_status.CombineWith(DiffStatus::kDirectDiff);
    ReplaceReferencesOtherTypeIdWithName(old_types_, old_bases);
    ReplaceReferencesOtherTypeIdWithName(new_types_, new_bases);
    record_type_diff_ir->SetBaseSpecifierDiffs(
        std::make_unique<CXXBaseSpecifierDiffIR>(old_bases, new_bases));
  }
  if (ir_diff_dumper_) {
    // Make copies of the fields removed and diffed, since we have to change
    // type ids -> type strings.
    std::vector<std::pair<RecordFieldIR, RecordFieldIR>> field_diff_dups =
        FixupDiffedFieldTypeIds(field_status_and_diffs.diffed_fields);
    std::vector<RecordFieldDiffIR> field_diffs_fixed =
        ConvertToDiffContainerVector<RecordFieldDiffIR,
                                     RecordFieldIR>(field_diff_dups);

    std::vector<RecordFieldIR> field_removed_dups = FixupRemovedFieldTypeIds(
        field_status_and_diffs.removed_fields, old_types_);
    std::vector<const RecordFieldIR *> fields_removed_fixed =
        ConvertToConstPtrVector(field_removed_dups);

    std::vector<RecordFieldIR> field_added_dups = FixupRemovedFieldTypeIds(
        field_status_and_diffs.added_fields, new_types_);
    std::vector<const RecordFieldIR *> fields_added_fixed =
        ConvertToConstPtrVector(field_added_dups);

    record_type_diff_ir->SetFieldDiffs(std::move(field_diffs_fixed));
    record_type_diff_ir->SetFieldsRemoved(std::move(fields_removed_fixed));
    record_type_diff_ir->SetFieldsAdded(std::move(fields_added_fixed));
    record_type_diff_ir->SetExtended(final_diff_status.IsExtension());

    if (final_diff_status.IsDirectDiff() &&
        !ir_diff_dumper_->AddDiffMessageIR(record_type_diff_ir.get(),
                                           UnwindTypeStack(), diff_kind)) {
      llvm::errs() << "AddDiffMessage on record type failed\n";
      ::exit(1);
    }
  }

  final_diff_status.CombineWith(
      CompareTemplateInfo(old_type->GetTemplateElements(),
                          new_type->GetTemplateElements(), diff_kind));

  return (final_diff_status.HasDiff() ? DiffStatus::kIndirectDiff
                                      : DiffStatus::kNoDiff);
}

DiffStatus AbiDiffHelper::CompareLvalueReferenceTypes(
    const LvalueReferenceTypeIR *old_type,
    const LvalueReferenceTypeIR *new_type, DiffMessageIR::DiffKind diff_kind) {
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(), diff_kind);
}

DiffStatus AbiDiffHelper::CompareRvalueReferenceTypes(
    const RvalueReferenceTypeIR *old_type,
    const RvalueReferenceTypeIR *new_type, DiffMessageIR::DiffKind diff_kind) {
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(), diff_kind);
}

DiffStatus AbiDiffHelper::CompareQualifiedTypes(
    const QualifiedTypeIR *old_type, const QualifiedTypeIR *new_type,
    DiffMessageIR::DiffKind diff_kind) {
  // If all the qualifiers are not the same, return direct_diff, else
  // recursively compare the unqualified types.
  if (old_type->IsConst() != new_type->IsConst() ||
      old_type->IsVolatile() != new_type->IsVolatile() ||
      old_type->IsRestricted() != new_type->IsRestricted()) {
    return DiffStatus::kDirectDiff;
  }
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(), diff_kind);
}

DiffStatus AbiDiffHelper::CompareArrayTypes(const ArrayTypeIR *old_type,
                                            const ArrayTypeIR *new_type,
                                            DiffMessageIR::DiffKind diff_kind) {
  if (!CompareSizeAndAlignment(old_type, new_type) ||
      old_type->IsOfUnknownBound() != new_type->IsOfUnknownBound()) {
    return DiffStatus::kDirectDiff;
  }
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(), diff_kind);
}

DiffStatus AbiDiffHelper::ComparePointerTypes(
    const PointerTypeIR *old_type, const PointerTypeIR *new_type,
    DiffMessageIR::DiffKind diff_kind) {
  // The following need to be the same for two pointer types to be considered
  // equivalent:
  // 1) Number of pointer indirections are the same.
  // 2) The ultimate pointee is the same.
  assert(CompareSizeAndAlignment(old_type, new_type));
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(), diff_kind);
}

DiffStatus AbiDiffHelper::CompareBuiltinTypes(
    const BuiltinTypeIR *old_type,
    const BuiltinTypeIR *new_type) {
  // If the size, alignment and is_unsigned are the same, return no_diff
  // else return direct_diff.
  if (!CompareSizeAndAlignment(old_type, new_type) ||
      old_type->IsUnsigned() != new_type->IsUnsigned() ||
      old_type->IsIntegralType() != new_type->IsIntegralType()) {
    return DiffStatus::kDirectDiff;
  }
  return DiffStatus::kNoDiff;
}

DiffStatus AbiDiffHelper::CompareFunctionParameters(
    const std::vector<ParamIR> &old_parameters,
    const std::vector<ParamIR> &new_parameters,
    DiffMessageIR::DiffKind diff_kind) {
  size_t old_parameters_size = old_parameters.size();
  if (old_parameters_size != new_parameters.size()) {
    return DiffStatus::kDirectDiff;
  }
  DiffStatus result = DiffStatus::kNoDiff;
  for (uint64_t i = 0; i < old_parameters_size; i++) {
    const ParamIR &old_parameter = old_parameters.at(i);
    const ParamIR &new_parameter = new_parameters.at(i);
    result.CombineWith(CompareParameterTypes(old_parameter.GetReferencedType(),
                                             new_parameter.GetReferencedType(),
                                             diff_kind));
    if (old_parameter.GetIsDefault() != new_parameter.GetIsDefault()) {
      result.CombineWith(DiffStatus::kDirectDiff);
    }
  }
  return result;
}

static const TypeIR *FindTypeById(
    const AbiElementMap<const TypeIR *> &type_graph,
    const std::string &type_id) {
  auto it = type_graph.find(type_id);
  return it == type_graph.end() ? nullptr : it->second;
}

struct Qualifiers {
  bool is_const = false;
  bool is_restricted = false;
  bool is_volatile = false;

  bool operator==(const Qualifiers &other) const {
    return (is_const == other.is_const &&
            is_restricted == other.is_restricted &&
            is_volatile == other.is_volatile);
  }

  bool operator!=(const Qualifiers &other) const { return !(*this == other); }
};

// This function returns the qualifiers and sets type_id to the unqalified or
// opaque type.
static Qualifiers ResolveQualifiers(const AbiElementMap<const TypeIR *> &types,
                                    std::string &type_id) {
  Qualifiers qual;
  while (true) {
    const TypeIR *type_ir = FindTypeById(types, type_id);
    if (type_ir == nullptr ||
        type_ir->GetKind() != LinkableMessageKind::QualifiedTypeKind) {
      return qual;
    }
    const QualifiedTypeIR *qualified_type_ir =
        static_cast<const QualifiedTypeIR *>(type_ir);
    qual.is_const |= qualified_type_ir->IsConst();
    qual.is_restricted |= qualified_type_ir->IsRestricted();
    qual.is_volatile |= qualified_type_ir->IsVolatile();
    type_id = qualified_type_ir->GetReferencedType();
  }
}

// This function returns whether the old_type can be implicitly casted to
// new_type. It resolves qualified pointers and references until it reaches a
// type that does not reference other types. It does not compare the final
// referenced types.
//
// If this function returns true, old_type_id and new_type_id are set to the
// final referenced types. are_qualifiers_equal represents whether the
// qualifiers are exactly the same.
//
// If this function returns false, old_type_id, new_type_id, and
// are_qualifiers_equal do not have valid values.
//
// This function follows C++ standard to determine whether qualifiers can be
// casted. The rules are described in
// Section 7.5 Qualification conversions [conv.qual] in C++17 standard
// and
// https://en.cppreference.com/w/cpp/language/implicit_conversion#Qualification_conversions
// Additionally, __restrict__ follows the same rules as const and volatile.
static bool ResolveImplicitlyConvertibleQualifiedReferences(
    const AbiElementMap<const TypeIR *> &old_types,
    const AbiElementMap<const TypeIR *> &new_types, std::string &old_type_id,
    std::string &new_type_id, bool &are_qualifiers_equal) {
  are_qualifiers_equal = true;
  bool is_first_level = true;
  bool is_const_since_second_level = true;
  while (true) {
    // Check qualifiers.
    const Qualifiers old_qual = ResolveQualifiers(old_types, old_type_id);
    const Qualifiers new_qual = ResolveQualifiers(new_types, new_type_id);
    are_qualifiers_equal &= (old_qual == new_qual);
    if (is_first_level) {
      is_first_level = false;
    } else {
      if ((old_qual.is_const && !new_qual.is_const) ||
          (old_qual.is_restricted && !new_qual.is_restricted) ||
          (old_qual.is_volatile && !new_qual.is_volatile)) {
        return false;
      }
      if (!is_const_since_second_level && old_qual != new_qual) {
        return false;
      }
      is_const_since_second_level &= new_qual.is_const;
    }
    // Stop if the unqualified types differ or don't reference other types.
    const TypeIR *old_type = FindTypeById(old_types, old_type_id);
    const TypeIR *new_type = FindTypeById(new_types, new_type_id);
    if (old_type == nullptr || new_type == nullptr) {
      return true;
    }
    const LinkableMessageKind kind = old_type->GetKind();
    if (kind != new_type->GetKind()) {
      return true;
    }
    if (kind != LinkableMessageKind::PointerTypeKind &&
        kind != LinkableMessageKind::LvalueReferenceTypeKind &&
        kind != LinkableMessageKind::RvalueReferenceTypeKind) {
      return true;
    }
    // Get the referenced types.
    old_type_id = old_type->GetReferencedType();
    new_type_id = new_type->GetReferencedType();
  }
}

DiffStatus AbiDiffHelper::CompareParameterTypes(
    const std::string &old_type_id, const std::string &new_type_id,
    DiffMessageIR::DiffKind diff_kind) {
  // Compare size and alignment.
  const TypeIR *old_type_ir = FindTypeById(old_types_, old_type_id);
  const TypeIR *new_type_ir = FindTypeById(new_types_, new_type_id);
  if (old_type_ir != nullptr && new_type_ir != nullptr &&
      !CompareSizeAndAlignment(old_type_ir, new_type_ir)) {
    return DiffStatus::kDirectDiff;
  }
  // Allow the new parameter to be more qualified than the old parameter.
  std::string old_referenced_type_id = old_type_id;
  std::string new_referenced_type_id = new_type_id;
  bool are_qualifiers_equal;
  if (!ResolveImplicitlyConvertibleQualifiedReferences(
          old_types_, new_types_, old_referenced_type_id,
          new_referenced_type_id, are_qualifiers_equal)) {
    return DiffStatus::kDirectDiff;
  }
  // Compare the unqualified referenced types.
  DiffStatus result = CompareAndDumpTypeDiff(old_referenced_type_id,
                                             new_referenced_type_id, diff_kind);
  if (!are_qualifiers_equal) {
    result.CombineWith(DiffStatus::kDirectExt);
  }
  return result;
}

// This function is the same as CompareParameterTypes except for the arguments
// to ResolveImplicitlyConvertibleQualifiedReferences.
DiffStatus AbiDiffHelper::CompareReturnTypes(
    const std::string &old_type_id, const std::string &new_type_id,
    DiffMessageIR::DiffKind diff_kind) {
  // Compare size and alignment.
  const TypeIR *old_type_ir = FindTypeById(old_types_, old_type_id);
  const TypeIR *new_type_ir = FindTypeById(new_types_, new_type_id);
  if (old_type_ir != nullptr && new_type_ir != nullptr &&
      !CompareSizeAndAlignment(old_type_ir, new_type_ir)) {
    return DiffStatus::kDirectDiff;
  }
  // Allow the new return type to be less qualified than the old return type.
  std::string old_referenced_type_id = old_type_id;
  std::string new_referenced_type_id = new_type_id;
  bool are_qualifiers_equal;
  if (!ResolveImplicitlyConvertibleQualifiedReferences(
          new_types_, old_types_, new_referenced_type_id,
          old_referenced_type_id, are_qualifiers_equal)) {
    return DiffStatus::kDirectDiff;
  }
  // Compare the unqualified referenced types.
  DiffStatus result = CompareAndDumpTypeDiff(old_referenced_type_id,
                                             new_referenced_type_id, diff_kind);
  if (!are_qualifiers_equal) {
    result.CombineWith(DiffStatus::kDirectExt);
  }
  return result;
}

DiffStatus AbiDiffHelper::CompareAndDumpTypeDiff(
    const TypeIR *old_type, const TypeIR *new_type, LinkableMessageKind kind,
    DiffMessageIR::DiffKind diff_kind) {
  if (ignored_linker_set_keys_.find(new_type->GetLinkerSetKey()) !=
      ignored_linker_set_keys_.end()) {
    return DiffStatus::kNoDiff;
  }

  switch (kind) {
    case LinkableMessageKind::BuiltinTypeKind:
      return CompareBuiltinTypes(static_cast<const BuiltinTypeIR *>(old_type),
                                 static_cast<const BuiltinTypeIR *>(new_type));
    case LinkableMessageKind::QualifiedTypeKind:
      return CompareQualifiedTypes(
          static_cast<const QualifiedTypeIR *>(old_type),
          static_cast<const QualifiedTypeIR *>(new_type), diff_kind);
    case LinkableMessageKind::ArrayTypeKind:
      return CompareArrayTypes(static_cast<const ArrayTypeIR *>(old_type),
                               static_cast<const ArrayTypeIR *>(new_type),
                               diff_kind);
    case LinkableMessageKind::EnumTypeKind:
      return CompareEnumTypes(static_cast<const EnumTypeIR *>(old_type),
                              static_cast<const EnumTypeIR *>(new_type),
                              diff_kind);

    case LinkableMessageKind::LvalueReferenceTypeKind:
      return CompareLvalueReferenceTypes(
          static_cast<const LvalueReferenceTypeIR *>(old_type),
          static_cast<const LvalueReferenceTypeIR *>(new_type), diff_kind);
    case LinkableMessageKind::RvalueReferenceTypeKind:
      return CompareRvalueReferenceTypes(
          static_cast<const RvalueReferenceTypeIR *>(old_type),
          static_cast<const RvalueReferenceTypeIR *>(new_type), diff_kind);
    case LinkableMessageKind::PointerTypeKind:
      return ComparePointerTypes(static_cast<const PointerTypeIR *>(old_type),
                                 static_cast<const PointerTypeIR *>(new_type),
                                 diff_kind);

    case LinkableMessageKind::RecordTypeKind:
      return CompareRecordTypes(static_cast<const RecordTypeIR *>(old_type),
                                static_cast<const RecordTypeIR *>(new_type),
                                diff_kind);

    case LinkableMessageKind::FunctionTypeKind: {
      DiffStatus result = CompareFunctionTypes(
          static_cast<const FunctionTypeIR *>(old_type),
          static_cast<const FunctionTypeIR *>(new_type), diff_kind);
      // Do not allow extending function pointers, function references, etc.
      if (result.IsExtension()) {
        result.CombineWith(DiffStatus::kDirectDiff);
      }
      return result;
    }
    case LinkableMessageKind::FunctionKind:
    case LinkableMessageKind::GlobalVarKind:
      llvm::errs() << "Unexpected LinkableMessageKind: " << kind << "\n";
      ::exit(1);
  }
}

static DiffStatus CompareDistinctKindMessages(
    const TypeIR *old_type, const TypeIR *new_type) {
  // For these types to be considered ABI compatible, the very least requirement
  // is that their sizes and alignments should be equal.
  // TODO: Fill in
  return DiffStatus::kDirectDiff;
}

DiffStatus AbiDiffHelper::CompareAndDumpTypeDiff(
    const std::string &old_type_id, const std::string &new_type_id,
    DiffMessageIR::DiffKind diff_kind) {
  // Check the map for type ids which have already been compared
  // These types have already been diffed, return without further comparison.
  if (!type_cache_->insert(old_type_id + new_type_id).second) {
    return DiffStatus::kNoDiff;
  }

  TypeStackGuard guard(type_stack_,
                       ConvertTypeIdToString(old_types_, old_type_id));

  AbiElementMap<const TypeIR *>::const_iterator old_it =
      old_types_.find(old_type_id);
  AbiElementMap<const TypeIR *>::const_iterator new_it =
      new_types_.find(new_type_id);

  if (old_it == old_types_.end() || new_it == new_types_.end()) {
    // One of the types were hidden, we cannot compare further.
    return AreOpaqueTypesEqual(old_type_id, new_type_id)
               ? DiffStatus::kNoDiff
               : DiffStatus::kDirectDiff;
  }

  LinkableMessageKind old_kind = old_it->second->GetKind();
  LinkableMessageKind new_kind = new_it->second->GetKind();
  DiffStatus diff_status = DiffStatus::kNoDiff;
  if (old_kind != new_kind) {
    diff_status = CompareDistinctKindMessages(old_it->second, new_it->second);
  } else {
    diff_status = CompareAndDumpTypeDiff(old_it->second, new_it->second,
                                         old_kind, diff_kind);
  }
  return diff_status;
}


}  // namespace repr
}  // namespace header_checker

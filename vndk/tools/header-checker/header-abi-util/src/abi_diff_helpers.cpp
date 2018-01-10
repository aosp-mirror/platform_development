#include <abi_diff_helpers.h>
#include <header_abi_util.h>

#include <llvm/Support/raw_ostream.h>

namespace abi_util {

std::string Unwind(const std::deque<std::string> *type_queue) {
  if (!type_queue) {
    return "";
  }
  std::string stack_str;
  std::deque<std::string> type_queue_copy = *type_queue;
  while (!type_queue_copy.empty()) {
    stack_str += type_queue_copy.front() + "-> ";
    type_queue_copy.pop_front();
  }
  return stack_str;
}

static void TypeQueueCheckAndPushBack(std::deque<std::string> *type_queue,
                                  const std::string &str) {
  if (type_queue) {
    type_queue->push_back(str);
  }
}

static void TypeQueueCheckAndPop(std::deque<std::string> *type_queue) {
 if (type_queue && !type_queue->empty()) {
      type_queue->pop_back();
    }
}

static bool IsAccessDownGraded(abi_util::AccessSpecifierIR old_access,
                               abi_util::AccessSpecifierIR new_access) {
  bool access_downgraded = false;
  switch (old_access) {
    case abi_util::AccessSpecifierIR::ProtectedAccess:
      if (new_access == abi_util::AccessSpecifierIR::PrivateAccess) {
        access_downgraded = true;
      }
      break;
    case abi_util::AccessSpecifierIR::PublicAccess:
      if (new_access != abi_util::AccessSpecifierIR::PublicAccess) {
        access_downgraded = true;
      }
      break;
    default:
      break;
  }
  return access_downgraded;
}

void AbiDiffHelper::CompareEnumFields(
    const std::vector<abi_util::EnumFieldIR> &old_fields,
    const std::vector<abi_util::EnumFieldIR> &new_fields,
    abi_util::EnumTypeDiffIR *enum_type_diff_ir) {
  AbiElementMap<const abi_util::EnumFieldIR *> old_fields_map;
  AbiElementMap<const abi_util::EnumFieldIR *> new_fields_map;
  abi_util::AddToMap(&old_fields_map, old_fields,
                     [](const abi_util::EnumFieldIR *f) {return f->GetName();},
                     [](const abi_util::EnumFieldIR *f) {return f;});

  abi_util::AddToMap(&new_fields_map, new_fields,
                     [](const abi_util::EnumFieldIR *f) {return f->GetName();},
                     [](const abi_util::EnumFieldIR *f) {return f;});

  std::vector<const abi_util::EnumFieldIR *> removed_fields =
      abi_util::FindRemovedElements(old_fields_map, new_fields_map);

  std::vector<const abi_util::EnumFieldIR *> added_fields =
      abi_util::FindRemovedElements(new_fields_map, old_fields_map);

  enum_type_diff_ir->SetFieldsAdded(std::move(added_fields));

  enum_type_diff_ir->SetFieldsRemoved(std::move(removed_fields));

  std::vector<std::pair<
      const abi_util::EnumFieldIR *, const abi_util::EnumFieldIR *>> cf =
      abi_util::FindCommonElements(old_fields_map, new_fields_map);
  std::vector<abi_util::EnumFieldDiffIR> enum_field_diffs;
  for (auto &&common_fields : cf) {
    if (common_fields.first->GetValue() != common_fields.second->GetValue()) {
      abi_util::EnumFieldDiffIR enum_field_diff_ir(common_fields.first,
                                                   common_fields.second);
      enum_field_diffs.emplace_back(std::move(enum_field_diff_ir));
    }
  }
  enum_type_diff_ir->SetFieldsDiff(std::move(enum_field_diffs));
}

DiffStatus AbiDiffHelper::CompareEnumTypes(
    const abi_util::EnumTypeIR *old_type, const abi_util::EnumTypeIR *new_type,
     std::deque<std::string> *type_queue,
     abi_util::DiffMessageIR::DiffKind diff_kind) {
  if (old_type->GetUniqueId() != new_type->GetUniqueId()) {
    return DiffStatus::direct_diff;
  }
  auto enum_type_diff_ir = std::make_unique<abi_util::EnumTypeDiffIR>();
  enum_type_diff_ir->SetName(old_type->GetName());
  const std::string &old_underlying_type = old_type->GetUnderlyingType();
  const std::string &new_underlying_type = new_type->GetUnderlyingType();
  if (old_underlying_type != new_underlying_type) {
    enum_type_diff_ir->SetUnderlyingTypeDiff(
        std::make_unique<std::pair<std::string, std::string>>(
            old_underlying_type, new_underlying_type));
  }
  CompareEnumFields(old_type->GetFields(), new_type->GetFields(),
                    enum_type_diff_ir.get());
  if ((enum_type_diff_ir->IsExtended() ||
       enum_type_diff_ir->IsIncompatible()) &&
      (ir_diff_dumper_ && !ir_diff_dumper_->AddDiffMessageIR(
          enum_type_diff_ir.get(), Unwind(type_queue), diff_kind))) {
    llvm::errs() << "AddDiffMessage on EnumTypeDiffIR failed\n";
    ::exit(1);
  }
  return DiffStatus::no_diff;
}

bool AbiDiffHelper::CompareVTableComponents(
    const abi_util::VTableComponentIR &old_component,
    const abi_util::VTableComponentIR &new_component) {
  return old_component.GetName() == new_component.GetName() &&
      old_component.GetValue() == new_component.GetValue() &&
      old_component.GetKind() == new_component.GetKind();
}

bool AbiDiffHelper::CompareVTables(
    const abi_util::RecordTypeIR *old_record,
    const abi_util::RecordTypeIR *new_record) {

  const std::vector<abi_util::VTableComponentIR> &old_components =
      old_record->GetVTableLayout().GetVTableComponents();
  const std::vector<abi_util::VTableComponentIR> &new_components =
      new_record->GetVTableLayout().GetVTableComponents();
  if (old_components.size() > new_components.size()) {
    // Something in the vtable got deleted.
    return false;
  }
  uint32_t i = 0;
  while (i < old_components.size()) {
    auto &old_element = old_components.at(i);
    auto &new_element = new_components.at(i);
    if (!CompareVTableComponents(old_element, new_element)) {
      return false;
    }
    i++;
  }
  return true;
}

bool AbiDiffHelper::CompareSizeAndAlignment(
    const abi_util::TypeIR *old_type,
    const abi_util::TypeIR *new_type) {
  return old_type->GetSize() == new_type->GetSize() &&
      old_type->GetAlignment() == new_type->GetAlignment();
}

DiffStatusPair<std::unique_ptr<abi_util::RecordFieldDiffIR>>
AbiDiffHelper::CompareCommonRecordFields(
    const abi_util::RecordFieldIR *old_field,
    const abi_util::RecordFieldIR *new_field,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {

  DiffStatus field_diff_status =
      CompareAndDumpTypeDiff(old_field->GetReferencedType(),
                             new_field->GetReferencedType(),
                             type_queue, diff_kind);

  if (old_field->GetOffset() != new_field->GetOffset() ||
      // TODO: Should this be an inquality check instead ? Some compilers can
      // make signatures dependant on absolute values of access specifiers.
      IsAccessDownGraded(old_field->GetAccess(), new_field->GetAccess()) ||
      (field_diff_status == DiffStatus::direct_diff)) {
    return std::make_pair(
        DiffStatus::direct_diff,
        std::make_unique<abi_util::RecordFieldDiffIR>(old_field, new_field)
        );
  }
  return std::make_pair(field_diff_status, nullptr);
}

DiffStatusPair<std::pair<std::vector<abi_util::RecordFieldDiffIR>,
                         std::vector<const abi_util::RecordFieldIR *>>>
AbiDiffHelper::CompareRecordFields(
    const std::vector<abi_util::RecordFieldIR> &old_fields,
    const std::vector<abi_util::RecordFieldIR> &new_fields,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  std::pair<std::vector<abi_util::RecordFieldDiffIR>,
  std::vector<const abi_util::RecordFieldIR *>> diffed_and_removed_fields;
  AbiElementMap<const abi_util::RecordFieldIR *> old_fields_map;
  AbiElementMap<const abi_util::RecordFieldIR *> new_fields_map;
  std::map<uint64_t, const abi_util::RecordFieldIR *> old_fields_offset_map;
  std::map<uint64_t, const abi_util::RecordFieldIR *> new_fields_offset_map;

  abi_util::AddToMap(
      &old_fields_map, old_fields,
      [](const abi_util::RecordFieldIR *f) {return f->GetName();},
      [](const abi_util::RecordFieldIR *f) {return f;});
  abi_util::AddToMap(
      &new_fields_map, new_fields,
      [](const abi_util::RecordFieldIR *f) {return f->GetName();},
      [](const abi_util::RecordFieldIR *f) {return f;});
  abi_util::AddToMap(
      &old_fields_offset_map, old_fields,
      [](const abi_util::RecordFieldIR *f) {return f->GetOffset();},
      [](const abi_util::RecordFieldIR *f) {return f;});
  abi_util::AddToMap(
      &new_fields_offset_map, new_fields,
      [](const abi_util::RecordFieldIR *f) {return f->GetOffset();},
      [](const abi_util::RecordFieldIR *f) {return f;});
  // If a field is removed from the map field_name -> offset see if another
  // field is present at the same offset and compare the size and type etc,
  // remove it from the removed fields if they're compatible.
  DiffStatus final_diff_status = DiffStatus::no_diff;
  std::vector<const abi_util::RecordFieldIR *> removed_fields =
      abi_util::FindRemovedElements(old_fields_map, new_fields_map);
  auto predicate =
      [&](const abi_util::RecordFieldIR *removed_field) {
        uint64_t old_field_offset = removed_field->GetOffset();
        auto corresponding_field_at_same_offset =
            new_fields_offset_map.find(old_field_offset);
        // Correctly reported as removed, so do not remove.
        if (corresponding_field_at_same_offset == new_fields_offset_map.end()) {
          return false;
        }
        auto comparison_result = CompareCommonRecordFields(
            removed_field, corresponding_field_at_same_offset->second,
            type_queue, diff_kind);
        // No actual diff, so remove it.
        return (comparison_result.second == nullptr);
      };
  removed_fields.erase(
      std::remove_if(removed_fields.begin(), removed_fields.end(), predicate),
      removed_fields.end());
  diffed_and_removed_fields.second = std::move(removed_fields);
  std::vector<std::pair<
      const abi_util::RecordFieldIR *, const abi_util::RecordFieldIR *>> cf =
      abi_util::FindCommonElements(old_fields_map, new_fields_map);
  bool common_field_diff_exists = false;
  for (auto &&common_fields : cf) {
    auto diffed_field_ptr = CompareCommonRecordFields(common_fields.first,
                                                      common_fields.second,
                                                      type_queue, diff_kind);
    if (!common_field_diff_exists &&
        (diffed_field_ptr.first &
        (DiffStatus::direct_diff | DiffStatus::indirect_diff))) {
        common_field_diff_exists = true;
    }
    if (diffed_field_ptr.second != nullptr) {
      diffed_and_removed_fields.first.emplace_back(
          std::move(*(diffed_field_ptr.second.release())));
    }
  }
  if (diffed_and_removed_fields.first.size() != 0 ||
      diffed_and_removed_fields.second.size() != 0) {
    final_diff_status = DiffStatus::direct_diff;
  } else if (common_field_diff_exists) {
    final_diff_status = DiffStatus::indirect_diff;
  }
  return std::make_pair(final_diff_status, diffed_and_removed_fields);
}

bool AbiDiffHelper::CompareBaseSpecifiers(
    const std::vector<abi_util::CXXBaseSpecifierIR> &old_base_specifiers,
    const std::vector<abi_util::CXXBaseSpecifierIR> &new_base_specifiers,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  if (old_base_specifiers.size() != new_base_specifiers.size()) {
    return false;
  }
  int i = 0;
  while (i < old_base_specifiers.size()) {
    if (CompareAndDumpTypeDiff(old_base_specifiers.at(i).GetReferencedType(),
                               new_base_specifiers.at(i).GetReferencedType(),
                               type_queue, diff_kind) ==
        DiffStatus::direct_diff ||
        (old_base_specifiers.at(i).GetAccess() !=
         new_base_specifiers.at(i).GetAccess())) {
      return false;
    }
    i++;
  }
  return true;
}

void AbiDiffHelper::CompareTemplateInfo(
    const std::vector<abi_util::TemplateElementIR> &old_template_elements,
    const std::vector<abi_util::TemplateElementIR> &new_template_elements,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  uint32_t old_template_size = old_template_elements.size();
  assert(old_template_size == new_template_elements.size());
  uint32_t i = 0;
  while (i < old_template_size) {
    const abi_util::TemplateElementIR &old_template_element =
        old_template_elements[i];
    const abi_util::TemplateElementIR &new_template_element =
        new_template_elements[i];
    CompareAndDumpTypeDiff(old_template_element.GetReferencedType(),
                           new_template_element.GetReferencedType(),
                           type_queue, diff_kind);
    i++;
  }
}

DiffStatus AbiDiffHelper::CompareRecordTypes(
    const abi_util::RecordTypeIR *old_type,
    const abi_util::RecordTypeIR *new_type,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  auto record_type_diff_ir = std::make_unique<abi_util::RecordTypeDiffIR>();
  // Compare names.
  if (!old_type->IsAnonymous() && !new_type->IsAnonymous() &&
      old_type->GetUniqueId() != new_type->GetUniqueId()) {
    // Do not dump anything since the record types themselves are fundamentally
    // different.
    return DiffStatus::direct_diff;
  }
  DiffStatus final_diff_status = DiffStatus::no_diff;
  record_type_diff_ir->SetName(old_type->GetName());
  if (old_type->GetAccess() != new_type->GetAccess()) {
    final_diff_status = DiffStatus::indirect_diff;
    record_type_diff_ir->SetAccessDiff(
        std::make_unique<abi_util::AccessSpecifierDiffIR>(
            old_type->GetAccess(), new_type->GetAccess()));
  }

  if (!CompareSizeAndAlignment(old_type, new_type)) {
    final_diff_status = DiffStatus::indirect_diff;
    record_type_diff_ir->SetTypeDiff(
        std::make_unique<abi_util::TypeDiffIR>(
            std::make_pair(old_type->GetSize(), new_type->GetSize()),
            std::make_pair(old_type->GetAlignment(),
                           new_type->GetAlignment())));
  }
  if (!CompareVTables(old_type, new_type)) {
    final_diff_status = DiffStatus::indirect_diff;
    record_type_diff_ir->SetVTableLayoutDiff(
        std::make_unique<abi_util::VTableLayoutDiffIR>(
            old_type->GetVTableLayout(), new_type->GetVTableLayout()));
  }
  auto field_status_and_diffs =
      CompareRecordFields(old_type->GetFields(), new_type->GetFields(),
                          type_queue, diff_kind);
  // TODO: combine this with base class diffs as well.
  final_diff_status = final_diff_status | field_status_and_diffs.first;
  auto field_diffs = field_status_and_diffs.second;
  record_type_diff_ir->SetFieldDiffs(std::move(field_diffs.first));
  record_type_diff_ir->SetFieldsRemoved(std::move(field_diffs.second));
  const std::vector<abi_util::CXXBaseSpecifierIR> &old_bases =
      old_type->GetBases();
  const std::vector<abi_util::CXXBaseSpecifierIR> &new_bases =
      new_type->GetBases();

  if (!CompareBaseSpecifiers(old_bases, new_bases, type_queue, diff_kind)) {
    record_type_diff_ir->SetBaseSpecifierDiffs (
        std::make_unique<abi_util::CXXBaseSpecifierDiffIR>(old_bases,
                                                           new_bases));
  }
  if (record_type_diff_ir->DiffExists() &&
      ir_diff_dumper_ &&
      !ir_diff_dumper_->AddDiffMessageIR(record_type_diff_ir.get(),
                                         Unwind(type_queue), diff_kind)) {
    llvm::errs() << "AddDiffMessage on record type failed\n";
    ::exit(1);
  } // No need to add a dump for an extension since records can't be "extended".

  CompareTemplateInfo(old_type->GetTemplateElements(),
                      new_type->GetTemplateElements(),
                      type_queue, diff_kind);

  return
      (final_diff_status &
      (DiffStatus::direct_diff | DiffStatus::indirect_diff)) ?
        DiffStatus::indirect_diff : DiffStatus::no_diff;
}

DiffStatus AbiDiffHelper::CompareLvalueReferenceTypes(
    const abi_util::LvalueReferenceTypeIR *old_type,
    const abi_util::LvalueReferenceTypeIR *new_type,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(),
                                type_queue, diff_kind);
}

DiffStatus AbiDiffHelper::CompareRvalueReferenceTypes(
    const abi_util::RvalueReferenceTypeIR *old_type,
    const abi_util::RvalueReferenceTypeIR *new_type,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(),
                                type_queue, diff_kind);
}

DiffStatus AbiDiffHelper::CompareQualifiedTypes(
    const abi_util::QualifiedTypeIR *old_type,
    const abi_util::QualifiedTypeIR *new_type,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  // If all the qualifiers are not the same, return direct_diff, else
  // recursively compare the unqualified types.
  if (old_type->IsConst() != new_type->IsConst() ||
      old_type->IsVolatile() != new_type->IsVolatile() ||
      old_type->IsRestricted() != new_type->IsRestricted()) {
    return DiffStatus::direct_diff;
  }
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(),
                                type_queue, diff_kind);
}

DiffStatus AbiDiffHelper::ComparePointerTypes(
    const abi_util::PointerTypeIR *old_type,
    const abi_util::PointerTypeIR *new_type,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  // The following need to be the same for two pointer types to be considered
  // equivalent:
  // 1) Number of pointer indirections are the same.
  // 2) The ultimate pointee is the same.
  assert(CompareSizeAndAlignment(old_type, new_type));
  return CompareAndDumpTypeDiff(old_type->GetReferencedType(),
                                new_type->GetReferencedType(),
                                type_queue, diff_kind);
}

DiffStatus AbiDiffHelper::CompareBuiltinTypes(
    const abi_util::BuiltinTypeIR *old_type,
    const abi_util::BuiltinTypeIR *new_type) {
  // If the size, alignment and is_unsigned are the same, return no_diff
  // else return direct_diff.
  uint64_t old_signedness = old_type->IsUnsigned();
  uint64_t new_signedness = new_type->IsUnsigned();

  if (!CompareSizeAndAlignment(old_type, new_type) ||
      old_signedness != new_signedness ||
      old_type->IsIntegralType() != new_type->IsIntegralType()) {
    return DiffStatus::direct_diff;
  }
  return DiffStatus::no_diff;
}

DiffStatus AbiDiffHelper::CompareFunctionParameters(
    const std::vector<abi_util::ParamIR> &old_parameters,
    const std::vector<abi_util::ParamIR> &new_parameters,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  size_t old_parameters_size = old_parameters.size();
  if (old_parameters_size != new_parameters.size()) {
    return DiffStatus::direct_diff;
  }
  uint64_t i = 0;
  while (i < old_parameters_size) {
    const abi_util::ParamIR &old_parameter = old_parameters.at(i);
    const abi_util::ParamIR &new_parameter = new_parameters.at(i);
    if ((CompareAndDumpTypeDiff(old_parameter.GetReferencedType(),
                               new_parameter.GetReferencedType(),
                               type_queue, diff_kind) ==
        DiffStatus::direct_diff) ||
        (old_parameter.GetIsDefault() != new_parameter.GetIsDefault())) {
      return DiffStatus::direct_diff;
    }
    i++;
  }
  return DiffStatus::no_diff;
}

DiffStatus AbiDiffHelper::CompareAndDumpTypeDiff(
    const abi_util::TypeIR *old_type, const abi_util::TypeIR *new_type,
    abi_util::LinkableMessageKind kind, std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  if (kind == abi_util::LinkableMessageKind::BuiltinTypeKind) {
    return CompareBuiltinTypes(
        static_cast<const abi_util::BuiltinTypeIR *>(old_type),
        static_cast<const abi_util::BuiltinTypeIR *>(new_type));
  }

  if (kind == abi_util::LinkableMessageKind::QualifiedTypeKind) {
    return CompareQualifiedTypes(
        static_cast<const abi_util::QualifiedTypeIR *>(old_type),
        static_cast<const abi_util::QualifiedTypeIR *>(new_type),
        type_queue, diff_kind);
  }

  if (kind == abi_util::LinkableMessageKind::EnumTypeKind) {
      return CompareEnumTypes(
          static_cast<const abi_util::EnumTypeIR *>(old_type),
          static_cast<const abi_util::EnumTypeIR *>(new_type),
          type_queue, diff_kind);

  }

  if (kind == abi_util::LinkableMessageKind::LvalueReferenceTypeKind) {
    return CompareLvalueReferenceTypes(
        static_cast<const abi_util::LvalueReferenceTypeIR *>(old_type),
        static_cast<const abi_util::LvalueReferenceTypeIR *>(new_type),
        type_queue, diff_kind);

  }

  if (kind == abi_util::LinkableMessageKind::RvalueReferenceTypeKind) {
    return CompareRvalueReferenceTypes(
        static_cast<const abi_util::RvalueReferenceTypeIR *>(old_type),
        static_cast<const abi_util::RvalueReferenceTypeIR *>(new_type),
        type_queue, diff_kind);
  }

  if (kind == abi_util::LinkableMessageKind::PointerTypeKind) {
    return ComparePointerTypes(
        static_cast<const abi_util::PointerTypeIR *>(old_type),
        static_cast<const abi_util::PointerTypeIR *>(new_type),
        type_queue, diff_kind);
  }

  if (kind == abi_util::LinkableMessageKind::RecordTypeKind) {
    return CompareRecordTypes(
        static_cast<const abi_util::RecordTypeIR *>(old_type),
        static_cast<const abi_util::RecordTypeIR *>(new_type),
        type_queue, diff_kind);
  }
  return DiffStatus::no_diff;
}

static DiffStatus CompareDistinctKindMessages(
    const abi_util::TypeIR *old_type, const abi_util::TypeIR *new_type) {
  // For these types to be considered ABI compatible, the very least requirement
  // is that their sizes and alignments should be equal.
  // TODO: Fill in
  return DiffStatus::direct_diff;
}

DiffStatus AbiDiffHelper::CompareAndDumpTypeDiff(
    const std::string &old_type_str, const std::string &new_type_str,
    std::deque<std::string> *type_queue,
    abi_util::DiffMessageIR::DiffKind diff_kind) {
  // If either of the types are not found in their respective maps, the type
  // was not exposed in a public header and we do a simple string comparison.
  // Any diff found using a simple string comparison will be a direct diff.

  // Check the map for type ids which have already been compared
  // These types have already been diffed, return without further comparison.
  if (!type_cache_->insert(old_type_str + new_type_str).second) {
    return DiffStatus::no_diff;
  } else {
    TypeQueueCheckAndPushBack(type_queue, old_type_str);
  }
  AbiElementMap<const abi_util::TypeIR *>::const_iterator old_it =
      old_types_.find(old_type_str);
  AbiElementMap<const abi_util::TypeIR *>::const_iterator new_it =
      new_types_.find(new_type_str);
  if (old_it == old_types_.end() || new_it == new_types_.end()) {
    // Do a simple string comparison.
    TypeQueueCheckAndPop(type_queue);
    return (old_type_str == new_type_str) ?
        DiffStatus::no_diff : DiffStatus::direct_diff;
  }
  abi_util::LinkableMessageKind old_kind =
      old_it->second->GetKind();
  abi_util::LinkableMessageKind new_kind =
      new_it->second->GetKind();
  DiffStatus diff_status = DiffStatus::no_diff;
  if (old_kind != new_kind) {
    diff_status = CompareDistinctKindMessages(old_it->second, new_it->second);
  } else {
    diff_status = CompareAndDumpTypeDiff(old_it->second , new_it->second ,
                                         old_kind, type_queue, diff_kind);
  }
  TypeQueueCheckAndPop(type_queue);
  return diff_status;
}

} // namespace abi_util

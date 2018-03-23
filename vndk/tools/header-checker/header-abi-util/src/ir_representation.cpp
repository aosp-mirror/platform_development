// Copyright (C) 2017 The Android Open Source Project
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

#include <abi_diff_helpers.h>
#include <ir_representation.h>
#include <ir_representation_protobuf.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#pragma clang diagnostic pop

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <llvm/Support/raw_ostream.h>

#include <string>
#include <memory>


namespace abi_util {

using MergeStatus = TextFormatToIRReader::MergeStatus;

std::unique_ptr<IRDumper> IRDumper::CreateIRDumper(
    TextFormatIR text_format, const std::string &dump_path) {
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return std::make_unique<ProtobufIRDumper>(dump_path);
    default:
      // Nothing else is supported yet.
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}

std::unique_ptr<IRDiffDumper> IRDiffDumper::CreateIRDiffDumper(
    TextFormatIR text_format, const std::string &dump_path) {
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return std::make_unique<ProtobufIRDiffDumper>(dump_path);
    default:
      // Nothing else is supported yet.
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}

std::unique_ptr<TextFormatToIRReader>
TextFormatToIRReader::CreateTextFormatToIRReader(
    TextFormatIR text_format, const std::set<std::string> *exported_headers) {
  switch (text_format) {
    case TextFormatIR::ProtobufTextFormat:
      return std::make_unique<ProtobufTextFormatToIRReader>(exported_headers);
    default:
      // Nothing else is supported yet.
      llvm::errs() << "Text format not supported yet\n";
      return nullptr;
  }
}

void TextFormatToIRReader::AddToODRListMap(
    const std::string &key,
    const TypeIR *value) {
  auto map_it = odr_list_map_.find(key);
  if (map_it == odr_list_map_.end()) {
    odr_list_map_.emplace(key, std::list<const TypeIR *>({value}));
    return;
  }
  odr_list_map_[key].emplace_back(value);
}

MergeStatus TextFormatToIRReader::IsBuiltinTypeNodePresent(
    const BuiltinTypeIR *builtin_type, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {

  std::string builtin_linker_set_key = builtin_type->GetLinkerSetKey();
  auto builtin_it = builtin_types_.find(builtin_linker_set_key);
  if (builtin_it != builtin_types_.end()) {
        return MergeStatus(false, builtin_it->second.GetSelfType());
  }
  // Add this builtin type to the parent graph's builtin_types_ map.
  // Before that, correct the type id of the builtin-type.
  const std::string &local_type_id = builtin_type->GetSelfType();
  std::string builtin_global_type_id = AllocateNewTypeId();
  auto it = builtin_types_.emplace(builtin_linker_set_key, *builtin_type);
  it.first->second.SetSelfType(builtin_global_type_id);
  it.first->second.SetReferencedType(builtin_global_type_id);
  type_graph_.emplace(builtin_global_type_id, &((it.first)->second));

  MergeStatus merge_status(true, builtin_global_type_id);
  local_to_global_type_id_map->emplace(local_type_id, merge_status);
  return merge_status;
}

MergeStatus TextFormatToIRReader::DoesUDTypeODRViolationExist(
    const TypeIR *ud_type, const TextFormatToIRReader &addend,
    const std::string ud_type_unique_id_and_source,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map_) {
  // Per entry in the map :
  // /-----------------------------------------------------------------------\
  // | UDType->UniqueTagId + UdType->source File => list(const UDTypeIR *)|
  // \-----------------------------------------------------------------------/
  auto it = odr_list_map_.find(ud_type_unique_id_and_source);
  if (it == odr_list_map_.end()) {
    // Calling this an ODR violation even though it means no UD with the same
    // name + source combination was seen in the parent graph. The type-id
    // passed does not matter since was_newly_added_ is true, the type will get
    // allocated a new type id.
    return MergeStatus(true, "");
  }
  std::set<std::string> type_cache;
  AbiDiffHelper diff_helper(type_graph_, addend.type_graph_, &type_cache,
                            nullptr, local_to_global_type_id_map_);
  for (auto &contender_ud : it->second) {
    if (diff_helper.CompareAndDumpTypeDiff(contender_ud->GetSelfType(),
                                           ud_type->GetSelfType())
        == DiffStatus::no_diff) {
      local_to_global_type_id_map_->emplace(ud_type->GetSelfType(),
                                            MergeStatus(
                                                false,
                                                contender_ud->GetSelfType()));
      return MergeStatus(false, contender_ud->GetSelfType());
    }
  }
#ifdef DEBUG
  llvm::errs() << "ODR violation detected for :" << ud_type->GetName() << "\n";
#endif
  return MergeStatus(true, (*(it->second.begin()))->GetSelfType());
}

MergeStatus TextFormatToIRReader::IsTypeNodePresent(
    const TypeIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  std::string unique_type_id;
  switch(addend_node->GetKind()) {
    case RecordTypeKind:
      unique_type_id =
          GetODRListMapKey(static_cast<const RecordTypeIR *>(addend_node));
      break;
    case EnumTypeKind:
      unique_type_id =
          GetODRListMapKey(static_cast<const EnumTypeIR *>(addend_node));
      break;
    case FunctionTypeKind:
      unique_type_id =
          GetODRListMapKey(static_cast<const FunctionTypeIR *>(addend_node));
      break;
    default:
      // We add the type proactively.
      return MergeStatus(true, "type-hidden");
  }
  // Every other type is a referencing type / builtin type, so it is proactively
  // added by returning MergeStatus with was_newly_added_ = true.
  return DoesUDTypeODRViolationExist(
      addend_node, addend, unique_type_id, local_to_global_type_id_map);
}

// This method merges the type referenced by 'references_type' into the parent
// graph. It also corrects the referenced_type field in the references_type
// object passed and returns the merge status of the *referenced type*.
MergeStatus TextFormatToIRReader::MergeReferencingTypeInternal(
    const TextFormatToIRReader &addend,
    ReferencesOtherType *references_type,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
    // First look in the local_to_global_type_id_map for the referenced type's
    // id.
    const std::string &referenced_type_id =
        references_type->GetReferencedType();
    auto local_to_global_it = local_to_global_type_id_map->find(
        referenced_type_id);
    if (local_to_global_it != local_to_global_type_id_map->end()) {
      // The type was already added to the parent graph. So change the
      // referenced type to the global type id.
      references_type->SetReferencedType(local_to_global_it->second.type_id_);
      return local_to_global_it->second;
    }
    // If that did not go through, look at the addend's type_map_ and get the
    // TypeIR* and call MergeType on it.
    auto local_type_it = addend.type_graph_.find(referenced_type_id);
    if (local_type_it != addend.type_graph_.end()) {
      // We don't care about merge_status.was_newly_added since we wouldn't have
      // gotten this far if we weren't adding this.
      MergeStatus merge_status =
          MergeType(local_type_it->second, addend,
                    local_to_global_type_id_map);
      const std::string &global_type_id = merge_status.type_id_;
      references_type->SetReferencedType(global_type_id);
      return merge_status;
    }
    // The referenced type was hidden, so just set it to type-hidden.
   const std::string &hidden_type_id = AllocateNewTypeId();
   references_type->SetReferencedType(hidden_type_id);
   return MergeStatus(true, hidden_type_id);
}

void TextFormatToIRReader::MergeRecordFields(
    const TextFormatToIRReader &addend, RecordTypeIR *added_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for(auto &field : added_node->GetFields()) {
    MergeReferencingTypeInternal(addend, &field, local_to_global_type_id_map);
  }
}

void TextFormatToIRReader::MergeRecordCXXBases(
    const TextFormatToIRReader &addend, RecordTypeIR *added_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for(auto &base : added_node->GetBases()) {
    MergeReferencingTypeInternal(addend, &base, local_to_global_type_id_map);
  }
}

void TextFormatToIRReader::MergeRecordTemplateElements(
    const TextFormatToIRReader &addend, RecordTypeIR *added_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for(auto &template_element : added_node->GetTemplateElements()) {
    MergeReferencingTypeInternal(addend, &template_element,
                         local_to_global_type_id_map);
  }
}

void TextFormatToIRReader::MergeRecordDependencies(
    const TextFormatToIRReader &addend, RecordTypeIR *added_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // First call MergeType on all its fields.
  MergeRecordFields(addend, added_node, local_to_global_type_id_map);

  // Call MergeType on CXXBases of the record.
  MergeRecordCXXBases(addend, added_node, local_to_global_type_id_map);

  MergeRecordTemplateElements(addend, added_node, local_to_global_type_id_map);
}

template <typename T>
std::pair<MergeStatus, typename AbiElementMap<T>::iterator>
TextFormatToIRReader::UpdateUDTypeAccounting(
    const T *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map,
    AbiElementMap<T> *specific_type_map) {
  std::string added_type_id = AllocateNewTypeId();
  // Add the ud-type with type-id to the type_graph_, since if there are generic
  // reference types which refer to the record being added, they'll need to find
  // it's id in the map.
  // Add ud-type to the parent graph.
  T added_type_ir = *addend_node;
  added_type_ir.SetSelfType(added_type_id);
  added_type_ir.SetReferencedType(added_type_id);
  auto it = AddToMapAndTypeGraph(std::move(added_type_ir), specific_type_map,
                                 &type_graph_);
  // Add to faciliate ODR checking.
  const std::string &key = GetODRListMapKey(&(it->second));
  MergeStatus type_merge_status = MergeStatus(true, added_type_id);
  AddToODRListMap(key, &(it->second));
  local_to_global_type_id_map->emplace(addend_node->GetSelfType(),
                                       type_merge_status);
  return {type_merge_status, it};
}
// This method is necessarily going to have a was_newly_merged_ = true in its
// MergeStatus return. So it necessarily merges a new RecordType.
MergeStatus TextFormatToIRReader::MergeRecordAndDependencies(
  const RecordTypeIR *addend_node, const TextFormatToIRReader &addend,
  AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto merge_status_and_it =
      UpdateUDTypeAccounting(addend_node, addend, local_to_global_type_id_map,
                             &record_types_);
  auto it = merge_status_and_it.second;
  MergeRecordDependencies(addend, &(it->second), local_to_global_type_id_map);
  return merge_status_and_it.first;
}

void TextFormatToIRReader::MergeEnumDependencies(
    const TextFormatToIRReader &addend, EnumTypeIR *added_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string underlying_type_id = added_node->GetUnderlyingType();
  // Get the underlying type, it nessarily has to be present in the addend's
  // type graph since builtin types can't be hidden. Call MergeType on it and
  // change the underlying type to that.
  auto it = addend.type_graph_.find(underlying_type_id);
  if (it == addend.type_graph_.end()) {
    llvm::errs() << "Enum underlying types should not be hidden\n";
    ::exit(1);
  }
  MergeStatus merge_status = MergeType(it->second, addend,
                                       local_to_global_type_id_map);
  added_node->SetUnderlyingType(merge_status.type_id_);
}

// This method is necessarily going to have a was_newly_merged_ = true in its
// MergeStatus return. So it necessarily merges a new EnumType.
MergeStatus TextFormatToIRReader::MergeEnumType(
    const EnumTypeIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto merge_status_and_it =
      UpdateUDTypeAccounting(addend_node, addend, local_to_global_type_id_map,
                             &enum_types_);
  auto it = merge_status_and_it.second;
  MergeEnumDependencies(addend, &(it->second), local_to_global_type_id_map);
  return merge_status_and_it.first;
}

MergeStatus TextFormatToIRReader::MergeFunctionType(
    const FunctionTypeIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto merge_status_and_it =
      UpdateUDTypeAccounting(addend_node, addend, local_to_global_type_id_map,
                             &function_types_);
  auto it = merge_status_and_it.second;
  MergeCFunctionLikeDeps(addend, &(it->second), local_to_global_type_id_map);
  return merge_status_and_it.first;
}

template <typename T>
MergeStatus TextFormatToIRReader::MergeReferencingTypeInternalAndUpdateParent(
    const TextFormatToIRReader &addend, const T *addend_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map,
    AbiElementMap<T> *parent_map, const std::string  &updated_self_type_id) {
    MergeStatus merge_status;
    uint64_t old_max_type_id = max_type_id_;
    // Create copy of addend_node
      T added_node =
          *(addend_node);
      added_node.SetSelfType(updated_self_type_id);
      // The merge status returned is the merge status of the referenced type.
      merge_status = MergeReferencingTypeInternal(addend, &added_node,
                                                  local_to_global_type_id_map);
      if (merge_status.was_newly_added_) {
        // Emplace to map (type-referenced -> Referencing type)
        AddToMapAndTypeGraph(std::move(added_node), parent_map,
                             &type_graph_);
        return MergeStatus(true, updated_self_type_id);
      }
      // The type that the added_node references was not newly added to the parent
      // graph. However, we still might need to add the added_node to the parent
      // graph, since for the particular 'Kind' of the added_node, it may not be
      // present in the parent graph. This will be determined by looking at the
      // appropriate 'type-referenced' -> TypeElement map in the parent for the
      // type-id returned by the MergeStatus. If the map doesn't have an entry for
      // the type-id returned by the MergeStatus, the added_type is not present in
      // the parent graph and needs to be 'newly' added. We also need to modify the
      // global type id in the local_to_global_type_id map. The added_node should
      // already have it's self_type and referenced_type fields fixed up.
      // We maintain a rollback id to have contiguous type ids.
      max_type_id_ = old_max_type_id;
      // Try finding the referenced_type is referred to by any referencing type
      // of the same kind in the parent graph. It is safe to call this on the
      // added_node, since the referenced_type in the added_node would have been
      // modified by the MergeReferencingTypeInternal call.
      auto it = parent_map->find(GetReferencedTypeMapKey(added_node));
      if (it == parent_map->end()) {
        // There was no counterpart found for the added_node's type Kind referencing
        // the referenced type, so we added it to the parent and also updated the
        // local_to_global_type_id_map's global_id value.
        AddToMapAndTypeGraph(std::move(added_node), parent_map,
                             &type_graph_);

        merge_status = MergeStatus(true, updated_self_type_id);
        return merge_status;
      }
      // Update local_to_global_type_id map's MergeStatus.was_newly_added  value for
      // this key with false since this was node was not newly added.
      // We never remove anything from the local_to_global_type_id_map, what's
      // the point ? Since you store the decision of whether the type was newly
      // added or not. It's global type id is the type-id of the element found
      // in the parent map which refers to the added_node's modified
      // referenced_type.
      merge_status = MergeStatus(false, it->second.GetSelfType());
      (*local_to_global_type_id_map)[addend_node->GetSelfType()] =
          merge_status;
      return merge_status;
}

MergeStatus TextFormatToIRReader::MergeReferencingType(
    const TextFormatToIRReader &addend, const TypeIR *addend_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map,
    const std::string &updated_self_type_id) {
  switch (addend_node->GetKind()) {
    case PointerTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const PointerTypeIR *>(addend_node),
          local_to_global_type_id_map, &pointer_types_, updated_self_type_id);
    case QualifiedTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const QualifiedTypeIR *>(addend_node),
          local_to_global_type_id_map, &qualified_types_, updated_self_type_id);
    case ArrayTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const ArrayTypeIR *>(addend_node),
          local_to_global_type_id_map, &array_types_, updated_self_type_id);
    case LvalueReferenceTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const LvalueReferenceTypeIR *>(addend_node),
          local_to_global_type_id_map, &lvalue_reference_types_,
          updated_self_type_id);
    case RvalueReferenceTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const RvalueReferenceTypeIR *>(addend_node),
          local_to_global_type_id_map, &rvalue_reference_types_,
          updated_self_type_id);
    default:
      // Only referencing types
      assert(0);
  }
}

// This method creates a new node for the addend node in the graph if MergeType
// on the reference returned a MergeStatus with was_newly_added_ = true.
MergeStatus TextFormatToIRReader::MergeGenericReferringType(
    const TextFormatToIRReader &addend, const TypeIR *addend_node,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // First add the type 'pro-actively'. We need to do this since we'll need to
  // fill in 'referenced-type' fields in all this type's descendants and
  // descendants which are compound types (records), can refer to this type.
  std::string added_type_id = AllocateNewTypeId();
  // Add the added record type to the local_to_global_type_id_map
  local_to_global_type_id_map->emplace(addend_node->GetSelfType(),
                                       MergeStatus(true, added_type_id));
  return MergeReferencingType(addend, addend_node, local_to_global_type_id_map,
                              added_type_id);
}

MergeStatus TextFormatToIRReader::MergeTypeInternal(
    const TypeIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  switch(addend_node->GetKind()) {
    case BuiltinTypeKind:
      return IsBuiltinTypeNodePresent(
          static_cast<const BuiltinTypeIR *>(addend_node), addend,
          local_to_global_type_id_map);
      break;
    case RecordTypeKind:
      return MergeRecordAndDependencies(
          static_cast<const RecordTypeIR *>(addend_node),
          addend, local_to_global_type_id_map);
    case EnumTypeKind:
      return MergeEnumType(static_cast<const EnumTypeIR *>(
          addend_node), addend, local_to_global_type_id_map);
    case FunctionTypeKind:
      return MergeFunctionType(static_cast<const FunctionTypeIR *>(
          addend_node), addend, local_to_global_type_id_map);
    default:
      return MergeGenericReferringType(addend, addend_node,
                                       local_to_global_type_id_map);
  }
  assert(0);
}

MergeStatus TextFormatToIRReader::MergeType(
    const TypeIR *addend_node,
    const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
    // Check if the addend type is already in the parent graph. Since we're
    // going to traverse all the dependencies add whichever ones are not in the
    // parent graph. This does not add the node itself though.
    auto type_it =
        local_to_global_type_id_map->find(addend_node->GetSelfType());
    if (type_it != local_to_global_type_id_map->end()) {
      return type_it->second;
    }

    MergeStatus merge_status = IsTypeNodePresent(addend_node, addend,
                                                 local_to_global_type_id_map);
    if (!merge_status.was_newly_added_) {
      return merge_status;
    }
    merge_status = MergeTypeInternal(addend_node, addend,
                                     local_to_global_type_id_map);
    return merge_status;
}

void TextFormatToIRReader::MergeCFunctionLikeDeps(
    const TextFormatToIRReader &addend, CFunctionLikeIR *cfunction_like_ir,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
 // Merge the return type first.
  auto ret_type_it =
      addend.type_graph_.find(cfunction_like_ir->GetReturnType());
  if (ret_type_it == addend.type_graph_.end()) {
    // Hidden types aren't officially added to the parent since there is
    // nothing actually backing it. We assign a type-id.
    cfunction_like_ir->SetReturnType(AllocateNewTypeId());
  } else {
    MergeStatus ret_merge_status = MergeType(ret_type_it->second, addend,
                                             local_to_global_type_id_map);
    cfunction_like_ir->SetReturnType(ret_merge_status.type_id_);
  }
  // Merge and fix parameters.
  for (auto &param : cfunction_like_ir->GetParameters()) {
    MergeReferencingTypeInternal(addend, &param, local_to_global_type_id_map);
  }
}

void TextFormatToIRReader::MergeFunctionDeps(
    FunctionIR *added_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  MergeCFunctionLikeDeps(addend, added_node, local_to_global_type_id_map);
  // Merge and fix template parameters
  for (auto &template_element : added_node->GetTemplateElements()) {
    MergeReferencingTypeInternal(addend, &template_element,
                                 local_to_global_type_id_map);
  }
}

template <typename T>
static bool IsLinkableMessagePresent(const LinkableMessageIR *lm,
                                     const AbiElementMap<T> &message_map) {
  return (message_map.find(lm->GetLinkerSetKey()) != message_map.end());
}

void TextFormatToIRReader::MergeFunction(
    const FunctionIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string &function_linkage_name = addend_node->GetLinkerSetKey();
  if (IsLinkableMessagePresent(addend_node, functions_)) {
    // The functions and all of its dependencies have already been added.
    // No two globally visible functions can have the same symbol name.
    return;
  }
  FunctionIR function_ir = *addend_node;
  MergeFunctionDeps(&function_ir, addend, local_to_global_type_id_map);
  // Add it to the parent's function map.
  functions_.emplace(function_linkage_name, std::move(function_ir));
}

std::string TextFormatToIRReader::AllocateNewTypeId() {
  return "type-" + std::to_string(++max_type_id_);
}

void TextFormatToIRReader::MergeGlobalVariable(
    const GlobalVarIR *addend_node, const TextFormatToIRReader &addend,
    AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string &global_variable_linkage_name =
      addend_node->GetLinkerSetKey();
  if (IsLinkableMessagePresent(addend_node, global_variables_)) {
    // The global variable and all of its dependencies have already been added.
    return;
  }
  GlobalVarIR global_variable_ir = *addend_node;
  MergeReferencingTypeInternal(addend, &global_variable_ir,
                               local_to_global_type_id_map);
  global_variables_.emplace(global_variable_linkage_name,
                            std::move(global_variable_ir));
}

void TextFormatToIRReader::MergeGraphs(const TextFormatToIRReader &addend) {
  // Iterate through nodes of addend reader and merge them.
  // Keep a merged types cache since if a type is merged, so will all of its
  // dependencies which weren't already merged.
  AbiElementMap<MergeStatus> merged_types_cache;

  for (auto &&type_ir : addend.type_graph_) {
    MergeType(type_ir.second, addend, &merged_types_cache);
  }

  for (auto &&function_ir : addend.functions_) {
    MergeFunction(&function_ir.second, addend, &merged_types_cache);
  }

  for (auto &&global_var_ir : addend.global_variables_) {
    MergeGlobalVariable(&global_var_ir.second, addend, &merged_types_cache);
  }
}
} // namespace abi_util


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

#include "linker/module_merger.h"
#include "repr/abi_diff_helpers.h"
#include "repr/ir_representation_internal.h"

#include <cassert>

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace linker {


MergeStatus ModuleMerger::MergeBuiltinType(
    const repr::BuiltinTypeIR *builtin_type, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  std::string linker_set_key = builtin_type->GetLinkerSetKey();
  auto builtin_it = module_->builtin_types_.find(linker_set_key);
  if (builtin_it != module_->builtin_types_.end()) {
    return MergeStatus(false, builtin_it->second.GetSelfType());
  }

  // Add this builtin type to the parent graph's builtin_types_ map.
  const std::string &type_id = builtin_type->GetSelfType();
  auto p = module_->builtin_types_.emplace(linker_set_key, *builtin_type);
  module_->type_graph_.emplace(type_id, &p.first->second);

  MergeStatus merge_status(true, type_id);
  local_to_global_type_id_map->emplace(type_id, merge_status);
  return merge_status;
}


MergeStatus ModuleMerger::LookupUserDefinedType(
    const repr::TypeIR *ud_type, const repr::ModuleIR &addend,
    const std::string &ud_type_unique_id_and_source,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map_) {
  auto it = module_->odr_list_map_.find(ud_type_unique_id_and_source);
  if (it == module_->odr_list_map_.end()) {
    // Calling this an ODR violation even though it means no UD with the same
    // name + source combination was seen in the parent graph. The type-id
    // passed does not matter since was_newly_added_ is true, the type will get
    // allocated a new type id.
    return MergeStatus(true, "");
  }

  // Initialize type comparator (which will compare the referenced types
  // recursively).
  std::set<std::string> type_cache;
  repr::DiffPolicyOptions diff_policy_options(false);
  repr::AbiDiffHelper diff_helper(module_->type_graph_, addend.type_graph_,
                                  diff_policy_options, &type_cache, nullptr);

  // Compare each user-defined type with the latest input user-defined type.
  // If there is a match, re-use the existing user-defined type.
  for (auto &definition : it->second) {
    const repr::TypeIR *contender_ud = definition.type_ir_;
    repr::DiffStatus result = diff_helper.CompareAndDumpTypeDiff(
        contender_ud->GetSelfType(), ud_type->GetSelfType());
    if (result == repr::DiffStatus::no_diff) {
      local_to_global_type_id_map_->emplace(
          ud_type->GetSelfType(),
          MergeStatus(false, contender_ud->GetSelfType()));
      return MergeStatus(false, contender_ud->GetSelfType());
    }
  }

#ifdef DEBUG
  llvm::errs() << "ODR violation detected for: " << ud_type->GetName() << "\n";
#endif
  return MergeStatus(true, it->second.begin()->type_ir_->GetSelfType());
}


MergeStatus ModuleMerger::LookupType(
    const repr::TypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  std::string unique_type_id;
  switch (addend_node->GetKind()) {
    case repr::RecordTypeKind:
      unique_type_id = repr::GetODRListMapKey(
          static_cast<const repr::RecordTypeIR *>(addend_node));
      break;
    case repr::EnumTypeKind:
      unique_type_id = repr::GetODRListMapKey(
          static_cast<const repr::EnumTypeIR *>(addend_node));
      break;
    case repr::FunctionTypeKind:
      unique_type_id = repr::GetODRListMapKey(
          static_cast<const repr::FunctionTypeIR *>(addend_node));
      break;
    default:
      // Other kinds (e.g. PointerTypeKind, QualifiedTypeKind, ArrayTypeKind,
      // LvalueReferenceTypeKind, RvalueReferenceTypeKind, or BuiltinTypeKind)
      // should be proactively added by returning MergeStatus with
      // was_newly_added_ = true.
      return MergeStatus(true, "type-hidden");
  }

  return LookupUserDefinedType(
      addend_node, addend, unique_type_id, local_to_global_type_id_map);
}


// This method merges the type referenced by 'references_type' into the parent
// graph. It also corrects the referenced_type field in the references_type
// object passed and returns the merge status of the *referenced type*.
MergeStatus ModuleMerger::MergeReferencingTypeInternal(
    const repr::ModuleIR &addend, repr::ReferencesOtherType *references_type,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // First look in the local_to_global_type_id_map for the referenced type's
  // id.
  const std::string &referenced_type_id = references_type->GetReferencedType();
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
        MergeType(local_type_it->second, addend, local_to_global_type_id_map);
    const std::string &global_type_id = merge_status.type_id_;
    references_type->SetReferencedType(global_type_id);
    return merge_status;
  }

  // If the referenced type was hidden, create the name reference type in the
  // parent module and keep the referenced type_id as-is.
  return MergeStatus(true, referenced_type_id);
}


void ModuleMerger::MergeRecordFields(
    const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for (auto &field : added_node->GetFields()) {
    MergeReferencingTypeInternal(addend, &field, local_to_global_type_id_map);
  }
}


void ModuleMerger::MergeRecordCXXBases(
    const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for (auto &base : added_node->GetBases()) {
    MergeReferencingTypeInternal(addend, &base, local_to_global_type_id_map);
  }
}


void ModuleMerger::MergeRecordTemplateElements(
    const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  for (auto &template_element : added_node->GetTemplateElements()) {
    MergeReferencingTypeInternal(
        addend, &template_element, local_to_global_type_id_map);
  }
}


void ModuleMerger::MergeRecordDependencies(
    const repr::ModuleIR &addend, repr::RecordTypeIR *added_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // First call MergeType on all its fields.
  MergeRecordFields(addend, added_node, local_to_global_type_id_map);

  // Call MergeType on CXXBases of the record.
  MergeRecordCXXBases(addend, added_node, local_to_global_type_id_map);

  MergeRecordTemplateElements(addend, added_node, local_to_global_type_id_map);
}


template <typename T>
std::pair<MergeStatus, typename repr::AbiElementMap<T>::iterator>
ModuleMerger::UpdateUDTypeAccounting(
    const T *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map,
    repr::AbiElementMap<T> *specific_type_map) {
  const std::string addend_compilation_unit_path =
      addend.GetCompilationUnitPath(addend_node);
  assert(addend_compilation_unit_path != "");
  std::string added_type_id = addend_node->GetSelfType();
  auto type_id_it = module_->type_graph_.find(added_type_id);
  if (type_id_it != module_->type_graph_.end()) {
    added_type_id = added_type_id + "#ODR:" + addend_compilation_unit_path;
  }

  // Add the ud-type with type-id to the type_graph_, since if there are generic
  // reference types which refer to the record being added, they'll need to find
  // it's id in the map.
  // Add ud-type to the parent graph.
  T added_type_ir = *addend_node;
  added_type_ir.SetSelfType(added_type_id);
  added_type_ir.SetReferencedType(added_type_id);
  auto it = AddToMapAndTypeGraph(std::move(added_type_ir), specific_type_map,
                                 &module_->type_graph_);
  // Add to facilitate ODR checking.
  const std::string &key = GetODRListMapKey(&(it->second));
  MergeStatus type_merge_status = MergeStatus(true, added_type_id);
  module_->AddToODRListMap(key, &(it->second), addend_compilation_unit_path);
  local_to_global_type_id_map->emplace(addend_node->GetSelfType(),
                                       type_merge_status);
  return {type_merge_status, it};
}


// This method is necessarily going to have a was_newly_merged_ = true in its
// MergeStatus return. So it necessarily merges a new RecordType.
MergeStatus ModuleMerger::MergeRecordAndDependencies(
    const repr::RecordTypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto p = UpdateUDTypeAccounting(
      addend_node, addend, local_to_global_type_id_map,
      &module_->record_types_);
  MergeRecordDependencies(addend, &p.second->second,
                          local_to_global_type_id_map);
  return p.first;
}


void ModuleMerger::MergeEnumDependencies(
    const repr::ModuleIR &addend, repr::EnumTypeIR *added_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string underlying_type_id = added_node->GetUnderlyingType();
  // Get the underlying type, it nessarily has to be present in the addend's
  // type graph since builtin types can't be hidden. Call MergeType on it and
  // change the underlying type to that.
  auto it = addend.type_graph_.find(underlying_type_id);
  if (it == addend.type_graph_.end()) {
    llvm::errs() << "Enum underlying types should not be hidden\n";
    ::exit(1);
  }
  MergeStatus merge_status = MergeType(
      it->second, addend, local_to_global_type_id_map);
  added_node->SetUnderlyingType(merge_status.type_id_);
}


// This method is necessarily going to have a was_newly_merged_ = true in its
// MergeStatus return. So it necessarily merges a new EnumType.
MergeStatus ModuleMerger::MergeEnumType(
    const repr::EnumTypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto p = UpdateUDTypeAccounting(
      addend_node, addend, local_to_global_type_id_map, &module_->enum_types_);
  MergeEnumDependencies(addend, &p.second->second, local_to_global_type_id_map);
  return p.first;
}


MergeStatus ModuleMerger::MergeFunctionType(
    const repr::FunctionTypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  auto p = UpdateUDTypeAccounting(
      addend_node, addend, local_to_global_type_id_map,
      &module_->function_types_);
  MergeCFunctionLikeDeps(addend, &p.second->second,
                         local_to_global_type_id_map);
  return p.first;
}


template <typename T>
MergeStatus ModuleMerger::MergeReferencingTypeInternalAndUpdateParent(
    const repr::ModuleIR &addend, const T *addend_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map,
    repr::AbiElementMap<T> *parent_map,
    const std::string &updated_self_type_id) {
  MergeStatus merge_status;

  // Create copy of addend_node
  T added_node = *addend_node;
  added_node.SetSelfType(updated_self_type_id);

  // The merge status returned is the merge status of the referenced type.
  merge_status = MergeReferencingTypeInternal(addend, &added_node,
                                              local_to_global_type_id_map);
  if (merge_status.was_newly_added_) {
    // Emplace to map (type-referenced -> Referencing type)
    AddToMapAndTypeGraph(std::move(added_node), parent_map,
                         &module_->type_graph_);
    return MergeStatus(true, updated_self_type_id);
  }

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
                         &module_->type_graph_);

    merge_status = MergeStatus(true, updated_self_type_id);
    return merge_status;
  }

  // Update local_to_global_type_id map's MergeStatus.was_newly_added value for
  // this key with false since this was node was not newly added.
  // We never remove anything from the local_to_global_type_id_map, what's
  // the point ? Since you store the decision of whether the type was newly
  // added or not. It's global type id is the type-id of the element found
  // in the parent map which refers to the added_node's modified
  // referenced_type.
  merge_status = MergeStatus(false, it->second.GetSelfType());
  (*local_to_global_type_id_map)[addend_node->GetSelfType()] = merge_status;

  return merge_status;
}


static bool IsReferencingType(repr::LinkableMessageKind kind) {
  switch (kind) {
    case repr::PointerTypeKind:
    case repr::QualifiedTypeKind:
    case repr::ArrayTypeKind:
    case repr::LvalueReferenceTypeKind:
    case repr::RvalueReferenceTypeKind:
      return true;
    case repr::RecordTypeKind:
    case repr::EnumTypeKind:
    case repr::BuiltinTypeKind:
    case repr::FunctionTypeKind:
    case repr::FunctionKind:
    case repr::GlobalVarKind:
      return false;
  }
}

// Trace the referenced type until reaching a RecordTypeIR, EnumTypeIR,
// FunctionTypeIR, or BuiltinTypeIR. Return nullptr if the referenced type is
// undefined or built-in.
static const repr::TypeIR *DereferenceType(const repr::ModuleIR &module,
                                           const repr::TypeIR *type_ir) {
  auto &type_graph = module.GetTypeGraph();
  while (IsReferencingType(type_ir->GetKind())) {
    auto it = type_graph.find(type_ir->GetReferencedType());
    // The referenced type is undefined in the module.
    if (it == type_graph.end()) {
      return nullptr;
    }
    type_ir = it->second;
  }
  return type_ir;
}


// This method creates a new node for the addend node in the graph if MergeType
// on the reference returned a MergeStatus with was_newly_added_ = true.
MergeStatus ModuleMerger::MergeReferencingType(
    const repr::ModuleIR &addend, const repr::TypeIR *addend_node,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // First add the type 'pro-actively'. We need to do this since we'll need to
  // fill in 'referenced-type' fields in all this type's descendants and
  // descendants which are compound types (records), can refer to this type.
  std::string added_type_id = addend_node->GetSelfType();
  auto type_id_it = module_->type_graph_.find(added_type_id);
  if (type_id_it != module_->type_graph_.end()) {
    const repr::TypeIR *final_referenced_type =
        DereferenceType(addend, addend_node);
    if (final_referenced_type != nullptr) {
      std::string compilation_unit_path =
          addend.GetCompilationUnitPath(final_referenced_type);
      // The path is empty for built-in types.
      if (compilation_unit_path != "") {
        added_type_id = added_type_id + "#ODR:" + compilation_unit_path;
      }
    }
  }

  // Add the added record type to the local_to_global_type_id_map.
  local_to_global_type_id_map->emplace(addend_node->GetSelfType(),
                                       MergeStatus(true, added_type_id));

  // Merge the type.
  switch (addend_node->GetKind()) {
    case repr::PointerTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const repr::PointerTypeIR *>(addend_node),
          local_to_global_type_id_map, &module_->pointer_types_,
          added_type_id);
    case repr::QualifiedTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const repr::QualifiedTypeIR *>(addend_node),
          local_to_global_type_id_map, &module_->qualified_types_,
          added_type_id);
    case repr::ArrayTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const repr::ArrayTypeIR *>(addend_node),
          local_to_global_type_id_map, &module_->array_types_,
          added_type_id);
    case repr::LvalueReferenceTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const repr::LvalueReferenceTypeIR *>(addend_node),
          local_to_global_type_id_map, &module_->lvalue_reference_types_,
          added_type_id);
    case repr::RvalueReferenceTypeKind:
      return MergeReferencingTypeInternalAndUpdateParent(
          addend, static_cast<const repr::RvalueReferenceTypeIR *>(addend_node),
          local_to_global_type_id_map, &module_->rvalue_reference_types_,
          added_type_id);
    default:
      // Only referencing types
      assert(0);
  }
}


MergeStatus ModuleMerger::MergeTypeInternal(
    const repr::TypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  switch (addend_node->GetKind()) {
    case repr::BuiltinTypeKind:
      return MergeBuiltinType(
          static_cast<const repr::BuiltinTypeIR *>(addend_node), addend,
          local_to_global_type_id_map);
    case repr::RecordTypeKind:
      return MergeRecordAndDependencies(
          static_cast<const repr::RecordTypeIR *>(addend_node), addend,
          local_to_global_type_id_map);
    case repr::EnumTypeKind:
      return MergeEnumType(static_cast<const repr::EnumTypeIR *>(addend_node),
                           addend, local_to_global_type_id_map);
    case repr::FunctionTypeKind:
      return MergeFunctionType(
          static_cast<const repr::FunctionTypeIR *>(addend_node), addend,
          local_to_global_type_id_map);
    default:
      return MergeReferencingType(addend, addend_node,
                                  local_to_global_type_id_map);
  }
  assert(0);
}


MergeStatus ModuleMerger::MergeType(
    const repr::TypeIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // Check if the addend type is already in the parent graph. Since we're
  // going to traverse all the dependencies add whichever ones are not in the
  // parent graph. This does not add the node itself though.
  auto type_it = local_to_global_type_id_map->find(addend_node->GetSelfType());
  if (type_it != local_to_global_type_id_map->end()) {
    return type_it->second;
  }

  MergeStatus merge_status = LookupType(
      addend_node, addend, local_to_global_type_id_map);
  if (!merge_status.was_newly_added_) {
    return merge_status;
  }
  merge_status = MergeTypeInternal(
      addend_node, addend, local_to_global_type_id_map);
  return merge_status;
}


void ModuleMerger::MergeCFunctionLikeDeps(
    const repr::ModuleIR &addend, repr::CFunctionLikeIR *cfunction_like_ir,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  // Merge the return type.
  auto ret_type_it =
      addend.type_graph_.find(cfunction_like_ir->GetReturnType());
  if (ret_type_it != addend.type_graph_.end()) {
    // Merge the type if we can find another type in the parent module.
    MergeStatus ret_merge_status = MergeType(ret_type_it->second, addend,
                                             local_to_global_type_id_map);
    cfunction_like_ir->SetReturnType(ret_merge_status.type_id_);
  }

  // Merge the argument types.
  for (auto &param : cfunction_like_ir->GetParameters()) {
    MergeReferencingTypeInternal(addend, &param, local_to_global_type_id_map);
  }
}


void ModuleMerger::MergeFunctionDeps(
    repr::FunctionIR *added_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  MergeCFunctionLikeDeps(addend, added_node, local_to_global_type_id_map);

  // Merge the template arguments.
  for (auto &template_element : added_node->GetTemplateElements()) {
    MergeReferencingTypeInternal(addend, &template_element,
                                 local_to_global_type_id_map);
  }
}


template <typename T>
static bool
IsLinkableMessagePresent(const repr::LinkableMessageIR *lm,
                         const repr::AbiElementMap<T> &message_map) {
  return (message_map.find(lm->GetLinkerSetKey()) != message_map.end());
}


void ModuleMerger::MergeFunction(
    const repr::FunctionIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string &function_linkage_name = addend_node->GetLinkerSetKey();
  if (IsLinkableMessagePresent(addend_node, module_->functions_)) {
    // The functions and all of its dependencies have already been added.
    // No two globally visible functions can have the same symbol name.
    return;
  }
  repr::FunctionIR function_ir = *addend_node;
  MergeFunctionDeps(&function_ir, addend, local_to_global_type_id_map);
  // Add it to the parent's function map.
  module_->functions_.emplace(function_linkage_name, std::move(function_ir));
}


void ModuleMerger::MergeGlobalVariable(
    const repr::GlobalVarIR *addend_node, const repr::ModuleIR &addend,
    repr::AbiElementMap<MergeStatus> *local_to_global_type_id_map) {
  const std::string &global_variable_linkage_name =
      addend_node->GetLinkerSetKey();
  if (IsLinkableMessagePresent(addend_node, module_->global_variables_)) {
    // The global variable and all of its dependencies have already been added.
    return;
  }
  repr::GlobalVarIR global_variable_ir = *addend_node;
  MergeReferencingTypeInternal(addend, &global_variable_ir,
                               local_to_global_type_id_map);
  module_->global_variables_.emplace(
      global_variable_linkage_name, std::move(global_variable_ir));
}


void ModuleMerger::MergeGraphs(const repr::ModuleIR &addend) {
  // Iterate through nodes of addend reader and merge them.
  // Keep a merged types cache since if a type is merged, so will all of its
  // dependencies which weren't already merged.
  repr::AbiElementMap<MergeStatus> merged_types_cache;

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


}  // namespace linker
}  // namespace header_checker


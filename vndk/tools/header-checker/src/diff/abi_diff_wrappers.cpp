// Copyright (C) 2016 The Android Open Source Project
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

#include "diff/abi_diff_wrappers.h"

#include "utils/header_abi_util.h"

#include <llvm/Support/raw_ostream.h>


namespace header_checker {
namespace diff {


using repr::AbiElementMap;
using repr::DiffStatus;
using repr::Unwind;


template <>
bool DiffWrapper<repr::RecordTypeIR>::DumpDiff(
    repr::DiffMessageIR::DiffKind diff_kind) {
  std::deque<std::string> type_queue;
  if (oldp_->GetLinkerSetKey() != newp_->GetLinkerSetKey()) {
    llvm::errs() << "Comparing two different unreferenced records\n";
    return false;
  }
  if (!type_cache_->insert(
          oldp_->GetSelfType() + newp_->GetSelfType()).second) {
    return true;
  }
  CompareRecordTypes(oldp_, newp_, &type_queue, diff_kind);
  return true;
}

template <>
bool DiffWrapper<repr::EnumTypeIR>::DumpDiff(
    repr::DiffMessageIR::DiffKind diff_kind) {
  std::deque<std::string> type_queue;
  if (oldp_->GetLinkerSetKey() != newp_->GetLinkerSetKey()) {
    llvm::errs() << "Comparing two different unreferenced enums\n";
    return false;
  }
  if (!type_cache_->insert(
      oldp_->GetSelfType() + newp_->GetSelfType()).second) {
    return true;
  }
  CompareEnumTypes(oldp_, newp_, &type_queue, diff_kind);
  return true;
}

template <>
bool DiffWrapper<repr::GlobalVarIR>::DumpDiff(
    repr::DiffMessageIR::DiffKind diff_kind) {
  std::deque<std::string> type_queue;
  type_queue.push_back(oldp_->GetName());
  DiffStatus type_diff = CompareAndDumpTypeDiff(oldp_->GetReferencedType(),
                                                newp_->GetReferencedType(),
                                                &type_queue, diff_kind);
  DiffStatus access_diff = (oldp_->GetAccess() == newp_->GetAccess()) ?
      DiffStatus::no_diff : DiffStatus::direct_diff;
  if ((type_diff | access_diff) & DiffStatus::direct_diff) {
    repr::GlobalVarIR old_global_var = *oldp_;
    repr::GlobalVarIR new_global_var = *newp_;
    ReplaceTypeIdsWithTypeNames(old_types_, &old_global_var);
    ReplaceTypeIdsWithTypeNames(new_types_, &new_global_var);
    repr::GlobalVarDiffIR global_var_diff_ir(&old_global_var,
                                                 &new_global_var);
    global_var_diff_ir.SetName(oldp_->GetName());
    return ir_diff_dumper_->AddDiffMessageIR(&global_var_diff_ir,
                                             Unwind(&type_queue), diff_kind);
  }
  return true;
}

template <>
bool DiffWrapper<repr::FunctionIR>::DumpDiff(
    repr::DiffMessageIR::DiffKind diff_kind) {
  std::deque<std::string> type_queue;
  type_queue.push_back(oldp_->GetName());

  DiffStatus param_diffs = CompareFunctionParameters(
      oldp_->GetParameters(), newp_->GetParameters(), &type_queue, diff_kind);

  DiffStatus return_type_diff = CompareAndDumpTypeDiff(
      oldp_->GetReturnType(), newp_->GetReturnType(), &type_queue, diff_kind);

  CompareTemplateInfo(oldp_->GetTemplateElements(),
                      newp_->GetTemplateElements(),
                      &type_queue, diff_kind);

  if ((param_diffs == DiffStatus::direct_diff ||
       return_type_diff == DiffStatus::direct_diff) ||
      (oldp_->GetAccess() != newp_->GetAccess())) {
    repr::FunctionIR old_function = *oldp_;
    repr::FunctionIR new_function = *newp_;
    ReplaceTypeIdsWithTypeNames(old_types_, &old_function);
    ReplaceTypeIdsWithTypeNames(new_types_, &new_function);
    repr::FunctionDiffIR function_diff_ir(&old_function, &new_function);
    function_diff_ir.SetName(oldp_->GetName());
    return ir_diff_dumper_->AddDiffMessageIR(&function_diff_ir,
                                             Unwind(&type_queue), diff_kind);
  }
  return true;
}


}  // namespace diff
}  // namespace header_checker

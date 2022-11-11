// Copyright (C) 2022 The Android Open Source Project
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

#include "repr/ir_diff_representation.h"

namespace header_checker {
namespace repr {

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
bool VTableLayoutDiffIR::IsExtended() const {
  const std::vector<VTableComponentIR> &old_components =
      old_layout_.GetVTableComponents();
  const std::vector<VTableComponentIR> &new_components =
      new_layout_.GetVTableComponents();
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

bool RecordTypeDiffIR::IsExtended() const {
  bool is_extended = false;
  if (type_diff_ != nullptr) {
    auto sizes = type_diff_->GetSizes();
    if (sizes.first < sizes.second) {
      is_extended = true;
    }
    if (sizes.first > sizes.second) {
      return false;
    }
    auto alignments = type_diff_->GetAlignments();
    if (alignments.first != alignments.second) {
      return false;
    }
  }
  if (access_diff_ != nullptr) {
    if (IsAccessDowngraded(access_diff_->GetOldAccess(),
                           access_diff_->GetNewAccess())) {
      return false;
    }
    is_extended = true;
  }
  if (base_specifier_diffs_ != nullptr) {
    return false;
  }
  if (vtable_diffs_ != nullptr) {
    if (!vtable_diffs_->IsExtended()) {
      return false;
    }
    is_extended = true;
  }
  // This function skips comparing the access specifiers of field_diffs_
  // because AbiDiffHelper::CompareCommonRecordFields does not report
  // upgraded access specifiers as ABI difference.
  if (field_diffs_.size() != 0 || fields_removed_.size() != 0) {
    return false;
  }
  if (fields_added_.size() != 0) {
    is_extended = true;
  }
  return is_extended;
}

}  // namespace repr
}  // namespace header_checker

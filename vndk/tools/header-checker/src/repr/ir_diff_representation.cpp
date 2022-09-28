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
    // TODO(b/248418092): Compare vtables.
    return false;
  }
  // This function skips comparing the access specifiers of field_diffs_
  // because AbiDiffHelper::CompareCommonRecordFields does not report
  // upgraded access specifiers as ABI difference.
  if (field_diffs_.size() != 0 || fields_removed_.size() != 0) {
    return false;
  }
  if (fields_added_.size() != 0) {
    if (type_diff_ != nullptr) {
      const uint64_t old_size = type_diff_->GetSizes().first;
      for (const RecordFieldIR *field_added : fields_added_) {
        // The offset is in bits; the size is in bytes.
        if (field_added->GetOffset() < old_size * 8) {
          return false;
        }
      }
    }
    is_extended = true;
  }
  return is_extended;
}

}  // namespace repr
}  // namespace header_checker

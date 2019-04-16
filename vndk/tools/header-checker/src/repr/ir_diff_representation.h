// Copyright (C) 2019 The Android Open Source Project
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

#ifndef HEADER_CHECKER_REPR_IR_DIFF_REPRESENTATION_H_
#define HEADER_CHECKER_REPR_IR_DIFF_REPRESENTATION_H_

#include "repr/ir_representation.h"

#include <cstdint>
#include <memory>
#include <string>
#include <utility>
#include <vector>


namespace header_checker {
namespace repr {


class DiffMessageIR {
 public:
  enum DiffKind {
    Extension,  // Applicable for enums.
    Added,
    Removed,
    Referenced,
    Unreferenced
  };

 public:
  virtual ~DiffMessageIR() {}

  virtual LinkableMessageKind Kind() const = 0;

  void SetName(const std::string &name) {
    name_ = name;
  }

  const std::string &GetName() const {
    return name_;
  }

 protected:
  std::string name_;
};

class AccessSpecifierDiffIR {
 public:
  AccessSpecifierDiffIR(AccessSpecifierIR old_access,
                        AccessSpecifierIR new_access)
      : old_access_(old_access), new_access_(new_access) {}

 protected:
  AccessSpecifierIR old_access_;
  AccessSpecifierIR new_access_;
};

class TypeDiffIR {
 public:
  TypeDiffIR(std::pair<uint64_t, uint64_t> &&sizes,
             std::pair<uint32_t, uint32_t> &&alignment)
      : sizes_(std::move(sizes)), alignments_(std::move(alignment)) {}

  const std::pair<uint64_t, uint64_t> &GetSizes() const {
    return sizes_;
  }

  const std::pair<uint32_t, uint32_t> &GetAlignments() const {
    return alignments_;
  }

 protected:
  std::pair<uint64_t, uint64_t> sizes_;
  std::pair<uint32_t, uint32_t> alignments_;
};

class VTableLayoutDiffIR {
 public:
  VTableLayoutDiffIR(const VTableLayoutIR &old_layout,
                     const VTableLayoutIR &new_layout)
      : old_layout_(old_layout), new_layout_(new_layout) {}

  const VTableLayoutIR &GetOldVTable() const {
    return old_layout_;
  }

  const VTableLayoutIR &GetNewVTable() const {
    return new_layout_;
  }

 protected:
  const VTableLayoutIR &old_layout_;
  const VTableLayoutIR &new_layout_;
};

class RecordFieldDiffIR {
 public:
  RecordFieldDiffIR(const RecordFieldIR *old_field,
                    const RecordFieldIR *new_field)
      : old_field_(old_field), new_field_(new_field) {}

  const RecordFieldIR *GetOldField() const {
    return old_field_;
  }

  const RecordFieldIR *GetNewField() const {
    return new_field_;
  }

  const RecordFieldIR *old_field_;
  const RecordFieldIR *new_field_;
};

class CXXBaseSpecifierDiffIR {
 public:
  CXXBaseSpecifierDiffIR(
      const std::vector<CXXBaseSpecifierIR> &old_base_specifiers,
      const std::vector<CXXBaseSpecifierIR> &new_base_specifiers)
      : old_base_specifiers_(old_base_specifiers),
        new_base_specifiers_(new_base_specifiers) {}

  const std::vector<CXXBaseSpecifierIR> &GetOldBases() const {
    return old_base_specifiers_;
  }

  const std::vector<CXXBaseSpecifierIR> &GetNewBases() const {
    return new_base_specifiers_;
  }

 protected:
  const std::vector<CXXBaseSpecifierIR> &old_base_specifiers_;
  const std::vector<CXXBaseSpecifierIR> &new_base_specifiers_;
};

class RecordTypeDiffIR : public DiffMessageIR {
 public:
  LinkableMessageKind Kind() const override {
    return LinkableMessageKind::RecordTypeKind;
  }

  void SetFieldDiffs(std::vector<RecordFieldDiffIR> &&field_diffs) {
    field_diffs_ = std::move(field_diffs);
  }

  const std::vector<RecordFieldDiffIR> &GetFieldDiffs() const {
    return field_diffs_;
  }

  void SetFieldsRemoved(std::vector<const RecordFieldIR *> &&fields_removed) {
    fields_removed_ = std::move(fields_removed);
  }

  void SetFieldsAdded(std::vector<const RecordFieldIR *> &&fields_added) {
    fields_added_ = std::move(fields_added);
  }

  const std::vector<const RecordFieldIR *> &GetFieldsRemoved() const {
    return fields_removed_;
  }

  const std::vector<const RecordFieldIR *> &GetFieldsAdded() const {
    return fields_added_;
  }

  void SetVTableLayoutDiff(std::unique_ptr<VTableLayoutDiffIR> &&vtable_diffs) {
    vtable_diffs_ = std::move(vtable_diffs);
  }

  void SetTypeDiff(std::unique_ptr<TypeDiffIR> &&type_diff) {
    type_diff_ = std::move(type_diff);
  }

  void SetAccessDiff(std::unique_ptr<AccessSpecifierDiffIR> &&access_diff) {
    access_diff_ = std::move(access_diff);
  }

  void SetBaseSpecifierDiffs(
      std::unique_ptr<CXXBaseSpecifierDiffIR> &&base_diffs) {
    base_specifier_diffs_ = std::move(base_diffs);
  }

  bool DiffExists() const {
    return (type_diff_ != nullptr) || (vtable_diffs_ != nullptr) ||
        (fields_removed_.size() != 0) || (field_diffs_.size() != 0) ||
        (access_diff_ != nullptr) || (base_specifier_diffs_ != nullptr);
  }

  const TypeDiffIR *GetTypeDiff() const {
    return type_diff_.get();
  }

  const VTableLayoutDiffIR *GetVTableLayoutDiff() const {
    return vtable_diffs_.get();
  }

  const CXXBaseSpecifierDiffIR *GetBaseSpecifiers() const {
    return base_specifier_diffs_.get();
  }

 protected:
  // optional implemented with vector / std::unique_ptr.
  std::unique_ptr<TypeDiffIR> type_diff_;
  std::unique_ptr<VTableLayoutDiffIR> vtable_diffs_;
  std::vector<RecordFieldDiffIR> field_diffs_;
  std::vector<const RecordFieldIR *> fields_removed_;
  std::vector<const RecordFieldIR *> fields_added_;
  std::unique_ptr<AccessSpecifierDiffIR> access_diff_;
  std::unique_ptr<CXXBaseSpecifierDiffIR> base_specifier_diffs_;
  // Template Diffs are not needed since they will show up in the linker set
  // key.
};

class EnumFieldDiffIR {
 public:
  EnumFieldDiffIR(const EnumFieldIR *old_field, const EnumFieldIR *new_field)
      : old_field_(old_field), new_field_(new_field) {}

  const EnumFieldIR *GetOldField() const {
    return old_field_;
  }

  const EnumFieldIR *GetNewField() const {
    return new_field_;
  }

 protected:
  const EnumFieldIR *old_field_;
  const EnumFieldIR *new_field_;
};

class EnumTypeDiffIR : public DiffMessageIR {
 public:
  void SetFieldsRemoved(std::vector<const EnumFieldIR *> &&fields_removed) {
    fields_removed_ = std::move(fields_removed);
  }

  const std::vector<const EnumFieldIR *> &GetFieldsRemoved() const {
    return fields_removed_;
  }

  void SetFieldsAdded(std::vector<const EnumFieldIR *> &&fields_added) {
    fields_added_ = std::move(fields_added);
  }

  const std::vector<const EnumFieldIR *> &GetFieldsAdded() const {
    return fields_added_;
  }

  void SetFieldsDiff(std::vector<EnumFieldDiffIR> &&fields_diff) {
    fields_diff_ = std::move(fields_diff);
  }

  const std::vector<EnumFieldDiffIR> &GetFieldsDiff() const {
    return fields_diff_;
  }

  void SetUnderlyingTypeDiff(
      std::unique_ptr<std::pair<std::string, std::string>> &&utype_diff) {
    underlying_type_diff_ = std::move(utype_diff);
  }

  const std::pair<std::string, std::string> *GetUnderlyingTypeDiff() const {
    return underlying_type_diff_.get();
  }

  bool IsExtended() const {
    if (fields_removed_.size() == 0 && fields_diff_.size() == 0 &&
        fields_added_.size() != 0) {
        return true;
    }
    return false;
  }

  bool IsIncompatible() const {
    if (fields_removed_.size() != 0 || fields_diff_.size() != 0) {
        return true;
    }
    return false;
  }

  LinkableMessageKind Kind() const override {
    return LinkableMessageKind::EnumTypeKind;
  }

 protected:
  // The underlying type can only be integral, so we just need to check for
  // referenced type.
  std::unique_ptr<std::pair<std::string, std::string>> underlying_type_diff_;
  std::vector<const EnumFieldIR *> fields_removed_;
  std::vector<const EnumFieldIR *> fields_added_;
  std::vector<EnumFieldDiffIR> fields_diff_;
  // Modifiable to allow implicit construction.
  std::string name_;
};

class GlobalVarDiffIR : public DiffMessageIR {
 public:
  LinkableMessageKind Kind() const override {
    return LinkableMessageKind::GlobalVarKind;
  }

  GlobalVarDiffIR(const GlobalVarIR *old_global_var,
                  const GlobalVarIR *new_global_var)
      : old_global_var_(old_global_var), new_global_var_(new_global_var) {}

  const GlobalVarIR *GetOldGlobalVar() const {
    return old_global_var_;
  }

  const GlobalVarIR *GetNewGlobalVar() const {
    return new_global_var_;
  }

 protected:
  const GlobalVarIR *old_global_var_;
  const GlobalVarIR *new_global_var_;
};

class FunctionDiffIR : public DiffMessageIR {
 public:
  FunctionDiffIR(const FunctionIR *old_function,
                 const FunctionIR *new_function)
      : old_function_(old_function), new_function_(new_function) {}

  LinkableMessageKind Kind() const override {
    return LinkableMessageKind::FunctionKind;
  }

  const FunctionIR *GetOldFunction() const {
    return old_function_;
  }

  const FunctionIR *GetNewFunction() const {
    return new_function_;
  }

 protected:
  const FunctionIR *old_function_;
  const FunctionIR *new_function_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_IR_DIFF_REPRESENTATION_H_

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

#ifndef ABI_DIFF_H_
#define ABI_DIFF_H_

#include "diff/abi_diff_wrappers.h"
#include "repr/ir_representation.h"

#include <string>
#include <vector>


namespace header_checker {
namespace diff {


using repr::AbiElementMap;
using repr::DiffPolicyOptions;


class HeaderAbiDiff {
 public:
  HeaderAbiDiff(const std::string &lib_name, const std::string &arch,
                const std::string &old_dump, const std::string &new_dump,
                const std::string &compatibility_report,
                const std::set<std::string> &ignored_symbols,
                bool allow_adding_removing_weak_symbols,
                const DiffPolicyOptions &diff_policy_options,
                bool check_all_apis, repr::TextFormatIR text_format_old,
                repr::TextFormatIR text_format_new,
                repr::TextFormatIR text_format_diff)
      : lib_name_(lib_name), arch_(arch), old_dump_(old_dump),
        new_dump_(new_dump), cr_(compatibility_report),
        ignored_symbols_(ignored_symbols),
        diff_policy_options_(diff_policy_options),
        allow_adding_removing_weak_symbols_(allow_adding_removing_weak_symbols),
        check_all_apis_(check_all_apis),
        text_format_old_(text_format_old), text_format_new_(text_format_new),
        text_format_diff_(text_format_diff) {}

  repr::CompatibilityStatusIR GenerateCompatibilityReport();

 private:
  repr::CompatibilityStatusIR CompareTUs(
      const repr::ModuleIR &old_tu,
      const repr::ModuleIR &new_tu,
      repr::IRDiffDumper *ir_diff_dumper);

  template <typename T, typename ElfSymbolType>
  bool CollectDynsymExportables(
      const AbiElementMap<T> &old_exportables,
      const AbiElementMap<T> &new_exportables,
      const AbiElementMap<ElfSymbolType> &old_elf_symbols,
      const AbiElementMap<ElfSymbolType> &new_elf_symbols,
      const AbiElementMap<const repr::TypeIR *> &old_types_map,
      const AbiElementMap<const repr::TypeIR *> &new_types_map,
      repr::IRDiffDumper *ir_diff_dumper);

  template <typename T>
  bool Collect(
      const AbiElementMap<const T *> &old_elements_map,
      const AbiElementMap<const T *> &new_elements_map,
      const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
      const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
      repr::IRDiffDumper *ir_diff_dumper,
      const AbiElementMap<const repr::TypeIR *> &old_types_map,
      const AbiElementMap<const repr::TypeIR *> &new_types_map);

  bool CollectElfSymbols(
      const AbiElementMap<const repr::ElfSymbolIR *> &old_symbols,
      const AbiElementMap<const repr::ElfSymbolIR *> &new_symbols,
      repr::IRDiffDumper *ir_diff_dumper);

  bool PopulateElfElements(
      std::vector<const repr::ElfSymbolIR *> &elf_elements,
      repr::IRDiffDumper *ir_diff_dumper,
      repr::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool PopulateRemovedElements(
      const AbiElementMap<const T *> &old_elements_map,
      const AbiElementMap<const T *> &new_elements_map,
      const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
      const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
      repr::IRDiffDumper *ir_diff_dumper,
      repr::IRDiffDumper::DiffKind diff_kind,
      const AbiElementMap<const repr::TypeIR *> &types_map);

  template <typename T>
  bool PopulateCommonElements(
      const AbiElementMap<const T *> &old_elements_map,
      const AbiElementMap<const T *> &new_elements_map,
      const AbiElementMap<const repr::TypeIR *> &old_types,
      const AbiElementMap<const repr::TypeIR *> &new_types,
      repr::IRDiffDumper *ir_diff_dumper,
      repr::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool DumpDiffElements(
      std::vector<std::pair<const T *, const T *>> &pairs,
      const AbiElementMap<const repr::TypeIR *> &old_types,
      const AbiElementMap<const repr::TypeIR *> &new_types,
      repr::IRDiffDumper *ir_diff_dumper,
      repr::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool DumpLoneElements(
      std::vector<const T *> &elements,
      const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
      const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
      repr::IRDiffDumper *ir_diff_dumper,
      repr::IRDiffDumper::DiffKind diff_kind,
      const AbiElementMap<const repr::TypeIR *> &old_types_map);

  std::pair<AbiElementMap<const repr::EnumTypeIR *>,
            AbiElementMap<const repr::RecordTypeIR *>>
  ExtractUserDefinedTypes(const repr::ModuleIR &tu);

  bool CollectUserDefinedTypes(
      const repr::ModuleIR &old_tu,
      const repr::ModuleIR &new_tu,
      const AbiElementMap<const repr::TypeIR *> &old_types_map,
      const AbiElementMap<const repr::TypeIR *> &new_types_map,
      repr::IRDiffDumper *ir_diff_dumper);

  template <typename T>
  bool CollectUserDefinedTypesInternal(
      const AbiElementMap<const T*> &old_ud_types_map,
      const AbiElementMap<const T*> &new_ud_types_map,
      const AbiElementMap<const repr::TypeIR *> &old_types_map,
      const AbiElementMap<const repr::TypeIR *> &new_types_map,
      repr::IRDiffDumper *ir_diff_dumper);

 private:
  const std::string &lib_name_;
  const std::string &arch_;
  const std::string &old_dump_;
  const std::string &new_dump_;
  const std::string &cr_;
  const std::set<std::string> &ignored_symbols_;
  const DiffPolicyOptions &diff_policy_options_;
  bool allow_adding_removing_weak_symbols_;
  bool check_all_apis_;
  std::set<std::string> type_cache_;
  repr::TextFormatIR text_format_old_;
  repr::TextFormatIR text_format_new_;
  repr::TextFormatIR text_format_diff_;
};


}  // namespace diff
}  // namespace header_checker


#endif  // ABI_DIFF_H_

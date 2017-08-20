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

#include "abi_diff_wrappers.h"

#include <ir_representation.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#include "proto/abi_diff.pb.h"
#pragma clang diagnostic pop

#include <string>
#include <vector>

class HeaderAbiDiff {
 public:
  HeaderAbiDiff(const std::string &lib_name, const std::string &arch,
                const std::string &old_dump, const std::string &new_dump,
                const std::string &compatibility_report,
                const std::set<std::string> &ignored_symbols,
                bool check_all_apis)
      : lib_name_(lib_name), arch_(arch), old_dump_(old_dump),
        new_dump_(new_dump), cr_(compatibility_report),
        ignored_symbols_(ignored_symbols), check_all_apis_(check_all_apis) { }

  abi_util::CompatibilityStatusIR GenerateCompatibilityReport();

 private:
  abi_util::CompatibilityStatusIR CompareTUs(
      const abi_util::TextFormatToIRReader *old_tu,
      const abi_util::TextFormatToIRReader *new_tu,
      abi_util::IRDiffDumper *ir_diff_dumper);

  template <typename T, typename ElfSymbolType>
  bool CollectDynsymExportables(
    const std::vector<T> &old_exportables,
    const std::vector<T> &new_exportables,
    const std::vector<ElfSymbolType> &old_elf_symbols,
    const std::vector<ElfSymbolType> &new_elf_symbols,
    const std::map<std::string, const abi_util::TypeIR *> &old_types_map,
    const std::map<std::string, const abi_util::TypeIR *> &new_types_map,
    abi_util::IRDiffDumper *ir_diff_dumper);

  template <typename T>
  bool Collect(
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map,
      const std::map<std::string, const abi_util::ElfSymbolIR *> *old_elf_map,
      const std::map<std::string, const abi_util::ElfSymbolIR *> *new_elf_map,
      abi_util::IRDiffDumper *ir_diff_dumper);

  bool CollectElfSymbols(
      const std::map<std::string, const abi_util::ElfSymbolIR *> &old_symbols,
      const std::map<std::string, const abi_util::ElfSymbolIR *> &new_symbols,
      abi_util::IRDiffDumper *ir_diff_dumper);

  bool PopulateElfElements(
      std::vector<const abi_util::ElfSymbolIR *> &elf_elements,
      abi_util::IRDiffDumper *ir_diff_dumper,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool PopulateRemovedElements(
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map,
      const std::map<std::string, const abi_util::ElfSymbolIR *> *elf_map,
      abi_util::IRDiffDumper *ir_diff_dumper,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool PopulateCommonElements(
      const std::map<std::string, const T *> &old_elements_map,
      const std::map<std::string, const T *> &new_elements_map,
      const std::map<std::string, const abi_util::TypeIR *> &old_types,
      const std::map<std::string, const abi_util::TypeIR *> &new_types,
      abi_util::IRDiffDumper *ir_diff_dumper,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool DumpDiffElements(
      std::vector<std::pair<const T *, const T *>> &pairs,
      const std::map<std::string, const abi_util::TypeIR *> &old_types,
      const std::map<std::string, const abi_util::TypeIR *> &new_types,
      abi_util::IRDiffDumper *ir_diff_dumper,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  template <typename T>
  bool DumpLoneElements(
      std::vector<const T *> &elements,
      const std::map<std::string, const abi_util::ElfSymbolIR *> *elf_map,
      abi_util::IRDiffDumper *ir_diff_dumper,
      abi_util::IRDiffDumper::DiffKind diff_kind);

  bool CollectUserDefinedTypes(
      const abi_util::TextFormatToIRReader *old_tu,
      const abi_util::TextFormatToIRReader *new_tu,
      const std::map<std::string, const abi_util::TypeIR *> &old_types_map,
      const std::map<std::string, const abi_util::TypeIR *> &new_types_map,
      abi_util::IRDiffDumper *ir_diff_dumper);

  template <typename T>
  bool CollectUserDefinedTypesInternal(
      const std::vector<T> &old_ud_types,
      const std::vector<T> &new_ud_types,
      const std::map<std::string, const abi_util::TypeIR *> &old_types_map,
      const std::map<std::string, const abi_util::TypeIR *> &new_types_map,
      abi_util::IRDiffDumper *ir_diff_dumper);

 private:
  const std::string &lib_name_;
  const std::string &arch_;
  const std::string &old_dump_;
  const std::string &new_dump_;
  const std::string &cr_;
  const std::set<std::string> &ignored_symbols_;
  bool check_all_apis_;
  std::set<std::string> type_cache_;
};

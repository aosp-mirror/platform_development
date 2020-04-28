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

#include "diff/abi_diff.h"

#include "utils/header_abi_util.h"

#include <llvm/Support/raw_ostream.h>

#include <memory>
#include <string>
#include <vector>

#include <stdlib.h>


namespace header_checker {
namespace diff {


repr::CompatibilityStatusIR HeaderAbiDiff::GenerateCompatibilityReport() {
  std::unique_ptr<repr::IRReader> old_reader =
      repr::IRReader::CreateIRReader(text_format_old_);
  std::unique_ptr<repr::IRReader> new_reader =
      repr::IRReader::CreateIRReader(text_format_new_);
  if (!old_reader || !new_reader || !old_reader->ReadDump(old_dump_) ||
      !new_reader->ReadDump(new_dump_)) {
    llvm::errs() << "Could not create Text Format readers\n";
    ::exit(1);
  }
  std::unique_ptr<repr::IRDiffDumper> ir_diff_dumper =
      repr::IRDiffDumper::CreateIRDiffDumper(text_format_diff_, cr_);
  repr::CompatibilityStatusIR status =
      CompareTUs(old_reader->GetModule(), new_reader->GetModule(),
                 ir_diff_dumper.get());
  if (!ir_diff_dumper->Dump()) {
    llvm::errs() << "Could not dump diff report\n";
    ::exit(1);
  }
  return status;
}

repr::CompatibilityStatusIR HeaderAbiDiff::CompareTUs(
    const repr::ModuleIR &old_tu, const repr::ModuleIR &new_tu,
    repr::IRDiffDumper *ir_diff_dumper) {
  // Collect all old and new types in maps, so that we can refer to them by
  // type name / linker_set_key later.
  const AbiElementMap<const repr::TypeIR *> old_types =
      old_tu.GetTypeGraph();
  const AbiElementMap<const repr::TypeIR *> new_types =
      new_tu.GetTypeGraph();

  // CollectDynsymExportables() fills in added, removed, unsafe, and safe function diffs.
  if (!CollectDynsymExportables(old_tu.GetFunctions(), new_tu.GetFunctions(),
                                old_tu.GetElfFunctions(),
                                new_tu.GetElfFunctions(),
                                old_types, new_types,
                                ir_diff_dumper) ||
      !CollectDynsymExportables(old_tu.GetGlobalVariables(),
                                new_tu.GetGlobalVariables(),
                                old_tu.GetElfObjects(),
                                new_tu.GetElfObjects(),
                                old_types, new_types,
                                ir_diff_dumper)) {
    llvm::errs() << "Unable to collect dynsym exportables\n";
    ::exit(1);
  }

  // By the time this call is reached, all referenced types have been diffed.
  // So all additional calls on ir_diff_dumper get DiffKind::Unreferenced.
  if (check_all_apis_ && !CollectUserDefinedTypes(old_tu, new_tu, old_types,
                                                  new_types, ir_diff_dumper)) {
    llvm::errs() << "Unable to collect user defined types\n";
    ::exit(1);
  }

  repr::CompatibilityStatusIR combined_status =
      ir_diff_dumper->GetCompatibilityStatusIR();

  ir_diff_dumper->AddLibNameIR(lib_name_);
  ir_diff_dumper->AddArchIR(arch_);
  ir_diff_dumper->AddCompatibilityStatusIR(combined_status);
  return combined_status;
}

std::pair<AbiElementMap<const repr::EnumTypeIR *>,
          AbiElementMap<const repr::RecordTypeIR *>>
HeaderAbiDiff::ExtractUserDefinedTypes(const repr::ModuleIR &tu) {
  AbiElementMap<const repr::EnumTypeIR *> enum_types;
  AbiElementMap<const repr::RecordTypeIR *> record_types;
  // Iterate through the ODRListMap, if there is more than 1 element in the
  // list, we cannot really unique the type by name, so skip it. If not, add a
  // map entry UniqueId -> const Record(Enum)TypeIR *.
  for (auto &it : tu.GetODRListMap()) {
    auto &odr_list = it.second;
    if (odr_list.size() != 1) {
      continue;
    }
    const repr::TypeIR *type = *(odr_list.begin());
    const repr::RecordTypeIR *record_type = nullptr;
    switch (type->GetKind()) {
      case repr::RecordTypeKind:
        record_type = static_cast<const repr::RecordTypeIR *>(type);
        if (record_type->IsAnonymous()) {
          continue;
        }
        record_types.emplace(
            record_type->GetUniqueId(), record_type);
        break;
      case repr::EnumTypeKind:
        enum_types.emplace(
            static_cast<const repr::EnumTypeIR *>(type)->GetUniqueId(),
            static_cast<const repr::EnumTypeIR *>(type));
        break;
      case repr::FunctionTypeKind:
        continue;
      default:
        // Only user defined types should have ODR list entries.
        assert(0);
    }
  }
  return std::make_pair(std::move(enum_types), std::move(record_types));
}

bool HeaderAbiDiff::CollectUserDefinedTypes(
    const repr::ModuleIR &old_tu, const repr::ModuleIR &new_tu,
    const AbiElementMap<const repr::TypeIR *> &old_types_map,
    const AbiElementMap<const repr::TypeIR *> &new_types_map,
    repr::IRDiffDumper *ir_diff_dumper) {

  auto old_enums_and_records_extracted = ExtractUserDefinedTypes(old_tu);
  auto new_enums_and_records_extracted = ExtractUserDefinedTypes(new_tu);

  return (CollectUserDefinedTypesInternal(
              old_enums_and_records_extracted.second,
              new_enums_and_records_extracted.second, old_types_map,
              new_types_map, ir_diff_dumper) &&
          CollectUserDefinedTypesInternal(
              old_enums_and_records_extracted.first,
              new_enums_and_records_extracted.first,
              old_types_map, new_types_map, ir_diff_dumper));
}

template <typename T>
bool HeaderAbiDiff::CollectUserDefinedTypesInternal(
    const AbiElementMap<const T*> &old_ud_types_map,
    const AbiElementMap<const T*> &new_ud_types_map,
    const AbiElementMap<const repr::TypeIR *> &old_types_map,
    const AbiElementMap<const repr::TypeIR *> &new_types_map,
    repr::IRDiffDumper *ir_diff_dumper) {

  return (Collect(old_ud_types_map, new_ud_types_map, nullptr, nullptr,
                  ir_diff_dumper, old_types_map, new_types_map) &&
          PopulateCommonElements(old_ud_types_map, new_ud_types_map,
                                 old_types_map, new_types_map, ir_diff_dumper,
                                 repr::DiffMessageIR::Unreferenced));
}

template <typename T, typename ElfSymbolType>
bool HeaderAbiDiff::CollectDynsymExportables(
    const AbiElementMap<T> &old_exportables,
    const AbiElementMap<T> &new_exportables,
    const AbiElementMap<ElfSymbolType> &old_elf_symbols,
    const AbiElementMap<ElfSymbolType> &new_elf_symbols,
    const AbiElementMap<const repr::TypeIR *> &old_types_map,
    const AbiElementMap<const repr::TypeIR *> &new_types_map,
    repr::IRDiffDumper *ir_diff_dumper) {
  AbiElementMap<const T *> old_exportables_map;
  AbiElementMap<const T *> new_exportables_map;
  AbiElementMap<const repr::ElfSymbolIR *> old_elf_symbol_map;
  AbiElementMap<const repr::ElfSymbolIR *> new_elf_symbol_map;

  utils::AddToMap(&old_exportables_map, old_exportables,
                  [](auto e) { return e->first;},
                  [](auto e) {return &(e->second);});
  utils::AddToMap(&new_exportables_map, new_exportables,
                  [](auto e) { return e->first;},
                  [](auto e) { return &(e->second);});

  utils::AddToMap(&old_elf_symbol_map, old_elf_symbols,
                  [](auto e) { return e->first;},
                  [](auto e) {return &(e->second);});
  utils::AddToMap(&new_elf_symbol_map, new_elf_symbols,
                  [](auto e) { return e->first;},
                  [](auto e) {return &(e->second);});

  if (!Collect(old_exportables_map,
               new_exportables_map, &old_elf_symbol_map, &new_elf_symbol_map,
               ir_diff_dumper, old_types_map, new_types_map) ||
      !CollectElfSymbols(old_elf_symbol_map, new_elf_symbol_map,
                         ir_diff_dumper) ||
      !PopulateCommonElements(old_exportables_map, new_exportables_map,
                              old_types_map, new_types_map, ir_diff_dumper,
                              repr::DiffMessageIR::Referenced)) {
    llvm::errs() << "Diffing dynsym exportables failed\n";
    return false;
  }
  return true;
}

// Collect the added and removed elements. The ELF maps are needed because the
// metadata for some symbols might be absent from AST.  For example, if a
// function Foo() is defined in an assembly file on target A, but in a C/C++
// file on target B. Even though Foo() does not have metadata surrounding it
// when building target A, it doesn't mean that Foo() is not a part of the ABI
// of the library.
template <typename T>
bool HeaderAbiDiff::Collect(
    const AbiElementMap<const T*> &old_elements_map,
    const AbiElementMap<const T*> &new_elements_map,
    const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
    const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
    repr::IRDiffDumper *ir_diff_dumper,
    const AbiElementMap<const repr::TypeIR *> &old_types_map,
    const AbiElementMap<const repr::TypeIR *> &new_types_map) {
  if (!PopulateRemovedElements(
          old_elements_map, new_elements_map, old_elf_map, new_elf_map,
          ir_diff_dumper, repr::DiffMessageIR::Removed, old_types_map) ||
      !PopulateRemovedElements(
          new_elements_map, old_elements_map, new_elf_map, old_elf_map,
          ir_diff_dumper, repr::DiffMessageIR::Added, new_types_map)) {
    llvm::errs() << "Populating functions in report failed\n";
    return false;
  }
  return true;
}

bool HeaderAbiDiff::CollectElfSymbols(
    const AbiElementMap<const repr::ElfSymbolIR *> &old_symbols,
    const AbiElementMap<const repr::ElfSymbolIR *> &new_symbols,
    repr::IRDiffDumper *ir_diff_dumper) {
  std::vector<const repr::ElfSymbolIR *> removed_elements =
      utils::FindRemovedElements(old_symbols, new_symbols);

  std::vector<const repr::ElfSymbolIR *> added_elements =
      utils::FindRemovedElements(new_symbols, old_symbols);

  return (PopulateElfElements(removed_elements, ir_diff_dumper,
                              repr::IRDiffDumper::DiffKind::Removed) &&
          PopulateElfElements(added_elements, ir_diff_dumper,
                              repr::IRDiffDumper::DiffKind::Added));
}

bool HeaderAbiDiff::PopulateElfElements(
    std::vector<const repr::ElfSymbolIR *> &elf_elements,
    repr::IRDiffDumper *ir_diff_dumper,
    repr::IRDiffDumper::DiffKind diff_kind) {
  for (auto &&elf_element : elf_elements) {
    if (allow_adding_removing_weak_symbols_ &&
        elf_element->GetBinding() == repr::ElfSymbolIR::Weak) {
      continue;
    }
    if (!ir_diff_dumper->AddElfSymbolMessageIR(elf_element, diff_kind)) {
      return false;
    }
  }
  return true;
}

template <typename T>
bool HeaderAbiDiff::PopulateRemovedElements(
    const AbiElementMap<const T*> &old_elements_map,
    const AbiElementMap<const T*> &new_elements_map,
    const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
    const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
    repr::IRDiffDumper *ir_diff_dumper,
    repr::IRDiffDumper::DiffKind diff_kind,
    const AbiElementMap<const repr::TypeIR *> &removed_types_map) {
  std::vector<const T *> removed_elements =
      utils::FindRemovedElements(old_elements_map, new_elements_map);
  if (!DumpLoneElements(removed_elements, old_elf_map, new_elf_map,
                        ir_diff_dumper, diff_kind, removed_types_map)) {
    llvm::errs() << "Dumping added or removed element to report failed\n";
    return false;
  }
  return true;
}

// Find the common elements (common records, common enums, common functions etc)
// Dump the differences (we need type maps for this diff since we'll get
// reachable types from here)
template <typename T>
bool HeaderAbiDiff::PopulateCommonElements(
    const AbiElementMap<const T *> &old_elements_map,
    const AbiElementMap<const T *> &new_elements_map,
    const AbiElementMap<const repr::TypeIR *> &old_types,
    const AbiElementMap<const repr::TypeIR *> &new_types,
    repr::IRDiffDumper *ir_diff_dumper,
    repr::IRDiffDumper::DiffKind diff_kind) {
  std::vector<std::pair<const T *, const T *>> common_elements =
      utils::FindCommonElements(old_elements_map, new_elements_map);
  if (!DumpDiffElements(common_elements, old_types, new_types,
                        ir_diff_dumper, diff_kind)) {
    llvm::errs() << "Dumping difference in common element to report failed\n";
    return false;
  }
  return true;
}

template <typename T>
bool HeaderAbiDiff::DumpLoneElements(
    std::vector<const T *> &elements,
    const AbiElementMap<const repr::ElfSymbolIR *> *old_elf_map,
    const AbiElementMap<const repr::ElfSymbolIR *> *new_elf_map,
    repr::IRDiffDumper *ir_diff_dumper,
    repr::IRDiffDumper::DiffKind diff_kind,
    const AbiElementMap<const repr::TypeIR *> &types_map) {
  std::smatch source_file_match;
  std::regex source_file_regex(" at ");

  for (auto &&element : elements) {
    if (IgnoreSymbol<T>(element, ignored_symbols_,
                        [](const T *e) {return e->GetLinkerSetKey();})) {
      continue;
    }

    // If an element (FunctionIR or GlobalVarIR) is missing from the new ABI
    // dump but a corresponding ELF symbol (ElfFunctionIR or ElfObjectIR) can
    // be found in the new ABI dump file, don't emit error on this element.
    // This may happen when the standard reference target implements the
    // function (or the global variable) in C/C++ and the target-under-test
    // implements the function (or the global variable) in assembly.
    const std::string &element_linker_set_key = element->GetLinkerSetKey();
    if (new_elf_map &&
        new_elf_map->find(element_linker_set_key) != new_elf_map->end()) {
      continue;
    }

    // If the `-ignore-weak-symbols` option is enabled, ignore the element if
    // it was a weak symbol.
    if (allow_adding_removing_weak_symbols_ && old_elf_map) {
      auto elem_it = old_elf_map->find(element_linker_set_key);
      if (elem_it != old_elf_map->end() &&
          elem_it->second->GetBinding() == repr::ElfSymbolIR::Weak) {
        continue;
      }
    }

    // If the record / enum has source file information, skip it.
    if (std::regex_search(element_linker_set_key, source_file_match,
                          source_file_regex)) {
      continue;
    }

    auto element_copy = *element;
    ReplaceTypeIdsWithTypeNames(types_map, &element_copy);
    if (!ir_diff_dumper->AddLinkableMessageIR(&element_copy, diff_kind)) {
      llvm::errs() << "Couldn't dump added or removed element\n";
      return false;
    }
  }
  return true;
}

template <typename T>
bool HeaderAbiDiff::DumpDiffElements(
    std::vector<std::pair<const T *,const T *>> &pairs,
    const AbiElementMap<const repr::TypeIR *> &old_types,
    const AbiElementMap<const repr::TypeIR *> &new_types,
    repr::IRDiffDumper *ir_diff_dumper,
    repr::IRDiffDumper::DiffKind diff_kind) {
  for (auto &&pair : pairs) {
    const T *old_element = pair.first;
    const T *new_element = pair.second;

    if (IgnoreSymbol<T>(old_element, ignored_symbols_,
                        [](const T *e) {return e->GetLinkerSetKey();})) {
      continue;
    }

    DiffWrapper<T> diff_wrapper(
        old_element, new_element, ir_diff_dumper, old_types, new_types,
        diff_policy_options_, &type_cache_);
    if (!diff_wrapper.DumpDiff(diff_kind)) {
      llvm::errs() << "Failed to diff elements\n";
      return false;
    }
  }
  return true;
}


}  // namespace diff
}  // namespace header_checker

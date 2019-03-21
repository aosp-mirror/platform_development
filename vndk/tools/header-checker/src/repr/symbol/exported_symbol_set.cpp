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

#include "repr/symbol/exported_symbol_set.h"

#include "repr/ir_representation.h"
#include "utils/stl_utils.h"
#include "utils/string_utils.h"

#include <fnmatch.h>
#include <cxxabi.h>


namespace header_checker {
namespace repr {


static inline bool IsCppSymbol(const std::string &name) {
  return utils::StartsWith(name, "_Z");
}


static inline bool HasMatchingGlobPattern(
    const ExportedSymbolSet::GlobPatternSet &patterns, const char *text) {
  for (auto &&pattern : patterns) {
    if (fnmatch(pattern.c_str(), text, 0) == 0) {
      return true;
    }
  }
  return false;
}


static inline bool HasMatchingGlobPattern(
    const ExportedSymbolSet::GlobPatternSet &patterns,
    const std::string &text) {
  return HasMatchingGlobPattern(patterns, text.c_str());
}


void ExportedSymbolSet::AddFunction(const std::string &name,
                                    ElfSymbolIR::ElfSymbolBinding binding) {
  funcs_.emplace(name, ElfFunctionIR(name, binding));
}


void ExportedSymbolSet::AddVar(const std::string &name,
                               ElfSymbolIR::ElfSymbolBinding binding) {
  vars_.emplace(name, ElfObjectIR(name, binding));
}


bool ExportedSymbolSet::HasSymbol(const std::string &name) const {
  if (funcs_.find(name) != funcs_.end()) {
    return true;
  }

  if (vars_.find(name) != vars_.end()) {
    return true;
  }

  if (HasMatchingGlobPattern(glob_patterns_, name)) {
    return true;
  }

  if (IsCppSymbol(name) && HasDemangledCppSymbolsOrPatterns()) {
    std::unique_ptr<char, utils::FreeDeleter> demangled_name_c_str(
        abi::__cxa_demangle(name.c_str(), nullptr, nullptr, nullptr));

    if (demangled_name_c_str) {
      std::string_view demangled_name(demangled_name_c_str.get());

      if (demangled_cpp_symbols_.find(demangled_name) !=
          demangled_cpp_symbols_.end()) {
        return true;
      }

      if (HasMatchingGlobPattern(demangled_cpp_glob_patterns_,
                                 demangled_name_c_str.get())) {
        return true;
      }
    }
  }

  return false;
}


}  // namespace repr
}  // namespace header_checker

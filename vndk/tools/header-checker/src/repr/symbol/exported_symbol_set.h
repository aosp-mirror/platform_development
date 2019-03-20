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

#ifndef EXPORTED_SYMBOL_SET_
#define EXPORTED_SYMBOL_SET_

#include "repr/ir_representation.h"

#include <functional>
#include <map>
#include <set>
#include <string>


namespace header_checker {
namespace repr {


class ExportedSymbolSet {
 public:
  using FunctionMap = std::map<std::string, ElfFunctionIR, std::less<>>;
  using VarMap = std::map<std::string, ElfObjectIR, std::less<>>;
  using NameSet = std::set<std::string, std::less<>>;
  using GlobPatternSet = std::set<std::string, std::less<>>;


 public:
  ExportedSymbolSet() {}

  const FunctionMap &GetFunctions() const {
    return funcs_;
  }

  const VarMap &GetVars() const {
    return vars_;
  }

  const GlobPatternSet &GetGlobPatterns() const {
    return glob_patterns_;
  }

  const GlobPatternSet &GetDemangledCppGlobPatterns() const {
    return demangled_cpp_glob_patterns_;
  }

  const NameSet &GetDemangledCppSymbols() const {
    return demangled_cpp_symbols_;
  }

  bool HasSymbol(const std::string &symbol_name) const;

  void AddFunction(const std::string &name,
                   ElfSymbolIR::ElfSymbolBinding binding);

  void AddVar(const std::string &name, ElfSymbolIR::ElfSymbolBinding binding);

  void AddGlobPattern(const std::string &pattern) {
    glob_patterns_.insert(pattern);
  }

  void AddDemangledCppGlobPattern(const std::string &pattern) {
    demangled_cpp_glob_patterns_.insert(pattern);
  }

  void AddDemangledCppSymbol(const std::string &pattern) {
    demangled_cpp_symbols_.insert(pattern);
  }


 private:
  bool HasDemangledCppSymbolsOrPatterns() const {
    return (!demangled_cpp_glob_patterns_.empty() ||
            !demangled_cpp_symbols_.empty());
  }


 private:
  FunctionMap funcs_;
  VarMap vars_;

  GlobPatternSet glob_patterns_;
  GlobPatternSet demangled_cpp_glob_patterns_;
  NameSet demangled_cpp_symbols_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // EXPORTED_SYMBOL_SET_

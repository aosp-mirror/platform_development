// Copyright (C) 2018 The Android Open Source Project
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

#ifndef VERSION_SCRIPT_PARSER_H_
#define VERSION_SCRIPT_PARSER_H_

#include "ir_representation.h"

#include <map>
#include <set>
#include <string>

namespace abi_util {

class VersionScriptParser {
 public:
  enum LineScope {
    global,
    local,
  };

  VersionScriptParser(const std::string &version_script,
                      const std::string &arch,
                      const std::string &api);
  bool Parse();

  const std::map<std::string, ElfFunctionIR> &GetFunctions();

  const std::map<std::string, ElfObjectIR> &GetGlobVars();

  const std::set<std::string> &GetFunctionRegexs();

  const std::set<std::string> &GetGlobVarRegexs();

 private:
  bool ParseInnerBlock(std::ifstream &symbol_ifstream);

  LineScope GetLineScope(std::string &line, LineScope scope);

  bool ParseSymbolLine(const std::string &line);

  bool SymbolInArchAndApiVersion(const std::string &line,
                                 const std::string &arch, int api);

  bool SymbolExported(const std::string &line, const std::string &arch,
                      int api);

  int ApiStrToInt(const std::string &api);

  void AddToVars(std::string &symbol);

  void AddToFunctions(std::string &symbol);

 private:
  const std::string &version_script_;
  const std::string &arch_;
  std::map<std::string, ElfFunctionIR> functions_;
  std::map<std::string, ElfObjectIR> globvars_;
  // Added to speed up version script parsing and linking.
  std::set<std::string> function_regexs_;
  std::set<std::string> globvar_regexs_;
  int api_;
};

}  // namespace abi_util

#endif  // VERSION_SCRIPT_PARSER_H_

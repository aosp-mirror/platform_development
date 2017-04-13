// Copyright (C) 2017 The Android Open Source Project
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

#include <set>
#include <string>
#include <vector>

namespace abi_util {

bool CollectExportedHeaderSet(const std::string &dir_name,
                              std::set<std::string> *exported_headers);
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

  const std::set<std::string> &GetFunctions();

  const std::set<std::string> &GetGlobVars();

 private:

  bool ParseInnerBlock(std::ifstream &symbol_ifstream);

  LineScope GetLineScope(std::string &line, LineScope scope);

  bool ParseSymbolLine(const std::string &line);

  bool SymbolInArchAndApiVersion(const std::string &line,
                                 const std::string &arch, int api);

  bool SymbolExported(const std::string &line, const std::string &arch,
                      int api);

  int ApiStrToInt(const std::string &api);

 private:
  const std::string &version_script_;
  const std::string &arch_;
  std::set<std::string> functions_;
  std::set<std::string> globvars_;
  int api_;
};

} // namespace abi_util

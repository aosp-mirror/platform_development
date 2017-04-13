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

#include <header_abi_util.h>

#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>

#include <memory>
#include <fstream>
#include <iostream>
#include <set>
#include <unordered_set>
#include <string>
#include <vector>
#include <regex>

namespace abi_util {

#define FUTURE_API 10000

std::unordered_set<std::string> AllArches({"arm", "arm64", "x86", "x86_64",
                                        "mips", "mips64"});

static bool StringContains(const std::string &line,
                           const std::string &substring) {
  return (line.find(substring) != std::string::npos);
}

static bool LineSatisfiesArch(const std::string &line,
                              const std::string arch) {
  bool has_arch_tags = false;
  for (auto &&possible_arch : AllArches) {
    if (StringContains(line, possible_arch)) {
      has_arch_tags = true;
      break;
    }
  }
  return (has_arch_tags && StringContains(line, arch)) || !has_arch_tags;
}

VersionScriptParser::VersionScriptParser(const std::string &version_script,
                                         const std::string &arch,
                                         const std::string &api) :
  version_script_(version_script), arch_(arch), api_(ApiStrToInt(api)) { }

int VersionScriptParser::ApiStrToInt(const std::string &api) {
  // Follow what build/soong/cc/gen_stub_libs.py does.
  if (api == "current") {
    return FUTURE_API;
  }
  return std::stoi(api);
}

bool VersionScriptParser::SymbolInArchAndApiVersion(const std::string &line,
                                                    const std::string &arch,
                                                    int api) {
  // If the tags do not have an "introduced" requirement, the symbol is
  // exported.
  if (!StringContains(line, "introduced") && LineSatisfiesArch(line, arch)) {
    return true;
  }
  if (line == "future") {
    return api == FUTURE_API;
  }
  const std::string regex_match_string1 = " *introduced-" + arch + "=([0-9]+)";
  const std::string regex_match_string2 = " *introduced=([0-9]+)";
  std::smatch matcher1;
  std::smatch matcher2;
  std::regex match_clause1(regex_match_string1);
  std::regex match_clause2(regex_match_string2);
  int matched_api = -1;
  if (std::regex_search(line, matcher1, match_clause1)) {
    matched_api = std::stoi(matcher1.str(1));
  } else if ((std::regex_search(line, matcher2, match_clause2)) &&
    LineSatisfiesArch(line, arch)) {
    matched_api = std::stoi(matcher2.str(1));
  }
  if ( matched_api > 0 && api >= matched_api) {
    return true;
  }
  return false;
}

bool VersionScriptParser::SymbolExported(const std::string &line,
                                         const std::string &arch, int api) {
  // Empty line means that the symbol is exported
  if (line.empty() || SymbolInArchAndApiVersion(line, arch, api)) {
    return true;
  }
  return false;
}

bool VersionScriptParser::ParseSymbolLine(const std::string &line) {
  //The symbol lies before the ; and the tags are after ;
  std::string::size_type pos = line.find(";");
  if (pos == std::string::npos) {
    llvm::errs() << "Couldn't find end of symbol" << line <<"\n";
    return false;
  }
  std::string symbol = line.substr(0, pos);
  std::string::size_type last_space = symbol.find_last_of(' ');
  symbol = symbol.substr(last_space + 1, pos);
  std::string tags = line.substr(pos + 1);
  if (SymbolExported(tags, arch_, api_)) {
    if (StringContains(tags, "var")) {
      globvars_.insert(symbol);
    } else {
      functions_.insert(symbol);
    }
  }
  return true;
}

typedef VersionScriptParser::LineScope LineScope;

LineScope VersionScriptParser::GetLineScope(std::string &line,
                                            LineScope scope) {
  if (StringContains(line, "local:")) {
    scope = LineScope::local;
  }
  return scope;
}

bool VersionScriptParser::ParseInnerBlock(std::ifstream &symbol_ifstream) {
  std::string line = "";
  LineScope scope = LineScope::global;

  while (std::getline(symbol_ifstream, line)) {
    if (line.find("}") != std::string::npos) {
      break;
    }
    if (line.c_str()[0] == '#') {
      continue;
    }
    scope = GetLineScope(line, scope);
    if (scope != LineScope::global || StringContains(line, "global:")) {
      continue;
    }
    ParseSymbolLine(line);
  }
  return true;
}

const std::set<std::string> &VersionScriptParser::GetFunctions() {
  return functions_;
}

const std::set<std::string> &VersionScriptParser::GetGlobVars() {
  return globvars_;
}

bool VersionScriptParser::Parse() {
  std::ifstream symbol_ifstream(version_script_);
  if (!symbol_ifstream.is_open()) {
    llvm::errs() << "Failed to open version script file\n";
    return false;
  }
  std::string line = "";

  while (std::getline(symbol_ifstream, line)) {
    // Skip comment lines.
    if (line.c_str()[0] == '#') {
      continue;
    }
    if (StringContains(line, "{")) {

      if ((StringContains(line, "PRIVATE"))) {
        continue;
      }
      ParseInnerBlock(symbol_ifstream);
    }
  }
  return true;
}

} // namespace abi_util

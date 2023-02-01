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

#include "utils/config_file.h"

#include <json/json.h>
#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <map>
#include <string>


static const std::string GLOBAL_SECTION_NAME = "global";


namespace header_checker {
namespace utils {


static std::map<std::string, bool> LoadFlags(const Json::Value &section) {
  std::map<std::string, bool> map;
  if (section.isMember("flags")) {
    for (auto &flag_keys : section["flags"].getMemberNames()) {
      map[flag_keys] = section["flags"][flag_keys].asBool();
    }
  }
  return map;
}

static std::vector<std::string>
LoadIgnoreLinkerSetKeys(const Json::Value &section) {
  std::vector<std::string> vec;
  for (auto &key : section.get("ignore_linker_set_keys", {})) {
    vec.emplace_back(key.asString());
  }
  return vec;
}

bool ConfigFile::HasGlobalSection() {
  return HasSection(GLOBAL_SECTION_NAME, "");
}

const ConfigSection &ConfigFile::GetGlobalSection() {
  return GetSection(GLOBAL_SECTION_NAME, "");
}

bool ConfigFile::Load(std::istream &istream) {
  Json::Value root;
  Json::CharReaderBuilder builder;
  std::string errorMessage;
  if (!Json::parseFromStream(builder, istream, &root, &errorMessage)) {
    llvm::errs() << "Failed to parse JSON: " << errorMessage << "\n";
    return false;
  }
  for (auto &key : root.getMemberNames()) {
    if (key == GLOBAL_SECTION_NAME) {
      ConfigSection &config_section = map_[{GLOBAL_SECTION_NAME, ""}];
      config_section.map_ = LoadFlags(root[GLOBAL_SECTION_NAME]);
      config_section.ignored_linker_set_keys_ =
          LoadIgnoreLinkerSetKeys(root[GLOBAL_SECTION_NAME]);
      continue;
    }
    for (auto &section : root[key]) {
      ConfigSection &config_section =
          map_[{key, section["target_version"].asString()}];
      config_section.map_ = LoadFlags(section);
      config_section.ignored_linker_set_keys_ = LoadIgnoreLinkerSetKeys(section);
    }
  }
  return true;
}

bool ConfigFile::Load(const std::string &path) {
  std::ifstream stream(path);
  return stream && Load(stream);
}

}  // namespace utils
}  // namespace header_checker

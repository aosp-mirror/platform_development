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
#include <string>


namespace header_checker {
namespace utils {


bool ConfigFile::Load(std::istream &istream) {
  Json::Value root;
  Json::CharReaderBuilder builder;
  std::string errorMessage;
  if (!Json::parseFromStream(builder, istream, &root, &errorMessage)) {
    llvm::errs() << "Failed to parse JSON: " << errorMessage << "\n";
    return false;
  }
  for (auto &key : root.getMemberNames()) {
    map_[key] = ConfigSection();
    if (root[key].isMember("flags")) {
      for (auto &flag_keys : root[key]["flags"].getMemberNames()) {
        map_[key].map_[flag_keys] = root[key]["flags"][flag_keys].asBool();
      }
    }
  }
  return true;
}

bool ConfigFile::Load(const std::string &path) {
  std::ifstream stream(path);
  return Load(stream);
}


}  // namespace utils
}  // namespace header_checker

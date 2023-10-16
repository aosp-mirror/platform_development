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

#include "utils/api_level.h"

#include <json/reader.h>
#include <llvm/Support/raw_ostream.h>

#include "utils/string_utils.h"


namespace header_checker {
namespace utils {


bool ApiLevelMap::Load(std::istream &stream) {
  Json::CharReaderBuilder builder;
  Json::Value json;
  std::string error_message;
  if (!Json::parseFromStream(builder, stream, &json, &error_message)) {
    llvm::errs() << "Cannot load ApiLevelMap: " << error_message << "\n";
    return false;
  }

  const Json::Value null_value;
  for (const Json::String &key : json.getMemberNames()) {
    Json::Value value = json.get(key, null_value);
    if (!value.isInt()) {
      llvm::errs() << "Cannot load ApiLevelMap: " << key
                   << " is not mapped to an integer.\n";
      return false;
    }
    codename_to_api_level_[key] = value.asInt();
  }
  return true;
}

llvm::Optional<ApiLevel> ApiLevelMap::Parse(const std::string &api) const {
  auto it = codename_to_api_level_.find(api);
  if (it != codename_to_api_level_.end()) {
    return it->second;
  }
  return ParseInt(api);
}


}  // namespace utils
}  // namespace header_checker

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

#include "utils/string_utils.h"

#include <llvm/ADT/Optional.h>

#include <cassert>
#include <string>


namespace header_checker {
namespace utils {


llvm::Optional<ApiLevel> ParseApiLevel(const std::string &api_level_str) {
  if (api_level_str == "current") {
    return FUTURE_API_LEVEL;
  }
  return ParseInt(api_level_str);
}


}  // namespace utils
}  // namespace header_checker

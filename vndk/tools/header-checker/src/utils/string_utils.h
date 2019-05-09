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

#ifndef STRING_UTILS_H_
#define STRING_UTILS_H_

#include <optional>
#include <string>
#include <vector>


namespace header_checker {
namespace utils {


std::string_view Trim(std::string_view s);

bool StartsWith(std::string_view s, std::string_view prefix);

bool EndsWith(std::string_view s, std::string_view suffix);

std::vector<std::string_view> Split(std::string_view s,
                                    std::string_view delim_chars);

std::optional<int> ParseInt(const std::string &s);

bool ParseBool(const std::string &s);

bool IsGlobPattern(std::string_view s);


}  // namespace utils
}  // namespace header_checker


#endif  // STRING_UTILS_H_

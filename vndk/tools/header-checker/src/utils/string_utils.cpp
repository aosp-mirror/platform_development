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

#include "utils/string_utils.h"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <string>
#include <utility>


namespace header_checker {
namespace utils {


std::string_view Trim(std::string_view s) {
  std::string::size_type start = s.find_first_not_of(" \t\r\n");
  if (start == std::string::npos) {
    return "";
  }
  std::string::size_type end = s.find_last_not_of(" \t\r\n");
  if (end == std::string::npos) {
    return s.substr(start);
  }
  return s.substr(start, end - start + 1);
}


bool StartsWith(std::string_view s, std::string_view prefix) {
  return s.size() >= prefix.size() && s.compare(0, prefix.size(), prefix) == 0;
}


bool EndsWith(std::string_view s, std::string_view suffix) {
  return (s.size() >= suffix.size() &&
          s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0);
}


std::vector<std::string_view> Split(std::string_view s,
                                    std::string_view delim_chars) {
  std::vector<std::string_view> res;
  std::string::size_type pos = 0;
  while (true) {
    pos = s.find_first_not_of(delim_chars, pos);
    if (pos == std::string::npos) {
      break;
    }

    std::string::size_type end = s.find_first_of(delim_chars, pos + 1);
    if (end == std::string::npos) {
      res.push_back(s.substr(pos));
      break;
    }

    res.push_back(s.substr(pos, end - pos));
    pos = end + 1;
  }
  return res;
}


std::optional<int> ParseInt(const std::string &s) {
  const char *start = s.c_str();
  if (*start == '\0') {
    return {};
  }

  char *endptr = nullptr;
  long int res = ::strtol(start, &endptr, 10);
  if (*endptr != '\0') {
    return {};
  }

  return static_cast<int>(res);
}


bool ParseBool(const std::string &s) {
  std::string value(s);
  std::transform(value.begin(), value.end(), value.begin(), std::tolower);
  return (value == "true" || value == "on" || value == "1");
}


bool IsGlobPattern(std::string_view s) {
  return s.find_first_of("*?[") != std::string::npos;
}


}  // namespace utils
}  // namespace header_checker

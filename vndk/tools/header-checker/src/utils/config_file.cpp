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

#include "utils/string_utils.h"

#include <fstream>
#include <map>
#include <string>


namespace header_checker {
namespace utils {


ConfigFile ConfigParser::ParseFile(std::istream &istream) {
  ConfigParser parser(istream);
  return parser.ParseFile();
}


ConfigFile ConfigParser::ParseFile(const std::string &path) {
  std::ifstream stream(path, std::ios_base::in);
  return ParseFile(stream);
}


ConfigFile ConfigParser::ParseFile() {
  size_t line_no = 0;
  std::string line;
  while (std::getline(stream_, line)) {
    ParseLine(++line_no, line);
  }
  return std::move(cfg_);
}


void ConfigParser::ParseLine(size_t line_no, std::string_view line) {
  if (line.empty() || line[0] == ';' || line[0] == '#') {
    // Skip empty or comment line.
    return;
  }

  // Parse section name line.
  if (line[0] == '[') {
    std::string::size_type pos = line.rfind(']');
    if (pos == std::string::npos) {
      ReportError(line_no, "bad section name line");
      return;
    }
    std::string_view section_name = line.substr(1, pos - 1);
    section_ = &cfg_.map_[std::string(section_name)];
    return;
  }

  // Parse key-value line.
  std::string::size_type pos = line.find('=');
  if (pos == std::string::npos) {
    ReportError(line_no, "bad key-value line");
    return;
  }

  // Add key-value entry to current section.
  std::string_view key = Trim(line.substr(0, pos));
  std::string_view value = Trim(line.substr(pos + 1));

  if (!section_) {
    section_ = &cfg_.map_[""];
  }
  section_->map_[std::string(key)] = std::string(value);
}


}  // namespace utils
}  // namespace header_checker

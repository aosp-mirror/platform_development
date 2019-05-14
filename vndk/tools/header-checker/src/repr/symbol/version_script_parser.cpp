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

#include "repr/symbol/version_script_parser.h"

#include "repr/symbol/exported_symbol_set.h"
#include "utils/string_utils.h"

#include <iostream>
#include <memory>
#include <regex>
#include <set>
#include <string>
#include <vector>


namespace header_checker {
namespace repr {


static constexpr char DEFAULT_ARCH[] = "arm64";


inline std::string GetIntroducedArchTag(const std::string &arch) {
  return "introduced-" + arch + "=";
}


VersionScriptParser::VersionScriptParser()
    : arch_(DEFAULT_ARCH), introduced_arch_tag_(GetIntroducedArchTag(arch_)),
      api_level_(utils::FUTURE_API_LEVEL), stream_(nullptr), line_no_(0) {}


void VersionScriptParser::SetArch(const std::string &arch) {
  arch_ = arch;
  introduced_arch_tag_ = GetIntroducedArchTag(arch);
}


VersionScriptParser::ParsedTags VersionScriptParser::ParseSymbolTags(
    const std::string &line) {
  static const char *const POSSIBLE_ARCHES[] = {
      "arm", "arm64", "x86", "x86_64", "mips", "mips64"};

  ParsedTags result;

  std::string_view line_view(line);
  std::string::size_type comment_pos = line_view.find('#');
  if (comment_pos == std::string::npos) {
    return result;
  }

  std::string_view comment_line = line_view.substr(comment_pos + 1);
  std::vector<std::string_view> tags = utils::Split(comment_line, " \t");

  bool has_introduced_arch_tags = false;

  for (auto &&tag : tags) {
    // Check excluded tags.
    if (excluded_symbol_tags_.find(tag) != excluded_symbol_tags_.end()) {
      result.has_excluded_tags_ = true;
    }

    // Check the var tag.
    if (tag == "var") {
      result.has_var_tag_ = true;
      continue;
    }

    // Check arch tags.
    if (tag == arch_) {
      result.has_arch_tags_ = true;
      result.has_current_arch_tag_ = true;
      continue;
    }

    for (auto &&possible_arch : POSSIBLE_ARCHES) {
      if (tag == possible_arch) {
        result.has_arch_tags_ = true;
        break;
      }
    }

    // Check introduced tags.
    if (utils::StartsWith(tag, "introduced=")) {
      std::optional<utils::ApiLevel> intro = utils::ParseApiLevel(
          std::string(tag.substr(sizeof("introduced=") - 1)));
      if (!intro) {
        ReportError("Bad introduced tag: " + std::string(tag));
      } else {
        if (!has_introduced_arch_tags) {
          result.has_introduced_tags_ = true;
          result.introduced_ = intro.value();
        }
      }
      continue;
    }

    if (utils::StartsWith(tag, introduced_arch_tag_)) {
      std::optional<utils::ApiLevel> intro = utils::ParseApiLevel(
          std::string(tag.substr(introduced_arch_tag_.size())));
      if (!intro) {
        ReportError("Bad introduced tag " + std::string(tag));
      } else {
        has_introduced_arch_tags = true;
        result.has_introduced_tags_ = true;
        result.introduced_ = intro.value();
      }
      continue;
    }

    // Check the future tag.
    if (tag == "future") {
      result.has_future_tag_ = true;
      continue;
    }

    // Check the weak binding tag.
    if (tag == "weak") {
      result.has_weak_tag_ = true;
      continue;
    }
  }

  return result;
}


bool VersionScriptParser::IsSymbolExported(
    const VersionScriptParser::ParsedTags &tags) {
  if (tags.has_excluded_tags_) {
    return false;
  }

  if (tags.has_arch_tags_ && !tags.has_current_arch_tag_) {
    return false;
  }

  if (tags.has_future_tag_) {
    return api_level_ == utils::FUTURE_API_LEVEL;
  }

  if (tags.has_introduced_tags_) {
    return api_level_ >= tags.introduced_;
  }

  return true;
}


bool VersionScriptParser::ParseSymbolLine(const std::string &line,
                                          bool is_in_extern_cpp) {
  // The symbol name comes before the ';'.
  std::string::size_type pos = line.find(";");
  if (pos == std::string::npos) {
    ReportError("No semicolon at the end of the symbol line: " + line);
    return false;
  }

  std::string symbol(utils::Trim(line.substr(0, pos)));

  ParsedTags tags = ParseSymbolTags(line);
  if (!IsSymbolExported(tags)) {
    return true;
  }

  if (is_in_extern_cpp) {
    if (utils::IsGlobPattern(symbol)) {
      exported_symbols_->AddDemangledCppGlobPattern(symbol);
    } else {
      exported_symbols_->AddDemangledCppSymbol(symbol);
    }
    return true;
  }

  if (utils::IsGlobPattern(symbol)) {
    exported_symbols_->AddGlobPattern(symbol);
    return true;
  }

  ElfSymbolIR::ElfSymbolBinding binding =
      tags.has_weak_tag_ ? ElfSymbolIR::ElfSymbolBinding::Weak
                         : ElfSymbolIR::ElfSymbolBinding::Global;

  if (tags.has_var_tag_) {
    exported_symbols_->AddVar(symbol, binding);
  } else {
    exported_symbols_->AddFunction(symbol, binding);
  }
  return true;
}


bool VersionScriptParser::ParseVersionBlock(bool ignore_symbols) {
  static const std::regex EXTERN_CPP_PATTERN(R"(extern\s*"[Cc]\+\+"\s*\{)");

  LineScope scope = LineScope::GLOBAL;
  bool is_in_extern_cpp = false;

  while (true) {
    std::string line;
    if (!ReadLine(line)) {
      break;
    }

    if (line.find("}") != std::string::npos) {
      if (is_in_extern_cpp) {
        is_in_extern_cpp = false;
        continue;
      }
      return true;
    }

    // Check extern "c++"
    if (std::regex_match(line, EXTERN_CPP_PATTERN)) {
      is_in_extern_cpp = true;
      continue;
    }

    // Check symbol visibility label
    if (utils::StartsWith(line, "local:")) {
      scope = LineScope::LOCAL;
      continue;
    }
    if (utils::StartsWith(line, "global:")) {
      scope = LineScope::GLOBAL;
      continue;
    }
    if (scope != LineScope::GLOBAL) {
      continue;
    }

    // Parse symbol line
    if (!ignore_symbols) {
      if (!ParseSymbolLine(line, is_in_extern_cpp)) {
        return false;
      }
    }
  }

  ReportError("No matching closing parenthesis");
  return false;
}


std::unique_ptr<ExportedSymbolSet> VersionScriptParser::Parse(
    std::istream &stream) {
  // Initialize the parser context
  stream_ = &stream;
  line_no_ = 0;
  exported_symbols_.reset(new ExportedSymbolSet());

  // Parse
  while (true) {
    std::string line;
    if (!ReadLine(line)) {
      break;
    }

    std::string::size_type lparen_pos = line.find("{");
    if (lparen_pos == std::string::npos) {
      ReportError("No version opening parenthesis" + line);
      return nullptr;
    }

    std::string version(utils::Trim(line.substr(0, lparen_pos - 1)));
    bool exclude_symbol_version = (excluded_symbol_versions_.find(version) !=
                                   excluded_symbol_versions_.end());

    if (!ParseVersionBlock(exclude_symbol_version)) {
      return nullptr;
    }
  }

  return std::move(exported_symbols_);
}


bool VersionScriptParser::ReadLine(std::string &line) {
  while (std::getline(*stream_, line)) {
    ++line_no_;
    line = std::string(utils::Trim(line));
    if (line.empty() || line[0] == '#') {
      continue;
    }
    return true;
  }
  return false;
}


VersionScriptParser::ErrorHandler::~ErrorHandler() {}


}  // namespace repr
}  // namespace header_checker

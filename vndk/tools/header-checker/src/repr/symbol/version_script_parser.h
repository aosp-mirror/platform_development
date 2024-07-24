// Copyright (C) 2018 The Android Open Source Project
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

#ifndef VERSION_SCRIPT_PARSER_H_
#define VERSION_SCRIPT_PARSER_H_

#include "repr/ir_representation.h"
#include "repr/symbol/exported_symbol_set.h"
#include "utils/api_level.h"

#include <functional>
#include <map>
#include <string>


namespace header_checker {
namespace repr {


enum ModeTagPolicy {
  MatchTagAndApi,
  MatchTagOnly,
};

class VersionScriptParser {
 private:
  // This comparison function allows finding elements by string_view.
  using ModeTagLevelMap = std::map<std::string, utils::ApiLevel, std::less<>>;

  enum class LineScope {
    GLOBAL,
    LOCAL,
  };

  struct ParsedTags {
   public:
    unsigned has_arch_tags_ : 1;
    unsigned has_current_arch_tag_ : 1;
    unsigned has_introduced_tags_ : 1;
    unsigned has_excluded_tags_ : 1;
    unsigned has_future_tag_ : 1;
    unsigned has_var_tag_ : 1;
    unsigned has_weak_tag_ : 1;
    utils::ApiLevel introduced_;
    ModeTagLevelMap mode_tags_;

   public:
    ParsedTags()
        : has_arch_tags_(0), has_current_arch_tag_(0), has_introduced_tags_(0),
          has_excluded_tags_(0), has_future_tag_(0), has_var_tag_(0),
          introduced_(-1) {}
  };


 public:
  class ErrorHandler {
   public:
    virtual ~ErrorHandler();

    virtual void OnError(int line_no, const std::string &error_msg) = 0;
  };


 public:
  VersionScriptParser();

  void SetArch(const std::string &arch);

  void SetApiLevel(utils::ApiLevel api_level) {
    api_level_ = api_level;
  }

  void SetApiLevelMap(utils::ApiLevelMap api_level_map);

  void AddExcludedSymbolVersion(const std::string &version) {
    excluded_symbol_versions_.insert(version);
  }

  void AddExcludedSymbolTag(const std::string &tag) {
    excluded_symbol_tags_.insert(tag);
  }

  // Returns whether the argument is valid.
  bool AddModeTag(std::string_view mode_tag);

  void SetModeTagPolicy(ModeTagPolicy policy) { mode_tag_policy_ = policy; }

  void SetErrorHandler(std::unique_ptr<ErrorHandler> error_handler) {
    error_handler_ = std::move(error_handler);
  }

  std::unique_ptr<ExportedSymbolSet> Parse(std::istream &version_script_stream);


 private:
  bool ReadLine(std::string &line);

  bool ParseVersionBlock(bool ignore_symbols, const ParsedTags &tags);

  bool ParseSymbolLine(const std::string &line, bool is_cpp_symbol,
                       const ParsedTags &version_block_tags);

  ParsedTags ParseSymbolTags(const std::string &line,
                             const ParsedTags &initial_value);

  bool MatchModeTags(const ParsedTags &tags);

  bool MatchIntroducedTags(const ParsedTags &tags);

  bool IsSymbolExported(const ParsedTags &tags);


 private:
  void ReportError(const std::string &error_msg) {
    if (error_handler_) {
      error_handler_->OnError(line_no_, error_msg);
    }
  }


 private:
  std::unique_ptr<ErrorHandler> error_handler_;

  std::string arch_;
  std::string introduced_arch_tag_;
  utils::ApiLevel api_level_;
  utils::ApiLevelMap api_level_map_;

  utils::StringSet excluded_symbol_versions_;
  utils::StringSet excluded_symbol_tags_;
  ModeTagLevelMap included_mode_tags_;
  ModeTagPolicy mode_tag_policy_;

  std::istream *stream_;
  int line_no_;

  std::unique_ptr<ExportedSymbolSet> exported_symbols_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // VERSION_SCRIPT_PARSER_H_

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
#ifndef AST_UTIL
#define AST_UTIL

#include <clang/AST/AST.h>

#include <map>
#include <string>

namespace ast_util {

constexpr static char type_id_prefix[] = "type-";

struct ASTCaches {

  ASTCaches(const std::string &translation_unit_source)
      : translation_unit_source_(translation_unit_source) { };

  std::string GetTypeId(const std::string &qual_type) {
    auto type_id_it = qual_type_to_type_id_cache_.find(qual_type);
    if (type_id_it == qual_type_to_type_id_cache_.end()) {
      qual_type_to_type_id_cache_.insert(
          std::make_pair(qual_type, ++max_type_id_));
          return type_id_prefix + std::to_string(max_type_id_);
    }
    return type_id_prefix + std::to_string(type_id_it->second);
  }

  std::string translation_unit_source_;
  std::set<std::string> type_cache_;
  std::map<const clang::Decl *, std::string> decl_to_source_file_cache_;
  std::map<std::string, uint64_t> qual_type_to_type_id_cache_;
  uint64_t max_type_id_ = 0;
};

}
#endif

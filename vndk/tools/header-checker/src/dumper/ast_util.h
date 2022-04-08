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

#ifndef AST_UTIL_H_
#define AST_UTIL_H_

#include <clang/AST/AST.h>
#include <clang/AST/Type.h>

#include <llvm/ADT/DenseSet.h>

#include <map>
#include <string>


namespace header_checker {
namespace dumper {


struct ASTCaches {
  ASTCaches(const std::string &translation_unit_source)
      : translation_unit_source_(translation_unit_source) {}

  std::string translation_unit_source_;
  std::map<const clang::Decl *, std::string> decl_to_source_file_cache_;

  llvm::DenseSet<clang::QualType> converted_qual_types_;
};


}  // namespace dumper
}  // namespace header_checker


#endif  // AST_UTIL_H_

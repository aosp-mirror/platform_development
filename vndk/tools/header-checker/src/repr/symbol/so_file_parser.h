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

#ifndef SO_FILE_PARSER_H_
#define SO_FILE_PARSER_H_

#include "repr/ir_representation.h"
#include "repr/symbol/exported_symbol_set.h"

#include <memory>
#include <string>


namespace header_checker {
namespace repr {


class SoFileParser {
 public:
  static std::unique_ptr<SoFileParser> Create(const std::string &so_file_path);

  virtual ~SoFileParser() {}

  virtual std::unique_ptr<ExportedSymbolSet> Parse() = 0;
};


}  // namespace repr
}  // namespace header_checker


#endif  // SO_FILE_PARSER_H_

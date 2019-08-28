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

#include "command_line_utils.h"

#include <llvm/ADT/SmallVector.h>
#include <llvm/ADT/StringMap.h>
#include <llvm/Support/CommandLine.h>

#include <algorithm>

namespace header_checker {
namespace utils {


static bool IsOptionInCategory(const llvm::cl::Option &option,
                               const llvm::cl::OptionCategory &category) {
  const llvm::SmallVectorImpl<llvm::cl::OptionCategory *> &categories =
      option.Categories;
  auto iter = std::find(categories.begin(), categories.end(), &category);
  return iter != categories.end();
}


void HideIrrelevantCommandLineOptions(
    const llvm::cl::OptionCategory &category) {
  llvm::StringMap<llvm::cl::Option *> &map = llvm::cl::getRegisteredOptions();
  for (llvm::StringMapEntry<llvm::cl::Option *> &p : map) {
    if (IsOptionInCategory(*p.second, category)) {
      continue;
    }
    if (p.first().startswith("help")) {
      continue;
    }
    p.second->setHiddenFlag(llvm::cl::Hidden);
  }
}


}  // namespace utils
}  // namespace header_checker

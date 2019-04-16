// Copyright (C) 2016 The Android Open Source Project
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

#ifndef ABI_DIFF_WRAPPERS_H_
#define ABI_DIFF_WRAPPERS_H_

#include "repr/abi_diff_helpers.h"
#include "repr/ir_representation.h"


namespace header_checker {
namespace diff {


using repr::AbiDiffHelper;
using repr::AbiElementMap;
using repr::DiffStatus;


template <typename T, typename F>
static bool IgnoreSymbol(const T *element,
                         const std::set<std::string> &ignored_symbols,
                         F func) {
  return ignored_symbols.find(func(element)) != ignored_symbols.end();
}

template <typename T>
class DiffWrapper : public AbiDiffHelper {
 public:
  DiffWrapper(const T *oldp, const T *newp,
              repr::IRDiffDumper *ir_diff_dumper,
              const AbiElementMap<const repr::TypeIR *> &old_types,
              const AbiElementMap<const repr::TypeIR *> &new_types,
              const repr::DiffPolicyOptions &diff_policy_options,
              std::set<std::string> *type_cache)
      : AbiDiffHelper(old_types, new_types, diff_policy_options, type_cache,
                      ir_diff_dumper),
        oldp_(oldp), newp_(newp) {}

  bool DumpDiff(repr::IRDiffDumper::DiffKind diff_kind);

 private:
  const T *oldp_;
  const T *newp_;
};


}  // namespace diff
}  // namespace header_checker


#endif  // ABI_DIFF_WRAPPERS_H_

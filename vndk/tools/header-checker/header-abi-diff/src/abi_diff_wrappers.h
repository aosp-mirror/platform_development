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

#ifndef ABI_DIFF_WRAPPERS_H
#define ABI_DIFF_WRAPPERS_H

#include <abi_diff_helpers.h>
#include <ir_representation.h>

#include <deque>

namespace abi_diff_wrappers {

using abi_util::AbiElementMap;
using abi_util::AbiDiffHelper;
using abi_util::DiffStatus;

template <typename T, typename F>
static bool IgnoreSymbol(const T *element,
                         const std::set<std::string> &ignored_symbols,
                         F func) {
  return ignored_symbols.find(func(element)) !=
      ignored_symbols.end();
}

template <typename T>
class DiffWrapper : public AbiDiffHelper {

 public:
  DiffWrapper(const T *oldp, const T *newp,
              abi_util::IRDiffDumper *ir_diff_dumper,
              const AbiElementMap<const abi_util::TypeIR *> &old_types,
              const AbiElementMap<const abi_util::TypeIR *> &new_types,
              std::set<std::string> *type_cache)
      : AbiDiffHelper(old_types, new_types, type_cache, ir_diff_dumper),
        oldp_(oldp), newp_(newp) { }

  bool DumpDiff(abi_util::IRDiffDumper::DiffKind diff_kind);

 private:
  const T *oldp_;
  const T *newp_;
};

} // abi_diff_wrappers

#endif // ABI_DIFF_WRAPPERS_H

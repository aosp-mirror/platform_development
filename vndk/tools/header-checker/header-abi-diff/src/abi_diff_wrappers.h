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

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wnested-anon-types"
#include "proto/abi_dump.pb.h"
#include "proto/abi_diff.pb.h"
#pragma clang diagnostic pop

namespace abi_diff_wrappers {

template <typename T, typename TDiff>
class DiffWrapperBase {
 public:
  virtual std::unique_ptr<TDiff> Get() = 0 ;
 protected:
  DiffWrapperBase(const T *oldp, const T *newp)
      : oldp_(oldp), newp_(newp) { }
  template <typename Element, typename ElementDiff>
  bool GetElementDiffs(
      google::protobuf::RepeatedPtrField<ElementDiff> *dst,
      const google::protobuf::RepeatedPtrField<Element> &old_elements,
      const google::protobuf::RepeatedPtrField<Element> &new_elements);

 private:
  template <typename Element, typename ElementDiff>
  void GetExtraElementDiffs(
      google::protobuf::RepeatedPtrField<ElementDiff> *dst, int i, int j,
      const google::protobuf::RepeatedPtrField<Element> &old_elements,
      const google::protobuf::RepeatedPtrField<Element> &new_elements);

 protected:
  const T *oldp_;
  const T *newp_;
};

template <typename T, typename TDiff>
class DiffWrapper : public DiffWrapperBase<T, TDiff> {
 public:
  DiffWrapper(const T *oldp, const T *newp)
      : DiffWrapperBase<T, TDiff>(oldp, newp) { }

  std::unique_ptr<TDiff> Get() override;
};

} // abi_diff_wrappers



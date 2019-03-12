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

#ifndef FIXED_ARGV_H_
#define FIXED_ARGV_H_

#include <array>
#include <tuple>
#include <utility>
#include <vector>

#include <assert.h>
#include <string.h>


namespace header_checker {
namespace dumper {


class FixedArgvAccess;


class FixedArgv {
  friend FixedArgvAccess;

 private:
  std::vector<const char *> argv_;

 public:
  FixedArgv(int argc, const char **argv) : argv_(argv, argv + argc) {}

  int GetArgc() const {
    return argv_.size();
  }

  const char *const *GetArgv() const {
    return argv_.data();
  }

  void Resize(int argc) {
    assert(argc <= argv_.size());
    argv_.resize(argc);
  }

  template <typename... T>
  const char *GetLastArg(T&& ...options) const {
    std::array<const char *, sizeof...(options)> opts{
        std::forward<T&&>(options)...};
    for (std::vector<const char *>::const_reverse_iterator it = argv_.rbegin(),
         end = argv_.rend(); it != end; ++it) {
      for (const char *opt : opts) {
        if (::strcmp(*it, opt) == 0) {
          return opt;
        }
      }
    }
    return nullptr;
  }

  template <typename... T>
  bool IsLastArgEqualFirstOption(const char *expected, T&& ...others) const {
    const char *last = GetLastArg(expected, others...);
    // Since GetLastArg() returns the address in {expected, others...}, pointer
    // comparison is sufficient.
    return last == expected;
  }

  template<typename... T>
  void PushForwardArgs(T&& ...arguments) {
    std::array<const char *, sizeof...(arguments)> args{
        std::forward<T&&>(arguments)...};
    if (!GetLastArg("--")) {
      argv_.push_back("--");
    }
    argv_.insert(argv_.end(), args.begin(), args.end());
  }
};


class FixedArgvAccess {
 private:
  FixedArgv &fixed_argv_;

 public:
  int argc_;
  const char **argv_;

 public:
  explicit FixedArgvAccess(FixedArgv &fixed_argv)
      : fixed_argv_(fixed_argv), argc_(fixed_argv.GetArgc()),
        argv_(fixed_argv.argv_.data()) {
  }

  ~FixedArgvAccess() {
    fixed_argv_.Resize(argc_);
  }

 private:
  FixedArgvAccess(const FixedArgvAccess &) = delete;
  FixedArgvAccess& operator=(const FixedArgvAccess &rhs) = delete;
};


class FixedArgvRegistry {
 public:
  typedef void (Function)(FixedArgv &);

 private:
  static FixedArgvRegistry *head_;

  Function *func_;
  FixedArgvRegistry *next_;

 public:
  FixedArgvRegistry(Function *func) : func_(func), next_(head_) {
    head_ = this;
  }

  static void Apply(FixedArgv &fixed_argv) {
    for (FixedArgvRegistry *ptr = head_; ptr; ptr = ptr->next_) {
      ptr->func_(fixed_argv);
    }
  }
};


}  // namespace dumper
}  // namespace header_checker


#endif  // FIXED_ARGV_H_

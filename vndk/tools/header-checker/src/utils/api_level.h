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

#ifndef API_LEVEL_H_
#define API_LEVEL_H_

#include <llvm/ADT/Optional.h>

#include <string>


namespace header_checker {
namespace utils {


using ApiLevel = int;


constexpr ApiLevel FUTURE_API_LEVEL = 10000;


llvm::Optional<ApiLevel> ParseApiLevel(const std::string &api);


}  // namespace utils
}  // namespace header_checker


#endif  // API_LEVEL_H_

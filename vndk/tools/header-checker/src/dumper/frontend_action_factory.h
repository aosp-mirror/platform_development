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

#ifndef FRONTEND_ACTION_FACTORY_H_
#define FRONTEND_ACTION_FACTORY_H_

#include "dumper/frontend_action.h"
#include "repr/ir_representation.h"

#include <clang/Tooling/Tooling.h>

#include <vector>


namespace header_checker {
namespace dumper {


class HeaderCheckerFrontendActionFactory
    : public clang::tooling::FrontendActionFactory {
 private:
  HeaderCheckerOptions &options_;

 public:
  HeaderCheckerFrontendActionFactory(HeaderCheckerOptions &options);

  std::unique_ptr<clang::FrontendAction> create() override;
};


}  // namespace dumper
}  // namespace header_checker


#endif  // FRONTEND_ACTION_FACTORY_H_

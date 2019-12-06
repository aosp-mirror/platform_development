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

#include "dumper/frontend_action_factory.h"

#include "dumper/frontend_action.h"

#include <clang/Frontend/FrontendActions.h>

#include <utility>


namespace header_checker {
namespace dumper {


HeaderCheckerFrontendActionFactory::HeaderCheckerFrontendActionFactory(
    HeaderCheckerOptions &options)
    : options_(options) {}

std::unique_ptr<clang::FrontendAction>
HeaderCheckerFrontendActionFactory::create() {
  return std::make_unique<HeaderCheckerFrontendAction>(options_);
}


}  // dumper
}  // header_checker

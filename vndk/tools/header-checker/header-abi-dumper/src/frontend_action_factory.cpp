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

#include "frontend_action_factory.h"

#include "frontend_action.h"

#include <clang/Frontend/FrontendActions.h>

HeaderCheckerFrontendActionFactory::HeaderCheckerFrontendActionFactory(
    const std::string &dump_name,
    const std::set<std::string> &exported_headers)
  : dump_name_(dump_name), exported_headers_(exported_headers) { }

clang::FrontendAction *HeaderCheckerFrontendActionFactory::create() {
  return new HeaderCheckerFrontendAction(dump_name_, exported_headers_);
}

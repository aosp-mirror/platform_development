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

#include <clang/Tooling/Tooling.h>

class HeaderCheckerFrontendActionFactory
    : public clang::tooling::FrontendActionFactory {
 private:
  std::string ref_dump_name_;
  bool should_generate_ref_dump_;

 public:
  HeaderCheckerFrontendActionFactory(const std::string &ref_dump_name,
                                     bool should_generate_ref_dump);

  clang::FrontendAction *create() override;
};

#endif  // FRONTEND_ACTION_FACTORY_H_

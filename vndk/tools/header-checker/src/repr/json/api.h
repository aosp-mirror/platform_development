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

#ifndef HEADER_CHECKER_REPR_JSON_API_H_
#define HEADER_CHECKER_REPR_JSON_API_H_

#include <memory>
#include <string>


namespace header_checker {
namespace repr {


class IRDumper;
class IRReader;
class ModuleIR;

std::unique_ptr<IRDumper> CreateJsonIRDumper(const std::string &dump_path);

std::unique_ptr<IRReader> CreateJsonIRReader(
    std::unique_ptr<ModuleIR> module_ir);


}  // namespace repr
}  // namespace header_checker


#endif  // HEADER_CHECKER_REPR_JSON_API_H_

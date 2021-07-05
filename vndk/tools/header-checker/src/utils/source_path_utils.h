// Copyright (C) 2021 The Android Open Source Project
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

#ifndef SOURCE_PATH_UTILS_H_
#define SOURCE_PATH_UTILS_H_

#include <set>
#include <string>
#include <vector>


namespace header_checker {
namespace utils {


struct RootDir {
  std::string path;
  std::string replacement;

  RootDir(std::string p, std::string r)
      : path(std::move(p)), replacement(std::move(r)) {}
};

typedef std::vector<RootDir> RootDirs;

RootDirs ParseRootDirs(const std::vector<std::string> &args);

// Resolve '..' and '.'; if the path starts with any of root_dirs, replace the
// prefix; don't resolve symbolic links.
std::string NormalizePath(std::string_view path, const RootDirs &root_dirs);

std::set<std::string>
CollectAllExportedHeaders(const std::vector<std::string> &exported_header_dirs,
                          const RootDirs &root_dirs);


}  // namespace utils
}  // namespace header_checker


#endif  // SOURCE_PATH_UTILS_H_

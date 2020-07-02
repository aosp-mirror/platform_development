// Copyright (C) 2020 The Android Open Source Project
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

#include "utils/header_abi_util.h"

#include <gtest/gtest.h>


namespace header_checker {
namespace utils {


TEST(CollectExportedHeadersTest, NormalizeAbsolutePaths) {
  const std::string root = "/root/dir";
  EXPECT_EQ("", NormalizePath(root, root));
  EXPECT_EQ("/unit/test", NormalizePath("/unit/test", root));
  EXPECT_EQ("/root/unit/test", NormalizePath(root + "/../unit/test", root));
}


TEST(CollectExportedHeadersTest, NormalizeCwdPaths) {
  const std::string cwd = GetCwd();
  ASSERT_NE("", cwd);
  EXPECT_EQ("", NormalizePath("", cwd));
  EXPECT_EQ("unit/test", NormalizePath("./unit/test/.", cwd));
  EXPECT_EQ("unit/test", NormalizePath("unit//test//", cwd));
  EXPECT_EQ("test", NormalizePath("unit/../test", cwd));
  EXPECT_EQ("unit/test", NormalizePath(cwd + "/unit/test", cwd));
  EXPECT_EQ('/', NormalizePath("../unit/test", cwd)[0]);
}


}  // namespace utils
}  // namespace header_checker

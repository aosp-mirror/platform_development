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

#include "utils/source_path_utils.h"

#include <android-base/file.h>
#include <gtest/gtest.h>

#include <filesystem>
#include <vector>

namespace header_checker {
namespace utils {

TEST(SourcePathUtilsTest, CollectAllExportedHeaders) {
  TemporaryDir temp_dir;
  std::error_code ec;
  // Prepare a header directory containing links, hidden files, etc.
  const std::filesystem::path header_dir =
      std::filesystem::path(temp_dir.path) / "include";
  ASSERT_TRUE(std::filesystem::create_directory(header_dir, ec));
  ASSERT_FALSE(ec);

  const std::filesystem::path header = header_dir / "header.h";
  ASSERT_TRUE(android::base::WriteStringToFile("// test", header));

  const std::filesystem::path no_ext_header = header_dir / "header";
  ASSERT_TRUE(android::base::WriteStringToFile("// test", no_ext_header));

  const std::filesystem::path subdir = header_dir / "subdir";
  ASSERT_TRUE(std::filesystem::create_directory(subdir, ec));
  ASSERT_FALSE(ec);

  const std::filesystem::path subdir_link = header_dir / "subdir_link";
  std::filesystem::create_directory_symlink(subdir, subdir_link, ec);
  ASSERT_FALSE(ec);

  const std::filesystem::path hidden_subdir_link = header_dir / ".subdir_link";
  std::filesystem::create_directory_symlink(subdir, hidden_subdir_link, ec);
  ASSERT_FALSE(ec);

  const std::filesystem::path header_link = subdir / "header_link.h";
  std::filesystem::create_symlink(header, header_link, ec);
  ASSERT_FALSE(ec);

  const std::filesystem::path hidden_header_link = subdir / ".header_link.h";
  std::filesystem::create_symlink(header, hidden_header_link, ec);
  ASSERT_FALSE(ec);

  const std::filesystem::path non_header_link = subdir / "header_link.txt";
  std::filesystem::create_symlink(header, non_header_link, ec);
  ASSERT_FALSE(ec);
  // Prepare a header directory like libc++.
  const std::filesystem::path libcxx_dir =
      std::filesystem::path(temp_dir.path) / "libcxx" / "include";
  ASSERT_TRUE(std::filesystem::create_directories(libcxx_dir, ec));
  ASSERT_FALSE(ec);

  const std::filesystem::path libcxx_header = libcxx_dir / "array";
  ASSERT_TRUE(android::base::WriteStringToFile("// test", libcxx_header));
  // Test the function.
  std::vector<std::string> exported_header_dirs{header_dir, libcxx_dir};
  std::vector<RootDir> root_dirs{{header_dir, "include"},
                                 {libcxx_dir, "libcxx"}};
  std::set<std::string> headers =
      CollectAllExportedHeaders(exported_header_dirs, root_dirs);

  std::set<std::string> expected_headers{
      "include/header.h", "include/subdir/header_link.h",
      "include/subdir_link/header_link.h", "libcxx/array"};
  ASSERT_EQ(headers, expected_headers);
}

TEST(SourcePathUtilsTest, NormalizeAbsolutePaths) {
  const std::vector<std::string> args{"/root/dir"};
  const RootDirs root_dirs = ParseRootDirs(args);
  ASSERT_EQ(1, root_dirs.size());
  ASSERT_EQ("/root/dir", root_dirs[0].path);
  ASSERT_EQ("", root_dirs[0].replacement);

  EXPECT_EQ("", NormalizePath("/root/dir", root_dirs));
  EXPECT_EQ("test", NormalizePath("/root/dir/test", root_dirs));
  EXPECT_EQ("/root/unit/test",
            NormalizePath("/root/dir/../unit/test", root_dirs));
}


TEST(SourcePathUtilsTest, NormalizeCwdPaths) {
  const RootDirs cwd = ParseRootDirs(std::vector<std::string>());
  ASSERT_EQ(1, cwd.size());
  ASSERT_NE("", cwd[0].path);
  ASSERT_EQ("", cwd[0].replacement);

  EXPECT_EQ("", NormalizePath("", cwd));
  EXPECT_EQ("unit/test", NormalizePath("./unit/test/.", cwd));
  EXPECT_EQ("unit/test", NormalizePath("unit//test//", cwd));
  EXPECT_EQ("test", NormalizePath("unit/../test", cwd));
  EXPECT_EQ("unit/test", NormalizePath(cwd[0].path + "/unit/test", cwd));
  EXPECT_EQ('/', NormalizePath("../unit/test", cwd)[0]);
}


TEST(SourcePathUtilsTest, NormalizePathsWithMultipleRootDirs) {
  const std::vector<std::string> args{"/before:/", "/before/dir:after"};
  const RootDirs root_dirs = ParseRootDirs(args);
  ASSERT_EQ(2, root_dirs.size());
  ASSERT_EQ("/before/dir", root_dirs[0].path);
  ASSERT_EQ("after", root_dirs[0].replacement);
  ASSERT_EQ("/before", root_dirs[1].path);
  ASSERT_EQ("/", root_dirs[1].replacement);

  EXPECT_EQ("/directory", NormalizePath("/before/directory", root_dirs));
  EXPECT_EQ("after", NormalizePath("/before/dir", root_dirs));
}


TEST(SourcePathUtilsTest, NormalizeRelativePaths) {
  const std::vector<std::string> args{"../before/.:..//after/."};
  const RootDirs root_dirs = ParseRootDirs(args);
  ASSERT_EQ(1, root_dirs.size());
  ASSERT_EQ('/', root_dirs[0].path[0]);
  ASSERT_EQ("../after", root_dirs[0].replacement);

  EXPECT_EQ("../after", NormalizePath("../before", root_dirs));
}


}  // namespace utils
}  // namespace header_checker

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

#include "utils/string_utils.h"

#include <gtest/gtest.h>


namespace header_checker {
namespace utils {


TEST(StringUtilsTest, Trim) {
  EXPECT_EQ("a b", Trim(" a b "));
  EXPECT_EQ("a b", Trim(" a b"));
  EXPECT_EQ("a b", Trim("a b "));
  EXPECT_EQ("a b", Trim("a b"));
  EXPECT_EQ("a b", Trim("\ta b\n"));
}


TEST(StringUtilsTest, StartsWith) {
  EXPECT_TRUE(StartsWith("abcd", "ab"));
  EXPECT_TRUE(StartsWith("a", "a"));
  EXPECT_TRUE(StartsWith("a", ""));
  EXPECT_TRUE(StartsWith("", ""));

  EXPECT_FALSE(StartsWith("ab", "abcd"));
  EXPECT_FALSE(StartsWith("", "ab"));
}


TEST(StringUtilsTest, EndsWith) {
  EXPECT_TRUE(EndsWith("abcd", "cd"));
  EXPECT_TRUE(EndsWith("d", "d"));
  EXPECT_TRUE(EndsWith("d", ""));
  EXPECT_TRUE(EndsWith("", ""));

  EXPECT_FALSE(EndsWith("cd", "abcd"));
  EXPECT_FALSE(EndsWith("", "cd"));
}


TEST(StringUtilsTest, Split) {
  std::vector<std::string_view> xs;

  xs = Split("   a  bb   ccc ", " ");
  EXPECT_EQ(3, xs.size());
  EXPECT_EQ("a", xs[0]);
  EXPECT_EQ("bb", xs[1]);
  EXPECT_EQ("ccc", xs[2]);

  xs = Split("a", " ");
  EXPECT_EQ(1, xs.size());
  EXPECT_EQ("a", xs[0]);

  xs = Split("a b", " ");
  EXPECT_EQ(2, xs.size());
  EXPECT_EQ("a", xs[0]);
  EXPECT_EQ("b", xs[1]);

  xs = Split("a \t \t \tb", " \t");
  EXPECT_EQ(2, xs.size());
  EXPECT_EQ("a", xs[0]);
  EXPECT_EQ("b", xs[1]);
}


TEST(StringUtilsTest, ParseInt) {
  EXPECT_FALSE(ParseInt(""));
  EXPECT_FALSE(ParseInt("a"));
  EXPECT_FALSE(ParseInt("0xa"));
  EXPECT_FALSE(ParseInt("16h"));

  EXPECT_TRUE(ParseInt("0").has_value());
  EXPECT_EQ(0, ParseInt("0").value());

  EXPECT_TRUE(ParseInt("16").has_value());
  EXPECT_EQ(16, ParseInt("16").value());

  EXPECT_TRUE(ParseInt("-16").has_value());
  EXPECT_EQ(-16, ParseInt("-16").value());
}


TEST(StringUtilsTest, ParseBool) {
  EXPECT_FALSE(ParseBool(""));
  EXPECT_FALSE(ParseBool("false"));
  EXPECT_FALSE(ParseBool("off"));
  EXPECT_FALSE(ParseBool("0"));

  EXPECT_TRUE(ParseBool("TRUE"));
  EXPECT_TRUE(ParseBool("True"));
  EXPECT_TRUE(ParseBool("true"));
  EXPECT_TRUE(ParseBool("ON"));
  EXPECT_TRUE(ParseBool("1"));
}


TEST(StringUtilsTest, IsGlobPattern) {
  EXPECT_TRUE(IsGlobPattern("*.so"));
  EXPECT_TRUE(IsGlobPattern("[ab].txt"));
  EXPECT_TRUE(IsGlobPattern("?.txt"));

  EXPECT_FALSE(IsGlobPattern("name"));
  EXPECT_FALSE(IsGlobPattern(".txt"));
  EXPECT_FALSE(IsGlobPattern(""));
}


}  // namespace utils
}  // namespace header_checker

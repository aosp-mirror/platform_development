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

#include "utils/config_file.h"

#include <sstream>

#include <gtest/gtest.h>


namespace header_checker {
namespace utils {


TEST(ConfigParserTest, Parse) {
  std::stringstream stream(R"(
# Comment line starts with hash symbol
; Comment line starts with semicolon

[section1]
key1 = value1
key2 = value2

[section2]
key1 = true
key2 = false
)");

  auto &&cfg = ConfigParser::ParseFile(stream);
  EXPECT_TRUE(cfg.HasSection("section1"));
  EXPECT_TRUE(cfg.HasSection("section2"));
  EXPECT_FALSE(cfg.HasSection("section3"));

  auto &&section1 = cfg.GetSection("section1");
  EXPECT_TRUE(section1.HasProperty("key1"));
  EXPECT_EQ("value1", section1.GetProperty("key1"));
  EXPECT_TRUE(section1.HasProperty("key2"));
  EXPECT_EQ("value2", section1.GetProperty("key2"));

  EXPECT_FALSE(section1.HasProperty("key3"));
  EXPECT_EQ("", section1.GetProperty("key3"));

  auto &&section2 = cfg.GetSection("section2");
  EXPECT_TRUE(section2.HasProperty("key1"));
  EXPECT_EQ("true", section2.GetProperty("key1"));
  EXPECT_TRUE(section2.HasProperty("key2"));
  EXPECT_EQ("false", section2.GetProperty("key2"));

  EXPECT_EQ("value1", cfg.GetProperty("section1", "key1"));
  EXPECT_EQ("value2", cfg.GetProperty("section1", "key2"));

  EXPECT_EQ("value1", cfg["section1"]["key1"]);
  EXPECT_EQ("value2", cfg["section1"]["key2"]);
}


TEST(ConfigParserTest, BadSectionNameLine) {
  std::stringstream stream(R"(
[section1
key1 = value1
)");

  size_t num_errors = 0;

  ConfigParser parser(stream);
  parser.SetErrorListener(
      [&num_errors](size_t line_no, const char *cause) {
        ++num_errors;
        EXPECT_EQ(2, line_no);
        EXPECT_STREQ("bad section name line", cause);
      });
  parser.ParseFile();

  EXPECT_EQ(1, num_errors);
}


TEST(ConfigParserTest, BadKeyValueLine) {
  std::stringstream stream(R"(
[section1]
key1
)");

  size_t num_errors = 0;

  ConfigParser parser(stream);
  parser.SetErrorListener(
      [&num_errors](size_t line_no, const char *cause) {
        ++num_errors;
        EXPECT_EQ(3, line_no);
        EXPECT_STREQ("bad key-value line", cause);
      });
  parser.ParseFile();

  EXPECT_EQ(1, num_errors);
}


}  // namespace utils
}  // namespace header_checker

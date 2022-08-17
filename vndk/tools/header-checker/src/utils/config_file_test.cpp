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


TEST(ConfigFileTest, Parse) {
  std::stringstream stream(R"(
// Comment line starts with slash
/* embedded comment */
{
  "section1": {
    "flags": {
      "key1": true,
      "key2": false,
    },
  },
  "section2": {
    "flags": {
      "key1": true,
      "key2": false,
    },
  },
}
)");

  ConfigFile cfg;
  cfg.Load(stream);
  EXPECT_TRUE(cfg.HasSection("section1"));
  EXPECT_TRUE(cfg.HasSection("section2"));
  EXPECT_FALSE(cfg.HasSection("section3"));

  auto &&section1 = cfg.GetSection("section1");
  EXPECT_TRUE(section1.HasProperty("key1"));
  EXPECT_TRUE(section1.GetProperty("key1"));
  EXPECT_TRUE(section1.HasProperty("key2"));
  EXPECT_FALSE(section1.GetProperty("key2"));

  EXPECT_FALSE(section1.HasProperty("key3"));
  EXPECT_FALSE(section1.GetProperty("key3"));

  auto &&section2 = cfg.GetSection("section2");
  EXPECT_TRUE(section2.HasProperty("key1"));
  EXPECT_TRUE(section2.GetProperty("key1"));
  EXPECT_TRUE(section2.HasProperty("key2"));
  EXPECT_FALSE(section2.GetProperty("key2"));

  EXPECT_TRUE(cfg.GetProperty("section1", "key1"));
  EXPECT_FALSE(cfg.GetProperty("section1", "key2"));

  EXPECT_TRUE(cfg["section1"]["key1"]);
  EXPECT_FALSE(cfg["section1"]["key2"]);
}

TEST(ConfigFileTest, BadJsonFormat) {
  std::stringstream stream(R"(
{
  "section1: {
  }
}
)");

  ConfigFile cfg;
  bool result = cfg.Load(stream);

  EXPECT_FALSE(result);
}


}  // namespace utils
}  // namespace header_checker

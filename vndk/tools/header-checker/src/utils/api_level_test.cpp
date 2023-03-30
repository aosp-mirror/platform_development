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

#include "utils/api_level.h"

#include <gtest/gtest.h>


namespace header_checker {
namespace utils {


TEST(ApiLevelTest, ParseApiLevel) {
  EXPECT_FALSE(ParseApiLevel(""));
  EXPECT_FALSE(ParseApiLevel("A"));

  EXPECT_TRUE(ParseApiLevel("current").hasValue());
  EXPECT_EQ(FUTURE_API_LEVEL, ParseApiLevel("current").getValue());

  EXPECT_TRUE(ParseApiLevel("16").hasValue());
  EXPECT_EQ(16l, ParseApiLevel("16").getValue());
}


}  // namespace utils
}  // namespace header_checker

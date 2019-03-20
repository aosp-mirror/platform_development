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

#include "repr/symbol/version_script_parser.h"

#include <gtest/gtest.h>

#include <map>
#include <sstream>
#include <string>


namespace header_checker {
namespace repr {


TEST(VersionScriptParserTest, SmokeTest) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        foo1;
        bar1;  # var
      local:
        *;
    };

    LIBEX_2.0 {
      global:
        foo2;
        bar2;  # var
    } LIBEX_1.0;
  )TESTDATA";

  VersionScriptParser parser;

  std::istringstream stream(testdata);
  std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
  ASSERT_TRUE(result);

  const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();
  EXPECT_NE(funcs.end(), funcs.find("foo1"));
  EXPECT_NE(funcs.end(), funcs.find("foo2"));
  EXPECT_EQ(funcs.end(), funcs.find("bar1"));
  EXPECT_EQ(funcs.end(), funcs.find("bar2"));

  const ExportedSymbolSet::VarMap &vars = result->GetVars();
  EXPECT_NE(vars.end(), vars.find("bar1"));
  EXPECT_NE(vars.end(), vars.find("bar2"));
  EXPECT_EQ(vars.end(), vars.find("foo1"));
  EXPECT_EQ(vars.end(), vars.find("foo2"));
}


TEST(VersionScriptParserTest, ExcludeSymbolVersions) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        foo1;
        bar1;  # var
      local:
        *;
    };

    LIBEX_PRIVATE {
      global:
        foo2;
        bar2;  # var
    } LIBEX_1.0;
  )TESTDATA";

  // excluded_symbol_versions = {}
  {
    VersionScriptParser parser;

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();
    EXPECT_NE(funcs.end(), funcs.find("foo2"));

    const ExportedSymbolSet::VarMap &vars = result->GetVars();
    EXPECT_NE(vars.end(), vars.find("bar2"));
  }

  // excluded_symbol_versions = {"LIBEX_PRIVATE"}
  {
    VersionScriptParser parser;
    parser.AddExcludedSymbolVersion("LIBEX_PRIVATE");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();
    EXPECT_EQ(funcs.end(), funcs.find("foo2"));

    const ExportedSymbolSet::VarMap &vars = result->GetVars();
    EXPECT_EQ(vars.end(), vars.find("bar2"));
  }
}


TEST(VersionScriptParserTest, VisibilityLabels) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        global_f1;
        global_v1;  # var
      local:
        local_f2;
        local_v2;  # var
      global:
        global_f3;
        global_v3;  # var
      global:
        global_f4;
        global_v4;  # var
      local:
        local_f5;
        local_v5;  # var
      local:
        local_f6;
        local_v6;  # var
    };
  )TESTDATA";

  VersionScriptParser parser;

  std::istringstream stream(testdata);
  std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
  ASSERT_TRUE(result);

  const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

  EXPECT_NE(funcs.end(), funcs.find("global_f1"));
  EXPECT_NE(funcs.end(), funcs.find("global_f3"));
  EXPECT_NE(funcs.end(), funcs.find("global_f4"));

  EXPECT_EQ(funcs.end(), funcs.find("local_f2"));
  EXPECT_EQ(funcs.end(), funcs.find("local_f5"));
  EXPECT_EQ(funcs.end(), funcs.find("local_f6"));

  const ExportedSymbolSet::VarMap &vars = result->GetVars();

  EXPECT_NE(vars.end(), vars.find("global_v1"));
  EXPECT_NE(vars.end(), vars.find("global_v3"));
  EXPECT_NE(vars.end(), vars.find("global_v4"));

  EXPECT_EQ(vars.end(), vars.find("local_v2"));
  EXPECT_EQ(vars.end(), vars.find("local_v5"));
  EXPECT_EQ(vars.end(), vars.find("local_v6"));
}


TEST(VersionScriptParserTest, ParseSymbolTagsIntroduced) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        test1;  # introduced=19
        test2;  # introduced=19 introduced-arm64=20
        test3;  # introduced-arm64=20 introduced=19
        test4;  # future
    };
  )TESTDATA";

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(18);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_EQ(funcs.end(), funcs.find("test1"));
    EXPECT_EQ(funcs.end(), funcs.find("test2"));
    EXPECT_EQ(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(19);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_EQ(funcs.end(), funcs.find("test2"));
    EXPECT_EQ(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm");
    parser.SetApiLevel(19);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
    EXPECT_NE(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(20);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
    EXPECT_NE(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(utils::FUTURE_API_LEVEL);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
    EXPECT_NE(funcs.end(), funcs.find("test3"));
    EXPECT_NE(funcs.end(), funcs.find("test4"));
  }
}


TEST(VersionScriptParserTest, ParseSymbolTagsArch) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        test1;
        test2;  # arm arm64
        test3;  # arm64
        test4;  # mips
    };
  )TESTDATA";

  {
    VersionScriptParser parser;
    parser.SetArch("arm");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
    EXPECT_EQ(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
    EXPECT_NE(funcs.end(), funcs.find("test3"));
    EXPECT_EQ(funcs.end(), funcs.find("test4"));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("mips");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_EQ(funcs.end(), funcs.find("test2"));
    EXPECT_EQ(funcs.end(), funcs.find("test3"));
    EXPECT_NE(funcs.end(), funcs.find("test4"));
  }
}


TEST(VersionScriptParserTest, ExcludeSymbolTags) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        test1;
        test2;  # exclude-tag
    };
  )TESTDATA";

  // exclude_symbol_tags = {}
  {
    VersionScriptParser parser;

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_NE(funcs.end(), funcs.find("test2"));
  }

  // exclude_symbol_tags = {"exclude-tag"}
  {
    VersionScriptParser parser;
    parser.AddExcludedSymbolTag("exclude-tag");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_NE(funcs.end(), funcs.find("test1"));
    EXPECT_EQ(funcs.end(), funcs.find("test2"));
  }
}


TEST(VersionScriptParserTest, ParseExternCpp) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        test1;
        extern "C++" {
          Test2::test();
          Test3::test();
          Test4::*;
        };
        test5;
    };
  )TESTDATA";

  VersionScriptParser parser;

  std::istringstream stream(testdata);
  std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
  ASSERT_TRUE(result);

  const ExportedSymbolSet::NameSet &cpp_symbols =
      result->GetDemangledCppSymbols();

  EXPECT_NE(cpp_symbols.end(), cpp_symbols.find("Test2::test()"));
  EXPECT_NE(cpp_symbols.end(), cpp_symbols.find("Test3::test()"));

  EXPECT_EQ(cpp_symbols.end(), cpp_symbols.find("test1"));
  EXPECT_EQ(cpp_symbols.end(), cpp_symbols.find("test4"));

  const ExportedSymbolSet::GlobPatternSet &cpp_glob_patterns =
      result->GetDemangledCppGlobPatterns();

  EXPECT_NE(cpp_glob_patterns.end(), cpp_glob_patterns.find("Test4::*"));
}


TEST(VersionScriptParserTest, ParseGlobPattern) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        test1*;
        test2[Aa];
        test3?;
        test4;
    };
  )TESTDATA";


  VersionScriptParser parser;

  std::istringstream stream(testdata);
  std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
  ASSERT_TRUE(result);

  const ExportedSymbolSet::GlobPatternSet &glob_patterns =
      result->GetGlobPatterns();

  EXPECT_NE(glob_patterns.end(), glob_patterns.find("test1*"));
  EXPECT_NE(glob_patterns.end(), glob_patterns.find("test2[Aa]"));
  EXPECT_NE(glob_patterns.end(), glob_patterns.find("test3?"));

  EXPECT_EQ(glob_patterns.end(), glob_patterns.find("test4"));
}


}  // namespace repr
}  // namespace header_checker

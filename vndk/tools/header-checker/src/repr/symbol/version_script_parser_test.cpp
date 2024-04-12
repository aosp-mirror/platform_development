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

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <map>
#include <sstream>
#include <string>


namespace header_checker {
namespace repr {


using testing::ElementsAre;
using testing::Key;


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
  EXPECT_THAT(funcs, ElementsAre(Key("foo1"), Key("foo2")));

  const ExportedSymbolSet::VarMap &vars = result->GetVars();
  EXPECT_THAT(vars, ElementsAre(Key("bar1"), Key("bar2")));
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
    EXPECT_THAT(funcs, ElementsAre(Key("foo1"), Key("foo2")));

    const ExportedSymbolSet::VarMap &vars = result->GetVars();
    EXPECT_THAT(vars, ElementsAre(Key("bar1"), Key("bar2")));
  }

  {
    VersionScriptParser parser;
    parser.AddExcludedSymbolVersion("*_PRIVATE");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();
    EXPECT_THAT(funcs, ElementsAre(Key("foo1")));

    const ExportedSymbolSet::VarMap &vars = result->GetVars();
    EXPECT_THAT(vars, ElementsAre(Key("bar1")));
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

  EXPECT_THAT(
      funcs, ElementsAre(Key("global_f1"), Key("global_f3"), Key("global_f4")));

  const ExportedSymbolSet::VarMap &vars = result->GetVars();

  EXPECT_THAT(
      vars, ElementsAre(Key("global_v1"), Key("global_v3"), Key("global_v4")));
}


TEST(VersionScriptParserTest, ParseSymbolTagsIntroduced) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {  # introduced=18
      global:
        test1;  # introduced=19
        test2;  # introduced=19 introduced-arm64=20
        test3;  # introduced-arm64=20 introduced=19
        test4;  # future
        test5;  # introduced=17
    };
  )TESTDATA";

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(17);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();
    // This may be an undefined behavior. ndkstubgen includes it in llndk mode,
    // but excludes it in ndk mode.
    EXPECT_THAT(funcs, ElementsAre(Key("test5")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(18);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test5")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(19);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test5")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm");
    parser.SetApiLevel(19);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2"), Key("test3"),
                                   Key("test5")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(20);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2"), Key("test3"),
                                   Key("test5")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");
    parser.SetApiLevel(utils::FUTURE_API_LEVEL);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2"), Key("test3"),
                                   Key("test4"), Key("test5")));
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

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("arm64");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2"), Key("test3")));
  }

  {
    VersionScriptParser parser;
    parser.SetArch("mips");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test4")));
  }
}


TEST(VersionScriptParserTest, ExcludeSymbolTags) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {  # exclude-tag-1
      global:
        test1;
        test2;  # exclude-tag-2
    };
  )TESTDATA";

  // exclude_symbol_tags = {}
  {
    VersionScriptParser parser;

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1"), Key("test2")));
  }

  {
    VersionScriptParser parser;
    parser.AddExcludedSymbolTag("exclude-tag-1");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_TRUE(funcs.empty());
  }

  {
    VersionScriptParser parser;
    parser.AddExcludedSymbolTag("exclude-tag-2");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("test1")));
  }
}


TEST(VersionScriptParserTest, IncludeSymbolTags) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {
      global:
        always;  # unknown
        api34;  # introduced=34
        api35;  # introduced=35
        llndk202404;  # llndk=202404
        llndk202504;  # llndk=202504
        systemapi;  # systemapi
        systemapi_llndk;  # systemapi llndk
    };
  )TESTDATA";

  {
    VersionScriptParser parser;
    parser.SetApiLevel(34);
    parser.AddModeTag("llndk=202404");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("always"), Key("api34"),
                                   Key("llndk202404"), Key("systemapi_llndk")));
  }

  {
    VersionScriptParser parser;
    parser.SetApiLevel(34);
    parser.AddModeTag("llndk");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs,
                ElementsAre(Key("always"), Key("api34"), Key("llndk202404"),
                            Key("llndk202504"), Key("systemapi_llndk")));
  }

  // Include all mode tags
  {
    VersionScriptParser parser;
    parser.SetApiLevel(utils::FUTURE_API_LEVEL);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("always"), Key("api34"), Key("api35"),
                                   Key("llndk202404"), Key("llndk202504"),
                                   Key("systemapi"), Key("systemapi_llndk")));
  }

  // Exclude all mode tags
  {
    VersionScriptParser parser;
    parser.SetApiLevel(utils::FUTURE_API_LEVEL);
    parser.AddModeTag("none");

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("always"), Key("api34"), Key("api35")));
  }
}


TEST(VersionScriptParserTest, SetModeTagPolicy) {
  static const char testdata[] = R"TESTDATA(
    LIBEX_1.0 {  # introduced=36
        api36;
        api36_llndk202504;
        api36_llndk202504;  # llndk=202504
        llndk202504;  # llndk=202504
    };
  )TESTDATA";

  {
    VersionScriptParser parser;
    parser.SetApiLevel(35);
    parser.AddModeTag("llndk=202504");
    parser.SetModeTagPolicy(ModeTagPolicy::MatchTagAndApi);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_TRUE(funcs.empty());
  }

  {
    VersionScriptParser parser;
    parser.SetApiLevel(35);
    parser.AddModeTag("llndk=202504");
    parser.SetModeTagPolicy(ModeTagPolicy::MatchTagOnly);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs,
                ElementsAre(Key("api36_llndk202504"), Key("llndk202504")));
  }

  {
    VersionScriptParser parser;
    parser.SetApiLevel(36);
    parser.SetModeTagPolicy(ModeTagPolicy::MatchTagAndApi);

    std::istringstream stream(testdata);
    std::unique_ptr<ExportedSymbolSet> result(parser.Parse(stream));
    ASSERT_TRUE(result);

    const ExportedSymbolSet::FunctionMap &funcs = result->GetFunctions();

    EXPECT_THAT(funcs, ElementsAre(Key("api36"), Key("api36_llndk202504"),
                                   Key("llndk202504")));
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

  EXPECT_THAT(cpp_symbols, ElementsAre("Test2::test()", "Test3::test()"));

  const ExportedSymbolSet::GlobPatternSet &cpp_glob_patterns =
      result->GetDemangledCppGlobPatterns();

  EXPECT_THAT(cpp_glob_patterns, ElementsAre("Test4::*"));
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

  EXPECT_THAT(glob_patterns, ElementsAre("test1*", "test2[Aa]", "test3?"));
}


}  // namespace repr
}  // namespace header_checker

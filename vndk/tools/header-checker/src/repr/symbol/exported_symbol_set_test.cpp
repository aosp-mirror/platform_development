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

#include "repr/symbol/exported_symbol_set.h"

#include "repr/ir_representation.h"

#include <gtest/gtest.h>


namespace header_checker {
namespace repr {


TEST(ExportedSymbolSetTest, AddFunction) {
  ExportedSymbolSet symbols;
  symbols.AddFunction("global", ElfSymbolIR::ElfSymbolBinding::Global);
  symbols.AddFunction("weak", ElfSymbolIR::ElfSymbolBinding::Weak);

  const ExportedSymbolSet::FunctionMap &funcs = symbols.GetFunctions();

  ASSERT_NE(funcs.end(), funcs.find("global"));
  EXPECT_EQ(ElfSymbolIR::ElfSymbolBinding::Global,
            funcs.at("global").GetBinding());

  ASSERT_NE(funcs.end(), funcs.find("weak"));
  EXPECT_EQ(ElfSymbolIR::ElfSymbolBinding::Weak,
            funcs.at("weak").GetBinding());
}


TEST(ExportedSymbolSetTest, AddVar) {
  ExportedSymbolSet symbols;
  symbols.AddVar("global", ElfSymbolIR::ElfSymbolBinding::Global);
  symbols.AddVar("weak", ElfSymbolIR::ElfSymbolBinding::Weak);

  const ExportedSymbolSet::VarMap &vars = symbols.GetVars();

  ASSERT_NE(vars.end(), vars.find("global"));
  EXPECT_EQ(ElfSymbolIR::ElfSymbolBinding::Global,
            vars.at("global").GetBinding());

  ASSERT_NE(vars.end(), vars.find("weak"));
  EXPECT_EQ(ElfSymbolIR::ElfSymbolBinding::Weak,
            vars.at("weak").GetBinding());
}


TEST(ExportedSymbolSetTest, AddGlobPattern) {
  ExportedSymbolSet symbols;
  symbols.AddGlobPattern("test1*");

  const ExportedSymbolSet::GlobPatternSet &globs = symbols.GetGlobPatterns();
  ASSERT_NE(globs.end(), globs.find("test1*"));
}


TEST(ExportedSymbolSetTest, AddDemangledCppGlobPattern) {
  ExportedSymbolSet symbols;
  symbols.AddDemangledCppGlobPattern("test1*");

  const ExportedSymbolSet::GlobPatternSet &globs =
      symbols.GetDemangledCppGlobPatterns();
  ASSERT_NE(globs.end(), globs.find("test1*"));
}


TEST(ExportedSymbolSetTest, AddDemangledCppSymbol) {
  ExportedSymbolSet symbols;
  symbols.AddDemangledCppSymbol("Test::test()");

  const ExportedSymbolSet::NameSet &names = symbols.GetDemangledCppSymbols();
  ASSERT_NE(names.end(), names.find("Test::test()"));
}


TEST(ExportedSymbolSetTest, HasSymbol) {
  ExportedSymbolSet symbols;

  symbols.AddFunction("global_func", ElfSymbolIR::ElfSymbolBinding::Global);
  symbols.AddVar("global_var", ElfSymbolIR::ElfSymbolBinding::Global);

  symbols.AddGlobPattern("test_glob1_*");
  symbols.AddGlobPattern("test_glob2_[Aa]");
  symbols.AddGlobPattern("test_glob3_?");

  symbols.AddDemangledCppGlobPattern("Test1::*");
  symbols.AddDemangledCppGlobPattern("Test2::[Aa]()");
  symbols.AddDemangledCppGlobPattern("Test3::?()");
  symbols.AddDemangledCppSymbol("Test4::test()");

  // Test literal names
  EXPECT_TRUE(symbols.HasSymbol("global_func"));
  EXPECT_TRUE(symbols.HasSymbol("global_var"));

  EXPECT_FALSE(symbols.HasSymbol(""));
  EXPECT_FALSE(symbols.HasSymbol("no_such_function"));

  // Test glob patterns
  EXPECT_TRUE(symbols.HasSymbol("test_glob1_a"));
  EXPECT_TRUE(symbols.HasSymbol("test_glob2_A"));
  EXPECT_TRUE(symbols.HasSymbol("test_glob2_a"));
  EXPECT_TRUE(symbols.HasSymbol("test_glob3_b"));

  EXPECT_FALSE(symbols.HasSymbol("test_glob2_Ax"));
  EXPECT_FALSE(symbols.HasSymbol("test_glob2_B"));
  EXPECT_FALSE(symbols.HasSymbol("test_glob3_Bx"));

  // Test C++ names and patterns
  EXPECT_TRUE(symbols.HasSymbol("_ZN5Test14testEv"));
  EXPECT_TRUE(symbols.HasSymbol("_ZN5Test21AEv"));
  EXPECT_TRUE(symbols.HasSymbol("_ZN5Test21aEv"));
  EXPECT_TRUE(symbols.HasSymbol("_ZN5Test31bEv"));
  EXPECT_TRUE(symbols.HasSymbol("_ZN5Test44testEv"));

  EXPECT_FALSE(symbols.HasSymbol("_ZN5Test22AxEv"));
  EXPECT_FALSE(symbols.HasSymbol("_ZN5Test21bEv"));
  EXPECT_FALSE(symbols.HasSymbol("_ZN5Test32BxEv"));
}


}  // namespace repr
}  // namespace header_checker

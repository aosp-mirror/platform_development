// Copyright (C) 2016 The Android Open Source Project
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


#include "ast_processing.h"
#include "frontend_action.h"
#include <header_abi_util.h>

#include <clang/AST/ASTConsumer.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Lex/Preprocessor.h>
#include <llvm/ADT/STLExtras.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>

HeaderCheckerFrontendAction::HeaderCheckerFrontendAction(
    const std::string &dump_name, const std::vector<std::string> &exports)
  : dump_name_(dump_name), export_header_dirs_(exports) { }

std::unique_ptr<clang::ASTConsumer>
HeaderCheckerFrontendAction::CreateASTConsumer(clang::CompilerInstance &ci,
                                               llvm::StringRef header_file) {
  // Add preprocessor callbacks.
  clang::Preprocessor &pp = ci.getPreprocessor();
  pp.addPPCallbacks(llvm::make_unique<HeaderASTPPCallbacks>());
  std::set<std::string> exported_headers;
  for (auto &&dir_name : export_header_dirs_) {
    if (!abi_util::CollectExportedHeaderSet(dir_name, &exported_headers)) {
         return nullptr;
    }
  }
  // Create AST consumers.
  return llvm::make_unique<HeaderASTConsumer>(header_file,
                                              &ci, dump_name_,
                                              exported_headers);
}

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

#include "frontend_action.h"

#include "ast_processing.h"

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
    if (!CollectExportedHeaderSet(dir_name, &exported_headers)) {
         return nullptr;
    }
  }
  // Create AST consumers.
  return llvm::make_unique<HeaderASTConsumer>(header_file,
                                              &ci, dump_name_,
                                              exported_headers);
}

bool HeaderCheckerFrontendAction::CollectExportedHeaderSet(
    const std::string &dir_name,
    std::set<std::string> *exported_headers) {
  std::error_code ec;
  llvm::sys::fs::recursive_directory_iterator walker(dir_name, ec);
  // Default construction - end of directory.
  llvm::sys::fs::recursive_directory_iterator end;
  llvm::sys::fs::file_status status;
  for ( ; walker != end; walker.increment(ec)) {
    if (ec) {
      llvm::errs() << "Failed to walk dir : " << dir_name << "\n";
      return false;
    }

    const std::string &file_path = walker->path();

    llvm::StringRef file_name(llvm::sys::path::filename(file_path));
    // Ignore swap files and hidden files / dirs. Do not recurse into them too.
    // We should also not look at source files. Many projects include source
    // files in their exports.
    if (file_name.empty() || file_name.startswith(".") ||
        file_name.endswith(".swp") || file_name.endswith(".swo") ||
        file_name.endswith("#") || file_name.endswith(".cpp") ||
        file_name.endswith(".cc") || file_name.endswith(".c")) {
      walker.no_push();
      continue;
    }

    if (walker->status(status)) {
      llvm::errs() << "Failed to stat file : " << file_path << "\n";
      return false;
    }

    if (!llvm::sys::fs::is_regular_file(status)) {
      // Ignore non regular files. eg: soft links.
      continue;
    }

    llvm::SmallString<128> abs_path(file_path);
    if (llvm::sys::fs::make_absolute(abs_path)) {
      llvm::errs() << "Failed to get absolute path for : " << file_name << "\n";
      return false;
    }
    exported_headers->insert(abs_path.str());
  }
  return true;
}

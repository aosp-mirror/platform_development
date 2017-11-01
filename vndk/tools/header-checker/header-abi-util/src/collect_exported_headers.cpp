// Copyright (C) 2017 The Android Open Source Project
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

#include <header_abi_util.h>

#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>

#include <set>
#include <string>
#include <vector>

namespace abi_util {

static bool ShouldSkipFile(llvm::StringRef &file_name) {
 // Ignore swap files and hidden files / dirs. Do not recurse into them too.
  // We should also not look at source files. Many projects include source
  // files in their exports.
  if (file_name.empty() || file_name.startswith(".") ||
      file_name.endswith(".swp") || file_name.endswith(".swo") ||
      file_name.endswith("#") || file_name.endswith(".cpp") ||
      file_name.endswith(".cc") || file_name.endswith(".c")) {
    return true;
  }
  return false;
}

std::string RealPath(const std::string &path) {
  char file_abs_path[PATH_MAX];
  if (realpath(path.c_str(), file_abs_path) == nullptr) {
    return "";
  }
  return file_abs_path;
}

bool CollectExportedHeaderSet(const std::string &dir_name,
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
    if (ShouldSkipFile(file_name)) {
      walker.no_push();
      continue;
    }

    if (walker->status(status)) {
      llvm::errs() << "Failed to stat file : " << file_path << "\n";
      return false;
    }

    if ((status.type() != llvm::sys::fs::file_type::symlink_file) &&
        !llvm::sys::fs::is_regular_file(status)) {
      // Ignore non regular files, except symlinks.
      continue;
    }

    exported_headers->insert(RealPath(file_path));
  }
  return true;
}

std::set<std::string> CollectAllExportedHeaders(
    const std::vector<std::string> &exported_header_dirs) {
  std::set<std::string> exported_headers;
  for (auto &&dir : exported_header_dirs) {
    if (!abi_util::CollectExportedHeaderSet(dir, &exported_headers)) {
      llvm::errs() << "Couldn't collect exported headers\n";
      ::exit(1);
    }
  }
  return exported_headers;
}

} // namespace abi_util

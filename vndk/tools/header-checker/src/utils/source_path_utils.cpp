// Copyright (C) 2021 The Android Open Source Project
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

#include "utils/source_path_utils.h"

#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>
#include <llvm/Support/raw_ostream.h>

#include <algorithm>
#include <set>
#include <string>
#include <vector>

namespace header_checker {
namespace utils {

static const std::vector<std::string> header_extensions{
    ".h", ".hh", ".hpp", ".hxx", ".h++", ".inl", ".inc", ".ipp", ".h.generic"};

static const std::vector<std::string> libcxx_include_dir{"libcxx", "include"};

static bool HasHeaderExtension(llvm::StringRef file_name) {
  return std::find_if(header_extensions.begin(), header_extensions.end(),
                      [file_name](const std::string &e) {
                        return file_name.ends_with(e);
                      }) != header_extensions.end();
}

static bool PathEndsWith(llvm::StringRef path,
                         const std::vector<std::string> &suffix) {
  auto path_it = llvm::sys::path::rbegin(path);
  auto suffix_it = suffix.rbegin();
  while (suffix_it != suffix.rend()) {
    if (path_it == llvm::sys::path::rend(path)) {
      return false;
    }
    if (*path_it != *suffix_it) {
      return false;
    }
    ++path_it;
    ++suffix_it;
  }
  return true;
}

static std::string GetCwd() {
  llvm::SmallString<256> cwd;
  if (llvm::sys::fs::current_path(cwd)) {
    llvm::errs() << "ERROR: Failed to get current working directory\n";
    ::exit(1);
  }
  return std::string(cwd);
}

RootDirs ParseRootDirs(const std::vector<std::string> &args) {
  RootDirs root_dirs;
  for (const std::string_view arg : args) {
    std::string_view path;
    std::string_view replacement;
    size_t colon_index = arg.find(":");
    if (colon_index != std::string_view::npos) {
      path = arg.substr(0, colon_index);
      replacement = arg.substr(colon_index + 1);
    } else {
      path = arg;
      replacement = "";
    }
    llvm::SmallString<256> norm_replacement(replacement.begin(),
                                            replacement.end());
    llvm::sys::path::remove_dots(norm_replacement, /* remove_dot_dot = */ true);
    root_dirs.emplace_back(NormalizePath(path, {}),
                           std::string(norm_replacement));
  }
  if (root_dirs.empty()) {
    root_dirs.emplace_back(GetCwd(), "");
  }
  // Sort by length in descending order so that NormalizePath finds the longest
  // matching root dir.
  std::sort(root_dirs.begin(), root_dirs.end(),
            [](RootDir &first, RootDir &second) {
              return first.path.size() > second.path.size();
            });
  for (size_t index = 1; index < root_dirs.size(); index++) {
    if (root_dirs[index - 1].path == root_dirs[index].path) {
      llvm::errs() << "Duplicate root dir: " << root_dirs[index].path << "\n";
      ::exit(1);
    }
  }
  return root_dirs;
}

std::string NormalizePath(std::string_view path, const RootDirs &root_dirs) {
  llvm::SmallString<256> norm_path(path.begin(), path.end());
  if (llvm::sys::fs::make_absolute(norm_path)) {
    return "";
  }
  llvm::sys::path::remove_dots(norm_path, /* remove_dot_dot = */ true);
  llvm::StringRef separator = llvm::sys::path::get_separator();
  // Convert /root/dir/path to path.
  for (const RootDir &root_dir : root_dirs) {
    // llvm::sys::path::replace_path_prefix("AB", "A", "") returns "B", so do
    // not use it.
    if (!norm_path.starts_with(root_dir.path)) {
      continue;
    }
    if (norm_path.size() == root_dir.path.size()) {
      return root_dir.replacement;
    }
    llvm::StringRef suffix = norm_path.substr(root_dir.path.size());
    if (suffix.starts_with(separator)) {
      if (root_dir.replacement.empty()) {
        return suffix.substr(separator.size()).str();
      }
      // replacement == "/"
      if (llvm::StringRef(root_dir.replacement).ends_with(separator)) {
        return root_dir.replacement + suffix.substr(separator.size()).str();
      }
      return root_dir.replacement + suffix.str();
    }
  }
  return std::string(norm_path);
}

static bool CollectExportedHeaderSet(const std::string &dir_name,
                                     std::set<std::string> *exported_headers,
                                     const RootDirs &root_dirs) {
  // Bazel creates temporary files in header directories. To avoid race
  // condition, this function filters headers by name extensions.
  // An exception is that libc++ headers do not have extensions.
  bool collect_headers_without_extensions =
      PathEndsWith(dir_name, libcxx_include_dir);
  std::error_code ec;
  llvm::sys::fs::recursive_directory_iterator walker(dir_name, ec);
  // Default construction - end of directory.
  llvm::sys::fs::recursive_directory_iterator end;
  for ( ; walker != end; walker.increment(ec)) {
    if (ec) {
      llvm::errs() << "Failed to walk directory: " << dir_name << ": "
                   << ec.message() << "\n";
      return false;
    }

    const std::string &file_path = walker->path();

    llvm::StringRef file_name(llvm::sys::path::filename(file_path));
    if (file_name.empty() || file_name.starts_with(".")) {
      // Ignore hidden files and directories.
      walker.no_push();
      continue;
    }
    if (!file_name.contains(".")) {
      if (!collect_headers_without_extensions) {
        continue;
      }
    } else if (!HasHeaderExtension(file_name)) {
      continue;
    }

    llvm::ErrorOr<llvm::sys::fs::basic_file_status> status = walker->status();
    if (!status) {
      llvm::errs() << "Failed to stat file: " << file_path << "\n";
      return false;
    }

    if ((status->type() != llvm::sys::fs::file_type::symlink_file) &&
        (status->type() != llvm::sys::fs::file_type::regular_file)) {
      // Ignore non regular files, except symlinks.
      continue;
    }

    exported_headers->insert(NormalizePath(file_path, root_dirs));
  }
  return true;
}

std::set<std::string>
CollectAllExportedHeaders(const std::vector<std::string> &exported_header_dirs,
                          const RootDirs &root_dirs) {
  std::set<std::string> exported_headers;
  for (auto &&dir : exported_header_dirs) {
    if (!CollectExportedHeaderSet(dir, &exported_headers, root_dirs)) {
      llvm::errs() << "Couldn't collect exported headers\n";
      ::exit(1);
    }
  }
  return exported_headers;
}

}  // namespace utils
}  // namespace header_checker

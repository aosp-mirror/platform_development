// Copyright (C) 2018 The Android Open Source Project
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

#include "fixed_argv.h"
#include "omp_header_data.h"

#include <llvm/ADT/SmallString.h>
#include <llvm/Support/FileSystem.h>
#include <llvm/Support/Path.h>

#include <fstream>
#include <string>
#include <system_error>

#include <stdlib.h>

static std::string openmp_include_dir;

static void RemoveOpenMPIncludeDir() {
  if (openmp_include_dir.empty()) {
    return;
  }

  // Remove the <omp.h> header file.
  llvm::SmallString<64> path;
  llvm::sys::path::append(path, openmp_include_dir, "omp.h");
  llvm::sys::fs::remove(llvm::Twine(path));

  // Remove the directory.
  llvm::sys::fs::remove(llvm::Twine(openmp_include_dir));
}

static std::error_code WriteFile(const char *path, const char *data,
                                 size_t size) {
  std::fstream output(path, std::ios_base::out | std::ios_base::trunc);
  if (!output) {
    return std::make_error_code(std::io_errc::stream);
  }

  output.write(data, size);
  if (!output) {
    return std::make_error_code(std::io_errc::stream);
  }

  return std::error_code();
}

static std::error_code CreateOpenMPIncludeDir() {
  llvm::SmallString<64> path;

  // Create a temporary directory for include fixes.
  std::error_code error_code =
      llvm::sys::fs::createUniqueDirectory("header-abi-dump-include", path);
  if (error_code) {
    return error_code;
  }

  openmp_include_dir = path.str();

  // Register a directory cleanup callback.
  ::atexit(RemoveOpenMPIncludeDir);

  // Create <omp.h> and write the content.
  llvm::sys::path::append(path, "omp.h");
  return WriteFile(path.c_str(), OMP_HEADER_DATA, sizeof(OMP_HEADER_DATA) - 1);
}

static void SetupOpenMPIncludeDir(FixedArgv &fixed_argv) {
  // FIXME: clang-3289846 does not have <omp.h>.  This workaround copies omp.h
  // from LLVM 5.0+ and adds `-isystem` to header search paths.
  if (fixed_argv.IsLastArgEqualFirstOption("-fopenmp", "-fno-openmp")) {
    std::error_code ec = CreateOpenMPIncludeDir();
    if (!ec) {
      fixed_argv.PushForwardArgs("-isystem", openmp_include_dir.c_str());
    }
  }
}

static FixedArgvRegistry X(SetupOpenMPIncludeDir);

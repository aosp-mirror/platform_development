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

#include "repr/protobuf/abi_diff.h"
#include "repr/protobuf/abi_dump.h"

#include <google/protobuf/text_format.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>

#include <llvm/Support/CommandLine.h>
#include <llvm/Support/raw_ostream.h>

#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

#include <stdlib.h>


static llvm::cl::OptionCategory merge_diff_category(
    "merge-abi-diff options");

static llvm::cl::list<std::string> diff_report_list(
    llvm::cl::Positional, llvm::cl::desc("<diff-reports>"), llvm::cl::Required,
    llvm::cl::cat(merge_diff_category), llvm::cl::OneOrMore);

static llvm::cl::opt<std::string> merged_diff_report(
    "o", llvm::cl::desc("<merged-diff-report>"), llvm::cl::Required,
    llvm::cl::cat(merge_diff_category));

static llvm::cl::opt<bool> advice_only(
    "advice-only", llvm::cl::desc("Advisory mode only"), llvm::cl::Optional,
    llvm::cl::cat(merge_diff_category));

static llvm::cl::opt<bool> do_not_break_on_extensions(
    "allow-extensions",
    llvm::cl::desc("Do not return a non zero status on extensions"),
    llvm::cl::Optional, llvm::cl::cat(merge_diff_category));

static bool IsStatusDowngraded(
    const abi_diff::CompatibilityStatus &old_status,
    const abi_diff::CompatibilityStatus &new_status) {
  bool status_downgraded = false;
  switch (old_status) {
    case abi_diff::CompatibilityStatus::EXTENSION:
      if (new_status == abi_diff::CompatibilityStatus::INCOMPATIBLE) {
        status_downgraded = true;
      }
      break;
    case abi_diff::CompatibilityStatus::COMPATIBLE:
      if (new_status != abi_diff::CompatibilityStatus::COMPATIBLE) {
        status_downgraded = true;
      }
      break;
    default:
      break;
  }
  return status_downgraded;
}

static abi_diff::CompatibilityStatus MergeDiffReports(
    const std::vector<std::string> &diff_reports,
    const std::string &merged_diff_report) {
  abi_diff::MergedTranslationUnitDiff merged_tu_diff;
  std::ofstream text_output(merged_diff_report);
  google::protobuf::io::OstreamOutputStream text_os(&text_output);
  abi_diff::CompatibilityStatus status =
      abi_diff::CompatibilityStatus::COMPATIBLE;

  for (auto &&i : diff_reports) {
    abi_diff::TranslationUnitDiff diff_tu;
    std::ifstream input(i);
    google::protobuf::io::IstreamInputStream text_is(&input);
    if (!google::protobuf::TextFormat::Parse(&text_is, &diff_tu)) {
      llvm::errs() << "Failed to parse diff report\n";
      ::exit(1);
    }
    abi_diff::ConciseDiffReportInformation *added_tu_diff =
        merged_tu_diff.add_diff_reports();
    if (!added_tu_diff) {
      llvm::errs() << "Failed to add diff report to merged report\n";
      ::exit(1);
    }
    abi_diff::CompatibilityStatus new_status = diff_tu.compatibility_status();
    added_tu_diff->set_lib_name(diff_tu.lib_name());
    added_tu_diff->set_arch(diff_tu.arch());
    added_tu_diff->set_diff_report_path(i);
    added_tu_diff->set_compatibility_status(new_status);
    // Only, if the status is downgraded, change it.
    if (IsStatusDowngraded(status, new_status)) {
      status = new_status;
    }
  }

  if (!google::protobuf::TextFormat::Print(merged_tu_diff, &text_os)) {
    llvm::errs() << "Serialization to ostream failed\n";
    ::exit(1);
  }
  return status;
}

int main(int argc, const char **argv) {
  GOOGLE_PROTOBUF_VERIFY_VERSION;
  llvm::cl::ParseCommandLineOptions(argc, argv, "merge-abi-diff");
  abi_diff::CompatibilityStatus extension_or_incompatible =
      MergeDiffReports(diff_report_list, merged_diff_report);
  std::string status_str = "";
  switch (extension_or_incompatible) {
    case abi_diff::CompatibilityStatus::INCOMPATIBLE:
      status_str = "broken";
      break;
    case abi_diff::CompatibilityStatus::EXTENSION:
      status_str = "extended";
      break;
    default:
      break;
  }
  if (extension_or_incompatible) {
    llvm::errs() << "******************************************************\n"
                 << "VNDK Abi "
                 << status_str
                 << ":"
                 << " Please check compatibility report at : "
                 << merged_diff_report << "\n"
                 << "******************************************************\n";
  }

  if (do_not_break_on_extensions &&
      extension_or_incompatible == abi_diff::CompatibilityStatus::EXTENSION) {
      extension_or_incompatible = abi_diff::CompatibilityStatus::COMPATIBLE;
  }

  if (!advice_only) {
    return extension_or_incompatible;
  }
  return abi_diff::CompatibilityStatus::COMPATIBLE;
}

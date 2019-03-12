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

#include "dumper/diagnostic_consumer.h"

#include <clang/Basic/DiagnosticCategories.h>
#include <clang/Basic/DiagnosticIDs.h>
#include <clang/Lex/LexDiagnostic.h>


namespace header_checker {
namespace dumper {


HeaderCheckerDiagnosticConsumer::HeaderCheckerDiagnosticConsumer(
    std::unique_ptr<clang::DiagnosticConsumer> wrapped)
    : wrapped_(std::move(wrapped)) {}

void HeaderCheckerDiagnosticConsumer::clear() {
  // Default implementation resets warning/error count.
  DiagnosticConsumer::clear();
  wrapped_->clear();
}

void HeaderCheckerDiagnosticConsumer::BeginSourceFile(
    const clang::LangOptions &lang_opts,
    const clang::Preprocessor *preprocessor) {
  wrapped_->BeginSourceFile(lang_opts, preprocessor);
}

void HeaderCheckerDiagnosticConsumer::EndSourceFile() {
  wrapped_->EndSourceFile();
}

void HeaderCheckerDiagnosticConsumer::finish() { wrapped_->finish(); }

bool HeaderCheckerDiagnosticConsumer::IncludeInDiagnosticCounts() const {
  return false;
}

void HeaderCheckerDiagnosticConsumer::HandleDiagnostic(
    clang::DiagnosticsEngine::Level level, const clang::Diagnostic &info) {
  if (level < clang::DiagnosticsEngine::Level::Error) {
    return;
  }
  unsigned id = info.getID();
  if (id == clang::diag::err_pp_hash_error ||
      id == clang::diag::fatal_too_many_errors) {
    return;
  }
  unsigned category = clang::DiagnosticIDs::getCategoryNumberForDiag(id);
  if (category == clang::diag::DiagCat_Semantic_Issue) {
    return;
  }
  // Default implementation increases warning/error count.
  DiagnosticConsumer::HandleDiagnostic(level, info);
  wrapped_->HandleDiagnostic(level, info);
}


}  // dumper
}  // header_checker

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

#ifndef DIAGNOSTIC_CONSUMER_H_
#define DIAGNOSTIC_CONSUMER_H_

#include <clang/Basic/Diagnostic.h>


namespace header_checker {
namespace dumper {


class HeaderCheckerDiagnosticConsumer : public clang::DiagnosticConsumer {
 private:
  std::unique_ptr<clang::DiagnosticConsumer> wrapped_;

 public:
  HeaderCheckerDiagnosticConsumer(
      std::unique_ptr<clang::DiagnosticConsumer> wrapped);
  void clear() override;
  void BeginSourceFile(const clang::LangOptions &lang_opts,
                       const clang::Preprocessor *preprocessor) override;
  void EndSourceFile() override;
  void finish() override;
  bool IncludeInDiagnosticCounts() const override;
  void HandleDiagnostic(clang::DiagnosticsEngine::Level level,
                        const clang::Diagnostic &info) override;
};


}  // namespace dumper
}  // namespace header_checker


#endif  // DIAGNOSTIC_CONSUMER_H_

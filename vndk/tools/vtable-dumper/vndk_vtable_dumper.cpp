/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "elf_handling.h"

#include <llvm/Support/CommandLine.h>

using llvm::Expected;
using llvm::StringMapEntry;
using llvm::cl::Hidden;
using llvm::cl::Option;
using llvm::cl::OptionCategory;
using llvm::cl::ParseCommandLineOptions;
using llvm::cl::Positional;
using llvm::cl::Required;
using llvm::cl::SetVersionPrinter;
using llvm::cl::cat;
using llvm::cl::desc;
using llvm::cl::getRegisteredOptions;
using llvm::cl::opt;
using llvm::dyn_cast;
using llvm::object::ObjectFile;
using llvm::object::OwningBinary;
using llvm::outs;

OptionCategory VTableDumperCategory("vndk-vtable-dumper options");

opt<std::string> FilePath(
        Positional, Required, cat(VTableDumperCategory),
        desc("shared_library.so"));

static void HideIrrelevantCommandLineOptions() {
    for (StringMapEntry<Option *> &P : getRegisteredOptions()) {
        if (P.second->Category == &VTableDumperCategory) {
            continue;
        }
        if (P.first().startswith("help")) {
            continue;
        }
        P.second->setHiddenFlag(Hidden);
    }
}

static void PrintCommandVersion() {
    outs() << "vndk-vtable-dumper 0.1\n";
}

int main(int argc, char **argv) {
    // Parse command line options.
    HideIrrelevantCommandLineOptions();
    SetVersionPrinter(PrintCommandVersion);
    ParseCommandLineOptions(argc, argv);

    // Load ELF shared object file and print the vtables.
    Expected<OwningBinary<ObjectFile>> Binary =
            ObjectFile::createObjectFile(FilePath);
    if (!Binary) {
        outs() << "Couldn't create object File \n";
        return 1;
    }
    ObjectFile *Objfile = dyn_cast<ObjectFile>(&(*Binary.get().getBinary()));
    if (!Objfile) {
        return 1;
    }
    auto SoFile = SharedObject::create(Objfile);
    if (!SoFile) {
        outs() << "Couldn't create ELFObjectFile \n";
        return 1;
    }
    SoFile->printVTables();
    return 0;
}

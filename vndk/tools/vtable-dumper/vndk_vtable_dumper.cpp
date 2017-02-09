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

using llvm::Expected;
using llvm::StringRef;
using llvm::dyn_cast;
using llvm::object::ObjectFile;
using llvm::object::OwningBinary;
using llvm::outs;

int main (int argc, char **argv)
{
    if (argc != 2) {
        outs() << "usage: vndk-vtable-dumper path \n";
        return 1;
    }
    Expected<OwningBinary<ObjectFile>> Binary =
            ObjectFile::createObjectFile(StringRef(argv[1]));
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

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

#ifndef ELF_HANDLING_H_
#define ELF_HANDLING_H_

#include <map>
#include <memory>
#include <string>
#include <system_error>
#include <vector>

#include <llvm/Object/ELFObjectFile.h>
#include <llvm/Object/ELFTypes.h>
#include <llvm/Object/SymbolSize.h>
#include <llvm/Support/Endian.h>
#include <llvm/Support/raw_ostream.h>

using llvm::object::ObjectFile;
using llvm::object::ELFObjectFile;
using llvm::object::SectionRef;
using llvm::object::RelocationRef;
using llvm::object::ELFFile;
using llvm::object::ELFType;
using llvm::object::ELFDataTypeTypedefHelper;
using llvm::object::SymbolRef;
using llvm::outs;

class SharedObject {
public:
    static std::unique_ptr<SharedObject> create(const ObjectFile *);
    /* Print mangled names if the argument is true; demangled if false.
     */
    virtual void printVTables(bool) const = 0;
    virtual ~SharedObject() = 0;
private:
    virtual bool getVTables() = 0;
};

class VFunction {
public:
    VFunction(
            const std::string &,
            const std::string &,
            uint64_t);

    uint64_t getOffset() const;
    bool operator<(const VFunction &) const;
    const std::string &getMangledName() const;
    const std::string &getDemangledName() const;
private:
    std::string mMangledName;
    std::string mDemangledName;
    uint64_t mOffset;
};

class VTable {
public:
    using func_iterator = std::vector<VFunction>::const_iterator;
    VTable(
            const std::string &,
            const std::string &,
            uint64_t,
            uint64_t);

    uint64_t getStartAddr() const;
    uint64_t getEndAddr() const;
    uint64_t getBaseOffset() const;
    uint64_t getVTableSize() const;
    func_iterator begin() const;
    func_iterator end() const;
    const std::string &getMangledName() const;
    const std::string &getDemangledName() const;
    void sortVFunctions();
    void addVFunction(
            const std::string &,
            const std::string &,
            uint64_t);

    bool operator<(const VTable &) const;
    bool operator<(const uint64_t) const;
private:
    std::vector<VFunction> mFunctions;
    std::string mMangledName;
    std::string mDemangledName;
    /* This holds the range(st_value, st_value) through which the
     * VTable spans.
     */
    uint64_t mStartAddr;
    uint64_t mEndAddr;
    uint64_t mBaseOffset;
};

template<typename ELFT>
class ELFSharedObject : public SharedObject {
public:
    void printVTables(bool) const override;
    bool getVTables() override;
    ~ELFSharedObject();
    ELFSharedObject(const ELFObjectFile<ELFT> *);

private:
    /* We need a sym value to SymbolRef map in case the relocation provides
     * us with an addr instead of a sym index into dynsym / symtab.
     */
    LLVM_ELF_IMPORT_TYPES_ELFT(ELFT)
    typedef ELFFile<ELFT> ELFO;
    typedef typename ELFO::Elf_Shdr Elf_Shdr;
    typedef typename ELFO::Elf_Ehdr Elf_Ehdr;
    typedef typename ELFO::Elf_Sym Elf_Sym;
    typedef typename ELFO::Elf_Rela Elf_Rela;
    typedef typename ELFO::uintX_t uintX_t;
    std::map<uint64_t, std::vector<SymbolRef>> mAddrToSymbolRef;
    const ELFObjectFile<ELFT> *mObj;
    /* We cache the relocation sections, to look through their relocations for
     * vfunctions. Sections with type SHT_PROGBITS are cached since they contain
     * vtables. We might need to peek at the contents of a vtable in cases of
     * relative relocations.
     */
    std::vector<SectionRef> mRelSectionRefs;
    std::vector<SectionRef> mProgBitSectionRefs;
    std::vector<VTable> mVTables;

private:
    bool cacheELFSections();
    bool initVTableRanges();
    void getVFunctions();
    VTable *identifyVTable(uint64_t);
    void relocateSym(
            const RelocationRef &,
            const SectionRef &,
            VTable *);

    bool absoluteRelocation(const RelocationRef &, VTable *);
    bool relativeRelocation(
            const RelocationRef &,
            const SectionRef &,
            VTable *);

    uint64_t identifyAddend(uint64_t);
    uint64_t getAddendFromSection(const SectionRef &, uint64_t);
    SymbolRef matchValueToSymbol(std::vector<SymbolRef> &, VTable *);
};

template <typename T>
static inline T UnWrap(llvm::Expected<T> ValueOrError) {
    if (!ValueOrError) {
        outs() << "\nError: "
               << llvm::toString(ValueOrError.takeError())
               << ".\n";
        outs().flush();
        exit(1);
    }
    return std::move(ValueOrError.get());
}


#endif  // ELF_HANDLING_H_

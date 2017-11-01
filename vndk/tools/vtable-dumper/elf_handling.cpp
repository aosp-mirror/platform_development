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

#include <cxxabi.h>

using llvm::ELF::ELFDATA2MSB;
using llvm::ELF::EM_ARM;
using llvm::ELF::EM_MIPS;
using llvm::ELF::R_AARCH64_ABS64;
using llvm::ELF::R_AARCH64_RELATIVE;
using llvm::ELF::R_ARM_ABS32;
using llvm::ELF::R_ARM_RELATIVE;
using llvm::ELF::R_X86_64_64;
using llvm::ELF::R_X86_64_RELATIVE;
using llvm::ELF::R_MIPS_64;
using llvm::ELF::R_MIPS_REL32;
using llvm::ELF::R_MIPS_NONE;
using llvm::ELF::SHT_PROGBITS;
using llvm::ELF::SHT_REL;
using llvm::ELF::SHT_RELA;
using llvm::Expected;
using llvm::StringRef;
using llvm::dyn_cast;
using llvm::object::ELF32BEObjectFile;
using llvm::object::ELF32LEObjectFile;
using llvm::object::ELF64BEObjectFile;
using llvm::object::ELF64LEObjectFile;
using llvm::object::symbol_iterator;
using llvm::support::endian::read;
using llvm::outs;
using llvm::Error;
using llvm::make_unique;

static std::string demangle(const std::string &MangledName) {
     char *Str = __cxxabiv1::__cxa_demangle(
             MangledName.c_str(),
             nullptr,
             0,
             nullptr);
     if (Str) {
         std::string DemangledString(Str);
         free(Str);
         return DemangledString;
     }
     return "";
}

SharedObject::~SharedObject() {}

template <typename ELFT>
static std::unique_ptr<SharedObject> createELFSharedObject(
        const ELFObjectFile<ELFT> *Objfile) {
    return make_unique<ELFSharedObject<ELFT>>(Objfile);
}

static std::unique_ptr<SharedObject>createELFObjFile(const ObjectFile *Obj) {
    if (const ELF32LEObjectFile *Objfile = dyn_cast<ELF32LEObjectFile>(Obj))
        return createELFSharedObject(Objfile);
    if (const ELF32BEObjectFile *Objfile = dyn_cast<ELF32BEObjectFile>(Obj))
        return createELFSharedObject(Objfile);
    if (const ELF64LEObjectFile *Objfile = dyn_cast<ELF64LEObjectFile>(Obj))
        return createELFSharedObject(Objfile);
    if (const ELF64BEObjectFile *Objfile = dyn_cast<ELF64BEObjectFile>(Obj))
        return createELFSharedObject(Objfile);

    return nullptr;
}

std::unique_ptr<SharedObject> SharedObject::create(const ObjectFile *Obj) {
    std::unique_ptr<SharedObject> res(createELFObjFile(Obj));
    if (res && res->getVTables()) {
        return res;
    }
    return nullptr;
}

template <typename ELFT>
ELFSharedObject<ELFT>::~ELFSharedObject() {}

template <typename ELFT>
ELFSharedObject<ELFT>::ELFSharedObject(
        const ELFObjectFile<ELFT> *Objfile)
    : mObj(Objfile) {}

template <typename ELFT>
bool ELFSharedObject<ELFT>::cacheELFSections() {
    for (const SectionRef &ElfSection : mObj->sections()) {
        const Elf_Shdr *ElfShdr =
                mObj->getSection(ElfSection.getRawDataRefImpl());
        if (!ElfShdr) {
            outs() << "Couldn't create elf shdr \n";
            return false;
        }
        switch (ElfShdr->sh_type) {
            case SHT_RELA:
            case SHT_REL:
                mRelSectionRefs.emplace_back(ElfSection);
                break;
            case SHT_PROGBITS:
                mProgBitSectionRefs.emplace_back(ElfSection);
                break;
            default :
                // Any other section won't have information pertinent
                // to vtables. Relocation entries will have the virtual
                // functions' relocation information, the PROGBITS sections
                // will have the vtables themselves.
                break;
        }
    }
    return true;
}

template <typename ELFT>
void ELFSharedObject<ELFT>::printVTables(bool Mangled) const {
    for (const VTable &Vtable : mVTables) {
        if (Vtable.getVTableSize() == 0)
            continue;
        outs() << Vtable.getDemangledName()
               << "\n"
               << Vtable.getMangledName()
               << ": "
               << Vtable.getVTableSize()
               << " entries"
               << "\n";
        for (const VFunction &Vfunction : Vtable) {
            std::string VfunctionName = (Mangled ?
                                         Vfunction.getMangledName() :
                                         Vfunction.getDemangledName());
            outs() << Vfunction.getOffset()
                   << "    (int (*)(...)) "
                   << VfunctionName
                   << "\n";
        }
        outs() << "\n"
               << "\n";
    }
}

template <typename ELFT>
bool ELFSharedObject<ELFT>::getVTables() {
    if (!cacheELFSections()) {
        return false;
    }
    if (!initVTableRanges()) {
        return true;
    }
    getVFunctions();
    for (VTable &Vtable : mVTables) {
        // Sort the functions by offset before displaying them since the order
        // of functions appearing in relocation sections might change. That
        // should not result in the vtable layout changing.
        Vtable.sortVFunctions();
    }
    return true;
}

template <typename ELFT>
bool ELFSharedObject<ELFT>::initVTableRanges() {
    // Go through all the symbols in the dynsym / symtab sections
    // and cache all the relevant symbols. i.e: symbols which correspond
    // to either vtables or functions.

    std::vector<std::pair<SymbolRef, uint64_t>> SymsAndSizes =
            computeSymbolSizes(*mObj);
    for (std::pair<SymbolRef, uint64_t> &Pair : SymsAndSizes) {
        SymbolRef Symbol = Pair.first;
        SymbolRef::Type SymType = UnWrap(Symbol.getType());
        uint64_t SymValue = Symbol.getValue();
        StringRef SymName = UnWrap(Symbol.getName());
        if (SymName.startswith("__ZTV") || SymName.startswith("_ZTV")) {
            mVTables.emplace_back(
                    SymName.str(),
                    demangle(SymName.str()),
                    Symbol.getValue(),
                    Symbol.getValue() + Pair.second);
        } else if (SymType == SymbolRef::ST_Function) {
            std::map<uint64_t, std::vector<SymbolRef>>::iterator It =
                    mAddrToSymbolRef.find(SymValue);
            if (It == mAddrToSymbolRef.end()) {
                mAddrToSymbolRef.insert(std::make_pair(
                        SymValue, std::vector<SymbolRef>(1, Symbol)));
            } else {
                std::vector<SymbolRef> &SymVec = It->second;
                SymVec.emplace_back(Symbol);
            }
        }
    }
    if (mVTables.size() == 0) {
        return false;
    }
    std::sort(mVTables.begin(), mVTables.end());
    return true;
}

template <typename ELFT>
void ELFSharedObject<ELFT>::getVFunctions() {
    for (const SectionRef &Section : mRelSectionRefs) {
        for (const RelocationRef &Relocation : Section.relocations()) {
            VTable *VtPtr = identifyVTable(Relocation.getOffset());
            if (VtPtr != nullptr) {
                relocateSym(Relocation, Section, VtPtr);
            }
        }
    }
}

template <typename ELFT>
VTable *ELFSharedObject<ELFT>::identifyVTable(uint64_t RelOffset) {
    typename std::vector<VTable>::iterator It;
    It = std::lower_bound(mVTables.begin(), mVTables.end(), RelOffset);
    if (It != mVTables.begin() && It->getStartAddr() != RelOffset) {
        It--;
    }
    if (It->getEndAddr() >= RelOffset) {
        return &(*It);
    }
    return nullptr;
}

template <typename ELFT>
void ELFSharedObject<ELFT>::relocateSym(
        const RelocationRef &Relocation,
        const SectionRef &Section,
        VTable *Vtablep) {
    const Elf_Ehdr *ElfHeader = mObj->getELFFile()->getHeader();
    if (ElfHeader->e_machine == EM_MIPS) {
        // bionic/linker/linker_mips.cpp , we handle only one type of
        // relocation. Depending on if the symbol can be inferred from r_info we
        // make it an absolute or a relative relocation.
        if (!absoluteRelocation(Relocation, Vtablep)) {
            relativeRelocation(Relocation, Section, Vtablep);
        }
    } else {
        switch(Relocation.getType()) {
            case R_AARCH64_RELATIVE:
            case R_X86_64_RELATIVE:
            case R_ARM_RELATIVE:
            {
                // The return value is ignored since failure to relocate
                // does not mean a fatal error. It might be that the dynsym /
                // symbol-table does not have enough information to get the
                // symbol name. Like-wise for absolute relocations.
                relativeRelocation(Relocation, Section, Vtablep);
                break;
            }
            case R_AARCH64_ABS64:
            case R_X86_64_64:
            case R_ARM_ABS32:
            {
                absoluteRelocation(Relocation, Vtablep);
                break;
            }
            default:
                break;
        }
    }
}

template <typename ELFT>
bool ELFSharedObject<ELFT>::absoluteRelocation(
        const RelocationRef &Relocation,
        VTable *Vtablep) {
    symbol_iterator Symi = Relocation.getSymbol();
    if (Symi == mObj->symbol_end()) {
        return false;
    }
    SymbolRef Symbol = *Symi;
    uint64_t RelOffset = Relocation.getOffset();
    StringRef SymbolName = UnWrap(Symbol.getName());
    std::string DemangledName = demangle(SymbolName.str());
    if (!DemangledName.empty()) {
        Vtablep->addVFunction(SymbolName.str(), DemangledName, RelOffset);
        return true;
    }
    return false;
}

template <typename ELFT>
bool ELFSharedObject<ELFT>::relativeRelocation(
        const RelocationRef &Relocation,
        const SectionRef &Section,
        VTable *Vtablep) {
    uint64_t Addend = 0;
    uint64_t RelOffset = Relocation.getOffset();
    if (mObj->getSection(Section.getRawDataRefImpl())->sh_type == SHT_RELA) {
        const Elf_Rela *Rela = mObj->getRela(Relocation.getRawDataRefImpl());
        Addend = static_cast<uint64_t>(Rela->r_addend);
    }

    if (Addend == 0) {
        Addend = identifyAddend(Relocation.getOffset());
    }

    std::map<uint64_t, std::vector<SymbolRef>>::iterator It =
            mAddrToSymbolRef.find(Addend);
    if (It == mAddrToSymbolRef.end()) {
        return false;
    }
    SymbolRef Symbol = matchValueToSymbol(It->second, Vtablep);
    StringRef SymbolName = UnWrap(Symbol.getName());
    std::string DemangledName = demangle(SymbolName.str());
    if (!DemangledName.empty()) {
        Vtablep->addVFunction(SymbolName.str(), DemangledName, RelOffset);
        return true;
    }
    return false;
}

template <typename ELFT>
SymbolRef ELFSharedObject<ELFT>::matchValueToSymbol(
        std::vector<SymbolRef> &SymVec,
        VTable *Vtablep) {
    constexpr size_t pos = sizeof("vtable for ") - 1;
    const std::string ClassName(Vtablep->getDemangledName().substr(pos));
    for (const SymbolRef &Symbol : SymVec) {
        StringRef SymbolName = UnWrap(Symbol.getName());
        if (SymbolName.str().find(ClassName) != std::string::npos)
            return Symbol;
    }
    // Return the 1st Symbol by default.
    return SymVec[0];
}

template <typename ELFT>
uint64_t ELFSharedObject<ELFT>::identifyAddend(uint64_t ROffset) {
    for (const SectionRef &Section : mProgBitSectionRefs) {
        uint64_t Begin = Section.getAddress();
        uint64_t End = Section.getAddress() + Section.getSize();
        if (ROffset >= Begin && ROffset <= End) {
            return getAddendFromSection(Section, ROffset - Begin);
        }
    }
    return 0;
}

template <typename ELFT>
uint64_t ELFSharedObject<ELFT>::getAddendFromSection(
        const SectionRef &Section,
        uint64_t Offset) {
    StringRef Contents;
    if (Section.getContents(Contents))
        return 0;
    const unsigned char *Bytes = Contents.bytes_begin() + Offset;
    uintX_t Addend = read<uintX_t, ELFT::TargetEndianness>(Bytes);
    const Elf_Ehdr *ElfHeader = mObj->getELFFile()->getHeader();
    if (ElfHeader->e_machine == EM_ARM ||
        ElfHeader->e_machine == EM_MIPS) {
        // Remove thumb flag as llvm suggests.
        Addend &= ~1;
    }
    return static_cast<uint64_t>(Addend);
}

VFunction::VFunction(
        const std::string &MangledName,
        const std::string &DemangledName,
        uint64_t VFunctionOffset)
    : mMangledName(MangledName),
      mDemangledName(DemangledName),
      mOffset(VFunctionOffset) {}

uint64_t VFunction::getOffset() const {
    return mOffset;
}

const std::string &VFunction::getDemangledName() const {
    return mDemangledName;
}

const std::string &VFunction::getMangledName() const {
    return mMangledName;
}

bool VFunction::operator<(const VFunction &Vfunction) const {
    return mOffset < Vfunction.getOffset();
}

VTable::VTable(
        const std::string &MangledName,
        const std::string &DemangledName,
        uint64_t Begin,
        uint64_t End)
    : mMangledName(MangledName),
      mDemangledName(DemangledName),
      mStartAddr(Begin),
      mEndAddr(End),
      mBaseOffset(Begin) {}

void VTable::addVFunction(
        const std::string &MangledName,
        const std::string &DemangledName,
        uint64_t RelOffset) {
    mFunctions.emplace_back(
            MangledName,
            DemangledName,
            RelOffset - mBaseOffset);
}

const std::string &VTable::getDemangledName() const {
    return mDemangledName;
}

const std::string &VTable::getMangledName() const {
    return mMangledName;
}

uint64_t VTable::getStartAddr() const {
    return mStartAddr;
}

uint64_t VTable::getEndAddr() const {
    return mEndAddr;
}

uint64_t VTable::getBaseOffset() const {
    return mBaseOffset;
}

uint64_t VTable::getVTableSize() const {
    return mFunctions.size();
}

VTable::func_iterator VTable::begin() const {
    return mFunctions.cbegin();
}

VTable::func_iterator VTable::end() const {
    return mFunctions.cend();
}

bool VTable::operator<(const VTable &Vtable) const {
    return mStartAddr < Vtable.getStartAddr();
}

bool VTable::operator<(const uint64_t ROffset) const {
    return mStartAddr < ROffset;
}

void VTable::sortVFunctions() {
    std::sort(mFunctions.begin(), mFunctions.end());
}

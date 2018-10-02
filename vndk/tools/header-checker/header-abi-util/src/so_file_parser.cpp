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
#include <ir_representation.h>

#include <llvm/Object/ELFObjectFile.h>
#include <llvm/Object/Binary.h>
#include <llvm/Object/ELFTypes.h>
#include <llvm/Object/SymbolSize.h>

using llvm::object::ELF32LEObjectFile;
using llvm::object::ELF32BEObjectFile;
using llvm::object::ELF64LEObjectFile;
using llvm::object::ELF64BEObjectFile;
using llvm::dyn_cast;
using llvm::ELF::STV_DEFAULT;
using llvm::ELF::STV_PROTECTED;
using llvm::ELF::STB_WEAK;
using llvm::ELF::STB_GLOBAL;

namespace abi_util {

template <typename T>
static inline T UnWrap(llvm::Expected<T> ValueOrError) {
    if (!ValueOrError) {
      llvm::errs() << "\nError: "
               << llvm::toString(ValueOrError.takeError())
               << ".\n";
      llvm::errs().flush();
      exit(1);
    }
    return std::move(ValueOrError.get());
}

template<typename T>
const std::map<std::string, abi_util::ElfFunctionIR> &
ELFSoFileParser<T>::GetFunctions() const {
  return functions_;
}

template<typename T>
const std::map<std::string, abi_util::ElfObjectIR> &
ELFSoFileParser<T>::GetGlobVars() const {
  return globvars_;
}

template<typename T>
bool ELFSoFileParser<T>::IsSymbolExported(const Elf_Sym *elf_sym) const {
  unsigned char visibility = elf_sym->getVisibility();
  unsigned char binding = elf_sym->getBinding();
  return (binding == STB_GLOBAL || binding == STB_WEAK) &&
         (visibility == STV_DEFAULT || visibility == STV_PROTECTED);
}

static abi_util::ElfSymbolIR::ElfSymbolBinding
LLVMToIRSymbolBinding(unsigned char binding) {
  switch (binding) {
    case STB_GLOBAL:
      return abi_util::ElfSymbolIR::ElfSymbolBinding::Global;
    case STB_WEAK:
      return abi_util::ElfSymbolIR::ElfSymbolBinding::Weak;
  }
  assert(0);
}

template<typename T>
void ELFSoFileParser<T>::GetSymbols() {
  assert(obj_ != nullptr);
  for (auto symbol_it : obj_->getDynamicSymbolIterators()) {
    const Elf_Sym *elf_sym =
          obj_->getSymbol(symbol_it.getRawDataRefImpl());
    assert (elf_sym != nullptr);
    if (!IsSymbolExported(elf_sym) || elf_sym->isUndefined()) {
      continue;
    }
    abi_util::ElfSymbolIR::ElfSymbolBinding symbol_binding =
        LLVMToIRSymbolBinding(elf_sym->getBinding());
    llvm::object::SymbolRef::Type type = UnWrap(symbol_it.getType());
    std::string symbol_name = UnWrap(symbol_it.getName());
    if (type == llvm::object::SymbolRef::Type::ST_Function) {
      functions_.emplace(symbol_name,
                         ElfFunctionIR(symbol_name, symbol_binding));
    } else if (type == llvm::object::SymbolRef::Type::ST_Data) {
      globvars_.emplace(symbol_name, ElfObjectIR(symbol_name, symbol_binding));
    }
  }
}

template<typename T>
static std::unique_ptr<SoFileParser> CreateELFSoFileParser(
    const llvm::object::ELFObjectFile<T> *elfo) {
  return llvm::make_unique<ELFSoFileParser<T>>(elfo);
}

std::unique_ptr<SoFileParser> SoFileParser::Create(
    const llvm::object::ObjectFile *objfile) {
   // Little-endian 32-bit
  if (const ELF32LEObjectFile *ELFObj = dyn_cast<ELF32LEObjectFile>(objfile)) {
    return CreateELFSoFileParser(ELFObj);
  }

  // Big-endian 32-bit
  if (const ELF32BEObjectFile *ELFObj = dyn_cast<ELF32BEObjectFile>(objfile)) {
    return CreateELFSoFileParser(ELFObj);
  }

  // Little-endian 64-bit
  if (const ELF64LEObjectFile *ELFObj = dyn_cast<ELF64LEObjectFile>(objfile)) {
    return CreateELFSoFileParser(ELFObj);
  }

  // Big-endian 64-bit
  if (const ELF64BEObjectFile *ELFObj = dyn_cast<ELF64BEObjectFile>(objfile)) {
    return CreateELFSoFileParser(ELFObj);
  }
  return nullptr;
}

} // namespace abi_util


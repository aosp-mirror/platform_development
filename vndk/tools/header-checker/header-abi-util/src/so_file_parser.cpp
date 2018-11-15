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

#include "so_file_parser.h"

#include "ir_representation.h"

#include <llvm/Object/Binary.h>
#include <llvm/Object/ELFObjectFile.h>
#include <llvm/Object/ELFTypes.h>
#include <llvm/Object/SymbolSize.h>

namespace abi_util {

template <typename T>
static inline T UnWrap(llvm::Expected<T> value_or_error) {
  if (!value_or_error) {
    llvm::errs() << "\nerror: " << llvm::toString(value_or_error.takeError())
                 << ".\n";
    llvm::errs().flush();
    exit(1);
  }
  return std::move(value_or_error.get());
}

static abi_util::ElfSymbolIR::ElfSymbolBinding
LLVMToIRSymbolBinding(unsigned char binding) {
  switch (binding) {
    case llvm::ELF::STB_GLOBAL:
      return abi_util::ElfSymbolIR::ElfSymbolBinding::Global;
    case llvm::ELF::STB_WEAK:
      return abi_util::ElfSymbolIR::ElfSymbolBinding::Weak;
  }
  assert(0);
}

template<typename T>
class ELFSoFileParser : public SoFileParser {
 private:
  LLVM_ELF_IMPORT_TYPES_ELFT(T)
  typedef llvm::object::ELFFile<T> ELFO;
  typedef typename ELFO::Elf_Sym Elf_Sym;

 public:
  ELFSoFileParser(const llvm::object::ELFObjectFile<T> *obj);

  ~ELFSoFileParser() override {}

  const std::map<std::string, ElfFunctionIR> &GetFunctions() const override {
    return functions_;
  }

  const std::map<std::string, ElfObjectIR> &GetGlobVars() const override {
    return globvars_;
  }

 private:
  bool IsSymbolExported(const Elf_Sym *elf_sym) const {
    unsigned char visibility = elf_sym->getVisibility();
    unsigned char binding = elf_sym->getBinding();
    return ((binding == llvm::ELF::STB_GLOBAL ||
             binding == llvm::ELF::STB_WEAK) &&
            (visibility == llvm::ELF::STV_DEFAULT ||
             visibility == llvm::ELF::STV_PROTECTED));
  }

 private:
  const llvm::object::ELFObjectFile<T> *obj_;
  std::map<std::string, abi_util::ElfFunctionIR> functions_;
  std::map<std::string, abi_util::ElfObjectIR> globvars_;
};

template<typename T>
ELFSoFileParser<T>::ELFSoFileParser(const llvm::object::ELFObjectFile<T> *obj) {
  assert(obj != nullptr);
  for (auto symbol_it : obj->getDynamicSymbolIterators()) {
    const Elf_Sym *elf_sym = obj->getSymbol(symbol_it.getRawDataRefImpl());
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
    const std::string &so_file_path) {
  auto binary = llvm::object::createBinary(so_file_path);
  if (!binary) {
    return nullptr;
  }

  llvm::object::ObjectFile *obj_file =
      llvm::dyn_cast<llvm::object::ObjectFile>(binary.get().getBinary());
  if (!obj_file) {
    return nullptr;
  }

  // Little-endian 32-bit
  if (auto elf_obj_file =
          llvm::dyn_cast<llvm::object::ELF32LEObjectFile>(obj_file)) {
    return CreateELFSoFileParser(elf_obj_file);
  }

  // Big-endian 32-bit
  if (auto elf_obj_file =
          llvm::dyn_cast<llvm::object::ELF32BEObjectFile>(obj_file)) {
    return CreateELFSoFileParser(elf_obj_file);
  }

  // Little-endian 64-bit
  if (auto elf_obj_file =
          llvm::dyn_cast<llvm::object::ELF64LEObjectFile>(obj_file)) {
    return CreateELFSoFileParser(elf_obj_file);
  }

  // Big-endian 64-bit
  if (auto elf_obj_file =
          llvm::dyn_cast<llvm::object::ELF64BEObjectFile>(obj_file)) {
    return CreateELFSoFileParser(elf_obj_file);
  }

  return nullptr;
}

}  // namespace abi_util

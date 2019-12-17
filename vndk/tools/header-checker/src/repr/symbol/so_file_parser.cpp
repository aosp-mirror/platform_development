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

#include "repr/symbol/so_file_parser.h"

#include "repr/ir_representation.h"

#include <llvm/Object/Binary.h>
#include <llvm/Object/ELFObjectFile.h>
#include <llvm/Object/ELFTypes.h>
#include <llvm/Object/SymbolSize.h>

#include <utility>


namespace header_checker {
namespace repr {


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


static ElfSymbolIR::ElfSymbolBinding
LLVMToIRSymbolBinding(unsigned char binding) {
  switch (binding) {
    case llvm::ELF::STB_GLOBAL:
      return ElfSymbolIR::ElfSymbolBinding::Global;
    case llvm::ELF::STB_WEAK:
      return ElfSymbolIR::ElfSymbolBinding::Weak;
  }
  assert(0);
}


template <typename T>
class ELFSoFileParser : public SoFileParser {
 private:
  LLVM_ELF_IMPORT_TYPES_ELFT(T)
  typedef llvm::object::ELFFile<T> ELFO;
  typedef typename ELFO::Elf_Sym Elf_Sym;

 public:
  ELFSoFileParser(const llvm::object::ELFObjectFile<T> *obj);

  ~ELFSoFileParser() override {}

  std::unique_ptr<ExportedSymbolSet> Parse() override {
    return std::move(exported_symbols_);
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
  std::unique_ptr<ExportedSymbolSet> exported_symbols_;
};


template <typename T>
ELFSoFileParser<T>::ELFSoFileParser(const llvm::object::ELFObjectFile<T> *obj) {
  assert(obj != nullptr);

  exported_symbols_.reset(new ExportedSymbolSet());

  for (auto symbol_it : obj->getDynamicSymbolIterators()) {
    const Elf_Sym *elf_sym = obj->getSymbol(symbol_it.getRawDataRefImpl());
    assert (elf_sym != nullptr);
    if (!IsSymbolExported(elf_sym) || elf_sym->isUndefined()) {
      continue;
    }

    ElfSymbolIR::ElfSymbolBinding symbol_binding =
        LLVMToIRSymbolBinding(elf_sym->getBinding());
    std::string symbol_name = UnWrap(symbol_it.getName());

    llvm::object::SymbolRef::Type type = UnWrap(symbol_it.getType());
    if (type == llvm::object::SymbolRef::Type::ST_Function) {
      exported_symbols_->AddFunction(symbol_name, symbol_binding);
    } else if (type == llvm::object::SymbolRef::Type::ST_Data) {
      exported_symbols_->AddVar(symbol_name, symbol_binding);
    }
  }
}


template <typename T>
static std::unique_ptr<SoFileParser> CreateELFSoFileParser(
    const llvm::object::ELFObjectFile<T> *elfo) {
  return std::make_unique<ELFSoFileParser<T>>(elfo);
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


}  // namespace repr
}  // namespace header_checker

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

#ifndef SO_FILE_PARSER_H_
#define SO_FILE_PARSER_H_

#include "ir_representation.h"

#include <llvm/Object/ELFObjectFile.h>
#include <llvm/Object/ELFTypes.h>

#include <memory>
#include <map>
#include <string>

namespace abi_util {

class SoFileParser {
 public:
  virtual ~SoFileParser() {}

  static std::unique_ptr<SoFileParser> Create(
      const llvm::object::ObjectFile *obj);

  virtual const std::map<std::string, ElfFunctionIR> &GetFunctions() const = 0;
  virtual const std::map<std::string, ElfObjectIR> &GetGlobVars() const = 0;
  virtual void GetSymbols() = 0;
};

template<typename T>
class ELFSoFileParser : public SoFileParser {
 private:
  LLVM_ELF_IMPORT_TYPES_ELFT(T)
  typedef llvm::object::ELFFile<T> ELFO;
  typedef typename ELFO::Elf_Sym Elf_Sym;

 public:
  ELFSoFileParser(const llvm::object::ELFObjectFile<T> *obj) : obj_(obj) {}
  ~ELFSoFileParser() override {}

  const std::map<std::string, ElfFunctionIR> &GetFunctions() const override;
  const std::map<std::string, ElfObjectIR> &GetGlobVars() const override;
  void GetSymbols() override;

 private:
  const llvm::object::ELFObjectFile<T> *obj_;
  std::map<std::string, abi_util::ElfFunctionIR> functions_;
  std::map<std::string, abi_util::ElfObjectIR> globvars_;

 private:
  bool IsSymbolExported(const Elf_Sym *elf_sym) const;
};

}  // namespace abi_util

#endif  // SO_FILE_PARSER_H_

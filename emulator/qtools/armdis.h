// Copyright 2006 The Android Open Source Project

#ifndef ARMDIS_H
#define ARMDIS_H

#include <inttypes.h>
#include "opcode.h"

class Arm {
 public:
  static char *disasm(uint32_t addr, uint32_t insn, char *buffer);
  static Opcode decode(uint32_t insn);

 private:
  static Opcode decode00(uint32_t insn);
  static Opcode decode01(uint32_t insn);
  static Opcode decode10(uint32_t insn);
  static Opcode decode11(uint32_t insn);
  static Opcode decode_mul(uint32_t insn);
  static Opcode decode_ldrh(uint32_t insn);
  static Opcode decode_alu(uint32_t insn);

  static char *disasm_alu(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_branch(uint32_t addr, Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_bx(uint32_t insn, char *ptr);
  static char *disasm_bkpt(uint32_t insn, char *ptr);
  static char *disasm_clz(uint32_t insn, char *ptr);
  static char *disasm_memblock(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_mem(uint32_t insn, char *ptr);
  static char *disasm_memhalf(uint32_t insn, char *ptr);
  static char *disasm_mcr(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_mla(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_umlal(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_mul(Opcode opcode, uint32_t insn, char *ptr);
  static char *disasm_mrs(uint32_t insn, char *ptr);
  static char *disasm_msr(uint32_t insn, char *ptr);
  static char *disasm_pld(uint32_t insn, char *ptr);
  static char *disasm_swi(uint32_t insn, char *ptr);
  static char *disasm_swp(Opcode opcode, uint32_t insn, char *ptr);
};

extern char *disasm_insn_thumb(uint32_t pc, uint32_t insn1, uint32_t insn2, char *result);
extern Opcode decode_insn_thumb(uint32_t given);

#endif /* ARMDIS_H */

// Copyright 2006 The Android Open Source Project

#ifndef OPCODE_H
#define OPCODE_H

#include <inttypes.h>

// Note: this list of opcodes must match the list used to initialize
// the opflags[] array in opcode.cpp.
enum Opcode {
    OP_INVALID,
    OP_UNDEFINED,
    OP_ADC,
    OP_ADD,
    OP_AND,
    OP_B,
    OP_BL,
    OP_BIC,
    OP_BKPT,
    OP_BLX,
    OP_BX,
    OP_CDP,
    OP_CLZ,
    OP_CMN,
    OP_CMP,
    OP_EOR,
    OP_LDC,
    OP_LDM,
    OP_LDR,
    OP_LDRB,
    OP_LDRBT,
    OP_LDRH,
    OP_LDRSB,
    OP_LDRSH,
    OP_LDRT,
    OP_MCR,
    OP_MLA,
    OP_MOV,
    OP_MRC,
    OP_MRS,
    OP_MSR,
    OP_MUL,
    OP_MVN,
    OP_ORR,
    OP_PLD,
    OP_RSB,
    OP_RSC,
    OP_SBC,
    OP_SMLAL,
    OP_SMULL,
    OP_STC,
    OP_STM,
    OP_STR,
    OP_STRB,
    OP_STRBT,
    OP_STRH,
    OP_STRT,
    OP_SUB,
    OP_SWI,
    OP_SWP,
    OP_SWPB,
    OP_TEQ,
    OP_TST,
    OP_UMLAL,
    OP_UMULL,

    // Define thumb opcodes
    OP_THUMB_UNDEFINED,
    OP_THUMB_ADC,
    OP_THUMB_ADD,
    OP_THUMB_AND,
    OP_THUMB_ASR,
    OP_THUMB_B,
    OP_THUMB_BIC,
    OP_THUMB_BKPT,
    OP_THUMB_BL,
    OP_THUMB_BLX,
    OP_THUMB_BX,
    OP_THUMB_CMN,
    OP_THUMB_CMP,
    OP_THUMB_EOR,
    OP_THUMB_LDMIA,
    OP_THUMB_LDR,
    OP_THUMB_LDRB,
    OP_THUMB_LDRH,
    OP_THUMB_LDRSB,
    OP_THUMB_LDRSH,
    OP_THUMB_LSL,
    OP_THUMB_LSR,
    OP_THUMB_MOV,
    OP_THUMB_MUL,
    OP_THUMB_MVN,
    OP_THUMB_NEG,
    OP_THUMB_ORR,
    OP_THUMB_POP,
    OP_THUMB_PUSH,
    OP_THUMB_ROR,
    OP_THUMB_SBC,
    OP_THUMB_STMIA,
    OP_THUMB_STR,
    OP_THUMB_STRB,
    OP_THUMB_STRH,
    OP_THUMB_SUB,
    OP_THUMB_SWI,
    OP_THUMB_TST,

    OP_END                // must be last
};

extern uint32_t opcode_flags[];
extern const char *opcode_names[];

// Define bit flags for the opcode categories
static const uint32_t kCatByte          = 0x0001;
static const uint32_t kCatHalf          = 0x0002;
static const uint32_t kCatWord          = 0x0004;
static const uint32_t kCatLong          = 0x0008;
static const uint32_t kCatNumBytes      = (kCatByte | kCatHalf | kCatWord | kCatLong);
static const uint32_t kCatMultiple      = 0x0010;
static const uint32_t kCatSigned        = 0x0020;
static const uint32_t kCatLoad          = 0x0040;
static const uint32_t kCatStore         = 0x0080;
static const uint32_t kCatMemoryRef     = (kCatLoad | kCatStore);
static const uint32_t kCatAlu           = 0x0100;
static const uint32_t kCatBranch        = 0x0200;
static const uint32_t kCatBranchLink    = 0x0400;
static const uint32_t kCatBranchExch    = 0x0800;
static const uint32_t kCatCoproc        = 0x1000;
static const uint32_t kCatLoadMultiple  = (kCatLoad | kCatMultiple);
static const uint32_t kCatStoreMultiple = (kCatStore | kCatMultiple);

inline bool isALU(Opcode op)    { return (opcode_flags[op] & kCatAlu) != 0; }
inline bool isBranch(Opcode op) { return (opcode_flags[op] & kCatBranch) != 0; }
inline bool isBranchLink(Opcode op) {
    return (opcode_flags[op] & kCatBranchLink) != 0;
}
inline bool isBranchExch(Opcode op) {
    return (opcode_flags[op] & kCatBranchExch) != 0;
}
inline bool isLoad(Opcode op)   { return (opcode_flags[op] & kCatLoad) != 0; }
inline bool isLoadMultiple(Opcode op) {
    return (opcode_flags[op] & kCatLoadMultiple) == kCatLoadMultiple;
}
inline bool isStoreMultiple(Opcode op) {
    return (opcode_flags[op] & kCatStoreMultiple) == kCatStoreMultiple;
}
inline bool isStore(Opcode op)  { return (opcode_flags[op] & kCatStore) != 0; }
inline bool isSigned(Opcode op) { return (opcode_flags[op] & kCatSigned) != 0; }
inline bool isMemoryRef(Opcode op) {
    return (opcode_flags[op] & kCatMemoryRef) != 0;
}
inline int getAccessSize(Opcode op) { return opcode_flags[op] & kCatNumBytes; }
inline bool isCoproc(Opcode op) { return (opcode_flags[op] & kCatCoproc) != 0; }
inline int getNumAccesses(Opcode op, uint32_t binary) {
  extern int num_one_bits[];
  int num_accesses = 0;
  if (opcode_flags[op] & kCatNumBytes)
    num_accesses = 1;
  else if (opcode_flags[op] & kCatMultiple) {
    num_accesses = num_one_bits[(binary >> 8) & 0xff]
                   + num_one_bits[binary & 0xff];
  }
  return num_accesses;
}

#endif  // OPCODE_H

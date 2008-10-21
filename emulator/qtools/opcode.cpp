// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <inttypes.h>
#include "opcode.h"

// Note: this array depends on the Opcode enum defined in opcode.h
uint32_t opcode_flags[] = {
    0,                                             // OP_INVALID
    0,                                             // OP_UNDEFINED
    kCatAlu,                                       // OP_ADC
    kCatAlu,                                       // OP_ADD
    kCatAlu,                                       // OP_AND
    kCatBranch,                                    // OP_B
    kCatBranch | kCatBranchLink,                   // OP_BL
    kCatAlu,                                       // OP_BIC
    0,                                             // OP_BKPT
    kCatBranch | kCatBranchLink | kCatBranchExch,  // OP_BLX
    kCatBranch | kCatBranchExch,                   // OP_BX
    kCatCoproc,                                    // OP_CDP
    kCatAlu,                                       // OP_CLZ
    kCatAlu,                                       // OP_CMN
    kCatAlu,                                       // OP_CMP
    kCatAlu,                                       // OP_EOR
    kCatCoproc | kCatLoad,                         // OP_LDC
    kCatLoad | kCatMultiple,                       // OP_LDM
    kCatLoad | kCatWord,                           // OP_LDR
    kCatLoad | kCatByte,                           // OP_LDRB
    kCatLoad | kCatByte,                           // OP_LDRBT
    kCatLoad | kCatHalf,                           // OP_LDRH
    kCatLoad | kCatByte | kCatSigned,              // OP_LDRSB
    kCatLoad | kCatHalf | kCatSigned,              // OP_LDRSH
    kCatLoad | kCatWord,                           // OP_LDRT
    kCatCoproc,                                    // OP_MCR
    kCatAlu,                                       // OP_MLA
    kCatAlu,                                       // OP_MOV
    kCatCoproc,                                    // OP_MRC
    0,                                             // OP_MRS
    0,                                             // OP_MSR
    kCatAlu,                                       // OP_MUL
    kCatAlu,                                       // OP_MVN
    kCatAlu,                                       // OP_ORR
    0,                                             // OP_PLD
    kCatAlu,                                       // OP_RSB
    kCatAlu,                                       // OP_RSC
    kCatAlu,                                       // OP_SBC
    kCatAlu,                                       // OP_SMLAL
    kCatAlu,                                       // OP_SMULL
    kCatCoproc | kCatStore,                        // OP_STC
    kCatStore | kCatMultiple,                      // OP_STM
    kCatStore | kCatWord,                          // OP_STR
    kCatStore | kCatByte,                          // OP_STRB
    kCatStore | kCatByte,                          // OP_STRBT
    kCatStore | kCatHalf,                          // OP_STRH
    kCatStore | kCatWord,                          // OP_STRT
    kCatAlu,                                       // OP_SUB
    0,                                             // OP_SWI
    kCatLoad | kCatStore,                          // OP_SWP
    kCatLoad | kCatStore | kCatByte,               // OP_SWPB
    kCatAlu,                                       // OP_TEQ
    kCatAlu,                                       // OP_TST
    kCatAlu,                                       // OP_UMLAL
    kCatAlu,                                       // OP_UMULL

    0,                                             // OP_THUMB_UNDEFINED,
    kCatAlu,                                       // OP_THUMB_ADC,
    kCatAlu,                                       // OP_THUMB_ADD,
    kCatAlu,                                       // OP_THUMB_AND,
    kCatAlu,                                       // OP_THUMB_ASR,
    kCatBranch,                                    // OP_THUMB_B,
    kCatAlu,                                       // OP_THUMB_BIC,
    0,                                             // OP_THUMB_BKPT,
    kCatBranch | kCatBranchLink,                   // OP_THUMB_BL,
    kCatBranch | kCatBranchLink | kCatBranchExch,  // OP_THUMB_BLX,
    kCatBranch | kCatBranchExch,                   // OP_THUMB_BX,
    kCatAlu,                                       // OP_THUMB_CMN,
    kCatAlu,                                       // OP_THUMB_CMP,
    kCatAlu,                                       // OP_THUMB_EOR,
    kCatLoad | kCatMultiple,                       // OP_THUMB_LDMIA,
    kCatLoad | kCatWord,                           // OP_THUMB_LDR,
    kCatLoad | kCatByte,                           // OP_THUMB_LDRB,
    kCatLoad | kCatHalf,                           // OP_THUMB_LDRH,
    kCatLoad | kCatByte | kCatSigned,              // OP_THUMB_LDRSB,
    kCatLoad | kCatHalf | kCatSigned,              // OP_THUMB_LDRSH,
    kCatAlu,                                       // OP_THUMB_LSL,
    kCatAlu,                                       // OP_THUMB_LSR,
    kCatAlu,                                       // OP_THUMB_MOV,
    kCatAlu,                                       // OP_THUMB_MUL,
    kCatAlu,                                       // OP_THUMB_MVN,
    kCatAlu,                                       // OP_THUMB_NEG,
    kCatAlu,                                       // OP_THUMB_ORR,
    kCatLoad | kCatMultiple,                       // OP_THUMB_POP,
    kCatStore | kCatMultiple,                      // OP_THUMB_PUSH,
    kCatAlu,                                       // OP_THUMB_ROR,
    kCatAlu,                                       // OP_THUMB_SBC,
    kCatStore | kCatMultiple,                      // OP_THUMB_STMIA,
    kCatStore | kCatWord,                          // OP_THUMB_STR,
    kCatStore | kCatByte,                          // OP_THUMB_STRB,
    kCatStore | kCatHalf,                          // OP_THUMB_STRH,
    kCatAlu,                                       // OP_THUMB_SUB,
    0,                                             // OP_THUMB_SWI,
    kCatAlu,                                       // OP_THUMB_TST,

    0,                                             // OP_END
};

const char *opcode_names[] = {
    "invalid",
    "undefined",
    "adc",
    "add",
    "and",
    "b",
    "bl",
    "bic",
    "bkpt",
    "blx",
    "bx",
    "cdp",
    "clz",
    "cmn",
    "cmp",
    "eor",
    "ldc",
    "ldm",
    "ldr",
    "ldrb",
    "ldrbt",
    "ldrh",
    "ldrsb",
    "ldrsh",
    "ldrt",
    "mcr",
    "mla",
    "mov",
    "mrc",
    "mrs",
    "msr",
    "mul",
    "mvn",
    "orr",
    "pld",
    "rsb",
    "rsc",
    "sbc",
    "smlal",
    "smull",
    "stc",
    "stm",
    "str",
    "strb",
    "strbt",
    "strh",
    "strt",
    "sub",
    "swi",
    "swp",
    "swpb",
    "teq",
    "tst",
    "umlal",
    "umull",

    "undefined",
    "adc",
    "add",
    "and",
    "asr",
    "b",
    "bic",
    "bkpt",
    "bl",
    "blx",
    "bx",
    "cmn",
    "cmp",
    "eor",
    "ldmia",
    "ldr",
    "ldrb",
    "ldrh",
    "ldrsb",
    "ldrsh",
    "lsl",
    "lsr",
    "mov",
    "mul",
    "mvn",
    "neg",
    "orr",
    "pop",
    "push",
    "ror",
    "sbc",
    "stmia",
    "str",
    "strb",
    "strh",
    "sub",
    "swi",
    "tst",

    NULL
};

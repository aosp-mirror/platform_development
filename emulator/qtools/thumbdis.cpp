/* Instruction printing code for the ARM
   Copyright 1994, 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002
   Free Software Foundation, Inc.
   Contributed by Richard Earnshaw (rwe@pegasus.esprit.ec.org)
   Modification by James G. Smith (jsmith@cygnus.co.uk)

This file is part of libopcodes. 

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your option)
any later version. 

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
more details. 

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.  */

/* Modified to fit into the qtools framework.  The main differences are:
 *
 * - The disassembly function returns a string instead of writing it to a
 * file stream.
 *
 * - All the references to the struct "disassemble_info" have been removed.
 *
 * - A set of enums for the thumb opcodes have been defined, along with a
 * "decode()" function that maps a thumb instruction to an opcode enum.
 *
 * - Eliminated uses of the special characters ', `, and ? from the
 * thumb_opcodes[] table so that we can easily specify separate opcodes
 * for distinct instructions.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "opcode.h"


struct thumb_opcode
{
    unsigned short value, mask;  /* recognise instruction if (op&mask)==value */
    Opcode opcode;
    const char * assembler;      /* how to disassemble this instruction */
};

/* format of the assembler string :
   
   %%                   %
   %<bitfield>d         print the bitfield in decimal
   %<bitfield>x         print the bitfield in hex
   %<bitfield>X         print the bitfield as 1 hex digit without leading "0x"
   %<bitfield>r         print as an ARM register
   %<bitfield>f         print a floating point constant if >7 else a
                          floating point register
   %<code>y             print a single precision VFP reg.
                          Codes: 0=>Sm, 1=>Sd, 2=>Sn, 3=>multi-list, 4=>Sm pair
   %<code>z             print a double precision VFP reg
                          Codes: 0=>Dm, 1=>Dd, 2=>Dn, 3=>multi-list
   %c                   print condition code (always bits 28-31)
   %P                   print floating point precision in arithmetic insn
   %Q                   print floating point precision in ldf/stf insn
   %R                   print floating point rounding mode
   %<bitnum>'c          print specified char iff bit is one
   %<bitnum>`c          print specified char iff bit is zero
   %<bitnum>?ab         print a if bit is one else print b
   %p                   print 'p' iff bits 12-15 are 15
   %t                   print 't' iff bit 21 set and bit 24 clear
   %o                   print operand2 (immediate or register + shift)
   %a                   print address for ldr/str instruction
   %s                   print address for ldr/str halfword/signextend instruction
   %b                   print branch destination
   %B                   print arm BLX(1) destination
   %A                   print address for ldc/stc/ldf/stf instruction
   %m                   print register mask for ldm/stm instruction
   %C                   print the PSR sub type.
   %F                   print the COUNT field of a LFM/SFM instruction.
Thumb specific format options:
   %D                   print Thumb register (bits 0..2 as high number if bit 7 set)
   %S                   print Thumb register (bits 3..5 as high number if bit 6 set)
   %<bitfield>I         print bitfield as a signed decimal
                          (top bit of range being the sign bit)
   %M                   print Thumb register mask
   %N                   print Thumb register mask (with LR)
   %O                   print Thumb register mask (with PC)
   %T                   print Thumb condition code (always bits 8-11)
   %I                   print cirrus signed shift immediate: bits 0..3|4..6
   %<bitfield>B         print Thumb branch destination (signed displacement)
   %<bitfield>W         print (bitfield * 4) as a decimal
   %<bitfield>H         print (bitfield * 2) as a decimal
   %<bitfield>a         print (bitfield * 4) as a pc-rel offset + decoded symbol
*/


static struct thumb_opcode thumb_opcodes[] =
{
    /* Thumb instructions.  */

    /* ARM V5 ISA extends Thumb.  */
    {0xbe00, 0xff00, OP_THUMB_BKPT, "bkpt\t%0-7x"},
    {0x4780, 0xff87, OP_THUMB_BLX, "blx\t%3-6r"},  /* note: 4 bit register number.  */
    /* Format 5 instructions do not update the PSR.  */
    {0x1C00, 0xFFC0, OP_THUMB_MOV, "mov\t%0-2r, %3-5r"},
    /* Format 4.  */
    {0x4000, 0xFFC0, OP_THUMB_AND, "and\t%0-2r, %3-5r"},
    {0x4040, 0xFFC0, OP_THUMB_EOR, "eor\t%0-2r, %3-5r"},
    {0x4080, 0xFFC0, OP_THUMB_LSL, "lsl\t%0-2r, %3-5r"},
    {0x40C0, 0xFFC0, OP_THUMB_LSR, "lsr\t%0-2r, %3-5r"},
    {0x4100, 0xFFC0, OP_THUMB_ASR, "asr\t%0-2r, %3-5r"},
    {0x4140, 0xFFC0, OP_THUMB_ADC, "adc\t%0-2r, %3-5r"},
    {0x4180, 0xFFC0, OP_THUMB_SBC, "sbc\t%0-2r, %3-5r"},
    {0x41C0, 0xFFC0, OP_THUMB_ROR, "ror\t%0-2r, %3-5r"},
    {0x4200, 0xFFC0, OP_THUMB_TST, "tst\t%0-2r, %3-5r"},
    {0x4240, 0xFFC0, OP_THUMB_NEG, "neg\t%0-2r, %3-5r"},
    {0x4280, 0xFFC0, OP_THUMB_CMP, "cmp\t%0-2r, %3-5r"},
    {0x42C0, 0xFFC0, OP_THUMB_CMN, "cmn\t%0-2r, %3-5r"},
    {0x4300, 0xFFC0, OP_THUMB_ORR, "orr\t%0-2r, %3-5r"},
    {0x4340, 0xFFC0, OP_THUMB_MUL, "mul\t%0-2r, %3-5r"},
    {0x4380, 0xFFC0, OP_THUMB_BIC, "bic\t%0-2r, %3-5r"},
    {0x43C0, 0xFFC0, OP_THUMB_MVN, "mvn\t%0-2r, %3-5r"},
    /* format 13 */
    {0xB000, 0xFF80, OP_THUMB_ADD, "add\tsp, #%0-6W"},
    {0xB080, 0xFF80, OP_THUMB_SUB, "sub\tsp, #%0-6W"},
    /* format 5 */
    {0x4700, 0xFF80, OP_THUMB_BX, "bx\t%S"},
    {0x4400, 0xFF00, OP_THUMB_ADD, "add\t%D, %S"},
    {0x4500, 0xFF00, OP_THUMB_CMP, "cmp\t%D, %S"},
    {0x4600, 0xFF00, OP_THUMB_MOV, "mov\t%D, %S"},
    /* format 14 */
    {0xB400, 0xFE00, OP_THUMB_PUSH, "push\t%N"},
    {0xBC00, 0xFE00, OP_THUMB_POP, "pop\t%O"},
    /* format 2 */
    {0x1800, 0xFE00, OP_THUMB_ADD, "add\t%0-2r, %3-5r, %6-8r"},
    {0x1A00, 0xFE00, OP_THUMB_SUB, "sub\t%0-2r, %3-5r, %6-8r"},
    {0x1C00, 0xFE00, OP_THUMB_ADD, "add\t%0-2r, %3-5r, #%6-8d"},
    {0x1E00, 0xFE00, OP_THUMB_SUB, "sub\t%0-2r, %3-5r, #%6-8d"},
    /* format 8 */
    {0x5200, 0xFE00, OP_THUMB_STRH, "strh\t%0-2r, [%3-5r, %6-8r]"},
    {0x5A00, 0xFE00, OP_THUMB_LDRH, "ldrh\t%0-2r, [%3-5r, %6-8r]"},
    {0x5600, 0xFE00, OP_THUMB_LDRSB, "ldrsb\t%0-2r, [%3-5r, %6-8r]"},
    {0x5E00, 0xFE00, OP_THUMB_LDRSH, "ldrsh\t%0-2r, [%3-5r, %6-8r]"},
    /* format 7 */
    {0x5000, 0xFE00, OP_THUMB_STR, "str\t%0-2r, [%3-5r, %6-8r]"},
    {0x5400, 0xFE00, OP_THUMB_STRB, "strb\t%0-2r, [%3-5r, %6-8r]"},
    {0x5800, 0xFE00, OP_THUMB_LDR, "ldr\t%0-2r, [%3-5r, %6-8r]"},
    {0x5C00, 0xFE00, OP_THUMB_LDRB, "ldrb\t%0-2r, [%3-5r, %6-8r]"},
    /* format 1 */
    {0x0000, 0xF800, OP_THUMB_LSL, "lsl\t%0-2r, %3-5r, #%6-10d"},
    {0x0800, 0xF800, OP_THUMB_LSR, "lsr\t%0-2r, %3-5r, #%6-10d"},
    {0x1000, 0xF800, OP_THUMB_ASR, "asr\t%0-2r, %3-5r, #%6-10d"},
    /* format 3 */
    {0x2000, 0xF800, OP_THUMB_MOV, "mov\t%8-10r, #%0-7d"},
    {0x2800, 0xF800, OP_THUMB_CMP, "cmp\t%8-10r, #%0-7d"},
    {0x3000, 0xF800, OP_THUMB_ADD, "add\t%8-10r, #%0-7d"},
    {0x3800, 0xF800, OP_THUMB_SUB, "sub\t%8-10r, #%0-7d"},
    /* format 6 */
    /* TODO: Disassemble PC relative "LDR rD,=<symbolic>" */
    {0x4800, 0xF800, OP_THUMB_LDR, "ldr\t%8-10r, [pc, #%0-7W]\t(%0-7a)"},
    /* format 9 */
    {0x6000, 0xF800, OP_THUMB_STR, "str\t%0-2r, [%3-5r, #%6-10W]"},
    {0x6800, 0xF800, OP_THUMB_LDR, "ldr\t%0-2r, [%3-5r, #%6-10W]"},
    {0x7000, 0xF800, OP_THUMB_STRB, "strb\t%0-2r, [%3-5r, #%6-10d]"},
    {0x7800, 0xF800, OP_THUMB_LDRB, "ldrb\t%0-2r, [%3-5r, #%6-10d]"},
    /* format 10 */
    {0x8000, 0xF800, OP_THUMB_STRH, "strh\t%0-2r, [%3-5r, #%6-10H]"},
    {0x8800, 0xF800, OP_THUMB_LDRH, "ldrh\t%0-2r, [%3-5r, #%6-10H]"},
    /* format 11 */
    {0x9000, 0xF800, OP_THUMB_STR, "str\t%8-10r, [sp, #%0-7W]"},
    {0x9800, 0xF800, OP_THUMB_LDR, "ldr\t%8-10r, [sp, #%0-7W]"},
    /* format 12 */
    {0xA000, 0xF800, OP_THUMB_ADD, "add\t%8-10r, pc, #%0-7W\t(adr %8-10r,%0-7a)"},
    {0xA800, 0xF800, OP_THUMB_ADD, "add\t%8-10r, sp, #%0-7W"},
    /* format 15 */
    {0xC000, 0xF800, OP_THUMB_STMIA, "stmia\t%8-10r!,%M"},
    {0xC800, 0xF800, OP_THUMB_LDMIA, "ldmia\t%8-10r!,%M"},
    /* format 18 */
    {0xE000, 0xF800, OP_THUMB_B, "b\t%0-10B"},
    /* format 19 */
    /* special processing required in disassembler */
    {0xF000, 0xF800, OP_THUMB_BL, ""},
    {0xF800, 0xF800, OP_THUMB_BL, "second half of BL instruction %0-15x"},
    {0xE800, 0xF800, OP_THUMB_BLX, "second half of BLX instruction %0-15x"},
    /* format 16 */
    {0xD000, 0xFF00, OP_THUMB_B, "beq\t%0-7B"},
    {0xD100, 0xFF00, OP_THUMB_B, "bne\t%0-7B"},
    {0xD200, 0xFF00, OP_THUMB_B, "bcs\t%0-7B"},
    {0xD300, 0xFF00, OP_THUMB_B, "bcc\t%0-7B"},
    {0xD400, 0xFF00, OP_THUMB_B, "bmi\t%0-7B"},
    {0xD500, 0xFF00, OP_THUMB_B, "bpl\t%0-7B"},
    {0xD600, 0xFF00, OP_THUMB_B, "bvs\t%0-7B"},
    {0xD700, 0xFF00, OP_THUMB_B, "bvc\t%0-7B"},
    {0xD800, 0xFF00, OP_THUMB_B, "bhi\t%0-7B"},
    {0xD900, 0xFF00, OP_THUMB_B, "bls\t%0-7B"},
    {0xDA00, 0xFF00, OP_THUMB_B, "bge\t%0-7B"},
    {0xDB00, 0xFF00, OP_THUMB_B, "blt\t%0-7B"},
    {0xDC00, 0xFF00, OP_THUMB_B, "bgt\t%0-7B"},
    {0xDD00, 0xFF00, OP_THUMB_B, "ble\t%0-7B"},
    /* format 17 */
    {0xDE00, 0xFF00, OP_THUMB_UNDEFINED, "undefined"},
    {0xDF00, 0xFF00, OP_THUMB_SWI, "swi\t%0-7d"},
    /* format 9 */
    {0x6000, 0xF800, OP_THUMB_STR, "str\t%0-2r, [%3-5r, #%6-10W]"},
    {0x6800, 0xF800, OP_THUMB_LDR, "ldr\t%0-2r, [%3-5r, #%6-10W]"},
    {0x7000, 0xF800, OP_THUMB_STRB, "strb\t%0-2r, [%3-5r, #%6-10d]"},
    {0x7800, 0xF800, OP_THUMB_LDRB, "ldrb\t%0-2r, [%3-5r, #%6-10d]"},
    /* the rest */
    {0x0000, 0x0000, OP_THUMB_UNDEFINED, "undefined instruction %0-15x"},
    {0x0000, 0x0000, OP_END, 0}
};

#define BDISP23(x,y) ((((((x) & 0x07ff) << 11) | ((y) & 0x07ff)) \
                     ^ 0x200000) - 0x200000) /* 23bit */

static const char * arm_conditional[] =
{"eq", "ne", "cs", "cc", "mi", "pl", "vs", "vc",
 "hi", "ls", "ge", "lt", "gt", "le", "", "nv"};

typedef struct
{
  const char * name;
  const char * description;
  const char * reg_names[16];
}
arm_regname;

static arm_regname regnames[] =
{
  { "raw" , "Select raw register names",
    { "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"}},
  { "gcc",  "Select register names used by GCC",
    { "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "sl",  "fp",  "ip",  "sp",  "lr",  "pc" }},
  { "std",  "Select register names used in ARM's ISA documentation",
    { "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10", "r11", "r12", "sp",  "lr",  "pc" }},
  { "apcs", "Select register names used in the APCS",
    { "a1", "a2", "a3", "a4", "v1", "v2", "v3", "v4", "v5", "v6", "sl",  "fp",  "ip",  "sp",  "lr",  "pc" }},
  { "atpcs", "Select register names used in the ATPCS",
    { "a1", "a2", "a3", "a4", "v1", "v2", "v3", "v4", "v5", "v6", "v7",  "v8",  "IP",  "SP",  "LR",  "PC" }},
  { "special-atpcs", "Select special register names used in the ATPCS",
    { "a1", "a2", "a3", "a4", "v1", "v2", "v3", "WR", "v5", "SB", "SL",  "FP",  "IP",  "SP",  "LR",  "PC" }}
};

/* Default to STD register name set.  */
static unsigned int regname_selected = 2;

#define NUM_ARM_REGNAMES  NUM_ELEM (regnames)
#define arm_regnames      regnames[regname_selected].reg_names

Opcode decode_insn_thumb(uint32_t given)
{
    struct thumb_opcode * insn;

    for (insn = thumb_opcodes; insn->assembler; insn++) {
        if ((given & insn->mask) == insn->value)
            return insn->opcode;
    }
    return OP_THUMB_UNDEFINED;
}

// Generates the disassembly string for the thumb instruction "insn1".
// If "insn1" is a BL or BLX instruction that is the first of two Thumb
// instructions, then insn2 is the second of two instructions.  Otherwise,
// insn2 is ignored.
char *disasm_insn_thumb(uint32_t pc, uint32_t insn1, uint32_t insn2, char *result)
{
    struct thumb_opcode * insn;
    static char buf[80];
    char *ptr;
    uint32_t addr;
    int len;

    if (result == NULL)
        result = buf;
    ptr = result;

    for (insn = thumb_opcodes; insn->assembler; insn++) {
        if ((insn1 & insn->mask) != insn->value)
            continue;

        const char * c = insn->assembler;

        /* Special processing for Thumb 2-instruction BL sequence:  */
        if (!*c) { /* Check for empty (not NULL) assembler string.  */
            uint32_t offset;

            offset = BDISP23 (insn1, insn2);
            offset = offset * 2 + pc + 4;
            
            if ((insn2 & 0x1000) == 0) {
                len = sprintf(ptr, "blx\t");
                offset &= 0xfffffffc;
            } else {
                len = sprintf(ptr, "bl\t");
            }
            ptr += len;
            
            sprintf(ptr, "0x%x", offset);
            return result;
        }
        
        insn1 &= 0xffff;
        
        for (; *c; c++) {
            if (*c != '%') {
                len = sprintf(ptr, "%c", *c);
                ptr += len;
                continue;
            }

            int domaskpc = 0;
            int domasklr = 0;
            
            switch (*++c) {
                case '%':
                    len = sprintf(ptr, "%%");
                    ptr += len;
                    break;
                    
                case 'S': {
                    uint32_t reg;
                    
                    reg = (insn1 >> 3) & 0x7;
                    if (insn1 & (1 << 6))
                        reg += 8;
                    
                    len = sprintf(ptr, "%s", arm_regnames[reg]);
                    ptr += len;
                    break;
                }
                    
                case 'D': {
                    uint32_t reg;
                    
                    reg = insn1 & 0x7;
                    if (insn1 & (1 << 7))
                        reg += 8;
                    
                    len = sprintf(ptr, "%s", arm_regnames[reg]);
                    ptr += len;
                    break;
                }
                    
                case 'T':
                    len = sprintf(ptr, "%s",
                          arm_conditional [(insn1 >> 8) & 0xf]);
                    ptr += len;
                    break;
                    
                case 'N':
                    if (insn1 & (1 << 8))
                        domasklr = 1;
                    /* Fall through.  */
                case 'O':
                    if (*c == 'O' && (insn1 & (1 << 8)))
                        domaskpc = 1;
                    /* Fall through.  */
                case 'M': {
                    int started = 0;
                    int reg;
                    
                    len = sprintf(ptr, "{");
                    ptr += len;
                    
                    /* It would be nice if we could spot
                       ranges, and generate the rS-rE format: */
                    for (reg = 0; (reg < 8); reg++)
                        if ((insn1 & (1 << reg)) != 0) {
                            if (started) {
                                len = sprintf(ptr, ", ");
                                ptr += len;
                            }
                            started = 1;
                            len = sprintf(ptr, "%s", arm_regnames[reg]);
                            ptr += len;
                        }
                    
                    if (domasklr) {
                        if (started) {
                            len = sprintf(ptr, ", ");
                            ptr += len;
                        }
                        started = 1;
                        len = sprintf(ptr, arm_regnames[14] /* "lr" */);
                        ptr += len;
                    }
                    
                    if (domaskpc) {
                        if (started) {
                            len = sprintf(ptr, ", ");
                            ptr += len;
                        }
                        len = sprintf(ptr, arm_regnames[15] /* "pc" */);
                        ptr += len;
                    }
                    
                    len = sprintf(ptr, "}");
                    ptr += len;
                    break;
                }
                    
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9': {
                    int bitstart = *c++ - '0';
                    int bitend = 0;
                    
                    while (*c >= '0' && *c <= '9')
                        bitstart = (bitstart * 10) + *c++ - '0';
                    
                    switch (*c) {
                        case '-': {
                            uint32_t reg;
                            
                            c++;
                            while (*c >= '0' && *c <= '9')
                                bitend = (bitend * 10) + *c++ - '0';
                            if (!bitend)
                                abort ();
                            reg = insn1 >> bitstart;
                            reg &= (2 << (bitend - bitstart)) - 1;
                            switch (*c) {
                                case 'r':
                                    len = sprintf(ptr, "%s", arm_regnames[reg]);
                                    break;
                                    
                                case 'd':
                                    len = sprintf(ptr, "%d", reg);
                                    break;
                                    
                                case 'H':
                                    len = sprintf(ptr, "%d", reg << 1);
                                    break;
                                    
                                case 'W':
                                    len = sprintf(ptr, "%d", reg << 2);
                                    break;
                                    
                                case 'a':
                                    /* PC-relative address -- the bottom two
                                       bits of the address are dropped
                                       before the calculation.  */
                                    addr = ((pc + 4) & ~3) + (reg << 2);
                                    len = sprintf(ptr, "0x%x", addr);
                                    break;
                                    
                                case 'x':
                                    len = sprintf(ptr, "0x%04x", reg);
                                    break;
                                    
                                case 'I':
                                    reg = ((reg ^ (1 << bitend)) - (1 << bitend));
                                    len = sprintf(ptr, "%d", reg);
                                    break;
                                    
                                case 'B':
                                    reg = ((reg ^ (1 << bitend)) - (1 << bitend));
                                    addr = reg * 2 + pc + 4;
                                    len = sprintf(ptr, "0x%x", addr);
                                    break;
                                    
                                default:
                                    abort ();
                            }
                            ptr += len;
                            break;
                        }
                            
                        case '\'':
                            c++;
                            if ((insn1 & (1 << bitstart)) != 0) {
                                len = sprintf(ptr, "%c", *c);
                                ptr += len;
                            }
                            break;
                            
                        case '?':
                            ++c;
                            if ((insn1 & (1 << bitstart)) != 0)
                                len = sprintf(ptr, "%c", *c++);
                            else
                                len = sprintf(ptr, "%c", *++c);
                            ptr += len;
                            break;
                            
                        default:
                            abort ();
                    }
                    break;
                }
                    
                default:
                    abort ();
            }
        }
        return result;
    }
    
    /* No match.  */
    abort ();
}

/****************************************************************************
 ****************************************************************************
 ***
 ***   This header was automatically generated from a Linux kernel header
 ***   of the same name, to make information necessary for userspace to
 ***   call into the kernel available to libc.  It contains only constants,
 ***   structures, and macros generated from the original header, and thus,
 ***   contains no copyrightable information.
 ***
 ***   To edit the content of this header, modify the corresponding
 ***   source file (e.g. under external/kernel-headers/original/) then
 ***   run bionic/libc/kernel/tools/update_all.py
 ***
 ***   Any manual change here will be lost the next time this script will
 ***   be run. You've been warned!
 ***
 ****************************************************************************
 ****************************************************************************/
#ifndef __ASM_CPU_FEATURES_H
#define __ASM_CPU_FEATURES_H
#include <asm/cpu.h>
#include <asm/cpu-info.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <cpu-feature-overrides.h>
#ifndef current_cpu_type
#define current_cpu_type() current_cpu_data.cputype
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_tlb
#define cpu_has_tlb (cpu_data[0].options & MIPS_CPU_TLB)
#endif
#ifndef cpu_has_4kex
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_4kex (cpu_data[0].options & MIPS_CPU_4KEX)
#endif
#ifndef cpu_has_3k_cache
#define cpu_has_3k_cache (cpu_data[0].options & MIPS_CPU_3K_CACHE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#define cpu_has_6k_cache 0
#define cpu_has_8k_cache 0
#ifndef cpu_has_4k_cache
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_4k_cache (cpu_data[0].options & MIPS_CPU_4K_CACHE)
#endif
#ifndef cpu_has_tx39_cache
#define cpu_has_tx39_cache (cpu_data[0].options & MIPS_CPU_TX39_CACHE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_fpu
#define cpu_has_fpu (current_cpu_data.options & MIPS_CPU_FPU)
#define raw_cpu_has_fpu (raw_current_cpu_data.options & MIPS_CPU_FPU)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#else
#define raw_cpu_has_fpu cpu_has_fpu
#endif
#ifndef cpu_has_32fpr
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_32fpr (cpu_data[0].options & MIPS_CPU_32FPR)
#endif
#ifndef cpu_has_counter
#define cpu_has_counter (cpu_data[0].options & MIPS_CPU_COUNTER)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_watch
#define cpu_has_watch (cpu_data[0].options & MIPS_CPU_WATCH)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_divec
#define cpu_has_divec (cpu_data[0].options & MIPS_CPU_DIVEC)
#endif
#ifndef cpu_has_vce
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_vce (cpu_data[0].options & MIPS_CPU_VCE)
#endif
#ifndef cpu_has_cache_cdex_p
#define cpu_has_cache_cdex_p (cpu_data[0].options & MIPS_CPU_CACHE_CDEX_P)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_cache_cdex_s
#define cpu_has_cache_cdex_s (cpu_data[0].options & MIPS_CPU_CACHE_CDEX_S)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_prefetch
#define cpu_has_prefetch (cpu_data[0].options & MIPS_CPU_PREFETCH)
#endif
#ifndef cpu_has_mcheck
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_mcheck (cpu_data[0].options & MIPS_CPU_MCHECK)
#endif
#ifndef cpu_has_ejtag
#define cpu_has_ejtag (cpu_data[0].options & MIPS_CPU_EJTAG)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_llsc
#define cpu_has_llsc (cpu_data[0].options & MIPS_CPU_LLSC)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_mips16
#define cpu_has_mips16 (cpu_data[0].ases & MIPS_ASE_MIPS16)
#endif
#ifndef cpu_has_mdmx
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_mdmx (cpu_data[0].ases & MIPS_ASE_MDMX)
#endif
#ifndef cpu_has_mips3d
#define cpu_has_mips3d (cpu_data[0].ases & MIPS_ASE_MIPS3D)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_smartmips
#define cpu_has_smartmips (cpu_data[0].ases & MIPS_ASE_SMARTMIPS)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_vtag_icache
#define cpu_has_vtag_icache (cpu_data[0].icache.flags & MIPS_CACHE_VTAG)
#endif
#ifndef cpu_has_dc_aliases
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_dc_aliases (cpu_data[0].dcache.flags & MIPS_CACHE_ALIASES)
#endif
#ifndef cpu_has_ic_fills_f_dc
#define cpu_has_ic_fills_f_dc (cpu_data[0].icache.flags & MIPS_CACHE_IC_F_DC)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_pindexed_dcache
#define cpu_has_pindexed_dcache (cpu_data[0].dcache.flags & MIPS_CACHE_PINDEX)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_icache_snoops_remote_store
#define cpu_icache_snoops_remote_store 1
#endif
#ifndef cpu_has_mips32r1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_mips32r1 (cpu_data[0].isa_level & MIPS_CPU_ISA_M32R1)
#endif
#ifndef cpu_has_mips32r2
#define cpu_has_mips32r2 (cpu_data[0].isa_level & MIPS_CPU_ISA_M32R2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_mips64r1
#define cpu_has_mips64r1 (cpu_data[0].isa_level & MIPS_CPU_ISA_M64R1)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_mips64r2
#define cpu_has_mips64r2 (cpu_data[0].isa_level & MIPS_CPU_ISA_M64R2)
#endif
#define cpu_has_mips32 (cpu_has_mips32r1 | cpu_has_mips32r2)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_mips64 (cpu_has_mips64r1 | cpu_has_mips64r2)
#define cpu_has_mips_r1 (cpu_has_mips32r1 | cpu_has_mips64r1)
#define cpu_has_mips_r2 (cpu_has_mips32r2 | cpu_has_mips64r2)
#ifndef cpu_has_dsp
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_dsp (cpu_data[0].ases & MIPS_ASE_DSP)
#endif
#ifndef cpu_has_mipsmt
#define cpu_has_mipsmt (cpu_data[0].ases & MIPS_ASE_MIPSMT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_userlocal
#define cpu_has_userlocal (cpu_data[0].options & MIPS_CPU_ULRI)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_nofpuex
#define cpu_has_nofpuex (cpu_data[0].options & MIPS_CPU_NOFPUEX)
#endif
#ifndef cpu_has_64bits
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_64bits (cpu_data[0].isa_level & MIPS_CPU_ISA_64BIT)
#endif
#ifndef cpu_has_64bit_zero_reg
#define cpu_has_64bit_zero_reg (cpu_data[0].isa_level & MIPS_CPU_ISA_64BIT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_64bit_gp_regs
#define cpu_has_64bit_gp_regs 0
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_has_64bit_addresses
#define cpu_has_64bit_addresses 0
#endif
#ifndef cpu_has_vint
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_has_vint 0
#endif
#ifndef cpu_has_veic
#define cpu_has_veic 0
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#ifndef cpu_has_inclusive_pcaches
#define cpu_has_inclusive_pcaches (cpu_data[0].options & MIPS_CPU_INCLUSIVE_CACHES)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#ifndef cpu_dcache_line_size
#define cpu_dcache_line_size() cpu_data[0].dcache.linesz
#endif
#ifndef cpu_icache_line_size
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define cpu_icache_line_size() cpu_data[0].icache.linesz
#endif
#ifndef cpu_scache_line_size
#define cpu_scache_line_size() cpu_data[0].scache.linesz
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#endif

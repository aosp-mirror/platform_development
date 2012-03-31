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
#ifndef _ASM_SN_ARCH_H
#define _ASM_SN_ARCH_H
#include <linux/types.h>
#include <asm/sn/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef u64 hubreg_t;
#define cputonasid(cpu) (sn_cpu_info[(cpu)].p_nasid)
#define cputoslice(cpu) (sn_cpu_info[(cpu)].p_slice)
#define makespnum(_nasid, _slice)   (((_nasid) << CPUS_PER_NODE_SHFT) | (_slice))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define INVALID_NASID (nasid_t)-1
#define INVALID_CNODEID (cnodeid_t)-1
#define INVALID_PNODEID (pnodeid_t)-1
#define INVALID_MODULE (moduleid_t)-1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define INVALID_PARTID (partid_t)-1
#define NASID_TO_REGION(nnode)   ((nnode) >>   (is_fine_dirmode() ? NASID_TO_FINEREG_SHFT : NASID_TO_COARSEREG_SHFT))
#define NASID_TO_COMPACT_NODEID(nnode) (nasid_to_compact_node[nnode])
#define COMPACT_TO_NASID_NODEID(cnode) (compact_to_nasid_node[cnode])
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CPUID_TO_COMPACT_NODEID(cpu) (cpuid_to_compact_node[(cpu)])
#endif

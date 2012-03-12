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
#ifndef _ASM_SN_ADDRS_H
#define _ASM_SN_ADDRS_H
#ifndef __ASSEMBLY__
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif
#include <asm/addrspace.h>
#include <asm/sn/kldir.h>
#ifndef __ASSEMBLY__
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PS_UINT_CAST (unsigned long)
#define UINT64_CAST (unsigned long)
#define HUBREG_CAST (volatile hubreg_t *)
#else
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PS_UINT_CAST
#define UINT64_CAST
#define HUBREG_CAST
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NASID_GET_META(_n) ((_n) >> NASID_LOCAL_BITS)
#define NASID_MAKE(_m, _l) (((_m) << NASID_LOCAL_BITS) | (_l))
#define NODE_ADDRSPACE_MASK (NODE_ADDRSPACE_SIZE - 1)
#define TO_NODE_ADDRSPACE(_pa) (UINT64_CAST (_pa) & NODE_ADDRSPACE_MASK)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CHANGE_ADDR_NASID(_pa, _nasid)   ((UINT64_CAST(_pa) & ~NASID_MASK) |   (UINT64_CAST(_nasid) << NASID_SHFT))
#define NODE_OFFSET(_n) (UINT64_CAST (_n) << NODE_SIZE_BITS)
#define NODE_CAC_BASE(_n) (CAC_BASE + NODE_OFFSET(_n))
#define NODE_HSPEC_BASE(_n) (HSPEC_BASE + NODE_OFFSET(_n))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NODE_IO_BASE(_n) (IO_BASE + NODE_OFFSET(_n))
#define NODE_MSPEC_BASE(_n) (MSPEC_BASE + NODE_OFFSET(_n))
#define NODE_UNCAC_BASE(_n) (UNCAC_BASE + NODE_OFFSET(_n))
#define TO_NODE(_n, _x) (NODE_OFFSET(_n) | ((_x) ))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define TO_NODE_CAC(_n, _x) (NODE_CAC_BASE(_n) | ((_x) & TO_PHYS_MASK))
#define TO_NODE_UNCAC(_n, _x) (NODE_UNCAC_BASE(_n) | ((_x) & TO_PHYS_MASK))
#define TO_NODE_MSPEC(_n, _x) (NODE_MSPEC_BASE(_n) | ((_x) & TO_PHYS_MASK))
#define TO_NODE_HSPEC(_n, _x) (NODE_HSPEC_BASE(_n) | ((_x) & TO_PHYS_MASK))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define RAW_NODE_SWIN_BASE(nasid, widget)   (NODE_IO_BASE(nasid) + (UINT64_CAST(widget) << SWIN_SIZE_BITS))
#define WIDGETID_GET(addr) ((unsigned char)((addr >> SWIN_SIZE_BITS) & 0xff))
#define SWIN_SIZE_BITS 24
#define SWIN_SIZE (UINT64_CAST 1 << 24)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SWIN_SIZEMASK (SWIN_SIZE - 1)
#define SWIN_WIDGET_MASK 0xF
#define SWIN_WIDGETADDR(addr) ((addr) & SWIN_SIZEMASK)
#define SWIN_WIDGETNUM(addr) (((addr) >> SWIN_SIZE_BITS) & SWIN_WIDGET_MASK)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NODE_SWIN_ADDR(nasid, addr)   (((addr) >= NODE_SWIN_BASE(nasid, 0)) &&   ((addr) < (NODE_SWIN_BASE(nasid, HUB_NUM_WIDGET) + SWIN_SIZE)  ))
#define UALIAS_BASE HSPEC_BASE
#define UALIAS_SIZE 0x10000000  
#define UALIAS_LIMIT (UALIAS_BASE + UALIAS_SIZE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HUB_REGISTER_WIDGET 1
#define IALIAS_BASE NODE_SWIN_BASE(0, HUB_REGISTER_WIDGET)
#define IALIAS_SIZE 0x800000  
#define IS_IALIAS(_a) (((_a) >= IALIAS_BASE) &&   ((_a) < (IALIAS_BASE + IALIAS_SIZE)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define NODE_BDOOR_BASE(_n) (NODE_HSPEC_BASE(_n) + (NODE_ADDRSPACE_SIZE/2))
#define NODE_BDECC_BASE(_n) (NODE_BDOOR_BASE(_n))
#define NODE_BDDIR_BASE(_n) (NODE_BDOOR_BASE(_n) + (NODE_ADDRSPACE_SIZE/4))
#define LOCAL_HUB_ADDR(_x) (HUBREG_CAST (IALIAS_BASE + (_x)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define REMOTE_HUB_ADDR(_n, _x) (HUBREG_CAST (NODE_SWIN_BASE(_n, 1) +   0x800000 + (_x)))
#ifndef __ASSEMBLY__
#define HUB_L(_a) *(_a)
#define HUB_S(_a, _d) *(_a) = (_d)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LOCAL_HUB_L(_r) HUB_L(LOCAL_HUB_ADDR(_r))
#define LOCAL_HUB_S(_r, _d) HUB_S(LOCAL_HUB_ADDR(_r), (_d))
#define REMOTE_HUB_L(_n, _r) HUB_L(REMOTE_HUB_ADDR((_n), (_r)))
#define REMOTE_HUB_S(_n, _r, _d) HUB_S(REMOTE_HUB_ADDR((_n), (_r)), (_d))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define REMOTE_HUB_PI_L(_n, _sn, _r) HUB_L(REMOTE_HUB_PI_ADDR((_n), (_sn), (_r)))
#define REMOTE_HUB_PI_S(_n, _sn, _r, _d) HUB_S(REMOTE_HUB_PI_ADDR((_n), (_sn), (_r)), (_d))
#endif
#define HUB_REG_PTR(_base, _off)   (HUBREG_CAST((__psunsigned_t)(_base) + (__psunsigned_t)(_off)))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define HUB_REG_PTR_L(_base, _off)   HUB_L(HUB_REG_PTR((_base), (_off)))
#define HUB_REG_PTR_S(_base, _off, _data)   HUB_S(HUB_REG_PTR((_base), (_off)), (_data))
#define PHYS_RAMBASE 0x0
#define K0_RAMBASE PHYS_TO_K0(PHYS_RAMBASE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EX_HANDLER_OFFSET(slice) ((slice) << 16)
#define EX_HANDLER_ADDR(nasid, slice)   PHYS_TO_K0(NODE_OFFSET(nasid) | EX_HANDLER_OFFSET(slice))
#define EX_HANDLER_SIZE 0x0400
#define EX_FRAME_OFFSET(slice) ((slice) << 16 | 0x400)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define EX_FRAME_ADDR(nasid, slice)   PHYS_TO_K0(NODE_OFFSET(nasid) | EX_FRAME_OFFSET(slice))
#define EX_FRAME_SIZE 0x0c00
#define ARCS_SPB_OFFSET 0x1000
#define ARCS_SPB_ADDR(nasid)   PHYS_TO_K0(NODE_OFFSET(nasid) | ARCS_SPB_OFFSET)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define ARCS_SPB_SIZE 0x0400
#define KLDIR_OFFSET 0x2000
#define KLDIR_ADDR(nasid)   TO_NODE_UNCAC((nasid), KLDIR_OFFSET)
#define KLDIR_SIZE 0x0400
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLI_LAUNCH 0  
#define KLI_KLCONFIG 1
#define KLI_NMI 2
#define KLI_GDA 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLI_FREEMEM 4
#define KLI_SYMMON_STK 5
#define KLI_PI_ERROR 6
#define KLI_KERN_VARS 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLI_KERN_XP 8
#define KLI_KERN_PARTID 9
#ifndef __ASSEMBLY__
#define KLD_BASE(nasid) ((kldir_ent_t *) KLDIR_ADDR(nasid))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLD_LAUNCH(nasid) (KLD_BASE(nasid) + KLI_LAUNCH)
#define KLD_NMI(nasid) (KLD_BASE(nasid) + KLI_NMI)
#define KLD_KLCONFIG(nasid) (KLD_BASE(nasid) + KLI_KLCONFIG)
#define KLD_PI_ERROR(nasid) (KLD_BASE(nasid) + KLI_PI_ERROR)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLD_GDA(nasid) (KLD_BASE(nasid) + KLI_GDA)
#define KLD_SYMMON_STK(nasid) (KLD_BASE(nasid) + KLI_SYMMON_STK)
#define KLD_FREEMEM(nasid) (KLD_BASE(nasid) + KLI_FREEMEM)
#define KLD_KERN_VARS(nasid) (KLD_BASE(nasid) + KLI_KERN_VARS)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLD_KERN_XP(nasid) (KLD_BASE(nasid) + KLI_KERN_XP)
#define KLD_KERN_PARTID(nasid) (KLD_BASE(nasid) + KLI_KERN_PARTID)
#define LAUNCH_OFFSET(nasid, slice)   (KLD_LAUNCH(nasid)->offset +   KLD_LAUNCH(nasid)->stride * (slice))
#define LAUNCH_ADDR(nasid, slice)   TO_NODE_UNCAC((nasid), LAUNCH_OFFSET(nasid, slice))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LAUNCH_SIZE(nasid) KLD_LAUNCH(nasid)->size
#define NMI_OFFSET(nasid, slice)   (KLD_NMI(nasid)->offset +   KLD_NMI(nasid)->stride * (slice))
#define NMI_ADDR(nasid, slice)   TO_NODE_UNCAC((nasid), NMI_OFFSET(nasid, slice))
#define NMI_SIZE(nasid) KLD_NMI(nasid)->size
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KLCONFIG_OFFSET(nasid) KLD_KLCONFIG(nasid)->offset
#define KLCONFIG_ADDR(nasid)   TO_NODE_UNCAC((nasid), KLCONFIG_OFFSET(nasid))
#define KLCONFIG_SIZE(nasid) KLD_KLCONFIG(nasid)->size
#define GDA_ADDR(nasid) KLD_GDA(nasid)->pointer
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GDA_SIZE(nasid) KLD_GDA(nasid)->size
#define SYMMON_STK_OFFSET(nasid, slice)   (KLD_SYMMON_STK(nasid)->offset +   KLD_SYMMON_STK(nasid)->stride * (slice))
#define SYMMON_STK_STRIDE(nasid) KLD_SYMMON_STK(nasid)->stride
#define SYMMON_STK_ADDR(nasid, slice)   TO_NODE_CAC((nasid), SYMMON_STK_OFFSET(nasid, slice))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SYMMON_STK_SIZE(nasid) KLD_SYMMON_STK(nasid)->stride
#define SYMMON_STK_END(nasid) (SYMMON_STK_ADDR(nasid, 0) + KLD_SYMMON_STK(nasid)->size)
#define UNIX_DEBUG_LOADADDR 0x300000
#define SYMMON_LOADADDR(nasid)   TO_NODE(nasid, PHYS_TO_K0(UNIX_DEBUG_LOADADDR - 0x1000))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FREEMEM_OFFSET(nasid) KLD_FREEMEM(nasid)->offset
#define FREEMEM_ADDR(nasid) SYMMON_STK_END(nasid)
#define FREEMEM_SIZE(nasid) KLD_FREEMEM(nasid)->size
#define PI_ERROR_OFFSET(nasid) KLD_PI_ERROR(nasid)->offset
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PI_ERROR_ADDR(nasid)   TO_NODE_UNCAC((nasid), PI_ERROR_OFFSET(nasid))
#define PI_ERROR_SIZE(nasid) KLD_PI_ERROR(nasid)->size
#define NODE_OFFSET_TO_K0(_nasid, _off)   PHYS_TO_K0((NODE_OFFSET(_nasid) + (_off)) | CAC_BASE)
#define NODE_OFFSET_TO_K1(_nasid, _off)   TO_UNCAC((NODE_OFFSET(_nasid) + (_off)) | UNCAC_BASE)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K0_TO_NODE_OFFSET(_k0addr)   ((__psunsigned_t)(_k0addr) & NODE_ADDRSPACE_MASK)
#define KERN_VARS_ADDR(nasid) KLD_KERN_VARS(nasid)->pointer
#define KERN_VARS_SIZE(nasid) KLD_KERN_VARS(nasid)->size
#define KERN_XP_ADDR(nasid) KLD_KERN_XP(nasid)->pointer
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KERN_XP_SIZE(nasid) KLD_KERN_XP(nasid)->size
#define GPDA_ADDR(nasid) TO_NODE_CAC(nasid, GPDA_OFFSET)
#endif
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */

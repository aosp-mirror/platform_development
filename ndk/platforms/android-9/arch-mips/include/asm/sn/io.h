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
#ifndef _ASM_SN_IO_H
#define _ASM_SN_IO_H
#define IIO_ITTE_BASE 0x400160  
#define IIO_ITTE(bigwin) (IIO_ITTE_BASE + 8*(bigwin))
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ITTE_OFFSET_BITS 5  
#define IIO_ITTE_OFFSET_MASK ((1<<IIO_ITTE_OFFSET_BITS)-1)
#define IIO_ITTE_OFFSET_SHIFT 0
#define IIO_ITTE_WIDGET_BITS 4  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ITTE_WIDGET_MASK ((1<<IIO_ITTE_WIDGET_BITS)-1)
#define IIO_ITTE_WIDGET_SHIFT 8
#define IIO_ITTE_IOSP 1  
#define IIO_ITTE_IOSP_MASK 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ITTE_IOSP_SHIFT 12
#define HUB_PIO_MAP_TO_MEM 0
#define HUB_PIO_MAP_TO_IO 1
#define IIO_ITTE_INVALID_WIDGET 3  
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define IIO_ITTE_PUT(nasid, bigwin, io_or_mem, widget, addr)   REMOTE_HUB_S((nasid), IIO_ITTE(bigwin),   (((((addr) >> BWIN_SIZE_BITS) &   IIO_ITTE_OFFSET_MASK) << IIO_ITTE_OFFSET_SHIFT) |   (io_or_mem << IIO_ITTE_IOSP_SHIFT) |   (((widget) & IIO_ITTE_WIDGET_MASK) << IIO_ITTE_WIDGET_SHIFT)))
#define IIO_ITTE_DISABLE(nasid, bigwin)   IIO_ITTE_PUT((nasid), HUB_PIO_MAP_TO_MEM,   (bigwin), IIO_ITTE_INVALID_WIDGET, 0)
#define IIO_ITTE_GET(nasid, bigwin) REMOTE_HUB_ADDR((nasid), IIO_ITTE(bigwin))
#define IIO_IOPRB(_x) (IIO_IOPRB_0 + ( ( (_x) < HUB_WIDGET_ID_MIN ?   (_x) :   (_x) - (HUB_WIDGET_ID_MIN-1)) << 3) )
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif

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
#ifndef _UAPI_LINUX_KD_H
#define _UAPI_LINUX_KD_H
#include <linux/types.h>
#include <linux/compiler.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define GIO_FONT 0x4B60
#define PIO_FONT 0x4B61
#define GIO_FONTX 0x4B6B
#define PIO_FONTX 0x4B6C
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct consolefontdesc {
 unsigned short charcount;
 unsigned short charheight;
 char __user *chardata;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define PIO_FONTRESET 0x4B6D
#define GIO_CMAP 0x4B70
#define PIO_CMAP 0x4B71
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KIOCSOUND 0x4B2F
#define KDMKTONE 0x4B30
#define KDGETLED 0x4B31
#define KDSETLED 0x4B32
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define LED_SCR 0x01
#define LED_NUM 0x02
#define LED_CAP 0x04
#define KDGKBTYPE 0x4B33
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KB_84 0x01
#define KB_101 0x02
#define KB_OTHER 0x03
#define KDADDIO 0x4B34
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDDELIO 0x4B35
#define KDENABIO 0x4B36
#define KDDISABIO 0x4B37
#define KDSETMODE 0x4B3A
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KD_TEXT 0x00
#define KD_GRAPHICS 0x01
#define KD_TEXT0 0x02
#define KD_TEXT1 0x03
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDGETMODE 0x4B3B
#define KDMAPDISP 0x4B3C
#define KDUNMAPDISP 0x4B3D
typedef char scrnmap_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define E_TABSZ 256
#define GIO_SCRNMAP 0x4B40
#define PIO_SCRNMAP 0x4B41
#define GIO_UNISCRNMAP 0x4B69
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PIO_UNISCRNMAP 0x4B6A
#define GIO_UNIMAP 0x4B66
struct unipair {
 unsigned short unicode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned short fontpos;
};
struct unimapdesc {
 unsigned short entry_ct;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct unipair __user *entries;
};
#define PIO_UNIMAP 0x4B67
#define PIO_UNIMAPCLR 0x4B68
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct unimapinit {
 unsigned short advised_hashsize;
 unsigned short advised_hashstep;
 unsigned short advised_hashlevel;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define UNI_DIRECT_BASE 0xF000
#define UNI_DIRECT_MASK 0x01FF
#define K_RAW 0x00
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K_XLATE 0x01
#define K_MEDIUMRAW 0x02
#define K_UNICODE 0x03
#define K_OFF 0x04
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDGKBMODE 0x4B44
#define KDSKBMODE 0x4B45
#define K_METABIT 0x03
#define K_ESCPREFIX 0x04
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDGKBMETA 0x4B62
#define KDSKBMETA 0x4B63
#define K_SCROLLLOCK 0x01
#define K_NUMLOCK 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K_CAPSLOCK 0x04
#define KDGKBLED 0x4B64
#define KDSKBLED 0x4B65
struct kbentry {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char kb_table;
 unsigned char kb_index;
 unsigned short kb_value;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define K_NORMTAB 0x00
#define K_SHIFTTAB 0x01
#define K_ALTTAB 0x02
#define K_ALTSHIFTTAB 0x03
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDGKBENT 0x4B46
#define KDSKBENT 0x4B47
struct kbsentry {
 unsigned char kb_func;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char kb_string[512];
};
#define KDGKBSENT 0x4B48
#define KDSKBSENT 0x4B49
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct kbdiacr {
 unsigned char diacr, base, result;
};
struct kbdiacrs {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int kb_cnt;
 struct kbdiacr kbdiacr[256];
};
#define KDGKBDIACR 0x4B4A
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDSKBDIACR 0x4B4B
struct kbdiacruc {
 unsigned int diacr, base, result;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct kbdiacrsuc {
 unsigned int kb_cnt;
 struct kbdiacruc kbdiacruc[256];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDGKBDIACRUC 0x4BFA
#define KDSKBDIACRUC 0x4BFB
struct kbkeycode {
 unsigned int scancode, keycode;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define KDGETKEYCODE 0x4B4C
#define KDSETKEYCODE 0x4B4D
#define KDSIGACCEPT 0x4B4E
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct kbd_repeat {
 int delay;
 int period;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KDKBDREP 0x4B52
#define KDFONTOP 0x4B72
struct console_font_op {
 unsigned int op;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int flags;
 unsigned int width, height;
 unsigned int charcount;
 unsigned char __user *data;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct console_font {
 unsigned int width, height;
 unsigned int charcount;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char *data;
};
#define KD_FONT_OP_SET 0
#define KD_FONT_OP_GET 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define KD_FONT_OP_SET_DEFAULT 2
#define KD_FONT_OP_COPY 3
#define KD_FONT_FLAG_DONT_RECALC 1
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */

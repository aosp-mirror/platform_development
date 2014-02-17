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
#ifndef __UHID_H_
#define __UHID_H_
#include <linux/input.h>
#include <linux/types.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
enum uhid_event_type {
 UHID_CREATE,
 UHID_DESTROY,
 UHID_START,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 UHID_STOP,
 UHID_OPEN,
 UHID_CLOSE,
 UHID_OUTPUT,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 UHID_OUTPUT_EV,
 UHID_INPUT,
 UHID_FEATURE,
 UHID_FEATURE_ANSWER,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct uhid_create_req {
 __u8 name[128];
 __u8 phys[64];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 uniq[64];
 __u8 __user *rd_data;
 __u16 rd_size;
 __u16 bus;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 vendor;
 __u32 product;
 __u32 version;
 __u32 country;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((__packed__));
#define UHID_DATA_MAX 4096
enum uhid_report_type {
 UHID_FEATURE_REPORT,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 UHID_OUTPUT_REPORT,
 UHID_INPUT_REPORT,
};
struct uhid_input_req {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 data[UHID_DATA_MAX];
 __u16 size;
} __attribute__((__packed__));
struct uhid_output_req {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 data[UHID_DATA_MAX];
 __u16 size;
 __u8 rtype;
} __attribute__((__packed__));
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct uhid_output_ev_req {
 __u16 type;
 __u16 code;
 __s32 value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} __attribute__((__packed__));
struct uhid_feature_req {
 __u32 id;
 __u8 rnum;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 rtype;
} __attribute__((__packed__));
struct uhid_feature_answer_req {
 __u32 id;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u16 err;
 __u16 size;
 __u8 data[UHID_DATA_MAX];
} __attribute__((__packed__));
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct uhid_event {
 __u32 type;
 union {
 struct uhid_create_req create;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct uhid_input_req input;
 struct uhid_output_req output;
 struct uhid_output_ev_req output_ev;
 struct uhid_feature_req feature;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct uhid_feature_answer_req feature_answer;
 } u;
} __attribute__((__packed__));
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */

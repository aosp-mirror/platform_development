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
#define CHET_MT 0
#define CHET_ST 1
#define CHET_IE 2
#define CHET_DT 3
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CHET_V1 4
#define CHET_V2 5
#define CHET_V3 6
#define CHET_V4 7
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct changer_params {
 int cp_curpicker;
 int cp_npickers;
 int cp_nslots;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cp_nportals;
 int cp_ndrives;
};
struct changer_vendor_params {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cvp_n1;
 char cvp_label1[16];
 int cvp_n2;
 char cvp_label2[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cvp_n3;
 char cvp_label3[16];
 int cvp_n4;
 char cvp_label4[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int reserved[8];
};
struct changer_move {
 int cm_fromtype;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cm_fromunit;
 int cm_totype;
 int cm_tounit;
 int cm_flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define CM_INVERT 1
struct changer_exchange {
 int ce_srctype;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int ce_srcunit;
 int ce_fdsttype;
 int ce_fdstunit;
 int ce_sdsttype;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int ce_sdstunit;
 int ce_flags;
};
#define CE_INVERT1 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CE_INVERT2 2
struct changer_position {
 int cp_type;
 int cp_unit;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cp_flags;
};
#define CP_INVERT 1
struct changer_element_status {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int ces_type;
 unsigned char __user *ces_data;
};
#define CESTATUS_FULL 0x01
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CESTATUS_IMPEXP 0x02
#define CESTATUS_EXCEPT 0x04
#define CESTATUS_ACCESS 0x08
#define CESTATUS_EXENAB 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CESTATUS_INENAB 0x20
struct changer_get_element {
 int cge_type;
 int cge_unit;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cge_status;
 int cge_errno;
 int cge_srctype;
 int cge_srcunit;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cge_id;
 int cge_lun;
 char cge_pvoltag[36];
 char cge_avoltag[36];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int cge_flags;
};
#define CGE_ERRNO 0x01
#define CGE_INVERT 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CGE_SRC 0x04
#define CGE_IDLUN 0x08
#define CGE_PVOLTAG 0x10
#define CGE_AVOLTAG 0x20
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct changer_set_voltag {
 int csv_type;
 int csv_unit;
 char csv_voltag[36];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int csv_flags;
};
#define CSV_PVOLTAG 0x01
#define CSV_AVOLTAG 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CSV_CLEARTAG 0x04
#define CHIOMOVE _IOW('c', 1,struct changer_move)
#define CHIOEXCHANGE _IOW('c', 2,struct changer_exchange)
#define CHIOPOSITION _IOW('c', 3,struct changer_position)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CHIOGPICKER _IOR('c', 4,int)
#define CHIOSPICKER _IOW('c', 5,int)
#define CHIOGPARAMS _IOR('c', 6,struct changer_params)
#define CHIOGSTATUS _IOW('c', 8,struct changer_element_status)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define CHIOGELEM _IOW('c',16,struct changer_get_element)
#define CHIOINITELEM _IO('c',17)
#define CHIOSVOLTAG _IOW('c',18,struct changer_set_voltag)
#define CHIOGVPARAMS _IOR('c',19,struct changer_vendor_params)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */

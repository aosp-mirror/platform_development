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
#ifndef _LINUX_SOM_H
#define _LINUX_SOM_H
#include <linux/time.h>
#define SOM_PAGESIZE 4096
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct som_hdr {
 short system_id;
 short a_magic;
 unsigned int version_id;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct timespec file_time;
 unsigned int entry_space;
 unsigned int entry_subspace;
 unsigned int entry_offset;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int aux_header_location;
 unsigned int aux_header_size;
 unsigned int som_length;
 unsigned int presumed_dp;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int space_location;
 unsigned int space_total;
 unsigned int subspace_location;
 unsigned int subspace_total;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int loader_fixup_location;
 unsigned int loader_fixup_total;
 unsigned int space_strings_location;
 unsigned int space_strings_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int init_array_location;
 unsigned int init_array_total;
 unsigned int compiler_location;
 unsigned int compiler_total;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int symbol_location;
 unsigned int symbol_total;
 unsigned int fixup_request_location;
 unsigned int fixup_request_total;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int symbol_strings_location;
 unsigned int symbol_strings_size;
 unsigned int unloadable_sp_location;
 unsigned int unloadable_sp_size;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int checksum;
};
#define SOM_SID_PARISC_1_0 0x020b
#define SOM_SID_PARISC_1_1 0x0210
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SOM_SID_PARISC_2_0 0x0214
#define SOM_LIB_EXEC 0x0104
#define SOM_RELOCATABLE 0x0106
#define SOM_EXEC_NONSHARE 0x0107
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SOM_EXEC_SHARE 0x0108
#define SOM_EXEC_DEMAND 0x010B
#define SOM_LIB_DYN 0x010D
#define SOM_LIB_SHARE 0x010E
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define SOM_LIB_RELOC 0x0619
#define SOM_ID_OLD 85082112
#define SOM_ID_NEW 87102412
struct aux_id {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int mandatory :1;
 unsigned int copy :1;
 unsigned int append :1;
 unsigned int ignore :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int reserved :12;
 unsigned int type :16;
 unsigned int length;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct som_exec_auxhdr {
 struct aux_id som_auxhdr;
 int exec_tsize;
 int exec_tmem;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int exec_tfile;
 int exec_dsize;
 int exec_dmem;
 int exec_dfile;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int exec_bsize;
 int exec_entry;
 int exec_flags;
 int exec_bfill;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
union name_pt {
 char * n_name;
 unsigned int n_strx;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct space_dictionary_record {
 union name_pt name;
 unsigned int is_loadable :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int is_defined :1;
 unsigned int is_private :1;
 unsigned int has_intermediate_code :1;
 unsigned int is_tspecific :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int reserved :11;
 unsigned int sort_key :8;
 unsigned int reserved2 :8;
 int space_number;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int subspace_index;
 unsigned int subspace_quantity;
 int loader_fix_index;
 unsigned int loader_fix_quantity;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int init_pointer_index;
 unsigned int init_pointer_quantity;
};
struct subspace_dictionary_record {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int space_index;
 unsigned int access_control_bits :7;
 unsigned int memory_resident :1;
 unsigned int dup_common :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int is_common :1;
 unsigned int quadrant :2;
 unsigned int initially_frozen :1;
 unsigned int is_first :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int code_only :1;
 unsigned int sort_key :8;
 unsigned int replicate_init :1;
 unsigned int continuation :1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int is_tspecific :1;
 unsigned int is_comdat :1;
 unsigned int reserved :4;
 int file_loc_init_value;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int initialization_length;
 unsigned int subspace_start;
 unsigned int subspace_length;
 unsigned int reserved2 :5;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int alignment :27;
 union name_pt name;
 int fixup_request_index;
 unsigned int fixup_request_quantity;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#endif

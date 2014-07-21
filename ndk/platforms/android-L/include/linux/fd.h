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
#ifndef _UAPI_LINUX_FD_H
#define _UAPI_LINUX_FD_H
#include <linux/ioctl.h>
#include <linux/compiler.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct floppy_struct {
 unsigned int size,
 sect,
 head,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 track,
 stretch;
#define FD_STRETCH 1
#define FD_SWAPSIDES 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_ZEROBASED 4
#define FD_SECTBASEMASK 0x3FC
#define FD_MKSECTBASE(s) (((s) ^ 1) << 2)
#define FD_SECTBASE(floppy) ((((floppy)->stretch & FD_SECTBASEMASK) >> 2) ^ 1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char gap,
 rate,
#define FD_2M 0x4
#define FD_SIZECODEMASK 0x38
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_SIZECODE(floppy) (((((floppy)->rate&FD_SIZECODEMASK)>> 3)+ 2) %8)
#define FD_SECTSIZE(floppy) ( (floppy)->rate & FD_2M ?   512 : 128 << FD_SIZECODE(floppy) )
#define FD_PERP 0x40
 spec1,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 fmt_gap;
 const char * name;
};
#define FDCLRPRM _IO(2, 0x41)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDSETPRM _IOW(2, 0x42, struct floppy_struct)
#define FDSETMEDIAPRM FDSETPRM
#define FDDEFPRM _IOW(2, 0x43, struct floppy_struct)
#define FDGETPRM _IOR(2, 0x04, struct floppy_struct)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDDEFMEDIAPRM FDDEFPRM
#define FDGETMEDIAPRM FDGETPRM
#define FDMSGON _IO(2,0x45)
#define FDMSGOFF _IO(2,0x46)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_FILL_BYTE 0xF6
struct format_descr {
 unsigned int device,head,track;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDFMTBEG _IO(2,0x47)
#define FDFMTTRK _IOW(2,0x48, struct format_descr)
#define FDFMTEND _IO(2,0x49)
struct floppy_max_errors {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int
 abort,
 read_track,
 reset,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 recal,
 reporting;
};
#define FDSETEMSGTRESH _IO(2,0x4a)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDFLUSH _IO(2,0x4b)
#define FDSETMAXERRS _IOW(2, 0x4c, struct floppy_max_errors)
#define FDGETMAXERRS _IOR(2, 0x0e, struct floppy_max_errors)
typedef char floppy_drive_name[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDGETDRVTYP _IOR(2, 0x0f, floppy_drive_name)
struct floppy_drive_params {
 signed char cmos;
 unsigned long max_dtr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long hlt;
 unsigned long hut;
 unsigned long srt;
 unsigned long spinup;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long spindown;
 unsigned char spindown_offset;
 unsigned char select_delay;
 unsigned char rps;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char tracks;
 unsigned long timeout;
 unsigned char interleave_sect;
 struct floppy_max_errors max_errors;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 char flags;
#define FTD_MSG 0x10
#define FD_BROKEN_DCL 0x20
#define FD_DEBUG 0x02
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_SILENT_DCL_CLEAR 0x4
#define FD_INVERTED_DCL 0x80
 char read_track;
 short autodetect[8];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int checkfreq;
 int native_format;
};
enum {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FD_NEED_TWADDLE_BIT,
 FD_VERIFY_BIT,
 FD_DISK_NEWCHANGE_BIT,
 FD_UNUSED_BIT,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FD_DISK_CHANGED_BIT,
 FD_DISK_WRITABLE_BIT,
 FD_OPEN_SHOULD_FAIL_BIT
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDSETDRVPRM _IOW(2, 0x90, struct floppy_drive_params)
#define FDGETDRVPRM _IOR(2, 0x11, struct floppy_drive_params)
struct floppy_drive_struct {
 unsigned long flags;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_NEED_TWADDLE (1 << FD_NEED_TWADDLE_BIT)
#define FD_VERIFY (1 << FD_VERIFY_BIT)
#define FD_DISK_NEWCHANGE (1 << FD_DISK_NEWCHANGE_BIT)
#define FD_DISK_CHANGED (1 << FD_DISK_CHANGED_BIT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_DISK_WRITABLE (1 << FD_DISK_WRITABLE_BIT)
 unsigned long spinup_date;
 unsigned long select_date;
 unsigned long first_read_date;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 short probed_format;
 short track;
 short maxblock;
 short maxtrack;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int generation;
 int keep_data;
 int fd_ref;
 int fd_device;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned long last_checked;
 char *dmabuf;
 int bufblocks;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDGETDRVSTAT _IOR(2, 0x12, struct floppy_drive_struct)
#define FDPOLLDRVSTAT _IOR(2, 0x13, struct floppy_drive_struct)
enum reset_mode {
 FD_RESET_IF_NEEDED,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 FD_RESET_IF_RAWCMD,
 FD_RESET_ALWAYS
};
#define FDRESET _IO(2, 0x54)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct floppy_fdc_state {
 int spec1;
 int spec2;
 int dtr;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char version;
 unsigned char dor;
 unsigned long address;
 unsigned int rawcmd:2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int reset:1;
 unsigned int need_configure:1;
 unsigned int perp_mode:2;
 unsigned int has_fifo:1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned int driver_version;
#define FD_DRIVER_VERSION 0x100
 unsigned char track[4];
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FDGETFDCSTAT _IOR(2, 0x15, struct floppy_fdc_state)
struct floppy_write_errors {
 unsigned int write_errors;
 unsigned long first_error_sector;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int first_error_generation;
 unsigned long last_error_sector;
 int last_error_generation;
 unsigned int badness;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define FDWERRORCLR _IO(2, 0x56)
#define FDWERRORGET _IOR(2, 0x17, struct floppy_write_errors)
#define FDHAVEBATCHEDRAWCMD
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct floppy_raw_cmd {
 unsigned int flags;
#define FD_RAW_READ 1
#define FD_RAW_WRITE 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_RAW_NO_MOTOR 4
#define FD_RAW_DISK_CHANGE 4
#define FD_RAW_INTR 8
#define FD_RAW_SPIN 0x10
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_RAW_NO_MOTOR_AFTER 0x20
#define FD_RAW_NEED_DISK 0x40
#define FD_RAW_NEED_SEEK 0x80
#define FD_RAW_MORE 0x100
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_RAW_STOP_IF_FAILURE 0x200
#define FD_RAW_STOP_IF_SUCCESS 0x400
#define FD_RAW_SOFTFAILURE 0x800
#define FD_RAW_FAILURE 0x10000
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define FD_RAW_HARDFAILURE 0x20000
 void __user *data;
 char *kernel_data;
 struct floppy_raw_cmd *next;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 long length;
 long phys_length;
 int buffer_length;
 unsigned char rate;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 unsigned char cmd_count;
 unsigned char cmd[16];
 unsigned char reply_count;
 unsigned char reply[16];
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int track;
 int resultcode;
 int reserved1;
 int reserved2;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
#define FDRAWCMD _IO(2, 0x58)
#define FDTWADDLE _IO(2, 0x59)
#define FDEJECT _IO(2, 0x5a)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#endif

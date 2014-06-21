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
#ifndef _UAPI_DVBVIDEO_H_
#define _UAPI_DVBVIDEO_H_
#include <linux/types.h>
#include <stdint.h>
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#include <time.h>
typedef enum {
 VIDEO_FORMAT_4_3,
 VIDEO_FORMAT_16_9,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VIDEO_FORMAT_221_1
} video_format_t;
typedef enum {
 VIDEO_SYSTEM_PAL,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VIDEO_SYSTEM_NTSC,
 VIDEO_SYSTEM_PALN,
 VIDEO_SYSTEM_PALNc,
 VIDEO_SYSTEM_PALM,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VIDEO_SYSTEM_NTSC60,
 VIDEO_SYSTEM_PAL60,
 VIDEO_SYSTEM_PALM60
} video_system_t;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef enum {
 VIDEO_PAN_SCAN,
 VIDEO_LETTER_BOX,
 VIDEO_CENTER_CUT_OUT
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
} video_displayformat_t;
typedef struct {
 int w;
 int h;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 video_format_t aspect_ratio;
} video_size_t;
typedef enum {
 VIDEO_SOURCE_DEMUX,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VIDEO_SOURCE_MEMORY
} video_stream_source_t;
typedef enum {
 VIDEO_STOPPED,
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 VIDEO_PLAYING,
 VIDEO_FREEZED
} video_play_state_t;
#define VIDEO_CMD_PLAY (0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_CMD_STOP (1)
#define VIDEO_CMD_FREEZE (2)
#define VIDEO_CMD_CONTINUE (3)
#define VIDEO_CMD_FREEZE_TO_BLACK (1 << 0)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_CMD_STOP_TO_BLACK (1 << 0)
#define VIDEO_CMD_STOP_IMMEDIATELY (1 << 1)
#define VIDEO_PLAY_FMT_NONE (0)
#define VIDEO_PLAY_FMT_GOP (1)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct video_command {
 __u32 cmd;
 __u32 flags;
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 struct {
 __u64 pts;
 } stop;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __s32 speed;
 __u32 format;
 } play;
 struct {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 data[16];
 } raw;
 };
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_VSYNC_FIELD_UNKNOWN (0)
#define VIDEO_VSYNC_FIELD_ODD (1)
#define VIDEO_VSYNC_FIELD_EVEN (2)
#define VIDEO_VSYNC_FIELD_PROGRESSIVE (3)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct video_event {
 __s32 type;
#define VIDEO_EVENT_SIZE_CHANGED 1
#define VIDEO_EVENT_FRAME_RATE_CHANGED 2
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_EVENT_DECODER_STOPPED 3
#define VIDEO_EVENT_VSYNC 4
 __kernel_time_t timestamp;
 union {
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 video_size_t size;
 unsigned int frame_rate;
 unsigned char vsync_field;
 } u;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
};
struct video_status {
 int video_blank;
 video_play_state_t play_state;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 video_stream_source_t stream_source;
 video_format_t video_format;
 video_displayformat_t display_format;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
struct video_still_picture {
 char __user *iFrame;
 __s32 size;
};
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
typedef
struct video_highlight {
 int active;
 __u8 contrast1;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 contrast2;
 __u8 color1;
 __u8 color2;
 __u32 ypos;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u32 xpos;
} video_highlight_t;
typedef struct video_spu {
 int active;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 int stream_id;
} video_spu_t;
typedef struct video_spu_palette {
 int length;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 __user *palette;
} video_spu_palette_t;
typedef struct video_navi_pack {
 int length;
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
 __u8 data[1024];
} video_navi_pack_t;
typedef __u16 video_attributes_t;
#define VIDEO_CAP_MPEG1 1
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_CAP_MPEG2 2
#define VIDEO_CAP_SYS 4
#define VIDEO_CAP_PROG 8
#define VIDEO_CAP_SPU 16
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_CAP_NAVI 32
#define VIDEO_CAP_CSS 64
#define VIDEO_STOP _IO('o', 21)
#define VIDEO_PLAY _IO('o', 22)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_FREEZE _IO('o', 23)
#define VIDEO_CONTINUE _IO('o', 24)
#define VIDEO_SELECT_SOURCE _IO('o', 25)
#define VIDEO_SET_BLANK _IO('o', 26)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_GET_STATUS _IOR('o', 27, struct video_status)
#define VIDEO_GET_EVENT _IOR('o', 28, struct video_event)
#define VIDEO_SET_DISPLAY_FORMAT _IO('o', 29)
#define VIDEO_STILLPICTURE _IOW('o', 30, struct video_still_picture)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_FAST_FORWARD _IO('o', 31)
#define VIDEO_SLOWMOTION _IO('o', 32)
#define VIDEO_GET_CAPABILITIES _IOR('o', 33, unsigned int)
#define VIDEO_CLEAR_BUFFER _IO('o', 34)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_SET_ID _IO('o', 35)
#define VIDEO_SET_STREAMTYPE _IO('o', 36)
#define VIDEO_SET_FORMAT _IO('o', 37)
#define VIDEO_SET_SYSTEM _IO('o', 38)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_SET_HIGHLIGHT _IOW('o', 39, video_highlight_t)
#define VIDEO_SET_SPU _IOW('o', 50, video_spu_t)
#define VIDEO_SET_SPU_PALETTE _IOW('o', 51, video_spu_palette_t)
#define VIDEO_GET_NAVI _IOR('o', 52, video_navi_pack_t)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_SET_ATTRIBUTES _IO('o', 53)
#define VIDEO_GET_SIZE _IOR('o', 55, video_size_t)
#define VIDEO_GET_FRAME_RATE _IOR('o', 56, unsigned int)
#define VIDEO_GET_PTS _IOR('o', 57, __u64)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define VIDEO_GET_FRAME_COUNT _IOR('o', 58, __u64)
#define VIDEO_COMMAND _IOWR('o', 59, struct video_command)
#define VIDEO_TRY_COMMAND _IOWR('o', 60, struct video_command)
#endif
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */

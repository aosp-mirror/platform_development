/*
 * Copyright (C) 2014 The Android Open Source Project

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __ANDROID_DLEXT_H__
#define __ANDROID_DLEXT_H__

#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

/* bitfield definitions for android_dlextinfo.flags */
enum {
  /* When set, the reserved_addr and reserved_size fields must point to an
   * already-reserved region of address space which will be used to load the
   * library if it fits. If the reserved region is not large enough, the load
   * will fail.
   */
  ANDROID_DLEXT_RESERVED_ADDRESS      = 0x1,

  /* As DLEXT_RESERVED_ADDRESS, but if the reserved region is not large enough,
   * the linker will choose an available address instead.
   */
  ANDROID_DLEXT_RESERVED_ADDRESS_HINT = 0x2,

  /* When set, write the GNU RELRO section of the mapped library to relro_fd
   * after relocation has been performed, to allow it to be reused by another
   * process loading the same library at the same address. This implies
   * ANDROID_DLEXT_USE_RELRO.
   */
  ANDROID_DLEXT_WRITE_RELRO           = 0x4,

  /* When set, compare the GNU RELRO section of the mapped library to relro_fd
   * after relocation has been performed, and replace any relocated pages that
   * are identical with a version mapped from the file.
   */
  ANDROID_DLEXT_USE_RELRO             = 0x8,

  /* Mask of valid bits */
  ANDROID_DLEXT_VALID_FLAG_BITS       = ANDROID_DLEXT_RESERVED_ADDRESS |
                                        ANDROID_DLEXT_RESERVED_ADDRESS_HINT |
                                        ANDROID_DLEXT_WRITE_RELRO |
                                        ANDROID_DLEXT_USE_RELRO,
};

typedef struct {
  int     flags;
  void*   reserved_addr;
  size_t  reserved_size;
  int     relro_fd;
} android_dlextinfo;

extern void* android_dlopen_ext(const char* filename, int flag, const android_dlextinfo* extinfo);

__END_DECLS

#endif /* __ANDROID_DLEXT_H__ */

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
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

#ifndef LIBC_INCLUDE_MALLOC_H_
#define LIBC_INCLUDE_MALLOC_H_

/*
 * Declaration of malloc routines. Bionic uses dlmalloc (see
 * upstream-dlmalloc) but doesn't directly include it here to keep the
 * defined malloc.h interface small.
 */
#include <sys/cdefs.h>
#include <stddef.h>

__BEGIN_DECLS

extern __mallocfunc void*  malloc(size_t);
extern __mallocfunc void*  calloc(size_t, size_t);
extern void*  realloc(void *, size_t);
extern void   free(void *);

extern void*  memalign(size_t  alignment, size_t  bytesize);
extern size_t malloc_usable_size(const void*);

extern void*  valloc(size_t  bytesize);
extern void*  pvalloc(size_t bytesize);

#ifndef STRUCT_MALLINFO_DECLARED
#define STRUCT_MALLINFO_DECLARED 1
struct mallinfo {
  size_t arena;
  size_t ordblks;
  size_t smblks;
  size_t hblks;
  size_t hblkhd;
  size_t usmblks;
  size_t fsmblks;
  size_t uordblks;
  size_t fordblks;
  size_t keepcost;
};
#endif  /* STRUCT_MALLINFO_DECLARED */

extern struct mallinfo mallinfo(void);

__END_DECLS

#endif  /* LIBC_INCLUDE_MALLOC_H_ */

/*-
 * Copyright 2000 David E. O'Brien, John D. Polstra.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* The following trick defines int32_t w/o including <stdint.h>
   We want "int" to be 4-byte.  But just in case it isn't, the following
   line turns into "typedef int int32_t[0]" and later fails with error messages
   read like:

      crtbrand.c:83: error: excess elements in struct initializer
 */
typedef int int32_t[sizeof(int) == 4];

#define ABI_VENDOR	"Android"
#define ABI_SECTION	".note.android.ident"
#define ABI_NOTETYPE	1
#define ABI_ANDROID_API	PLATFORM_SDK_VERSION

/*
 * Special ".note" entry to tag an Android binary and specify the ABI version.
 *
 * For all arches except sparc, gcc emits the section directive for the
 * following struct with a PROGBITS type.  However, the section should be
 * of NOTE type, according to the Generic SysV ABI spec.
 *
 * Unfortunately, there is no clean way to tell gcc to use another section type,
 * so this C file (or the C file that includes it) must be compiled in multiple
 * steps:
 *
 * - Compile the .c file to a .s file.
 * - Edit the .s file to change the 'progbits' type to 'note', for the section
 *   directive that defines the .note.ABI-tag section.
 * - Compile the .s file to an object file.
 *
 * These steps are done in the invididual Makefiles for each applicable arch.
 */
static const struct {
    int32_t	namesz;
    int32_t	descsz;
    int32_t	type;
    char	name[sizeof ABI_VENDOR];
    int32_t	android_api;
} abitag __attribute__ ((section (ABI_SECTION), aligned(4), used)) = {
    sizeof ABI_VENDOR,
    sizeof(int32_t),
    ABI_NOTETYPE,
    ABI_VENDOR,
    ABI_ANDROID_API,
};

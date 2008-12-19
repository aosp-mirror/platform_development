# Assumes PREBUILT is defined to point to the correct flavor of the prebuilt 
# directory in the Android source tree

CC  := $(PREBUILT)/toolchain/arm-eabi-4.2.1/bin/arm-eabi-gcc
AR  := $(PREBUILT)/toolchain/arm-eabi-4.2.1/bin/arm-eabi-ar

INC   := -I$(NDK_BASE)/include/bionic/libc/arch-arm/include \
         -I$(NDK_BASE)/include/kernel/include \
         -I$(NDK_BASE)/include/bionic/libm/include \
         -I$(NDK_BASE)/include/bionic/libm/include/arm \
         -I$(NDK_BASE)/include/bionic/libc/include \
         -I$(NDK_BASE)/include/bionic/libstdc++/include

LINK       := -nostdlib -Bdynamic \
     -Wl,-T,$(NDK_BASE)/config/armelf.x \
     -Wl,-dynamic-linker,/system/bin/linker \
     -Wl,-z,nocopyreloc \
     -L$(NDK_BASE)/lib \
     -Wl,-rpath-link=$(NDK_BASE)/lib \
    $(NDK_BASE)/lib/crtbegin_dynamic.o

POSTLINK := $(NDK_BASE)/lib/crtend_android.o

%.o: %.cpp
	$(CC) $(CFLAGS) -fno-exceptions -fno-rtti $(INC) -o $@ -c $< 
  
%.o: %.c
	$(CC) $(CFLAGS) $(INC) -o $@ -c $< 
  

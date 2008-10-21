
CC         := $(NDK_BASE)/toolchain/arm-eabi/bin/arm-eabi-gcc
AR         := $(NDK_BASE)/toolchain/arm-eabi/bin/arm-eabi-ar

INC        := -I$(NDK_BASE)/include/bionic/arch-arm/include \
	-I$(NDK_BASE)/include/bionic/include \
	-I$(NDK_BASE)/include/kernel/include \
	-I$(NDK_BASE)/include/libm/include \
	-I$(NDK_BASE)/include/libm/include/arm \
	-I$(NDK_BASE)/include/libstdc++/include

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
  

CC := arm-elf-gcc
LD := arm-elf-ld
AS := arm-elf-as
OBJCOPY := arm-elf-objcopy
OBJDUMP := arm-elf-objdump

OPT := -g
CFLAGS := $(OPT) -mcpu=arm9

.SUFFIXES: .dis .bin .elf

.c.elf:
	$(CC) $(CFLAGS)  -Xlinker --script ../tests.ld -o $@ $< -nostdlib

.c.s:
	$(CC) $(CFLAGS) -static -S $<

.S.elf:
	$(CC) $(CFLAGS) -Xlinker --script ../tests.ld -nostdlib -o $@ $<

.elf.dis:
	$(OBJDUMP) -adx $< > $@

.elf.bin:
	$(OBJCOPY) -O binary $< $@

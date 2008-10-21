include ../common_head.mk

P4ROOT=/work/android
QEMU=$(P4ROOT)/device/tools/qemu/arm-softmmu/qemu-system-arm
QTOOLS=$(P4ROOT)/device/tools/qtools

all: test.elf test.bin test.dis

trace: test.elf test.bin
	$(QEMU) -QEMU -kernel test.bin -trace foo
	$(QTOOLS)/post_trace foo
	$(QTOOLS)/read_trace foo test.elf > t1

gtrace: trace
	$(QTOOLS)/q2g -r $(P4ROOT)/device/out/linux-arm-release/symbols foo test.elf foo.gtrace
	gtracepost64 foo.gtrace > t2

include ../common_tail.mk

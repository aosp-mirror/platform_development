# Makefile to build the Windows SDK under linux.
#
# This file is included by build/core/Makefile when a PRODUCT-sdk-win_sdk build
# is requested.
#
# Summary of operations:
# - create a regular Linux SDK
# - build a few Windows tools
# - mirror the linux SDK directory and patch it with the Windows tools
#
# This way we avoid the headache of building a full SDK in MinGW mode, which is
# made complicated by the fact the build system does not support cross-compilation.

# We can only use this under Linux
ifneq ($(shell uname),Linux)
$(error Linux is required to create a Windows SDK)
endif
ifeq ($(strip $(shell which unix2dos todos 2>/dev/null)),)
$(error Need a unix2dos command. Please 'apt-get install tofrodos')
endif

# Define WIN_SDK_TARGETS (the list of targets located in topdir/sdk)
# and the WIN_SDK_BUILD_PREREQ (the list of build prerequisites)
# that are tools-dependent and not platform-dependent.
include $(TOPDIR)sdk/build/windows_sdk_tools.mk

# This is the list of targets that we want to generate as
# Windows executables. All the targets specified here are located in
# the topdir/development directory and are somehow platform-dependent.
WIN_TARGETS := \
	aapt \
	aapt2 \
	adb \
	aidl \
	aprotoc \
	bcc_compat \
	clang \
	etc1tool \
	dexdump dmtracedump \
	fastboot \
	hprof-conv \
	libaapt2_jni \
	llvm-rs-cc \
	sqlite3 \
	zipalign \
	split-select \
	$(WIN_SDK_TARGETS)

WIN_TARGETS := $(foreach t,$(WIN_TARGETS),$(ALL_MODULES.host_cross_$(t).INSTALLED))

# MAIN_SDK_NAME/DIR is set in build/core/Makefile
WIN_SDK_NAME := $(subst $(HOST_OS)-$(SDK_HOST_ARCH),windows,$(MAIN_SDK_NAME))
WIN_SDK_DIR  := $(subst $(HOST_OS)-$(SDK_HOST_ARCH),windows,$(MAIN_SDK_DIR))
WIN_SDK_ZIP  := $(WIN_SDK_DIR)/$(WIN_SDK_NAME).zip

$(call dist-for-goals, win_sdk, $(WIN_SDK_ZIP))

# b/36697262 - we want the 64-bit libaapt2_jni and its dependencies
ifdef HOST_CROSS_2ND_ARCH
$(call dist-for-goals,win_sdk,$(ALL_MODULES.host_cross_libaapt2_jni$(HOST_CROSS_2ND_ARCH_MODULE_SUFFIX).BUILT):lib64/libaapt2_jni.dll)
$(call dist-for-goals, win_sdk, prebuilts/gcc/linux-x86/host/x86_64-w64-mingw32-4.8/x86_64-w64-mingw32/bin/libwinpthread-1.dll:lib64/libwinpthread-1.dll)
endif

.PHONY: win_sdk winsdk-tools

define winsdk-banner
$(info )
$(info ====== [Windows SDK] $1 ======)
$(info )
endef

define winsdk-info
$(info MAIN_SDK_NAME: $(MAIN_SDK_NAME))
$(info WIN_SDK_NAME : $(WIN_SDK_NAME))
$(info WIN_SDK_DIR  : $(WIN_SDK_DIR))
$(info WIN_SDK_ZIP  : $(WIN_SDK_ZIP))
endef

win_sdk: $(WIN_SDK_ZIP)
	$(call winsdk-banner,Done)

winsdk-tools: $(WIN_TARGETS)
	$(call winsdk-banner,Tools Done)

$(WIN_SDK_ZIP): $(WIN_TARGETS) $(INTERNAL_SDK_TARGET)
	$(call winsdk-banner,Build $(WIN_SDK_NAME))
	$(call winsdk-info)
	$(hide) rm -rf $(WIN_SDK_DIR)
	$(hide) mkdir -p $(WIN_SDK_DIR)
	$(hide) cp -rf $(MAIN_SDK_DIR)/$(MAIN_SDK_NAME) $(WIN_SDK_DIR)/$(WIN_SDK_NAME)
	$(hide) USB_DRIVER_HOOK=$(USB_DRIVER_HOOK) \
		PLATFORM_VERSION=$(PLATFORM_VERSION) \
		$(TOPDIR)development/build/tools/patch_windows_sdk.sh $(subst @,-q,$(hide)) \
		$(WIN_SDK_DIR)/$(WIN_SDK_NAME) $(OUT_DIR) $(TOPDIR)
	$(hide) PLATFORM_VERSION=$(PLATFORM_VERSION) \
		$(TOPDIR)sdk/build/patch_windows_sdk.sh $(subst @,-q,$(hide)) \
		$(WIN_SDK_DIR)/$(WIN_SDK_NAME) $(OUT_DIR) $(TOPDIR)
	$(hide) ( \
		cd $(WIN_SDK_DIR) && \
		rm -f $(WIN_SDK_NAME).zip && \
		zip -rq $(subst @,-q,$(hide)) $(WIN_SDK_NAME).zip $(WIN_SDK_NAME) \
		)
	@echo "Windows SDK generated at $(WIN_SDK_ZIP)"

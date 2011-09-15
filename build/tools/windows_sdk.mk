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

# We can only use this under Linux with the mingw32 package installed.
ifneq ($(shell uname),Linux)
$(error Linux is required to create a Windows SDK)
endif
ifeq ($(strip $(shell which i586-mingw32msvc-gcc 2>/dev/null)),)
$(error MinGW is required to build a Windows SDK. Please 'apt-get install mingw32')
endif
ifeq ($(strip $(shell which unix2dos todos 2>/dev/null)),)
$(error Need a unix2dos command. Please 'apt-get install tofrodos')
endif

# Define WIN_SDK_TARGETS, the list of targets located in topdir/sdk
# and are tools-dependent, not platform-dependent.
include $(TOPDIR)sdk/build/windows_sdk_tools.mk

# This is the list of targets that we want to generate as
# Windows executables. All the targets specified here are located in
# the topdir/development directory and are somehow platform-dependent.
WIN_TARGETS := \
	aapt adb aidl \
	etc1tool \
	dexdump dmtracedump \
	fastboot \
	hprof-conv \
	llvm-rs-cc \
	prebuilt \
	sqlite3 \
	zipalign \
	$(WIN_SDK_TARGETS)

# This is the list of *Linux* build tools that we need
# in order to be able to make the WIN_TARGETS. They are
# build prerequisites.
WIN_BUILD_PREREQ := \
	acp \
	llvm-rs-cc


# MAIN_SDK_NAME/DIR is set in build/core/Makefile
WIN_SDK_NAME := $(subst $(HOST_OS)-$(HOST_ARCH),windows,$(MAIN_SDK_NAME))
WIN_SDK_DIR  := $(subst $(HOST_OS)-$(HOST_ARCH),windows,$(MAIN_SDK_DIR))
WIN_SDK_ZIP  := $(WIN_SDK_DIR)/$(WIN_SDK_NAME).zip

$(call dist-for-goals, win_sdk, $(WIN_SDK_ZIP))

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

winsdk-tools: $(WIN_BUILD_PREREQ)
	$(call winsdk-banner,Build Windows Tools)
	$(hide) USE_MINGW=1 USE_CCACHE="" $(MAKE) PRODUCT-$(TARGET_PRODUCT)-$(strip $(WIN_TARGETS)) $(if $(hide),,showcommands)

$(WIN_SDK_ZIP): winsdk-tools sdk
	$(call winsdk-banner,Build $(WIN_SDK_NAME))
	$(call winsdk-info)
	$(hide) rm -rf $(WIN_SDK_DIR)
	$(hide) mkdir -p $(WIN_SDK_DIR)
	$(hide) cp -rf $(MAIN_SDK_DIR)/$(MAIN_SDK_NAME) $(WIN_SDK_DIR)/$(WIN_SDK_NAME)
	$(hide) USB_DRIVER_HOOK=$(USB_DRIVER_HOOK) \
		$(TOPDIR)development/build/tools/patch_windows_sdk.sh $(subst @,-q,$(hide)) \
		$(WIN_SDK_DIR)/$(WIN_SDK_NAME) $(OUT_DIR) $(TOPDIR)
	$(hide) $(TOPDIR)sdk/build/patch_windows_sdk.sh $(subst @,-q,$(hide)) \
		$(WIN_SDK_DIR)/$(WIN_SDK_NAME) $(OUT_DIR) $(TOPDIR)
	$(hide) ( \
		cd $(WIN_SDK_DIR) && \
		rm -f $(WIN_SDK_NAME).zip && \
		zip -rq $(subst @,-q,$(hide)) $(WIN_SDK_NAME).zip $(WIN_SDK_NAME) \
		)
	@echo "Windows SDK generated at $(WIN_SDK_ZIP)"

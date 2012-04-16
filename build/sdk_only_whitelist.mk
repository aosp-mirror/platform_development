# Whitelist of SDK projects that can be built for the SDK on Windows

# The Windows SDK cannot build all the projects from the SDK tree, typically
# due to obvious compiler/architectures differences. When building the Windows
# SDK, we only care about a subset of projects (e.g. generally the SDK tools
# and a few platform-specific binaries.)
#
# This file defines a whitelist of projects that can be built in the Windows
# SDK case. Note that whitelisting a project directory will NOT actually build
# it -- it will only allow one to reference it as a make dependency.
#
# This file is included by build/core/main.mk.

# Note that there are 2 flavors of this file:
#
# - The other file: sdk/build/sdk_only_whitelist.mk
#   must list all projects that are that are NOT specific to a given platform.
#   These binaries are the ones typically found in the SDK/tools directory.
#
# - This file: development/build/sdk_only_whitelist.mk
#   must list all projects that are specific to a given platform. These
#   projects generate files that are generally locates in SDK/platform-tools,
#   or SDK/platforms/, etc.

# -----
# Whitelist of platform specific projects that do NOT need Java (e.g. C libraries)

subdirs += \
	prebuilt \
	prebuilts \
	build/libs/host \
	build/tools/zipalign \
	dalvik/dexdump \
	dalvik/libdex \
	dalvik/tools/dmtracedump \
	dalvik/tools/hprof-conv \
	development/host \
	development/tools/etc1tool \
	development/tools/line_endings \
	external/clang \
	external/easymock \
	external/expat \
	external/libpng \
	external/llvm \
	external/sqlite/dist \
	external/zlib \
	frameworks/base \
	frameworks/compile \
	frameworks/native \
	system/core/adb \
	system/core/fastboot \
	system/core/libcutils \
	system/core/liblog \
	system/core/libzipfile \
	system/extras/ext4_utils

# -----
# Whitelist of platform specific projects that DO require Java

ifneq (,$(shell which javac 2>/dev/null))
subdirs += \
	build/tools/signapk \
	dalvik/dx \
	libcore \
	development/apps \
	development/tools/mkstubs \
	packages

else
$(warning SDK_ONLY: javac not available.)
endif

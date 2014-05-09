#
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


#
# This file is included by build.git/target/product/sdk.mk
# to define the tools that needed to be built and included
# in an SDK.
#
# If you add a dependency here, you will want to then
# modify build/tools.atree to have the new files
# packaged in the SDK.
#

# Host tools and java libraries that are parts of the SDK.
PRODUCT_PACKAGES += \
	aapt \
	adb \
	aidl \
	zipalign \
	bcc_compat \
	bios.bin \
	commons-compress-1.0 \
	dexdump \
	dmtracedump \
	emmalib \
	etc1tool \
	hprof-conv \
	jython \
	layoutlib \
	layoutlib-tests \
	llvm-rs-cc \
	vgabios-cirrus.bin

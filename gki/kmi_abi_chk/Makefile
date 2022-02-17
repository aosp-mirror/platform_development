# Copyright (C) 2021 The Android Open Source Project
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

kmi_sym-objs := kmi_sym_main.o kmi_sym_list.o
obj-m += kmi_sym.o

GKI_VER ?= 5.10
GKI_DIR ?= ${ANDROID_BUILD_TOP}/kernel
KSRC=${GKI_DIR}/out/android12-${GKI_VER}/common

GKI_BID ?= 7618735
CFLAGS_kmi_sym_main.o = -DGKI_BID=${GKI_BID}
AFLAGS_kmi_sym_list.o = -DGKI_BID=${GKI_BID}

all:
	make -C $(KSRC) M=$(PWD) modules
	cp kmi_sym.ko prebuilts/kmi_sym-a12-${GKI_VER}-${GKI_BID}.ko

clean:
	make -C $(KSRC) M=$(PWD) clean

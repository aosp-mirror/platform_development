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

INTERNAL_GKI_PACKAGE_TARGET := $(PRODUCT_OUT)/gki-boot-$(TARGET_KERNEL_USE)-$(FILE_NAME_TAG).zip

$(INTERNAL_GKI_PACKAGE_TARGET): $(PRODUCT_OUT)/boot.img $(PRODUCT_OUT)/vendor_boot.img
	@echo "Package: $@"
	$(hide) $(SOONG_ZIP) -o $@ -C $(PRODUCT_OUT) -f $(PRODUCT_OUT)/boot.img -f $(PRODUCT_OUT)/vendor_boot.img

.PHONY: kernel_img_zip
kernel_img_zip: $(INTERNAL_GKI_PACKAGE_TARGET)

$(call dist-for-goals, kernel_img_zip, $(INTERNAL_GKI_PACKAGE_TARGET))

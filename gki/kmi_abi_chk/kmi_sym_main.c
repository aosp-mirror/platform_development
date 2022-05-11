/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <linux/module.h>    // included for all kernel modules
#include <linux/kernel.h>    // included for KERN_INFO
#include <linux/init.h>      // included for __init and __exit macros
#include <linux/stringify.h>

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("A kernel module using all GKI KMI symbols");
MODULE_IMPORT_NS(CRYPTO_INTERNAL);

extern void *kmi_sym_arr[];

static int __init kmi_sym_init(void)
{
	int cnt;

	for (cnt = 0; kmi_sym_arr[cnt] != 0; ++cnt)
		;
	printk(KERN_INFO "GKI build: %s\n", __stringify(GKI_BID));
	printk(KERN_INFO "%d GKI KMI symbols at %p\n", cnt, kmi_sym_arr);

	return 0;
}

static void __exit kmi_sym_cleanup(void)
{
	printk(KERN_INFO "Cleaning up GKI KMI test.\n");
}

module_init(kmi_sym_init);
module_exit(kmi_sym_cleanup);

#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from vndk_definition_tool import NDK_LIBS


class NDKLibDictTest(unittest.TestCase):
    def test_is_ll_ndk(self):
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib/libc.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib/libdl.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib/liblog.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib/libm.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib/libstdc++.so'))

        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib64/libc.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib64/libdl.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib64/liblog.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib64/libm.so'))
        self.assertTrue(NDK_LIBS.is_ll_ndk('/system/lib64/libstdc++.so'))

        self.assertFalse(NDK_LIBS.is_ll_ndk('/system/lib/libm'))

        # libz.so is not LL-NDK anymore.
        self.assertFalse(NDK_LIBS.is_ll_ndk('/system/lib/libz.so'))
        self.assertFalse(NDK_LIBS.is_ll_ndk('/system/lib64/libz.so'))

    def test_is_sp_ndk(self):
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libEGL.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libGLESv1_CM.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libGLESv2.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libGLESv3.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libnativewindow.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libsync.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib/libvulkan.so'))

        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libEGL.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libGLESv1_CM.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libGLESv2.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libGLESv3.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libnativewindow.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libsync.so'))
        self.assertTrue(NDK_LIBS.is_sp_ndk('/system/lib64/libvulkan.so'))

        # Vendor libraries with the same name are still not SP-NDK.
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/libEGL.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/libGLESv1_CM.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/libGLESv2.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/libGLESv3.so'))

        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/egl/libEGL.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/egl/libGLESv1_CM.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/egl/libGLESv2.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/egl/libGLESv3.so'))

        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/libvulkan.so'))
        self.assertFalse(NDK_LIBS.is_sp_ndk('/vendor/lib64/vulkan.so'))

        # LL-NDK is not SP-NDK.
        self.assertFalse(NDK_LIBS.is_sp_ndk('/system/lib/libc.so'))

    def test_is_hlndk(self):
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libOpenMAXAL.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libOpenSLES.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libandroid.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libcamera2ndk.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libjnigraphics.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib/libmediandk.so'))

        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libOpenMAXAL.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libOpenSLES.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libandroid.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libcamera2ndk.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libjnigraphics.so'))
        self.assertTrue(NDK_LIBS.is_hl_ndk('/system/lib64/libmediandk.so'))

        # LL-NDK and SP-NDK are not HL-NDK.
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libc.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libEGL.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libGLESv1_CM.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libGLESv2.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libGLESv3.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib/libvulkan.so'))

        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libc.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libEGL.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libGLESv1_CM.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libGLESv2.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libGLESv3.so'))
        self.assertFalse(NDK_LIBS.is_hl_ndk('/system/lib64/libvulkan.so'))

    def test_is_ndk(self):
        # LL-NDK
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libc.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libdl.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/liblog.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libm.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libstdc++.so'))

        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libc.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libdl.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/liblog.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libm.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libstdc++.so'))

        # SP-NDK
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libEGL.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libGLESv1_CM.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libGLESv2.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libGLESv3.so'))

        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libEGL.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libGLESv1_CM.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libGLESv2.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libGLESv3.so'))

        # HL-NDK
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libOpenMAXAL.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libOpenSLES.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libandroid.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libcamera2ndk.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libjnigraphics.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libmediandk.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib/libvulkan.so'))

        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libOpenMAXAL.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libOpenSLES.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libandroid.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libcamera2ndk.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libjnigraphics.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libmediandk.so'))
        self.assertTrue(NDK_LIBS.is_ndk('/system/lib64/libvulkan.so'))

        # libz.so is not NDK anymore.
        self.assertFalse(NDK_LIBS.is_ndk('/system/lib/libz.so'))
        self.assertFalse(NDK_LIBS.is_ndk('/system/lib64/libz.so'))

    def test_classify(self):
        self.assertEqual(NDK_LIBS.NOT_NDK,
                         NDK_LIBS.classify('/system/lib/libfoo.so'))
        self.assertEqual(NDK_LIBS.LL_NDK,
                         NDK_LIBS.classify('/system/lib/libc.so'))
        self.assertEqual(NDK_LIBS.SP_NDK,
                         NDK_LIBS.classify('/system/lib/libEGL.so'))
        self.assertEqual(NDK_LIBS.HL_NDK,
                         NDK_LIBS.classify('/system/lib/libmediandk.so'))

        self.assertEqual(NDK_LIBS.NOT_NDK,
                         NDK_LIBS.classify('/system/lib64/libfoo.so'))
        self.assertEqual(NDK_LIBS.LL_NDK,
                         NDK_LIBS.classify('/system/lib64/libc.so'))
        self.assertEqual(NDK_LIBS.SP_NDK,
                         NDK_LIBS.classify('/system/lib64/libEGL.so'))
        self.assertEqual(NDK_LIBS.HL_NDK,
                         NDK_LIBS.classify('/system/lib64/libmediandk.so'))


if __name__ == '__main__':
    unittest.main()

#!/usr/bin/env python3

from __future__ import print_function

import unittest

from vndk_definition_tool import ELFResolver


class ELFResolverTest(unittest.TestCase):
    def setUp(self):
        lib_set = {
            '/system/lib/liba.so': 'a',
            '/system/lib/libb.so': 'b',
            '/vendor/lib/liba.so': 'a2',
            '/vendor/lib/libc.so': 'c',
            '/vendor/lib/libd.so': 'd',
            '/system/lib/hw/libe.so': 'e',
            '/vendor/lib/hw/libf.so': 'f',
        }

        self.resolver = ELFResolver(lib_set, ['/system/lib', '/vendor/lib'])


    def test_get_candidates(self):
        r = self.resolver

        self.assertEqual(
            ['/system/lib/libx.so', '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so')))

        self.assertEqual(
            ['/C/libx.so', '/system/lib/libx.so', '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so', ['/C'])))

        self.assertEqual(
            ['/C/libx.so', '/D/libx.so', '/system/lib/libx.so',
             '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so',
                                  ['/C', '/D'])))

        self.assertEqual(
            ['/E/libx.so', '/system/lib/libx.so', '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so', None,
                                  ['/E'])))

        self.assertEqual(
            ['/E/libx.so', '/F/libx.so', '/system/lib/libx.so',
             '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so', None,
                                  ['/E', '/F'])))

        self.assertEqual(
            ['/C/libx.so', '/D/libx.so', '/E/libx.so', '/F/libx.so',
             '/system/lib/libx.so', '/vendor/lib/libx.so'],
            list(r.get_candidates('/system/lib/libreq.so', 'libx.so',
                                  ['/C', '/D'], ['/E', '/F'])))

        # Test app-specific search paths.
        self.assertEqual(
            ['/system/app/example/lib/armeabi-v7a/libx.so',
             '/C/libx.so', '/D/libx.so', '/E/libx.so', '/F/libx.so',
             '/system/lib/libx.so', '/vendor/lib/libx.so'],
            list(r.get_candidates(
                '/system/app/example/lib/armeabi-v7a/libreq.so',
                'libx.so',
                ['/C', '/D'], ['/E', '/F'])))


    def test_resolve(self):
        r = self.resolver
        self.assertEqual('a', r.resolve('/system/lib/libreq.so', 'liba.so'))
        self.assertEqual('c', r.resolve('/system/lib/libreq.so', 'libc.so'))

        self.assertEqual(None, r.resolve('/system/lib/libreq.so', 'libe.so'))
        self.assertEqual(
            'e',
            r.resolve('/system/lib/libreq.so', 'libe.so',
                      dt_rpath=['/system/lib/hw']))
        self.assertEqual(
            'e',
            r.resolve('/system/lib/libreq.so', 'libe.so',
                      dt_runpath=['/system/lib/hw']))

        self.assertEqual(
            'a2',
            r.resolve('/system/lib/libreq.so', 'liba.so',
                      dt_rpath=['/vendor/lib']))
        self.assertEqual(
            'a2',
            r.resolve('/system/lib/libreq.so', 'liba.so',
                      dt_runpath=['/vendor/lib']))

#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

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
                list(r.get_candidates('libx.so')))

        self.assertEqual(
                ['/C/libx.so', '/system/lib/libx.so', '/vendor/lib/libx.so'],
                list(r.get_candidates('libx.so', ['/C'])))

        self.assertEqual(
                ['/C/libx.so', '/D/libx.so', '/system/lib/libx.so',
                 '/vendor/lib/libx.so'],
                list(r.get_candidates('libx.so', ['/C', '/D'])))

        self.assertEqual(
                ['/E/libx.so', '/system/lib/libx.so', '/vendor/lib/libx.so'],
                list(r.get_candidates('libx.so', None, ['/E'])))

        self.assertEqual(
                ['/E/libx.so', '/F/libx.so', '/system/lib/libx.so',
                 '/vendor/lib/libx.so'],
                list(r.get_candidates('libx.so', None, ['/E', '/F'])))

        self.assertEqual(
                ['/C/libx.so', '/D/libx.so', '/E/libx.so', '/F/libx.so',
                 '/system/lib/libx.so', '/vendor/lib/libx.so'],
                list(r.get_candidates('libx.so', ['/C', '/D'], ['/E', '/F'])))

    def test_resolve(self):
        r = self.resolver
        self.assertEqual('a', r.resolve('liba.so'))
        self.assertEqual('c', r.resolve('libc.so'))

        self.assertEqual(None, r.resolve('libe.so'))
        self.assertEqual('e', r.resolve('libe.so', dt_rpath=['/system/lib/hw']))
        self.assertEqual(
                'e', r.resolve('libe.so', dt_runpath=['/system/lib/hw']))

        self.assertEqual('a2', r.resolve('liba.so', dt_rpath=['/vendor/lib']))
        self.assertEqual('a2', r.resolve('liba.so', dt_runpath=['/vendor/lib']))


if __name__ == '__main__':
    unittest.main()

#
# Copyright (C) 2017 The Android Open Source Project
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
import os
import unittest
import mock

import adb

class GetDeviceTest(unittest.TestCase):
    def setUp(self):
        self.android_serial = os.getenv('ANDROID_SERIAL')
        if 'ANDROID_SERIAL' in os.environ:
            del os.environ['ANDROID_SERIAL']

    def tearDown(self):
        if self.android_serial is not None:
            os.environ['ANDROID_SERIAL'] = self.android_serial
        else:
            if 'ANDROID_SERIAL' in os.environ:
                del os.environ['ANDROID_SERIAL']

    @mock.patch('adb.device.get_devices')
    def test_explicit(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        device = adb.get_device('foo')
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_from_env(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        os.environ['ANDROID_SERIAL'] = 'foo'
        device = adb.get_device()
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_arg_beats_env(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        os.environ['ANDROID_SERIAL'] = 'bar'
        device = adb.get_device('foo')
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_no_such_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        self.assertRaises(adb.DeviceNotFoundError, adb.get_device, ['baz'])

        os.environ['ANDROID_SERIAL'] = 'baz'
        self.assertRaises(adb.DeviceNotFoundError, adb.get_device)

    @mock.patch('adb.device.get_devices')
    def test_unique_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo']
        device = adb.get_device()
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_no_unique_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        self.assertRaises(adb.NoUniqueDeviceError, adb.get_device)


def main():
    suite = unittest.TestLoader().loadTestsFromName(__name__)
    unittest.TextTestRunner(verbosity=3).run(suite)

if __name__ == '__main__':
    main()

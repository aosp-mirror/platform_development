import unittest
from unittest.mock import patch, mock_open, Mock, MagicMock
from target_lib import BuildInfo, TargetLib
import zipfile
import os
import sqlite3
from tempfile import NamedTemporaryFile

class CreateTestBuild():
    def __init__(self, include_build_prop=True, include_ab_partitions=True):
        self.file = NamedTemporaryFile(dir='test/')
        self.test_path = self.file.name
        with zipfile.ZipFile(self.file, mode='w') as package:
            if include_build_prop:
                package.write('test/test_build.prop', 'SYSTEM/build.prop')
            if include_ab_partitions:
                package.write('test/test_ab_partitions.txt',
                    'META/ab_partitions.txt')
        self.test_info = {
            'file_name': self.test_path.split('/')[-1],
            'path': self.test_path,
            'time': 1628698830,
            'build_id': 'AOSP.MASTER',
            'build_version': '7392671',
            'build_flavor': 'aosp_cf_x86_64_phone-userdebug',
            'partitions': ['system', 'vendor'],
        }
        if not include_build_prop:
            self.test_info['build_id'] = ''
            self.test_info['build_version'] = ''
            self.test_info['build_flavor'] = ''
        if not include_ab_partitions:
            self.test_info['partitions'] = []

    def clean(self):
        self.file.close()

class TestBuildInfo(unittest.TestCase):
    def setUp(self):
        """
        Create a virtual Android build, which only have build.prop
        and ab_partitions.txt
        """
        self.test_build = CreateTestBuild()
        self.build_info = BuildInfo(
            self.test_build.test_info['file_name'],
            self.test_build.test_info['path'],
            self.test_build.test_info['time']
        )
        self.build_info.analyse_buildprop()

    def tearDown(self):
        self.test_build.clean()

    def test_analyse_buildprop(self):
        # Test if the build.prop and ab_partitions are not empty
        for key, value in self.test_build.test_info.items():
            self.assertEqual(value, self.build_info.__dict__[key],
                'The ' + key + ' is not parsed correctly.'
            )
        # Test if the ab_partitions is empty
        test_build_no_partitions = CreateTestBuild(include_ab_partitions=False)
        build_info = BuildInfo(
            test_build_no_partitions.test_info['file_name'],
            test_build_no_partitions.test_info['path'],
            test_build_no_partitions.test_info['time']
        )
        build_info.analyse_buildprop()
        self.assertEqual(build_info.partitions,
            test_build_no_partitions.test_info['partitions'],
            'The partition list is not empty if ab_partitions is not provided.'
        )
        test_build_no_partitions.clean()

    def test_to_sql_form_dict(self):
        sql_dict = self.build_info.to_sql_form_dict()
        for key, value in self.test_build.test_info.items():
            if key != 'partitions':
                self.assertEqual(value, sql_dict[key],
                    'The ' + key + ' is not parsed correctly.'
                )
            else:
                self.assertEqual(','.join(value), sql_dict[key],
                    'The partition list is not coverted to sql form properly.'
                )

    def test_to_dict(self):
        ordinary_dict = self.build_info.to_dict()
        for key, value in self.test_build.test_info.items():
            self.assertEqual(value, ordinary_dict[key],
                'The ' + key + ' is not parsed correctly.'
            )


class TestTargetLib(unittest.TestCase):
    def setUp(self):
        self.test_path = 'test/test_target_lib.db'
        self.tearDown()
        self.target_build = TargetLib(path=self.test_path)

    def tearDown(self):
        if os.path.isfile(self.test_path):
            os.remove(self.test_path)

    def test_init(self):
        # Test the database is created successfully
        self.assertTrue(os.path.isfile(self.test_path))
        test_columns = [
            {'name': 'FileName','type':'TEXT'},
            {'name': 'Path','type':'TEXT'},
            {'name': 'BuildID','type':'TEXT'},
            {'name': 'BuildVersion','type':'TEXT'},
            {'name': 'BuildFlavor','type':'TEXT'},
            {'name': 'Partitions','type':'TEXT'},
            {'name': 'UploadTime','type':'INTEGER'},
        ]
        connect = sqlite3.connect(self.test_path)
        cursor = connect.cursor()
        cursor.execute("PRAGMA table_info(Builds)")
        columns = cursor.fetchall()
        for column in test_columns:
            column_found = list(filter(lambda x: x[1]==column['name'], columns))
            self.assertEqual(len(column_found), 1,
                'The column ' + column['name'] + ' is not found in database'
            )
            self.assertEqual(column_found[0][2], column['type'],
                'The column ' + column['name'] + ' has a wrong type'
            )

    def test_new_build(self):
        test_build = CreateTestBuild()
        self.target_build.new_build(
            filename=test_build.test_info['file_name'],
            path=test_build.test_path
        )
        connect = sqlite3.connect(self.test_path)
        cursor = connect.cursor()
        cursor.execute("SELECT * FROM BUILDS")
        entries = cursor.fetchall()
        self.assertEqual(len(entries), 1,
            'The test build cannot be added into the database.'
        )
        test_build.clean()

    def test_get_builds(self):
        test_build = CreateTestBuild()
        # time.time() has to be mocked, otherwise it will be the current time
        mock_time = Mock(return_value=test_build.test_info['time'])
        with patch('time.time', mock_time):
            self.target_build.new_build(
                filename=test_build.test_info['file_name'],
                path=test_build.test_path
            )
        # build_list is read and parsed from the database
        # build_info is directly parsed from the package
        # Test if the read/write database process changes the data entry
        build_list = self.target_build.get_builds()
        build_info = BuildInfo(
            test_build.test_info['file_name'],
            test_build.test_info['path'],
            test_build.test_info['time']
        )
        build_info.analyse_buildprop()
        self.assertEqual(build_list[0], build_info,
            'The list of build info cannot be extracted from database.'
        )
        test_build.clean()

    def test_get_build_by_path(self):
        test_build = CreateTestBuild()
        # time.time() has to be mocked, otherwise it will be the current time
        mock_time = Mock(return_value=test_build.test_info['time'])
        with patch('time.time', mock_time):
            self.target_build.new_build(
                filename=test_build.test_info['file_name'],
                path=test_build.test_path
            )
        build = self.target_build.get_build_by_path(test_build.test_info['path'])
        build_info = BuildInfo(
            test_build.test_info['file_name'],
            test_build.test_info['path'],
            test_build.test_info['time']
        )
        build_info.analyse_buildprop()
        self.assertEqual(build, build_info,
            'Build info cannot be extracted by path.'
        )
        test_build.clean()


if __name__ == '__main__':
    unittest.main()

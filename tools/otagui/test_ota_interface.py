import unittest
from ota_interface import JobInfo, ProcessesManagement
from unittest.mock import patch, mock_open, Mock, MagicMock
import os
import sqlite3
import copy

class TestJobInfo(unittest.TestCase):
    def setUp(self):
        self.test_id = '286feab0-f16b-11eb-a72a-f7b1de0921ef'
        self.test_target = 'target/build.zip'
        self.test_verbose = True
        self.test_status = 'Running'
        self.test_extra = '--downgrade'
        self.test_start_time = 1628698830
        self.test_finish_time = 1628698831

    def setup_job(self, incremental = '', partial = [],
                  output = '', stdout = '', stderr = ''):
        job_info = JobInfo(id=self.test_id,
                           target=self.test_target,
                           incremental=incremental,
                           verbose=self.test_verbose,
                           partial=partial,
                           output=output,
                           status=self.test_status,
                           extra=self.test_extra,
                           start_time=self.test_start_time,
                           finish_time=self.test_finish_time,
                           stderr=stderr,
                           stdout=stdout
        )
        return job_info

    def test_init(self):
        # No incremental, no output, no stdout/stderr set.
        job_info1 = self.setup_job()
        for key, value in self.__dict__.items():
            if key.startswith('test_'):
                self.assertEqual(job_info1.__dict__[key[5:]], value,
                    'The value of ' + key + 'is not initialized correctly'
                )
        self.assertEqual(job_info1.output, 'output/'+self.test_id+'.zip',
            'Default output cannot be setup correctly'
        )
        self.assertEqual(job_info1.stderr, 'output/stderr.'+self.test_id,
            'Default stderr cannot be setup correctly'
        )
        self.assertEqual(job_info1.stdout, 'output/stdout.'+self.test_id,
            'Default stdout cannot be setup correctly'
        )
        # Test the incremental setup
        job_info2 = self.setup_job(incremental='target/source.zip')
        self.assertEqual(job_info2.incremental, 'target/source.zip',
            'incremental source cannot be initialized correctly'
        )
        self.assertTrue(job_info2.isIncremental,
            'incremental status cannot be initialized correctly'
        )
        # Test the stdout/stderr setup
        job_info3 = self.setup_job(stderr='output/stderr',
            stdout='output/stdout'
        )
        self.assertEqual(job_info3.stderr, 'output/stderr',
            'the stderr cannot be setup manually'
        )
        self.assertEqual(job_info3.stdout, 'output/stdout',
            'the stdout cannot be setup manually'
        )
        # Test the output setup
        job_info4 = self.setup_job(output='output/output.zip')
        self.assertEqual(job_info4.output, 'output/output.zip',
            'output cannot be setup manually'
        )
        # Test the partial setup
        job_info5 = self.setup_job(partial=['system', 'vendor'])
        self.assertEqual(job_info5.partial, ['system', 'vendor'],
            'partial list cannot be setup correctly'
        )

    def test_to_sql_form_dict(self):
        partial_list = ['system', 'vendor']
        job_info = self.setup_job(partial=partial_list)
        sql_dict = job_info.to_sql_form_dict()
        test_dict = {'id': self.test_id,
            'target': self.test_target,
            'incremental': '',
            'verbose': int(self.test_verbose),
            'partial': ','.join(partial_list),
            'output': 'output/' + self.test_id + '.zip',
            'status': self.test_status,
            'extra': self.test_extra,
            'stdout': 'output/stdout.' + self.test_id,
            'stderr': 'output/stderr.' + self.test_id,
            'start_time': self.test_start_time,
            'finish_time': self.test_finish_time,
            'isPartial': True,
            'isIncremental': False
        }
        for key, value in test_dict.items():
            self.assertEqual(value, sql_dict[key],
            'the ' + key + ' is not converted to sql form dict correctly'
        )

    def test_to_dict_basic(self):
        partial_list = ['system', 'vendor']
        job_info = self.setup_job(partial=partial_list)
        basic_dict = job_info.to_dict_basic()
        test_dict = {'id': self.test_id,
            'target': self.test_target,
            'target_name': self.test_target.split('/')[-1],
            'incremental': '',
            'verbose': int(self.test_verbose),
            'partial': partial_list,
            'output': 'output/' + self.test_id + '.zip',
            'status': self.test_status,
            'extra': self.test_extra,
            'stdout': 'output/stdout.' + self.test_id,
            'stderr': 'output/stderr.' + self.test_id,
            'start_time': self.test_start_time,
            'finish_time': self.test_finish_time,
            'isPartial': True,
            'isIncremental': False
        }
        for key, value in test_dict.items():
            self.assertEqual(value, basic_dict[key],
            'the ' + key + ' is not converted to basic form dict correctly'
        )

    def test_to_dict_detail(self):
        partial_list = ['system', 'vendor']
        test_incremental = 'target/source.zip'
        job_info = self.setup_job(partial=partial_list, incremental=test_incremental)
        mock_target_lib = Mock()
        mock_target_lib.get_build_by_path = Mock(
            side_effect = [
                Mock(file_name='build.zip', build_version=''),
                Mock(file_name='source.zip', build_version='')
            ]
        )
        test_dict = {'id': self.test_id,
            'target': self.test_target,
            'incremental': 'target/source.zip',
            'verbose': int(self.test_verbose),
            'partial': partial_list,
            'output': 'output/' + self.test_id + '.zip',
            'status': self.test_status,
            'extra': self.test_extra,
            'stdout': 'NO STD OUTPUT IS FOUND',
            'stderr': 'NO STD ERROR IS FOUND',
            'start_time': self.test_start_time,
            'finish_time': self.test_finish_time,
            'isPartial': True,
            'isIncremental': True
        }
        # Test with no stdout and stderr
        dict_detail = job_info.to_dict_detail(mock_target_lib)
        mock_target_lib.get_build_by_path.assert_any_call(self.test_target)
        mock_target_lib.get_build_by_path.assert_any_call(test_incremental)
        for key, value in test_dict.items():
            self.assertEqual(value, dict_detail[key],
            'the ' + key + ' is not converted to detailed dict correctly'
        )
        # Test with mocked stdout and stderr
        mock_target_lib.get_build_by_path = Mock(
            side_effect = [
                Mock(file_name='build.zip', build_version=''),
                Mock(file_name='source.zip', build_version='')
            ]
        )
        mock_file = mock_open(read_data="mock output")
        with patch("builtins.open", mock_file):
            dict_detail = job_info.to_dict_detail(mock_target_lib)
        test_dict['stderr'] = 'mock output'
        test_dict['stdout'] = 'mock output'
        for key, value in test_dict.items():
            self.assertEqual(value, dict_detail[key],
            'the ' + key + ' is not converted to detailed dict correctly'
        )

class TestProcessesManagement(unittest.TestCase):
    def setUp(self):
        if os.path.isfile('test_process.db'):
            self.tearDown()
        self.processes = ProcessesManagement(db_path='test_process.db')
        testcase_job_info = TestJobInfo()
        testcase_job_info.setUp()
        self.test_job_info = testcase_job_info.setup_job(incremental='target/source.zip')
        self.processes.insert_database(self.test_job_info)

    def tearDown(self):
        os.remove('test_process.db')
        try:
            os.remove('output/stderr.'+self.test_job_info.id)
            os.remove('output/stdout.'+self.test_job_info.id)
        except FileNotFoundError:
            pass

    def test_init(self):
        # Test the database is created successfully
        self.assertTrue(os.path.isfile('test_process.db'))
        test_columns = [
            {'name': 'ID','type':'TEXT'},
            {'name': 'TargetPath','type':'TEXT'},
            {'name': 'IncrementalPath','type':'TEXT'},
            {'name': 'Verbose','type':'INTEGER'},
            {'name': 'Partial','type':'TEXT'},
            {'name': 'OutputPath','type':'TEXT'},
            {'name': 'Status','type':'TEXT'},
            {'name': 'Downgrade','type':'INTEGER'},
            {'name': 'OtherFlags','type':'TEXT'},
            {'name': 'STDOUT','type':'TEXT'},
            {'name': 'STDERR','type':'TEXT'},
            {'name': 'StartTime','type':'INTEGER'},
            {'name': 'FinishTime','type':'INTEGER'},
        ]
        connect = sqlite3.connect('test_process.db')
        cursor = connect.cursor()
        cursor.execute("PRAGMA table_info(jobs)")
        columns = cursor.fetchall()
        for column in test_columns:
            column_found = list(filter(lambda x: x[1]==column['name'], columns))
            self.assertEqual(len(column_found), 1,
                'The column ' + column['name'] + ' is not found in database'
            )
            self.assertEqual(column_found[0][2], column['type'],
                'The column' + column['name'] + ' has a wrong type'
            )

    def test_get_status_by_ID(self):
        job_info = self.processes.get_status_by_ID(self.test_job_info.id)
        self.assertEqual(job_info, self.test_job_info,
            'The data read from database is not the same one as inserted'
        )

    def test_get_status(self):
        # Insert the same info again, but change the last digit of id to 0
        test_job_info2 = copy.copy(self.test_job_info)
        test_job_info2.id = test_job_info2.id[:-1] + '0'
        self.processes.insert_database(test_job_info2)
        job_infos = self.processes.get_status()
        self.assertEqual(len(job_infos), 2,
            'The number of data entries is not the same as created'
        )
        self.assertEqual(job_infos[0], self.test_job_info,
            'The data list read from database is not the same one as inserted'
        )
        self.assertEqual(job_infos[1], test_job_info2,
            'The data list read from database is not the same one as inserted'
        )

    def test_ota_run(self):
        # Test when the job exit normally
        mock_proc = Mock()
        mock_proc.wait = Mock(return_value=0)
        mock_Popen = Mock(return_value=mock_proc)
        test_command = [
            "ota_from_target_files", "-v","build/target.zip", "output/ota.zip",
            ]
        mock_pipes_template = Mock()
        mock_pipes_template.open = Mock()
        mock_Template = Mock(return_value=mock_pipes_template)
        # Mock the subprocess.Popen, subprocess.Popen().wait and pipes.Template
        with patch("subprocess.Popen", mock_Popen), \
            patch("pipes.Template", mock_Template):
            self.processes.ota_run(test_command, self.test_job_info.id)
        mock_Popen.assert_called_once()
        mock_proc.wait.assert_called_once()
        job_info = self.processes.get_status_by_ID(self.test_job_info.id)
        self.assertEqual(job_info.status, 'Finished')
        mock_Popen.reset_mock()
        mock_proc.wait.reset_mock()
        # Test when the job exit with prbolems
        mock_proc.wait = Mock(return_value=1)
        with patch("subprocess.Popen", mock_Popen), \
            patch("pipes.Template", mock_Template):
            self.processes.ota_run(test_command, self.test_job_info.id)
        mock_Popen.assert_called_once()
        mock_proc.wait.assert_called_once()
        job_info = self.processes.get_status_by_ID(self.test_job_info.id)
        self.assertEqual(job_info.status, 'Error')

    def test_ota_generate(self):
        test_args = dict({
            'output': 'ota.zip',
            'extra_keys': ['downgrade', 'wipe_user_data'],
            'extra': '--disable_vabc',
            'isIncremental': True,
            'isPartial': True,
            'partial': ['system', 'vendor'],
            'incremental': 'target/source.zip',
            'target': 'target/build.zip',
            'verbose': True
        })
        # Usually the order of commands make no difference, but the following
        # order has been validated, so it is best to follow this manner:
        #   ota_from_target_files [flags like -v, --downgrade]
        #   [-i incremental_source] [-p partial_list] target output
        test_command = [
            'ota_from_target_files', '-v', '--downgrade',
            '--wipe_user_data', '--disable_vabc', '-k',
            'build/make/target/product/security/testkey',
            '-i', 'target/source.zip',
            '--partial', 'system vendor', 'target/build.zip', 'ota.zip'
        ]
        mock_os_path_isfile = Mock(return_value=True)
        mock_threading = Mock()
        mock_thread = Mock(return_value=mock_threading)
        with patch("os.path.isfile", mock_os_path_isfile), \
            patch("threading.Thread", mock_thread):
                self.processes.ota_generate(test_args, id='test')
        job_info = self.processes.get_status_by_ID('test')
        self.assertEqual(job_info.status, 'Running',
            'The job cannot be stored into database properly'
        )
        # Test if the job stored into database properly
        for key, value in test_args.items():
            # extra_keys is merged to extra when stored into database
            if key=='extra_keys':
                continue
            self.assertEqual(job_info.__dict__[key], value,
                'The column ' + key + ' is not stored into database properly'
            )
        # Test if the command is in its order
        self.assertEqual(mock_thread.call_args[1]['args'][0], test_command,
            'The subprocess command is not in its good shape'
        )

if __name__ == '__main__':
    unittest.main()
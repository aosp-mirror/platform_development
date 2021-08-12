import unittest
from ota_interface import JobInfo, ProcessesManagement
from unittest.mock import patch, mock_open, Mock, MagicMock

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
    pass

if __name__ == '__main__':
    unittest.main()
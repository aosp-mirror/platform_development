#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import sys
import unittest
sys.path.append('../..')

from testrunner import am_instrument_parser


class AmParserTest(unittest.TestCase):

  def testParseAmInstResult(self):
    result="""INSTRUMENTATION_RESULT: performance.java_size=4871
INSTRUMENTATION_RESULT: stream=
Error: Failed to generate emma coverage.
INSTRUMENTATION_RESULT: performance.cpu_time=33846
INSTRUMENTATION_CODE: -1
"""
    bundle_dict = \
        am_instrument_parser._ParseInstrumentationFinishedBundle(result)
    self.assertEquals(4871, bundle_dict['java_size'])
    self.assertEquals(33846, bundle_dict['cpu_time'])
    self.assertEquals("\nError: Failed to generate emma coverage.",
        bundle_dict['stream'])

  def testParseAmInstStatus(self):
    # numtests before id
    segment1 = """INSTRUMENTATION_STATUS: stream=
INSTRUMENTATION_STATUS: test=testLaunchComplexActivity
INSTRUMENTATION_STATUS: class=LaunchPerformanceTest
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS_CODE: 1"""
    segment2 = """INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=testLaunchComplexActivity
INSTRUMENTATION_STATUS: performance.cpu_time=866
INSTRUMENTATION_STATUS: performance.execution_time=1242
INSTRUMENTATION_STATUS: class=LaunchPerformanceTest
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS_CODE: 0"""
    # numtests after id
    segment3 = """INSTRUMENTATION_STATUS: stream=
INSTRUMENTATION_STATUS: test=testLaunchSimpleActivity
INSTRUMENTATION_STATUS: class=LaunchPerformanceTest
INSTRUMENTATION_STATUS: current=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=8
INSTRUMENTATION_STATUS_CODE: 1"""
    segment4 = """INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=testLaunchSimpleActivity
INSTRUMENTATION_STATUS: performance.cpu_time=590
INSTRUMENTATION_STATUS: performance.execution_time=1122
INSTRUMENTATION_STATUS: class=LaunchPerformanceTest
INSTRUMENTATION_STATUS: current=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=8
INSTRUMENTATION_STATUS_CODE: 0"""

    result = am_instrument_parser.TestResult(segment1)
    map = result.GetResultFields()
    self.assertEquals('testLaunchComplexActivity', map['test'])
    self.assertEquals('LaunchPerformanceTest', map['class'])
    self.assertEquals('1', map['current'])
    self.assertEquals('2', map['numtests'])
    self.assertEquals('InstrumentationTestRunner', map['id'])
    self.assertEquals(1, result.GetStatusCode())

    result = am_instrument_parser.TestResult(segment2)
    map = result.GetResultFields()
    self.assertEquals('testLaunchComplexActivity', map['test'])
    self.assertEquals('866', map['cpu_time'])
    self.assertEquals('1242', map['execution_time'])
    self.assertEquals('LaunchPerformanceTest', map['class'])
    self.assertEquals('1', map['current'])
    self.assertEquals('2', map['numtests'])
    self.assertEquals('InstrumentationTestRunner', map['id'])
    self.assertEquals(0, result.GetStatusCode())

    result = am_instrument_parser.TestResult(segment3)
    map = result.GetResultFields()
    self.assertEquals('testLaunchSimpleActivity', map['test'])
    self.assertEquals('LaunchPerformanceTest', map['class'])
    self.assertEquals('2', map['current'])
    self.assertEquals('8', map['numtests'])
    self.assertEquals('InstrumentationTestRunner', map['id'])
    self.assertEquals(1, result.GetStatusCode())

    result = am_instrument_parser.TestResult(segment4)
    map = result.GetResultFields()
    self.assertEquals('testLaunchSimpleActivity', map['test'])
    self.assertEquals('590', map['cpu_time'])
    self.assertEquals('1122', map['execution_time'])
    self.assertEquals('LaunchPerformanceTest', map['class'])
    self.assertEquals('2', map['current'])
    self.assertEquals('8', map['numtests'])
    self.assertEquals('InstrumentationTestRunner', map['id'])
    self.assertEquals(0, result.GetStatusCode())

  def testParseAmInstOutput(self):
    result = """INSTRUMENTATION_STATUS: class=LaunchPerformanceTestCase
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: stream=
LaunchPerformanceTestCase:
INSTRUMENTATION_STATUS: test=testLaunchComplexActivity
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: class=LaunchPerformanceTestCase
INSTRUMENTATION_STATUS: current=1
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: performance.cpu_time=866
INSTRUMENTATION_STATUS: performance.execution_time=1242
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=testLaunchComplexActivity
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_STATUS: class=LaunchPerformanceTestCase
INSTRUMENTATION_STATUS: current=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: stream=
INSTRUMENTATION_STATUS: test=testLaunchSimpleActivity
INSTRUMENTATION_STATUS_CODE: 1
INSTRUMENTATION_STATUS: class=LaunchPerformanceTestCase
INSTRUMENTATION_STATUS: current=2
INSTRUMENTATION_STATUS: id=InstrumentationTestRunner
INSTRUMENTATION_STATUS: numtests=2
INSTRUMENTATION_STATUS: performance.cpu_time=590
INSTRUMENTATION_STATUS: performance.execution_time=1122
INSTRUMENTATION_STATUS: stream=.
INSTRUMENTATION_STATUS: test=testLaunchSimpleActivity
INSTRUMENTATION_STATUS_CODE: 0
INSTRUMENTATION_RESULT: performance.cpu_time=829
INSTRUMENTATION_RESULT: performance.execution_time=1708
INSTRUMENTATION_RESULT: performance.gc_invocation_count=0
INSTRUMENTATION_RESULT: performance.global_alloc_count=2848
INSTRUMENTATION_RESULT: performance.global_alloc_size=193079
INSTRUMENTATION_RESULT: performance.global_freed_count=1207
INSTRUMENTATION_RESULT: performance.global_freed_size=93040
INSTRUMENTATION_RESULT: performance.java_allocated=2175
INSTRUMENTATION_RESULT: performance.java_free=580
INSTRUMENTATION_RESULT: performance.java_private_dirty=740
INSTRUMENTATION_RESULT: performance.java_pss=1609
INSTRUMENTATION_RESULT: performance.java_shared_dirty=3860
INSTRUMENTATION_RESULT: performance.java_size=2755
INSTRUMENTATION_RESULT: performance.native_allocated=2585
INSTRUMENTATION_RESULT: performance.native_free=34
INSTRUMENTATION_RESULT: performance.native_private_dirty=632
INSTRUMENTATION_RESULT: performance.native_pss=701
INSTRUMENTATION_RESULT: performance.native_shared_dirty=1164
INSTRUMENTATION_RESULT: performance.native_size=2620
INSTRUMENTATION_RESULT: performance.other_private_dirty=896
INSTRUMENTATION_RESULT: performance.other_pss=1226
INSTRUMENTATION_RESULT: performance.other_shared_dirty=804
INSTRUMENTATION_RESULT: performance.pre_received_transactions=-1
INSTRUMENTATION_RESULT: performance.pre_sent_transactions=-1
INSTRUMENTATION_RESULT: performance.received_transactions=-1
INSTRUMENTATION_RESULT: performance.sent_transactions=-1
INSTRUMENTATION_RESULT: stream=
Test results for InstrumentationTestRunner=..
Time: 2.413

OK (2 tests)


INSTRUMENTATION_CODE: -1
"""
    (results_list, perf_dict) = \
        am_instrument_parser.ParseAmInstrumentOutput(result)
    self.assertEquals(829, perf_dict['cpu_time'])
    self.assertEquals(2848, perf_dict['global_alloc_count'])
    self.assertEquals(93040, perf_dict['global_freed_size'])
    self.assertEquals(740, perf_dict['java_private_dirty'])
    self.assertEquals(2755, perf_dict['java_size'])
    self.assertEquals(632, perf_dict['native_private_dirty'])
    self.assertEquals(2620, perf_dict['native_size'])
    self.assertEquals(804, perf_dict['other_shared_dirty'])
    self.assertEquals(-1, perf_dict['received_transactions'])
    self.assertTrue(len(perf_dict['stream']) > 50)
    self.assertEquals('-1', perf_dict['code'])


if __name__ == "__main__":
  unittest.main()

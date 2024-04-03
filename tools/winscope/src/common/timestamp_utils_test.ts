/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {TimestampUtils} from './timestamp_utils';

describe('TimestampUtils', () => {
  const MILLISECOND = BigInt(1000000);
  const SECOND = BigInt(1000) * MILLISECOND;
  const MINUTE = BigInt(60) * SECOND;
  const HOUR = BigInt(60) * MINUTE;

  beforeAll(() => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
  });

  describe('compareFn', () => {
    it('allows to sort arrays', () => {
      const array = [
        TimestampConverterUtils.makeRealTimestamp(100n),
        TimestampConverterUtils.makeRealTimestamp(10n),
        TimestampConverterUtils.makeRealTimestamp(12n),
        TimestampConverterUtils.makeRealTimestamp(110n),
        TimestampConverterUtils.makeRealTimestamp(11n),
      ];
      array.sort(TimestampUtils.compareFn);

      const expected = [
        TimestampConverterUtils.makeRealTimestamp(10n),
        TimestampConverterUtils.makeRealTimestamp(11n),
        TimestampConverterUtils.makeRealTimestamp(12n),
        TimestampConverterUtils.makeRealTimestamp(100n),
        TimestampConverterUtils.makeRealTimestamp(110n),
      ];
      expect(array).toEqual(expected);
    });
  });

  it('nano second regex accept all expected inputs', () => {
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('123')).toBeTrue();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('123ns')).toBeTrue();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('123 ns')).toBeTrue();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test(' 123 ns ')).toBeTrue();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('   123  ')).toBeTrue();

    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('1a23')).toBeFalse();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('a123 ns')).toBeFalse();
    expect(TimestampUtils.NS_TIMESTAMP_REGEX.test('')).toBeFalse();
  });
});

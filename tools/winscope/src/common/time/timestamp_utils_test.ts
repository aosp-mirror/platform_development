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

import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {TimestampUtils} from './timestamp_utils';

describe('TimestampUtils', () => {
  const MILLISECOND = BigInt(1000000);
  const SECOND = BigInt(1000) * MILLISECOND;
  const MINUTE = BigInt(60) * SECOND;
  const HOUR = BigInt(60) * MINUTE;

  beforeAll(() => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
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

  describe('isNsFormat', () => {
    it('accepts all expected inputs', () => {
      expect(TimestampUtils.isNsFormat('123')).toBeTrue();
      expect(TimestampUtils.isNsFormat('123ns')).toBeTrue();
      expect(TimestampUtils.isNsFormat('123 ns')).toBeTrue();
      expect(TimestampUtils.isNsFormat(' 123 ns ')).toBeTrue();
      expect(TimestampUtils.isNsFormat('   123  ')).toBeTrue();
    });

    it('rejects all expected inputs', () => {
      expect(TimestampUtils.isNsFormat('1a23')).toBeFalse();
      expect(TimestampUtils.isNsFormat('a123 ns')).toBeFalse();
      expect(TimestampUtils.isNsFormat('')).toBeFalse();
    });
  });

  describe('isHumanElapsedTimeFormat', () => {
    it('accepts all expected inputs', () => {
      expect(TimestampUtils.isHumanElapsedTimeFormat('1000ns')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1s')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1s0ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1s0ms0ns')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('0d1s1ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1h0m')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1h1m1s1ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1d0s1ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1d1h0m1s1ms')).toBeTrue();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1d')).toBeTrue();
    });

    it('rejects all expected inputs', () => {
      expect(TimestampUtils.isHumanElapsedTimeFormat('1n')).toBeFalse();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1hr')).toBeFalse();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1min')).toBeFalse();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1sec')).toBeFalse();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1')).toBeFalse();
      expect(TimestampUtils.isHumanElapsedTimeFormat('1m0')).toBeFalse();
    });
  });

  describe('isRealTimeOnlyFormat', () => {
    it('accepts all expected inputs', () => {
      expect(TimestampUtils.isRealTimeOnlyFormat('22:04:54.186')).toBeTrue();
      expect(TimestampUtils.isRealTimeOnlyFormat('22:04:54.186777')).toBeTrue();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:04:54.186234769'),
      ).toBeTrue();
    });

    it('rejects all expected inputs', () => {
      expect(
        TimestampUtils.isRealTimeOnlyFormat('2022-11-10, 22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('2022-11-10T22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('2:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('25:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:4:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:04:4.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:60:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:04:60.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealTimeOnlyFormat('22:04:54.1861234562'),
      ).toBeFalse();
      expect(TimestampUtils.isRealTimeOnlyFormat('22:04:54.')).toBeFalse();
    });
  });

  describe('isRealDateTimeFormat', () => {
    it('accepts all expected inputs', () => {
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:54.186'),
      ).toBeTrue();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:54.186777'),
      ).toBeTrue();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:54.186234769'),
      ).toBeTrue();
    });

    it('rejects all expected inputs', () => {
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10T22:04:54.186234769'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-13-10, 22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-32, 22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 25:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:60:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:60.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:54.1861234568'),
      ).toBeFalse();
      expect(
        TimestampUtils.isRealDateTimeFormat('2022-11-10, 22:04:54.'),
      ).toBeFalse();
    });
  });

  describe('isISOFormat', () => {
    it('accepts all expected inputs', () => {
      expect(TimestampUtils.isISOFormat('2022-11-10T22:04:54.186')).toBeTrue();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T22:04:54.186777'),
      ).toBeTrue();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T22:04:54.186234769'),
      ).toBeTrue();
    });

    it('rejects all expected inputs', () => {
      expect(
        TimestampUtils.isISOFormat('2022-11-10, 22:04:54.186234769'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-13-10T22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-11-32T22:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T25:04:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T22:60:54.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T22:04:60.186123456'),
      ).toBeFalse();
      expect(
        TimestampUtils.isISOFormat('2022-11-10T22:04:54.1861234568'),
      ).toBeFalse();
      expect(TimestampUtils.isISOFormat('2022-11-10T22:04:54.')).toBeFalse();
    });
  });
});

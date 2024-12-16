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

import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeRange} from './time';
import {TIME_UNIT_TO_NANO} from './time_units';

describe('Timestamp', () => {
  describe('arithmetic', () => {
    const REAL_TIMESTAMP_10 = TimestampConverterUtils.makeRealTimestamp(10n);
    const REAL_TIMESTAMP_20 = TimestampConverterUtils.makeRealTimestamp(20n);
    const ELAPSED_TIMESTAMP_10 =
      TimestampConverterUtils.makeElapsedTimestamp(10n);
    const ELAPSED_TIMESTAMP_20 =
      TimestampConverterUtils.makeElapsedTimestamp(20n);

    it('can add', () => {
      let timestamp = REAL_TIMESTAMP_10.add(REAL_TIMESTAMP_20.getValueNs());
      expect(timestamp.getValueNs()).toBe(30n);

      timestamp = ELAPSED_TIMESTAMP_10.add(ELAPSED_TIMESTAMP_20.getValueNs());
      expect(timestamp.getValueNs()).toBe(30n);
    });

    it('can subtract', () => {
      let timestamp = REAL_TIMESTAMP_20.minus(REAL_TIMESTAMP_10.getValueNs());
      expect(timestamp.getValueNs()).toBe(10n);

      timestamp = ELAPSED_TIMESTAMP_20.minus(ELAPSED_TIMESTAMP_10.getValueNs());
      expect(timestamp.getValueNs()).toBe(10n);
    });

    it('can divide', () => {
      let timestamp = TimestampConverterUtils.makeRealTimestamp(10n).div(2n);
      expect(timestamp.getValueNs()).toBe(5n);

      timestamp = ELAPSED_TIMESTAMP_10.div(2n);
      expect(timestamp.getValueNs()).toBe(5n);
    });
  });

  describe('formatting', () => {
    const MILLISECOND = BigInt(TIME_UNIT_TO_NANO.ms);
    const SECOND = BigInt(TIME_UNIT_TO_NANO.s);
    const MINUTE = BigInt(TIME_UNIT_TO_NANO.m);
    const HOUR = BigInt(TIME_UNIT_TO_NANO.h);
    const DAY = BigInt(TIME_UNIT_TO_NANO.d);

    it('elapsed timestamps', () => {
      expect(TimestampConverterUtils.makeElapsedTimestamp(0n).format()).toEqual(
        '0ns',
      );
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(1000n).format(),
      ).toEqual('1000ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          10n * MILLISECOND,
        ).format(),
      ).toEqual('10ms0ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(SECOND - 1n).format(),
      ).toEqual('999ms999999ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(SECOND).format(),
      ).toEqual('1s0ms0ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          SECOND + MILLISECOND,
        ).format(),
      ).toEqual('1s1ms0ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MINUTE - 1n).format(),
      ).toEqual('59s999ms999999ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MINUTE).format(),
      ).toEqual('1m0s0ms0ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND,
        ).format(),
      ).toEqual('1m1s1ms0ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND + 1n,
        ).format(),
      ).toEqual('1m1s1ms1ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(HOUR - 1n).format(),
      ).toEqual('59m59s999ms999999ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(HOUR).format(),
      ).toEqual('1h0m0s0ms0ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          HOUR + MINUTE + SECOND + MILLISECOND,
        ).format(),
      ).toEqual('1h1m1s1ms0ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(DAY - 1n).format(),
      ).toEqual('23h59m59s999ms999999ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(DAY).format(),
      ).toEqual('1d0h0m0s0ms0ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          DAY + HOUR + MINUTE + SECOND + MILLISECOND,
        ).format(),
      ).toEqual('1d1h1m1s1ms0ns');
    });

    it('real timestamps without timezone info', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      expect(TimestampConverterUtils.makeRealTimestamp(0n).format()).toEqual(
        '1970-01-01, 00:00:00.000',
      );
      expect(
        TimestampConverterUtils.makeRealTimestamp(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(),
      ).toEqual('2022-11-10, 22:04:54.186');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022).format(),
      ).toEqual('2022-11-10, 00:00:00.000');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022 + 1n).format(),
      ).toEqual('2022-11-10, 00:00:00.000');

      expect(TimestampConverterUtils.makeRealTimestamp(0n).format()).toEqual(
        '1970-01-01, 00:00:00.000',
      );
      expect(
        TimestampConverterUtils.makeRealTimestamp(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(),
      ).toEqual('2022-11-10, 22:04:54.186');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022).format(),
      ).toEqual('2022-11-10, 00:00:00.000');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022 + 1n).format(),
      ).toEqual('2022-11-10, 00:00:00.000');
    });

    it('real timestamps with timezone info', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          0n,
        ).format(),
      ).toEqual('1970-01-01, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(),
      ).toEqual('2022-11-11, 03:34:54.186');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 + 1n,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000');

      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          0n,
        ).format(),
      ).toEqual('1970-01-01, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(),
      ).toEqual('2022-11-11, 03:34:54.186');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 + 1n,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000');
    });
  });
});

describe('TimeRange', () => {
  describe('containsTimestamp', () => {
    const range = new TimeRange(
      TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(10n),
      TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(600n),
    );

    it('returns true for range containing timestamp', () => {
      expect(
        range.containsTimestamp(
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(10n),
        ),
      ).toBeTrue();

      expect(
        range.containsTimestamp(
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(600n),
        ),
      ).toBeTrue();

      expect(
        range.containsTimestamp(
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(300n),
        ),
      ).toBeTrue();
    });

    it('returns false for range not containing timestamp', () => {
      expect(
        range.containsTimestamp(
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(0n),
        ),
      ).toBeFalse();

      expect(
        range.containsTimestamp(
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(601n),
        ),
      ).toBeFalse();
    });
  });
});

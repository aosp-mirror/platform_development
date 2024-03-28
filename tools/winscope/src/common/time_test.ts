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
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(0n).format(true),
      ).toEqual('0ms');
      expect(TimestampConverterUtils.makeElapsedTimestamp(0n).format()).toEqual(
        '0ns',
      );
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(1000n).format(true),
      ).toEqual('0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(1000n).format(),
      ).toEqual('1000ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MILLISECOND - 1n).format(
          true,
        ),
      ).toEqual('0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MILLISECOND).format(true),
      ).toEqual('1ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(10n * MILLISECOND).format(
          true,
        ),
      ).toEqual('10ms');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(SECOND - 1n).format(true),
      ).toEqual('999ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(SECOND).format(true),
      ).toEqual('1s0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          SECOND + MILLISECOND,
        ).format(true),
      ).toEqual('1s1ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          SECOND + MILLISECOND,
        ).format(),
      ).toEqual('1s1ms0ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MINUTE - 1n).format(true),
      ).toEqual('59s999ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(MINUTE).format(true),
      ).toEqual('1m0s0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND,
        ).format(true),
      ).toEqual('1m1s1ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND + 1n,
        ).format(true),
      ).toEqual('1m1s1ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND + 1n,
        ).format(),
      ).toEqual('1m1s1ms1ns');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(HOUR - 1n).format(true),
      ).toEqual('59m59s999ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(HOUR - 1n).format(),
      ).toEqual('59m59s999ms999999ns');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(HOUR).format(true),
      ).toEqual('1h0m0s0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          HOUR + MINUTE + SECOND + MILLISECOND,
        ).format(true),
      ).toEqual('1h1m1s1ms');

      expect(
        TimestampConverterUtils.makeElapsedTimestamp(DAY - 1n).format(true),
      ).toEqual('23h59m59s999ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(DAY).format(true),
      ).toEqual('1d0h0m0s0ms');
      expect(
        TimestampConverterUtils.makeElapsedTimestamp(
          DAY + HOUR + MINUTE + SECOND + MILLISECOND,
        ).format(true),
      ).toEqual('1d1h1m1s1ms');
    });

    it('real timestamps without timezone info', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      expect(
        TimestampConverterUtils.makeRealTimestamp(0n).format(true),
      ).toEqual('1970-01-01, 00:00:00.000');
      expect(
        TimestampConverterUtils.makeRealTimestamp(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(true),
      ).toEqual('2022-11-10, 22:04:54.186');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022).format(true),
      ).toEqual('2022-11-10, 00:00:00.000');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022 + 1n).format(
          true,
        ),
      ).toEqual('2022-11-10, 00:00:00.000');

      expect(TimestampConverterUtils.makeRealTimestamp(0n).format()).toEqual(
        '1970-01-01, 00:00:00.000000000',
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
      ).toEqual('2022-11-10, 22:04:54.186123212');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022).format(),
      ).toEqual('2022-11-10, 00:00:00.000000000');
      expect(
        TimestampConverterUtils.makeRealTimestamp(NOV_10_2022 + 1n).format(),
      ).toEqual('2022-11-10, 00:00:00.000000001');
    });

    it('real timestamps with timezone info', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          0n,
        ).format(true),
      ).toEqual('1970-01-01, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(true),
      ).toEqual('2022-11-11, 03:34:54.186');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022,
        ).format(true),
      ).toEqual('2022-11-10, 05:30:00.000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 + 1n,
        ).format(true),
      ).toEqual('2022-11-10, 05:30:00.000');

      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          0n,
        ).format(),
      ).toEqual('1970-01-01, 05:30:00.000000000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ).format(),
      ).toEqual('2022-11-11, 03:34:54.186123212');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000000000');
      expect(
        TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
          NOV_10_2022 + 1n,
        ).format(),
      ).toEqual('2022-11-10, 05:30:00.000000001');
    });
  });
});

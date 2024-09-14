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
import {TimestampConverter} from './timestamp_converter';
import {TIME_UNIT_TO_NANO} from './time_units';

describe('TimestampConverter', () => {
  const MILLISECOND = BigInt(TIME_UNIT_TO_NANO.ms);
  const SECOND = BigInt(TIME_UNIT_TO_NANO.s);
  const MINUTE = BigInt(TIME_UNIT_TO_NANO.m);
  const HOUR = BigInt(TIME_UNIT_TO_NANO.h);
  const DAY = BigInt(TIME_UNIT_TO_NANO.d);

  const testElapsedNs = 100n;
  const testRealNs = 1659243341051481088n; // Sun, 31 Jul 2022 04:55:41 GMT to test timestamp conversion between different days
  const testMonotonicTimeOffsetNs = 5n * MILLISECOND;
  const testRealToBootTimeOffsetNs = MILLISECOND;

  beforeAll(() => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
  });

  describe('makes timestamps from ns without timezone info', () => {
    const converterWithMonotonicOffset = new TimestampConverter(
      TimestampConverterUtils.UTC_TIMEZONE_INFO,
    );
    converterWithMonotonicOffset.setRealToMonotonicTimeOffsetNs(
      testMonotonicTimeOffsetNs,
    );

    const converterWithBootTimeOffset = new TimestampConverter(
      TimestampConverterUtils.UTC_TIMEZONE_INFO,
    );
    converterWithBootTimeOffset.setRealToBootTimeOffsetNs(
      testRealToBootTimeOffsetNs,
    );

    it('can create real-formatted timestamp without real-time offset set', () => {
      const timestamp = new TimestampConverter(
        TimestampConverterUtils.UTC_TIMEZONE_INFO,
      ).makeTimestampFromRealNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(testRealNs);
      expect(timestamp.format()).toEqual('2022-07-31, 04:55:41.051');
    });

    it('can create real-formatted timestamp with real to monotonic offset', () => {
      const timestamp =
        converterWithMonotonicOffset.makeTimestampFromMonotonicNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testMonotonicTimeOffsetNs,
      );
      expect(timestamp.format()).toEqual('2022-07-31, 04:55:41.056');
    });

    it('can create real-formatted timestamp with real to boot time offset', () => {
      const timestamp =
        converterWithBootTimeOffset.makeTimestampFromBootTimeNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToBootTimeOffsetNs,
      );
      expect(timestamp.format()).toEqual('2022-07-31, 04:55:41.052');
    });

    it('can create elapsed-formatted timestamp', () => {
      const timestamp = new TimestampConverter(
        TimestampConverterUtils.UTC_TIMEZONE_INFO,
      ).makeTimestampFromMonotonicNs(testElapsedNs);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
      expect(timestamp.format()).toEqual('100ns');
    });

    it('formats real-formatted timestamp with offset correctly', () => {
      expect(
        converterWithMonotonicOffset
          .makeTimestampFromMonotonicNs(100n * MILLISECOND)
          .format(),
      ).toEqual('1970-01-01, 00:00:00.105');
      expect(
        converterWithMonotonicOffset
          .makeTimestampFromRealNs(100n * MILLISECOND)
          .format(),
      ).toEqual('1970-01-01, 00:00:00.100');
    });
  });

  describe('makes timestamps from ns with timezone info', () => {
    const converterWithMonotonicOffset = new TimestampConverter(
      TimestampConverterUtils.ASIA_TIMEZONE_INFO,
    );
    converterWithMonotonicOffset.setRealToMonotonicTimeOffsetNs(
      testMonotonicTimeOffsetNs,
    );
    converterWithMonotonicOffset.initializeUTCOffset(
      converterWithMonotonicOffset.makeTimestampFromRealNs(testRealNs),
    );

    const converterWithBootTimeOffset = new TimestampConverter(
      TimestampConverterUtils.ASIA_TIMEZONE_INFO,
    );
    converterWithBootTimeOffset.setRealToBootTimeOffsetNs(
      testRealToBootTimeOffsetNs,
    );
    converterWithBootTimeOffset.initializeUTCOffset(
      converterWithBootTimeOffset.makeTimestampFromRealNs(testRealNs),
    );

    it('can create real-formatted timestamp without real-time offset set', () => {
      const converter = new TimestampConverter(
        TimestampConverterUtils.ASIA_TIMEZONE_INFO,
      );
      converter.initializeUTCOffset(
        converter.makeTimestampFromRealNs(testRealNs),
      );

      const timestamp = converter.makeTimestampFromRealNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(testRealNs);
      expect(timestamp.format()).toEqual('2022-07-31, 10:25:41.051');
    });

    it('can create real-formatted timestamp with monotonic offset', () => {
      const timestamp =
        converterWithMonotonicOffset.makeTimestampFromMonotonicNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testMonotonicTimeOffsetNs,
      );
      expect(timestamp.format()).toEqual('2022-07-31, 10:25:41.056');
    });

    it('can create real-formatted timestamp with real to boot time offset', () => {
      const timestamp =
        converterWithBootTimeOffset.makeTimestampFromBootTimeNs(testRealNs);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToBootTimeOffsetNs,
      );
      expect(timestamp.format()).toEqual('2022-07-31, 10:25:41.052');
    });

    it('can create elapsed-formatted timestamp', () => {
      const timestamp = new TimestampConverter(
        TimestampConverterUtils.ASIA_TIMEZONE_INFO,
      ).makeTimestampFromMonotonicNs(testElapsedNs);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
      expect(timestamp.format()).toEqual('100ns');
    });

    describe('adds correct offset for different timezones', () => {
      it('creates correct real-formatted timestamps for different timezones', () => {
        const londonConverter = new TimestampConverter(
          {
            timezone: 'Europe/London',
            locale: 'en-US',
          },
          0n,
        );
        londonConverter.initializeUTCOffset(
          londonConverter.makeTimestampFromRealNs(testRealNs),
        );
        expect(
          londonConverter.makeTimestampFromRealNs(testRealNs).format(),
        ).toEqual('2022-07-31, 05:55:41.051');

        const zurichConverter = new TimestampConverter(
          {
            timezone: 'Europe/Zurich',
            locale: 'en-US',
          },
          0n,
        );
        zurichConverter.initializeUTCOffset(
          zurichConverter.makeTimestampFromRealNs(testRealNs),
        );
        expect(
          zurichConverter.makeTimestampFromRealNs(testRealNs).format(),
        ).toEqual('2022-07-31, 06:55:41.051');

        const westCoastConverter = new TimestampConverter(
          {
            timezone: 'America/Los_Angeles',
            locale: 'en-US',
          },
          0n,
        );
        westCoastConverter.initializeUTCOffset(
          westCoastConverter.makeTimestampFromRealNs(testRealNs),
        );
        expect(
          westCoastConverter.makeTimestampFromRealNs(testRealNs).format(),
        ).toEqual('2022-07-30, 21:55:41.051');

        const indiaConverter = new TimestampConverter(
          {
            timezone: 'Asia/Kolkata',
            locale: 'en-US',
          },
          0n,
        );
        indiaConverter.initializeUTCOffset(
          indiaConverter.makeTimestampFromRealNs(testRealNs),
        );
        expect(
          indiaConverter.makeTimestampFromRealNs(testRealNs).format(),
        ).toEqual('2022-07-31, 10:25:41.051');
      });
    });
  });

  describe('makes timestamps from string without timezone info', () => {
    const converterWithoutOffsets = new TimestampConverter(
      TimestampConverterUtils.UTC_TIMEZONE_INFO,
    );

    const converterWithMonotonicOffset = new TimestampConverter(
      TimestampConverterUtils.UTC_TIMEZONE_INFO,
    );
    converterWithMonotonicOffset.setRealToMonotonicTimeOffsetNs(
      testMonotonicTimeOffsetNs,
    );

    it('makeTimestampfromHumanElapsed', () => {
      expect(converterWithoutOffsets.makeTimestampFromHuman('0ns')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(0n),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1000ns')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(1000n),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('0ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(0n),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(MILLISECOND),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('10ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(10n * MILLISECOND),
      );

      expect(converterWithoutOffsets.makeTimestampFromHuman('999ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(
          999n * MILLISECOND,
        ),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1s')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(SECOND),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1s0ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(SECOND),
      );
      expect(
        converterWithoutOffsets.makeTimestampFromHuman('1s0ms0ns'),
      ).toEqual(converterWithoutOffsets.makeTimestampFromMonotonicNs(SECOND));
      expect(
        converterWithoutOffsets.makeTimestampFromHuman('1s0ms1ns'),
      ).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(SECOND + 1n),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('0d1s1ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(
          SECOND + MILLISECOND,
        ),
      );

      expect(converterWithoutOffsets.makeTimestampFromHuman('1m0s0ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(MINUTE),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1m1s1ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(
          MINUTE + SECOND + MILLISECOND,
        ),
      );

      expect(converterWithoutOffsets.makeTimestampFromHuman('1h0m')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(HOUR),
      );
      expect(
        converterWithoutOffsets.makeTimestampFromHuman('1h1m1s1ms'),
      ).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(
          HOUR + MINUTE + SECOND + MILLISECOND,
        ),
      );

      expect(converterWithoutOffsets.makeTimestampFromHuman('1d0s1ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(DAY + MILLISECOND),
      );
      expect(
        converterWithoutOffsets.makeTimestampFromHuman('1d1h1m1s1ms'),
      ).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(
          DAY + HOUR + MINUTE + SECOND + MILLISECOND,
        ),
      );

      expect(converterWithoutOffsets.makeTimestampFromHuman('1d')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(DAY),
      );
      expect(converterWithoutOffsets.makeTimestampFromHuman('1d1ms')).toEqual(
        converterWithoutOffsets.makeTimestampFromMonotonicNs(DAY + MILLISECOND),
      );
    });

    it('makeTimestampfromHumanElapsed throws on invalid input format', () => {
      const invalidFormatError = new Error('Invalid timestamp format');
      expect(() =>
        converterWithoutOffsets.makeTimestampFromHuman('1d1h1m1s0ns1ms'),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithoutOffsets.makeTimestampFromHuman('1dns'),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithoutOffsets.makeTimestampFromHuman('100'),
      ).toThrow(invalidFormatError);
      expect(() => converterWithoutOffsets.makeTimestampFromHuman('')).toThrow(
        invalidFormatError,
      );
    });

    it('makeTimestampFromHumanReal', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T22:04:54.186123212',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186123212n,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T22:04:54.186123212Z',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T22:04:54.186000212',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            212n,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T22:04:54.006000002',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 6000002n,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.006000002',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND + 6000002n,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.0',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.0100',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 +
            6n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            10n * MILLISECOND,
        ),
      );
      expect(
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.0175328',
        ),
      ).toEqual(
        converterWithMonotonicOffset.makeTimestampFromRealNs(
          NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND + 17532800n,
        ),
      );
    });

    it('makeTimestampFromHumanReal throws on invalid input format', () => {
      const invalidFormatError = new Error('Invalid timestamp format');
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman('100'),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '06h4m54s, 10 Nov 2022',
        ),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman(''),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.',
        ),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '2022-11-10T06:04:54.1234567890',
        ),
      ).toThrow(invalidFormatError);
      expect(() =>
        converterWithMonotonicOffset.makeTimestampFromHuman(
          '06:04:54.1234567890',
        ),
      ).toThrow(invalidFormatError);
    });

    it('can reverse-date format', () => {
      expect(
        converterWithMonotonicOffset
          .makeTimestampFromHuman('2022-11-10, 22:04:54.186123212')
          .format(),
      ).toEqual('2022-11-10, 22:04:54.186');
    });
  });

  describe('makes timestamps from string with timezone info', () => {
    const converter = new TimestampConverter(
      TimestampConverterUtils.ASIA_TIMEZONE_INFO,
    );
    converter.setRealToMonotonicTimeOffsetNs(testMonotonicTimeOffsetNs);
    converter.initializeUTCOffset(
      converter.makeTimestampFromRealNs(testRealNs),
    );

    it('makeTimestampFromHumanReal', () => {
      const NOV_10_2022 = 1668038400000n * MILLISECOND;
      testMakeTimestampFromHumanReal(
        '2022-11-11T03:34:54.186123212',
        NOV_10_2022 +
          22n * HOUR +
          4n * MINUTE +
          54n * SECOND +
          186n * MILLISECOND +
          123212n,
        '2022-11-11, 03:34:54.186',
      );

      testMakeTimestampFromHumanReal(
        '2022-11-11T03:34:54.186123212Z',
        NOV_10_2022 +
          22n * HOUR +
          4n * MINUTE +
          54n * SECOND +
          186n * MILLISECOND +
          123212n,
        '2022-11-11, 03:34:54.186',
      );

      testMakeTimestampFromHumanReal(
        '2022-11-10T11:34:54',
        NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND,
        '2022-11-10, 11:34:54.000',
      );

      testMakeTimestampFromHumanReal(
        '2022-11-10T11:34:54.0',
        NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND,
        '2022-11-10, 11:34:54.000',
      );

      testMakeTimestampFromHumanReal(
        '2022-11-10T11:34:54.0100',
        NOV_10_2022 +
          6n * HOUR +
          4n * MINUTE +
          54n * SECOND +
          10n * MILLISECOND,
        '2022-11-10, 11:34:54.010',
      );

      testMakeTimestampFromHumanReal(
        '2022-11-10T11:34:54.0175328',
        NOV_10_2022 + 6n * HOUR + 4n * MINUTE + 54n * SECOND + 17532800n,
        '2022-11-10, 11:34:54.018',
      );
    });

    it('can reverse-date format', () => {
      expect(
        converter
          .makeTimestampFromHuman('2022-11-11, 03:34:54.186123212')
          .format(),
      ).toEqual('2022-11-11, 03:34:54.186');
    });

    function testMakeTimestampFromHumanReal(
      timestampHuman: string,
      expectedNs: bigint,
      expectedFormattedTimestamp: string,
    ) {
      const timestamp = converter.makeTimestampFromHuman(timestampHuman);
      expect(timestamp.getValueNs()).toEqual(expectedNs);
      expect(timestamp.format()).toEqual(expectedFormattedTimestamp);
    }
  });
});

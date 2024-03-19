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

import {NO_TIMEZONE_OFFSET_FACTORY} from './timestamp_factory';
import {TimestampUtils} from './timestamp_utils';

describe('TimestampUtils', () => {
  const MILLISECOND = BigInt(1000000);
  const SECOND = BigInt(1000) * MILLISECOND;
  const MINUTE = BigInt(60) * SECOND;
  const HOUR = BigInt(60) * MINUTE;
  const DAY = BigInt(24) * HOUR;

  describe('compareFn', () => {
    it('throws if timestamps have different type', () => {
      const real = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n);
      const elapsed = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(10n);

      expect(() => {
        TimestampUtils.compareFn(real, elapsed);
      }).toThrow();
    });

    it('allows to sort arrays', () => {
      const array = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(12n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(110n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(11n),
      ];
      array.sort(TimestampUtils.compareFn);

      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(11n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(12n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(110n),
      ];
      expect(array).toEqual(expected);
    });
  });

  it('nanosecondsToHuman', () => {
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
        true,
      ),
    ).toEqual('0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
        false,
      ),
    ).toEqual('0ns');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1000n),
        true,
      ),
    ).toEqual('0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1000n),
        false,
      ),
    ).toEqual('1000ns');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MILLISECOND - 1n),
        true,
      ),
    ).toEqual('0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MILLISECOND),
        true,
      ),
    ).toEqual('1ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(10n * MILLISECOND),
        true,
      ),
    ).toEqual('10ms');

    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND - 1n),
        true,
      ),
    ).toEqual('999ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND),
        true,
      ),
    ).toEqual('1s0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND + MILLISECOND),
        true,
      ),
    ).toEqual('1s1ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND + MILLISECOND),
        false,
      ),
    ).toEqual('1s1ms0ns');

    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MINUTE - 1n),
        true,
      ),
    ).toEqual('59s999ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MINUTE),
        true,
      ),
    ).toEqual('1m0s0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND,
        ),
        true,
      ),
    ).toEqual('1m1s1ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND + 1n,
        ),
        true,
      ),
    ).toEqual('1m1s1ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
          MINUTE + SECOND + MILLISECOND + 1n,
        ),
        false,
      ),
    ).toEqual('1m1s1ms1ns');

    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(HOUR - 1n),
        true,
      ),
    ).toEqual('59m59s999ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(HOUR - 1n),
        false,
      ),
    ).toEqual('59m59s999ms999999ns');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(HOUR),
        true,
      ),
    ).toEqual('1h0m0s0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
          HOUR + MINUTE + SECOND + MILLISECOND,
        ),
        true,
      ),
    ).toEqual('1h1m1s1ms');

    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(DAY - 1n),
        true,
      ),
    ).toEqual('23h59m59s999ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(DAY),
        true,
      ),
    ).toEqual('1d0h0m0s0ms');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
          DAY + HOUR + MINUTE + SECOND + MILLISECOND,
        ),
        true,
      ),
    ).toEqual('1d1h1m1s1ms');
  });

  it('humanElapsedToNanoseconds', () => {
    expect(TimestampUtils.parseHumanElapsed('0ns')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
    );
    expect(TimestampUtils.parseHumanElapsed('1000ns')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(1000n),
    );
    expect(TimestampUtils.parseHumanElapsed('0ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
    );
    expect(TimestampUtils.parseHumanElapsed('1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MILLISECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('10ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(10n * MILLISECOND),
    );

    expect(TimestampUtils.parseHumanElapsed('999ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(999n * MILLISECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('1s')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('1s0ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('1s0ms0ns')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('1s0ms1ns')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND + 1n),
    );
    expect(TimestampUtils.parseHumanElapsed('0d1s1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(SECOND + MILLISECOND),
    );

    expect(TimestampUtils.parseHumanElapsed('1m0s0ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(MINUTE),
    );
    expect(TimestampUtils.parseHumanElapsed('1m1s1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
        MINUTE + SECOND + MILLISECOND,
      ),
    );

    expect(TimestampUtils.parseHumanElapsed('1h0m')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(HOUR),
    );
    expect(TimestampUtils.parseHumanElapsed('1h1m1s1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
        HOUR + MINUTE + SECOND + MILLISECOND,
      ),
    );

    expect(TimestampUtils.parseHumanElapsed('1d0s1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(DAY + MILLISECOND),
    );
    expect(TimestampUtils.parseHumanElapsed('1d1h1m1s1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
        DAY + HOUR + MINUTE + SECOND + MILLISECOND,
      ),
    );

    expect(TimestampUtils.parseHumanElapsed('1d')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(DAY),
    );
    expect(TimestampUtils.parseHumanElapsed('1d1ms')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(DAY + MILLISECOND),
    );
  });

  it('humanToNanoseconds throws on invalid input format', () => {
    const invalidFormatError = new Error('Invalid elapsed timestamp format');
    expect(() => TimestampUtils.parseHumanElapsed('1d1h1m1s0ns1ms')).toThrow(
      invalidFormatError,
    );
    expect(() => TimestampUtils.parseHumanElapsed('1dns')).toThrow(
      invalidFormatError,
    );
    expect(() => TimestampUtils.parseHumanElapsed('100')).toThrow(
      invalidFormatError,
    );
    expect(() => TimestampUtils.parseHumanElapsed('')).toThrow(
      invalidFormatError,
    );
  });

  it('nanosecondsToHumanReal', () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
        true,
      ),
    ).toEqual('1970-01-01T00:00:00.000');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ),
        true,
      ),
    ).toEqual('2022-11-10T22:04:54.186');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(NOV_10_2022),
        true,
      ),
    ).toEqual('2022-11-10T00:00:00.000');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(NOV_10_2022 + 1n),
        true,
      ),
    ).toEqual('2022-11-10T00:00:00.000');

    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
        false,
      ),
    ).toEqual('1970-01-01T00:00:00.000000000');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(
          NOV_10_2022 +
            22n * HOUR +
            4n * MINUTE +
            54n * SECOND +
            186n * MILLISECOND +
            123212n,
        ),
        false,
      ),
    ).toEqual('2022-11-10T22:04:54.186123212');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(NOV_10_2022),
        false,
      ),
    ).toEqual('2022-11-10T00:00:00.000000000');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(NOV_10_2022 + 1n),
        false,
      ),
    ).toEqual('2022-11-10T00:00:00.000000001');
  });

  it('humanRealToNanoseconds', () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T22:04:54.186123212'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(
        NOV_10_2022 +
          22n * HOUR +
          4n * MINUTE +
          54n * SECOND +
          186n * MILLISECOND +
          123212n,
      ),
    );
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T22:04:54.186123212Z'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668117894186123212n),
    );
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T22:04:54.186000212'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668117894186000212n),
    );
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T22:04:54.006000002'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668117894006000002n),
    );
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T06:04:54.006000002'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668060294006000002n),
    );
    expect(TimestampUtils.parseHumanReal('2022-11-10T06:04:54')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668060294000000000n),
    );
    expect(TimestampUtils.parseHumanReal('2022-11-10T06:04:54.0')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668060294000000000n),
    );
    expect(TimestampUtils.parseHumanReal('2022-11-10T06:04:54.0100')).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668060294010000000n),
    );
    expect(
      TimestampUtils.parseHumanReal('2022-11-10T06:04:54.0175328'),
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668060294017532800n),
    );
  });

  it('canReverseDateFormatting', () => {
    let timestamp =
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1668117894186123212n);
    expect(
      TimestampUtils.parseHumanReal(TimestampUtils.format(timestamp)),
    ).toEqual(timestamp);

    timestamp = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
      DAY + HOUR + MINUTE + SECOND + MILLISECOND + 1n,
    );
    expect(
      TimestampUtils.parseHumanElapsed(TimestampUtils.format(timestamp)),
    ).toEqual(timestamp);
  });

  it('humanToNanoseconds throws on invalid input format', () => {
    const invalidFormatError = new Error('Invalid real timestamp format');
    expect(() => TimestampUtils.parseHumanReal('23h59m59s999ms5ns')).toThrow(
      invalidFormatError,
    );
    expect(() => TimestampUtils.parseHumanReal('1d')).toThrow(
      invalidFormatError,
    );
    expect(() => TimestampUtils.parseHumanReal('100')).toThrow(
      invalidFormatError,
    );
    expect(() =>
      TimestampUtils.parseHumanReal('06h4m54s, 10 Nov 2022'),
    ).toThrow(invalidFormatError);
    expect(() => TimestampUtils.parseHumanReal('')).toThrow(invalidFormatError);
    expect(() => TimestampUtils.parseHumanReal('2022-11-10T06:04:54.')).toThrow(
      invalidFormatError,
    );
    expect(() =>
      TimestampUtils.parseHumanReal('2022-11-10T06:04:54.1234567890'),
    ).toThrow(invalidFormatError);
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

  it('format real', () => {
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n, 500n),
      ),
    ).toEqual('1970-01-01T00:00:00.000000600');
    expect(
      TimestampUtils.format(
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n * MILLISECOND, 500n),
        true,
      ),
    ).toEqual('1970-01-01T00:00:00.100');
  });

  it('format elapsed', () => {
    const timestamp = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(
      100n * MILLISECOND,
    );
    expect(TimestampUtils.format(timestamp, true)).toEqual('100ms');
    expect(TimestampUtils.format(timestamp)).toEqual('100ms0ns');
  });
});

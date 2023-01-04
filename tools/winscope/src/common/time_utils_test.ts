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
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TimeUtils} from './time_utils';

describe('TimeUtils', () => {
  const MILLISECOND = BigInt(1000000);
  const SECOND = BigInt(1000) * MILLISECOND;
  const MINUTE = BigInt(60) * SECOND;
  const HOUR = BigInt(60) * MINUTE;
  const DAY = BigInt(24) * HOUR;

  describe('compareFn', () => {
    it('throws if timestamps have different type', () => {
      const real = new RealTimestamp(10n);
      const elapsed = new ElapsedTimestamp(10n);

      expect(() => {
        TimeUtils.compareFn(real, elapsed);
      }).toThrow();
    });

    it('allows to sort arrays', () => {
      const array = [
        new RealTimestamp(100n),
        new RealTimestamp(10n),
        new RealTimestamp(12n),
        new RealTimestamp(110n),
        new RealTimestamp(11n),
      ];
      array.sort(TimeUtils.compareFn);

      const expected = [
        new RealTimestamp(10n),
        new RealTimestamp(11n),
        new RealTimestamp(12n),
        new RealTimestamp(100n),
        new RealTimestamp(110n),
      ];
      expect(array).toEqual(expected);
    });
  });

  it('nanosecondsToHuman', () => {
    expect(TimeUtils.format(new ElapsedTimestamp(0n), true)).toEqual('0ms');
    expect(TimeUtils.format(new ElapsedTimestamp(0n), false)).toEqual('0ns');
    expect(TimeUtils.format(new ElapsedTimestamp(1000n), true)).toEqual('0ms');
    expect(TimeUtils.format(new ElapsedTimestamp(1000n), false)).toEqual('1000ns');
    expect(TimeUtils.format(new ElapsedTimestamp(MILLISECOND - 1n), true)).toEqual('0ms');
    expect(TimeUtils.format(new ElapsedTimestamp(MILLISECOND), true)).toEqual('1ms');
    expect(TimeUtils.format(new ElapsedTimestamp(10n * MILLISECOND), true)).toEqual('10ms');

    expect(TimeUtils.format(new ElapsedTimestamp(SECOND - 1n), true)).toEqual('999ms');
    expect(TimeUtils.format(new ElapsedTimestamp(SECOND), true)).toEqual('1s0ms');
    expect(TimeUtils.format(new ElapsedTimestamp(SECOND + MILLISECOND), true)).toEqual('1s1ms');
    expect(TimeUtils.format(new ElapsedTimestamp(SECOND + MILLISECOND), false)).toEqual('1s1ms0ns');

    expect(TimeUtils.format(new ElapsedTimestamp(MINUTE - 1n), true)).toEqual('59s999ms');
    expect(TimeUtils.format(new ElapsedTimestamp(MINUTE), true)).toEqual('1m0s0ms');
    expect(TimeUtils.format(new ElapsedTimestamp(MINUTE + SECOND + MILLISECOND), true)).toEqual(
      '1m1s1ms'
    );
    expect(
      TimeUtils.format(new ElapsedTimestamp(MINUTE + SECOND + MILLISECOND + 1n), true)
    ).toEqual('1m1s1ms');
    expect(
      TimeUtils.format(new ElapsedTimestamp(MINUTE + SECOND + MILLISECOND + 1n), false)
    ).toEqual('1m1s1ms1ns');

    expect(TimeUtils.format(new ElapsedTimestamp(HOUR - 1n), true)).toEqual('59m59s999ms');
    expect(TimeUtils.format(new ElapsedTimestamp(HOUR - 1n), false)).toEqual('59m59s999ms999999ns');
    expect(TimeUtils.format(new ElapsedTimestamp(HOUR), true)).toEqual('1h0m0s0ms');
    expect(
      TimeUtils.format(new ElapsedTimestamp(HOUR + MINUTE + SECOND + MILLISECOND), true)
    ).toEqual('1h1m1s1ms');

    expect(TimeUtils.format(new ElapsedTimestamp(DAY - 1n), true)).toEqual('23h59m59s999ms');
    expect(TimeUtils.format(new ElapsedTimestamp(DAY), true)).toEqual('1d0h0m0s0ms');
    expect(
      TimeUtils.format(new ElapsedTimestamp(DAY + HOUR + MINUTE + SECOND + MILLISECOND), true)
    ).toEqual('1d1h1m1s1ms');
  });

  it('humanElapsedToNanoseconds', () => {
    expect(TimeUtils.parseHumanElapsed('0ns')).toEqual(new ElapsedTimestamp(0n));
    expect(TimeUtils.parseHumanElapsed('1000ns')).toEqual(new ElapsedTimestamp(1000n));
    expect(TimeUtils.parseHumanElapsed('0ms')).toEqual(new ElapsedTimestamp(0n));
    expect(TimeUtils.parseHumanElapsed('1ms')).toEqual(new ElapsedTimestamp(MILLISECOND));
    expect(TimeUtils.parseHumanElapsed('10ms')).toEqual(new ElapsedTimestamp(10n * MILLISECOND));

    expect(TimeUtils.parseHumanElapsed('999ms')).toEqual(new ElapsedTimestamp(999n * MILLISECOND));
    expect(TimeUtils.parseHumanElapsed('1s')).toEqual(new ElapsedTimestamp(SECOND));
    expect(TimeUtils.parseHumanElapsed('1s0ms')).toEqual(new ElapsedTimestamp(SECOND));
    expect(TimeUtils.parseHumanElapsed('1s0ms0ns')).toEqual(new ElapsedTimestamp(SECOND));
    expect(TimeUtils.parseHumanElapsed('1s0ms1ns')).toEqual(new ElapsedTimestamp(SECOND + 1n));
    expect(TimeUtils.parseHumanElapsed('0d1s1ms')).toEqual(
      new ElapsedTimestamp(SECOND + MILLISECOND)
    );

    expect(TimeUtils.parseHumanElapsed('1m0s0ms')).toEqual(new ElapsedTimestamp(MINUTE));
    expect(TimeUtils.parseHumanElapsed('1m1s1ms')).toEqual(
      new ElapsedTimestamp(MINUTE + SECOND + MILLISECOND)
    );

    expect(TimeUtils.parseHumanElapsed('1h0m')).toEqual(new ElapsedTimestamp(HOUR));
    expect(TimeUtils.parseHumanElapsed('1h1m1s1ms')).toEqual(
      new ElapsedTimestamp(HOUR + MINUTE + SECOND + MILLISECOND)
    );

    expect(TimeUtils.parseHumanElapsed('1d0s1ms')).toEqual(new ElapsedTimestamp(DAY + MILLISECOND));
    expect(TimeUtils.parseHumanElapsed('1d1h1m1s1ms')).toEqual(
      new ElapsedTimestamp(DAY + HOUR + MINUTE + SECOND + MILLISECOND)
    );

    expect(TimeUtils.parseHumanElapsed('1d')).toEqual(new ElapsedTimestamp(DAY));
    expect(TimeUtils.parseHumanElapsed('1d1ms')).toEqual(new ElapsedTimestamp(DAY + MILLISECOND));
  });

  it('humanToNanoseconds throws on invalid input format', () => {
    const invalidFormatError = new Error('Invalid elapsed timestamp format');
    expect(() => TimeUtils.parseHumanElapsed('1d1h1m1s0ns1ms')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanElapsed('1dns')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanElapsed('100')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanElapsed('')).toThrow(invalidFormatError);
  });

  it('nanosecondsToHumanReal', () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(TimeUtils.format(new RealTimestamp(0n), true)).toEqual('1970-01-01T00:00:00.000');
    expect(
      TimeUtils.format(
        new RealTimestamp(
          NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186n * MILLISECOND + 123212n
        ),
        true
      )
    ).toEqual('2022-11-10T22:04:54.186');
    expect(TimeUtils.format(new RealTimestamp(NOV_10_2022), true)).toEqual(
      '2022-11-10T00:00:00.000'
    );
    expect(TimeUtils.format(new RealTimestamp(NOV_10_2022 + 1n), true)).toEqual(
      '2022-11-10T00:00:00.000'
    );

    expect(TimeUtils.format(new RealTimestamp(0n), false)).toEqual('1970-01-01T00:00:00.000000000');
    expect(
      TimeUtils.format(
        new RealTimestamp(
          NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186n * MILLISECOND + 123212n
        ),
        false
      )
    ).toEqual('2022-11-10T22:04:54.186123212');
    expect(TimeUtils.format(new RealTimestamp(NOV_10_2022), false)).toEqual(
      '2022-11-10T00:00:00.000000000'
    );
    expect(TimeUtils.format(new RealTimestamp(NOV_10_2022 + 1n), false)).toEqual(
      '2022-11-10T00:00:00.000000001'
    );
  });

  it('humanRealToNanoseconds', () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(TimeUtils.parseHumanReal('2022-11-10T22:04:54.186123212')).toEqual(
      new RealTimestamp(
        NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186n * MILLISECOND + 123212n
      )
    );
    expect(TimeUtils.parseHumanReal('2022-11-10T22:04:54.186123212Z')).toEqual(
      new RealTimestamp(1668117894186123212n)
    );
    expect(TimeUtils.parseHumanReal('2022-11-10T22:04:54.186000212')).toEqual(
      new RealTimestamp(1668117894186000212n)
    );
    expect(TimeUtils.parseHumanReal('2022-11-10T22:04:54.006000002')).toEqual(
      new RealTimestamp(1668117894006000002n)
    );
    expect(TimeUtils.parseHumanReal('2022-11-10T06:04:54.006000002')).toEqual(
      new RealTimestamp(1668060294006000002n)
    );
  });

  it('canReverseDateFormatting', () => {
    let timestamp = new RealTimestamp(1668117894186123212n);
    expect(TimeUtils.parseHumanReal(TimeUtils.format(timestamp))).toEqual(timestamp);

    timestamp = new ElapsedTimestamp(DAY + HOUR + MINUTE + SECOND + MILLISECOND + 1n);
    expect(TimeUtils.parseHumanElapsed(TimeUtils.format(timestamp))).toEqual(timestamp);
  });

  it('humanToNanoseconds throws on invalid input format', () => {
    const invalidFormatError = new Error('Invalid real timestamp format');
    expect(() => TimeUtils.parseHumanReal('23h59m59s999ms5ns')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanReal('1d')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanReal('100')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanReal('06h4m54s, 10 Nov 2022')).toThrow(invalidFormatError);
    expect(() => TimeUtils.parseHumanReal('')).toThrow(invalidFormatError);
  });

  it('nano second regex accept all expected inputs', () => {
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('123')).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('123ns')).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('123 ns')).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test(' 123 ns ')).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('   123  ')).toBeTrue();

    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('1a23')).toBeFalse();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('a123 ns')).toBeFalse();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test('')).toBeFalse();
  });

  it('format real', () => {
    expect(TimeUtils.format(Timestamp.from(TimestampType.REAL, 100n, 500n))).toEqual(
      '1970-01-01T00:00:00.000000600'
    );
    expect(
      TimeUtils.format(Timestamp.from(TimestampType.REAL, 100n * MILLISECOND, 500n), true)
    ).toEqual('1970-01-01T00:00:00.100');
  });

  it('format elapsed', () => {
    expect(
      TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND, 500n), true)
    ).toEqual('100ms');
    expect(
      TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND), true)
    ).toEqual('100ms');
    expect(
      TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND, 500n))
    ).toEqual('100ms0ns');
    expect(TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND))).toEqual(
      '100ms0ns'
    );
  });
});

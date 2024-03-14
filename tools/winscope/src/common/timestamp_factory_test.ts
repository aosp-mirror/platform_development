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

import {TimestampType} from './time';
import {TimestampFactory} from './timestamp_factory';

describe('TimestampFactory', () => {
  const testElapsedNs = 100n;
  const testRealNs = 1659243341051481088n; // Sun, 31 Jul 2022 04:55:41 GMT to test timestamp conversion between different days
  const testRealToElapsedOffsetNs = 500n;

  describe('without timezone info', () => {
    const factory = new TimestampFactory();
    it('can create real timestamp', () => {
      const timestamp = factory.makeRealTimestamp(testRealNs);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(testRealNs);
    });

    it('can create real timestamp with offset', () => {
      const timestamp = factory.makeRealTimestamp(
        testRealNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs,
      );
    });

    it('can create elapsed timestamp', () => {
      const timestamp = factory.makeElapsedTimestamp(testElapsedNs);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
    });

    it('can create real timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.REAL,
        testRealNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs,
      );
    });

    it('can create elapsed timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        testElapsedNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
    });

    it('can create elapsed timestamp from type ignoring offset', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        testElapsedNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
    });

    it('throws error if creating real timestamp from type without offset', () => {
      expect(() =>
        factory.makeTimestampFromType(TimestampType.REAL, testRealNs),
      ).toThrow();
    });
  });

  describe('with timezone info', () => {
    const factory = new TimestampFactory({
      timezone: 'Asia/Kolkata',
      locale: 'en-US',
    });
    const expectedUtcOffsetNs = 19800000000000n;

    it('can create real timestamp', () => {
      const timestamp = factory.makeRealTimestamp(testRealNs);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(testRealNs + expectedUtcOffsetNs);
      expect(timestamp.toUTC().getValueNs()).toBe(testRealNs);
    });

    it('can create real timestamp with offset', () => {
      const timestamp = factory.makeRealTimestamp(
        testRealNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs + expectedUtcOffsetNs,
      );
      expect(timestamp.toUTC().getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs,
      );
    });

    it('can create elapsed timestamp', () => {
      const timestamp = factory.makeElapsedTimestamp(testElapsedNs);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
      expect(timestamp.toUTC().getValueNs()).toBe(testElapsedNs);
    });

    it('can create real timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.REAL,
        testRealNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs + expectedUtcOffsetNs,
      );
      expect(timestamp.toUTC().getValueNs()).toBe(
        testRealNs + testRealToElapsedOffsetNs,
      );
    });

    it('can create elapsed timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        testElapsedNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
      expect(timestamp.toUTC().getValueNs()).toBe(testElapsedNs);
    });

    it('can create elapsed timestamp from type ignoring offset', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        testElapsedNs,
        testRealToElapsedOffsetNs,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(testElapsedNs);
      expect(timestamp.toUTC().getValueNs()).toBe(testElapsedNs);
    });

    it('throws error if creating real timestamp from type without offset', () => {
      expect(() =>
        factory.makeTimestampFromType(TimestampType.REAL, testRealNs),
      ).toThrow();
    });
  });

  describe('adds correct offset for different timezones', () => {
    it('creates correct real timestamps for different timezones', () => {
      expect(
        new TimestampFactory({timezone: 'Europe/London', locale: 'en-US'})
          .makeRealTimestamp(testRealNs)
          .getValueNs(),
      ).toEqual(testRealNs + BigInt(1 * 3.6e12));
      expect(
        new TimestampFactory({timezone: 'Europe/Zurich', locale: 'en-US'})
          .makeRealTimestamp(testRealNs)
          .getValueNs(),
      ).toEqual(testRealNs + BigInt(2 * 3.6e12));
      expect(
        new TimestampFactory({timezone: 'America/Los_Angeles', locale: 'en-US'})
          .makeRealTimestamp(testRealNs)
          .getValueNs(),
      ).toEqual(testRealNs - BigInt(7 * 3.6e12));
      expect(
        new TimestampFactory({timezone: 'Asia/Kolkata', locale: 'en-US'})
          .makeRealTimestamp(testRealNs)
          .getValueNs(),
      ).toEqual(testRealNs + BigInt(5.5 * 3.6e12));
    });

    it('throws error for invalid timezone', () => {
      expect(() =>
        new TimestampFactory({
          timezone: 'Invalid/Timezone',
          locale: 'en-US',
        }).makeRealTimestamp(testRealNs),
      ).toThrow();
    });
  });
});

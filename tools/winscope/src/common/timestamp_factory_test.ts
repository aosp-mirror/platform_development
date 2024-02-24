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
  describe('without timezone info', () => {
    const factory = new TimestampFactory();
    it('can create real timestamp', () => {
      const timestamp = factory.makeRealTimestamp(100n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(100n);
    });

    it('can create real timestamp with offset', () => {
      const timestamp = factory.makeRealTimestamp(100n, 500n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(600n);
    });

    it('can create elapsed timestamp', () => {
      const timestamp = factory.makeElapsedTimestamp(100n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
    });

    it('can create real timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.REAL,
        100n,
        500n,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(600n);
    });

    it('can create elapsed timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        100n,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
    });

    it('can create elapsed timestamp from type ignoring offset', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        100n,
        500n,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
    });

    it('throws error if creating real timestamp from type without offset', () => {
      expect(() =>
        factory.makeTimestampFromType(TimestampType.REAL, 100n),
      ).toThrow();
    });
  });

  describe('with timezone info', () => {
    const factory = new TimestampFactory({
      timezone: 'Asia/Kolkata',
      locale: 'en-US',
    });
    it('can create real timestamp', () => {
      const timestamp = factory.makeRealTimestamp(100n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(19800000000100n);
      expect(timestamp.toUTC().getValueNs()).toBe(100n);
    });

    it('can create real timestamp with offset', () => {
      const timestamp = factory.makeRealTimestamp(100n, 500n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(19800000000600n);
      expect(timestamp.toUTC().getValueNs()).toBe(600n);
    });

    it('can create elapsed timestamp', () => {
      const timestamp = factory.makeElapsedTimestamp(100n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
      expect(timestamp.toUTC().getValueNs()).toBe(100n);
    });

    it('can create real timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.REAL,
        100n,
        500n,
      );
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(19800000000600n);
      expect(timestamp.toUTC().getValueNs()).toBe(600n);
    });

    it('can create elapsed timestamp from type', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        100n,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
      expect(timestamp.toUTC().getValueNs()).toBe(100n);
    });

    it('can create elapsed timestamp from type ignoring offset', () => {
      const timestamp = factory.makeTimestampFromType(
        TimestampType.ELAPSED,
        100n,
        500n,
      );
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
      expect(timestamp.toUTC().getValueNs()).toBe(100n);
    });

    it('throws error if creating real timestamp from type without offset', () => {
      expect(() =>
        factory.makeTimestampFromType(TimestampType.REAL, 100n),
      ).toThrow();
    });
  });

  describe('adds correct offset for different timezones', () => {
    it('creates correct real timestamps for different timezones', () => {
      const realTimestampNs = 1706094750112797658n;
      expect(
        new TimestampFactory({timezone: 'Europe/London', locale: 'en-US'})
          .makeRealTimestamp(realTimestampNs)
          .getValueNs(),
      ).toEqual(1706094750112797658n);
      expect(
        new TimestampFactory({timezone: 'Europe/Zurich', locale: 'en-US'})
          .makeRealTimestamp(realTimestampNs)
          .getValueNs(),
      ).toEqual(1706098350112797658n);
      expect(
        new TimestampFactory({timezone: 'America/Los_Angeles', locale: 'en-US'})
          .makeRealTimestamp(realTimestampNs)
          .getValueNs(),
      ).toEqual(1706065950112797658n);
      expect(
        new TimestampFactory({timezone: 'Asia/Kolkata', locale: 'en-US'})
          .makeRealTimestamp(realTimestampNs)
          .getValueNs(),
      ).toEqual(1706114550112797658n);
    });

    it('throws error for invalid timezone', () => {
      expect(() =>
        new TimestampFactory({
          timezone: 'Invalid/Timezone',
          locale: 'en-US',
        }).makeRealTimestamp(10n),
      ).toThrow();
    });
  });
});

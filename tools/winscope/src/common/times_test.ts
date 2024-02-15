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

import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from './time';

describe('Timestamp', () => {
  describe('from', () => {
    it('throws when missing elapsed timestamp', () => {
      expect(() => {
        Timestamp.from(TimestampType.REAL, 100n);
      }).toThrow();
    });

    it('can create real timestamp', () => {
      const timestamp = Timestamp.from(TimestampType.REAL, 100n, 500n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(600n);
    });

    it('can create elapsed timestamp', () => {
      let timestamp = Timestamp.from(TimestampType.ELAPSED, 100n, 500n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);

      timestamp = Timestamp.from(TimestampType.ELAPSED, 100n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(100n);
    });
  });

  describe('arithmetic', () => {
    it('can add', () => {
      let timestamp = new RealTimestamp(10n).plus(new RealTimestamp(20n));
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(30n);

      timestamp = new ElapsedTimestamp(10n).plus(new ElapsedTimestamp(20n));
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(30n);
    });

    it('can subtract', () => {
      let timestamp = new RealTimestamp(20n).minus(new RealTimestamp(10n));
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(10n);

      timestamp = new ElapsedTimestamp(20n).minus(new ElapsedTimestamp(10n));
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(10n);
    });

    it('can divide', () => {
      let timestamp = new RealTimestamp(10n).div(2n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(5n);

      timestamp = new ElapsedTimestamp(10n).div(2n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(5n);
    });

    it('fails between different timestamp types', () => {
      const error = new Error('Attemping to do timestamp arithmetic on different timestamp types');
      expect(() => {
        new RealTimestamp(20n).minus(new ElapsedTimestamp(10n));
      }).toThrow(error);
      expect(() => {
        new RealTimestamp(20n).plus(new ElapsedTimestamp(10n));
      }).toThrow(error);
      expect(() => {
        new ElapsedTimestamp(20n).minus(new RealTimestamp(10n));
      }).toThrow(error);
      expect(() => {
        new ElapsedTimestamp(20n).plus(new RealTimestamp(10n));
      }).toThrow(error);
    });
  });
});

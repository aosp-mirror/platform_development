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
import {NO_TIMEZONE_OFFSET_FACTORY} from './timestamp_factory';

describe('Timestamp', () => {
  describe('arithmetic', () => {
    const REAL_TIMESTAMP_10 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n);
    const REAL_TIMESTAMP_20 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(20n);
    const ELAPSED_TIMESTAMP_10 =
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(10n);
    const ELAPSED_TIMESTAMP_20 =
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(20n);

    it('can add', () => {
      let timestamp = REAL_TIMESTAMP_10.plus(REAL_TIMESTAMP_20);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(30n);

      timestamp = ELAPSED_TIMESTAMP_10.plus(ELAPSED_TIMESTAMP_20);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(30n);
    });

    it('can subtract', () => {
      let timestamp = REAL_TIMESTAMP_20.minus(REAL_TIMESTAMP_10);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(10n);

      timestamp = ELAPSED_TIMESTAMP_20.minus(ELAPSED_TIMESTAMP_10);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(10n);
    });

    it('can divide', () => {
      let timestamp = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n).div(2n);
      expect(timestamp.getType()).toBe(TimestampType.REAL);
      expect(timestamp.getValueNs()).toBe(5n);

      timestamp = ELAPSED_TIMESTAMP_10.div(2n);
      expect(timestamp.getType()).toBe(TimestampType.ELAPSED);
      expect(timestamp.getValueNs()).toBe(5n);
    });

    it('fails between different timestamp types', () => {
      const error = new Error(
        'Attemping to do timestamp arithmetic on different timestamp types',
      );
      expect(() => {
        REAL_TIMESTAMP_20.minus(ELAPSED_TIMESTAMP_10);
      }).toThrow(error);
      expect(() => {
        REAL_TIMESTAMP_20.plus(ELAPSED_TIMESTAMP_10);
      }).toThrow(error);
      expect(() => {
        ELAPSED_TIMESTAMP_20.minus(REAL_TIMESTAMP_10);
      }).toThrow(error);
      expect(() => {
        ELAPSED_TIMESTAMP_20.plus(REAL_TIMESTAMP_10);
      }).toThrow(error);
    });
  });
});

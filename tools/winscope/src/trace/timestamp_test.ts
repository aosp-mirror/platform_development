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

import {Timestamp, TimestampType} from './timestamp';

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
});

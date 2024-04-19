/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {TimelineUtils} from './timeline_utils';

describe('TimelineUtils', () => {
  describe('rangeContainsTimestamp', () => {
    const range = {
      from: TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(
        10n,
      ),
      to: TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(600n),
    };

    it('returns true for range containing timestamp', () => {
      expect(
        TimelineUtils.rangeContainsTimestamp(
          range,
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(10n),
        ),
      ).toBeTrue();

      expect(
        TimelineUtils.rangeContainsTimestamp(
          range,
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(600n),
        ),
      ).toBeTrue();

      expect(
        TimelineUtils.rangeContainsTimestamp(
          range,
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(300n),
        ),
      ).toBeTrue();
    });

    it('returns false for range not containing timestamp', () => {
      expect(
        TimelineUtils.rangeContainsTimestamp(
          range,
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(0n),
        ),
      ).toBeFalse();

      expect(
        TimelineUtils.rangeContainsTimestamp(
          range,
          TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromNs(601n),
        ),
      ).toBeFalse();
    });
  });
});

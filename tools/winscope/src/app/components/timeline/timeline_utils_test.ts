/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeRange} from 'common/time/time';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TransitionStatus} from 'trace/transitions/status';
import {
  convertHexToRgb,
  getTimeRangeForTransition,
  isTransitionWithUnknownEnd,
  isTransitionWithUnknownStart,
} from './timeline_utils';

describe('TimelineUtils', () => {
  const zeroTs = TimestampConverterUtils.makeZeroTimestamp();

  describe('isTransitionWithUnknownStart', () => {
    it('returns true if dispatch time missing', () => {
      const transition = makeTransition({});
      expect(isTransitionWithUnknownStart(transition)).toBeTrue();
    });

    it('returns false if dispatch time present, even if zero', () => {
      const transition = makeTransition({dispatchTimeNs: zeroTs});
      expect(isTransitionWithUnknownStart(transition)).toBeFalse();
    });
  });

  describe('isTransitionWithUnknownEnd', () => {
    it('returns true if aborted and no shell time present', () => {
      const transition = makeTransition({status: 'ABORTED'});
      expect(isTransitionWithUnknownEnd(transition)).toBeTrue();
    });

    it('returns false if aborted and shell time present', () => {
      const transition = makeTransition({
        status: 'ABORTED',
        shellAbortTimeNs: zeroTs,
      });
      expect(isTransitionWithUnknownEnd(transition)).toBeFalse();
    });

    it('returns true if not aborted and finish time missing', () => {
      const transition = makeTransition({});
      expect(isTransitionWithUnknownEnd(transition)).toBeTrue();
    });

    it('returns false if not aborted and finish time present', () => {
      const transition = makeTransition({finishTimeNs: zeroTs});
      expect(isTransitionWithUnknownEnd(transition)).toBeFalse();
    });
  });

  describe('getTimeRangeForTransition', () => {
    const ts8 = TimestampConverterUtils.makeRealTimestamp(8n);
    const ts9 = TimestampConverterUtils.makeRealTimestamp(9n);
    const ts10 = TimestampConverterUtils.makeRealTimestamp(10n);
    const ts12 = TimestampConverterUtils.makeRealTimestamp(12n);
    const ts16 = TimestampConverterUtils.makeRealTimestamp(16n);
    const ts17 = TimestampConverterUtils.makeRealTimestamp(17n);
    const ts20 = TimestampConverterUtils.makeRealTimestamp(20n);
    const ts21 = TimestampConverterUtils.makeRealTimestamp(21n);
    const ts22 = TimestampConverterUtils.makeRealTimestamp(22n);
    const fullTimeRange = new TimeRange(ts10, ts20);
    const converter = TimestampConverterUtils.TIMESTAMP_CONVERTER;

    it('returns undefined if dispatch, finish and abort times missing', () => {
      const transition = makeTransition({});
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if dispatch time missing but create time present', () => {
      const transition = makeTransition({
        createTimeNs: ts12,
        finishTimeNs: ts17,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if finish time before full time range', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts8,
        finishTimeNs: ts9,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if abort time before full time range', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts8,
        status: TransitionStatus.ABORTED,
        shellAbortTimeNs: ts9,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if finish and abort times missing and dispatch time before full time range', () => {
      const transition = makeTransition({dispatchTimeNs: ts8});
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if dispatch time after full time range and finish time present', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts21,
        finishTimeNs: ts22,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns undefined if dispatch time after full time range and abort time present', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts21,
        status: TransitionStatus.ABORTED,
        shellAbortTimeNs: ts22,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toBeUndefined();
    });

    it('returns range from dispatch to finish time', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts12,
        finishTimeNs: ts17,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toEqual(new TimeRange(ts12, ts17));
    });

    it('returns range from dispatch to abort time', () => {
      const transition = makeTransition({
        dispatchTimeNs: ts12,
        status: TransitionStatus.ABORTED,
        shellAbortTimeNs: ts17,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toEqual(new TimeRange(ts12, ts17));
    });

    it('returns range from unknown start to finish time', () => {
      const transition = makeTransition({finishTimeNs: ts17});
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toEqual(new TimeRange(ts16, ts17));
    });

    it('returns range from unknown start to abort time', () => {
      const transition = makeTransition({
        status: TransitionStatus.ABORTED,
        shellAbortTimeNs: ts17,
      });
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toEqual(new TimeRange(ts16, ts17));
    });

    it('returns range from dispatch time to unknown end', () => {
      const transition = makeTransition({dispatchTimeNs: ts16});
      expect(
        getTimeRangeForTransition(transition, fullTimeRange, converter),
      ).toEqual(new TimeRange(ts16, ts17));
    });
  });

  describe('convertHexToRgb', () => {
    it('handles full regex', () => {
      expect(convertHexToRgb('0135AF')).toEqual({r: 1, g: 53, b: 175});
    });

    it('handles full regex with # prefix', () => {
      expect(convertHexToRgb('#0135AF')).toEqual({r: 1, g: 53, b: 175});
    });

    it('handles shorthand regex', () => {
      expect(convertHexToRgb('13F')).toEqual({r: 17, g: 51, b: 255});
    });

    it('handles shorthand regex with #', () => {
      expect(convertHexToRgb('#13F')).toEqual({r: 17, g: 51, b: 255});
    });

    it('robust to invalid hex string', () => {
      expect(convertHexToRgb('#1')).toEqual(undefined);
    });
  });

  function makeTransition(properties: object) {
    return new HierarchyTreeBuilder()
      .setId('')
      .setName('')
      .setProperties(properties)
      .build();
  }
});

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

import {TimeDuration} from './time_duration';
import {TIME_UNIT_TO_NANO} from './time_units';

describe('TimeDuration', () => {
  const MILLISECOND = BigInt(TIME_UNIT_TO_NANO.ms);
  const SECOND = BigInt(TIME_UNIT_TO_NANO.s);
  const MINUTE = BigInt(TIME_UNIT_TO_NANO.m);

  it('formats to nearest ms, using locale format for number', () => {
    const expected0 = makeExpectedString(0);
    expect(new TimeDuration(0n).format()).toEqual(expected0);
    expect(new TimeDuration(1000n).format()).toEqual(expected0);
    expect(new TimeDuration(10n * MILLISECOND).format()).toEqual(
      makeExpectedString(10),
    );

    const expected1000 = makeExpectedString(1000);
    expect(new TimeDuration(SECOND - 1n).format()).toEqual(expected1000);
    expect(new TimeDuration(SECOND).format()).toEqual(expected1000);
    expect(new TimeDuration(SECOND + MILLISECOND).format()).toEqual(
      makeExpectedString(1001),
    );

    const expected60000 = makeExpectedString(60000);
    expect(new TimeDuration(MINUTE - 1n).format()).toEqual(expected60000);
    expect(new TimeDuration(MINUTE).format()).toEqual(expected60000);

    const expected61001 = makeExpectedString(61001);
    expect(new TimeDuration(MINUTE + SECOND + MILLISECOND).format()).toEqual(
      expected61001,
    );
    expect(
      new TimeDuration(MINUTE + SECOND + MILLISECOND + 1n).format(),
    ).toEqual(expected61001);
  });

  function makeExpectedString(value: number) {
    return BigInt(value).toLocaleString() + ' ms';
  }
});

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

import {TIME_UNIT_TO_NANO} from './time_units';
import {UTCOffset} from './utc_offset';

describe('UTCOffset', () => {
  const utcOffset = new UTCOffset();

  it('sets positive offset for whole single-digit number hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * 2));
    expect(utcOffset.format()).toEqual('UTC+02:00');
  });

  it('sets positive offset for whole double-digit number hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * 11));
    expect(utcOffset.format()).toEqual('UTC+11:00');
  });

  it('sets positive offset for fractional hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * 5.5));
    expect(utcOffset.format()).toEqual('UTC+05:30');
  });

  it('sets negative offset for whole single-digit number hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * -8));
    expect(utcOffset.format()).toEqual('UTC-08:00');
  });

  it('sets negative offset for whole double-digit number hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * -10));
    expect(utcOffset.format()).toEqual('UTC-10:00');
  });

  it('sets negative offset for fractional hours', () => {
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * -4.5));
    expect(utcOffset.format()).toEqual('UTC-04:30');
  });

  it('does not set offset for invalid value', () => {
    const utcOffset = new UTCOffset();
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * 15)); // later than UTC+14:00
    expect(utcOffset.getValueNs()).toBeUndefined();
    utcOffset.initialize(BigInt(TIME_UNIT_TO_NANO.h * -13)); // earlier than UTC-12:00
    expect(utcOffset.getValueNs()).toBeUndefined();
  });
});

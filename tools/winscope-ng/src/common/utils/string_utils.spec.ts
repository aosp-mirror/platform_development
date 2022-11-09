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
import {TimeUtils} from "./time_utils";

describe("StringUtils", () => {
  it("nanosecondsToHuman", () => {
    const MILLISECOND = 1000000;
    const SECOND = 1000000000;
    const MINUTE = 60 * SECOND;
    const HOUR = 60 * MINUTE;
    const DAY = 24 * HOUR;

    expect(TimeUtils.nanosecondsToHuman(0)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(1000)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(MILLISECOND-1)).toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(MILLISECOND)).toEqual("1ms");
    expect(TimeUtils.nanosecondsToHuman(10 * MILLISECOND)).toEqual("10ms");

    expect(TimeUtils.nanosecondsToHuman(SECOND-1)).toEqual("999ms");
    expect(TimeUtils.nanosecondsToHuman(SECOND)).toEqual("1s0ms");
    expect(TimeUtils.nanosecondsToHuman(SECOND + MILLISECOND)).toEqual("1s1ms");

    expect(TimeUtils.nanosecondsToHuman(MINUTE-1)).toEqual("59s999ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE)).toEqual("1m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE + SECOND + MILLISECOND)).toEqual("1m1s1ms");

    expect(TimeUtils.nanosecondsToHuman(HOUR-1)).toEqual("59m59s999ms");
    expect(TimeUtils.nanosecondsToHuman(HOUR)).toEqual("1h0m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1h1m1s1ms");

    expect(TimeUtils.nanosecondsToHuman(DAY-1)).toEqual("23h59m59s999ms");
    expect(TimeUtils.nanosecondsToHuman(DAY)).toEqual("1d0h0m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(DAY + HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1d1h1m1s1ms");
  });
});

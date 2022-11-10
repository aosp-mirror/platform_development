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

describe("TimeUtils", () => {
  it("nanosecondsToHuman", () => {
    const MILLISECOND = 1000000;
    const SECOND = 1000 * MILLISECOND;
    const MINUTE = 60 * SECOND;
    const HOUR = 60 * MINUTE;
    const DAY = 24 * HOUR;

    expect(TimeUtils.nanosecondsToHuman(0)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(0, false)) .toEqual("0ns");
    expect(TimeUtils.nanosecondsToHuman(1000)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(1000, false)) .toEqual("1000ns");
    expect(TimeUtils.nanosecondsToHuman(MILLISECOND-1)).toEqual("0ms");
    expect(TimeUtils.nanosecondsToHuman(MILLISECOND)).toEqual("1ms");
    expect(TimeUtils.nanosecondsToHuman(10 * MILLISECOND)).toEqual("10ms");

    expect(TimeUtils.nanosecondsToHuman(SECOND-1)).toEqual("999ms");
    expect(TimeUtils.nanosecondsToHuman(SECOND)).toEqual("1s0ms");
    expect(TimeUtils.nanosecondsToHuman(SECOND + MILLISECOND)).toEqual("1s1ms");
    expect(TimeUtils.nanosecondsToHuman(SECOND + MILLISECOND, false)).toEqual("1s1ms0ns");

    expect(TimeUtils.nanosecondsToHuman(MINUTE-1)).toEqual("59s999ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE)).toEqual("1m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE + SECOND + MILLISECOND)).toEqual("1m1s1ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE + SECOND + MILLISECOND + 1)).toEqual("1m1s1ms");
    expect(TimeUtils.nanosecondsToHuman(MINUTE + SECOND + MILLISECOND + 1, false)).toEqual("1m1s1ms1ns");

    expect(TimeUtils.nanosecondsToHuman(HOUR-1)).toEqual("59m59s999ms");
    expect(TimeUtils.nanosecondsToHuman(HOUR-1, false)).toEqual("59m59s999ms999999ns");
    expect(TimeUtils.nanosecondsToHuman(HOUR)).toEqual("1h0m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1h1m1s1ms");

    expect(TimeUtils.nanosecondsToHuman(DAY-1)).toEqual("23h59m59s999ms");
    expect(TimeUtils.nanosecondsToHuman(DAY)).toEqual("1d0h0m0s0ms");
    expect(TimeUtils.nanosecondsToHuman(DAY + HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1d1h1m1s1ms");
  });

  it("humanToNanoseconds", () => {
    const MILLISECOND = BigInt(1000000);
    const SECOND = BigInt(1000) * MILLISECOND;
    const MINUTE = BigInt(60) * SECOND;
    const HOUR = BigInt(60) * MINUTE;
    const DAY = BigInt(24) * HOUR;

    expect(TimeUtils.humanToNanoseconds("0ns")) .toEqual(BigInt(0));
    expect(TimeUtils.humanToNanoseconds("1000ns")) .toEqual(BigInt(1000));
    expect(TimeUtils.humanToNanoseconds("0ms")).toEqual(BigInt(0));
    expect(TimeUtils.humanToNanoseconds("1ms")).toEqual(MILLISECOND);
    expect(TimeUtils.humanToNanoseconds("10ms")).toEqual(BigInt(10) * MILLISECOND);

    expect(TimeUtils.humanToNanoseconds("999ms")).toEqual(BigInt(999) * MILLISECOND);
    expect(TimeUtils.humanToNanoseconds("1s")).toEqual(SECOND);
    expect(TimeUtils.humanToNanoseconds("1s0ms")).toEqual(SECOND);
    expect(TimeUtils.humanToNanoseconds("1s0ms0ns")).toEqual(SECOND);
    expect(TimeUtils.humanToNanoseconds("1s0ms1ns")).toEqual(SECOND + BigInt(1));
    expect(TimeUtils.humanToNanoseconds("0d1s1ms")).toEqual(SECOND + MILLISECOND);

    expect(TimeUtils.humanToNanoseconds("1m0s0ms")).toEqual(MINUTE);
    expect(TimeUtils.humanToNanoseconds("1m1s1ms")).toEqual(MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanToNanoseconds("1h0m")).toEqual(HOUR);
    expect(TimeUtils.humanToNanoseconds("1h1m1s1ms")).toEqual(HOUR + MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanToNanoseconds("1d0s1ms")).toEqual(DAY + MILLISECOND);
    expect(TimeUtils.humanToNanoseconds("1d1h1m1s1ms")).toEqual(DAY + HOUR + MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanToNanoseconds("1d")).toEqual(DAY);
    expect(TimeUtils.humanToNanoseconds("1d1ms")).toEqual(DAY + MILLISECOND);
  });

  it("humanToNanoseconds throws on invalid input format", () => {
    const invalidFormatError = new Error("Invalid timestamp format");
    expect(() => TimeUtils.humanToNanoseconds("1d1h1m1s0ns1ms") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanToNanoseconds("1dns") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanToNanoseconds("100") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanToNanoseconds("") )
      .toThrow(invalidFormatError);
  });
});

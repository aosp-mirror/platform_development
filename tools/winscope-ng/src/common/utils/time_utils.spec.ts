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
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from "common/trace/timestamp";
import {TimeUtils} from "./time_utils";

describe("TimeUtils", () => {
  const MILLISECOND = BigInt(1000000);
  const SECOND = BigInt(1000) * MILLISECOND;
  const MINUTE = BigInt(60) * SECOND;
  const HOUR = BigInt(60) * MINUTE;
  const DAY = BigInt(24) * HOUR;

  describe("compareFn", () => {
    it("throws if timestamps have different type", () => {
      const real = new RealTimestamp(10n);
      const elapsed = new ElapsedTimestamp(10n);

      expect(() => {
        TimeUtils.compareFn(real, elapsed);
      }).toThrow();
    });

    it("allows to sort arrays", () => {
      const array = [
        new RealTimestamp(100n),
        new RealTimestamp(10n),
        new RealTimestamp(12n),
        new RealTimestamp(110n),
        new RealTimestamp(11n),
      ];
      array.sort(TimeUtils.compareFn);

      const expected = [
        new RealTimestamp(10n),
        new RealTimestamp(11n),
        new RealTimestamp(12n),
        new RealTimestamp(100n),
        new RealTimestamp(110n),
      ];
      expect(array).toEqual(expected);
    });
  });

  it("nanosecondsToHuman", () => {
    expect(TimeUtils.nanosecondsToHumanElapsed(0)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(0, false)) .toEqual("0ns");
    expect(TimeUtils.nanosecondsToHumanElapsed(1000)) .toEqual("0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(1000, false)) .toEqual("1000ns");
    expect(TimeUtils.nanosecondsToHumanElapsed(MILLISECOND-1n)).toEqual("0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(MILLISECOND)).toEqual("1ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(10n * MILLISECOND)).toEqual("10ms");

    expect(TimeUtils.nanosecondsToHumanElapsed(SECOND-1n)).toEqual("999ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(SECOND)).toEqual("1s0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(SECOND + MILLISECOND)).toEqual("1s1ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(SECOND + MILLISECOND, false)).toEqual("1s1ms0ns");

    expect(TimeUtils.nanosecondsToHumanElapsed(MINUTE-1n)).toEqual("59s999ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(MINUTE)).toEqual("1m0s0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(MINUTE + SECOND + MILLISECOND)).toEqual("1m1s1ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(MINUTE + SECOND + MILLISECOND + 1n)).toEqual("1m1s1ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(MINUTE + SECOND + MILLISECOND + 1n, false)).toEqual("1m1s1ms1ns");

    expect(TimeUtils.nanosecondsToHumanElapsed(HOUR-1n)).toEqual("59m59s999ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(HOUR-1n, false)).toEqual("59m59s999ms999999ns");
    expect(TimeUtils.nanosecondsToHumanElapsed(HOUR)).toEqual("1h0m0s0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1h1m1s1ms");

    expect(TimeUtils.nanosecondsToHumanElapsed(DAY-1n)).toEqual("23h59m59s999ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(DAY)).toEqual("1d0h0m0s0ms");
    expect(TimeUtils.nanosecondsToHumanElapsed(DAY + HOUR + MINUTE + SECOND + MILLISECOND)).toEqual("1d1h1m1s1ms");
  });

  it("humanElapsedToNanoseconds", () => {
    expect(TimeUtils.humanElapsedToNanoseconds("0ns")) .toEqual(BigInt(0));
    expect(TimeUtils.humanElapsedToNanoseconds("1000ns")) .toEqual(BigInt(1000));
    expect(TimeUtils.humanElapsedToNanoseconds("0ms")).toEqual(BigInt(0));
    expect(TimeUtils.humanElapsedToNanoseconds("1ms")).toEqual(MILLISECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("10ms")).toEqual(BigInt(10) * MILLISECOND);

    expect(TimeUtils.humanElapsedToNanoseconds("999ms")).toEqual(BigInt(999) * MILLISECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("1s")).toEqual(SECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("1s0ms")).toEqual(SECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("1s0ms0ns")).toEqual(SECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("1s0ms1ns")).toEqual(SECOND + BigInt(1));
    expect(TimeUtils.humanElapsedToNanoseconds("0d1s1ms")).toEqual(SECOND + MILLISECOND);

    expect(TimeUtils.humanElapsedToNanoseconds("1m0s0ms")).toEqual(MINUTE);
    expect(TimeUtils.humanElapsedToNanoseconds("1m1s1ms")).toEqual(MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanElapsedToNanoseconds("1h0m")).toEqual(HOUR);
    expect(TimeUtils.humanElapsedToNanoseconds("1h1m1s1ms")).toEqual(HOUR + MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanElapsedToNanoseconds("1d0s1ms")).toEqual(DAY + MILLISECOND);
    expect(TimeUtils.humanElapsedToNanoseconds("1d1h1m1s1ms")).toEqual(DAY + HOUR + MINUTE + SECOND + MILLISECOND);

    expect(TimeUtils.humanElapsedToNanoseconds("1d")).toEqual(DAY);
    expect(TimeUtils.humanElapsedToNanoseconds("1d1ms")).toEqual(DAY + MILLISECOND);
  });

  it("humanToNanoseconds throws on invalid input format", () => {
    const invalidFormatError = new Error("Invalid elapsed timestamp format");
    expect(() => TimeUtils.humanElapsedToNanoseconds("1d1h1m1s0ns1ms") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanElapsedToNanoseconds("1dns") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanElapsedToNanoseconds("100") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanElapsedToNanoseconds("") )
      .toThrow(invalidFormatError);
  });

  it("nanosecondsToHumanReal", () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(TimeUtils.nanosecondsToHumanReal(0))
      .toEqual("00h00m00s000ms0ns, 1 Jan 1970 UTC");
    expect(TimeUtils.nanosecondsToHumanReal(
      NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186n * MILLISECOND + 123212n))
      .toEqual("22h04m54s186ms123212ns, 10 Nov 2022 UTC");
    expect(TimeUtils.nanosecondsToHumanReal(NOV_10_2022))
      .toEqual("00h00m00s000ms0ns, 10 Nov 2022 UTC");
    expect(TimeUtils.nanosecondsToHumanReal(NOV_10_2022 + 1n))
      .toEqual("00h00m00s000ms1ns, 10 Nov 2022 UTC");
  });

  it("humanRealToNanoseconds", () => {
    const NOV_10_2022 = 1668038400000n * MILLISECOND;
    expect(TimeUtils.humanRealToNanoseconds("22h04m54s186ms123212ns, 10 Nov 2022 UTC"))
      .toEqual(NOV_10_2022 + 22n * HOUR + 4n * MINUTE + 54n * SECOND + 186n * MILLISECOND + 123212n);
    expect(TimeUtils.humanRealToNanoseconds("22h04m54s186ms123212ns, 10 Nov 2022")).toEqual(1668117894186123212n);
    expect(TimeUtils.humanRealToNanoseconds("22h04m54s186ms212ns, 10 Nov 2022 UTC")).toEqual(1668117894186000212n);
    expect(TimeUtils.humanRealToNanoseconds("22h04m54s6ms2ns, 10 Nov 2022")).toEqual(1668117894006000002n);
    expect(TimeUtils.humanRealToNanoseconds("06h4m54s6ms2ns, 10 Nov 2022")).toEqual(1668060294006000002n);
  });

  it("humanToNanoseconds throws on invalid input format", () => {
    const invalidFormatError = new Error("Invalid real timestamp format");
    expect(() => TimeUtils.humanRealToNanoseconds("23h59m59s999ms5ns") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanRealToNanoseconds("1d") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanRealToNanoseconds("100") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanRealToNanoseconds("06h4m54s, 10 Nov 2022") )
      .toThrow(invalidFormatError);
    expect(() => TimeUtils.humanRealToNanoseconds("") )
      .toThrow(invalidFormatError);
  });

  it("nano second regex accept all expected inputs", () => {
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("123")).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("123ns")).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("123 ns")).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test(" 123 ns ")).toBeTrue();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("   123  ")).toBeTrue();

    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("1a23")).toBeFalse();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("a123 ns")).toBeFalse();
    expect(TimeUtils.NS_TIMESTAMP_REGEX.test("")).toBeFalse();
  });

  it("format real", () => {
    expect(TimeUtils.format(Timestamp.from(TimestampType.REAL, 100n, 500n))).toEqual("00h00m00s000ms600ns, 1 Jan 1970 UTC");
    expect(TimeUtils.format(Timestamp.from(TimestampType.REAL, 100n * MILLISECOND, 500n), true)).toEqual("00h00m00s100ms, 1 Jan 1970 UTC");
  });

  it("format elapsed", () => {
    expect(TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND, 500n), true)).toEqual("100ms");
    expect(TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND), true)).toEqual("100ms");
    expect(TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND, 500n))).toEqual("100ms0ns");
    expect(TimeUtils.format(Timestamp.from(TimestampType.ELAPSED, 100n * MILLISECOND))).toEqual("100ms0ns");
  });
});

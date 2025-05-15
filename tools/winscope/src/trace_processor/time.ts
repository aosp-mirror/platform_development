// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

type Brand<T, V extends string> = T & {__type: V};

// The |time| type represents trace time in the same units and domain as trace
// processor (i.e. typically boot time in nanoseconds, but most of the UI should
// be completely agnostic to this).
export type time = Brand<bigint, 'time'>;

// The |duration| type is used to represent the duration of time between two
// |time|s. The domain is irrelevant because a duration is relative.
export type duration = bigint;

export class Time {
  static readonly ZERO = Time.fromRaw(0n);

  // Cast a bigint to a |time|. Supports potentially |undefined| values.
  // I.e. it performs the following conversions:
  // - `bigint` -> `time`
  // - `bigint|undefined` -> `time|undefined`
  //
  // Use this function with caution. The function is effectively a no-op in JS,
  // but using it tells TypeScript that "this value is a time value". It's up to
  // the caller to ensure the value is in the correct units and time domain.
  //
  // If you're reaching for this function after doing some maths on a |time|
  // value and it's decayed to a |bigint| consider using the static math methods
  // in |Time| instead, as they will do the appropriate casting for you.
  static fromRaw(v: bigint): time;
  static fromRaw(v?: bigint): time | undefined;
  static fromRaw(v?: bigint): time | undefined {
    return v as time | undefined;
  }
}

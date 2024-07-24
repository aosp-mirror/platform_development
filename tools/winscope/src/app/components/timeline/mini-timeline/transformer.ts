/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Segment} from 'app/components/timeline/segment';
import {TimeRange, Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';

export class Transformer {
  private timestampType: TimestampType;

  private fromWidth: bigint;
  private targetWidth: number;

  private fromOffset: bigint;
  private toOffset: number;

  constructor(private fromRange: TimeRange, private toRange: Segment) {
    this.timestampType = fromRange.from.getType();

    this.fromWidth =
      this.fromRange.to.getValueNs() - this.fromRange.from.getValueNs();
    // Needs to be a whole number to be compatible with bigints
    this.targetWidth = Math.round(this.toRange.to - this.toRange.from);

    this.fromOffset = this.fromRange.from.getValueNs();
    // Needs to be a whole number to be compatible with bigints
    this.toOffset = this.toRange.from;
  }

  transform(x: Timestamp): number {
    return (
      this.toOffset +
      (this.targetWidth * Number(x.getValueNs() - this.fromOffset)) /
        Number(this.fromWidth)
    );
  }

  untransform(x: number): Timestamp {
    x = Math.round(x);
    const valueNs =
      this.fromOffset +
      (BigInt(x - this.toOffset) * this.fromWidth) / BigInt(this.targetWidth);
    return NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(
      this.timestampType,
      valueNs,
      0n,
    );
  }
}

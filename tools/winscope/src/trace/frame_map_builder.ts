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

import {FrameMap} from './frame_map';
import {
  AbsoluteEntryIndex,
  AbsoluteFrameIndex,
  FramesRange,
} from './index_types';

export class FrameMapBuilder {
  private readonly lengthEntries: number;
  private readonly lengthFrames: number;

  // See comments in FrameMap about the semantics of these lookup tables
  private readonly entryToStartFrame: Array<AbsoluteFrameIndex | undefined>;
  private readonly entryToEndFrame: Array<AbsoluteFrameIndex | undefined>;
  private readonly frameToStartEntry: Array<AbsoluteEntryIndex | undefined>;
  private readonly frameToEndEntry: Array<AbsoluteEntryIndex | undefined>;

  private isFinalized = false;

  constructor(lengthEntries: number, lengthFrames: number) {
    this.lengthEntries = lengthEntries;
    this.lengthFrames = lengthFrames;

    this.entryToStartFrame = new Array<AbsoluteFrameIndex | undefined>(
      this.lengthEntries,
    ).fill(undefined);
    this.entryToEndFrame = new Array<AbsoluteFrameIndex | undefined>(
      this.lengthEntries,
    ).fill(undefined);
    this.frameToStartEntry = new Array<AbsoluteEntryIndex | undefined>(
      this.lengthFrames,
    ).fill(undefined);
    this.frameToEndEntry = new Array<AbsoluteEntryIndex | undefined>(
      this.lengthFrames,
    ).fill(undefined);
  }

  setFrames(
    entry: AbsoluteEntryIndex,
    range: FramesRange | undefined,
  ): FrameMapBuilder {
    this.checkIsNotFinalized();
    if (!range || range.start === range.end) {
      return this;
    }

    this.setStartArrayValue(this.entryToStartFrame, entry, range.start);
    this.setEndArrayValue(this.entryToEndFrame, entry, range.end);

    for (let frame = range.start; frame < range.end; ++frame) {
      this.setStartArrayValue(this.frameToStartEntry, frame, entry);
      this.setEndArrayValue(this.frameToEndEntry, frame, entry + 1);
    }

    return this;
  }

  build(): FrameMap {
    this.checkIsNotFinalized();
    this.finalizeStartArray(this.entryToStartFrame);
    this.finalizeEndArray(this.entryToEndFrame);
    this.finalizeStartArray(this.frameToStartEntry);
    this.finalizeEndArray(this.frameToEndEntry);
    this.isFinalized = true;
    return new FrameMap(
      this.lengthEntries,
      this.lengthFrames,
      this.entryToStartFrame,
      this.entryToEndFrame,
      this.frameToStartEntry,
      this.frameToEndEntry,
    );
  }

  private setStartArrayValue(
    array: Array<number | undefined>,
    index: number,
    value: number,
  ) {
    const currentValue = array[index];
    if (currentValue === undefined) {
      array[index] = value;
    } else {
      array[index] = Math.min(currentValue, value);
    }
  }

  private setEndArrayValue(
    array: Array<number | undefined>,
    index: number,
    value: number,
  ) {
    const currentValue = array[index];
    if (currentValue === undefined) {
      array[index] = value;
    } else {
      array[index] = Math.max(currentValue, value);
    }
  }

  private finalizeStartArray(array: Array<number | undefined>) {
    let firstValidStart: number | undefined = undefined;
    for (let i = array.length - 1; i >= 0; --i) {
      if (array[i] === undefined) {
        array[i] = firstValidStart;
      } else {
        firstValidStart = array[i];
      }
    }
  }

  private finalizeEndArray(array: Array<number | undefined>) {
    let lastValidEnd: number | undefined = undefined;
    for (let i = 0; i < array.length; ++i) {
      if (array[i] === undefined) {
        array[i] = lastValidEnd;
      } else {
        lastValidEnd = array[i];
      }
    }
  }

  private checkIsNotFinalized() {
    if (this.isFinalized) {
      throw new Error('Attemped to modify already finalized frame map.');
    }
  }
}

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

import {
  AbsoluteEntryIndex,
  AbsoluteFrameIndex,
  EntriesRange,
  FramesRange,
} from './index_types';

export class FrameMap {
  readonly lengthEntries: number;
  readonly lengthFrames: number;

  // These lookup tables allow to convert a "[start_entry; end_entry[" range
  // to "[start_frame; end_frame[" in O(1) time.
  //
  // entryToStartFrame[i] is:
  // - start_frame of entry_i
  // - start_frame of the first entry_j (j > i), if entry_i has no associated frames
  // - undefined, if all the entries with index >= i have no associated frames
  //
  // entryToEndFrame[i] is:
  // - end_frame of entry_i
  // - end_frame of the last entry_j (j < i), if entry_i has no associated frames
  // - undefined, if all the entries with index <= i have no associated frames
  private readonly entryToStartFrame: Array<AbsoluteFrameIndex | undefined>;
  private readonly entryToEndFrame: Array<AbsoluteFrameIndex | undefined>;

  // These lookup tables allow to convert a "[start_frame; end_frame[" range
  // to "[start_entry; end_entry[" in O(1) time.
  //
  // frameToStartEntry[i] is:
  // - start_entry of frame_i
  // - start_entry of the first frame_j (j > i), if frame_i has no associated entries
  // - undefined, if all the frames with index >= i have no associated entries
  //
  // frameToEndEntry[i] is:
  // - end_entry of frame_i
  // - end_entry of the last frame_j (j < i), if frame_i has no associated entries
  // - undefined, if all the frames with index <= i have no associated entries
  private readonly frameToStartEntry: Array<AbsoluteEntryIndex | undefined>;
  private readonly frameToEndEntry: Array<AbsoluteEntryIndex | undefined>;

  constructor(
    lengthEntries: number,
    lengthFrames: number,
    entryToStartFrame: Array<AbsoluteFrameIndex | undefined>,
    entryToEndFrame: Array<AbsoluteFrameIndex | undefined>,
    frameToStartEntry: Array<AbsoluteEntryIndex | undefined>,
    frameToEndEntry: Array<AbsoluteEntryIndex | undefined>,
  ) {
    this.lengthEntries = lengthEntries;
    this.lengthFrames = lengthFrames;
    this.entryToStartFrame = entryToStartFrame;
    this.entryToEndFrame = entryToEndFrame;
    this.frameToStartEntry = frameToStartEntry;
    this.frameToEndEntry = frameToEndEntry;
  }

  getFramesRange(entries: EntriesRange): FramesRange | undefined {
    entries = this.clampEntriesRangeToFitBounds(entries);
    if (entries.start >= entries.end) {
      return undefined;
    }

    const startFrame = this.getStartFrameOfFirstGreaterOrEqualMappedEntry(
      entries.start,
    );
    const endFrame = this.getEndFrameOfLastLowerOrEqualMappedEntry(
      entries.end - 1,
    );

    if (
      startFrame === undefined ||
      endFrame === undefined ||
      startFrame >= endFrame
    ) {
      return undefined;
    }

    return {start: startFrame, end: endFrame};
  }

  getFullTraceFramesRange(): FramesRange | undefined {
    return this.getFramesRange({start: 0, end: this.lengthEntries});
  }

  getEntriesRange(frames: FramesRange): EntriesRange | undefined {
    frames = this.clampFramesRangeToFitBounds(frames);
    if (frames.start >= frames.end) {
      return undefined;
    }

    const startEntry = this.getStartEntryOfFirstGreaterOrEqualMappedFrame(
      frames.start,
    );
    const endEntry = this.getEndEntryOfLastLowerOrEqualMappedFrame(
      frames.end - 1,
    );

    if (
      startEntry === undefined ||
      endEntry === undefined ||
      startEntry >= endEntry
    ) {
      return undefined;
    }

    return {start: startEntry, end: endEntry};
  }

  private getStartFrameOfFirstGreaterOrEqualMappedEntry(
    entry: AbsoluteEntryIndex,
  ): AbsoluteFrameIndex | undefined {
    if (entry < 0 || entry >= this.lengthEntries) {
      throw Error(`Entry index out of bounds: ${entry}`);
    }
    return this.entryToStartFrame[entry];
  }

  private getEndFrameOfLastLowerOrEqualMappedEntry(
    entry: AbsoluteEntryIndex,
  ): AbsoluteFrameIndex | undefined {
    if (entry < 0 || entry >= this.lengthEntries) {
      throw Error(`Entry index out of bounds: ${entry}`);
    }
    return this.entryToEndFrame[entry];
  }

  private getStartEntryOfFirstGreaterOrEqualMappedFrame(
    frame: AbsoluteFrameIndex,
  ): AbsoluteEntryIndex | undefined {
    if (frame < 0 || frame >= this.lengthFrames) {
      throw Error(`Frame index out of bounds: ${frame}`);
    }
    return this.frameToStartEntry[frame];
  }

  private getEndEntryOfLastLowerOrEqualMappedFrame(
    frame: AbsoluteFrameIndex,
  ): AbsoluteEntryIndex | undefined {
    if (frame < 0 || frame >= this.lengthFrames) {
      throw Error(`Frame index out of bounds: ${frame}`);
    }
    return this.frameToEndEntry[frame];
  }

  private clampEntriesRangeToFitBounds(entries: EntriesRange): EntriesRange {
    return {
      start: Math.max(entries.start, 0),
      end: Math.min(entries.end, this.lengthEntries),
    };
  }

  private clampFramesRangeToFitBounds(frames: FramesRange): FramesRange {
    return {
      start: Math.max(frames.start, 0),
      end: Math.min(frames.end, this.lengthFrames),
    };
  }
}

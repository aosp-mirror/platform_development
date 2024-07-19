/*
 * Copyright 2024 Google LLC
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

import { Timeline } from './timeline';

/** Translates pixel values to frames / video timestamps, and vice-versa. */
export class VisualTimeline extends EventTarget {
  constructor(
    width: number,
    public readonly timeline: Timeline,
  ) {
    super();
    this._width = width;
  }

  private _width: number;

  get width(): number {
    return this._width;
  }

  /**
   * Pixel-width of the interaction area.
   *
   * `0`px ≙ `0`s, while `width`px ≙ the duration.
   */
  set width(value: number) {
    if (value == this._width) return;
    this._width = value;
    this.dispatchEvent(new Event('timeline-changed'));
  }

  /** Pixel-value that marks the given time. */
  timeToPx(timeSeconds: number): number {
    if (timeSeconds < 0) return Number.NEGATIVE_INFINITY;
    const duration = this.timeline.duration;
    if (timeSeconds > duration) return Number.POSITIVE_INFINITY;

    return this.width * (timeSeconds / duration);
  }

  /** Pixel-value that marks the given time. */
  timeToPxClamped(timeSeconds: number): number {
    if (timeSeconds < 0) return Number.NEGATIVE_INFINITY;
    const duration = this.timeline.duration;
    if (timeSeconds > duration) return Number.POSITIVE_INFINITY;

    return (
      this.width * (this.timeline.clampTimeToFrame(timeSeconds) / duration)
    );
  }

  /** Pixel-value that marks the beginning of the given frame. */
  frameToPx(frame: number): number {
    return this.timeToPx(this.timeline.frameToTime(frame));
  }

  /** Video-time associated with the given pixel value. */
  pxToTime(px: number): number {
    if (px < 0) return Number.NEGATIVE_INFINITY;
    const width = this.width;
    if (px > width) return Number.POSITIVE_INFINITY;

    return this.timeline.duration * (px / width);
  }

  /** Frame number associated with the given pixel value. */
  pxToFrame(px: number): number {
    return this.timeline.timeToFrame(this.pxToTime(px));
  }
}

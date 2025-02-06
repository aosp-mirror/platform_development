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

// golden recordings from motion tests produces frames at exactly 16ms each
const frameDuration = 0.016;

/** Maps frame time to frame number and vice versa. */
export class Timeline {
  /** Duration , represented as a double-precision floating-point value in seconds. */
  readonly duration: number;
  /**
   * The labels of all
   */
  readonly frameLabels: ReadonlyArray<string>;
  readonly frameTimes: ReadonlyArray<number>;

  constructor(frameIds: ReadonlyArray<string | number>) {
    this.frameLabels = frameIds.map((it) =>
      typeof it === 'string' ? it : `${it}ms`
    );

    let lastFrameTime = 0;

    const frameTimes = [lastFrameTime];

    for (let i = 0; i < frameIds.length - 1; i++) {
      const thisFrame = frameIds[i];
      const nextFrame = frameIds[i + 1];

      if (typeof thisFrame === 'number' && typeof nextFrame === 'number') {
        lastFrameTime += (nextFrame - thisFrame) / 1000;
      } else {
        lastFrameTime += frameDuration;
      }
      frameTimes.push(lastFrameTime);
    }
    this.frameTimes = frameTimes;

    this.duration = frameTimes[frameTimes.length - 1] + frameDuration;
  }

  /** Number of frames in this timeline. */
  get frameCount(): number {
    return this.frameLabels.length;
  }

  /**
   * Compute the frame number associated the time in seconds.
   *
   * Each frame starts at the frame time, and just before the next frame's start time, or duration
   * if it's the last frame.
   */
  timeToFrame(timeSeconds: number): number {
    if (timeSeconds < 0) return Number.NEGATIVE_INFINITY;
    if (timeSeconds >= this.duration) return Number.POSITIVE_INFINITY;

    const frameTimes = this.frameTimes;

    let start = 0;
    let end = frameTimes.length - 1;

    while (start <= end) {
      let mid = (start + end) >> 1;

      if (frameTimes[mid] === timeSeconds) {
        return mid;
      }

      if (timeSeconds < frameTimes[mid]) {
        end = mid - 1;
      } else {
        start = mid + 1;
      }
    }

    return end;
  }

  /**
   * Start time of the frame with `frameNumber`.
   */
  frameToTime(frameNumber: number): number {
    frameNumber = Math.floor(frameNumber);
    if (frameNumber < 0) return Number.NEGATIVE_INFINITY;
    const frameTimes = this.frameTimes;
    if (frameNumber >= frameTimes.length) return Number.POSITIVE_INFINITY;

    return frameTimes[frameNumber];
  }

  clampTimeToFrame(timeSeconds: number): number {
    return this.frameToTime(this.timeToFrame(timeSeconds));
  }
}

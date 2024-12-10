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

declare module 'mp4box' {
  // mp4box does not have TypeScript support, so we must declare the types below
  export interface FileInfo {
    tracks: Track[];
  }

  export interface Track {
    id: number;
  }

  export interface Sample {
    duration: number;
    timescale: number;
  }

  export type MP4ArrayBuffer = ArrayBuffer & {fileStart: number};

  export interface MP4File {
    onReady?: (info: FileInfo) => void;
    onSamples?: (id: number, user: unknown, samples: Sample[]) => void;
    appendBuffer(data: MP4ArrayBuffer): number;
    start(): void;
    setExtractionOptions(
      trackId: number,
      user?: unknown,
      options?: {nbSamples?: number; rapAlignment?: number},
    ): void;
  }

  export function createFile(): MP4File;
}

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

// This class is needed for testing because Node.js doesn't provide the Web API's Blob type
class Blob {
  constructor(buffer: ArrayBuffer) {
    this.size = buffer.byteLength;
    this.type = "application/octet-stream";
    this.buffer = buffer;
  }

  arrayBuffer(): Promise<ArrayBuffer> {
    return new Promise<ArrayBuffer>((resolve, reject) => {
      resolve(this.buffer);
    });
  }

  slice(start?: number, end?: number, contentType?: string): Blob {
    throw new Error("Not implemented!");
  }

  stream(): any {
    throw new Error("Not implemented!");
  }

  text(): Promise<string> {
    throw new Error("Not implemented!");
  }

  readonly size: number;
  readonly type: string;
  private readonly buffer: ArrayBuffer;
}

export {Blob};

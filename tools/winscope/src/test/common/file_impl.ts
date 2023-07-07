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

// This class is needed for unit tests because Node.js doesn't provide
// an implementation of the Web API's File type
class FileImpl {
  readonly size: number;
  readonly type: string;
  readonly name: string;
  readonly lastModified: number = 0;
  readonly webkitRelativePath: string = '';
  private readonly buffer: ArrayBuffer;

  constructor(buffer: ArrayBuffer, fileName: string) {
    this.buffer = buffer;
    this.size = this.buffer.byteLength;
    this.type = 'application/octet-stream';
    this.name = fileName;
  }

  arrayBuffer(): Promise<ArrayBuffer> {
    return new Promise<ArrayBuffer>((resolve) => {
      resolve(this.buffer);
    });
  }

  slice(start?: number, end?: number, contentType?: string): Blob {
    throw new Error('Not implemented!');
  }

  stream(): any {
    throw new Error('Not implemented!');
  }

  text(): Promise<string> {
    const utf8Decoder = new TextDecoder();
    const text = utf8Decoder.decode(this.buffer);
    return new Promise<string>((resolve) => {
      resolve(text);
    });
  }
}

export {FileImpl};

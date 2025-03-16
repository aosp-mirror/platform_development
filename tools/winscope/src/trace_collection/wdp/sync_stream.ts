/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  ArrayBufferBuilder,
  BufferToken,
  byteArrayToString,
  ResizableBuffer,
} from 'common/buffer_utils';
import {AdbWebSocketStream} from './adb_websocket_stream';
import {ErrorListener} from './websocket_stream';

export class SyncStream extends AdbWebSocketStream {
  private static readonly DATA_ID = 'DATA';
  private static readonly DONE_ID = 'DONE';

  private cmdOut = new ResizableBuffer();
  private lastChunkOffset = 0;

  constructor(
    sock: WebSocket,
    deviceSerialNumber: string,
    errorListener: ErrorListener,
  ) {
    super(sock, deviceSerialNumber, 'sync', errorListener);
  }

  async pullFile(filepath: string): Promise<Uint8Array> {
    return await new Promise<Uint8Array>((resolve) => {
      this.onClose = () => {
        resolve(this.cmdOut.get());
      };
      this.write(this.makeTokens(['RECV', filepath.length, filepath]));
    });
  }

  private makeTokens(tokens: BufferToken[]): Uint8Array {
    const buffer = new ArrayBufferBuilder().append(tokens).build();
    return new Uint8Array(buffer);
  }

  protected override onData = (data: Uint8Array) => {
    // add data from last chunk
    const offset = Math.min(data.length, this.lastChunkOffset);
    this.cmdOut.append(data.slice(0, offset));
    data = data.slice(offset);
    this.lastChunkOffset = Math.max(0, this.lastChunkOffset - offset);
    if (data.length === 0) {
      return;
    }
    if (data.length < 8) {
      console.error('Remaining data too small', data);
      this.close();
      return;
    }

    // check start id of next chunk
    const startId = byteArrayToString(data.slice(0, 4));
    const chunkLength = this.getChunkLength(data.slice(4, 8));
    if (data.length === 8 && startId === SyncStream.DONE_ID) {
      this.close();
      return;
    }
    if (startId !== SyncStream.DATA_ID) {
      console.error("expected 'DATA' id, received", startId);
      this.close();
      return;
    }
    if (data.length === 8) {
      this.lastChunkOffset = chunkLength;
      return;
    }
    data = data.slice(8);

    // check end id of remaining data
    const endId = byteArrayToString(
      data.slice(data.length - 8, data.length - 4),
    );
    if (this.containsMultipleChunks(endId, chunkLength, data.length)) {
      this.lastChunkOffset = 0;
      this.cmdOut.append(data.slice(0, chunkLength));
      this.onData(data.slice(chunkLength));
      return;
    }

    // add remaining data
    this.lastChunkOffset = chunkLength - data.length;
    if (endId === SyncStream.DONE_ID) {
      data = data.slice(0, data.length - 8);
    }
    this.cmdOut.append(data);
    if (endId === SyncStream.DONE_ID) {
      this.close();
    }
  };

  private getChunkLength(data: Uint8Array) {
    const dataView = new DataView(
      data.buffer,
      data.byteOffset,
      data.byteLength,
    );
    return dataView.getUint32(0, true);
  }

  private containsMultipleChunks(
    endId: string,
    chunkLength: number,
    dataLength: number,
  ) {
    return (
      (endId !== SyncStream.DONE_ID && chunkLength < dataLength) ||
      (endId === SyncStream.DONE_ID && chunkLength < dataLength - 8)
    );
  }
}

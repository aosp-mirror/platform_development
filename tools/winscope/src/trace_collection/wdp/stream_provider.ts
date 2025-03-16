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

import {AdbWebSocketStream, DataListener} from './adb_websocket_stream';
import {DevicesMsgListener, DevicesStream} from './devices_stream';
import {ShellStream} from './shell_stream';
import {SyncStream} from './sync_stream';
import {ErrorListener} from './websocket_stream';

export class StreamProvider {
  private streams: AdbWebSocketStream[] = [];
  private devicesStream: DevicesStream | undefined;

  createSyncStream(
    deviceSerialNumber: string,
    sock: WebSocket,
    errorListener: ErrorListener,
  ): SyncStream {
    const stream = new SyncStream(sock, deviceSerialNumber, errorListener);
    this.streams.push(stream);
    return stream;
  }

  createShellStream(
    deviceSerialNumber: string,
    sock: WebSocket,
    dataListener: DataListener,
    errorListener: ErrorListener,
  ): ShellStream {
    const stream = new ShellStream(
      sock,
      deviceSerialNumber,
      dataListener,
      errorListener,
    );
    this.streams.push(stream);
    return stream;
  }

  removeStream(stream: AdbWebSocketStream) {
    this.streams = this.streams.filter((s) => s !== stream);
  }

  createDevicesStream(
    sock: WebSocket,
    msgListener: DevicesMsgListener,
    errorListener: () => void,
  ): DevicesStream {
    if (this.devicesStream) {
      this.devicesStream.close();
      this.devicesStream = undefined;
    }
    this.devicesStream = new DevicesStream(sock, msgListener, errorListener);
    return this.devicesStream;
  }

  closeAllStreams() {
    this.streams.forEach((stream) => stream.close());
    this.devicesStream?.close();
  }
}

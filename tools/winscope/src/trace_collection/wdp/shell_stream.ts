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
import {ErrorListener} from './websocket_stream';

export class ShellStream extends AdbWebSocketStream {
  private completeResolve:
    | ((value: void | PromiseLike<void>) => void)
    | undefined;
  readonly complete = new Promise<void>((resolve) => {
    this.completeResolve = resolve;
  });

  constructor(
    sock: WebSocket,
    deviceSerialNumber: string,
    stdoutListener: DataListener,
    errorListener: ErrorListener,
  ) {
    super(sock, deviceSerialNumber, 'shell', errorListener);
    this.onData = stdoutListener;
    this.onClose = () => {
      if (this.completeResolve) {
        this.completeResolve();
      }
    };
  }
}

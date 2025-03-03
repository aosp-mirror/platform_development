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

import {WebSocketStream} from './websocket_stream';

export type DevicesMsgListener = (data: string) => Promise<void>;

export class DevicesStream extends WebSocketStream {
  constructor(
    sock: WebSocket,
    private dataListener: DevicesMsgListener,
    errorListener: () => void,
  ) {
    super(sock);
    sock.onerror = errorListener;
  }

  override async connect() {
    let messagePromiseQueue = Promise.resolve();
    this.sock.onmessage = (e: MessageEvent<string>) => {
      messagePromiseQueue = messagePromiseQueue.then(async () => {
        await this.dataListener(e.data);
      });
    };
  }
}

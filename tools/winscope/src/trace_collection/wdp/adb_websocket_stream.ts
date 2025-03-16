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

import {FunctionUtils} from 'common/function_utils';
import {ErrorListener, WebSocketStream} from './websocket_stream';

interface AdbResponse {
  error?: {
    type: string;
    message: string;
  };
}

export type DataListener = (data: Uint8Array) => void;

export abstract class AdbWebSocketStream extends WebSocketStream {
  protected onData: DataListener = FunctionUtils.DO_NOTHING;

  constructor(
    sock: WebSocket,
    private deviceSerialNumber: string,
    private service: string,
    errorListener: ErrorListener,
  ) {
    super(sock);
    this.onError = async (msg: string) => {
      await errorListener(msg);
      this.close();
    };
    sock.onmessage = async (e: MessageEvent) => {
      try {
        if (e.data instanceof ArrayBuffer) {
          this.onData(new Uint8Array(e.data));
        } else if (e.data instanceof Blob) {
          this.onData(new Uint8Array(await e.data.arrayBuffer()));
        } else {
          throw new Error('Expected message data to be ArrayBuffer or Blob');
        }
      } catch (error) {
        console.debug('WebSocket failed, state: ' + sock.readyState);
        let adbError: string | undefined;
        if (typeof e.data === 'string') {
          try {
            const data: AdbResponse = JSON.parse(e.data);
            if (data.error) {
              adbError = data.error.message;
            }
          } catch (e) {
            // do nothing
          }
        }
        this.onError(
          `Could not parse data:\nReceived: ${e.data}` +
            `\nError: ${(error as Error).message}.` +
            (adbError ? `\nADB Error: ` + adbError : ''),
        );
      }
    };
  }

  override async connect(args = '') {
    await this.write(
      JSON.stringify({
        header: {
          serialNumber: this.deviceSerialNumber,
          command: this.service + ':' + args,
        },
      }),
    );
  }
}

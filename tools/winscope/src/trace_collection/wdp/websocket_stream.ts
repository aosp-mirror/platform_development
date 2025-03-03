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
import {TimeUtils} from 'common/time/time_utils';

export type ErrorListener = (msg: string) => Promise<void>;

export abstract class WebSocketStream {
  constructor(protected sock: WebSocket) {
    sock.binaryType = 'arraybuffer';
    sock.onclose = () => this.onClose();
  }

  abstract connect(): Promise<void>;

  protected onError: ErrorListener = FunctionUtils.DO_NOTHING_ASYNC;
  protected onClose: () => void = FunctionUtils.DO_NOTHING;

  async write(data: string | Uint8Array): Promise<void> {
    await TimeUtils.wait(() => this.isOpen());
    this.sock.send(data);
  }

  close(): void {
    this.sock.close();
  }

  isOpen(): boolean {
    return this.sock.readyState === WebSocket.OPEN;
  }

  isClosed(): boolean {
    return this.sock.readyState === WebSocket.CLOSED;
  }
}

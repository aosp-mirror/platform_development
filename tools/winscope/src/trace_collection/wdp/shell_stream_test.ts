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

import {TimeUtils} from 'common/time/time_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {ShellStream} from './shell_stream';

describe('ShellStream', () => {
  const serialNumber = '123';
  const dataListener = jasmine.createSpy();
  const errorListener = jasmine.createSpy();
  let stream: ShellStream;
  let webSocket: jasmine.SpyObj<WebSocket>;

  beforeEach(() => {
    webSocket = UnitTestUtils.makeFakeWebSocket();
    errorListener.calls.reset();
    stream = new ShellStream(
      webSocket,
      serialNumber,
      dataListener,
      errorListener,
    );
  });

  afterEach(() => {
    expect(errorListener).not.toHaveBeenCalled();
  });

  it('connects to open shell service', async () => {
    await stream.connect();
    expect(webSocket.send).toHaveBeenCalledOnceWith(
      JSON.stringify({
        header: {
          serialNumber,
          command: 'shell:',
        },
      }),
    );
  });

  it('connects to closed shell service', async () => {
    await stream.connect('test command');
    expect(webSocket.send).toHaveBeenCalledOnceWith(
      JSON.stringify({
        header: {
          serialNumber,
          command: 'shell:test command',
        },
      }),
    );
  });

  it('calls data listener on array buffer message', async () => {
    const data = new ArrayBuffer(0);
    const message = UnitTestUtils.makeFakeWebSocketMessage(data);
    webSocket.onmessage!(message);
    expect(dataListener).toHaveBeenCalledWith(new Uint8Array(data));
  });

  it('calls data listener on blob message', async () => {
    const data = new Blob();
    const message = UnitTestUtils.makeFakeWebSocketMessage(data);
    webSocket.onmessage!(message);
    expect(dataListener).toHaveBeenCalledWith(
      new Uint8Array(await data.arrayBuffer()),
    );
  });

  it('resolves complete promise on close', async () => {
    let completed = false;
    stream.complete.then(() => {
      completed = true;
    });
    await stream.connect();
    expect(completed).toBeFalse();
    webSocket.onclose!(new CloseEvent(''));
    await TimeUtils.wait(() => completed);
  });

  it('calls error listener if unexpected message type received - AdbResponse json', async () => {
    const data = JSON.stringify({error: {type: '', message: 'failed'}});
    const message = UnitTestUtils.makeFakeWebSocketMessage(data);
    webSocket.onmessage!(message);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data:\nReceived: {"error":{"type":"","message":"failed"}}` +
        `\nError: Expected message data to be ArrayBuffer or Blob.` +
        `\nADB Error: failed`,
    );
    errorListener.calls.reset();
  });

  it('calls error listener if unexpected message type received - unknown string', async () => {
    const message = UnitTestUtils.makeFakeWebSocketMessage('unknown error');
    webSocket.onmessage!(message);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data:\nReceived: unknown error` +
        `\nError: Expected message data to be ArrayBuffer or Blob.`,
    );
    errorListener.calls.reset();
  });

  it('calls error listener if unexpected message type received - unknown code', async () => {
    const message = UnitTestUtils.makeFakeWebSocketMessage(200);
    webSocket.onmessage!(message);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data:\nReceived: 200` +
        `\nError: Expected message data to be ArrayBuffer or Blob.`,
    );
    errorListener.calls.reset();
  });
});

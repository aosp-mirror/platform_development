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
import {DevicesStream} from './devices_stream';

describe('DevicesStream', () => {
  const dataListener = jasmine.createSpy();
  const errorListener = jasmine.createSpy();
  const testMessage = 'test';
  let stream: DevicesStream;
  let webSocket: jasmine.SpyObj<WebSocket>;

  beforeEach(() => {
    webSocket = UnitTestUtils.makeFakeWebSocket();
    errorListener.calls.reset();
    stream = new DevicesStream(webSocket, dataListener, errorListener);
  });

  afterEach(() => {
    expect(errorListener).not.toHaveBeenCalled();
  });

  it('connects by setting data listener to onmessage', async () => {
    let called = false;
    dataListener.and.callFake(() => {
      called = true;
    });
    receiveMessage();
    expect(called).toBeFalse();
    await stream.connect();
    receiveMessage();
    await TimeUtils.wait(() => called);
    expect(dataListener).toHaveBeenCalledOnceWith(testMessage);
  });

  it('calls error listener on socket error', async () => {
    webSocket.onerror!(new Event('error'));
    expect(errorListener).toHaveBeenCalledTimes(1);
    errorListener.calls.reset();
  });

  function receiveMessage() {
    webSocket.onmessage!(UnitTestUtils.makeFakeWebSocketMessage('test'));
  }
});

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

import {stringToByteArray} from 'common/buffer_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {waitToBeCalled} from 'test/utils';
import {StreamProvider} from './stream_provider';

describe('StreamProvider', () => {
  const serialNumber = 'testSerialNumber';
  let dataListener: jasmine.Spy;
  let errorListener: jasmine.Spy;
  let sock: jasmine.SpyObj<WebSocket>;
  let streamProvider: StreamProvider;

  beforeEach(() => {
    sock = UnitTestUtils.makeFakeWebSocket();
    dataListener = jasmine.createSpy();
    errorListener = jasmine.createSpy();
    streamProvider = new StreamProvider();
  });

  it('creates, stores and closes sync stream', async () => {
    const stream = streamProvider.createSyncStream(
      serialNumber,
      sock,
      errorListener,
    );
    await stream.connect();
    expect(sock.send).toHaveBeenCalledWith(
      JSON.stringify({
        header: {serialNumber, command: 'sync:'},
      }),
    );

    sock.onmessage!(UnitTestUtils.makeFakeWebSocketMessage(''));
    expect(errorListener).toHaveBeenCalledTimes(1);

    const spy = spyOn(stream, 'close');
    streamProvider.closeAllStreams();
    expect(spy).toHaveBeenCalledTimes(1);

    streamProvider.removeStream(stream);
    streamProvider.closeAllStreams();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('creates, stores and closes shell stream', async () => {
    const stream = streamProvider.createShellStream(
      serialNumber,
      sock,
      dataListener,
      errorListener,
    );
    await stream.connect();
    expect(sock.send).toHaveBeenCalledWith(
      JSON.stringify({
        header: {serialNumber, command: 'shell:'},
      }),
    );

    sock.onmessage!(
      UnitTestUtils.makeFakeWebSocketMessage(stringToByteArray('').buffer),
    );
    expect(dataListener).toHaveBeenCalledTimes(1);

    sock.onmessage!(UnitTestUtils.makeFakeWebSocketMessage(''));
    expect(errorListener).toHaveBeenCalledTimes(1);

    const spy = spyOn(stream, 'close');
    streamProvider.closeAllStreams();
    expect(spy).toHaveBeenCalledTimes(1);

    streamProvider.removeStream(stream);
    streamProvider.closeAllStreams();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('creates, stores and closes devices stream', async () => {
    const stream = createDevicesStream();
    await stream.connect();

    sock.onmessage!(UnitTestUtils.makeFakeWebSocketMessage(''));
    await waitToBeCalled(dataListener, 1);

    sock.onerror!(new Event(''));
    expect(errorListener).toHaveBeenCalledTimes(1);

    const spy = spyOn(stream, 'close');
    streamProvider.closeAllStreams();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('closes existing devices stream before creating new one', async () => {
    const stream = createDevicesStream();
    const spy = spyOn(stream, 'close');
    const newStream = createDevicesStream();
    expect(spy).toHaveBeenCalled();
    expect(stream).not.toEqual(newStream);
  });

  function createDevicesStream() {
    return streamProvider.createDevicesStream(
      sock,
      dataListener,
      errorListener,
    );
  }
});

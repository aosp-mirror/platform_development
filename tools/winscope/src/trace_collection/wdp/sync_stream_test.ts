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
  byteArrayToString,
  stringToByteArray,
} from 'common/buffer_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {AdbDevice} from 'trace_collection/adb_device';
import {SyncStream} from './sync_stream';

describe('SyncStream', () => {
  const mockDevice: AdbDevice = {
    id: '123',
    authorized: true,
    model: '',
    displays: [],
    multiDisplayScreenRecordingAvailable: false,
  };
  const errorListener = jasmine.createSpy();
  const testFileDataString = 'test file data';
  const testFileData = stringToByteArray(testFileDataString);
  const testFilepath = 'test_filepath';
  const expectedSendBuffer = new Uint8Array(
    new ArrayBufferBuilder()
      .append(['RECV', testFilepath.length, testFilepath])
      .build(),
  );
  const emptyByte = Uint8Array.from([0, 0, 0, 0]);
  let stream: SyncStream;
  let webSocket: jasmine.SpyObj<WebSocket>;

  beforeEach(async () => {
    webSocket = UnitTestUtils.makeFakeWebSocket();
    errorListener.calls.reset();
    stream = new SyncStream(webSocket, mockDevice, errorListener);
    await stream.connect();
  });

  afterEach(() => {
    expect(errorListener).not.toHaveBeenCalled();
  });

  it('connects to sync service', async () => {
    expect(webSocket.send).toHaveBeenCalledOnceWith(
      JSON.stringify({
        header: {
          serialNumber: mockDevice.id,
          command: 'sync:',
        },
      }),
    );
  });

  it('calls error listener if unexpected message type received - AdbResponse json', async () => {
    setMessageResponses([
      JSON.stringify({error: {type: '', message: 'failed'}}),
    ]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data: \nReceived: {"error":{"type":"","message":"failed"}}` +
        `\nError: Expected message data to be ArrayBuffer or Blob.` +
        `\nADB Error: failed`,
    );
    expect(receivedData).toEqual(Uint8Array.from([]));
    errorListener.calls.reset();
  });

  it('calls error listener if unexpected message type received - unknown string', async () => {
    setMessageResponses(['unknown error']);
    const receivedData = await stream.pullFile(testFilepath);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data: \nReceived: unknown error` +
        `\nError: Expected message data to be ArrayBuffer or Blob.`,
    );
    expect(receivedData).toEqual(Uint8Array.from([]));
    errorListener.calls.reset();
  });

  it('calls error listener if unexpected message type received - unknown code', async () => {
    setMessageResponses([200]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(errorListener).toHaveBeenCalledOnceWith(
      `Could not parse data: \nReceived: 200` +
        `\nError: Expected message data to be ArrayBuffer or Blob.`,
    );
    expect(receivedData).toEqual(Uint8Array.from([]));
    errorListener.calls.reset();
  });

  it('pulls file data from one chunk in one message', async () => {
    const messageData = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, testFileData, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data from one chunk across two messages', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3);
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, fileData1])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append([fileData2, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data from one chunk across three messages', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3, 5);
    const fileData3 = testFileData.slice(5);
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, fileData1])
      .build();
    const messageData2 = new ArrayBufferBuilder().append([fileData2]).build();
    const messageData3 = new ArrayBufferBuilder()
      .append([fileData3, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2, messageData3]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data from multiple chunks in one message', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3);
    const messageData = new ArrayBufferBuilder()
      .append(['DATA', fileData1.length, fileData1])
      .append(['DATA', fileData2.length, fileData2, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data from multiple chunks, one chunk per message', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3);
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', fileData1.length, fileData1])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append(['DATA', fileData2.length, fileData2, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data from multiple chunks across multiple messages', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3, 5);
    const fileData3 = testFileData.slice(5);
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', fileData1.length + fileData2.length, fileData1])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append([fileData2])
      .append(['DATA', fileData3.length, fileData3, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data where DATA id is in separate message', async () => {
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append([testFileData, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data where DONE id is in separate message', async () => {
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, testFileData])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append(['DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('pulls file data where DATA and DONE ids in separate messages', async () => {
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length])
      .build();
    const messageData2 = new ArrayBufferBuilder()
      .append([testFileData])
      .build();
    const messageData3 = new ArrayBufferBuilder()
      .append(['DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2, messageData3]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('robust to file data where length is too small', async () => {
    const messageData = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, testFileData, 'DONE'])
      .build();

    webSocket.send.withArgs(expectedSendBuffer).and.callFake(() => {
      const message = jasmine.createSpyObj<MessageEvent<ArrayBuffer>>([], {
        'data': messageData,
      });
      webSocket.onmessage!(message);
    });
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  it('robust to unexpected id at start of chunk', async () => {
    const fileData1 = testFileData.slice(0, 3);
    const fileData2 = testFileData.slice(3);
    const messageData1 = new ArrayBufferBuilder()
      .append(['DATA', fileData1.length, fileData1])
      .build();

    const messageData2 = new ArrayBufferBuilder()
      .append(['NEXT', fileData2.length, fileData2, 'DONE', emptyByte])
      .build();
    setMessageResponses([messageData1, messageData2]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual('tes');
  });

  it('pulls file data from blob', async () => {
    const messageData = new ArrayBufferBuilder()
      .append(['DATA', testFileData.length, testFileData, 'DONE', emptyByte])
      .build();
    setMessageResponses([new Blob([messageData])]);
    const receivedData = await stream.pullFile(testFilepath);
    expect(byteArrayToString(receivedData)).toEqual(testFileDataString);
  });

  function setMessageResponses(
    messageData: Array<Blob | ArrayBuffer | number | string>,
  ) {
    webSocket.send.withArgs(expectedSendBuffer).and.callFake(() => {
      messageData.forEach((data) => {
        const message = UnitTestUtils.makeFakeWebSocketMessage(data);
        webSocket.onmessage!(message);
      });
    });
    errorListener.and.callFake(() => {
      webSocket.close();
    });
  }
});

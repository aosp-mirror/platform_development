/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {WindowUtils} from 'common/window_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {DevicesStream} from './devices_stream';
import {StreamProvider} from './stream_provider';
import {WdpDeviceConnection} from './wdp_device_connection';
import {
  WdpDeviceConnectionResponse,
  WdpHostConnection,
  WdpRequestDevicesResponse,
} from './wdp_host_connection';

describe('WdpHostConnection', () => {
  const listener = jasmine.createSpyObj<ConnectionStateListener>(
    'ConnectionStateListener',
    [
      'onAvailableTracesChange',
      'onError',
      'onConnectionStateChange',
      'onDevicesChange',
    ],
  );
  const testApproveUrl = 'test_approve_url';
  let connection: WdpHostConnection;
  let devicesStreamSpy: jasmine.Spy;
  let popupSpy: jasmine.Spy;

  beforeAll(() => {
    devicesStreamSpy = spyOn(StreamProvider.prototype, 'createDevicesStream');
  });

  beforeEach(() => {
    popupSpy = spyOn(WindowUtils, 'showPopupWindow');
    connection = new WdpHostConnection(listener);
    resetListener();
  });

  afterEach(() => {
    expect(listener.onAvailableTracesChange).not.toHaveBeenCalled();
    expect(listener.onError).not.toHaveBeenCalled();
    expect(listener.onConnectionStateChange).not.toHaveBeenCalled();
    expect(listener.onDevicesChange).not.toHaveBeenCalled();
  });

  describe('initialization and destruction:', () => {
    it('closes all streams onDestroy', async () => {
      const spy = spyOn(StreamProvider.prototype, 'closeAllStreams');
      connection.onDestroy();
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  describe('unsuccessful request:', () => {
    let fakeSocket: WebSocket;

    beforeEach(() => {
      fakeSocket = UnitTestUtils.makeFakeWebSocket();
    });

    it('not found', async () => {
      devicesStreamSpy.and.callFake((_, dataListener, errorListener) => {
        return new DevicesStream(fakeSocket, dataListener, errorListener);
      });
      await connection.requestDevices();
      fakeSocket.onerror!(new Event('error'));
      expect(popupSpy).not.toHaveBeenCalled();
      expect(listener.onConnectionStateChange).toHaveBeenCalledOnceWith(
        ConnectionState.NOT_FOUND,
      );
      listener.onConnectionStateChange.calls.reset();
    });

    it('unauthorized server - pop ups enabled', async () => {
      await requestDevicesFromUnauthServer();
      expect(popupSpy).toHaveBeenCalledOnceWith(testApproveUrl);
      expect(listener.onConnectionStateChange).toHaveBeenCalledWith(
        ConnectionState.UNAUTH,
      );
      listener.onConnectionStateChange.calls.reset();
    });

    it('unauthorized server - pop ups disabled', async () => {
      popupSpy.and.returnValue(false);
      await requestDevicesFromUnauthServer();
      expect(listener.onError).toHaveBeenCalledWith(
        'Please enable popups and try again.',
      );
      listener.onError.calls.reset();
    });

    it('unauthorized server - force shows pop up multiple times', async () => {
      devicesStreamSpy.calls.reset();
      await requestDevicesFromUnauthServer();
      await requestDevicesFromUnauthServer();
      expect(popupSpy.calls.allArgs()).toEqual([
        [testApproveUrl],
        [testApproveUrl],
      ]);
      listener.onConnectionStateChange.calls.reset();
    });

    it('error - no approve URL, message present', async () => {
      await requestDevices({error: {message: 'test message'}}, fakeSocket);
      expect(listener.onError).toHaveBeenCalledWith('test message');
      expect(popupSpy).not.toHaveBeenCalled();
      listener.onError.calls.reset();
    });

    it('error - no approve URL or message', async () => {
      await requestDevices({error: {}}, fakeSocket);
      expect(listener.onError).toHaveBeenCalledWith('Unknown WDP Error');
      expect(popupSpy).not.toHaveBeenCalled();
      listener.onError.calls.reset();
    });

    it('set security token does not throw', () => {
      expect(() => connection.setSecurityToken('')).not.toThrow();
    });

    async function requestDevicesFromUnauthServer() {
      await requestDevices(
        {
          error: {
            type: 'ORIGIN_NOT_ALLOWLISTED',
            approveUrl: testApproveUrl,
          },
        },
        fakeSocket,
      );
    }
  });

  describe('device requests:', () => {
    let fakeDevicesSocket: jasmine.SpyObj<WebSocket>;
    const testApproveDeviceUrl = 'test_approve_device_url';
    const mockDevJson: WdpDeviceConnectionResponse = {
      serialNumber: '35562',
      proxyStatus: 'ADB',
      adbStatus: 'DEVICE',
      adbProps: {
        model: 'Pixel 6',
      },
      approveUrl: testApproveDeviceUrl,
    };

    beforeEach(() => {
      fakeDevicesSocket = UnitTestUtils.makeFakeWebSocket();
      spyOn(WdpDeviceConnection.prototype, 'updateProperties');
    });

    it('handles empty response', async () => {
      await requestDevices({}, fakeDevicesSocket);
      checkDevices([]);
    });

    it('handles empty devices in response', async () => {
      await requestDevices({device: []}, fakeDevicesSocket);
      checkDevices([]);
    });

    it('adds new device', async () => {
      await requestDevices({device: [mockDevJson]}, fakeDevicesSocket);
      checkDevices([
        new WdpDeviceConnection(
          mockDevJson.serialNumber,
          listener,
          testApproveDeviceUrl,
        ),
      ]);
    });

    it('removes previous devices no longer present', async () => {
      await requestDevices({device: [mockDevJson]}, fakeDevicesSocket);
      await requestDevices({device: []}, fakeDevicesSocket);
      checkDevices([]);
    });

    function checkDevices(expectedDevices: WdpDeviceConnection[], popups = 0) {
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.IDLE,
      ]);
      expect(listener.onDevicesChange.calls.mostRecent().args).toEqual([
        expectedDevices,
      ]);
      expect(connection.getDevices()).toEqual(expectedDevices);
      expect(popupSpy).toHaveBeenCalledTimes(popups);
      listener.onConnectionStateChange.calls.reset();
      listener.onDevicesChange.calls.reset();
    }
  });

  async function requestDevices(
    response: WdpRequestDevicesResponse,
    fakeSocket: WebSocket,
  ) {
    await new Promise<void>((resolve) => {
      devicesStreamSpy.and.callFake((_, dListener, eListener) => {
        const newDataListener = async (data: string) => {
          await dListener(data);
          resolve();
        };
        return new DevicesStream(fakeSocket, newDataListener, eListener);
      });
      const data = JSON.stringify(response);
      connection.requestDevices().then(() => {
        const message = UnitTestUtils.makeFakeWebSocketMessage(data);
        fakeSocket.onmessage!(message);
      });
    });
  }

  function resetListener() {
    listener.onAvailableTracesChange.calls.reset();
    listener.onError.calls.reset();
    listener.onConnectionStateChange.calls.reset();
    listener.onDevicesChange.calls.reset();
  }
});

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

import {
  HttpRequest,
  HttpRequestHeaderType,
  HttpRequestStatus,
  HttpResponse,
} from 'common/http_request';
import {waitToBeCalled} from 'test/utils';
import {
  AdbDeviceConnection,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {Endpoint} from './endpoint';
import {VERSION, WINSCOPE_PROXY_URL} from './utils';
import {WinscopeProxyHostConnection} from './winscope_proxy_host_connection';

type HttpRequestGetType = (
  path: string,
  headers: HttpRequestHeaderType,
  type?: XMLHttpRequest['responseType'],
) => Promise<HttpResponse>;

type HttpRequestPostType = (
  path: string,
  headers: HttpRequestHeaderType,
  jsonRequest?: object,
) => Promise<HttpResponse>;

describe('WinscopeProxyHostConnection', () => {
  const listener = jasmine.createSpyObj<ConnectionStateListener>(
    'ConnectionStateListener',
    [
      'onAvailableTracesChange',
      'onError',
      'onConnectionStateChange',
      'onDevicesChange',
    ],
  );
  const getVersionHeader = () => VERSION;

  let connection: WinscopeProxyHostConnection;
  let getSpy: jasmine.Spy<HttpRequestGetType>;
  let postSpy: jasmine.Spy<HttpRequestPostType>;

  beforeEach(() => {
    connection = new WinscopeProxyHostConnection(listener);
    resetListener();
  });

  afterEach(() => {
    expect(listener.onAvailableTracesChange).not.toHaveBeenCalled();
    expect(listener.onError).not.toHaveBeenCalled();
    expect(listener.onConnectionStateChange).not.toHaveBeenCalled();
    expect(listener.onDevicesChange).not.toHaveBeenCalled();
    connection.onDestroy();
  });

  describe('initialization:', () => {
    beforeAll(() => {
      localStorage.clear();
    });

    beforeEach(async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(successfulResponse);
    });

    afterEach(() => {
      localStorage.clear();
      listener.onError.calls.reset();
    });

    it('uses stored token on initialization', async () => {
      connection.setSecurityToken('test_initial_token');
      connection = new WinscopeProxyHostConnection(listener);
      await connection.requestDevices();
      checkDevicesRequested('test_initial_token');
    });

    it('sets security token and sends as header', async () => {
      resetSpies();
      connection.setSecurityToken('test_token');
      await connection.requestDevices();
      checkDevicesRequested('test_token');
    });

    it('does not set empty token', async () => {
      connection.setSecurityToken('test_initial_token');
      connection = new WinscopeProxyHostConnection(listener);
      resetSpies();
      connection.setSecurityToken('');
      await connection.requestDevices();
      checkDevicesRequested('test_initial_token');
    });

    function resetSpies() {
      getSpy.calls.reset();
      postSpy.calls.reset();
    }
  });

  describe('unsuccessful request:', () => {
    it('unauthorized server', async () => {
      const unauthResponse: HttpResponse = {
        status: HttpRequestStatus.UNAUTH,
        type: '',
        text: '',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(unauthResponse);
      await connection.requestDevices();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.UNAUTH,
      ]);
      listener.onConnectionStateChange.calls.reset();
    });

    it('invalid version - header undefined', async () => {
      await checkInvalidVersion(() => undefined);
    });

    it('invalid version - old major', async () => {
      await checkInvalidVersion(() => '0.0.0');
    });

    it('invalid version - old minor', async () => {
      const [major, minor, patch] = VERSION.split('.');
      await checkInvalidVersion(() =>
        [major, Number(minor) - 1, patch].join('.'),
      );
    });

    it('invalid version - old patch', async () => {
      const [major, minor, patch] = VERSION.split('.');
      await checkInvalidVersion(() =>
        [major, minor, Number(patch) - 1].join('.'),
      );
    });

    it('error state with response type text', async () => {
      const errorResponse: HttpResponse = {
        status: HttpRequestStatus.ERROR,
        type: 'text',
        text: 'test error message',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(errorResponse);
      await connection.requestDevices();
      expect(listener.onError.calls.mostRecent().args).toEqual([
        errorResponse.text,
      ]);
      listener.onError.calls.reset();
    });

    it('error state with response type empty', async () => {
      const errorResponse: HttpResponse = {
        status: HttpRequestStatus.ERROR,
        type: '',
        text: 'test error message',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(errorResponse);
      await connection.requestDevices();
      expect(listener.onError.calls.mostRecent().args).toEqual([
        errorResponse.text,
      ]);
      listener.onError.calls.reset();
    });

    it('error state with response type array buffer', async () => {
      const errorResponse: HttpResponse = {
        status: HttpRequestStatus.ERROR,
        type: 'arraybuffer',
        text: '',
        body: [],
        getHeader: getVersionHeader,
      };
      setHttpSpies(errorResponse);
      await connection.requestDevices();
      expect(listener.onError.calls.mostRecent().args).toEqual([
        'No data received.',
      ]);
      listener.onError.calls.reset();
    });

    async function checkInvalidVersion(getHeader: () => string | undefined) {
      const invalidResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '',
        body: undefined,
        getHeader,
      };
      setHttpSpies(invalidResponse);
      await connection.requestDevices();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.INVALID_VERSION,
      ]);
      listener.onConnectionStateChange.calls.reset();
    }
  });

  describe('device requests:', () => {
    const successfulDevicesResponse: HttpResponse = {
      status: HttpRequestStatus.SUCCESS,
      type: 'text',
      text: JSON.stringify([
        {
          id: '35562',
          authorized: true,
          model: 'Pixel 6',
        },
      ]),
      body: undefined,
      getHeader: getVersionHeader,
    };

    it('requests devices from proxy', async () => {
      const unsentResponse: HttpResponse = {
        status: HttpRequestStatus.UNSENT,
        type: '',
        text: '',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(unsentResponse);
      await connection.requestDevices();
      checkDevicesRequested();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.NOT_FOUND,
      ]);
      listener.onConnectionStateChange.calls.reset();
    });

    it('sets error state if onSuccess callback fails', async () => {
      const noDevicesResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'arraybuffer',
        text: '[0,]',
        body: undefined,
        getHeader: getVersionHeader,
      };
      setHttpSpies(noDevicesResponse);
      await connection.requestDevices();
      checkDevicesRequested();
      expect(listener.onError.calls.mostRecent().args).toEqual([
        'Could not find devices. Received:\n[0,]',
      ]);
      listener.onError.calls.reset();
    });

    it('fetches devices', async () => {
      setHttpSpies(successfulDevicesResponse);
      await connection.requestDevices();
      checkDevicesRequested();
      checkDevices();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.IDLE,
      ]);
      listener.onConnectionStateChange.calls.reset();
      listener.onDevicesChange.calls.reset();
    });

    it('sets up worker to fetch devices', async () => {
      setHttpSpies(successfulDevicesResponse);
      await connection.requestDevices();
      checkDevicesRequested();
      checkDevices();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.IDLE,
      ]);
      listener.onConnectionStateChange.calls.reset();
      listener.onDevicesChange.calls.reset();
      getSpy.calls.reset();

      await waitToBeCalled(listener.onConnectionStateChange, 1);
      await waitToBeCalled(getSpy, 1);
      checkDevicesRequested();
      checkDevices();
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.IDLE,
      ]);
      listener.onConnectionStateChange.calls.reset();
      listener.onDevicesChange.calls.reset();
      connection.onDestroy();
    });

    function checkDevices() {
      const devices = connection.getDevices();
      checkDeviceProperties(devices);
      expect(listener.onDevicesChange).toHaveBeenCalledTimes(1);
      checkDeviceProperties(listener.onDevicesChange.calls.allArgs()[0][0]);
    }

    function checkDeviceProperties(devices: AdbDeviceConnection[]) {
      expect(devices.length).toEqual(1);
      expect(devices[0].getFormattedName()).toEqual('Pixel 6 (35562)');
      expect(devices[0].getState()).toEqual(AdbDeviceState.AVAILABLE);
    }
  });

  function setHttpSpies(getResponse: HttpResponse, postResponse = getResponse) {
    getSpy = spyOn(HttpRequest, 'get').and.returnValue(
      Promise.resolve(getResponse),
    );
    postSpy = spyOn(HttpRequest, 'post').and.returnValue(
      Promise.resolve(postResponse),
    );
  }

  function resetListener() {
    listener.onAvailableTracesChange.calls.reset();
    listener.onError.calls.reset();
    listener.onConnectionStateChange.calls.reset();
    listener.onDevicesChange.calls.reset();
  }

  function checkDevicesRequested(header = '') {
    expect(getSpy).toHaveBeenCalledWith(
      WINSCOPE_PROXY_URL + Endpoint.DEVICES,
      [['Winscope-Token', header]],
      undefined,
    );
  }
});

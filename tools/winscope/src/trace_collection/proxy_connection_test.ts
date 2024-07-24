/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {AdbDevice} from 'trace_collection/adb_device';
import {ConnectionState} from 'trace_collection/connection_state';
import {ProxyConnection} from 'trace_collection/proxy_connection';
import {ProxyEndpoint} from './proxy_endpoint';
import {TraceRequest} from './trace_request';

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

describe('ProxyConnection', () => {
  const detectStateChangesInUi = jasmine.createSpy();
  const progressCallback = jasmine.createSpy();
  const availableTracesChangeCallback = jasmine.createSpy();
  const mockDevice: AdbDevice = {
    id: '35562',
    model: 'Pixel 6',
    authorized: true,
  };
  const mockTraceRequest: TraceRequest = {
    name: 'layers_trace',
    config: [],
  };
  const getVersionHeader = () => ProxyConnection.VERSION;
  let connection: ProxyConnection;
  let getSpy: jasmine.Spy<HttpRequestGetType>;
  let postSpy: jasmine.Spy<HttpRequestPostType>;

  describe('server not found', () => {
    const unsentResponse: HttpResponse = {
      status: HttpRequestStatus.UNSENT,
      type: '',
      text: '',
      body: undefined,
      getHeader: getVersionHeader,
    };

    beforeEach(async () => {
      await setUpTestEnvironment(unsentResponse);
    });

    afterEach(() => {
      localStorage.clear();
    });

    it('requests devices on initialization', () => {
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.NOT_FOUND);
    });

    it('requests devices on restarting connection', async () => {
      resetSpies();
      await connection.restartConnection();
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.NOT_FOUND);
    });

    it('posts start trace request to proxy and updates state if proxy not found', async () => {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.START_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        requestObj,
      );
      expect(connection.getState()).toEqual(ConnectionState.NOT_FOUND);
    });
  });

  describe('unsuccessful request', () => {
    afterEach(() => {
      localStorage.clear();
    });

    it('unauthorized server', async () => {
      const unauthResponse: HttpResponse = {
        status: HttpRequestStatus.UNAUTH,
        type: '',
        text: '',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(unauthResponse);
      await postToProxy();
      expect(connection.getState()).toEqual(ConnectionState.UNAUTH);
    });

    it('invalid version - header undefined', async () => {
      await checkInvalidVersion(() => undefined);
    });

    it('invalid version - old major', async () => {
      await checkInvalidVersion(() => '0.0.0');
    });

    it('invalid version - old minor', async () => {
      const [major, minor, patch] = ProxyConnection.VERSION.split('.');
      await checkInvalidVersion(() =>
        [major, Number(minor) - 1, patch].join('.'),
      );
    });

    it('invalid version - old patch', async () => {
      const [major, minor, patch] = ProxyConnection.VERSION.split('.');
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
      await setUpTestEnvironment(errorResponse);
      await postToProxy();
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
      expect(connection.getErrorText()).toEqual(errorResponse.text);
    });

    it('error state with response type empty', async () => {
      const errorResponse: HttpResponse = {
        status: HttpRequestStatus.ERROR,
        type: '',
        text: 'test error message',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(errorResponse);
      await postToProxy();
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
      expect(connection.getErrorText()).toEqual(errorResponse.text);
    });

    it('error state with response type array buffer', async () => {
      const errorResponse: HttpResponse = {
        status: HttpRequestStatus.ERROR,
        type: 'arraybuffer',
        text: '',
        body: [],
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(errorResponse);
      await postToProxy();
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
    });

    async function postToProxy() {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
    }

    async function checkInvalidVersion(getHeader: () => string | undefined) {
      const invalidResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '',
        body: undefined,
        getHeader,
      };
      await setUpTestEnvironment(invalidResponse);
      await postToProxy();
      expect(connection.getState()).toEqual(ConnectionState.INVALID_VERSION);
    }
  });

  describe('successful responses to tracing process', () => {
    beforeEach(async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(successfulResponse);
    });

    afterEach(() => {
      localStorage.clear();
    });

    it('uses stored token on initialization', async () => {
      resetSpies();
      connection.setSecurityToken('test_initial_token');
      connection = await createProxyConnection();
      checkGetDevicesRequest('test_initial_token');
    });

    it('sets security token and sends as header', async () => {
      resetSpies();
      connection.setSecurityToken('test_token');
      await connection.restartConnection();
      checkGetDevicesRequest('test_token');
    });

    it('does not set empty token', async () => {
      connection.setSecurityToken('test_initial_token');
      connection = await createProxyConnection();
      resetSpies();
      connection.setSecurityToken('');
      await connection.restartConnection();
      checkGetDevicesRequest('test_initial_token');
    });

    it('throws error on startTrace if no traces requested', async () => {
      await expectAsync(
        connection.startTrace(mockDevice, []),
      ).toBeRejectedWithError('No traces requested');
    });

    it('throws error on endTrace if no traces requested', async () => {
      await expectAsync(connection.endTrace()).toBeRejectedWithError(
        'Trace not started before stopping',
      );
    });

    it('throws error on dumpState if no dumps requested', async () => {
      await expectAsync(
        connection.dumpState(mockDevice, []),
      ).toBeRejectedWithError('No dumps requested');
    });

    it('posts start trace request to proxy and updates state to tracing', async () => {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);

      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.START_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        requestObj,
      );
      expect(connection.getState()).toEqual(ConnectionState.TRACING);
    });

    it('posts end trace request to proxy', async () => {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
      resetSpies();
      await connection.endTrace();

      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.END_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        undefined,
      );
    });

    it('posts dump state request to proxy', async () => {
      const requestObj = [mockTraceRequest];
      await connection.dumpState(mockDevice, requestObj);

      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.DUMP +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        requestObj.map((t) => t.name),
      );
      expect(connection.getState()).toEqual(ConnectionState.DUMPING_STATE);
    });
  });

  describe('finding devices', () => {
    afterEach(() => {
      localStorage.clear();
    });

    it('sets error state if onSuccess callback fails', async () => {
      const noDevicesResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'arraybuffer',
        text: '[0,]',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(noDevicesResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
      expect(connection.getErrorText()).toEqual(
        'Could not find devices. Received:\n[0,]',
      );
    });

    it('fetches devices', async () => {
      const noDevicesResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'text',
        text: JSON.stringify({
          '35562': {authorized: mockDevice.authorized, model: mockDevice.model},
        }),
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(noDevicesResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([mockDevice]);
    });
  });

  describe('files', () => {
    const testFileArray = [window.btoa('[20]')];
    const testFile = new File(testFileArray, 'test_file');

    const successfulResponse: HttpResponse = {
      status: HttpRequestStatus.SUCCESS,
      type: 'arraybuffer',
      text: 'True',
      body: new TextEncoder().encode(
        JSON.stringify({'test_file': testFileArray}),
      ),
      getHeader: getVersionHeader,
    };
    afterEach(() => {
      localStorage.clear();
    });

    it('sets error state if fetching files fails', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'arraybuffer',
        text: 'False',
        body: new TextEncoder().encode('[0,]'),
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(successfulResponse);
      resetSpies();
      await connection.fetchLastTracingSessionData(mockDevice);

      expect(getSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.FETCH +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        'arraybuffer',
      );
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
      expect(connection.getErrorText()).toEqual(
        'Could not fetch files. Received:\nFalse',
      );
    });

    it('fetches last tracing session data without ongoing tracing', async () => {
      await setUpTestEnvironment(successfulResponse);
      resetSpies();
      const files = await connection.fetchLastTracingSessionData(mockDevice);

      expect(getSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.FETCH +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        'arraybuffer',
      );
      expect(connection.getState()).toEqual(ConnectionState.LOADING_DATA);
      expect(files).toEqual([testFile]);
    });

    it('fetches last tracing session data from ongoing tracing', async () => {
      await setUpTestEnvironment(successfulResponse);
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
      await connection.endTrace();
      resetSpies();
      const files = await connection.fetchLastTracingSessionData(mockDevice);

      expect(getSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.FETCH +
          `${mockDevice.id}/${mockTraceRequest.name}/`,
        [['Winscope-Token', '']],
        'arraybuffer',
      );
      expect(connection.getState()).toEqual(ConnectionState.LOADING_DATA);
      expect(files).toEqual([testFile]);
    });
  });

  async function setUpTestEnvironment(response: HttpResponse) {
    getSpy = spyOn(HttpRequest, 'get').and.returnValue(
      Promise.resolve(response),
    );
    postSpy = spyOn(HttpRequest, 'post').and.returnValue(
      Promise.resolve(response),
    );
    connection = await createProxyConnection();
  }

  function checkGetDevicesRequest(header = '') {
    expect(getSpy).toHaveBeenCalledOnceWith(
      ProxyConnection.WINSCOPE_PROXY_URL + ProxyEndpoint.DEVICES,
      [['Winscope-Token', header]],
      undefined,
    );
  }

  function resetSpies() {
    getSpy.calls.reset();
    postSpy.calls.reset();
  }

  async function createProxyConnection() {
    const connection = new ProxyConnection();
    await connection.initialize(
      detectStateChangesInUi,
      progressCallback,
      availableTracesChangeCallback,
    );
    return connection;
  }
});

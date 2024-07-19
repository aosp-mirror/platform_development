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

import {AdbDevice} from 'trace_collection/adb_device';
import {ConnectionState} from 'trace_collection/connection_state';
import {ProxyConnection} from 'trace_collection/proxy_connection';
import {HttpRequest} from './http_request';
import {OnRequestStateChangeCallback} from './on_request_state_change_callback';
import {ProxyEndpoint} from './proxy_endpoint';
import {RequestHeaderType} from './request_header_type';
import {TraceRequest} from './trace_request';

type HttpRequestGetType = (
  path: string,
  headers: RequestHeaderType,
  onReadyStateChange: OnRequestStateChangeCallback,
  type?: XMLHttpRequest['responseType'],
) => Promise<void>;

type HttpRequestPostType = (
  path: string,
  headers: RequestHeaderType,
  onReadyStateChange: OnRequestStateChangeCallback,
  jsonRequest?: object,
) => Promise<void>;

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
  let getSpy: jasmine.Spy<HttpRequestGetType>;
  let postSpy: jasmine.Spy<HttpRequestPostType>;

  describe('call through to server', () => {
    let connection: ProxyConnection;

    beforeEach(async () => {
      getSpy = spyOn(HttpRequest, 'get').and.callThrough();
      postSpy = spyOn(HttpRequest, 'post').and.callThrough();
      connection = await createProxyConnection();
    });

    afterEach(() => {
      localStorage.clear();
    });

    it('requests devices on initialization', () => {
      checkGetDevicesRequest();
    });

    it('requests devices on restarting connection', async () => {
      resetSpies();
      await connection.restartConnection();
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.NOT_FOUND);
    });

    it('posts start trace request to proxy and does not update state if proxy not found', async () => {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.START_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        jasmine.any(Function),
        requestObj,
      );
      expect(connection.getState()).toEqual(ConnectionState.NOT_FOUND);
    });
  });

  describe('mock server responses', () => {
    let connection: ProxyConnection;

    beforeEach(async () => {
      connection = await createProxyConnection();
      getSpy = spyOn(HttpRequest, 'get');
      postSpy = spyOn(HttpRequest, 'post');
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

    it('throws error on startTrace if no traces requested', async () => {
      resetSpies();
      await expectAsync(
        connection.startTrace(mockDevice, []),
      ).toBeRejectedWithError('No traces requested');
    });

    it('throws error on endTrace if no traces requested', async () => {
      resetSpies();
      await expectAsync(connection.endTrace()).toBeRejectedWithError(
        'Trace not started before stopping',
      );
    });

    it('throws error on dumpState if no dumps requested', async () => {
      resetSpies();
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
        jasmine.any(Function),
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
        jasmine.any(Function),
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
        jasmine.any(Function),
        requestObj.map((t) => t.name),
      );
      expect(connection.getState()).toEqual(ConnectionState.DUMPING_STATE);
    });

    it('fetches last tracing session data without ongoing tracing', async () => {
      resetSpies();
      await connection.fetchLastTracingSessionData(mockDevice);

      expect(getSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.FETCH +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        jasmine.any(Function),
        'arraybuffer',
      );
      expect(connection.getState()).toEqual(ConnectionState.LOADING_DATA);
    });

    it('fetches last tracing session data from ongoing tracing', async () => {
      const requestObj = [mockTraceRequest];
      await connection.startTrace(mockDevice, requestObj);
      await connection.endTrace();
      resetSpies();
      await connection.fetchLastTracingSessionData(mockDevice);

      expect(getSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.FETCH +
          `${mockDevice.id}/${mockTraceRequest.name}/`,
        [['Winscope-Token', '']],
        jasmine.any(Function),
        'arraybuffer',
      );
      expect(connection.getState()).toEqual(ConnectionState.LOADING_DATA);
    });
  });

  function checkGetDevicesRequest(header = '') {
    expect(getSpy).toHaveBeenCalledOnceWith(
      ProxyConnection.WINSCOPE_PROXY_URL + ProxyEndpoint.DEVICES,
      [['Winscope-Token', header]],
      jasmine.any(Function),
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

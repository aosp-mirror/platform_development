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
import {ProxyTracingErrors} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {ConnectionState} from 'trace_collection/connection_state';
import {TraceTarget} from 'trace_collection/trace_target';
import {Endpoint} from './endpoint';
import {VERSION, WINSCOPE_PROXY_URL} from './utils';
import {
  WinscopeProxyDeviceConnection,
  WinscopeProxyDeviceConnectionResponse,
} from './winscope_proxy_device_connection';

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

describe('WinscopeProxyDeviceConnection', () => {
  const listener = jasmine.createSpyObj<AdbDeviceConnectionListener>(
    'AdbDeviceConnectionListener',
    ['onAvailableTracesChange', 'onError', 'onConnectionStateChange'],
  );
  const testId = 'testid';
  const getVersionHeader = () => VERSION;
  const successfulEndTraceResponse: HttpResponse = {
    status: HttpRequestStatus.SUCCESS,
    type: '',
    text: '[]',
    body: '[]',
    getHeader: getVersionHeader,
  };
  const securityHeader: HttpRequestHeaderType = [['Winscope-Token', '']];

  let connection: WinscopeProxyDeviceConnection;
  let getSpy: jasmine.Spy<HttpRequestGetType>;
  let postSpy: jasmine.Spy<HttpRequestPostType>;

  beforeEach(() => {
    connection = new WinscopeProxyDeviceConnection(
      testId,
      listener,
      securityHeader,
    );
    resetListener();
  });

  afterEach(() => {
    expect(listener.onAvailableTracesChange).not.toHaveBeenCalled();
    expect(listener.onError).not.toHaveBeenCalled();
    expect(listener.onConnectionStateChange).not.toHaveBeenCalled();
  });

  describe('errors:', () => {
    it('throws error on tryAuthorize', async () => {
      await expectAsync(connection.tryAuthorize()).toBeRejected();
    });

    it('unauthorized server', async () => {
      const unauthResponse: HttpResponse = {
        status: HttpRequestStatus.UNAUTH,
        type: '',
        text: '',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setHttpSpies(unauthResponse);
      await connection.runShellCommand('');
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.UNAUTH,
      ]);
      listener.onConnectionStateChange.calls.reset();
    });
  });

  describe('device properties:', () => {
    const successfulDeviceResponse: WinscopeProxyDeviceConnectionResponse = {
      id: '35562',
      authorized: true,
      model: 'Pixel 6',
    };
    const unauthDeviceResponse: WinscopeProxyDeviceConnectionResponse = {
      id: '35562',
      authorized: false,
      model: 'Pixel 6',
    };

    beforeEach(() => {
      spyOn(connection, 'runShellCommand').and.returnValue(Promise.resolve(''));
    });

    it('updates name from model', async () => {
      expect(connection.getFormattedName()).toEqual(`offline (${testId})`);
      await connection.updateProperties(successfulDeviceResponse);
      expect(connection.getFormattedName()).toEqual(`Pixel 6 (${testId})`);
    });

    it('updates state to AVAILABLE', async () => {
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
      await connection.updateProperties(successfulDeviceResponse);
      expect(connection.getState()).toEqual(AdbDeviceState.AVAILABLE);
    });

    it('updates state to UNAUTHORIZED', async () => {
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
      await connection.updateProperties(unauthDeviceResponse);
      expect(connection.getState()).toEqual(AdbDeviceState.UNAUTHORIZED);
    });

    it('updates state from AVAILABLE to UNAUTHORIZED', async () => {
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
      await connection.updateProperties(successfulDeviceResponse);
      expect(connection.getState()).toEqual(AdbDeviceState.AVAILABLE);
      await connection.updateProperties(unauthDeviceResponse);
      expect(connection.getState()).toEqual(AdbDeviceState.UNAUTHORIZED);
    });
  });

  describe('shell commands:', () => {
    it('converts command to shell command and output to string', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: '123',
        getHeader: getVersionHeader,
      };
      await setHttpSpies(successfulResponse);
      const output = await connection.runShellCommand('test cmd');
      expect(output).toEqual('123');
      expect(postSpy).toHaveBeenCalledOnceWith(
        WINSCOPE_PROXY_URL + Endpoint.RUN_ADB_CMD + `${testId}/`,
        securityHeader,
        {cmd: 'shell test cmd'},
      );
    });

    it('handles json output', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: '"123"',
        getHeader: getVersionHeader,
      };
      await setHttpSpies(successfulResponse);
      const output = await connection.runShellCommand('');
      expect(output).toEqual('123');
    });

    it('handles undefined output', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setHttpSpies(successfulResponse);
      const output = await connection.runShellCommand('');
      expect(output).toEqual('');
    });
  });

  describe('tracing:', () => {
    const targetName = 'TestTarget';
    const startCmd = 'start cmd';
    const stopCmd = 'stop cmd';
    const mockTarget = new TraceTarget(targetName, [], startCmd, stopCmd, []);
    let userNotifierChecker: UserNotifierChecker;

    beforeAll(() => {
      userNotifierChecker = new UserNotifierChecker();
    });

    beforeEach(async () => {
      userNotifierChecker.reset();
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'True',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setHttpSpies(successfulResponse);
    });

    it('posts start trace request to proxy', async () => {
      await startTrace();
      checkStartTraceRequested();
    });

    it('sets connection state if start trace fails due to unsent response', async () => {
      const response: HttpResponse = {
        status: HttpRequestStatus.UNSENT,
        type: '',
        text: 'True',
        body: undefined,
        getHeader: getVersionHeader,
      };
      postSpy.and.returnValue(Promise.resolve(response));
      await startTrace();
      checkStartTraceRequested();
      expect(listener.onConnectionStateChange).toHaveBeenCalledOnceWith(
        ConnectionState.NOT_FOUND,
      );
      listener.onConnectionStateChange.calls.reset();
    });

    it('handles trace timeout', async () => {
      getSpy.and.returnValue(
        Promise.resolve({
          status: HttpRequestStatus.SUCCESS,
          type: '',
          text: 'False',
          body: undefined,
          getHeader: getVersionHeader,
        }),
      );
      postSpy.and.returnValue(
        Promise.resolve({
          status: HttpRequestStatus.SUCCESS,
          type: '',
          text: 'True',
          body: '[]',
          getHeader: getVersionHeader,
        }),
      );
      await startTrace();
      checkStartTraceRequested(2);
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.TRACE_TIMEOUT,
      ]);
      listener.onConnectionStateChange.calls.reset();
    });

    it('posts end trace request to proxy and handles response without errors', async () => {
      await startAndEndTrace(successfulEndTraceResponse);
      checkTraceEnded();
      userNotifierChecker.expectNone();
    });

    it('sets connection state if end trace fails due to unsent response', async () => {
      await startAndEndTrace({
        status: HttpRequestStatus.UNSENT,
        type: '',
        text: '[]',
        body: '[]',
        getHeader: getVersionHeader,
      });
      checkTraceEnded();
      expect(listener.onConnectionStateChange).toHaveBeenCalledOnceWith(
        ConnectionState.NOT_FOUND,
      );
      listener.onConnectionStateChange.calls.reset();
    });

    it('posts end trace request to proxy and handles response with errors', async () => {
      await startAndEndTrace({
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '["please check your display state", "b\'unknown error\'"]',
        body: '["please check your display state", "b\'unknown error\'"]',
        getHeader: getVersionHeader,
      });
      checkTraceEnded();
      userNotifierChecker.expectAdded([
        new ProxyTracingErrors([
          'please check your display state (must be on at start of trace)',
          "'unknown error'",
        ]),
      ]);
    });

    it('posts end trace request to proxy and handles non-serializable errors', async () => {
      await startAndEndTrace({
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '["please check your display state", "b\'unknown error\'"]',
        body: undefined,
        getHeader: getVersionHeader,
      });
      checkTraceEnded();
      expect(listener.onError.calls.mostRecent().args).toEqual([
        `Error handling request response: SyntaxError: "undefined" is not valid JSON
Request text: ["please check your display state", "b'unknown error'"]
Request body: undefined`,
      ]);
      listener.onError.calls.reset();
    });

    async function startTrace() {
      postSpy.calls.reset();
      await connection.startTrace(mockTarget);
    }

    function checkStartTraceRequested(times = 1) {
      expect(postSpy).toHaveBeenCalledTimes(1);
      expect(postSpy).toHaveBeenCalledWith(
        WINSCOPE_PROXY_URL + Endpoint.START_TRACE + `${testId}/`,
        securityHeader,
        {
          targetId: targetName,
          startCmd,
          stopCmd,
        },
      );
    }

    async function startAndEndTrace(endingTraceResponse: HttpResponse) {
      await connection.startTrace(mockTarget);
      resetSpies();
      postSpy.and.returnValue(Promise.resolve(endingTraceResponse));
      await connection.endTrace(mockTarget);
    }

    function checkTraceEnded() {
      expect(postSpy).toHaveBeenCalledOnceWith(
        WINSCOPE_PROXY_URL + Endpoint.END_TRACE + `${testId}/`,
        securityHeader,
        {targetId: targetName},
      );
    }

    function resetSpies() {
      getSpy.calls.reset();
      postSpy.calls.reset();
    }
  });

  describe('fetching file:', () => {
    const testFileEncoded = window.btoa('[20]');
    const testFileData = Uint8Array.from(window.atob(testFileEncoded), (c) =>
      c.charCodeAt(0),
    );
    const testFilepath = 'test_filepath';

    it('sets error state if fetching files fails', async () => {
      const response: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'arraybuffer',
        text: 'False',
        body: new TextEncoder().encode('[0,]'),
        getHeader: getVersionHeader,
      };
      await setHttpSpies(response);
      const data = await connection.pullFile(testFilepath);
      expect(data).toEqual(Uint8Array.from([]));
      checkFetchRequested();
      expect(listener.onError).toHaveBeenCalledOnceWith(
        'Could not fetch file. Received: False',
      );
      listener.onError.calls.reset();
    });

    it('sets connection state if fetching files fails due to unsent response', async () => {
      const response: HttpResponse = {
        status: HttpRequestStatus.UNSENT,
        type: 'arraybuffer',
        text: 'False',
        body: new TextEncoder().encode('[0,]'),
        getHeader: getVersionHeader,
      };
      await setHttpSpies(response);
      const data = await connection.pullFile(testFilepath);
      expect(data).toEqual(Uint8Array.from([]));
      checkFetchRequested();
      expect(listener.onConnectionStateChange).toHaveBeenCalledOnceWith(
        ConnectionState.NOT_FOUND,
      );
      listener.onConnectionStateChange.calls.reset();
    });

    it('fetches last tracing session data', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'arraybuffer',
        text: 'True',
        body: new TextEncoder().encode(
          JSON.stringify({'test_filepath': testFileEncoded}),
        ),
        getHeader: getVersionHeader,
      };
      await setHttpSpies(successfulResponse);
      const data = await connection.pullFile(testFilepath);
      expect(data).toEqual(testFileData);

      checkFetchRequested();
      expect(data).toEqual(testFileData);
    });

    function checkFetchRequested() {
      expect(getSpy).toHaveBeenCalledOnceWith(
        WINSCOPE_PROXY_URL + Endpoint.FETCH + `${testId}/${testFilepath}`,
        securityHeader,
        'arraybuffer',
      );
    }
  });

  async function setHttpSpies(
    getResponse: HttpResponse,
    postResponse = getResponse,
  ) {
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
  }
});

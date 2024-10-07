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
import {ProxyTracingErrors} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {waitToBeCalled} from 'test/utils';
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
  const configOptionsChangeCallback = jasmine.createSpy();
  const availableTracesChangeCallback = jasmine.createSpy();
  const mockDevice: AdbDevice = {
    id: '35562',
    model: 'Pixel 6',
    authorized: true,
    displays: ['"Test Display" 12345 Extra Info'],
    multiDisplayScreenRecordingAvailable: false,
  };
  const mockTraceRequest: TraceRequest = {
    name: 'layers_trace',
    config: [],
  };
  const getVersionHeader = () => ProxyConnection.VERSION;
  const successfulEndTraceResponse: HttpResponse = {
    status: HttpRequestStatus.SUCCESS,
    type: '',
    text: '[]',
    body: '[]',
    getHeader: getVersionHeader,
  };

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
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
    });

    async function checkInvalidVersion(getHeader: () => string | undefined) {
      const invalidResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '',
        body: undefined,
        getHeader,
      };
      await setUpTestEnvironment(invalidResponse);
      expect(connection.getState()).toEqual(ConnectionState.INVALID_VERSION);
    }
  });

  describe('successful responses to tracing process', () => {
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
      await checkSuccessfulTraceRequest([mockTraceRequest]);
    });

    it('posts start trace requests without updating screen recording config', async () => {
      const requestEmptyStrDisplays: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: ''}],
        },
      ];
      await checkSuccessfulTraceRequest(requestEmptyStrDisplays);

      const requestEmptyArrDisplays: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: []}],
        },
      ];
      await checkSuccessfulTraceRequest(requestEmptyArrDisplays);

      const requestDisplayStrWithoutName: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: '12345 Other Info'}],
        },
      ];
      await checkSuccessfulTraceRequest(requestDisplayStrWithoutName);

      const requestDisplayArrWithoutName: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: ['12345 Other Info']}],
        },
      ];
      await checkSuccessfulTraceRequest(requestDisplayArrWithoutName);
    });

    it('posts start trace requests with updated screen recording config', async () => {
      const requestDisplayStrWithName: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: '"Test Display" 12345 Other Info'}],
        },
      ];
      await checkSuccessfulTraceRequest(requestDisplayStrWithName, [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: '12345 Other Info'}],
        },
      ]);

      const requestDisplayArrWithName: TraceRequest[] = [
        {
          name: 'screen_recording',
          config: [
            {key: 'displays', value: ['"Test Display" 12345 Other Info']},
          ],
        },
      ];
      await checkSuccessfulTraceRequest(requestDisplayArrWithName, [
        {
          name: 'screen_recording',
          config: [{key: 'displays', value: ['12345 Other Info']}],
        },
      ]);
    });

    it('handles trace timeout', async () => {
      const requestObj = [mockTraceRequest];
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
      await connection.startTrace(mockDevice, requestObj);

      expect(postSpy).toHaveBeenCalledWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.START_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        requestObj,
      );
      expect(postSpy).toHaveBeenCalledWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.END_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        undefined,
      );
      expect(connection.getState()).toEqual(ConnectionState.TRACE_TIMEOUT);
    });

    it('posts end trace request to proxy and handles response without errors', async () => {
      await startAndEndTrace(successfulEndTraceResponse);
      checkTraceEndedSuccessfully();
      userNotifierChecker.expectNone();
    });

    it('posts end trace request to proxy and handles response with errors', async () => {
      await startAndEndTrace({
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: '["please check your display state", "b\'unknown error\'"]',
        body: '["please check your display state", "b\'unknown error\'"]',
        getHeader: getVersionHeader,
      });
      checkTraceEndedSuccessfully();
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
      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.END_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        undefined,
      );
      expect(connection.getState()).toEqual(ConnectionState.ERROR);
      expect(connection.getErrorText()).toContain(
        'Error handling request response',
      );
    });

    it('posts dump state request to proxy', async () => {
      await checkSuccessfulDumpRequest([mockTraceRequest]);
    });

    it('posts dump requests without updating screenshot config', async () => {
      const requestEmptyArrDisplays: TraceRequest[] = [
        {
          name: 'screenshot',
          config: [{key: 'displays', value: []}],
        },
      ];
      await checkSuccessfulDumpRequest(requestEmptyArrDisplays);

      const requestDisplayArrWithoutName: TraceRequest[] = [
        {
          name: 'screenshot',
          config: [{key: 'displays', value: ['12345 Other Info']}],
        },
      ];
      await checkSuccessfulDumpRequest(requestDisplayArrWithoutName);
    });

    it('posts dump requests with updated screenshot config', async () => {
      const requestDisplayArrWithName: TraceRequest[] = [
        {
          name: 'screenshot',
          config: [
            {key: 'displays', value: ['"Test Display" 12345 Other Info']},
          ],
        },
      ];
      await checkSuccessfulDumpRequest(requestDisplayArrWithName, [
        {
          name: 'screenshot',
          config: [{key: 'displays', value: ['12345 Other Info']}],
        },
      ]);
    });

    function checkTraceEndedSuccessfully() {
      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.END_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        undefined,
      );
      expect(connection.getState()).toEqual(ConnectionState.ENDING_TRACE);
    }

    async function checkSuccessfulTraceRequest(
      requests: TraceRequest[],
      updatedRequests = requests,
    ) {
      postSpy.calls.reset();
      await connection.startTrace(mockDevice, requests);

      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.START_TRACE +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        updatedRequests,
      );
      expect(connection.getState()).toEqual(ConnectionState.TRACING);
    }

    async function checkSuccessfulDumpRequest(
      requests: TraceRequest[],
      updatedRequests = requests,
    ) {
      postSpy.calls.reset();
      await connection.dumpState(mockDevice, requests);

      expect(postSpy).toHaveBeenCalledOnceWith(
        ProxyConnection.WINSCOPE_PROXY_URL +
          ProxyEndpoint.DUMP +
          `${mockDevice.id}/`,
        [['Winscope-Token', '']],
        updatedRequests,
      );
      expect(connection.getState()).toEqual(ConnectionState.DUMPING_STATE);
    }
  });

  describe('wayland trace availability', () => {
    beforeEach(() => {
      availableTracesChangeCallback.calls.reset();
    });

    afterEach(() => {
      localStorage.clear();
    });

    it('updates availability of wayland trace if available', async () => {
      const successfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'true',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(successfulResponse);
      expect(availableTracesChangeCallback).toHaveBeenCalledOnceWith([
        'wayland_trace',
      ]);
    });

    it('does not update availability of traces if call fails', async () => {
      const unsuccessfulResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: '',
        text: 'false',
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(unsuccessfulResponse);
      expect(availableTracesChangeCallback).not.toHaveBeenCalled();
    });
  });

  describe('finding devices', () => {
    const successfulResponse: HttpResponse = {
      status: HttpRequestStatus.SUCCESS,
      type: 'text',
      text: JSON.stringify({
        '35562': {
          authorized: mockDevice.authorized,
          model: mockDevice.model,
          displays: ['Display 12345 Extra Info displayName="Test Display"'],
        },
      }),
      body: undefined,
      getHeader: getVersionHeader,
    };

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
      await setUpTestEnvironment(successfulResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([mockDevice]);
    });

    it('sets up worker to fetch devices', async () => {
      await setUpTestEnvironment(successfulResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([mockDevice]);
      expect(getSpy).toHaveBeenCalledWith(
        ProxyConnection.WINSCOPE_PROXY_URL + ProxyEndpoint.CHECK_WAYLAND,
        [['Winscope-Token', '']],
        undefined,
      );

      getSpy.calls.reset();
      detectStateChangesInUi.calls.reset();

      await waitToBeCalled(detectStateChangesInUi, 1);
      await waitToBeCalled(getSpy, 1);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([mockDevice]);
      expect(getSpy).not.toHaveBeenCalledWith(
        ProxyConnection.WINSCOPE_PROXY_URL + ProxyEndpoint.CHECK_WAYLAND,
        [['Winscope-Token', '']],
        undefined,
      );
    });

    it('handles missing displayName', async () => {
      const response: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'text',
        text: JSON.stringify({
          '35562': {
            authorized: mockDevice.authorized,
            model: mockDevice.model,
            displays: ['Display 12345 Extra Info'],
          },
        }),
        body: undefined,
        getHeader: getVersionHeader,
      };

      await setUpTestEnvironment(response);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([
        {
          id: '35562',
          model: 'Pixel 6',
          authorized: true,
          displays: ['12345 Extra Info'],
          multiDisplayScreenRecordingAvailable: false,
        },
      ]);
    });

    it('updates multi display screen recording availability for incompatible version', async () => {
      const oldVersionResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'text',
        text: JSON.stringify({
          '35562': {
            authorized: mockDevice.authorized,
            model: mockDevice.model,
            displays: ['Display 12345 Extra Info displayName="Test Display"'],
            screenrecord_version: '1.3',
          },
        }),
        body: undefined,
        getHeader: getVersionHeader,
      };
      await setUpTestEnvironment(oldVersionResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([mockDevice]);
    });

    it('updates multi display screen recording availability for compatible version', async () => {
      const compatibleVersionResponse: HttpResponse = {
        status: HttpRequestStatus.SUCCESS,
        type: 'text',
        text: JSON.stringify({
          '35562': {
            authorized: mockDevice.authorized,
            model: mockDevice.model,
            displays: ['Display 12345 Extra Info displayName="Test Display"'],
            screenrecord_version: '1.4',
          },
        }),
        body: undefined,
        getHeader: getVersionHeader,
      };
      const mockDeviceWithMultiDisplayScreenRecording: AdbDevice = {
        id: '35562',
        model: 'Pixel 6',
        authorized: true,
        displays: ['"Test Display" 12345 Extra Info'],
        multiDisplayScreenRecordingAvailable: true,
      };

      await setUpTestEnvironment(compatibleVersionResponse);
      checkGetDevicesRequest();
      expect(connection.getState()).toEqual(ConnectionState.IDLE);
      expect(connection.getDevices()).toEqual([
        mockDeviceWithMultiDisplayScreenRecording,
      ]);
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
      await startAndEndTrace(successfulEndTraceResponse);
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
    expect(getSpy).toHaveBeenCalledWith(
      ProxyConnection.WINSCOPE_PROXY_URL + ProxyEndpoint.DEVICES,
      [['Winscope-Token', header]],
      undefined,
    );
  }

  async function startAndEndTrace(endingTraceResponse: HttpResponse) {
    await connection.startTrace(mockDevice, [mockTraceRequest]);
    resetSpies();
    postSpy.and.returnValue(Promise.resolve(endingTraceResponse));
    await connection.endTrace();
  }

  function resetSpies() {
    getSpy.calls.reset();
    postSpy.calls.reset();
  }

  async function createProxyConnection() {
    const connection = new ProxyConnection();
    await connection.initialize(
      detectStateChangesInUi,
      availableTracesChangeCallback,
      configOptionsChangeCallback,
    );
    return connection;
  }
});

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

import {ArrayBufferBuilder, stringToByteArray} from 'common/buffer_utils';
import {FunctionUtils} from 'common/function_utils';
import {WindowUtils} from 'common/window_utils';
import {
  ProxyTracingErrors,
  ProxyTracingWarnings,
} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {TraceTarget} from 'trace_collection/trace_target';
import {AdbWebSocketStream} from './adb_websocket_stream';
import {ShellStream} from './shell_stream';
import {StreamProvider} from './stream_provider';
import {SyncStream} from './sync_stream';
import {WdpDeviceConnection} from './wdp_device_connection';
import {WdpDeviceConnectionResponse} from './wdp_host_connection';

describe('WdpDeviceConnection', () => {
  const listener = jasmine.createSpyObj<AdbDeviceConnectionListener>(
    'AdbDeviceConnectionListener',
    ['onAvailableTracesChange', 'onError', 'onConnectionStateChange'],
  );
  const testId = 'test id';
  const testApproveUrl = 'test_approve_url';
  let connection: WdpDeviceConnection;
  const emptyResp = stringToByteArray('').buffer;
  let popupSpy: jasmine.Spy;
  let openStream: ShellStream | undefined;

  beforeEach(() => {
    popupSpy = spyOn(WindowUtils, 'showPopupWindow');
    connection = new WdpDeviceConnection(testId, listener);
    resetListener();
  });

  afterEach(() => {
    expect(listener.onAvailableTracesChange).not.toHaveBeenCalled();
    expect(listener.onError).not.toHaveBeenCalled();
    expect(listener.onConnectionStateChange).not.toHaveBeenCalled();
  });

  describe('authorization and destruction:', () => {
    it('handles tryAuthorize if no approve URL', async () => {
      await expectAsync(connection.tryAuthorize()).toBeResolved();
    });

    it('shows popup on tryAuthorize', async () => {
      connection = new WdpDeviceConnection(testId, listener, testApproveUrl);
      popupSpy.and.returnValue(true);
      await connection.tryAuthorize();
      expect(popupSpy).toHaveBeenCalledOnceWith(testApproveUrl);
    });

    it('calls listener if popup fails to show', async () => {
      connection = new WdpDeviceConnection(testId, listener, testApproveUrl);
      popupSpy.and.returnValue(false);
      await connection.tryAuthorize();
      expect(popupSpy).toHaveBeenCalledOnceWith(testApproveUrl);
      expect(listener.onError).toHaveBeenCalledOnceWith(
        'Please enable popups and try again.',
      );
      listener.onError.calls.reset();
    });

    it('closes active trace stream onDestroy', async () => {
      spyOn(AdbWebSocketStream.prototype, 'write').and.callFake(
        FunctionUtils.DO_NOTHING_ASYNC,
      );
      await connection.startTrace(new TraceTarget('', [], '', '', [], true));
      const closeSpy = spyOn(AdbWebSocketStream.prototype, 'close');
      connection.onDestroy();
      expect(closeSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('device properties:', () => {
    const mockDevJson: WdpDeviceConnectionResponse = {
      serialNumber: testId,
      proxyStatus: 'ADB',
      adbStatus: 'DEVICE',
      adbProps: {
        model: 'Pixel_6',
      },
      approveUrl: testApproveUrl,
    };

    it('updates name from model', async () => {
      setEmptyRespToAllShellCommands();
      expect(connection.getFormattedName()).toEqual(`offline (${testId})`);
      await connection.updateProperties(mockDevJson);
      expect(connection.getFormattedName()).toEqual(`Pixel 6 (${testId})`);
    });

    it('handles missing model name', async () => {
      setEmptyRespToAllShellCommands();
      await connection.updateProperties({
        serialNumber: testId,
        proxyStatus: 'ADB',
        adbStatus: 'DEVICE',
        adbProps: {},
        approveUrl: testApproveUrl,
      });
      expect(connection.getFormattedName()).toEqual(`unknown (${testId})`);
    });

    it('updates state to AVAILABLE', async () => {
      setEmptyRespToAllShellCommands();
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
      await connection.updateProperties(mockDevJson);
      expect(connection.getState()).toEqual(AdbDeviceState.AVAILABLE);
    });

    it('updates state from AVAILABLE to OFFLINE', async () => {
      setEmptyRespToAllShellCommands();
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
      await connection.updateProperties(mockDevJson);
      expect(connection.getState()).toEqual(AdbDeviceState.AVAILABLE);
      await connection.updateProperties({
        serialNumber: testId,
        proxyStatus: 'ADB',
        adbStatus: 'OFFLINE',
        adbProps: {
          model: 'Pixel_6',
        },
        approveUrl: testApproveUrl,
      });
      expect(connection.getState()).toEqual(AdbDeviceState.OFFLINE);
    });

    it('sets state to UNAUTHORIZED and shows authorization pop up', async () => {
      popupSpy.and.returnValue(true);
      await connection.updateProperties({
        serialNumber: testId,
        proxyStatus: 'PROXY_UNAUTHORIZED',
        adbStatus: 'DEVICE',
        adbProps: {
          model: 'Pixel_6',
        },
        approveUrl: testApproveUrl,
      });
      expect(connection.getState()).toEqual(AdbDeviceState.UNAUTHORIZED);
      expect(popupSpy).toHaveBeenCalledOnceWith(testApproveUrl);
    });

    it('only shows device authorization popup if available and not already shown for this device', async () => {
      popupSpy.and.returnValue(true);
      const devJson: WdpDeviceConnectionResponse = {
        serialNumber: testId,
        proxyStatus: 'PROXY_UNAUTHORIZED',
        adbStatus: 'DEVICE',
        adbProps: {
          model: 'Pixel_6',
        },
        approveUrl: undefined,
      };
      await connection.updateProperties(devJson);
      expect(connection.getState()).toEqual(AdbDeviceState.UNAUTHORIZED);
      expect(popupSpy).not.toHaveBeenCalled();

      devJson.approveUrl = testApproveUrl;
      await connection.updateProperties(devJson);
      await connection.updateProperties(devJson);
      expect(connection.getState()).toEqual(AdbDeviceState.UNAUTHORIZED);
      expect(popupSpy).toHaveBeenCalledOnceWith(testApproveUrl);
    });

    function setEmptyRespToAllShellCommands() {
      setShellStreamResponses(
        [
          'service check Wayland',
          'screenrecord --version',
          'su root dumpsys SurfaceFlinger --display-id',
        ],
        [],
      );
    }
  });

  describe('shell commands:', () => {
    it('converts command to shell command and output to string', async () => {
      setShellStreamResponses(
        [],
        [{command: 'test cmd', resps: ['cmd complete']}],
      );
      const output = await connection.runShellCommand('test cmd');
      expect(output).toEqual('cmd complete');
      expect(listener.onConnectionStateChange).not.toHaveBeenCalled();
    });

    it('handles response over multiple messages', async () => {
      setShellStreamResponses(
        [],
        [{command: 'test cmd', resps: ['cmd ', 'complete']}],
      );
      const output = await connection.runShellCommand('test cmd');
      expect(output).toEqual('cmd complete');
    });

    it('calls listener on shell command error', async () => {
      setShellStreamResponses(
        [],
        [],
        [],
        [{command: 'test cmd', resps: ['test error']}],
      );
      const output = await connection.runShellCommand('test cmd');
      expect(output).toEqual('');
      expect(listener.onError).toHaveBeenCalledTimes(1);
      listener.onError.calls.reset();
    });
  });

  describe('tracing:', () => {
    const targetName = 'TestTarget';
    const startCmd = 'start cmd';
    const stopCmd = 'stop cmd';
    const mockTarget = new TraceTarget(targetName, [], startCmd, stopCmd, []);
    const mockSrTarget = new TraceTarget(
      targetName,
      [],
      startCmd,
      stopCmd,
      [],
      true,
    );

    let userNotifierChecker: UserNotifierChecker;
    let runShellCmdSpy: jasmine.Spy;

    beforeAll(() => {
      userNotifierChecker = new UserNotifierChecker();
    });

    beforeEach(() => {
      userNotifierChecker.reset();
      runShellCmdSpy = spyOn(connection, 'runShellCommand').and.callThrough();
    });

    afterEach(() => {
      userNotifierChecker.expectNone();
    });

    it('starts trace via non-interactive adb shell command', async () => {
      setShellStreamResponses([], [{command: startCmd, resps: ['started.']}]);
      await connection.startTrace(mockTarget);
      expect(runShellCmdSpy).toHaveBeenCalledWith(startCmd);
    });

    it('starts trace via non-interactive adb shell command with warnings', async () => {
      setShellStreamResponses(
        [],
        [{command: startCmd, resps: ['Error starting trace.']}],
      );
      await connection.startTrace(mockTarget);
      expect(runShellCmdSpy).toHaveBeenCalledWith(startCmd);
      userNotifierChecker.expectNotified([
        new ProxyTracingWarnings(['Error starting trace.']),
      ]);
      userNotifierChecker.reset();
    });

    it('starts trace via non-interactive adb shell command with warnings and done token present', async () => {
      setShellStreamResponses(
        [],
        [{command: startCmd, resps: ['Trace started. Error starting trace.']}],
      );
      await connection.startTrace(mockTarget);
      expect(runShellCmdSpy).toHaveBeenCalledWith(startCmd);
      userNotifierChecker.expectNotified([
        new ProxyTracingWarnings(['Error starting trace.']),
      ]);
      userNotifierChecker.reset();
    });

    it('starts screen recording with no errors', async () => {
      const srShellResponses = [
        {
          command: '',
          resps: [''],
        },
        {
          command: stringToByteArray(startCmd),
          resps: [''],
        },
      ];
      setShellStreamResponses([], [], srShellResponses);
      await connection.startTrace(mockSrTarget);
      expect(runShellCmdSpy).not.toHaveBeenCalledWith(startCmd);
      expect(openStream).toBeDefined();
      expect(openStream?.isOpen()).toBeTrue();
    });

    it('ends trace via non-interactive adb shell command with no errors', async () => {
      setShellStreamResponses(
        [],
        [
          {command: startCmd, resps: ['started.']},
          {command: stopCmd, resps: ['']},
        ],
      );
      await connection.startTrace(mockTarget);
      await connection.endTrace(mockTarget);
      expect(runShellCmdSpy).toHaveBeenCalledWith(stopCmd);
    });

    it('ends screen recording with no errors', async () => {
      const srShellResponses = [
        {
          command: '',
          resps: [''],
        },
        {
          command: stringToByteArray(startCmd),
          resps: [''],
        },
        {
          command: new Uint8Array([0x03]),
          resps: [''],
        },
      ];
      setShellStreamResponses([stopCmd], [], srShellResponses);
      await connection.startTrace(mockSrTarget);
      await connection.endTrace(mockSrTarget);
      expect(openStream?.isOpen()).toBeFalse();
      expect(runShellCmdSpy).toHaveBeenCalledWith(stopCmd);
    });

    it('ends screen recording with errors', async () => {
      const srShellResponses = [
        {
          command: '',
          resps: [''],
        },
        {
          command: stringToByteArray(startCmd),
          resps: ['ERROR: please check your display state'],
        },
        {
          command: new Uint8Array([0x03]),
          resps: [''],
        },
      ];
      setShellStreamResponses([stopCmd], [], srShellResponses);
      await connection.startTrace(mockSrTarget);
      await connection.endTrace(mockSrTarget);
      expect(openStream?.isOpen()).toBeFalse();
      expect(runShellCmdSpy).toHaveBeenCalledWith(stopCmd);
      userNotifierChecker.expectNotified([
        new ProxyTracingErrors([
          'Error ending screen recording on device: ' +
            'ERROR: please check your display state (must be on at start of trace)',
        ]),
      ]);
      userNotifierChecker.reset();
    });
  });

  describe('fetching file:', () => {
    const fileData = stringToByteArray('4');
    const encodedData = new ArrayBufferBuilder()
      .append([
        'DATA',
        fileData.length,
        fileData,
        'DONE',
        Uint8Array.from([0, 0, 0, 0]),
      ])
      .build();
    const testFilepath = 'test_filepath';

    it('sets error state if fetching files fails', async () => {
      setSyncStreamResponses([{filepath: testFilepath, data: fileData}]);
      const data = await connection.pullFile(testFilepath);
      expect(data).toEqual(Uint8Array.from([]));
      expect(listener.onError).toHaveBeenCalledWith(
        `Could not parse data:
Received: 52
Error: Expected message data to be ArrayBuffer or Blob.`,
      );
      listener.onError.calls.reset();
    });

    it('fetches last session data', async () => {
      setSyncStreamResponses([{filepath: testFilepath, data: encodedData}]);
      const data = await connection.pullFile(testFilepath);
      expect(data).toEqual(fileData);
      listener.onError.calls.reset();
    });
  });

  function makeServiceCommandJson(command: string, service = 'shell') {
    return JSON.stringify({
      header: {
        serialNumber: testId,
        command: service + ':' + command,
      },
    });
  }

  function setShellStreamResponses(
    commandsWithNoResponse: string[],
    commandsWithResponse: Array<{command: string; resps: string[]}>,
    commandsWithOpenStream: Array<{
      command: string | Uint8Array;
      resps: string[];
    }> = [],
    commandsWithStrResponse: Array<{command: string; resps: string[]}> = [],
  ) {
    const shellStreamSpy = spyOn(StreamProvider.prototype, 'createShellStream');
    shellStreamSpy.and.callFake((device, sock, datalistener, errorlistener) => {
      const fakeAdbSocket = UnitTestUtils.makeFakeWebSocket();
      const shellStream = new ShellStream(
        fakeAdbSocket,
        device,
        datalistener,
        errorlistener,
      );

      fakeAdbSocket.close.and.callFake(() => {
        fakeAdbSocket.onclose!(new CloseEvent(''));
        spyOn(shellStream, 'isOpen').and.returnValue(false);
      });

      commandsWithNoResponse.forEach((command) => {
        fakeAdbSocket.send
          .withArgs(makeServiceCommandJson(command))
          .and.callFake(async () => {
            const message = UnitTestUtils.makeFakeWebSocketMessage(emptyResp);
            fakeAdbSocket.onmessage!(message);
            fakeAdbSocket.onclose!(new CloseEvent(''));
          });
      });
      commandsWithResponse.forEach(({command, resps}) => {
        fakeAdbSocket.send
          .withArgs(makeServiceCommandJson(command))
          .and.callFake(async () => {
            resps.forEach((resp) => {
              const data = stringToByteArray(resp).buffer;
              const message = UnitTestUtils.makeFakeWebSocketMessage(data);
              fakeAdbSocket.onmessage!(message);
            });
            fakeAdbSocket.onclose!(new CloseEvent(''));
          });
      });
      commandsWithOpenStream.forEach(({command, resps}) => {
        let data: string | Uint8Array;
        if (typeof command === 'string') {
          data = makeServiceCommandJson(command);
        } else {
          data = command;
        }
        fakeAdbSocket.send.withArgs(data).and.callFake(async () => {
          resps.forEach((resp) => {
            openStream = shellStream;
            const data = stringToByteArray(resp).buffer;
            const message = UnitTestUtils.makeFakeWebSocketMessage(data);
            fakeAdbSocket.onmessage!(message);
          });
        });
      });
      commandsWithStrResponse.forEach(({command, resps}) => {
        fakeAdbSocket.send
          .withArgs(makeServiceCommandJson(command))
          .and.callFake(async () => {
            resps.forEach((resp) => {
              const message = UnitTestUtils.makeFakeWebSocketMessage(resp);
              fakeAdbSocket.onmessage!(message);
            });
            fakeAdbSocket.onclose!(new CloseEvent(''));
          });
      });
      return shellStream;
    });
  }

  function setSyncStreamResponses(
    responses: Array<{filepath: string; data: ArrayBuffer}>,
  ) {
    const syncStreamSpy = spyOn(StreamProvider.prototype, 'createSyncStream');
    syncStreamSpy.and.callFake((device, sock, errorlistener) => {
      const fakeAdbSocket = UnitTestUtils.makeFakeWebSocket();
      const syncStream = new SyncStream(fakeAdbSocket, device, errorlistener);

      fakeAdbSocket.close.and.callFake(() => {
        fakeAdbSocket.onclose!(new CloseEvent(''));
        spyOn(syncStream, 'isOpen').and.returnValue(false);
      });

      fakeAdbSocket.send
        .withArgs(makeServiceCommandJson('', 'sync'))
        .and.callFake(async () => {
          const message = UnitTestUtils.makeFakeWebSocketMessage(emptyResp);
          fakeAdbSocket.onmessage!(message);
        });

      responses.forEach(({filepath, data}) => {
        const tokens = new ArrayBufferBuilder()
          .append(['RECV', filepath.length, filepath])
          .build();
        fakeAdbSocket.send
          .withArgs(new Uint8Array(tokens))
          .and.callFake(async () => {
            const message = UnitTestUtils.makeFakeWebSocketMessage(data);
            setTimeout(() => {
              fakeAdbSocket.onmessage!(message);
              fakeAdbSocket.onclose!(new CloseEvent(''));
            }, 50);
          });
      });
      return syncStream;
    });
  }

  function resetListener() {
    listener.onAvailableTracesChange.calls.reset();
    listener.onError.calls.reset();
    listener.onConnectionStateChange.calls.reset();
  }
});

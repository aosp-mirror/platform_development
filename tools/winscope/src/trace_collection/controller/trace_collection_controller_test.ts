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

import {ProgressListener} from 'messaging/progress_listener';
import {ProxyTracingWarnings} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {AdbDeviceState} from 'trace_collection/adb/adb_device_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {MockAdbHostConnection} from 'trace_collection/mock/mock_adb_host_connection';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';
import {UserRequest} from 'trace_collection/user_request';
import {PerfettoSessionModerator} from './perfetto_session_moderator';
import {TraceCollectionController} from './trace_collection_controller';
import {TracingSession} from './tracing_session';
import {WINSCOPE_BACKUP_DIR} from './winscope_backup_dir';

describe('TraceCollectionController', () => {
  const listener = jasmine.createSpyObj<
    ConnectionStateListener & ProgressListener
  >('ConnectionStateListener', [
    'onAvailableTracesChange',
    'onDevicesChange',
    'onError',
    'onConnectionStateChange',
    'onProgressUpdate',
    'onOperationFinished',
  ]);

  const mockDevice = new MockAdbDeviceConnection(
    '35562',
    'Pixel 6',
    AdbDeviceState.AVAILABLE,
    listener,
  );
  const mockUserRequest: UserRequest = {
    target: UiTraceTarget.WINDOW_MANAGER_TRACE,
    config: [],
  };

  let controller: TraceCollectionController;
  let restartSpy: jasmine.Spy;
  let moveSpy: jasmine.Spy;
  let runShellCmdSpy: jasmine.Spy;

  beforeEach(() => {
    restartSpy = spyOn(
      MockAdbHostConnection.prototype,
      'restart',
    ).and.callThrough();
    moveSpy = spyOn(TracingSession.prototype, 'moveFiles');
    runShellCmdSpy = spyOn(mockDevice, 'runShellCommand');
    controller = new TraceCollectionController(
      AdbConnectionType.MOCK,
      listener,
    );
    resetListener();
  });

  describe('initialization and destruction:', () => {
    let hostDestroySpy: jasmine.Spy;
    let securityTokenSpy: jasmine.Spy;

    beforeEach(() => {
      hostDestroySpy = spyOn(MockAdbHostConnection.prototype, 'onDestroy');
      securityTokenSpy = spyOn(
        MockAdbHostConnection.prototype,
        'setSecurityToken',
      );
    });

    it('exposes connection type', () => {
      expect(controller.getConnectionType()).toEqual(AdbConnectionType.MOCK);
    });

    it('restarts host connection', async () => {
      await controller.restartConnection();
      expect(restartSpy).toHaveBeenCalledTimes(1);
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.CONNECTING,
      ]);
    });

    it('sets security token', () => {
      controller.setSecurityToken('12345');
      expect(securityTokenSpy).toHaveBeenCalledOnceWith('12345');
    });

    it('requests devices', async () => {
      await controller.requestDevices();
      expect(listener.onDevicesChange).toHaveBeenCalledTimes(1);
      expect(listener.onConnectionStateChange.calls.mostRecent().args).toEqual([
        ConnectionState.IDLE,
      ]);
    });

    it('cancels device requests', async () => {
      const spy = spyOn(
        MockAdbHostConnection.prototype,
        'cancelDeviceRequests',
      );
      controller.cancelDeviceRequests();
      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('destroys adb session and host on destroy', async () => {
      runShellCmdSpy.and.returnValue('');
      await controller.startTrace(mockDevice, [mockUserRequest]);
      const spies = [
        spyOn(TracingSession.prototype, 'onDestroy'),
        hostDestroySpy,
      ];
      await controller.onDestroy(mockDevice);
      spies.forEach((spy) => expect(spy).toHaveBeenCalledTimes(1));
    });
  });

  describe('starts traces:', () => {
    let startSpy: jasmine.Spy<(target: TraceTarget) => Promise<void>>;
    let userNotifierChecker: UserNotifierChecker;

    beforeEach(async () => {
      startSpy = spyOn(mockDevice, 'startTrace');
      runShellCmdSpy.and.returnValue('');
      userNotifierChecker = new UserNotifierChecker();
    });

    it('restarts connection if no traces requested', async () => {
      await controller.startTrace(mockDevice, []);
      expect(restartSpy).toHaveBeenCalledTimes(1);
      expect(startSpy).not.toHaveBeenCalled();
      userNotifierChecker.expectNotified([
        new ProxyTracingWarnings([
          'None of the requested targets are available on this device.',
        ]),
      ]);
    });

    it('starts legacy traces', async () => {
      const target = new TraceTarget('WmLegacyTrace', [], '', '', [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['wm_trace.winscope', 'wm_trace.pb'],
          'window_trace',
        ),
      ]);
      await checkTracingSessionsStarted(
        [mockUserRequest, mockUserRequest],
        [target, target],
      );
    });

    it('starts perfetto traces', async () => {
      runShellCmdSpy
        .withArgs('perfetto --query')
        .and.returnValue('android.windowmanager');
      await checkTracingSessionsStarted(
        [mockUserRequest],
        [
          new TraceTarget('PerfettoTrace', [], '', '', [
            new AdbFileIdentifier(
              '/data/misc/perfetto-traces/winscope-proxy-trace.perfetto-trace',
              [],
              'trace.perfetto-trace',
            ),
          ]),
        ],
      );
    });

    async function checkTracingSessionsStarted(
      requests: UserRequest[],
      targets: TraceTarget[],
    ) {
      startSpy.calls.reset();
      const stopCurrentSession = spyOn(
        PerfettoSessionModerator.prototype,
        'tryStopCurrentPerfettoSession',
      );
      const clearPreviousConfigFiles = spyOn(
        PerfettoSessionModerator.prototype,
        'clearPreviousConfigFiles',
      );

      await controller.startTrace(mockDevice, requests);

      expect(stopCurrentSession).toHaveBeenCalledTimes(1);
      expect(clearPreviousConfigFiles).toHaveBeenCalledTimes(1);
      expect(runShellCmdSpy.calls.allArgs().slice(1, 3).flat()).toEqual([
        `su root rm -rf ${WINSCOPE_BACKUP_DIR}`,
        `su root mkdir ${WINSCOPE_BACKUP_DIR}`,
      ]);
      startSpy.calls.allArgs().forEach((args, index) => {
        expect(args[0].traceName).toEqual(targets[index].traceName);
        expect(args[0].fileIdentifiers).toEqual(targets[index].fileIdentifiers);
      });
      userNotifierChecker.expectNone();
    }
  });

  describe('ends traces:', () => {
    it('ends tracing controller', async () => {
      const endSpy = spyOn(TracingSession.prototype, 'stop');
      runShellCmdSpy.and.returnValue('');
      await controller.startTrace(mockDevice, [
        mockUserRequest,
        mockUserRequest,
      ]);
      await controller.endTrace(mockDevice);
      expect(endSpy).toHaveBeenCalledTimes(2);
      expect(moveSpy).toHaveBeenCalledTimes(2);
      expect(listener.onProgressUpdate).toHaveBeenCalledTimes(4);
      expect(listener.onOperationFinished).toHaveBeenCalledTimes(1);
    });
  });

  describe('dumps state:', () => {
    let userNotifierChecker: UserNotifierChecker;

    beforeEach(async () => {
      runShellCmdSpy.and.returnValue('');
      userNotifierChecker = new UserNotifierChecker();
    });

    it('restarts connection if no dumps requested', async () => {
      await controller.dumpState(mockDevice, []);
      expect(restartSpy).toHaveBeenCalledTimes(1);
      expect(runShellCmdSpy).not.toHaveBeenCalled();
      userNotifierChecker.expectNotified([
        new ProxyTracingWarnings([
          'None of the requested targets are available on this device.',
        ]),
      ]);
    });

    it('dumps legacy states', async () => {
      runShellCmdSpy.calls.reset();

      const expectedCommands = [
        'su root cmd window tracing frame',
        'su root cmd window tracing level debug',
        'su root cmd window tracing size 16000',
        'su root cmd window tracing start\necho "WM trace (legacy) started."',
        'su root cmd window tracing frame',
        'su root cmd window tracing level debug',
        'su root cmd window tracing size 16000',
        'su root cmd window tracing start\necho "WM trace (legacy) started."',
      ];

      await checkDump([mockUserRequest, mockUserRequest], expectedCommands);
    });

    it('dumps perfetto states', async () => {
      runShellCmdSpy
        .withArgs('perfetto --query')
        .and.returnValue('android.surfaceflinger.layers');

      const req = [
        {
          target: UiTraceTarget.SURFACE_FLINGER_DUMP,
          config: [],
        },
      ];
      const expectedCommands = [
        `cat << EOF >> /data/misc/perfetto-configs/winscope-proxy-dump.conf
data_sources: {
  config {
    name: "android.surfaceflinger.layers"
    surfaceflinger_layers_config: {
      mode: MODE_DUMP
      trace_flags: TRACE_FLAG_INPUT
      trace_flags: TRACE_FLAG_COMPOSITION
      trace_flags: TRACE_FLAG_HWC
      trace_flags: TRACE_FLAG_BUFFERS
      trace_flags: TRACE_FLAG_VIRTUAL_DISPLAYS
    }
  }
}
EOF`,
        `cat << EOF >> /data/misc/perfetto-configs/winscope-proxy-dump.conf
buffers: {
  size_kb: 500000
  fill_policy: RING_BUFFER
}
duration_ms: 1
EOF
rm -f /data/misc/perfetto-traces/winscope-proxy-dump.perfetto-trace
perfetto --out /data/misc/perfetto-traces/winscope-proxy-dump.perfetto-trace --txt --config /data/misc/perfetto-configs/winscope-proxy-dump.conf
echo 'Dumped perfetto'`,
      ];
      await checkDump(req, expectedCommands);
    });

    async function checkDump(req: UserRequest[], commands: string[]) {
      const stopCurrentSession = spyOn(
        PerfettoSessionModerator.prototype,
        'tryStopCurrentPerfettoSession',
      );
      const clearPreviousConfigFiles = spyOn(
        PerfettoSessionModerator.prototype,
        'clearPreviousConfigFiles',
      );

      await controller.dumpState(mockDevice, req);

      expect(stopCurrentSession).toHaveBeenCalledTimes(1);
      expect(clearPreviousConfigFiles).toHaveBeenCalledTimes(1);

      const expectedCommands = [
        'perfetto --query',
        `su root rm -rf ${WINSCOPE_BACKUP_DIR}`,
        `su root mkdir ${WINSCOPE_BACKUP_DIR}`,
      ].concat(commands);
      runShellCmdSpy.calls.allArgs().forEach((args, index) => {
        expect(args[0]).toEqual(expectedCommands[index]);
      });
      userNotifierChecker.expectNone();
    }
  });

  describe('fetches data:', () => {
    const data = Uint8Array.from([]);
    const devicePath = 'archive/test_path';
    const fetchedPath = 'test_path';
    let findSpy: jasmine.Spy;
    let pullSpy: jasmine.Spy;

    beforeEach(async () => {
      findSpy = spyOn(
        MockAdbDeviceConnection.prototype,
        'findFiles',
      ).and.returnValue(Promise.resolve([devicePath, devicePath]));
      pullSpy = spyOn(
        MockAdbDeviceConnection.prototype,
        'pullFile',
      ).and.returnValue(Promise.resolve(data));
    });

    it('fetches last tracing session data', async () => {
      expect(await controller.fetchLastSessionData(mockDevice)).toEqual([
        new File([data], fetchedPath),
        new File([data], fetchedPath),
      ]);
      expect(listener.onProgressUpdate).toHaveBeenCalledTimes(2);
      expect(listener.onOperationFinished).toHaveBeenCalledTimes(1);
    });

    it('does not keep data from last fetch', async () => {
      await controller.fetchLastSessionData(mockDevice);
      expect(await controller.fetchLastSessionData(mockDevice)).toEqual([
        new File([data], fetchedPath),
        new File([data], fetchedPath),
      ]);
    });
  });

  function resetListener() {
    listener.onAvailableTracesChange.calls.reset();
    listener.onDevicesChange.calls.reset();
    listener.onError.calls.reset();
    listener.onConnectionStateChange.calls.reset();
    listener.onProgressUpdate.calls.reset();
    listener.onOperationFinished.calls.reset();
  }
});

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

import {AdbDeviceState} from 'trace_collection/adb/adb_device_connection';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';
import {UserRequest, UserRequestConfig} from 'trace_collection/user_request';
import {
  PerfettoSessionModerator,
  PERFETTO_DUMP_CONFIG_FILE,
  PERFETTO_TRACE_CONFIG_FILE,
} from './perfetto_session_moderator';
import {TracingSession} from './tracing_session';
import {UserRequestParser} from './user_request_parser';

describe('UserRequestParser', () => {
  const mockDevice = new MockAdbDeviceConnection(
    '35562',
    'Pixel 6',
    AdbDeviceState.AVAILABLE,
    jasmine.createSpyObj<ConnectionStateListener>('', ['onDevicesChange']),
  );
  const moderator = new PerfettoSessionModerator(mockDevice, false);
  const expectedSfLegacyTarget = new TraceTarget(
    'SfLegacyTrace',
    [
      'su root service call SurfaceFlinger 1029 i32 16000$',
      'su root service call SurfaceFlinger 1033 i32 0',
    ],
    'su root service call SurfaceFlinger 1025 i32 1\necho "SF layers trace (legacy) started."',
    'su root service call SurfaceFlinger 1025 i32 0 >/dev/null 2>&1\necho "SF layers trace (legacy) stopped."',
    [
      new AdbFileIdentifier(
        '/data/misc/wmtrace/',
        ['layers_trace.winscope', 'layers_trace.pb'],
        'layers_trace',
      ),
    ],
  );
  const expectedSfLegacySession = new TracingSession(expectedSfLegacyTarget);
  const expectedWmPerfettoSetupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.windowmanager"
    windowmanager_config: {
      log_level: LOG_LEVEL_DEBUG
      log_frequency: LOG_FREQUENCY_FRAME
    }
  }
}
EOF`;
  let isTooManySessions: jasmine.Spy;

  beforeEach(() => {
    isTooManySessions = spyOn(moderator, 'isTooManySessions');
    isTooManySessions.and.returnValue(Promise.resolve(false));
  });

  it('makes legacy session due to too many perfetto sessions', async () => {
    const req: UserRequest[] = [
      {target: UiTraceTarget.SURFACE_FLINGER_TRACE, config: []},
    ];
    spyOn(moderator, 'isDataSourceAvailable').and.returnValue(
      Promise.resolve(true),
    );
    isTooManySessions.and.returnValue(Promise.resolve(true));
    expect(await parseRequests(req)).toEqual([expectedSfLegacySession]);
  });

  it('makes perfetto session with multiple setup commands', async () => {
    const req: UserRequest[] = [
      {target: UiTraceTarget.SURFACE_FLINGER_TRACE, config: []},
      {target: UiTraceTarget.WINDOW_MANAGER_TRACE, config: []},
    ];
    spyOn(moderator, 'isDataSourceAvailable').and.returnValue(
      Promise.resolve(true),
    );
    expect(await parseRequests(req)).toEqual([
      moderator.createTracingSession([
        `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.surfaceflinger.layers"
    surfaceflinger_layers_config: {
      mode: MODE_ACTIVE
    }
  }
}
EOF`,
        expectedWmPerfettoSetupCommand,
      ]),
    ]);
  });

  it('makes multiple legacy sessions', async () => {
    const req: UserRequest[] = [
      {target: UiTraceTarget.SURFACE_FLINGER_TRACE, config: []},
      {target: UiTraceTarget.SURFACE_FLINGER_TRACE, config: []},
    ];
    spyOn(moderator, 'isDataSourceAvailable').and.returnValue(
      Promise.resolve(false),
    );
    expect(await parseRequests(req)).toEqual([
      expectedSfLegacySession,
      expectedSfLegacySession,
    ]);
  });

  it('makes combination of perfetto and legacy sessions', async () => {
    const req: UserRequest[] = [
      {target: UiTraceTarget.WINDOW_MANAGER_TRACE, config: []},
      {target: UiTraceTarget.SURFACE_FLINGER_TRACE, config: []},
    ];
    const dsSpy = spyOn(moderator, 'isDataSourceAvailable');
    dsSpy
      .withArgs('android.surfaceflinger.layers')
      .and.returnValue(Promise.resolve(false));
    dsSpy
      .withArgs('android.windowmanager')
      .and.returnValue(Promise.resolve(true));
    expect(await parseRequests(req)).toEqual([
      expectedSfLegacySession,
      moderator.createTracingSession([expectedWmPerfettoSetupCommand]),
    ]);
  });

  describe('makes SF trace perfetto session', () => {
    const expectedSfSetupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.surfaceflinger.layers"
    surfaceflinger_layers_config: {
      mode: MODE_ACTIVE
    }
  }
}
EOF`;

    it('without config', async () => {
      await checkPerfettoSessionCreated(
        expectedSfSetupCommand,
        'android.surfaceflinger.layers',
        UiTraceTarget.SURFACE_FLINGER_TRACE,
      );
    });

    it('with invalid config', async () => {
      await checkPerfettoSessionCreated(
        expectedSfSetupCommand,
        'android.surfaceflinger.layers',
        UiTraceTarget.SURFACE_FLINGER_TRACE,
        [{key: 'invalid', value: '123'}],
      );
    });

    it('with flags', async () => {
      const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.surfaceflinger.layers"
    surfaceflinger_layers_config: {
      mode: MODE_ACTIVE
      trace_flags: TRACE_FLAG_INPUT
      trace_flags: TRACE_FLAG_HWC
      trace_flags: TRACE_FLAG_VIRTUAL_DISPLAYS
    }
  }
}
EOF`;
      await checkPerfettoSessionCreated(
        setupCommand,
        'android.surfaceflinger.layers',
        UiTraceTarget.SURFACE_FLINGER_TRACE,
        [{key: 'input'}, {key: 'hwc'}, {key: 'virtualdisplays'}],
      );
    });
  });

  describe('makes WM trace perfetto session', () => {
    it('without config', async () => {
      await checkPerfettoSessionCreated(
        expectedWmPerfettoSetupCommand,
        'android.windowmanager',
        UiTraceTarget.WINDOW_MANAGER_TRACE,
      );
    });

    it('with invalid config', async () => {
      await checkPerfettoSessionCreated(
        expectedWmPerfettoSetupCommand,
        'android.windowmanager',
        UiTraceTarget.WINDOW_MANAGER_TRACE,
        [{key: 'invalid', value: '123'}],
      );
    });

    it('with log level and frequency', async () => {
      const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.windowmanager"
    windowmanager_config: {
      log_level: LOG_LEVEL_CRITICAL
      log_frequency: LOG_FREQUENCY_TRANSACTION
    }
  }
}
EOF`;
      await checkPerfettoSessionCreated(
        setupCommand,
        'android.windowmanager',
        UiTraceTarget.WINDOW_MANAGER_TRACE,
        [
          {key: 'tracinglevel', value: 'critical'},
          {key: 'tracingtype', value: 'transaction'},
        ],
      );
    });
  });

  it('makes VC perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.viewcapture"
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.viewcapture',
      UiTraceTarget.VIEW_CAPTURE,
    );
  });

  it('makes transactions perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.surfaceflinger.transactions"
    surfaceflinger_transactions_config: {
      mode: MODE_ACTIVE
    }
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.surfaceflinger.transactions',
      UiTraceTarget.TRANSACTIONS,
    );
  });

  it('makes protolog perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.protolog"
    protolog_config: {
      tracing_mode: ENABLE_ALL
    }
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.protolog',
      UiTraceTarget.PROTO_LOG,
    );
  });

  it('makes IME perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.inputmethod"
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.inputmethod',
      UiTraceTarget.IME,
    );
  });

  it('makes transitions perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "com.android.wm.shell.transition"
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'com.android.wm.shell.transition',
      UiTraceTarget.TRANSITIONS,
    );
  });

  it('makes input perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "android.input.inputevent"
    android_input_event_config {
      mode: TRACE_MODE_TRACE_ALL
    }
  }
}
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.input.inputevent',
      UiTraceTarget.INPUT,
    );
  });

  it('makes SF dump perfetto session', async () => {
    const setupCommand = `cat << EOF >> ${PERFETTO_DUMP_CONFIG_FILE}
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
EOF`;
    await checkPerfettoSessionCreated(
      setupCommand,
      'android.surfaceflinger.layers',
      UiTraceTarget.SURFACE_FLINGER_DUMP,
      [],
      new PerfettoSessionModerator(mockDevice, true),
    );
  });

  describe('makes SF trace legacy session', () => {
    it('without config', async () => {
      await checkSession(expectedSfLegacyTarget, []);
    });

    it('with invalid config', async () => {
      const config = [{key: 'invalid', value: '123'}];
      await checkSession(expectedSfLegacyTarget, config);
    });

    it('with flags', async () => {
      const config = [
        {key: 'composition'},
        {key: 'metadata'},
        {key: 'tracebuffers'},
      ];
      const expectedTarget = new TraceTarget(
        expectedSfLegacyTarget.traceName,
        [
          'su root service call SurfaceFlinger 1029 i32 16000$',
          'su root service call SurfaceFlinger 1033 i32 44',
        ],
        expectedSfLegacyTarget.startCmd,
        expectedSfLegacyTarget.stopCmd,
        expectedSfLegacyTarget.fileIdentifiers,
      );
      await checkSession(expectedTarget, config);
    });

    it('with buffer size', async () => {
      const config = [{key: 'sfbuffersize', value: '32000'}];
      const expectedTarget = new TraceTarget(
        expectedSfLegacyTarget.traceName,
        [
          'su root service call SurfaceFlinger 1029 i32 32000$',
          'su root service call SurfaceFlinger 1033 i32 0',
        ],
        expectedSfLegacyTarget.startCmd,
        expectedSfLegacyTarget.stopCmd,
        expectedSfLegacyTarget.fileIdentifiers,
      );
      await checkSession(expectedTarget, config);
    });

    async function checkSession(
      target: TraceTarget,
      config: UserRequestConfig[],
    ) {
      await checkLegacySessionCreated(
        UiTraceTarget.SURFACE_FLINGER_TRACE,
        target,
        config,
      );
    }
  });

  describe('makes WM trace legacy session', () => {
    const expectedTarget = new TraceTarget(
      'WmLegacyTrace',
      [
        'su root cmd window tracing frame',
        'su root cmd window tracing level debug',
        'su root cmd window tracing size 16000',
      ],
      'su root cmd window tracing start\necho "WM trace (legacy) started."',
      'su root cmd window tracing stop\necho "WM trace (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['wm_trace.winscope', 'wm_trace.pb'],
          'window_trace',
        ),
      ],
    );

    it('without config', async () => {
      await checkSession(expectedTarget, []);
    });

    it('with invalid config', async () => {
      await checkSession(expectedTarget, [{key: 'invalid', value: '123'}]);
    });

    it('with frequency', async () => {
      const target = new TraceTarget(
        expectedTarget.traceName,
        [
          'su root cmd window tracing transaction',
          'su root cmd window tracing level debug',
          'su root cmd window tracing size 16000',
        ],
        expectedTarget.startCmd,
        expectedTarget.stopCmd,
        expectedTarget.fileIdentifiers,
      );
      await checkSession(target, [{key: 'tracingtype', value: 'transaction'}]);
    });

    it('with log level', async () => {
      const target = new TraceTarget(
        expectedTarget.traceName,
        [
          'su root cmd window tracing frame',
          'su root cmd window tracing level critical',
          'su root cmd window tracing size 16000',
        ],
        expectedTarget.startCmd,
        expectedTarget.stopCmd,
        expectedTarget.fileIdentifiers,
      );
      await checkSession(target, [{key: 'tracinglevel', value: 'critical'}]);
    });

    it('with buffer size', async () => {
      const target = new TraceTarget(
        expectedTarget.traceName,
        [
          'su root cmd window tracing frame',
          'su root cmd window tracing level debug',
          'su root cmd window tracing size 32000',
        ],
        expectedTarget.startCmd,
        expectedTarget.stopCmd,
        expectedTarget.fileIdentifiers,
      );
      await checkSession(target, [{key: 'wmbuffersize', value: '32000'}]);
    });

    async function checkSession(
      target: TraceTarget,
      config: UserRequestConfig[],
    ) {
      await checkLegacySessionCreated(
        UiTraceTarget.WINDOW_MANAGER_TRACE,
        target,
        config,
      );
    }
  });

  it('makes VC legacy session', async () => {
    const target = new TraceTarget(
      'VcLegacy',
      [],
      'su root settings put global view_capture_enabled 1\necho "ViewCapture tracing (legacy) started."',
      'su root sh -c "cmd launcherapps dump-view-hierarchies >/data/misc/wmtrace/view_capture_trace.zip"' +
        '\nsu root settings put global view_capture_enabled 0\necho "ViewCapture tracing (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['view_capture_trace.zip'],
          'view_capture_trace.zip',
        ),
      ],
    );
    await checkLegacySessionCreated(UiTraceTarget.VIEW_CAPTURE, target);
  });

  it('makes transactions legacy session', async () => {
    const target = new TraceTarget(
      'TransactionsLegacy',
      [],
      'su root service call SurfaceFlinger 1041 i32 1\necho "SF transactions trace (legacy) started."',
      'su root service call SurfaceFlinger 1041 i32 0 >/dev/null 2>&1\necho "SF transactions trace (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['transactions_trace.winscope', 'transactions_trace.pb'],
          'transactions',
        ),
      ],
    );
    await checkLegacySessionCreated(UiTraceTarget.TRANSACTIONS, target);
  });

  it('makes protolog legacy session', async () => {
    const target = new TraceTarget(
      'ProtologLegacy',
      [],
      'su root cmd window logging start\necho "ProtoLog (legacy) started."',
      'su root cmd window logging stop >/dev/null 2>&1\necho "ProtoLog (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['wm_log.winscope', 'wm_log.pb'],
          'proto_log',
        ),
      ],
    );
    await checkLegacySessionCreated(UiTraceTarget.PROTO_LOG, target);
  });

  it('makes IME legacy session', async () => {
    const target = new TraceTarget(
      'ImeLegacy',
      [],
      'su root ime tracing start\necho "IME tracing (legacy) started."',
      'su root ime tracing stop >/dev/null 2>&1\necho "IME tracing (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['ime_trace_clients.winscope', 'ime_trace_clients.pb'],
          'ime_trace_clients',
        ),
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['ime_trace_service.winscope', 'ime_trace_service.pb'],
          'ime_trace_service',
        ),
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['ime_trace_managerservice.winscope', 'ime_trace_managerservice.pb'],
          'ime_trace_managerservice',
        ),
      ],
    );
    await checkLegacySessionCreated(UiTraceTarget.IME, target);
  });

  it('makes transitions legacy session', async () => {
    const target = new TraceTarget(
      'TransitionsLegacy',
      [],
      'su root cmd window shell tracing start && su root dumpsys activity service SystemUIService WMShell transitions tracing start' +
        '\necho "Transition traces (legacy) started."',
      'su root cmd window shell tracing stop && su root dumpsys activity service SystemUIService WMShell transitions tracing stop >/dev/null 2>&1' +
        '\n echo "Transition traces (legacy) stopped."',
      [
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['wm_transition_trace.winscope', 'wm_transition_trace.pb'],
          'wm_transition_trace',
        ),
        new AdbFileIdentifier(
          '/data/misc/wmtrace/',
          ['shell_transition_trace.winscope', 'shell_transition_trace.pb'],
          'shell_transition_trace',
        ),
      ],
    );
    await checkLegacySessionCreated(UiTraceTarget.TRANSITIONS, target);
  });

  describe('makes screen recording sessions', () => {
    const expectedTargetActive = new TraceTarget(
      'ScreenRecordingactive',
      [
        'settings put system show_touches 0 && settings put system pointer_location 0',
      ],
      `
      screenrecord --bugreport --bit-rate 8M /data/local/tmp/screen_active.mp4 & \
      echo "ScreenRecorder started."
      `,
      'settings put system pointer_location 0 && \
      settings put system show_touches 0 && \
      pkill -l SIGINT screenrecord >/dev/null 2>&1',
      [
        new AdbFileIdentifier(
          `/data/local/tmp/screen_active.mp4`,
          [],
          `screen_recording_active`,
        ),
      ],
      true,
    );
    const expectedSessionActive = new TracingSession(expectedTargetActive);

    const expectedSessionD1 = new TracingSession(
      new TraceTarget(
        'ScreenRecordingd1',
        expectedTargetActive.setupCmds,
        `
      screenrecord --bugreport --bit-rate 8M --display-id d1 /data/local/tmp/screen_d1.mp4 & \
      echo "ScreenRecorder started."
      `,
        expectedTargetActive.stopCmd,
        [
          new AdbFileIdentifier(
            `/data/local/tmp/screen_d1.mp4`,
            [],
            `screen_recording_d1`,
          ),
        ],
        true,
      ),
    );

    it('without config', async () => {
      const req = getRequest([]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with invalid config', async () => {
      const req = getRequest([{key: 'invalid'}]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with pointer and touches', async () => {
      const req = getRequest([{key: 'pointer_and_touches'}]);
      const expectedTargetWithPointerAndTouches = new TraceTarget(
        expectedTargetActive.traceName,
        [
          'settings put system show_touches 1 && settings put system pointer_location 1',
        ],
        expectedTargetActive.startCmd,
        expectedTargetActive.stopCmd,
        expectedTargetActive.fileIdentifiers,
        true,
      );
      expect(await parseRequests(req)).toEqual([
        new TracingSession(expectedTargetWithPointerAndTouches),
      ]);
    });

    it('with empty displays string value', async () => {
      const req = getRequest([{key: 'displays', value: ''}]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with empty displays array value', async () => {
      const req = getRequest([{key: 'displays', value: []}]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with single display specified', async () => {
      const req = getRequest([{key: 'displays', value: 'd1'}]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with multiple displays specified', async () => {
      const req = getRequest([{key: 'displays', value: ['d1', 'd2']}]);
      const expectedTargetd2 = new TraceTarget(
        'ScreenRecordingd2',
        [],
        `
      screenrecord --bugreport --bit-rate 8M --display-id d2 /data/local/tmp/screen_d2.mp4 & \
      echo "ScreenRecorder started."
      `,
        expectedTargetActive.stopCmd,
        [
          new AdbFileIdentifier(
            `/data/local/tmp/screen_d2.mp4`,
            [],
            `screen_recording_d2`,
          ),
        ],
        true,
      );
      expect(await parseRequests(req)).toEqual([
        expectedSessionD1,
        new TracingSession(expectedTargetd2),
      ]);
    });

    it('with extra info after display', async () => {
      const req = getRequest([{key: 'displays', value: 'd1 Other Info'}]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with extra info after display in array', async () => {
      const req = getRequest([{key: 'displays', value: ['d1 Other Info']}]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with extra info before display', async () => {
      const req = getRequest([
        {key: 'displays', value: '"Test Display" d1 Other Info'},
      ]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with extra info before display in array', async () => {
      const req = getRequest([
        {key: 'displays', value: ['"Test Display" d1 Other Info']},
      ]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    function getRequest(config: UserRequestConfig[]): UserRequest[] {
      return [{target: UiTraceTarget.SCREEN_RECORDING, config}];
    }
  });

  it('makes wayland session', async () => {
    const req = [{target: UiTraceTarget.WAYLAND, config: []}];
    expect(await parseRequests(req)).toEqual([
      new TracingSession(
        new TraceTarget(
          'Wayland',
          [],
          'su root service call Wayland 26 i32 1 >/dev/null\necho "Wayland trace started."',
          'su root service call Wayland 26 i32 0 >/dev/null\necho "Wayland trace ended."',
          [
            new AdbFileIdentifier(
              '/data/misc/wltrace',
              ['wl_trace.winscope', 'wl_trace.pb'],
              'wl_trace',
            ),
          ],
        ),
      ),
    ]);
  });

  it('makes eventlog session', async () => {
    const startTimeSeconds = 123000;
    spyOn(Date, 'now').and.returnValue(startTimeSeconds);
    const req = [{target: UiTraceTarget.EVENTLOG, config: []}];
    expect(await parseRequests(req)).toEqual([
      new TracingSession(
        new TraceTarget(
          'Eventlog',
          [],
          'rm -f /data/local/tmp/eventlog.winscope' +
            '\n echo "EventLog started."',
          'echo "EventLog\\n" > /data/local/tmp/eventlog.winscope ' +
            `&& su root logcat -b events -v threadtime -v printable -v uid -v nsec -v epoch -b events -t 123 >> /data/local/tmp/eventlog.winscope`,
          [
            new AdbFileIdentifier(
              '/data/local/tmp',
              ['eventlog.winscope', 'eventlog.pb'],
              'eventlog',
            ),
          ],
        ),
      ),
    ]);
  });

  it('makes SF dump legacy session', async () => {
    const req = [{target: UiTraceTarget.SURFACE_FLINGER_DUMP, config: []}];
    expect(await parseRequests(req)).toEqual([
      new TracingSession(
        new TraceTarget(
          'SfDumpLegacy',
          [],
          `su root dumpsys SurfaceFlinger --proto > /data/local/tmp/sf_dump.winscope`,
          '',
          [
            new AdbFileIdentifier(
              `/data/local/tmp/sf_dump.winscope`,
              [],
              'layers_dump',
            ),
          ],
        ),
      ),
    ]);
  });

  it('makes WM dump legacy session', async () => {
    const req = [{target: UiTraceTarget.WINDOW_MANAGER_DUMP, config: []}];
    expect(await parseRequests(req)).toEqual([
      new TracingSession(
        new TraceTarget(
          'WmDumpLegacy',
          [],
          `su root dumpsys window --proto > /data/local/tmp/wm_dump.winscope`,
          '',
          [
            new AdbFileIdentifier(
              `/data/local/tmp/wm_dump.winscope`,
              [],
              'window_dump',
            ),
          ],
        ),
      ),
    ]);
  });

  describe('makes screenshot session', () => {
    const expectedTargetActive = new TraceTarget(
      'Screenshotactive',
      [],
      `screencap -p > /data/local/tmp/screenshot_active.png`,
      '',
      [
        new AdbFileIdentifier(
          `/data/local/tmp/screenshot_active.png`,
          [],
          `screenshot_active.png`,
        ),
      ],
    );
    const expectedSessionActive = new TracingSession(expectedTargetActive);
    const expectedSessionD1 = new TracingSession(
      new TraceTarget(
        'Screenshotd1',
        expectedTargetActive.setupCmds,
        `screencap -p -d d1 > /data/local/tmp/screenshot_d1.png`,
        expectedTargetActive.stopCmd,
        [
          new AdbFileIdentifier(
            `/data/local/tmp/screenshot_d1.png`,
            [],
            `screenshot_d1.png`,
          ),
        ],
      ),
    );

    it('without config', async () => {
      const req = getRequest([]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with invalid config', async () => {
      const req = getRequest([{key: 'invalid'}]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with empty displays', async () => {
      const req = getRequest([{key: 'displays', value: []}]);
      expect(await parseRequests(req)).toEqual([expectedSessionActive]);
    });

    it('with single display specified', async () => {
      const req = getRequest([{key: 'displays', value: ['d1']}]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with multiple displays specified', async () => {
      const req = getRequest([{key: 'displays', value: ['d1', 'd2']}]);
      const expectedTargetd2 = new TraceTarget(
        'Screenshotd2',
        [],
        `screencap -p -d d2 > /data/local/tmp/screenshot_d2.png`,
        expectedTargetActive.stopCmd,
        [
          new AdbFileIdentifier(
            `/data/local/tmp/screenshot_d2.png`,
            [],
            `screenshot_d2.png`,
          ),
        ],
      );
      expect(await parseRequests(req)).toEqual([
        expectedSessionD1,
        new TracingSession(expectedTargetd2),
      ]);
    });

    it('with extra info after display', async () => {
      const req = getRequest([{key: 'displays', value: ['d1 Other Info']}]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    it('with extra info before display', async () => {
      const req = getRequest([
        {key: 'displays', value: ['"Test Display" d1 Other Info']},
      ]);
      expect(await parseRequests(req)).toEqual([expectedSessionD1]);
    });

    function getRequest(config: UserRequestConfig[]): UserRequest[] {
      return [{target: UiTraceTarget.SCREENSHOT, config}];
    }
  });

  async function parseRequests(
    req: UserRequest[],
    perfettoModerator = moderator,
  ): Promise<TracingSession[]> {
    return await new UserRequestParser()
      .setPerfettoModerator(perfettoModerator)
      .setRequests(req)
      .parse();
  }

  async function checkPerfettoSessionCreated(
    setupCmd: string,
    ds: string,
    target: UiTraceTarget,
    config: UserRequestConfig[] = [],
    perfettoModerator = moderator,
  ) {
    spyOn(perfettoModerator, 'isDataSourceAvailable')
      .withArgs(ds)
      .and.returnValue(Promise.resolve(true));
    const req: UserRequest[] = [{target, config}];
    expect(await parseRequests(req, perfettoModerator)).toEqual([
      perfettoModerator.createTracingSession([setupCmd]),
    ]);
  }

  async function checkLegacySessionCreated(
    uiTarget: UiTraceTarget,
    target: TraceTarget,
    config: UserRequestConfig[] = [],
  ) {
    const req = [{target: uiTarget, config}];
    expect(await parseRequests(req)).toEqual([new TracingSession(target)]);
  }
});

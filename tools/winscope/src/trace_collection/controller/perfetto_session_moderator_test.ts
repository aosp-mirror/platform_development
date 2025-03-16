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

import {ProxyTracingWarnings} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {AdbDeviceState} from 'trace_collection/adb/adb_device_connection';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {
  PerfettoSessionModerator,
  PERFETTO_DUMP_CONFIG_FILE,
  PERFETTO_DUMP_FILE,
  PERFETTO_TRACE_CONFIG_FILE,
  PERFETTO_TRACE_FILE,
  PERFETTO_TRACING_SESSIONS_END,
  PERFETTO_TRACING_SESSIONS_START,
  PERFETTO_UNIQUE_SESSION_NAME,
} from './perfetto_session_moderator';
import {TracingSession} from './tracing_session';

describe('PerfettoSessionModerator', () => {
  const mockDevice = new MockAdbDeviceConnection(
    '35562',
    'Pixel 6',
    AdbDeviceState.AVAILABLE,
    jasmine.createSpyObj<ConnectionStateListener>('', ['onDevicesChange']),
  );
  let runShellCmdSpy: jasmine.Spy;

  beforeEach(() => {
    runShellCmdSpy = spyOn(mockDevice, 'runShellCommand');
  });

  describe('clearPreviousConfigFiles', () => {
    it('trace', async () => {
      const moderator = new PerfettoSessionModerator(mockDevice, false);
      await moderator.clearPreviousConfigFiles();
      expect(runShellCmdSpy).toHaveBeenCalledOnceWith(
        `su root rm -f ${PERFETTO_TRACE_CONFIG_FILE}`,
      );
    });

    it('dump', async () => {
      const moderator = new PerfettoSessionModerator(mockDevice, true);
      await moderator.clearPreviousConfigFiles();
      expect(runShellCmdSpy).toHaveBeenCalledOnceWith(
        `su root rm -f ${PERFETTO_DUMP_CONFIG_FILE}`,
      );
    });
  });

  describe('isTooManySessions', () => {
    let moderator: PerfettoSessionModerator;
    let userNotifierChecker: UserNotifierChecker;

    beforeEach(() => {
      moderator = new PerfettoSessionModerator(mockDevice, false);
      userNotifierChecker = new UserNotifierChecker();
    });

    it('handles within limit', async () => {
      setQueryResp(PERFETTO_TRACING_SESSIONS_START + 'session1');
      await checkNotTooManySessions();
    });

    it('handles within limit (end query message present)', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          'session1' +
          PERFETTO_TRACING_SESSIONS_END,
      );
      expect(await moderator.isTooManySessions()).toBeFalse();
    });

    it('handles within limit with existing session', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          `session1\nsession2\n${PERFETTO_UNIQUE_SESSION_NAME}\nsession4\nsession5`,
      );
      await checkNotTooManySessions();
    });

    it('handles within limit with existing session (end query message present)', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          `session1\nsession2\n${PERFETTO_UNIQUE_SESSION_NAME}\nsession4\nsession5` +
          PERFETTO_TRACING_SESSIONS_END,
      );
      await checkNotTooManySessions();
    });

    it('warns if above limit', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          'session1\nsession2\nsession3\nsession4\nsession5',
      );
      await checkTooManySessions();
    });

    it('warns if above limit (end query message present)', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          'session1\nsession2\nsession3\nsession4\nsession5' +
          PERFETTO_TRACING_SESSIONS_END,
      );
      await checkTooManySessions();
    });
    it('warns if above limit with existing session', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          `session1\nsession2\nsession3\n${PERFETTO_UNIQUE_SESSION_NAME}\nsession4\nsession5`,
      );
      await checkTooManySessions();
    });
    it('warns if above limit with existion session (end query message present)', async () => {
      setQueryResp(
        PERFETTO_TRACING_SESSIONS_START +
          `session1\nsession2\nsession3\n${PERFETTO_UNIQUE_SESSION_NAME}\nsession4\nsession5` +
          PERFETTO_TRACING_SESSIONS_END,
      );
      await checkTooManySessions();
    });

    function setQueryResp(resp: string) {
      runShellCmdSpy.withArgs('perfetto --query').and.returnValue(resp);
    }

    async function checkNotTooManySessions() {
      expect(await moderator.isTooManySessions()).toBeFalse();
      userNotifierChecker.expectNone();
    }

    async function checkTooManySessions() {
      expect(await moderator.isTooManySessions()).toBeTrue();
      userNotifierChecker.expectNotified([
        new ProxyTracingWarnings([
          'Limit of 5 Perfetto sessions reached on device. Will attempt to collect legacy traces.',
        ]),
      ]);
    }
  });

  describe('tryStopCurrentPerfettoSession', () => {
    let moderator: PerfettoSessionModerator;

    beforeEach(() => {
      moderator = new PerfettoSessionModerator(mockDevice, false);
    });

    it('handles no existing session', async () => {
      runShellCmdSpy.and.returnValue('');
      await moderator.tryStopCurrentPerfettoSession();
      expect(runShellCmdSpy.calls.allArgs().flat()).toEqual([
        'perfetto --query',
      ]);
    });

    it('stops existing session only once', async () => {
      runShellCmdSpy.and.returnValue('');
      runShellCmdSpy
        .withArgs('perfetto --query')
        .and.returnValue(
          PERFETTO_TRACING_SESSIONS_START + PERFETTO_UNIQUE_SESSION_NAME,
        );
      await moderator.tryStopCurrentPerfettoSession();
      await moderator.tryStopCurrentPerfettoSession();
      expect(runShellCmdSpy.calls.allArgs().flat()).toEqual([
        'perfetto --query',
        'perfetto --attach=WINSCOPE-PROXY-TRACING-SESSION --stop',
      ]);
    });

    it('stops existing session (end query message present)', async () => {
      runShellCmdSpy.and.returnValue('');
      runShellCmdSpy
        .withArgs('perfetto --query')
        .and.returnValue(
          PERFETTO_TRACING_SESSIONS_START +
            PERFETTO_UNIQUE_SESSION_NAME +
            PERFETTO_TRACING_SESSIONS_END,
        );
      await moderator.tryStopCurrentPerfettoSession();
      expect(runShellCmdSpy.calls.allArgs().flat()).toEqual([
        'perfetto --query',
        'perfetto --attach=WINSCOPE-PROXY-TRACING-SESSION --stop',
      ]);
    });
  });

  describe('isDataSourceAvailable', () => {
    let moderator: PerfettoSessionModerator;

    beforeEach(() => {
      moderator = new PerfettoSessionModerator(mockDevice, false);
    });

    it('true if available', async () => {
      runShellCmdSpy.withArgs('perfetto --query').and.returnValue('ds1');
      expect(await moderator.isDataSourceAvailable('ds1')).toEqual(true);
    });

    it('false if not available', async () => {
      runShellCmdSpy.withArgs('perfetto --query').and.returnValue('');
      expect(await moderator.isDataSourceAvailable('ds1')).toEqual(false);
    });
  });

  describe('createTracingSession', () => {
    it('trace', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, false);
      const session = moderator.createTracingSession(['setup1']);
      const expectedTarget = new TraceTarget(
        'PerfettoTrace',
        ['setup1'],
        `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
buffers: {
  size_kb: 500000
  fill_policy: RING_BUFFER
}
duration_ms: 0
file_write_period_ms: 999999999
write_into_file: true
unique_session_name: "${PERFETTO_UNIQUE_SESSION_NAME}"
EOF
rm -f ${PERFETTO_TRACE_FILE}
perfetto --out ${PERFETTO_TRACE_FILE} --txt --config ${PERFETTO_TRACE_CONFIG_FILE} --detach=WINSCOPE-PROXY-TRACING-SESSION
echo 'Perfetto trace started.'`,
        `perfetto --attach=WINSCOPE-PROXY-TRACING-SESSION --stop
echo 'Perfetto trace stopped.'`,
        [
          new AdbFileIdentifier(
            PERFETTO_TRACE_FILE,
            [],
            'trace.perfetto-trace',
          ),
        ],
      );
      expect(session).toEqual(new TracingSession(expectedTarget));
    });

    it('dump', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, true);
      const session = moderator.createTracingSession(['setup1']);
      const expectedTarget = new TraceTarget(
        'PerfettoDump',
        ['setup1'],
        `cat << EOF >> ${PERFETTO_DUMP_CONFIG_FILE}
buffers: {
  size_kb: 500000
  fill_policy: RING_BUFFER
}
duration_ms: 1
EOF
rm -f ${PERFETTO_DUMP_FILE}
perfetto --out ${PERFETTO_DUMP_FILE} --txt --config ${PERFETTO_DUMP_CONFIG_FILE}
echo 'Dumped perfetto'`,
        '',
        [new AdbFileIdentifier(PERFETTO_DUMP_FILE, [], 'dump.perfetto-trace')],
      );
      expect(session).toEqual(new TracingSession(expectedTarget));
    });
  });

  describe('createSetupCommand', () => {
    it('trace', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, false);
      expect(moderator.createSetupCommand('ds1')).toEqual(
        `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "ds1"
  }
}
EOF`,
      );
    });

    it('trace with config', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, false);
      expect(moderator.createSetupCommand('ds1', 'extraconfig {}')).toEqual(
        `cat << EOF >> ${PERFETTO_TRACE_CONFIG_FILE}
data_sources: {
  config {
    name: "ds1"
    extraconfig {}
  }
}
EOF`,
      );
    });

    it('dump', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, true);
      expect(moderator.createSetupCommand('ds1')).toEqual(
        `cat << EOF >> ${PERFETTO_DUMP_CONFIG_FILE}
data_sources: {
  config {
    name: "ds1"
  }
}
EOF`,
      );
    });

    it('dump with config', () => {
      const moderator = new PerfettoSessionModerator(mockDevice, true);
      expect(moderator.createSetupCommand('ds1', 'extraconfig {}')).toEqual(
        `cat << EOF >> ${PERFETTO_DUMP_CONFIG_FILE}
data_sources: {
  config {
    name: "ds1"
    extraconfig {}
  }
}
EOF`,
      );
    });
  });
});

/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {UserNotifier} from 'common/user_notifier';
import {ProxyTracingWarnings} from 'messaging/user_warnings';
import {AdbDeviceConnection} from 'trace_collection/adb/adb_device_connection';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {TracingSession} from './tracing_session';

// Perfetto files
export const PERFETTO_TRACE_FILE =
  '/data/misc/perfetto-traces/winscope-proxy-trace.perfetto-trace';
export const PERFETTO_DUMP_FILE =
  '/data/misc/perfetto-traces/winscope-proxy-dump.perfetto-trace';
export const PERFETTO_TRACE_CONFIG_FILE =
  '/data/misc/perfetto-configs/winscope-proxy-trace.conf';
export const PERFETTO_DUMP_CONFIG_FILE =
  '/data/misc/perfetto-configs/winscope-proxy-dump.conf';
export const PERFETTO_UNIQUE_SESSION_NAME = 'winscope proxy perfetto tracing';

// Perfetto query helpers
export const PERFETTO_TRACING_SESSIONS_START = `TRACING SESSIONS:

ID      UID     STATE      BUF (#) KB   DUR (s)   #DS  STARTED  NAME
===     ===     =====      ==========   =======   ===  =======  ====\n`;
export const PERFETTO_TRACING_SESSIONS_END =
  '\nNOTE: Some tracing sessions are not reported in the list above.';

export class PerfettoSessionModerator {
  private queryResult: string | undefined;
  private prevSessionActive = false;
  private concurrentSessions: number | undefined;
  private configFilepath: string;

  constructor(private device: AdbDeviceConnection, private isDump: boolean) {
    this.configFilepath = isDump
      ? PERFETTO_DUMP_CONFIG_FILE
      : PERFETTO_TRACE_CONFIG_FILE;
  }

  async clearPreviousConfigFiles() {
    console.debug('Clearing perfetto config file for previous tracing session');
    await this.device.runShellCommand(`su root rm -f ${this.configFilepath}`);
    console.debug('Cleared perfetto config file for previous tracing session');
  }

  async isTooManySessions() {
    if (this.concurrentSessions === undefined) {
      this.concurrentSessions = await this.getConcurrentSessions();
    }
    const tooManyPerfettoSessions = this.concurrentSessions >= 5;
    if (tooManyPerfettoSessions) {
      const warning =
        'Limit of 5 Perfetto sessions reached on device. Will attempt to collect legacy traces.';
      UserNotifier.add(new ProxyTracingWarnings([warning])).notify();
    }
    return tooManyPerfettoSessions;
  }

  async tryStopCurrentPerfettoSession() {
    if (this.concurrentSessions === undefined) {
      this.concurrentSessions = await this.getConcurrentSessions();
    }
    if (!this.prevSessionActive) {
      return;
    }
    console.debug('Stopping already-running winscope perfetto session.');
    await this.device?.runShellCommand(
      'perfetto --attach=WINSCOPE-PROXY-TRACING-SESSION --stop',
    );
    this.prevSessionActive = false;
    console.debug('Stopped already-running winscope perfetto session.');
  }

  async isDataSourceAvailable(ds: string): Promise<boolean> {
    const queryResult = await this.getQueryResult();
    return queryResult.includes(ds);
  }

  createTracingSession(setupCommands: string[]): TracingSession {
    if (this.isDump) {
      return new TracingSession(this.makePerfettoDumpTarget(setupCommands));
    } else {
      return new TracingSession(this.makePerfettoTraceTarget(setupCommands));
    }
  }

  createSetupCommand(ds: string, config?: string): string {
    const spacer = '\n    ';
    return `cat << EOF >> ${this.configFilepath}
data_sources: {
  config {
    name: "${ds}"${config ? spacer + config : ''}
  }
}
EOF`;
  }

  private makePerfettoDumpTarget(setupCommands: string[]) {
    return new TraceTarget(
      'PerfettoDump',
      setupCommands,
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
  }

  private makePerfettoTraceTarget(setupCommands: string[]) {
    return new TraceTarget(
      'PerfettoTrace',
      setupCommands,
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
      [new AdbFileIdentifier(PERFETTO_TRACE_FILE, [], 'trace.perfetto-trace')],
    );
  }

  private async getConcurrentSessions(): Promise<number> {
    const queryRes = await this.getQueryResult();
    const startIndex = queryRes.indexOf(PERFETTO_TRACING_SESSIONS_START);
    let numberOfConcurrentSessions = 0;
    if (startIndex !== -1) {
      let concurrentSessions = queryRes.slice(startIndex);
      console.debug(`Concurrent sessions:\n${concurrentSessions}`);
      concurrentSessions = concurrentSessions.slice(
        PERFETTO_TRACING_SESSIONS_START.length,
      );

      const endIndex = concurrentSessions.indexOf(
        PERFETTO_TRACING_SESSIONS_END,
      );
      if (endIndex !== -1) {
        concurrentSessions = concurrentSessions.slice(0, endIndex);
      }

      numberOfConcurrentSessions =
        concurrentSessions.length > 0
          ? concurrentSessions.trim().split('\n').length
          : 0;

      if (concurrentSessions.includes(PERFETTO_UNIQUE_SESSION_NAME)) {
        this.prevSessionActive = true;
        numberOfConcurrentSessions -= 1;
      }
    }
    return numberOfConcurrentSessions;
  }

  private async getQueryResult() {
    if (this.queryResult === undefined) {
      this.queryResult = await this.device.runShellCommand('perfetto --query');
    }
    return this.queryResult;
  }
}

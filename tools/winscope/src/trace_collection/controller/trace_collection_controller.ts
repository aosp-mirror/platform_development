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

import {FileUtils} from 'common/file_utils';
import {TimeUtils} from 'common/time/time_utils';
import {UserNotifier} from 'common/user_notifier';
import {ProgressListener} from 'messaging/progress_listener';
import {ProxyTracingWarnings} from 'messaging/user_warnings';
import {AdbDeviceConnection} from 'trace_collection/adb/adb_device_connection';
import {AdbHostConnection} from 'trace_collection/adb/adb_host_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {MockAdbHostConnection} from 'trace_collection/mock/mock_adb_host_connection';
import {UserRequest} from 'trace_collection/user_request';
import {WdpHostConnection} from 'trace_collection/wdp/wdp_host_connection';
import {WinscopeProxyHostConnection} from 'trace_collection/winscope_proxy/winscope_proxy_host_connection';
import {PerfettoSessionModerator} from './perfetto_session_moderator';
import {TracingSession} from './tracing_session';
import {UserRequestParser} from './user_request_parser';
import {WINSCOPE_BACKUP_DIR} from './winscope_backup_dir';

export class TraceCollectionController {
  private activeTracingSessions: TracingSession[] = [];
  private host: AdbHostConnection;

  constructor(
    connectionType: string,
    private listener: ConnectionStateListener & ProgressListener,
  ) {
    if (connectionType === AdbConnectionType.WDP) {
      this.host = new WdpHostConnection(listener);
    } else if (connectionType === AdbConnectionType.MOCK) {
      this.host = new MockAdbHostConnection(listener);
    } else {
      this.host = new WinscopeProxyHostConnection(listener);
    }
  }

  getConnectionType(): AdbConnectionType {
    return this.host.connectionType;
  }

  async restartConnection(): Promise<void> {
    await this.host.restart();
  }

  setSecurityToken(token: string) {
    this.host.setSecurityToken(token);
  }

  getDevices(): AdbDeviceConnection[] {
    return this.host.getDevices();
  }

  cancelDeviceRequests() {
    this.host.cancelDeviceRequests();
  }

  async requestDevices() {
    this.host.requestDevices();
  }

  async onDestroy(device: AdbDeviceConnection) {
    for (const session of this.activeTracingSessions) {
      await session.onDestroy(device);
    }
    this.host.onDestroy();
  }

  async startTrace(
    device: AdbDeviceConnection,
    requestedTraces: UserRequest[],
  ): Promise<void> {
    const perfettoModerator = new PerfettoSessionModerator(device, false);
    const sessions = await this.getSessions(perfettoModerator, requestedTraces);
    this.activeTracingSessions = [];
    if (sessions.length === 0) {
      return;
    }
    await this.prepareDevice(device, perfettoModerator);
    for (const session of sessions) {
      await session.start(device);
      this.activeTracingSessions.push(session);
    }
    // TODO(b/330118129): identify source of additional start latency that affects some traces
    await TimeUtils.sleepMs(1000); // 1s timeout ensures SR fully started
  }

  async endTrace(device: AdbDeviceConnection) {
    for (const [index, session] of this.activeTracingSessions.entries()) {
      await session.stop(device);
      this.listener.onProgressUpdate(
        'Ending trace...',
        (100 * index) / this.activeTracingSessions.length,
      );
    }
    await this.moveFiles(device, this.activeTracingSessions);
    this.activeTracingSessions = [];
    this.listener.onOperationFinished(true);
  }

  async dumpState(
    device: AdbDeviceConnection,
    requestedDumps: UserRequest[],
  ): Promise<void> {
    const perfettoModerator = new PerfettoSessionModerator(device, true);
    const sessions = await this.getSessions(perfettoModerator, requestedDumps);
    if (sessions.length === 0) {
      return;
    }
    await this.prepareDevice(device, perfettoModerator);
    for (const [index, session] of sessions.entries()) {
      await session.dump(device);
      this.listener.onProgressUpdate(
        'Dumping state...',
        (100 * index) / this.activeTracingSessions.length,
      );
    }
    await this.moveFiles(device, sessions);
    this.listener.onOperationFinished(true);
  }

  async fetchLastSessionData(device: AdbDeviceConnection): Promise<File[]> {
    const adbData: File[] = [];
    const paths = await device.findFiles(`${WINSCOPE_BACKUP_DIR}*`, []);
    for (const [index, filepath] of paths.entries()) {
      console.debug(`Fetching file ${filepath} from device`);
      const data = await device.pullFile(filepath);
      const filename = FileUtils.removeDirFromFileName(filepath);
      adbData.push(new File([data], filename));
      this.listener.onProgressUpdate(
        'Fetching files...',
        (100 * index) / paths.length,
      );
      console.debug(`Fetched ${filepath}`);
    }
    this.listener.onOperationFinished(true);
    return adbData;
  }

  private async getSessions(
    perfettoModerator: PerfettoSessionModerator,
    req: UserRequest[],
  ): Promise<TracingSession[]> {
    const sessions = await new UserRequestParser()
      .setPerfettoModerator(perfettoModerator)
      .setRequests(req)
      .parse();

    if (sessions.length === 0) {
      UserNotifier.add(
        new ProxyTracingWarnings([
          'None of the requested targets are available on this device.',
        ]),
      ).notify();
      await this.host.restart();
      return [];
    }
    return sessions;
  }

  private async prepareDevice(
    device: AdbDeviceConnection,
    perfettoModerator: PerfettoSessionModerator,
  ) {
    await perfettoModerator.tryStopCurrentPerfettoSession();
    await perfettoModerator.clearPreviousConfigFiles();
    console.debug('Clearing previous tracing session files from device');
    await device.runShellCommand(`su root rm -rf ${WINSCOPE_BACKUP_DIR}`);
    await device.runShellCommand(`su root mkdir ${WINSCOPE_BACKUP_DIR}`);
    console.debug('Cleared previous tracing session files from device');
  }

  private async moveFiles(
    device: AdbDeviceConnection,
    sessions: TracingSession[],
  ) {
    for (const [index, session] of sessions.entries()) {
      await session.moveFiles(device);
      this.listener.onProgressUpdate(
        'Moving files...',
        (100 * index) / sessions.length,
      );
    }
  }
}

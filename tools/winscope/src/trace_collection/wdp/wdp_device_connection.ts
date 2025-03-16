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

import {
  byteArrayToString,
  ResizableBuffer,
  stringToByteArray,
} from 'common/buffer_utils';
import {UserNotifier} from 'common/user_notifier';
import {WindowUtils} from 'common/window_utils';
import {
  ProxyTracingErrors,
  ProxyTracingWarnings,
} from 'messaging/user_warnings';
import {
  AdbDeviceConnection,
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {TraceTarget} from 'trace_collection/trace_target';
import {DataListener} from './adb_websocket_stream';
import {ShellStream} from './shell_stream';
import {StreamProvider} from './stream_provider';
import {WdpDeviceConnectionResponse} from './wdp_host_connection';
import {ErrorListener} from './websocket_stream';

export class WdpDeviceConnection extends AdbDeviceConnection {
  private static readonly WDP_ADB_URL = 'ws://localhost:9167/adb-json';
  private authorizeDevicePopup = false;
  private streamProvider = new StreamProvider();
  private screenRecordingStreams = new Map<string, ShellStream>();

  constructor(
    id: string,
    listener: AdbDeviceConnectionListener,
    private approveUrl?: string,
  ) {
    super(id, listener);
  }

  override onDestroy() {
    this.streamProvider.closeAllStreams();
  }

  override async tryAuthorize(): Promise<void> {
    if (this.approveUrl) {
      const popup = WindowUtils.showPopupWindow(this.approveUrl);
      if (!popup) {
        await this.listener.onError(`Please enable popups and try again.`);
        this.authorizeDevicePopup = false;
      } else {
        this.authorizeDevicePopup = true;
      }
    }
  }

  override async runShellCommand(cmd: string): Promise<string> {
    const cmdOut = new ResizableBuffer();
    const dataListener = (data: Uint8Array) => {
      cmdOut.append(data);
    };
    const errorListener = async (msg: string) => {
      this.listener.onError(msg);
    };
    const stream = this.createShellStream(dataListener, errorListener);
    await stream.connect(cmd);
    await stream.complete;
    const output = byteArrayToString(cmdOut.get()).trimEnd();
    this.streamProvider.removeStream(stream);
    return output;
  }

  override async startTrace(target: TraceTarget): Promise<void> {
    console.debug(`Starting trace for ${target.traceName} on ${this.id}`);
    if (target.isScreenRecording) {
      await this.startScreenRecording(target);
    } else {
      const output = await this.runShellCommand(target.startCmd);
      const doneToken = 'started.';
      const index = output.indexOf(doneToken);
      let warning: string | undefined;
      if (index === -1) {
        warning = output;
      } else {
        const msg = output.slice(index + doneToken.length);
        warning = msg.length > 0 ? msg.trim() : undefined;
      }
      if (warning) {
        UserNotifier.add(new ProxyTracingWarnings([warning])).notify();
      }
    }
    console.debug(`Started trace for ${target.traceName} on ${this.id}`);
  }

  override async endTrace(target: TraceTarget) {
    if (target.isScreenRecording) {
      const stream = this.screenRecordingStreams.get(target.traceName);
      await stream?.write(ESC_CHAR_VINTR);
      stream?.close();
      this.screenRecordingStreams.delete(target.traceName);
    }
    console.debug(`Ending trace for ${target.traceName}.`);
    const output = await this.runShellCommand(target.stopCmd);
    console.debug(`Ended trace for ${target.traceName}. Output: ${output}`);
  }

  protected override async updatePropertiesFromResponse(
    devJson: WdpDeviceConnectionResponse,
  ) {
    if (devJson.adbProps) {
      this.model = devJson.adbProps['model']?.replace('_', ' ') ?? 'unknown';
    }
    if (devJson.proxyStatus !== 'PROXY_UNAUTHORIZED') {
      this.authorizeDevicePopup = false;
    }
    this.approveUrl = devJson.approveUrl;
    if (devJson.proxyStatus === 'PROXY_UNAUTHORIZED') {
      this.state = AdbDeviceState.UNAUTHORIZED;
      if (devJson.approveUrl && !this.authorizeDevicePopup) {
        await this.tryAuthorize();
        return;
      }
    }
    if (devJson.adbStatus === 'OFFLINE') {
      this.state = AdbDeviceState.OFFLINE;
    }
    if (devJson.adbStatus === 'DEVICE' && devJson.proxyStatus === 'ADB') {
      this.state = AdbDeviceState.AVAILABLE;
    }
  }

  override async pullFile(filepath: string): Promise<Uint8Array> {
    const sock = new WebSocket(WdpDeviceConnection.WDP_ADB_URL);
    const stream = this.streamProvider.createSyncStream(
      this.id,
      sock,
      async (msg: string) => {
        console.error(msg);
        await this.listener.onError(msg);
      },
    );
    await stream.connect();
    const fileData = await stream.pullFile(filepath);
    this.streamProvider.removeStream(stream);
    return fileData;
  }

  private async startScreenRecording(target: TraceTarget) {
    const cmdOut = new ResizableBuffer();
    const dataListener = (data: Uint8Array) => {
      cmdOut.append(data);
    };
    const stream = this.createShellStream(dataListener);
    this.screenRecordingStreams.set(target.traceName, stream);
    stream.complete.then(() => {
      const stdout = byteArrayToString(cmdOut.get());
      const index = stdout.indexOf('ERROR');
      if (index === -1) {
        return;
      }
      let output =
        'Error ending screen recording on device: ' + stdout.slice(index);
      output = output.replace(
        'please check your display state',
        'please check your display state (must be on at start of trace)',
      );
      UserNotifier.add(new ProxyTracingErrors([output])).notify();
    });
    await stream.connect();
    await stream.write(stringToByteArray(target.startCmd));
  }

  private createShellStream(
    dataListener: DataListener,
    errorListener: ErrorListener = async (msg: string) =>
      this.listener.onError(msg),
  ): ShellStream {
    const sock = new WebSocket(WdpDeviceConnection.WDP_ADB_URL);
    return this.streamProvider.createShellStream(
      this.id,
      sock,
      dataListener,
      async (msg: string) => {
        console.error(msg);
        errorListener(msg);
      },
    );
  }
}

const ESC_CHAR_VINTR = new Uint8Array([0x03]);

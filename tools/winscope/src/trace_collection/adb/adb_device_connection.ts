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
import {ConnectionState} from 'trace_collection/connection_state';
import {TraceTarget} from 'trace_collection/trace_target';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';

export interface AdbDeviceConnectionListener {
  onError(errorText: string): Promise<void>;
  onConnectionStateChange(newState: ConnectionState): Promise<void>;
  onAvailableTracesChange(
    newTraces: UiTraceTarget[],
    removedTraces: UiTraceTarget[],
  ): void;
}

export abstract class AdbDeviceConnection {
  private static readonly MULTI_DISPLAY_SCREENRECORD_VERSION = '1.4';
  protected state = AdbDeviceState.OFFLINE;
  protected model = '';
  protected displays: string[] = [];
  protected multiDisplayScreenRecording = false;

  constructor(
    readonly id: string,
    protected listener: AdbDeviceConnectionListener,
  ) {}

  getState() {
    return this.state;
  }

  hasMultiDisplayScreenRecording(): boolean {
    return this.multiDisplayScreenRecording;
  }

  getDisplays() {
    return this.displays;
  }

  getFormattedName(): string {
    let status = '';
    if (this.state === AdbDeviceState.OFFLINE) {
      status = 'offline';
    } else if (this.state === AdbDeviceState.UNAUTHORIZED) {
      status = 'unauthorized';
    }
    if (status && this.model) {
      status += ' ';
    }
    return `${status}${this.model} (${this.id})`;
  }

  async checkRoot(): Promise<boolean> {
    const root = await this.runShellCommand('su root id -u');
    const isRoot = Number(root) === 0;
    if (!isRoot) {
      UserNotifier.add(
        new ProxyTracingWarnings([
          'Unable to acquire root privileges on the device - ' +
            `check the output of 'adb -s ${this.id} shell su root id'`,
        ]),
      ).notify();
    }
    return isRoot;
  }

  async updateAvailableTraces() {
    if (
      this.state === AdbDeviceState.AVAILABLE &&
      (await this.isWaylandAvailable())
    ) {
      this.listener.onAvailableTracesChange([UiTraceTarget.WAYLAND], []);
    } else {
      this.listener.onAvailableTracesChange([], [UiTraceTarget.WAYLAND]);
    }
  }

  async updateProperties(resp: object) {
    this.updatePropertiesFromResponse(resp);
    await this.updateDisplaysInformation();
  }

  async findFiles(path: string, matchers: string[]): Promise<string[]> {
    if (matchers.length === 0) {
      matchers.push('');
    }
    for (const matcher of matchers) {
      let matchingFiles: string;
      if (matcher.length > 0) {
        matchingFiles = await this.runShellCommand(
          `su root find ${path} -name ${matcher}`,
        );
      } else {
        matchingFiles = await this.runShellCommand(`su root find ${path}`);
      }
      const files = matchingFiles
        .split('\n')
        .filter(
          (file) => !file.includes('No such file') && file.trim().length > 0,
        );
      if (files.length > 0) {
        return files;
      }
    }
    return [];
  }

  private async updateDisplaysInformation() {
    let screenRecordVersion = '0';
    if (this.state === AdbDeviceState.AVAILABLE) {
      try {
        const output = await this.runShellCommand('screenrecord --version');
        if (!output.includes('unrecognized option')) {
          screenRecordVersion = output;
        } else {
          const helpText = await this.runShellCommand('screenrecord --help');
          const versionStartIndex = helpText.indexOf('v') + 1;
          screenRecordVersion = helpText.slice(
            versionStartIndex,
            versionStartIndex + 3,
          );
        }
      } catch (e) {
        // swallow
        console.error(e);
      }
    }
    this.multiDisplayScreenRecording =
      screenRecordVersion >=
      AdbDeviceConnection.MULTI_DISPLAY_SCREENRECORD_VERSION;

    if (this.state === AdbDeviceState.AVAILABLE) {
      const output = await this.runShellCommand(
        'su root dumpsys SurfaceFlinger --display-id',
      );
      if (!output.includes('Display')) {
        this.displays = [];
      } else {
        this.displays = output
          .trim()
          .split('\n')
          .map((display) => {
            const parts = display.split(' ').slice(1);
            const displayNameStartIndex = parts.findIndex((part) =>
              part.includes('displayName'),
            );
            if (displayNameStartIndex !== -1) {
              const displayName = parts
                .slice(displayNameStartIndex)
                .join(' ')
                .slice(12);
              if (displayName.length > 2) {
                return [displayName]
                  .concat(parts.slice(0, displayNameStartIndex))
                  .join(' ');
              }
            }
            return parts.join(' ');
          });
      }
    } else {
      this.displays = [];
    }
  }

  private async isWaylandAvailable(): Promise<boolean> {
    const serviceCheck = await this.runShellCommand('service check Wayland');
    return !serviceCheck.includes('not found');
  }

  abstract tryAuthorize(): Promise<void>;
  abstract onDestroy(): void;
  abstract runShellCommand(cmd: string): Promise<string>;
  abstract startTrace(target: TraceTarget): Promise<void>;
  abstract endTrace(target: TraceTarget): Promise<void>;
  abstract pullFile(filepath: string): Promise<Uint8Array>;
  protected abstract updatePropertiesFromResponse(resp: object): void;
}

export enum AdbDeviceState {
  OFFLINE,
  UNAUTHORIZED,
  AVAILABLE,
}

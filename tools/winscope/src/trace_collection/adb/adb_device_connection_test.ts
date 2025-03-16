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
import {
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';

describe('AdbDeviceConnection', () => {
  const listener = jasmine.createSpyObj<AdbDeviceConnectionListener>(
    'AdbDeviceConnectionListener',
    ['onAvailableTracesChange'],
  );

  const testId = '35562';
  const testModel = 'Pixel';
  let connection: MockAdbDeviceConnection;
  let runShellCmdSpy: jasmine.Spy;
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(() => {
    userNotifierChecker = new UserNotifierChecker();
  });

  beforeEach(() => {
    connection = new MockAdbDeviceConnection(
      testId,
      testModel,
      AdbDeviceState.AVAILABLE,
      listener,
    );
    runShellCmdSpy = spyOn(connection, 'runShellCommand').and.returnValue(
      Promise.resolve(''),
    );
    listener.onAvailableTracesChange.calls.reset();
    userNotifierChecker.reset();
  });

  afterEach(() => {
    userNotifierChecker.expectNone();
  });

  it('formats name for offline device', () => {
    const connection = new MockAdbDeviceConnection(
      testId,
      testModel,
      AdbDeviceState.OFFLINE,
      listener,
    );
    expect(connection.getFormattedName()).toEqual('offline Pixel (35562)');
  });

  it('formats name for unauthorized device', () => {
    const connection = new MockAdbDeviceConnection(
      testId,
      testModel,
      AdbDeviceState.UNAUTHORIZED,
      listener,
    );
    expect(connection.getFormattedName()).toEqual('unauthorized Pixel (35562)');
  });

  it('formats name for idle device', () => {
    expect(connection.getFormattedName()).toEqual('Pixel (35562)');
  });

  it('checks root success', async () => {
    runShellCmdSpy.withArgs('su root id -u').and.returnValue('0');
    expect(await connection.checkRoot()).toBeTrue();
  });

  it('checks root failure', async () => {
    runShellCmdSpy.withArgs('su root id -u').and.returnValue('1');
    expect(await connection.checkRoot()).toBeFalse();
    userNotifierChecker.expectNotified([
      new ProxyTracingWarnings([
        'Unable to acquire root privileges on the device - ' +
          `check the output of 'adb -s 35562 shell su root id'`,
      ]),
    ]);
    userNotifierChecker.reset();
  });

  it('updates availability of wayland trace if available', async () => {
    await connection.updateAvailableTraces();
    expect(listener.onAvailableTracesChange).toHaveBeenCalledOnceWith(
      [UiTraceTarget.WAYLAND],
      [],
    );
  });

  it('updates availability of traces if not available', async () => {
    runShellCmdSpy
      .withArgs('service check Wayland')
      .and.returnValue('not found');
    await connection.updateAvailableTraces();
    expect(listener.onAvailableTracesChange).toHaveBeenCalledOnceWith(
      [],
      [UiTraceTarget.WAYLAND],
    );
  });

  it('updates hasMultiDisplayScreenRecording via screenrecord --version - old version', async () => {
    runShellCmdSpy.withArgs('screenrecord --version').and.returnValue('1.3');
    await connection.updateProperties({});
    expect(connection.hasMultiDisplayScreenRecording()).toBeFalse();
  });

  it('updates hasMultiDisplayScreenRecording via screenrecord --version - compatible version', async () => {
    runShellCmdSpy.withArgs('screenrecord --version').and.returnValue('1.4');
    await connection.updateProperties({});
    expect(connection.hasMultiDisplayScreenRecording()).toBeTrue();
  });

  it('updates hasMultiDisplayScreenRecording via screenrecord --help - old version', async () => {
    runShellCmdSpy
      .withArgs('screenrecord --version')
      .and.returnValue('unrecognized option');
    runShellCmdSpy.withArgs('screenrecord --help').and.returnValue('v1.3');
    await connection.updateProperties({});
    expect(connection.hasMultiDisplayScreenRecording()).toBeFalse();
  });

  it('updates hasMultiDisplayScreenRecording via screenrecord --help - compatible version', async () => {
    runShellCmdSpy
      .withArgs('screenrecord --version')
      .and.returnValue('unrecognized option');
    runShellCmdSpy.withArgs('screenrecord --help').and.returnValue('v1.4');
    await connection.updateProperties({});
    expect(connection.hasMultiDisplayScreenRecording()).toBeTrue();
  });

  it('handles error in screen recording command', async () => {
    runShellCmdSpy
      .withArgs('screenrecord --version')
      .and.throwError(new Error('test error'));
    await expectAsync(connection.updateProperties({})).toBeResolved();
    expect(connection.hasMultiDisplayScreenRecording()).toBeFalse();
  });

  it('adds display', async () => {
    runShellCmdSpy
      .withArgs('su root dumpsys SurfaceFlinger --display-id')
      .and.returnValue('Display 12345 Extra Info displayName="Test Display"');
    await connection.updateProperties({});
    expect(connection.getDisplays()).toEqual([
      '"Test Display" 12345 Extra Info',
    ]);
  });

  it('adds display with missing displayName', async () => {
    runShellCmdSpy
      .withArgs('su root dumpsys SurfaceFlinger --display-id')
      .and.returnValue('Display 12345 Extra Info');
    await connection.updateProperties({});
    expect(connection.getDisplays()).toEqual(['12345 Extra Info']);
  });

  it('clears display', async () => {
    runShellCmdSpy
      .withArgs('su root dumpsys SurfaceFlinger --display-id')
      .and.returnValue('Display 12345 Extra Info');
    await connection.updateProperties({});
    expect(connection.getDisplays().length).toEqual(1);
    runShellCmdSpy
      .withArgs('su root dumpsys SurfaceFlinger --display-id')
      .and.returnValue('');
    await connection.updateProperties({});
    expect(connection.getDisplays().length).toEqual(0);
  });

  it('finds files via exact filepath', async () => {
    runShellCmdSpy.withArgs('su root find filepath').and.returnValue('file');
    expect(await connection.findFiles('filepath', [])).toEqual(['file']);
  });

  it('finds files via first matcher', async () => {
    runShellCmdSpy
      .withArgs('su root find filepath -name m1')
      .and.returnValue('file');
    expect(await connection.findFiles('filepath', ['m1', 'm2'])).toEqual([
      'file',
    ]);
  });

  it('finds files via second matcher', async () => {
    runShellCmdSpy
      .withArgs('su root find filepath -name m2')
      .and.returnValue('file');
    expect(await connection.findFiles('filepath', ['m1', 'm2'])).toEqual([
      'file',
    ]);
  });

  it('handles "No such file" error', async () => {
    runShellCmdSpy
      .withArgs('su root find filepath')
      .and.returnValue('No such file');
    expect(await connection.findFiles('filepath', [])).toEqual([]);
  });

  it('ignores whitespace', async () => {
    runShellCmdSpy
      .withArgs('su root find filepath')
      .and.returnValue('file\n  ');
    expect(await connection.findFiles('filepath', [])).toEqual(['file']);
  });
});

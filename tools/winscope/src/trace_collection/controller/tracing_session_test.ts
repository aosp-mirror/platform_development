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

import {
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {TracingSession} from './tracing_session';
import {WINSCOPE_BACKUP_DIR} from './winscope_backup_dir';

describe('TracingSession', () => {
  const listener: AdbDeviceConnectionListener = {
    onError: jasmine.createSpy(),
    onConnectionStateChange: jasmine.createSpy(),
    onAvailableTracesChange: jasmine.createSpy(),
  };
  const mockDevice = new MockAdbDeviceConnection(
    '35562',
    'Pixel 6',
    AdbDeviceState.AVAILABLE,
    listener,
  );
  const fileIdentifiers = [
    new AdbFileIdentifier('test path 1', ['matcher'], 'saved file 1'),
    new AdbFileIdentifier('test path 2', ['matcher'], 'saved file 2'),
  ];
  const sessionName = 'TestSession';
  const stopCmd = 'test stop cmd';
  const startCmd = 'test start cmd';
  const setupCmds = ['setup1', 'setup2'];
  let session: TracingSession;
  let target: TraceTarget;
  let startTraceSpy: jasmine.Spy;
  let endTraceSpy: jasmine.Spy;
  let moveFilesSpy: jasmine.Spy;
  let runShellCmdSpy: jasmine.Spy;
  let findFilesSpy: jasmine.Spy;

  beforeEach(() => {
    target = new TraceTarget(
      sessionName,
      setupCmds,
      startCmd,
      stopCmd,
      fileIdentifiers,
    );
    session = new TracingSession(target);
    startTraceSpy = spyOn(mockDevice, 'startTrace');
    endTraceSpy = spyOn(mockDevice, 'endTrace');
    runShellCmdSpy = spyOn(mockDevice, 'runShellCommand');
    findFilesSpy = spyOn(mockDevice, 'findFiles').and.returnValue(
      Promise.resolve(['file']),
    );
    moveFilesSpy = spyOn(session, 'moveFiles').and.callThrough();
  });

  it('starts traces', async () => {
    await session.start(mockDevice);
    expect(runShellCmdSpy.calls.allArgs()).toEqual([['setup1'], ['setup2']]);
    expect(startTraceSpy).toHaveBeenCalledOnceWith(target);
  });

  it('stops traces and moves files', async () => {
    await session.stop(mockDevice);
    checkTraceStopSpies(0);
    await session.start(mockDevice);
    await session.stop(mockDevice);
    checkTraceStopSpies(1, [target]);
  });

  it('stops traces on destroy', async () => {
    await session.onDestroy(mockDevice);
    checkTraceStopSpies(0);
    await session.start(mockDevice);
    checkTraceStopSpies(0);
    await session.onDestroy(mockDevice);
    checkTraceStopSpies(1);
  });

  it('dumps states', async () => {
    await session.dump(mockDevice);
    expect(runShellCmdSpy.calls.allArgs()).toEqual([
      ['setup1'],
      ['setup2'],
      [startCmd],
    ]);
  });

  it('moves files', async () => {
    await session.moveFiles(mockDevice);
    expect(findFilesSpy.calls.allArgs()).toEqual([
      ['test path 1', ['matcher']],
      ['test path 2', ['matcher']],
    ]);
    expect(runShellCmdSpy.calls.allArgs()).toEqual([
      [
        `su root [ ! -f file ] || su root mv file ${WINSCOPE_BACKUP_DIR}saved file 1`,
      ],
      [
        `su root [ ! -f file ] || su root mv file ${WINSCOPE_BACKUP_DIR}saved file 2`,
      ],
    ]);
  });

  it('handles error in move command', async () => {
    runShellCmdSpy
      .withArgs(
        `su root [ ! -f file ] || su root mv file ${WINSCOPE_BACKUP_DIR}saved file 1`,
      )
      .and.throwError(new Error());
    await expectAsync(session.moveFiles(mockDevice)).toBeResolved();
  });

  function checkTraceStopSpies(times: number, endArgs?: TraceTarget[]) {
    expect(endTraceSpy).toHaveBeenCalledTimes(times);
    expect(moveFilesSpy).toHaveBeenCalledTimes(times);
    if (endArgs) {
      expect(endTraceSpy).toHaveBeenCalledWith(...endArgs);
    }
  }
});

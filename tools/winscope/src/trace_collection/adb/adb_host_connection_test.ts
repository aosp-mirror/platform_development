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

import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {MockAdbHostConnection} from 'trace_collection/mock/mock_adb_host_connection';

describe('AdbHostConnection', () => {
  const listener = jasmine.createSpyObj<ConnectionStateListener>(
    'ConnectionStateListener',
    [
      'onAvailableTracesChange',
      'onDevicesChange',
      'onError',
      'onConnectionStateChange',
    ],
  );

  let connection: MockAdbHostConnection;

  beforeEach(() => {
    connection = new MockAdbHostConnection(listener);
    resetListener();
  });

  it('initializes extra parameters', () => {
    const spy = spyOn(
      MockAdbHostConnection.prototype,
      'initializeExtraParameters',
    );
    connection = new MockAdbHostConnection(listener);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('calls listener on new state', async () => {
    await connection.restart();
    expect(listener.onConnectionStateChange.calls.allArgs()).toEqual([
      [ConnectionState.CONNECTING],
    ]);
  });

  it('destroys devices and host onDestroy', () => {
    const hostSpy = spyOn(MockAdbHostConnection.prototype, 'destroyHost');
    const deviceSpy = spyOn(connection.devices[0], 'onDestroy');
    connection.onDestroy();
    expect(hostSpy).toHaveBeenCalledTimes(1);
    expect(deviceSpy).toHaveBeenCalledTimes(1);
  });

  function resetListener() {
    listener.onAvailableTracesChange.calls.reset();
    listener.onDevicesChange.calls.reset();
    listener.onError.calls.reset();
    listener.onConnectionStateChange.calls.reset();
  }
});

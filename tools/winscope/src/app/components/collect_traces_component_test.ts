/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {CommonModule} from '@angular/common';
import {
  Component,
  NO_ERRORS_SCHEMA,
  QueryList,
  ViewChildren,
} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDialogModule} from '@angular/material/dialog';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {ProxyTraceTimeout} from 'messaging/user_warnings';
import {NoTraceTargetsSelected, WinscopeEvent} from 'messaging/winscope_event';
import {MockAdbConnection} from 'test/unit/mock_adb_connection';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {TraceType} from 'trace/trace_type';
import {AdbConnection} from 'trace_collection/adb_connection';
import {AdbDevice} from 'trace_collection/adb_device';
import {ConnectionState} from 'trace_collection/connection_state';
import {ProxyConnection} from 'trace_collection/proxy_connection';
import {AdbProxyComponent} from './adb_proxy_component';
import {CollectTracesComponent} from './collect_traces_component';
import {LoadProgressComponent} from './load_progress_component';
import {TraceConfigComponent} from './trace_config_component';
import {WarningDialogComponent} from './warning_dialog_component';
import {WebAdbComponent} from './web_adb_component';

describe('CollectTracesComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
  const mockDevice: AdbDevice = {
    id: '35562',
    model: 'Pixel 6',
    authorized: true,
    displays: [],
    multiDisplayScreenRecordingAvailable: false,
  };
  const mockDeviceWatch: AdbDevice = {
    id: '75432',
    model: 'Pixel Watch',
    authorized: true,
    displays: [],
    multiDisplayScreenRecordingAvailable: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatIconModule,
        MatCardModule,
        MatListModule,
        MatButtonModule,
        MatDividerModule,
        MatProgressBarModule,
        BrowserAnimationsModule,
        MatSnackBarModule,
        MatDialogModule,
        MatCheckboxModule,
        FormsModule,
      ],
      providers: [MatSnackBar],
      declarations: [
        TestHostComponent,
        CollectTracesComponent,
        AdbProxyComponent,
        WebAdbComponent,
        TraceConfigComponent,
        LoadProgressComponent,
        WarningDialogComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders the expected card title', () => {
    const title = assertDefined(htmlElement.querySelector('.title'));
    expect(title.innerHTML).toContain('Collect Traces');
  });

  it('displays connecting message', () => {
    getConnection().state = ConnectionState.CONNECTING;
    fixture.detectChanges();

    const connectingMessage = assertDefined(
      htmlElement.querySelector('.connecting-message'),
    );
    expect(connectingMessage.innerHTML).toContain('Connecting...');
  });

  it('displays adb set up', () => {
    getCollectTracesComponent().adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    fixture.detectChanges();

    const setUpAdbEl = assertDefined(htmlElement.querySelector('.set-up-adb'));
    expect(setUpAdbEl.querySelector('.proxy-tab')).toBeTruthy();
  });

  it('displays no connected devices', () => {
    getConnection().state = ConnectionState.IDLE;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('No devices detected');
  });

  it('displays connected authorized devices', () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    connection.devices = [mockDevice];
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('Pixel 6');
    expect(el.innerHTML).toContain('smartphone');
  });

  it('displays connected unauthorized devices', () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    connection.devices = [
      {
        id: '35562',
        model: 'Pixel 6',
        authorized: false,
        displays: [],
        multiDisplayScreenRecordingAvailable: false,
      },
    ];
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('unauthorized');
    expect(el.innerHTML).toContain('screen_lock_portrait');
  });

  it('detects changes in devices', async () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('No devices detected');

    connection.devices = [mockDevice];
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel 6 (35562)',
    );
  });

  it('displays connected devices again if selected device no longer present', () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    connection.devices = [mockDevice];
    fixture.detectChanges();

    const device = assertDefined(
      htmlElement.querySelector<HTMLElement>('.available-device'),
    );
    device.click();
    fixture.detectChanges();

    connection.devices = [mockDeviceWatch];
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel Watch (75432)',
    );
  });

  it('auto selects last device', () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    connection.devices = [mockDevice];
    fixture.detectChanges();

    const device = assertDefined(
      htmlElement.querySelector<HTMLElement>('.available-device'),
    );
    device.click();
    fixture.detectChanges();
    let configSection = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(configSection.textContent).toContain('Pixel 6');

    connection.devices = [mockDeviceWatch];
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel Watch (75432)',
    );
    expect(htmlElement.querySelector('.trace-collection-config')).toBeNull();

    connection.devices = [mockDevice];
    fixture.detectChanges();
    configSection = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(configSection.textContent).toContain('Pixel 6');
  });

  it('displays trace collection config elements', () => {
    goToConfigSection();

    const el = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(el.innerHTML).toContain('smartphone');
    expect(el.innerHTML).toContain('Pixel 6');
    expect(el.innerHTML).toContain('35562');

    const traceSection = assertDefined(
      htmlElement.querySelector('.trace-section'),
    );
    expect(traceSection.querySelector('trace-config')?.textContent).toContain(
      'Trace targets',
    );
    expect(traceSection.querySelector('.start-btn')?.textContent).toContain(
      'Start trace',
    );

    const dumpSection = assertDefined(
      htmlElement.querySelector('.dump-section'),
    );
    expect(dumpSection.querySelector('trace-config')?.textContent).toContain(
      'Dump targets',
    );
    expect(dumpSection.querySelector('.dump-btn')?.textContent).toContain(
      'Dump state',
    );
  });

  it('updates config on change in trace config component', async () => {
    goToConfigSection();
    await fixture.whenStable();
    fixture.detectChanges();

    clickCheckboxAndCheckTraceConfig('window_trace', false);
    clickCheckboxAndCheckTraceConfig('window_dump', true);
  });

  it('start trace button works as expected', async () => {
    goToConfigSection();

    const spy = spyOn(getConnection(), 'startTrace');
    await clickStartTraceButton();
    expect(spy).toHaveBeenCalled();
  });

  it('emits event if no trace targets selected', async () => {
    goToConfigSection();
    const collectTracesComponent = getCollectTracesComponent();

    let lastEvent: WinscopeEvent | undefined;
    collectTracesComponent.setEmitEvent(async (event: WinscopeEvent) => {
      lastEvent = event;
    });

    Object.values(collectTracesComponent.traceConfig).forEach(
      (c) => (c.enabled = false),
    );
    const spy = spyOn(getConnection(), 'startTrace');
    await clickStartTraceButton();

    expect(lastEvent).toEqual(new NoTraceTargetsSelected());
    expect(spy).not.toHaveBeenCalled();
  });

  it('dump state button works as expected', async () => {
    goToConfigSection();

    const filesSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    await clickDumpStateButton();

    expect(filesSpy).toHaveBeenCalledOnceWith({
      requested: [
        {name: 'Window Manager', types: [TraceType.WINDOW_MANAGER]},
        {name: 'Surface Flinger', types: [TraceType.SURFACE_FLINGER]},
        {name: 'Screenshot', types: [TraceType.SCREENSHOT]},
      ],
      collected: getConnection().files,
    });
  });

  it('emits event if no dump targets selected', async () => {
    goToConfigSection();
    const collectTracesComponent = getCollectTracesComponent();

    let lastEvent: WinscopeEvent | undefined;
    collectTracesComponent.setEmitEvent(async (event: WinscopeEvent) => {
      lastEvent = event;
    });

    Object.values(collectTracesComponent.dumpConfig).forEach(
      (c) => (c.enabled = false),
    );
    const filesSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    await clickDumpStateButton();

    expect(lastEvent).toEqual(new NoTraceTargetsSelected());
    expect(filesSpy).not.toHaveBeenCalled();
  });

  it('does not collect files if dumping fails', async () => {
    goToConfigSection();

    const filesSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    const connection = getConnection();
    spyOn(connection, 'dumpState').and.callFake(async () => {
      connection.state = ConnectionState.ERROR;
    });
    await clickDumpStateButton();

    expect(filesSpy).not.toHaveBeenCalled();
  });

  it('change device button works as expected', () => {
    goToConfigSection();
    expect(getCollectTracesComponent().getSelectedDevice()).toBeDefined();

    const spy = spyOn(getConnection(), 'restartConnection');

    const change = assertDefined(
      htmlElement.querySelector<HTMLElement>('.change-btn'),
    );
    change.click();

    expect(spy).toHaveBeenCalled();
  });

  it('fetch existing traces button emits files and restarts connection if no files found', async () => {
    const connection = getConnection();
    connection.files = [];
    const emitSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    const restartSpy = spyOn(connection, 'restartConnection');
    goToConfigSection();

    const fetchButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.fetch-btn'),
    );

    fetchButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(emitSpy).toHaveBeenCalledOnceWith({
      requested: [],
      collected: [],
    });
    expect(restartSpy).toHaveBeenCalledTimes(1);
  });

  it('fetch existing traces button emits files and does not restart connection if files found', async () => {
    const connection = getConnection();
    const testFile = new File([], 'test_file');
    connection.files = [testFile];
    const emitSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    const restartSpy = spyOn(connection, 'restartConnection');
    goToConfigSection();

    const fetchButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.fetch-btn'),
    );

    fetchButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(emitSpy).toHaveBeenCalledWith({
      requested: [],
      collected: [testFile],
    });
    expect(restartSpy).not.toHaveBeenCalled();
  });

  it('displays unknown error message', () => {
    const connection = getConnection();
    connection.state = ConnectionState.ERROR;
    fixture.detectChanges();

    const testErrorMessage = 'bad things are happening';
    connection.errorText = testErrorMessage;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.unknown-error'));
    expect(el.innerHTML).toContain('Error:');
    expect(el.innerHTML).toContain(testErrorMessage);

    const spy = spyOn(connection, 'restartConnection').and.callThrough();
    const retryButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.retry-btn'),
    );
    retryButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('displays starting trace elements', () => {
    goToConfigSection();
    const connection = getConnection();
    connection.state = ConnectionState.STARTING_TRACE;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.starting-trace'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Starting trace...');

    const endButton = assertDefined(
      el.querySelector<HTMLButtonElement>('.end-btn button'),
    );
    expect(endButton.disabled).toBeTrue();
  });

  it('displays tracing elements and ends trace correctly', async () => {
    goToConfigSection();
    const connection = getConnection();
    connection.state = ConnectionState.TRACING;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.tracing'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Tracing...');
    expect(progress.innerHTML).toContain('cable');

    const endSpy = spyOn(connection, 'endTrace').and.callThrough();
    const fetchSpy = spyOn(connection, 'fetchLastTracingSessionData');
    const endButton = assertDefined(
      el.querySelector<HTMLElement>('.end-btn button'),
    );
    endButton.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(endSpy).toHaveBeenCalled();
    expect(fetchSpy).toHaveBeenCalled();
  });

  it('displays ending trace elements', () => {
    goToConfigSection();
    const connection = getConnection();
    connection.state = ConnectionState.ENDING_TRACE;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.ending-trace'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Ending trace...');
    expect(progress.innerHTML).toContain('cable');

    const endButton = assertDefined(
      el.querySelector<HTMLButtonElement>('.end-btn button'),
    );
    expect(endButton.disabled).toBeTrue();
  });

  it('displays dumping state elements', () => {
    goToConfigSection();
    const connection = getConnection();
    connection.state = ConnectionState.DUMPING_STATE;
    fixture.detectChanges();

    const progress = assertDefined(htmlElement.querySelector('.dumping-state'));
    expect(progress.querySelector('.end-btn button')).toBeNull();
  });

  it('displays loading data elements', () => {
    goToConfigSection();
    const connection = getConnection();
    connection.state = ConnectionState.LOADING_DATA;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.load-data'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Fetching...');

    const endButton = assertDefined(
      el.querySelector<HTMLButtonElement>('.end-btn button'),
    );
    expect(endButton.disabled).toBeTrue();
  });

  it('starts traces after IME warning dialog', async () => {
    const spy = spyOn(getConnection(), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const buttons = dialog.querySelectorAll<HTMLElement>(
      '.warning-action-buttons button',
    );
    buttons.item(buttons.length - 1).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalled();
  });

  it('goes back to edit config display after IME warning dialog', async () => {
    const spy = spyOn(getConnection(), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const button = assertDefined(
      dialog.querySelector<HTMLElement>('.warning-action-buttons button'),
    );
    button.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).not.toHaveBeenCalled();
    expect(htmlElement.querySelector('trace-config')).toBeTruthy();
  });

  it('does not show IME warning dialog again in same session if user selects "Do not show again"', async () => {
    const spy = spyOn(getConnection(), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const option = assertDefined(
      dialog.querySelector<HTMLInputElement>(
        '.warning-action-boxes mat-checkbox input',
      ),
    );
    option.checked = true;
    option.click();
    fixture.detectChanges();

    const button = assertDefined(
      dialog.querySelector<HTMLElement>('.warning-action-buttons button'),
    );
    button.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(spy).not.toHaveBeenCalled();
    expect(htmlElement.querySelector('trace-config')).toBeTruthy();

    await clickStartTraceButton();

    expect(spy).toHaveBeenCalled();
    expect(document.querySelector('warning-dialog')).toBeNull();
  });

  it('handles successful external operations', () => {
    goToConfigSection();
    const collectTracesComponent = getCollectTracesComponent();

    collectTracesComponent.onProgressUpdate('test operation', 0);
    const el = assertDefined(htmlElement.querySelector('.load-data'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.textContent).toContain('test operation');

    collectTracesComponent.onOperationFinished(true);
    expect(htmlElement.querySelector('.load-data')).toBeNull();
    expect(htmlElement.querySelector('.trace-collection-config')).toBeTruthy();
  });

  it('restarts connection on unsuccessful external operation', () => {
    goToConfigSection();
    const collectTracesComponent = getCollectTracesComponent();

    collectTracesComponent.onProgressUpdate('test operation', 0);

    const spy = spyOn(getConnection(), 'restartConnection');
    collectTracesComponent.onOperationFinished(false);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('refreshes dumps', async () => {
    goToConfigSection();
    const collectTracesComponent = getCollectTracesComponent();
    const spy = spyOn(getConnection(), 'dumpState');
    collectTracesComponent.refreshDumps = true;
    fixture.detectChanges();

    getConnection().setState(ConnectionState.IDLE);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalledOnceWith(mockDevice, [
      {name: 'window_dump', config: []},
      {name: 'layers_dump', config: []},
      {name: 'screenshot', config: [{key: 'displays', value: []}]},
      {name: 'perfetto_dump', config: []},
    ]);
  });

  it('does not refresh dumps if no device selected', async () => {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    const collectTracesComponent = getCollectTracesComponent();
    collectTracesComponent.refreshDumps = true;
    const spy = spyOn(connection, 'dumpState');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).not.toHaveBeenCalled();
  });

  it('refreshes dumps using stored dump config', async () => {
    goToConfigSection();
    await fixture.whenStable();
    fixture.detectChanges();
    clickCheckboxAndCheckTraceConfig('window_dump', true);

    component.showSecondComponent = true;
    fixture.detectChanges();
    const newComponent = getCollectTracesComponent(1);
    const spy = spyOn(getConnection(), 'dumpState');
    newComponent.refreshDumps = true;
    fixture.detectChanges();

    getConnection().setState(ConnectionState.IDLE);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalledOnceWith(mockDevice, [
      {name: 'layers_dump', config: []},
      {name: 'screenshot', config: [{key: 'displays', value: []}]},
      {name: 'perfetto_dump', config: []},
    ]);
  });

  it('update available traces from connection', () => {
    const config = getCollectTracesComponent().traceConfig;
    expect(config['wayland_trace']?.available).toBeFalse();
    getConnection().availableTracesChangeCallback(['wayland_trace']);
    fixture.detectChanges();
    expect(config['wayland_trace']?.available).toBeTrue();
  });

  it('fetches tracing data if trace times out', async () => {
    goToConfigSection();
    const userNotifierChecker = new UserNotifierChecker();
    const connection = getConnection();
    const emitSpy = spyOn(getCollectTracesComponent().filesCollected, 'emit');
    connection.setState(ConnectionState.TRACE_TIMEOUT);

    await fixture.whenStable();
    expect(emitSpy).toHaveBeenCalledOnceWith({
      requested: [],
      collected: connection.files,
    });
    userNotifierChecker.expectAdded([new ProxyTraceTimeout()]);
  });

  it('updates options in media based config on devices change from connection', () => {
    checkMediaBasedConfigUpdates(false);
  });

  it('updates multiple selection in screen recording config on devices change from connection', () => {
    checkMediaBasedConfigUpdates(true);
  });

  describe('ProxyConnection', () => {
    beforeEach(async () => {
      await startProxyConnection();
    });

    it('displays adb proxy element', () => {
      expect(htmlElement.querySelector('adb-proxy')).toBeTruthy();
    });

    it('adds security token and restarts connection', async () => {
      spyOn(assertDefined(component.adbConnection), 'getState').and.returnValue(
        ConnectionState.UNAUTH,
      );
      fixture.detectChanges();

      const connection = assertDefined(component.adbConnection);
      const securityTokenSpy = spyOn(connection, 'setSecurityToken');
      const restartSpy = spyOn(connection, 'restartConnection');

      const proxyTokenInput = assertDefined(
        htmlElement.querySelector<HTMLInputElement>(
          '.proxy-token-input-field input',
        ),
      );
      proxyTokenInput.value = '12345';
      proxyTokenInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      assertDefined(htmlElement.querySelector<HTMLElement>('.retry')).click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(securityTokenSpy).toHaveBeenCalledOnceWith('12345');
      expect(restartSpy).toHaveBeenCalledTimes(1);
    });
  });

  function goToConfigSection() {
    const connection = getConnection();
    connection.state = ConnectionState.IDLE;
    connection.devices = [mockDevice];
    fixture.detectChanges();
    const device = assertDefined(
      htmlElement.querySelector<HTMLElement>('.available-device'),
    );
    device.click();
    fixture.detectChanges();
  }

  function clickCheckboxAndCheckTraceConfig(key: string, isDump: boolean) {
    const collectTracesComponent = getCollectTracesComponent();
    expect(
      isDump
        ? collectTracesComponent.dumpConfig[key].enabled
        : collectTracesComponent.traceConfig[key].enabled,
    ).toBeTrue();

    const checkboxSection = assertDefined(
      htmlElement.querySelector(isDump ? '.dump-section' : '.trace-section'),
    );
    const traceBoxes = Array.from(
      checkboxSection.querySelectorAll<HTMLElement>('.trace-checkbox'),
    );

    const expectedName = isDump
      ? collectTracesComponent.dumpConfig[key].name
      : collectTracesComponent.traceConfig[key].name;
    const traceBox = assertDefined(
      traceBoxes.find((box) => box.textContent?.includes(expectedName)),
    );
    const traceCheckboxInput = assertDefined(
      traceBox.querySelector<HTMLInputElement>('input'),
    );
    traceCheckboxInput.click();
    fixture.detectChanges();
    expect(
      isDump
        ? collectTracesComponent.dumpConfig[key].enabled
        : collectTracesComponent.traceConfig[key].enabled,
    ).toBeFalse();
  }

  async function startProxyConnection() {
    const collectTracesComponent = getCollectTracesComponent();
    collectTracesComponent.adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    component.adbConnection = new ProxyConnection();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function updateTraceConfigToInvalidIMEFrameMapping() {
    const config = assertDefined(getCollectTracesComponent().traceConfig);
    config['ime'].enabled = true;
    config['layers_trace'].enabled = false;
  }

  async function clickStartTraceButton() {
    const start = assertDefined(
      htmlElement.querySelector<HTMLElement>('.start-btn button'),
    );
    start.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function clickDumpStateButton() {
    const dump = assertDefined(
      htmlElement.querySelector<HTMLElement>('.dump-btn button'),
    );
    dump.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function openAndReturnDialog(): Promise<HTMLElement> {
    updateTraceConfigToInvalidIMEFrameMapping();
    await clickStartTraceButton();
    const dialog = assertDefined(
      document.querySelector<HTMLElement>('warning-dialog'),
    );
    expect(dialog.textContent).toContain(
      'Cannot build frame mapping for IME with selected traces',
    );
    return dialog;
  }

  function getCollectTracesComponent(index = 0): CollectTracesComponent {
    return assertDefined(component.collectTracesComponents?.get(index));
  }

  function getConnection(): MockAdbConnection {
    const connection = assertDefined(component.adbConnection);
    expect(connection).toBeInstanceOf(MockAdbConnection);
    return connection as MockAdbConnection;
  }

  function checkMediaBasedConfigUpdates(
    multiDisplayScreenRecordingAvailable: boolean,
  ) {
    checkMediaBasedConfig([], false);

    const validDeviceWithDisplays: AdbDevice = {
      id: '35562',
      model: 'Pixel 6',
      authorized: true,
      displays: ['12345 Extra Info'],
      multiDisplayScreenRecordingAvailable,
    };

    // does not update if no selected device
    getConnection().devicesChangeCallback([validDeviceWithDisplays]);
    fixture.detectChanges();
    checkMediaBasedConfig([], false);

    goToConfigSection();

    // does not update if selected device not in new devices
    getConnection().devicesChangeCallback([
      {
        id: '99',
        model: 'Pixel 6',
        authorized: true,
        displays: ['12345 Extra Info'],
        multiDisplayScreenRecordingAvailable,
      },
    ]);
    fixture.detectChanges();
    checkMediaBasedConfig([], false);

    getConnection().devicesChangeCallback([validDeviceWithDisplays]);
    fixture.detectChanges();
    checkMediaBasedConfig(
      ['12345 Extra Info'],
      multiDisplayScreenRecordingAvailable,
    );

    if (multiDisplayScreenRecordingAvailable) {
      validDeviceWithDisplays.multiDisplayScreenRecordingAvailable = false;
      getConnection().devicesChangeCallback([validDeviceWithDisplays]);
      fixture.detectChanges();
      checkMediaBasedConfig(['12345 Extra Info'], false);
    }
  }

  function checkMediaBasedConfig(
    displays: string[],
    multiDisplayScreenRecordingAvailable: boolean,
  ) {
    const screenRecordingConfig = assertDefined(
      getCollectTracesComponent().traceConfig['screen_recording'].config,
    ).selectionConfigs[0];
    const screenshotConfig = assertDefined(
      getCollectTracesComponent().dumpConfig['screenshot'].config,
    ).selectionConfigs[0];
    expect(screenRecordingConfig.options).toEqual(displays);
    expect(screenshotConfig.options).toEqual(displays);
    expect(screenRecordingConfig.value).toEqual(
      multiDisplayScreenRecordingAvailable ? [] : '',
    );
  }

  @Component({
    selector: 'host-component',
    template: `
      <collect-traces
        [adbConnection]="adbConnection"
        [storage]="storage"></collect-traces>

      <collect-traces
        *ngIf="showSecondComponent"
        [adbConnection]="adbConnection"
        [storage]="storage"></collect-traces>
    `,
  })
  class TestHostComponent {
    adbConnection: AdbConnection = new MockAdbConnection();
    storage = new InMemoryStorage();
    showSecondComponent = false;

    @ViewChildren(CollectTracesComponent)
    collectTracesComponents: QueryList<CollectTracesComponent> | undefined;
  }
});

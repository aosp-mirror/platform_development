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
import {TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDialogModule} from '@angular/material/dialog';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSelectModule} from '@angular/material/select';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTabsModule} from '@angular/material/tabs';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {ProxyTraceTimeout} from 'messaging/user_warnings';
import {
  AppRefreshDumpsRequest,
  NoTraceTargetsSelected,
  WinscopeEvent,
} from 'messaging/winscope_event';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {waitToBeCalled} from 'test/unit/spy_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {TraceType} from 'trace/trace_type';
import {
  AdbDeviceConnection,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionState} from 'trace_collection/connection_state';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';
import {WdpDeviceConnection} from 'trace_collection/wdp/wdp_device_connection';
import {WdpHostConnection} from 'trace_collection/wdp/wdp_host_connection';
import {WinscopeProxyDeviceConnection} from 'trace_collection/winscope_proxy/winscope_proxy_device_connection';
import {CollectTracesComponent} from './collect_traces_component';
import {LoadProgressComponent} from './load_progress_component';
import {TraceConfigComponent} from './trace_config_component';
import {WarningDialogComponent} from './warning_dialog_component';
import {WdpSetupComponent} from './wdp_setup_component';
import {WinscopeProxySetupComponent} from './winscope_proxy_setup_component';

describe('CollectTracesComponent', () => {
  let hostComponent: TestHostComponent;
  let component: CollectTracesComponent;
  let dom: DOMTestHelper<TestHostComponent>;
  let mockDevice: MockAdbDeviceConnection;
  let mockDeviceWatch: MockAdbDeviceConnection;
  const testFile = new File([], 'test_file');

  beforeAll(() => {
    spyOn(WdpHostConnection.prototype, 'requestDevices');
  });

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
        MatTabsModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
      ],
      providers: [MatSnackBar],
      declarations: [
        TestHostComponent,
        CollectTracesComponent,
        WinscopeProxySetupComponent,
        WdpSetupComponent,
        TraceConfigComponent,
        LoadProgressComponent,
        WarningDialogComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
    component = assertDefined(hostComponent.components?.get(0));
    mockDevice = new MockAdbDeviceConnection(
      '35562',
      'Pixel 6',
      AdbDeviceState.AVAILABLE,
      component,
    );
    mockDeviceWatch = new MockAdbDeviceConnection(
      '75432',
      'Pixel Watch',
      AdbDeviceState.AVAILABLE,
      component,
    );
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders the expected card title', () => {
    const title = dom.get('.title');
    title.checkText('Collect Traces');
  });

  it('defaults to overriding host', () => {
    expect(component.controller?.getConnectionType()).toEqual(
      AdbConnectionType.MOCK,
    );
  });

  it('refreshes connection', () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    dom.findAndClick('.refresh-connection');
    expect(spy).toHaveBeenCalled();
  });

  it('displays no connected devices', () => {
    setSpyWithDevices([]);
    const el = dom.get('.devices-connecting');
    el.checkText('No devices detected');
  });

  it('displays connected authorized devices', () => {
    setSpyWithDevices([mockDevice]);
    const el = dom.get('.devices-connecting');
    el.checkText('Pixel 6');
    el.checkText('smartphone');
  });

  it('displays connected unauthorized devices', () => {
    setSpyWithDevices([
      new MockAdbDeviceConnection(
        '35562',
        '',
        AdbDeviceState.UNAUTHORIZED,
        component,
      ),
    ]);
    const el = dom.get('.devices-connecting');
    el.checkText('unauthorized');
    el.checkText('screen_lock_portrait');
  });

  it('detects changes in devices', async () => {
    const spy = setSpyWithDevices([]);
    const el = dom.get('.devices-connecting');
    el.checkText('No devices detected');

    spy.and.returnValue([mockDevice]);
    await dom.detectChangesAndWaitStable();
    el.checkText('Select a device: smartphone  Pixel 6 (35562)');
  });

  it('displays connected devices again if selected device no longer present', () => {
    const spy = setSpyWithDevices([mockDevice]);
    clickAvailableDevice();

    spy.and.returnValue([mockDeviceWatch]);
    dom.detectChanges();
    const el = dom.get('.devices-connecting');
    el.checkText('Select a device: smartphone  Pixel Watch (75432)');
  });

  it('auto selects last device', () => {
    const spy = setSpyWithDevices([mockDevice]);
    clickAvailableDevice();
    let configSection = dom.get('.trace-collection-config');
    configSection.checkText('Pixel 6');

    spy.and.returnValue([mockDeviceWatch]);
    dom.detectChanges();

    const el = dom.get('.devices-connecting');
    el.checkText('Select a device: smartphone  Pixel Watch (75432)');
    expect(dom.find('.trace-collection-config')).toBeUndefined();

    spy.and.returnValue([mockDevice]);
    dom.detectChanges();
    configSection = dom.get('.trace-collection-config');
    configSection.checkText('Pixel 6');
  });

  it('displays trace collection config elements', async () => {
    goToConfigSection();

    const el = dom.get('.trace-collection-config');
    el.checkText('smartphone');
    el.checkText('Pixel 6');
    el.checkText('35562');

    const traceSection = dom.get('.trace-section');
    traceSection.get('trace-config').checkText('Trace targets');
    traceSection.get('.start-btn').checkText('Start trace');

    await changeTab(1);
    const dumpSection = dom.get('.dump-section');
    dumpSection.get('trace-config').checkText('Dump targets');
    dumpSection.get('.dump-btn').checkText('Dump state');
  });

  it('updates config on change in trace config component', async () => {
    goToConfigSection();
    await dom.detectChangesAndWaitStable();
    clickCheckboxAndCheckTraceConfig(UiTraceTarget.WINDOW_MANAGER_TRACE, false);
    await changeTab(1);
    clickCheckboxAndCheckTraceConfig(UiTraceTarget.WINDOW_MANAGER_DUMP, true);
  });

  it('start trace button works as expected', async () => {
    goToConfigSection();
    const spy = spyOn(assertDefined(component.controller), 'startTrace');
    await clickStartTraceButton();
    expect(spy).toHaveBeenCalled();
  });

  it('emits event if no trace targets selected', async () => {
    goToConfigSection();
    let lastEvent: WinscopeEvent | undefined;
    component.setEmitEvent(async (event: WinscopeEvent) => {
      lastEvent = event;
    });

    Object.values(component.traceConfig).forEach(
      (c) => (c.config.enabled = false),
    );
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
    await clickStartTraceButton();

    expect(lastEvent).toEqual(new NoTraceTargetsSelected());
    expect(spy).not.toHaveBeenCalled();
  });

  it('dump state button works as expected', async () => {
    goToConfigSection();
    await changeTab(1);
    const filesSpy = spyOn(component.filesCollected, 'emit');
    const controller = assertDefined(component.controller);
    spyOn(controller, 'fetchLastSessionData').and.returnValue(
      Promise.resolve([testFile]),
    );

    await clickDumpStateButton();
    await waitToBeCalled(filesSpy);
    expect(filesSpy).toHaveBeenCalledOnceWith({
      requested: [
        {name: 'Window Manager', types: [TraceType.WINDOW_MANAGER]},
        {name: 'Surface Flinger', types: [TraceType.SURFACE_FLINGER]},
        {name: 'Screenshot', types: [TraceType.SCREENSHOT]},
      ],
      collected: [testFile],
    });
  });

  it('emits event if no dump targets selected', async () => {
    goToConfigSection();
    await changeTab(1);
    let lastEvent: WinscopeEvent | undefined;
    component.setEmitEvent(async (event: WinscopeEvent) => {
      lastEvent = event;
    });

    Object.values(component.dumpConfig).forEach(
      (c) => (c.config.enabled = false),
    );
    const filesSpy = spyOn(component.filesCollected, 'emit');
    await clickDumpStateButton();

    expect(lastEvent).toEqual(new NoTraceTargetsSelected());
    expect(filesSpy).not.toHaveBeenCalled();
  });

  it('does not collect files if dumping fails', async () => {
    goToConfigSection();
    await changeTab(1);
    const filesSpy = spyOn(component.filesCollected, 'emit');
    const controller = assertDefined(component.controller);
    spyOn(controller, 'dumpState').and.callFake(async () => {
      component.state = ConnectionState.ERROR;
    });
    await clickDumpStateButton();

    expect(filesSpy).not.toHaveBeenCalled();
  });

  it('change device button works as expected', () => {
    goToConfigSection();
    expect(component.getSelectedDevice()).toBeDefined();

    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    dom.findAndClick('.change-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('fetch existing traces button emits files and restarts host if no files found', async () => {
    const controller = assertDefined(component.controller);
    spyOn(controller, 'fetchLastSessionData').and.returnValue(
      Promise.resolve([]),
    );
    const emitSpy = spyOn(component.filesCollected, 'emit');
    const restartSpy = spyOn(controller, 'restartConnection');
    goToConfigSection();

    await dom.clickAndWaitStable('.fetch-btn');
    expect(emitSpy).toHaveBeenCalledOnceWith({
      requested: [],
      collected: [],
    });
    expect(restartSpy).toHaveBeenCalledTimes(1);
  });

  it('fetch existing traces button emits files and does not restart host if files found', async () => {
    const controller = assertDefined(component.controller);
    spyOn(controller, 'fetchLastSessionData').and.returnValue(
      Promise.resolve([testFile]),
    );
    const emitSpy = spyOn(component.filesCollected, 'emit');
    const restartSpy = spyOn(controller, 'restartConnection');
    goToConfigSection();

    await dom.clickAndWaitStable('.fetch-btn');
    expect(emitSpy).toHaveBeenCalledWith({
      requested: [],
      collected: [testFile],
    });
    expect(restartSpy).not.toHaveBeenCalled();
  });

  it('displays unknown error message', () => {
    component.state = ConnectionState.ERROR;
    dom.detectChanges();

    const testErrorMessage = 'bad things are happening';
    component.errorText = testErrorMessage;
    dom.detectChanges();

    const el = dom.get('.unknown-error');
    el.checkText('Error:');
    el.checkText(testErrorMessage);

    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    dom.findAndClick('.retry-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('displays starting trace elements', () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.STARTING_TRACE);
    dom.detectChanges();
    checkTracingProgress('Starting trace...', true);
  });

  it('displays tracing elements and ends trace correctly', async () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.TRACING);
    dom.detectChanges();
    checkTracingProgress('Tracing...', false);

    const controller = assertDefined(component.controller);
    const endSpy = spyOn(controller, 'endTrace').and.callFake(async () => {
      component.onConnectionStateChange(ConnectionState.ENDING_TRACE);
    });
    const fetchSpy = spyOn(controller, 'fetchLastSessionData').and.returnValue(
      Promise.resolve([]),
    );
    await dom.clickAndWaitStable('.end-btn button');
    expect(endSpy).toHaveBeenCalled();
    expect(fetchSpy).toHaveBeenCalled();
  });

  it('displays ending trace elements', () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.ENDING_TRACE);
    dom.detectChanges();
    checkTracingProgress('Ending trace...', true);
  });

  it('displays dumping state elements', async () => {
    goToConfigSection();
    await changeTab(1);
    component.onConnectionStateChange(ConnectionState.DUMPING_STATE);
    dom.detectChanges();
    const progress = dom.get('.dumping-state');
    expect(progress.find('.end-btn button')).toBeUndefined();
  });

  it('displays loading data elements', async () => {
    goToConfigSection();
    await component.onConnectionStateChange(ConnectionState.LOADING_DATA);
    dom.detectChanges();
    checkTracingProgress('Fetching...');
  });

  it('starts traces after IME warning dialog', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();
    await dialog.clickLastAndWaitStable('.warning-action-buttons button');
    expect(spy).toHaveBeenCalled();
  });

  it('goes back to edit config display after IME warning dialog', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();
    await dialog.clickAndWaitStable('.warning-action-buttons button');
    expect(spy).not.toHaveBeenCalled();
    expect(dom.find('trace-config')).toBeDefined();
  });

  it('does not show IME warning dialog again in same controller if user selects "Do not show again"', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const option = dialog.get('.warning-action-boxes mat-checkbox input');
    option.getHTMLElement<HTMLInputElement>().checked = true;
    option.click();

    await dialog.clickAndWaitStable('.warning-action-buttons button');
    expect(spy).not.toHaveBeenCalled();
    expect(dom.find('trace-config')).toBeDefined();

    await clickStartTraceButton();
    expect(spy).toHaveBeenCalled();
    expect(dom.findInDocument('warning-dialog')).toBeUndefined();
  });

  it('handles successful external operations', () => {
    goToConfigSection();
    component.onProgressUpdate('test operation', 0);
    checkTracingProgress('test operation');
    component.onOperationFinished(true);
    expect(dom.find('.tracing-progress')).toBeUndefined();
    expect(dom.find('.trace-collection-config')).toBeDefined();
  });

  it('restarts host on unsuccessful external operation', () => {
    goToConfigSection();
    component.onProgressUpdate('test operation', 0);
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    component.onOperationFinished(false);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('refreshes dumps', async () => {
    goToConfigSection();
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'dumpState');
    component.refreshDumps = true;
    await component.onConnectionStateChange(ConnectionState.CONNECTING);
    await dom.detectChangesAndWaitStable();
    expect(spy).toHaveBeenCalledOnceWith(mockDevice, [
      {target: UiTraceTarget.WINDOW_MANAGER_DUMP, config: []},
      {target: UiTraceTarget.SURFACE_FLINGER_DUMP, config: []},
      {
        target: UiTraceTarget.SCREENSHOT,
        config: [{key: 'displays', value: []}],
      },
    ]);
  });

  it('does not refresh dumps if no device selected', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'dumpState');
    component.refreshDumps = true;
    await component.onConnectionStateChange(ConnectionState.CONNECTING);
    await dom.detectChangesAndWaitStable();
    expect(spy).not.toHaveBeenCalled();
  });

  it('refreshes dumps using stored dump config', async () => {
    goToConfigSection();
    await dom.detectChangesAndWaitStable();
    await changeTab(1);
    clickCheckboxAndCheckTraceConfig(UiTraceTarget.WINDOW_MANAGER_DUMP, true);

    hostComponent.showFirstComponent = false;
    dom.detectChanges();
    hostComponent.showSecondComponent = true;
    await dom.detectChangesAndWaitStable();
    const newComponent = assertDefined(hostComponent.components?.get(0));
    const controller = assertDefined(newComponent.controller);
    const spy = spyOn(controller, 'dumpState');
    await newComponent.onWinscopeEvent(new AppRefreshDumpsRequest());
    dom.detectChanges();

    await newComponent.onConnectionStateChange(ConnectionState.CONNECTING);
    await dom.detectChangesAndWaitStable();
    const newDevice = new MockAdbDeviceConnection(
      '35562',
      'Pixel 6',
      AdbDeviceState.AVAILABLE,
      newComponent,
    );
    expect(spy).toHaveBeenCalledOnceWith(newDevice, [
      {target: UiTraceTarget.SURFACE_FLINGER_DUMP, config: []},
      {
        target: UiTraceTarget.SCREENSHOT,
        config: [{key: 'displays', value: []}],
      },
    ]);
  });

  it('update available traces from host', () => {
    const config = component.traceConfig;
    expect(config[UiTraceTarget.WAYLAND]?.available).toBeFalse();
    component.onAvailableTracesChange([UiTraceTarget.WAYLAND], []);
    dom.detectChanges();
    expect(config[UiTraceTarget.WAYLAND]?.available).toBeTrue();
    component.onAvailableTracesChange([], [UiTraceTarget.WAYLAND]);
    dom.detectChanges();
    expect(config[UiTraceTarget.WAYLAND]?.available).toBeFalse();
  });

  it('sets error state onError', async () => {
    const msg = 'test error message';
    await component.onError(msg);
    expect(component.state).toEqual(ConnectionState.ERROR);
    expect(component.errorText).toEqual(msg);
  });

  it('ends trace if trace times out', async () => {
    goToConfigSection();
    const userNotifierChecker = new UserNotifierChecker();
    const spy = spyOn(component, 'endTrace').and.callThrough();
    await component.onConnectionStateChange(ConnectionState.TRACE_TIMEOUT);
    await dom.whenStable();
    expect(spy).toHaveBeenCalledTimes(1);
    userNotifierChecker.expectAdded([new ProxyTraceTimeout()]);
  });

  it('updates options in media based config on devices change from host', () => {
    checkMediaBasedConfigUpdates(false);
  });

  it('updates multiple selection in screen recording config on devices change from host', () => {
    checkMediaBasedConfigUpdates(true);
  });

  it('changes host type on mat select change', async () => {
    await changeConnection(1);
    expect(component.controller?.getConnectionType()).toEqual(
      AdbConnectionType.WDP,
    );
    await changeConnection(0);
    expect(component.controller?.getConnectionType()).toEqual(
      AdbConnectionType.WINSCOPE_PROXY,
    );
  });

  it('changes host type by default if in store', async () => {
    await changeConnection(1);
    hostComponent.showFirstComponent = false;
    dom.detectChanges();
    hostComponent.showSecondComponent = true;
    await dom.detectChangesAndWaitStable();
    const component = assertDefined(hostComponent.components?.get(0));
    expect(component.controller?.getConnectionType()).toEqual(
      AdbConnectionType.WDP,
    );
  });

  it('cancels device requests', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'cancelDeviceRequests');
    await component.onConnectionStateChange(ConnectionState.CONNECTING);
    expect(spy).not.toHaveBeenCalled();

    const cancelStates = [
      ConnectionState.ERROR,
      ConnectionState.NOT_FOUND,
      ConnectionState.INVALID_VERSION,
      ConnectionState.UNAUTH,
      ConnectionState.STARTING_TRACE,
      ConnectionState.TRACING,
      ConnectionState.ENDING_TRACE,
      ConnectionState.DUMPING_STATE,
      ConnectionState.LOADING_DATA,
    ];
    for (const [index, state] of cancelStates.entries()) {
      await component.onConnectionStateChange(state);
      expect(spy).toHaveBeenCalledTimes(index + 1);
    }
  });

  describe('WinscopeProxyHostConnection', () => {
    beforeEach(async () => {
      hostComponent.showFirstComponent = false;
      hostComponent.storage = new InMemoryStorage();
      hostComponent.showSecondComponent = true;
      await dom.detectChangesAndWaitStable();
      component = assertDefined(hostComponent.components?.get(0));
      component.state = ConnectionState.UNAUTH;
      dom.detectChanges();
    });

    it('defaults to winscope proxy host', () => {
      expect(component.controller?.getConnectionType()).toEqual(
        AdbConnectionType.WINSCOPE_PROXY,
      );
    });

    it('displays proxy element if not adb success', () => {
      expect(dom.find('winscope-proxy-setup')).toBeTruthy();
    });

    it('adds security token and restarts host', async () => {
      const controller = assertDefined(component.controller);
      const securityTokenSpy = spyOn(controller, 'setSecurityToken');
      const restartSpy = spyOn(controller, 'restartConnection');
      dom.findAndDispatchInput('.proxy-token-input-field', '12345');
      await dom.clickAndWaitStable('.retry');
      expect(securityTokenSpy).toHaveBeenCalledOnceWith('12345');
      expect(restartSpy).toHaveBeenCalledTimes(1);
    });

    it('does not show authorize device button', () => {
      const device = new WinscopeProxyDeviceConnection('35562', component, []);
      const stateSpy = spyOn(device, 'getState');
      setSpyWithDevices([device]);
      stateSpy.and.returnValue(AdbDeviceState.OFFLINE);
      dom.detectChanges();
      expect(dom.find('.authorize-btn')).toBeUndefined();

      stateSpy.and.returnValue(AdbDeviceState.AVAILABLE);
      dom.detectChanges();
      expect(dom.find('.authorize-btn')).toBeUndefined();

      stateSpy.and.returnValue(AdbDeviceState.UNAUTHORIZED);
      dom.detectChanges();
      expect(dom.find('.authorize-btn')).toBeUndefined();
    });
  });

  describe('WdpHostConnection', () => {
    beforeEach(async () => {
      hostComponent.showSecondComponent = true;
      hostComponent.showFirstComponent = false;
      await dom.detectChangesAndWaitStable();
      component = assertDefined(hostComponent.components?.get(0));
      await changeConnection(1);
      component.state = ConnectionState.UNAUTH;
      dom.detectChanges();
    });

    it('displays proxy element if not adb success', () => {
      expect(dom.find('wdp-setup')).toBeTruthy();
    });

    it('restarts host', async () => {
      const controller = assertDefined(component.controller);
      const securityTokenSpy = spyOn(controller, 'setSecurityToken');
      const restartSpy = spyOn(controller, 'restartConnection');

      await dom.clickAndWaitStable('wdp-setup .retry');
      expect(securityTokenSpy).not.toHaveBeenCalled();
      expect(restartSpy).toHaveBeenCalledTimes(1);
    });

    it('tries to authorize device', () => {
      const device = new WdpDeviceConnection('35562', component);
      const authorizeSpy = spyOn(device, 'tryAuthorize');
      const stateSpy = spyOn(device, 'getState');
      setSpyWithDevices([device]);
      stateSpy.and.returnValue(AdbDeviceState.OFFLINE);
      dom.detectChanges();
      expect(dom.find('.authorize-btn')).toBeUndefined();

      stateSpy.and.returnValue(AdbDeviceState.AVAILABLE);
      dom.detectChanges();
      expect(dom.find('.authorize-btn')).toBeUndefined();

      stateSpy.and.returnValue(AdbDeviceState.UNAUTHORIZED);
      dom.detectChanges();
      dom.findAndClick('.authorize-btn');
      expect(authorizeSpy).toHaveBeenCalledTimes(1);
    });
  });

  function setSpyWithDevices(
    devices: AdbDeviceConnection[],
    c = component,
  ): jasmine.Spy {
    const controller = assertDefined(c.controller);
    c.state = ConnectionState.IDLE;
    const spy = spyOn(controller, 'getDevices').and.returnValue(devices);
    dom.detectChanges();
    return spy;
  }

  function goToConfigSection() {
    setSpyWithDevices([mockDevice], component);
    clickAvailableDevice();
  }

  function clickAvailableDevice() {
    dom.findAndClick('.available-device');
  }

  function clickCheckboxAndCheckTraceConfig(
    key: UiTraceTarget,
    isDump: boolean,
  ) {
    expect(
      isDump
        ? component.dumpConfig[key].config.enabled
        : component.traceConfig[key].config.enabled,
    ).toBeTrue();

    const checkboxSection = dom.get(
      isDump ? '.dump-section' : '.trace-section',
    );
    const boxes = Array.from(checkboxSection.findAll('.trace-checkbox'));

    const expectedName = isDump
      ? component.dumpConfig[key].name
      : component.traceConfig[key].name;
    const traceBox = assertDefined(
      boxes.find((box) => box.getText()?.includes(expectedName)),
    );
    traceBox.findAndClick('input');
    expect(
      isDump
        ? component.dumpConfig[key].config.enabled
        : component.traceConfig[key].config.enabled,
    ).toBeFalse();
  }

  function updateTraceConfigToInvalidIMEFrameMapping() {
    const config = assertDefined(component.traceConfig);
    config[UiTraceTarget.IME].config.enabled = true;
    config[UiTraceTarget.SURFACE_FLINGER_TRACE].config.enabled = false;
  }

  async function clickStartTraceButton() {
    await dom.clickAndWaitStable('.start-btn button');
  }

  function checkTracingProgress(message: string, endButtonDisabled?: boolean) {
    const el = dom.get('.tracing-progress');
    const progress = el.get('load-progress');
    progress.checkText(message);
    const endButton = el.find('.end-btn button');
    if (endButtonDisabled === undefined) {
      expect(endButton).toBeUndefined();
    } else {
      progress.checkInnerHTML('cable');
      assertDefined(endButton).checkDisabled(endButtonDisabled);
    }
  }

  async function clickDumpStateButton() {
    await dom.clickAndWaitStable('.dump-btn button');
  }

  async function openAndReturnDialog(): Promise<
    DOMTestHelper<TestHostComponent>
  > {
    updateTraceConfigToInvalidIMEFrameMapping();
    await clickStartTraceButton();
    const dialog = dom.getInDocument('warning-dialog');
    dialog.checkText('Cannot build frame mapping for IME with selected traces');
    return dialog;
  }

  function checkMediaBasedConfigUpdates(multiDisplayScreenRecording: boolean) {
    checkMediaBasedConfig([], false);

    const device1 = new MockAdbDeviceConnection(
      '35562',
      'Pixel 6',
      AdbDeviceState.AVAILABLE,
      component,
      ['12345 Extra Info'],
      multiDisplayScreenRecording,
    );
    // does not update if no selected device
    component.onDevicesChange([device1]);
    dom.detectChanges();
    checkMediaBasedConfig([], false);

    goToConfigSection();

    // does not update if selected device not in new devices
    const device2 = new MockAdbDeviceConnection(
      '99',
      'Pixel 6',
      AdbDeviceState.AVAILABLE,
      component,
      ['12345 Extra Info'],
      multiDisplayScreenRecording,
    );
    component.onDevicesChange([device2]);
    dom.detectChanges();
    checkMediaBasedConfig([], false);

    component.onDevicesChange([device1]);
    dom.detectChanges();
    checkMediaBasedConfig(['12345 Extra Info'], multiDisplayScreenRecording);

    if (multiDisplayScreenRecording) {
      const device3 = new MockAdbDeviceConnection(
        '35562',
        'Pixel 6',
        AdbDeviceState.AVAILABLE,
        component,
        ['12345 Extra Info'],
        false,
      );
      component.onDevicesChange([device3]);
      dom.detectChanges();
      checkMediaBasedConfig(['12345 Extra Info'], false);
    }
  }

  function checkMediaBasedConfig(
    displays: string[],
    multiDisplayScreenRecording: boolean,
  ) {
    const screenRecordingConfig = assertDefined(
      component.traceConfig[UiTraceTarget.SCREEN_RECORDING].config,
    ).selectionConfigs[0];
    const screenshotConfig = assertDefined(
      component.dumpConfig[UiTraceTarget.SCREENSHOT].config,
    ).selectionConfigs[0];
    expect(screenRecordingConfig.options).toEqual(displays);
    expect(screenshotConfig.options).toEqual(displays);
    expect(screenRecordingConfig.value).toEqual(
      multiDisplayScreenRecording ? [] : '',
    );
  }

  async function changeConnection(index: number) {
    await dom.openMatSelect();
    await dom.whenRenderingDone();
    const panel = dom.getMatSelectPanel();
    panel.findAndClickByIndex('.mat-option', index);
  }

  async function changeTab(index: number) {
    const selector = '.target-tabs .mat-tab-label';
    await dom.clickByIndexAndWaitStable(selector, index);
  }

  @Component({
    selector: 'host-component',
    template: `
      <collect-traces
        *ngIf="showFirstComponent"
        [storage]="storage"></collect-traces>

      <collect-traces
        *ngIf="showSecondComponent"
        [storage]="storage"></collect-traces>
    `,
  })
  class TestHostComponent {
    storage = new InMemoryStorage();
    showFirstComponent = true;
    showSecondComponent = false;

    constructor() {
      this.storage.add('adbConnectionType', AdbConnectionType.MOCK);
    }

    @ViewChildren(CollectTracesComponent)
    components: QueryList<CollectTracesComponent> | undefined;
  }
});

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
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {waitToBeCalled} from 'test/utils';
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
import {WinscopeProxyDeviceConnection} from 'trace_collection/winscope_proxy/winscope_proxy_device_connection';
import {CollectTracesComponent} from './collect_traces_component';
import {LoadProgressComponent} from './load_progress_component';
import {TraceConfigComponent} from './trace_config_component';
import {WarningDialogComponent} from './warning_dialog_component';
import {WdpSetupComponent} from './wdp_setup_component';
import {WinscopeProxySetupComponent} from './winscope_proxy_setup_component';

describe('CollectTracesComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;
  let component: CollectTracesComponent;
  let htmlElement: HTMLElement;
  let mockDevice: MockAdbDeviceConnection;
  let mockDeviceWatch: MockAdbDeviceConnection;
  const testFile = new File([], 'test_file');

  beforeAll(() => {
    spyOn(window, 'open');
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
    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
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
    const title = assertDefined(htmlElement.querySelector('.title'));
    expect(title.textContent).toContain('Collect Traces');
  });

  it('defaults to overriding host', () => {
    expect(component.controller?.getConnectionType()).toEqual(
      AdbConnectionType.MOCK,
    );
  });

  it('refreshes connection', () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.refresh-connection'),
    ).click();
    expect(spy).toHaveBeenCalled();
  });

  it('displays no connected devices', () => {
    setSpyWithDevices([]);
    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('No devices detected');
  });

  it('displays connected authorized devices', () => {
    setSpyWithDevices([mockDevice]);
    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('Pixel 6');
    expect(el.textContent).toContain('smartphone');
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
    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('unauthorized');
    expect(el.textContent).toContain('screen_lock_portrait');
  });

  it('detects changes in devices', async () => {
    const spy = setSpyWithDevices([]);
    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('No devices detected');

    spy.and.returnValue([mockDevice]);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(el.textContent?.trim()).toContain(
      'Select a device: smartphone  Pixel 6 (35562)',
    );
  });

  it('displays connected devices again if selected device no longer present', () => {
    const spy = setSpyWithDevices([mockDevice]);
    clickAvailableDevice();

    spy.and.returnValue([mockDeviceWatch]);
    fixture.detectChanges();
    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel Watch (75432)',
    );
  });

  it('auto selects last device', () => {
    const spy = setSpyWithDevices([mockDevice]);
    clickAvailableDevice();
    let configSection = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(configSection.textContent).toContain('Pixel 6');

    spy.and.returnValue([mockDeviceWatch]);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel Watch (75432)',
    );
    expect(htmlElement.querySelector('.trace-collection-config')).toBeNull();

    spy.and.returnValue([mockDevice]);
    fixture.detectChanges();
    configSection = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(configSection.textContent).toContain('Pixel 6');
  });

  it('displays trace collection config elements', async () => {
    goToConfigSection();

    const el = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(el.textContent).toContain('smartphone');
    expect(el.textContent).toContain('Pixel 6');
    expect(el.textContent).toContain('35562');

    const traceSection = assertDefined(
      htmlElement.querySelector('.trace-section'),
    );
    expect(traceSection.querySelector('trace-config')?.textContent).toContain(
      'Trace targets',
    );
    expect(traceSection.querySelector('.start-btn')?.textContent).toContain(
      'Start trace',
    );

    await changeTab(1);
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
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.change-btn'),
    ).click();
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

    assertDefined(htmlElement.querySelector<HTMLElement>('.fetch-btn')).click();
    fixture.detectChanges();
    await fixture.whenStable();

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

    assertDefined(htmlElement.querySelector<HTMLElement>('.fetch-btn')).click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(emitSpy).toHaveBeenCalledWith({
      requested: [],
      collected: [testFile],
    });
    expect(restartSpy).not.toHaveBeenCalled();
  });

  it('displays unknown error message', () => {
    component.state = ConnectionState.ERROR;
    fixture.detectChanges();

    const testErrorMessage = 'bad things are happening';
    component.errorText = testErrorMessage;
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.unknown-error'));
    expect(el.textContent).toContain('Error:');
    expect(el.textContent).toContain(testErrorMessage);

    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'restartConnection');
    assertDefined(htmlElement.querySelector<HTMLElement>('.retry-btn')).click();
    expect(spy).toHaveBeenCalled();
  });

  it('displays starting trace elements', () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.STARTING_TRACE);
    fixture.detectChanges();
    checkTracingProgress('Starting trace...', true);
  });

  it('displays tracing elements and ends trace correctly', async () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.TRACING);
    fixture.detectChanges();
    checkTracingProgress('Tracing...', false);

    const controller = assertDefined(component.controller);
    const endSpy = spyOn(controller, 'endTrace').and.callFake(async () => {
      component.onConnectionStateChange(ConnectionState.ENDING_TRACE);
    });
    const fetchSpy = spyOn(controller, 'fetchLastSessionData').and.returnValue(
      Promise.resolve([]),
    );
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.end-btn button'),
    ).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(endSpy).toHaveBeenCalled();
    expect(fetchSpy).toHaveBeenCalled();
  });

  it('displays ending trace elements', () => {
    goToConfigSection();
    component.onConnectionStateChange(ConnectionState.ENDING_TRACE);
    fixture.detectChanges();
    checkTracingProgress('Ending trace...', true);
  });

  it('displays dumping state elements', async () => {
    goToConfigSection();
    await changeTab(1);
    component.onConnectionStateChange(ConnectionState.DUMPING_STATE);
    fixture.detectChanges();
    const progress = assertDefined(htmlElement.querySelector('.dumping-state'));
    expect(progress.querySelector('.end-btn button')).toBeNull();
  });

  it('displays loading data elements', async () => {
    goToConfigSection();
    await component.onConnectionStateChange(ConnectionState.LOADING_DATA);
    fixture.detectChanges();
    checkTracingProgress('Fetching...');
  });

  it('starts traces after IME warning dialog', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
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
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();
    assertDefined(
      dialog.querySelector<HTMLElement>('.warning-action-buttons button'),
    ).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).not.toHaveBeenCalled();
    expect(htmlElement.querySelector('trace-config')).toBeTruthy();
  });

  it('does not show IME warning dialog again in same controller if user selects "Do not show again"', async () => {
    const controller = assertDefined(component.controller);
    const spy = spyOn(controller, 'startTrace');
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

    assertDefined(
      dialog.querySelector<HTMLElement>('.warning-action-buttons button'),
    ).click();
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
    component.onProgressUpdate('test operation', 0);
    checkTracingProgress('test operation');
    component.onOperationFinished(true);
    expect(htmlElement.querySelector('.tracing-progress')).toBeNull();
    expect(htmlElement.querySelector('.trace-collection-config')).toBeTruthy();
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
    fixture.detectChanges();
    await fixture.whenStable();
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
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).not.toHaveBeenCalled();
  });

  it('refreshes dumps using stored dump config', async () => {
    goToConfigSection();
    await fixture.whenStable();
    fixture.detectChanges();
    await changeTab(1);
    clickCheckboxAndCheckTraceConfig(UiTraceTarget.WINDOW_MANAGER_DUMP, true);

    hostComponent.showFirstComponent = false;
    fixture.detectChanges();
    hostComponent.showSecondComponent = true;
    fixture.detectChanges();
    await fixture.whenStable();
    const newComponent = assertDefined(hostComponent.components?.get(0));
    const controller = assertDefined(newComponent.controller);
    const spy = spyOn(controller, 'dumpState');
    await newComponent.onWinscopeEvent(new AppRefreshDumpsRequest());
    fixture.detectChanges();

    await newComponent.onConnectionStateChange(ConnectionState.CONNECTING);
    fixture.detectChanges();
    await fixture.whenStable();
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
    fixture.detectChanges();
    expect(config[UiTraceTarget.WAYLAND]?.available).toBeTrue();
    component.onAvailableTracesChange([], [UiTraceTarget.WAYLAND]);
    fixture.detectChanges();
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
    await fixture.whenStable();
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
    fixture.detectChanges();
    hostComponent.showSecondComponent = true;
    fixture.detectChanges();
    await fixture.whenStable();
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
      fixture.detectChanges();
      await fixture.whenStable();
      component = assertDefined(hostComponent.components?.get(0));
      component.state = ConnectionState.UNAUTH;
      fixture.detectChanges();
    });

    it('defaults to winscope proxy host', () => {
      expect(component.controller?.getConnectionType()).toEqual(
        AdbConnectionType.WINSCOPE_PROXY,
      );
    });

    it('displays proxy element if not adb success', () => {
      expect(htmlElement.querySelector('winscope-proxy-setup')).toBeTruthy();
    });

    it('adds security token and restarts host', async () => {
      const controller = assertDefined(component.controller);
      const securityTokenSpy = spyOn(controller, 'setSecurityToken');
      const restartSpy = spyOn(controller, 'restartConnection');

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

    it('does not show authorize device button', () => {
      const device = new WinscopeProxyDeviceConnection('35562', component, []);
      const stateSpy = spyOn(device, 'getState');
      setSpyWithDevices([device]);
      stateSpy.and.returnValue(AdbDeviceState.OFFLINE);
      fixture.detectChanges();
      expect(htmlElement.querySelector('.authorize-btn')).toBeNull();

      stateSpy.and.returnValue(AdbDeviceState.AVAILABLE);
      fixture.detectChanges();
      expect(htmlElement.querySelector('.authorize-btn')).toBeNull();

      stateSpy.and.returnValue(AdbDeviceState.UNAUTHORIZED);
      fixture.detectChanges();
      expect(htmlElement.querySelector('.authorize-btn')).toBeNull();
    });
  });

  describe('WdpHostConnection', () => {
    beforeEach(async () => {
      hostComponent.showSecondComponent = true;
      hostComponent.showFirstComponent = false;
      fixture.detectChanges();
      await fixture.whenStable();
      component = assertDefined(hostComponent.components?.get(0));
      await changeConnection(1);
      component.state = ConnectionState.UNAUTH;
      fixture.detectChanges();
    });

    it('displays proxy element if not adb success', () => {
      expect(htmlElement.querySelector('wdp-setup')).toBeTruthy();
    });

    it('restarts host', async () => {
      const controller = assertDefined(component.controller);
      const securityTokenSpy = spyOn(controller, 'setSecurityToken');
      const restartSpy = spyOn(controller, 'restartConnection');

      assertDefined(
        htmlElement.querySelector<HTMLElement>('wdp-setup .retry'),
      ).click();
      fixture.detectChanges();
      await fixture.whenStable();
      expect(securityTokenSpy).not.toHaveBeenCalled();
      expect(restartSpy).toHaveBeenCalledTimes(1);
    });

    it('tries to authorize device', () => {
      const device = new WdpDeviceConnection('35562', component);
      const authorizeSpy = spyOn(device, 'tryAuthorize');
      const stateSpy = spyOn(device, 'getState');
      setSpyWithDevices([device]);
      stateSpy.and.returnValue(AdbDeviceState.OFFLINE);
      fixture.detectChanges();
      expect(htmlElement.querySelector('.authorize-btn')).toBeNull();

      stateSpy.and.returnValue(AdbDeviceState.AVAILABLE);
      fixture.detectChanges();
      expect(htmlElement.querySelector('.authorize-btn')).toBeNull();

      stateSpy.and.returnValue(AdbDeviceState.UNAUTHORIZED);
      fixture.detectChanges();
      assertDefined(
        htmlElement.querySelector<HTMLElement>('.authorize-btn'),
      ).click();
      fixture.detectChanges();
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
    fixture.detectChanges();
    return spy;
  }

  function goToConfigSection() {
    setSpyWithDevices([mockDevice], component);
    clickAvailableDevice();
  }

  function clickAvailableDevice() {
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.available-device'),
    ).click();
    fixture.detectChanges();
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

    const checkboxSection = assertDefined(
      htmlElement.querySelector(isDump ? '.dump-section' : '.trace-section'),
    );
    const traceBoxes = Array.from(
      checkboxSection.querySelectorAll<HTMLElement>('.trace-checkbox'),
    );

    const expectedName = isDump
      ? component.dumpConfig[key].name
      : component.traceConfig[key].name;
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
    const start = assertDefined(
      htmlElement.querySelector<HTMLElement>('.start-btn button'),
    );
    start.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function checkTracingProgress(message: string, endButtonDisabled?: boolean) {
    const el = assertDefined(htmlElement.querySelector('.tracing-progress'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.textContent).toContain(message);
    const endButton = el.querySelector<HTMLButtonElement>('.end-btn button');
    if (endButtonDisabled === undefined) {
      expect(endButton).toBeNull();
    } else {
      expect(progress.innerHTML).toContain('cable');
      expect(endButton?.disabled).toEqual(endButtonDisabled);
    }
  }

  async function clickDumpStateButton() {
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.dump-btn button'),
    ).click();
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
    fixture.detectChanges();
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
    fixture.detectChanges();
    checkMediaBasedConfig([], false);

    component.onDevicesChange([device1]);
    fixture.detectChanges();
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
      fixture.detectChanges();
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
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.mat-select-trigger'),
    ).click();
    fixture.detectChanges();
    await fixture.whenStable();
    const options = document.querySelectorAll<HTMLElement>('.mat-option');
    options.item(index).click();
    fixture.detectChanges();
  }

  async function changeTab(index: number) {
    const labels = htmlElement.querySelectorAll<HTMLElement>(
      '.target-tabs .mat-tab-label',
    );
    labels[index].click();
    fixture.detectChanges();
    await fixture.whenStable();
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

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
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {AdbDevice} from 'trace_collection/adb_device';
import {ConnectionState} from 'trace_collection/connection_state';
import {
  TraceConfigurationMap,
  TRACES,
} from 'trace_collection/trace_collection_utils';
import {AdbProxyComponent} from './adb_proxy_component';
import {CollectTracesComponent} from './collect_traces_component';
import {LoadProgressComponent} from './load_progress_component';
import {TraceConfigComponent} from './trace_config_component';
import {WarningDialogComponent} from './warning_dialog_component';
import {WebAdbComponent} from './web_adb_component';

describe('CollectTracesComponent', () => {
  let fixture: ComponentFixture<CollectTracesComponent>;
  let component: CollectTracesComponent;
  let htmlElement: HTMLElement;
  const mockDevice: AdbDevice = {
    id: '35562',
    model: 'Pixel 6',
    authorized: true,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
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
      ],
      providers: [MatSnackBar],
      declarations: [
        CollectTracesComponent,
        AdbProxyComponent,
        WebAdbComponent,
        TraceConfigComponent,
        LoadProgressComponent,
        WarningDialogComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(CollectTracesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.isAdbProxy = true;
    component.storage = new InMemoryStorage();
    component.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TracingSettings',
      TRACES['default'],
      component.storage,
    );
    component.dumpConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'DumpSettings',
      {
        window_dump: {
          name: 'Window Manager',
          run: true,
          config: undefined,
        },
        layers_dump: {
          name: 'Surface Flinger',
          run: true,
          config: undefined,
        },
      },
      component.storage,
    );
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
    assertDefined(component.adbConnection).getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.CONNECTING);
    fixture.detectChanges();

    const connectingMessage = assertDefined(
      htmlElement.querySelector('.connecting-message'),
    );
    expect(connectingMessage.innerHTML).toContain('Connecting...');
  });

  it('displays adb set up', () => {
    assertDefined(component).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    fixture.detectChanges();

    const setUpAdbEl = assertDefined(htmlElement.querySelector('.set-up-adb'));
    expect(setUpAdbEl.querySelector('.proxy-tab')).toBeTruthy();
  });

  it('displays adb proxy element', () => {
    assertDefined(component).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    component.isAdbProxy = true;
    fixture.detectChanges();

    expect(htmlElement.querySelector('adb-proxy')).toBeTruthy();
  });

  it('displays no connected devices', () => {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.IDLE);
    connection.getDevices = jasmine.createSpy().and.returnValue([]);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('No devices detected');
  });

  it('displays connected authorized devices', () => {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.IDLE);
    connection.getDevices = jasmine
      .createSpy()
      .and.returnValue([{id: '35562', model: 'Pixel 6', authorized: true}]);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('Pixel 6');
    expect(el.innerHTML).toContain('smartphone');
  });

  it('displays connected unauthorized devices', () => {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.IDLE);
    connection.getDevices = jasmine
      .createSpy()
      .and.returnValue([{id: '35562', model: 'Pixel 6', authorized: false}]);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.innerHTML).toContain('unauthorized');
    expect(el.innerHTML).toContain('screen_lock_portrait');
  });

  it('detects changes in devices', async () => {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.IDLE);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.devices-connecting'));
    expect(el.textContent).toContain('No devices detected');

    connection.getDevices = jasmine
      .createSpy()
      .and.returnValue([{id: '35562', model: 'Pixel 6', authorized: true}]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.textContent).toContain(
      'Select a device: smartphone  Pixel 6 (35562)',
    );
  });

  it('displays trace collection config elements', () => {
    goToConfigSection();

    const el = assertDefined(
      htmlElement.querySelector('.trace-collection-config'),
    );
    expect(el.innerHTML).toContain('smartphone');
    expect(el.innerHTML).toContain('Pixel 6');
    expect(el.innerHTML).toContain('35562');

    const traceSection = htmlElement.querySelector('.trace-section');
    expect(traceSection).toBeTruthy();

    const dumpSection = htmlElement.querySelector('.dump-section');
    expect(dumpSection).toBeTruthy();
  });

  it('start trace button works as expected', () => {
    goToConfigSection();

    const spy = spyOn(assertDefined(component.adbConnection), 'startTrace');
    clickStartTraceButton();
    expect(spy).toHaveBeenCalled();
  });

  it('dump state button works as expected', () => {
    goToConfigSection();

    const spy = spyOn(assertDefined(component.adbConnection), 'dumpState');
    const dump = assertDefined(
      htmlElement.querySelector('.dump-btn button'),
    ) as HTMLButtonElement;
    dump.click();
    expect(spy).toHaveBeenCalled();
  });

  it('change device button works as expected', () => {
    goToConfigSection();
    expect(component.getSelectedDevice()).toBeDefined();

    const spy = spyOn(
      assertDefined(component.adbConnection),
      'restartConnection',
    );

    const change = assertDefined(
      htmlElement.querySelector('.change-btn'),
    ) as HTMLButtonElement;
    change.click();

    expect(spy).toHaveBeenCalled();
  });

  it('fetch existing traces button emits files and restarts connection if no files found', async () => {
    const connection = assertDefined(component.adbConnection);
    const fetchSpy = spyOn(
      connection,
      'fetchLastTracingSessionData',
    ).and.returnValue(Promise.resolve([] as File[]));
    const emitSpy = spyOn(component.filesCollected, 'emit');
    const restartSpy = spyOn(connection, 'restartConnection');
    goToConfigSection();

    const fetchButton = assertDefined(
      htmlElement.querySelector('.fetch-btn'),
    ) as HTMLButtonElement;

    fetchButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fetchSpy).toHaveBeenCalledTimes(1);
    expect(emitSpy).toHaveBeenCalledTimes(1);
    expect(restartSpy).toHaveBeenCalledTimes(1);
  });

  it('displays unknown error message', () => {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.ERROR);
    fixture.detectChanges();

    const testErrorMessage = 'bad things are happening';
    assertDefined(connection).getErrorText = jasmine
      .createSpy()
      .and.returnValue(testErrorMessage);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.unknown-error'));
    expect(el.innerHTML).toContain('Error:');
    expect(el.innerHTML).toContain(testErrorMessage);

    const spy = spyOn(connection, 'restartConnection').and.callThrough();
    const retryButton = assertDefined(
      htmlElement.querySelector('.retry-btn'),
    ) as HTMLButtonElement;
    retryButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('displays starting trace elements', () => {
    goToConfigSection();

    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.STARTING_TRACE);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.starting-trace'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Starting trace...');

    const endButton = assertDefined(
      el.querySelector('.end-btn button'),
    ) as HTMLButtonElement;
    expect(endButton.disabled).toBeTrue();
  });

  it('displays tracing elements', () => {
    goToConfigSection();

    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.TRACING);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.tracing'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Tracing...');
    expect(progress.innerHTML).toContain('cable');

    const spy = spyOn(connection, 'endTrace');
    const endButton = assertDefined(
      el.querySelector('.end-btn button'),
    ) as HTMLButtonElement;
    expect(endButton.disabled).toBeFalse();
    endButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('displays loading data elements', () => {
    goToConfigSection();

    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.LOADING_DATA);
    fixture.detectChanges();

    const el = assertDefined(htmlElement.querySelector('.load-data'));
    const progress = assertDefined(el.querySelector('load-progress'));
    expect(progress.innerHTML).toContain('Fetching...');

    const endButton = assertDefined(
      el.querySelector('.end-btn button'),
    ) as HTMLButtonElement;
    expect(endButton.disabled).toBeTrue();
  });

  it('starts traces after IME warning dialog', async () => {
    const spy = spyOn(assertDefined(component.adbConnection), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const buttons = dialog.querySelectorAll('.warning-action-buttons button');
    (buttons.item(buttons.length - 1) as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).toHaveBeenCalled();
  });

  it('goes back to edit config display after IME warning dialog', async () => {
    const spy = spyOn(assertDefined(component.adbConnection), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const button = assertDefined(
      dialog.querySelector('.warning-action-buttons button'),
    ) as HTMLElement;
    button.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(spy).not.toHaveBeenCalled();
    expect(htmlElement.querySelector('trace-config')).toBeTruthy();
  });

  it('does not show IME warning dialog again in same session if user selects "Do not show again"', async () => {
    const spy = spyOn(assertDefined(component.adbConnection), 'startTrace');
    goToConfigSection();
    const dialog = await openAndReturnDialog();

    const option = assertDefined(
      dialog.querySelector('.warning-action-boxes mat-checkbox input'),
    ) as HTMLInputElement;
    option.checked = true;
    option.click();
    fixture.detectChanges();

    const button = assertDefined(
      dialog.querySelector('.warning-action-buttons button'),
    ) as HTMLElement;
    button.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(spy).not.toHaveBeenCalled();
    expect(htmlElement.querySelector('trace-config')).toBeTruthy();

    await clickStartTraceButton();

    expect(spy).toHaveBeenCalled();
    expect(document.querySelector('warning-dialog')).toBeNull();
  });

  function goToConfigSection() {
    const connection = assertDefined(component.adbConnection);
    connection.getState = jasmine
      .createSpy()
      .and.returnValue(ConnectionState.IDLE);

    connection.getDevices = jasmine.createSpy().and.returnValue([mockDevice]);
    fixture.detectChanges();
    const device = assertDefined(
      htmlElement.querySelector('.available-device'),
    ) as HTMLElement;
    device.click();
    fixture.detectChanges();
  }

  function updateTraceConfigToInvalidIMEFrameMapping() {
    const config = assertDefined(component.traceConfig);
    config['ime'].run = true;
    config['layers_trace'].run = false;
  }

  async function clickStartTraceButton() {
    const start = assertDefined(
      htmlElement.querySelector('.start-btn button'),
    ) as HTMLButtonElement;
    start.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function openAndReturnDialog(): Promise<HTMLElement> {
    updateTraceConfigToInvalidIMEFrameMapping();
    await clickStartTraceButton();
    const dialog = assertDefined(
      document.querySelector('warning-dialog'),
    ) as HTMLElement;
    expect(dialog.textContent).toContain(
      'Cannot build frame mapping for IME with selected traces',
    );
    return dialog;
  }
});

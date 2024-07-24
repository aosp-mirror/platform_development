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
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {MockStorage} from 'test/unit/mock_storage';
import {
  TraceConfigurationMap,
  TRACES,
} from 'trace_collection/trace_collection_utils';
import {AdbProxyComponent} from './adb_proxy_component';
import {CollectTracesComponent} from './collect_traces_component';
import {TraceConfigComponent} from './trace_config_component';
import {WebAdbComponent} from './web_adb_component';

describe('CollectTracesComponent', () => {
  let fixture: ComponentFixture<CollectTracesComponent>;
  let component: CollectTracesComponent;
  let htmlElement: HTMLElement;

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
      ],
      providers: [MatSnackBar],
      declarations: [
        CollectTracesComponent,
        AdbProxyComponent,
        WebAdbComponent,
        TraceConfigComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(CollectTracesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.isAdbProxy = true;
    component.storage = new MockStorage();
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
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
        layers_dump: {
          name: 'Surface Flinger',
          isTraceCollection: undefined,
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
    expect(htmlElement.querySelector('.title')?.innerHTML).toContain(
      'Collect Traces',
    );
  });

  it('displays connecting message', () => {
    assertDefined(component.connect).isConnectingState = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.connecting-message')?.innerHTML,
    ).toContain('Connecting...');
  });

  it('displays adb set up', async () => {
    assertDefined(component.connect).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      expect(htmlElement.querySelector('.set-up-adb')).toBeTruthy();
      const proxyTab: HTMLButtonElement | null =
        htmlElement.querySelector('.proxy-tab');
      expect(proxyTab).toBeInstanceOf(HTMLButtonElement);
      const webTab: HTMLButtonElement | null =
        htmlElement.querySelector('.web-tab');
      expect(webTab).toBeInstanceOf(HTMLButtonElement);
    });
  });

  it('displays adb proxy element', async () => {
    assertDefined(component.connect).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    component.isAdbProxy = true;
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      expect(htmlElement.querySelector('adb-proxy')).toBeTruthy();
      expect(htmlElement.querySelector('web-adb')).toBeFalsy();
    });
  });

  it('displays web adb element', async () => {
    assertDefined(component.connect).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    component.isAdbProxy = false;
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      expect(htmlElement.querySelector('adb-proxy')).toBeFalsy();
      expect(htmlElement.querySelector('web-adb')).toBeTruthy();
    });
  });

  it('changes to adb workflow tab', async () => {
    assertDefined(component.connect).adbSuccess = jasmine
      .createSpy()
      .and.returnValue(false);
    component.isAdbProxy = true;
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const webTab: HTMLButtonElement | null =
        htmlElement.querySelector('.web-tab');
      expect(webTab).toBeInstanceOf(HTMLButtonElement);
      webTab?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(component.displayWebAdbTab).toHaveBeenCalled();
      });
    });
  });

  it('displays no connected devices', async () => {
    assertDefined(component.connect).isDevicesState = jasmine
      .createSpy()
      .and.returnValue(true);
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({});
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.devices-connecting');
      expect(el).toBeTruthy();
      expect(el?.innerHTML).toContain('No devices detected');
    });
  });

  it('displays connected authorised devices', async () => {
    assertDefined(component.connect).isDevicesState = jasmine
      .createSpy()
      .and.returnValue(true);
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': {model: 'Pixel 6', authorised: true}});
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.devices-connecting');
      expect(el).toBeTruthy();
      expect(el?.innerHTML).toContain('Connected devices:');
      expect(el?.innerHTML).toContain('Pixel 6');
      expect(el?.innerHTML).toContain('smartphone');
    });
  });

  it('displays connected unauthorised devices', async () => {
    assertDefined(component.connect).isDevicesState = jasmine
      .createSpy()
      .and.returnValue(true);
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': {model: 'Pixel 6', authorised: false}});
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.devices-connecting');
      expect(el).toBeTruthy();
      expect(el?.innerHTML).toContain('Connected devices:');
      expect(el?.innerHTML).toContain('unauthorised');
      expect(el?.innerHTML).toContain('screen_lock_portrait');
    });
  });

  it('displays trace collection config elements', async () => {
    assertDefined(component.connect).isStartTraceState = jasmine
      .createSpy()
      .and.returnValue(true);
    const mock = {model: 'Pixel 6', authorised: true};
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': mock});
    assertDefined(component.connect).selectedDevice = jasmine
      .createSpy()
      .and.returnValue(mock);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.trace-collection-config');
      expect(el).toBeTruthy();
      expect(el?.innerHTML).toContain('smartphone');
      expect(el?.innerHTML).toContain('Pixel 6');
      expect(el?.innerHTML).toContain('35562');

      const traceSection = htmlElement.querySelector('.trace-section');
      expect(traceSection).toBeTruthy();

      const dumpSection = htmlElement.querySelector('.dump-section');
      expect(dumpSection).toBeTruthy();
    });
  });

  it('start trace button works as expected', async () => {
    assertDefined(component.connect).isStartTraceState = jasmine
      .createSpy()
      .and.returnValue(true);
    const mock = {model: 'Pixel 6', authorised: true};
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': mock});
    assertDefined(component.connect).selectedDevice = jasmine
      .createSpy()
      .and.returnValue(mock);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const start: HTMLButtonElement | null =
        htmlElement.querySelector('.start-btn');
      expect(start).toBeInstanceOf(HTMLButtonElement);
      start?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(component.startTracing).toHaveBeenCalled();
        expect(assertDefined(component.connect).startTrace).toHaveBeenCalled();
      });
    });
  });

  it('dump state button works as expected', async () => {
    assertDefined(component.connect).isStartTraceState = jasmine
      .createSpy()
      .and.returnValue(true);
    const mock = {model: 'Pixel 6', authorised: true};
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': mock});
    assertDefined(component.connect).selectedDevice = jasmine
      .createSpy()
      .and.returnValue(mock);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const dump: HTMLButtonElement | null =
        htmlElement.querySelector('.dump-btn');
      expect(dump).toBeInstanceOf(HTMLButtonElement);
      dump?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(component.dumpState).toHaveBeenCalled();
        expect(assertDefined(component.connect).dumpState).toHaveBeenCalled();
      });
    });
  });

  it('change device button works as expected', async () => {
    assertDefined(component.connect).isStartTraceState = jasmine
      .createSpy()
      .and.returnValue(true);
    const mock = {model: 'Pixel 6', authorised: true};
    assertDefined(component.connect).devices = jasmine
      .createSpy()
      .and.returnValue({'35562': mock});
    assertDefined(component.connect).selectedDevice = jasmine
      .createSpy()
      .and.returnValue(mock);
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const change: HTMLButtonElement | null =
        htmlElement.querySelector('.change-btn');
      expect(change).toBeInstanceOf(HTMLButtonElement);
      change?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(
          assertDefined(component.connect).resetLastDevice,
        ).toHaveBeenCalled();
      });
    });
  });

  it('displays unknown error message', () => {
    assertDefined(component.connect).isErrorState = jasmine
      .createSpy()
      .and.returnValue(true);
    assertDefined(component.connect).proxy!.errorText =
      'bad things are happening';
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.unknown-error');
      expect(el?.innerHTML).toContain('Error:');
      expect(el?.innerHTML).toContain('bad things are happening');
      const retry: HTMLButtonElement | null =
        htmlElement.querySelector('.retry-btn');
      expect(retry).toBeInstanceOf(HTMLButtonElement);
      retry?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(assertDefined(component.connect).restart).toHaveBeenCalled();
      });
    });
  });

  it('displays end tracing elements', () => {
    assertDefined(component.connect).isEndTraceState = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      const el = htmlElement.querySelector('.end-tracing');
      expect(el?.innerHTML).toContain('Tracing...');
      expect(htmlElement.querySelector('mat-progress-bar')).toBeTruthy();

      const end: HTMLButtonElement | null = htmlElement.querySelector('.end');
      expect(end).toBeInstanceOf(HTMLButtonElement);
      end?.dispatchEvent(new Event('click'));
      fixture.whenStable().then(() => {
        expect(component.endTrace).toHaveBeenCalled();
        expect(assertDefined(component.connect).endTrace).toHaveBeenCalled();
      });
    });
  });

  it('displays loading data elements', () => {
    assertDefined(component.connect).isLoadDataState = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();
    fixture.whenStable().then(() => {
      expect(htmlElement.querySelector('.load-data')?.innerHTML).toContain(
        'Loading data...',
      );
      expect(htmlElement.querySelector('mat-progress-bar')).toBeTruthy();
    });
  });
});

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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {Store} from 'common/store';
import {TraceType} from 'trace/trace_type';
import {TraceConfigComponent} from './trace_config_component';

describe('TraceConfigComponent', () => {
  let fixture: ComponentFixture<TraceConfigComponent>;
  let component: TraceConfigComponent;
  let htmlElement: HTMLElement;
  let configChangeSpy: jasmine.Spy;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatDividerModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        MatTooltipModule,
        MatButtonModule,
      ],
      declarations: [TraceConfigComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(TraceConfigComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    configChangeSpy = spyOn(component.traceConfigChange, 'emit');
    await setComponentInputs(component);
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays config alphabetically by name', () => {
    expect(
      Array.from(
        htmlElement.querySelectorAll<HTMLElement>('.trace-checkbox'),
      ).map((box) => box.textContent?.trim()),
    ).toEqual([
      'layers_trace',
      'multiple_selection_trace',
      'optional_multiple_selection_trace',
      'optional_selection_trace',
      'unavailable_trace',
      'window_trace',
    ]);
  });

  it('displays advanced config alphabetically by name', () => {
    expect(
      Array.from(
        htmlElement.querySelectorAll<HTMLElement>('.config-heading'),
      ).map((box) => box.textContent?.trim()),
    ).toEqual([
      'layers_trace configuration',
      'multiple_selection_trace configuration',
      'optional_multiple_selection_trace configuration',
      'optional_selection_trace configuration',
    ]);
  });

  it('applies stored config and emits event on init', async () => {
    expect(
      assertDefined(component.traceConfig)['layers_trace'].enabled,
    ).toBeTrue();
    const inputElement = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.trace-checkbox input'),
    );
    inputElement.click();
    fixture.detectChanges();
    expect(
      assertDefined(component.traceConfig)['layers_trace'].enabled,
    ).toBeFalse();

    const newFixture = TestBed.createComponent(TraceConfigComponent);
    const newComponent = newFixture.componentInstance;
    const spy = spyOn(newComponent.traceConfigChange, 'emit');
    await setComponentInputs(
      newComponent,
      newFixture,
      assertDefined(component.storage),
    );
    expect(spy).toHaveBeenCalledTimes(1);
    expect(
      assertDefined(newComponent.traceConfig)['layers_trace'].enabled,
    ).toBeFalse();
  });

  it('handles proxy initial trace config', async () => {
    const newFixture = TestBed.createComponent(TraceConfigComponent);
    const newComponent = newFixture.componentInstance;
    const spy = spyOn(newComponent.traceConfigChange, 'emit');

    newComponent.title = 'Targets';
    newComponent.initialTraceConfig = component.traceConfig;
    newComponent.traceConfigStoreKey = 'TestConfigSettings';
    newComponent.storage = component.storage;
    await detectNgModelChanges(newFixture);
    newFixture.detectChanges();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('trace checkbox enabled by default', () => {
    const traceKey = 'layers_trace';
    configChangeSpy.calls.reset();
    const config = assertDefined(component.traceConfig);

    const box = getTraceBoxForKey('layers_trace');
    const inputElement = assertDefined(
      box.querySelector<HTMLInputElement>('input'),
    );

    expect(box.textContent).toContain(traceKey);
    expect(inputElement.checked).toBeTrue();
    expect(inputElement.ariaChecked).toEqual('true');
    expect(config[traceKey].enabled).toBeTrue();

    inputElement.click();
    fixture.detectChanges();
    expect(inputElement.checked).toBeFalse();
    expect(inputElement.ariaChecked).toEqual('false');
    expect(config[traceKey].enabled).toBeFalse();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('trace checkbox not enabled by default', () => {
    const traceKey = 'window_trace';
    configChangeSpy.calls.reset();
    const config = assertDefined(component.traceConfig);

    const box = getTraceBoxForKey(traceKey);
    const inputElement = assertDefined(
      box.querySelector<HTMLInputElement>('input'),
    );

    expect(box.textContent).toContain(traceKey);
    expect(inputElement.checked).toBeFalse();
    expect(inputElement.ariaChecked).toEqual('false');
    expect(config[traceKey].enabled).toBeFalse();

    inputElement.click();
    fixture.detectChanges();
    expect(inputElement.checked).toBeTrue();
    expect(inputElement.ariaChecked).toEqual('true');
    expect(config[traceKey].enabled).toBeTrue();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('disables checkbox for unavailable trace', () => {
    const traceKey = 'unavailable_trace';
    const box = getTraceBoxForKey(traceKey);
    const inputElement = assertDefined(
      box.querySelector<HTMLInputElement>('input'),
    );
    expect(inputElement.disabled).toBeTrue();
    expect(box.textContent).toContain(traceKey);
  });

  it('enable and select configs show', () => {
    const enable_config_opt = assertDefined(
      htmlElement.querySelector('.enable-config-opt'),
    );
    expect(enable_config_opt.innerHTML).toContain('trace buffers');
    expect(enable_config_opt.innerHTML).not.toContain('tracing level');

    const selection_config_opt = assertDefined(
      htmlElement.querySelector('.selection-config-opt'),
    );
    expect(selection_config_opt.innerHTML).not.toContain('trace buffers');
    expect(selection_config_opt.innerHTML).toContain('tracing level');
  });

  it('changing enable config model value causes box to change', async () => {
    const inputElement = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.enable-config input'),
    );
    assertDefined(
      assertDefined(component.traceConfig)['layers_trace'].config,
    ).enableConfigs[0].enabled = false;
    await detectNgModelChanges();
    expect(inputElement.checked).toBeFalse();
    expect(inputElement.ariaChecked).toEqual('false');

    assertDefined(
      assertDefined(component.traceConfig)['layers_trace'].config,
    ).enableConfigs[0].enabled = true;
    await detectNgModelChanges();
    expect(inputElement.checked).toBeTrue();
    expect(inputElement.ariaChecked).toEqual('true');
  });

  it('changing enable config by DOM interaction emits event', async () => {
    configChangeSpy.calls.reset();
    const inputElement = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.enable-config input'),
    );
    inputElement.click();
    fixture.detectChanges();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('changing selected config causes select to change', async () => {
    configChangeSpy.calls.reset();
    await openSelect(0);

    const panel = assertDefined(
      document.querySelector<HTMLElement>('.mat-select-panel'),
    );
    expect(panel.querySelector('.user-option')).toBeNull();

    clickFirstOption(panel);
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('clicking None button clears optional single selection config value', async () => {
    configChangeSpy.calls.reset();
    await openSelect(getIndexForConfigKey('optional_selection_trace'));

    const panel = assertDefined(
      document.querySelector<HTMLElement>('.mat-select-panel'),
    );
    clickFirstOption(panel);
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
    expect(
      configChangeSpy.calls.mostRecent().args[0]['optional_selection_trace']
        .config.selectionConfigs[0].value,
    ).toEqual('12345');

    const noneButton = assertDefined(
      panel.querySelectorAll<HTMLElement>('.user-option').item(0),
    );
    noneButton.click();
    fixture.detectChanges();
    expect(configChangeSpy).toHaveBeenCalledTimes(2);
    expect(
      configChangeSpy.calls.mostRecent().args[0]['optional_selection_trace']
        .config.selectionConfigs[0].value,
    ).toEqual('');
  });

  it('clicking All button selects or clears all options for multiple selection config', async () => {
    configChangeSpy.calls.reset();
    await openSelect(getIndexForConfigKey('multiple_selection_trace'));

    const panel = assertDefined(
      document.querySelector<HTMLElement>('.mat-select-panel'),
    );
    const allButton = assertDefined(
      panel.querySelector<HTMLElement>('.user-option'),
    );
    allButton.click();
    fixture.detectChanges();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
    expect(
      configChangeSpy.calls.mostRecent().args[0]['multiple_selection_trace']
        .config.selectionConfigs[0].value,
    ).toEqual(['12345', '67890']);

    allButton.click();
    fixture.detectChanges();
    expect(configChangeSpy).toHaveBeenCalledTimes(2);
    expect(
      configChangeSpy.calls.mostRecent().args[0]['multiple_selection_trace']
        .config.selectionConfigs[0].value,
    ).toEqual([]);
  });

  it('stabilizes tooltip position', async () => {
    await openSelect(getIndexForConfigKey('optional_selection_trace'));

    const panel = assertDefined(
      document.querySelector<HTMLElement>('.mat-select-panel'),
    );
    const options = panel.querySelectorAll<HTMLElement>('mat-option');

    const shortOption = options.item(0);
    shortOption.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(
      document.querySelector<HTMLElement>('.mat-tooltip-panel'),
    ).toBeNull();

    const longOption = options.item(1);
    longOption.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    const tooltipPanel = assertDefined(
      document.querySelector<HTMLElement>('.mat-tooltip-panel'),
    );
    expect(tooltipPanel?.style.top.length).toBeGreaterThan(0);
    expect(tooltipPanel?.style.left.length).toBeGreaterThan(0);
  });

  async function setComponentInputs(
    c: TraceConfigComponent,
    f: ComponentFixture<TraceConfigComponent> = fixture,
    storage: Store = new InMemoryStorage(),
  ) {
    c.title = 'Targets';
    c.initialTraceConfig = {
      layers_trace: {
        name: 'layers_trace',
        enabled: true,
        available: true,
        types: [TraceType.SURFACE_FLINGER],
        config: {
          enableConfigs: [
            {
              name: 'trace buffers',
              key: 'tracebuffers',
              enabled: true,
            },
          ],
          selectionConfigs: [
            {
              key: 'tracinglevel',
              name: 'tracing level',
              options: ['verbose', 'debug', 'critical'],
              value: 'debug',
            },
          ],
        },
      },
      window_trace: {
        name: 'window_trace',
        enabled: false,
        available: true,
        types: [TraceType.WINDOW_MANAGER],
        config: undefined,
      },
      unavailable_trace: {
        name: 'unavailable_trace',
        enabled: false,
        available: false,
        types: [TraceType.TEST_TRACE_STRING],
        config: undefined,
      },
      optional_selection_trace: {
        name: 'optional_selection_trace',
        enabled: true,
        available: true,
        types: [TraceType.TEST_TRACE_STRING],
        config: {
          enableConfigs: [],
          selectionConfigs: [
            {
              key: 'displays',
              name: 'displays',
              options: ['12345', 'long_option'.repeat(20)],
              value: '',
              optional: true,
            },
          ],
        },
      },
      multiple_selection_trace: {
        name: 'multiple_selection_trace',
        enabled: true,
        available: true,
        types: [TraceType.TEST_TRACE_STRING],
        config: {
          enableConfigs: [],
          selectionConfigs: [
            {
              key: 'displays',
              name: 'displays',
              options: ['12345', '67890'],
              value: [],
            },
          ],
        },
      },
      optional_multiple_selection_trace: {
        name: 'optional_multiple_selection_trace',
        enabled: true,
        available: true,
        types: [TraceType.TEST_TRACE_STRING],
        config: {
          enableConfigs: [],
          selectionConfigs: [
            {
              key: 'displays',
              name: 'displays',
              options: ['12345', '67890'],
              value: [],
              optional: true,
            },
          ],
        },
      },
    };
    c.traceConfigStoreKey = 'TestConfigSettings';
    c.storage = storage;
    await detectNgModelChanges(f);
    f.detectChanges();
  }

  async function detectNgModelChanges(
    f: ComponentFixture<TraceConfigComponent> = fixture,
  ) {
    f.detectChanges();
    await f.whenStable();
    f.detectChanges();
  }

  function getTraceBoxForKey(traceKey: string): HTMLElement {
    const index = component
      .getSortedTraceKeys()
      .findIndex((key) => key === traceKey);
    return assertDefined(
      htmlElement.querySelectorAll<HTMLElement>('.trace-checkbox').item(index),
    );
  }

  function getIndexForConfigKey(configKey: string): number {
    return component
      .getSortedConfigKeys()
      .findIndex((key) => key === configKey);
  }

  async function openSelect(index: number) {
    const selectTrigger = assertDefined(
      htmlElement
        .querySelectorAll<HTMLElement>('.mat-select-trigger')
        .item(index),
    );
    selectTrigger.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function clickFirstOption(panel: HTMLElement) {
    const newOption = assertDefined(
      panel.querySelector<HTMLElement>('mat-option'),
    );
    newOption.click();
    fixture.detectChanges();
  }
});

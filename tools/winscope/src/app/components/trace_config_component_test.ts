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
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
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
    configChangeSpy.calls.reset();
    const config = assertDefined(component.traceConfig);

    const box = assertDefined(htmlElement.querySelector('.trace-checkbox'));
    const inputElement = assertDefined(
      box.querySelector<HTMLInputElement>('input'),
    );

    expect(box.textContent).toContain('layers_trace');
    expect(inputElement.checked).toBeTrue();
    expect(inputElement.ariaChecked).toEqual('true');
    expect(config['layers_trace'].enabled).toBeTrue();

    inputElement.click();
    fixture.detectChanges();
    expect(inputElement.checked).toBeFalse();
    expect(inputElement.ariaChecked).toEqual('false');
    expect(config['layers_trace'].enabled).toBeFalse();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('trace checkbox not enabled by default', () => {
    configChangeSpy.calls.reset();
    const config = assertDefined(component.traceConfig);

    const box = assertDefined(
      htmlElement.querySelectorAll('.trace-checkbox').item(1),
    );
    const inputElement = assertDefined(
      box.querySelector<HTMLInputElement>('input'),
    );

    expect(box.textContent).toContain('window_trace');
    expect(inputElement.checked).toBeFalse();
    expect(inputElement.ariaChecked).toEqual('false');
    expect(config['window_trace'].enabled).toBeFalse();

    inputElement.click();
    fixture.detectChanges();
    expect(inputElement.checked).toBeTrue();
    expect(inputElement.ariaChecked).toEqual('true');
    expect(config['window_trace'].enabled).toBeTrue();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  it('disables checkbox for unavailable trace', () => {
    const boxes = htmlElement.querySelectorAll('.trace-checkbox');

    const unavailableBox = assertDefined(boxes.item(2));
    const unavailableInput = assertDefined(
      unavailableBox.querySelector('input'),
    );
    expect(unavailableInput.disabled).toBeTrue();
    expect(unavailableBox.textContent).toContain('unavailable_trace');
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

  it('check that changing selected config causes select to change', async () => {
    configChangeSpy.calls.reset();
    const selectTrigger = assertDefined(
      htmlElement.querySelector('.mat-select-trigger'),
    );
    (selectTrigger as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    const newOption = assertDefined(
      document.querySelector<HTMLElement>('mat-option'),
    );
    newOption.click();
    fixture.detectChanges();
    expect(configChangeSpy).toHaveBeenCalledTimes(1);
  });

  async function setComponentInputs(
    c: TraceConfigComponent,
    f: ComponentFixture<TraceConfigComponent> = fixture,
    storage: Storage = new InMemoryStorage(),
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
});

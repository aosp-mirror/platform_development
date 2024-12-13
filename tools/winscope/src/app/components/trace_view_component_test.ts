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

import {OverlayModule} from '@angular/cdk/overlay';
import {CommonModule} from '@angular/common';
import {Component, CUSTOM_ELEMENTS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTabsModule} from '@angular/material/tabs';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {Viewer, ViewType} from 'viewers/viewer';
import {ViewerStub} from 'viewers/viewer_stub';
import {TraceViewComponent} from './trace_view_component';

describe('TraceViewComponent', () => {
  const traceSf = UnitTestUtils.makeEmptyTrace(TraceType.SURFACE_FLINGER);
  const traceWm = new TraceBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setEntries([{}])
    .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
    .setDescriptors(['file_1', 'file_1'])
    .build();
  const traceSr = UnitTestUtils.makeEmptyTrace(TraceType.SCREEN_RECORDING);
  const traceProtolog = UnitTestUtils.makeEmptyTrace(TraceType.PROTO_LOG);

  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent, TraceViewComponent],
      imports: [
        CommonModule,
        MatCardModule,
        MatDividerModule,
        MatTabsModule,
        MatTooltipModule,
        OverlayModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        BrowserAnimationsModule,
        MatInputModule,
        ReactiveFormsModule,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    htmlElement = fixture.nativeElement;
    component = fixture.componentInstance;
    component.viewers = [
      new ViewerStub('Title0', 'Content0', traceSf, ViewType.TRACE_TAB),
      new ViewerStub('Title1', 'Content1', traceWm, ViewType.TRACE_TAB),
      new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
      new ViewerStub('Title3', 'Content3', traceProtolog, ViewType.TRACE_TAB),
    ];
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates viewer tabs', () => {
    const tabs = htmlElement.querySelectorAll('.tab');
    expect(tabs.length).toEqual(3);
    expect(tabs.item(0).textContent).toContain('Title0');
    expect(tabs.item(1).textContent).toContain('Title1 Dump');
  });

  it('creates viewer overlay', () => {
    const overlayContainer = assertDefined(
      htmlElement.querySelector('.overlay-container'),
    );
    expect(overlayContainer.textContent).toContain('Content2');
  });

  it('throws error if more than one overlay present', () => {
    expect(() => {
      component.viewers = [
        new ViewerStub('Title0', 'Content0', traceSf, ViewType.TRACE_TAB),
        new ViewerStub('Title1', 'Content1', traceWm, ViewType.OVERLAY),
        new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
      ];
      fixture.detectChanges();
    }).toThrowError();
  });

  it('switches view on click', () => {
    const tabButtons = htmlElement.querySelectorAll<HTMLElement>('.tab');

    // Initially tab 0
    fixture.detectChanges();
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    tabButtons.item(1).click();
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    tabButtons.item(0).click();
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it("emits 'view switched' events", () => {
    const traceViewComponent = assertDefined(component.traceViewComponent);
    const tabButtons = htmlElement.querySelectorAll<HTMLElement>('.tab');

    const emitAppEvent = jasmine.createSpy();
    traceViewComponent.setEmitEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    tabButtons.item(1).click();
    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );

    tabButtons.item(0).click();
    expect(emitAppEvent).toHaveBeenCalledTimes(2);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );
  });

  it("handles 'view switch' requests", async () => {
    const traceViewComponent = assertDefined(component.traceViewComponent);

    // Initially tab 0
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    await traceViewComponent.onWinscopeEvent(
      new TabbedViewSwitchRequest(traceWm),
    );
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    await traceViewComponent.onWinscopeEvent(
      new TabbedViewSwitchRequest(traceSf),
    );
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it('emits TabbedViewSwitched event on viewer changes', () => {
    const traceViewComponent = assertDefined(component.traceViewComponent);
    const emitAppEvent = jasmine.createSpy();
    traceViewComponent.setEmitEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    component.viewers = [new ViewerStub('Title1', 'Content1', traceWm)];
    fixture.detectChanges();

    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );
  });

  it('disables filter presets button for viewers without presets', () => {
    const filterPresets = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.filter-presets'),
    );
    expect(filterPresets.textContent).toContain('Filter Presets');
    expect(filterPresets.disabled).toBeFalse();
    const tabButtons = htmlElement.querySelectorAll<HTMLElement>('.tab');
    tabButtons.item(2).click();
    fixture.detectChanges();
    expect(filterPresets.disabled).toBeTrue();
  });

  it('saves preset by button', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    openFilterPresets();

    const overlayPanel = assertDefined(
      document.querySelector('.overlay-panel'),
    );
    const existingPresets = assertDefined(
      overlayPanel.querySelector('.existing-presets-section'),
    );
    expect(existingPresets.textContent).toContain('No existing presets found');

    const saveButton = assertDefined(
      overlayPanel.querySelector<HTMLButtonElement>('.save-field button'),
    );
    expect(saveButton.disabled).toBeTrue();

    const inputEl = assertDefined(
      overlayPanel.querySelector<HTMLInputElement>('.save-field input'),
    );
    updateInputField(inputEl, 'Test Preset');
    saveButton.click();
    fixture.detectChanges();

    expect(emitAppEvent).toHaveBeenCalledWith(
      new FilterPresetSaveRequest(
        'Test Preset.Surface Flinger',
        TraceType.SURFACE_FLINGER,
      ),
    );
    expect(existingPresets.textContent).toContain('Test Preset');
    expect(inputEl.value).toEqual('');
    expect(saveButton.disabled).toBeTrue();
  });

  it('saves preset by keydown', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    openFilterPresets();

    const overlayPanel = assertDefined(
      document.querySelector('.overlay-panel'),
    );

    const inputEl = assertDefined(
      overlayPanel.querySelector<HTMLInputElement>('.save-field input'),
    );
    inputEl.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));
    fixture.detectChanges();
    expect(emitAppEvent).not.toHaveBeenCalled();

    updateInputField(inputEl, 'Test Preset');
    inputEl.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));
    fixture.detectChanges();

    expect(emitAppEvent).toHaveBeenCalledWith(
      new FilterPresetSaveRequest(
        'Test Preset.Surface Flinger',
        TraceType.SURFACE_FLINGER,
      ),
    );
  });

  it('saves preset between sessions', () => {
    savePresetByButton('Test Preset');

    component.showSecondComponent = true;
    fixture.detectChanges();

    openFilterPresets();
    const existingPresets = assertDefined(
      document.querySelector('.overlay-panel .existing-presets-section'),
    );
    expect(existingPresets.textContent).toContain('Test Preset');
  });

  it('deletes preset', () => {
    savePresetByButton('Test Preset');
    const saveButton = assertDefined(
      document.querySelector<HTMLButtonElement>('.save-field button'),
    );
    updateInputField(
      assertDefined(
        document.querySelector<HTMLInputElement>('.save-field input'),
      ),
      'Test Preset',
    );
    expect(saveButton.disabled).toBeTrue();

    assertDefined(
      document.querySelector<HTMLElement>('.delete-button'),
    ).click();
    fixture.detectChanges();
    expect(
      document.querySelector<HTMLElement>('.existing-presets-section')
        ?.textContent,
    ).toContain('No existing presets found');
    expect(saveButton.disabled).toBeFalse();
  });

  it('does not show presets for different trace', () => {
    savePresetByButton('Test Preset');
    closeFilterPresets();

    const tabs = htmlElement.querySelectorAll<HTMLElement>('.tab');
    tabs.item(1).click();
    fixture.detectChanges();

    openFilterPresets();
    const existingPresets = assertDefined(
      document.querySelector('.overlay-panel'),
    );
    expect(existingPresets.textContent).toContain('No existing presets found');
  });

  it('emits apply preset request', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    savePresetByButton('Test Preset');

    const preset = assertDefined(
      document.querySelector<HTMLElement>(
        '.overlay-panel .existing-preset button',
      ),
    );
    preset.click();
    fixture.detectChanges();

    expect(emitAppEvent).toHaveBeenCalledWith(
      new FilterPresetApplyRequest(
        'Test Preset.Surface Flinger',
        TraceType.SURFACE_FLINGER,
      ),
    );
  });

  it('does not show global tab first', () => {
    component.viewers = [
      new ViewerStub('Title0', 'Content0', undefined, ViewType.GLOBAL_SEARCH),
      new ViewerStub('Title1', 'Content1', traceWm, ViewType.TRACE_TAB),
    ];
    fixture.detectChanges();
    const visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');
  });

  it('shows tooltips for tabs with trace descriptors', () => {
    const tabs = htmlElement.querySelectorAll('.tab');
    const wmTab = tabs.item(1);
    wmTab.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(
      document.querySelector<HTMLElement>('.mat-tooltip-panel')?.textContent,
    ).toEqual('file_1');
    wmTab.dispatchEvent(new Event('mouseleave'));
    fixture.detectChanges();
  });

  function getVisibleTabContents() {
    const contents: HTMLElement[] = [];
    htmlElement
      .querySelectorAll<HTMLElement>('.trace-view-content div')
      .forEach((content) => {
        if (content.style.display !== 'none') {
          contents.push(content);
        }
      });
    return contents;
  }

  function savePresetByButton(presetName: string) {
    openFilterPresets();
    const overlayPanel = assertDefined(
      document.querySelector('.overlay-panel'),
    );
    const saveButton = assertDefined(
      overlayPanel.querySelector<HTMLButtonElement>('.save-field button'),
    );

    const inputEl = assertDefined(
      overlayPanel.querySelector<HTMLInputElement>('.save-field input'),
    );
    updateInputField(inputEl, presetName);
    saveButton.click();
    fixture.detectChanges();
  }

  function openFilterPresets() {
    const filterPresets = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.filter-presets'),
    );
    filterPresets.click();
    fixture.detectChanges();
  }

  function closeFilterPresets() {
    assertDefined(
      document.querySelector<HTMLElement>('.cdk-overlay-backdrop'),
    ).click();
    fixture.detectChanges();
  }

  function updateInputField(inputEl: HTMLInputElement, value: string) {
    inputEl.value = value;
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  @Component({
    selector: 'host-component',
    template: `
      <trace-view
        *ngIf="!showSecondComponent"
        [viewers]="viewers"
        [store]="store"></trace-view>

      <trace-view
        *ngIf="showSecondComponent"
        [viewers]="viewers"
        [store]="store"></trace-view>
    `,
  })
  class TestHostComponent {
    viewers: Viewer[] = [];
    store = new InMemoryStorage();
    showSecondComponent = false;

    @ViewChild(TraceViewComponent)
    traceViewComponent: TraceViewComponent | undefined;
  }
});

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
import {TestBed} from '@angular/core/testing';
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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {checkTooltips, DOMTestHelper} from 'test/unit/dom_test_utils';
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

  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

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
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.viewers = [
      new ViewerStub('Title0', 'Content0', traceSf, ViewType.TRACE_TAB),
      new ViewerStub('Title1', 'Content1', traceWm, ViewType.TRACE_TAB),
      new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
      new ViewerStub('Title3', 'Content3', traceProtolog, ViewType.TRACE_TAB),
    ];
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates viewer tabs', () => {
    const tabs = getTabs();
    expect(tabs.length).toEqual(3);
    tabs[0].checkText('Title0');
    tabs[1].checkText('Title1 Dump');
  });

  it('creates viewer overlay', () => {
    const overlayContainer = dom.get('.overlay-container');
    overlayContainer.checkText('Content2');
  });

  it('throws error if more than one overlay present', () => {
    expect(() => {
      component.viewers = [
        new ViewerStub('Title0', 'Content0', traceSf, ViewType.TRACE_TAB),
        new ViewerStub('Title1', 'Content1', traceWm, ViewType.OVERLAY),
        new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
      ];
      dom.detectChanges();
    }).toThrowError();
  });

  it('switches view on click', () => {
    const tabs = getTabs();

    // Initially tab 0
    dom.detectChanges();
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    tabs[1].click();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    tabs[1].click();
    tabs[0].click();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it("emits 'view switched' events", () => {
    const traceViewComponent = assertDefined(component.traceViewComponent);
    const tabs = getTabs();

    const emitAppEvent = jasmine.createSpy();
    traceViewComponent.setEmitEvent(emitAppEvent);
    expect(emitAppEvent).not.toHaveBeenCalled();

    tabs[1].click();
    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );

    tabs[0].click();
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
    dom.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    await traceViewComponent.onWinscopeEvent(
      new TabbedViewSwitchRequest(traceSf),
    );
    dom.detectChanges();
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
    dom.detectChanges();

    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );
  });

  it('disables filter presets button for viewers without presets', () => {
    const filterPresets = dom.get('.filter-presets');
    filterPresets.checkText('Filter Presets');
    filterPresets.checkDisabled(false);
    const tabs = getTabs();
    tabs[2].click();
    filterPresets.checkDisabled(true);
  });

  it('saves preset by button', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    openFilterPresets();

    const overlay = getOverlay();
    const existingPresets = overlay.get('.existing-presets-section');
    existingPresets.checkText('No existing presets found');

    const saveButton = overlay.get('.save-field button');
    saveButton.checkDisabled(true);

    const input = overlay.findAndDispatchInput('.save-field', 'Test Preset');
    saveButton.click();

    expect(emitAppEvent).toHaveBeenCalledWith(
      new FilterPresetSaveRequest(
        'Test Preset.Surface Flinger',
        TraceType.SURFACE_FLINGER,
      ),
    );
    existingPresets.checkText('Test Preset');
    input.checkValue('');
    saveButton.checkDisabled(true);
  });

  it('saves preset by keydown', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    openFilterPresets();

    const overlay = getOverlay();

    const inputEl = overlay.get('.save-field input');
    inputEl.keydownEnter();
    expect(emitAppEvent).not.toHaveBeenCalled();

    inputEl.dispatchInput('Test Preset');
    inputEl.keydownEnter();

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
    dom.detectChanges();

    openFilterPresets();
    const existingPresets = dom.getInDocument(
      '.overlay-panel .existing-presets-section',
    );
    existingPresets.checkText('Test Preset');
  });

  it('deletes preset', () => {
    savePresetByButton('Test Preset');
    const overlay = getOverlay();
    const saveButton = overlay.get('.save-field button');
    overlay.findAndDispatchInput('.save-field', 'Test Preset');
    saveButton.checkDisabled(true);

    dom.findAndClickInDocument('.delete-button');
    dom
      .getInDocument('.existing-presets-section')
      .checkText('No existing presets found');
    saveButton.checkDisabled(false);
  });

  it('does not show presets for different trace', () => {
    savePresetByButton('Test Preset');
    dom.findAndClickInDocument('.cdk-overlay-backdrop');
    const tabs = getTabs();
    tabs[1].click();

    openFilterPresets();
    const overlay = getOverlay();
    overlay.checkText('No existing presets found');
  });

  it('emits apply preset request', () => {
    const emitAppEvent = jasmine.createSpy();
    component.traceViewComponent?.setEmitEvent(emitAppEvent);
    savePresetByButton('Test Preset');

    dom.findAndClickInDocument('.overlay-panel .existing-preset button');
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
    dom.detectChanges();
    const visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');
  });

  it('shows tooltips for tabs with trace descriptors', async () => {
    const tabs = getTabs();
    const wmTab = tabs[1];
    await checkTooltips([wmTab], ['file_1']);
  });

  function getVisibleTabContents() {
    const contents: HTMLElement[] = [];
    dom.findAll('.trace-view-content div').forEach((content) => {
      const element = content.getHTMLElement();
      if (element.style.display !== 'none') {
        contents.push(element);
      }
    });
    return contents;
  }

  function getTabs() {
    return dom.findAll('.tab');
  }

  function getOverlay() {
    return dom.getInDocument('.overlay-panel');
  }

  function savePresetByButton(presetName: string) {
    openFilterPresets();
    const overlay = getOverlay();
    overlay.findAndDispatchInput('.save-field', presetName);
    overlay.findAndClick('.save-field button');
  }

  function openFilterPresets() {
    dom.findAndClick('.filter-presets');
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

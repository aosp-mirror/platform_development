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
import {Component, CUSTOM_ELEMENTS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatTabsModule} from '@angular/material/tabs';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {INVALID_TIME_NS} from 'common/time';
import {
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TraceType} from 'trace/trace_type';
import {Viewer, ViewType} from 'viewers/viewer';
import {ViewerStub} from 'viewers/viewer_stub';
import {TraceViewComponent} from './trace_view_component';

describe('TraceViewComponent', () => {
  const traceSf = new TraceBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setEntries([])
    .build();
  const traceWm = new TraceBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setEntries([{}])
    .setTimestamps([
      TimestampConverterUtils.makeElapsedTimestamp(INVALID_TIME_NS),
    ])
    .build();
  const traceSr = new TraceBuilder<object>()
    .setType(TraceType.SCREEN_RECORDING)
    .setEntries([])
    .build();

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
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    htmlElement = fixture.nativeElement;
    component = fixture.componentInstance;
    component.viewers = [
      new ViewerStub('Title0', 'Content0', traceSf, ViewType.TAB),
      new ViewerStub('Title1', 'Content1', traceWm, ViewType.TAB),
      new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
    ];
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates viewer tabs', () => {
    const tabs = htmlElement.querySelectorAll('.tab');
    expect(tabs.length).toEqual(2);
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
        new ViewerStub('Title0', 'Content0', traceSf, ViewType.TAB),
        new ViewerStub('Title1', 'Content1', traceWm, ViewType.OVERLAY),
        new ViewerStub('Title2', 'Content2', traceSr, ViewType.OVERLAY),
      ];
      fixture.detectChanges();
    }).toThrowError();
  });

  it('switches view on click', () => {
    const tabButtons = htmlElement.querySelectorAll('.tab');

    // Initially tab 0
    fixture.detectChanges();
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    (tabButtons[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    (tabButtons[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it("emits 'view switched' events", () => {
    const traceViewComponent = assertDefined(component.traceViewComponent);
    const tabButtons = htmlElement.querySelectorAll('.tab');

    const emitAppEvent = jasmine.createSpy();
    traceViewComponent.setEmitEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    (tabButtons[1] as HTMLButtonElement).click();
    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TABBED_VIEW_SWITCHED,
      } as WinscopeEvent),
    );

    (tabButtons[0] as HTMLButtonElement).click();
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

  const getVisibleTabContents = () => {
    const contents: HTMLElement[] = [];
    htmlElement
      .querySelectorAll('.trace-view-content div')
      .forEach((content) => {
        if ((content as HTMLElement).style.display !== 'none') {
          contents.push(content as HTMLElement);
        }
      });
    return contents;
  };

  @Component({
    selector: 'host-component',
    template: `
      <trace-view
        [viewers]="viewers"></trace-view>
    `,
  })
  class TestHostComponent {
    viewers: Viewer[] = [];

    @ViewChild(TraceViewComponent)
    traceViewComponent: TraceViewComponent | undefined;
  }
});

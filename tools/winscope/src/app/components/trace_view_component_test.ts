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
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {TraceType} from 'trace/trace_type';
import {ViewerStub} from 'viewers/viewer_stub';
import {TraceViewComponent} from './trace_view_component';

describe('TraceViewComponent', () => {
  let fixture: ComponentFixture<TraceViewComponent>;
  let component: TraceViewComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TraceViewComponent],
      imports: [CommonModule, MatCardModule, MatDividerModule],
      schemas: [NO_ERRORS_SCHEMA, CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TraceViewComponent);
    htmlElement = fixture.nativeElement;
    component = fixture.componentInstance;
    component.viewers = [
      new ViewerStub('Title0', 'Content0', [TraceType.SURFACE_FLINGER]),
      new ViewerStub('Title1', 'Content1', [TraceType.WINDOW_MANAGER]),
    ];
    component.ngOnChanges();
    fixture.detectChanges();
  });

  it('can be created', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('creates viewer tabs', () => {
    const tabs: NodeList = htmlElement.querySelectorAll('.tab');
    expect(tabs.length).toEqual(2);
    expect(tabs.item(0)!.textContent).toContain('Title0');
    expect(tabs.item(1)!.textContent).toContain('Title1');
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
    const tabButtons = htmlElement.querySelectorAll('.tab');

    const emitAppEvent = jasmine.createSpy();
    component.setEmitEvent(emitAppEvent);

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
    const tabButtons = htmlElement.querySelectorAll('.tab');

    // Initially tab 0
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    await component.onWinscopeEvent(
      new TabbedViewSwitchRequest(TraceType.WINDOW_MANAGER),
    );
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    await component.onWinscopeEvent(
      new TabbedViewSwitchRequest(TraceType.SURFACE_FLINGER),
    );
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it('emits tab set onChanges', () => {
    const emitAppEvent = jasmine.createSpy();
    component.setEmitEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    component.ngOnChanges();

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
});

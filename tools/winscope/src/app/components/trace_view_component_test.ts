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
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {AppEvent, AppEventType, TabbedViewSwitchRequest} from 'app/app_event';
import {assertDefined} from 'common/assert_utils';
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
      imports: [
        CommonModule,
        MatCardModule,
        MatDividerModule,
        FormsModule,
        ReactiveFormsModule,
        BrowserAnimationsModule,
        MatInputModule,
        MatFormFieldModule,
      ],
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
    tabButtons[1].dispatchEvent(new Event('click'));
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    tabButtons[0].dispatchEvent(new Event('click'));
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it("emits 'view switched' events", () => {
    const tabButtons = htmlElement.querySelectorAll('.tab');

    const emitAppEvent = jasmine.createSpy();
    component.setEmitAppEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    tabButtons[1].dispatchEvent(new Event('click'));
    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: AppEventType.TABBED_VIEW_SWITCHED,
      } as AppEvent)
    );

    tabButtons[0].dispatchEvent(new Event('click'));
    expect(emitAppEvent).toHaveBeenCalledTimes(2);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: AppEventType.TABBED_VIEW_SWITCHED,
      } as AppEvent)
    );
  });

  it("handles 'view switch' requests", async () => {
    const tabButtons = htmlElement.querySelectorAll('.tab');

    // Initially tab 0
    let visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');

    // Switch to tab 1
    await component.onAppEvent(new TabbedViewSwitchRequest(TraceType.WINDOW_MANAGER));
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content1');

    // Switch to tab 0
    await component.onAppEvent(new TabbedViewSwitchRequest(TraceType.SURFACE_FLINGER));
    fixture.detectChanges();
    visibleTabContents = getVisibleTabContents();
    expect(visibleTabContents.length).toEqual(1);
    expect(visibleTabContents[0].innerHTML).toEqual('Content0');
  });

  it('emits event on download button click', () => {
    const spy = spyOn(component.downloadTracesButtonClick, 'emit');

    const downloadButton = assertDefined(htmlElement.querySelector('.save-button'));
    downloadButton.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledOnceWith('winscope.zip');

    downloadButton.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('does not emit event if invalid file name chosen', () => {
    const spy = spyOn(component.downloadTracesButtonClick, 'emit');
    const downloadButton = assertDefined(htmlElement.querySelector('.save-button'));
    const inputEl = assertDefined(htmlElement.querySelector('.file-name-input-field input'));

    // all invalid file names
    updateFileNameInput(inputEl, downloadButton, 'w?in$scope');
    updateFileNameInput(inputEl, downloadButton, 'winscope.');
    updateFileNameInput(inputEl, downloadButton, 'w..scope');
    updateFileNameInput(inputEl, downloadButton, 'wins--pe');
    updateFileNameInput(inputEl, downloadButton, 'wi##cope');
    expect(spy).not.toHaveBeenCalled();

    // valid file name
    updateFileNameInput(inputEl, downloadButton, 'Winscope2');
    expect(spy).toHaveBeenCalledWith('Winscope2.zip');

    // invalid, so spy should only have been called once
    updateFileNameInput(inputEl, downloadButton, 'w^^scope');
    expect(spy).toHaveBeenCalledTimes(1);

    // all valid file names
    updateFileNameInput(inputEl, downloadButton, 'win_scope');
    expect(spy).toHaveBeenCalledWith('win_scope.zip');
    updateFileNameInput(inputEl, downloadButton, 'win-scope');
    expect(spy).toHaveBeenCalledWith('win-scope.zip');
    updateFileNameInput(inputEl, downloadButton, 'win.scope');
    expect(spy).toHaveBeenCalledWith('win.scope.zip');
    updateFileNameInput(inputEl, downloadButton, 'win.sc.ope');
    expect(spy).toHaveBeenCalledWith('win.sc.ope.zip');
  });

  it('emits tab set onChanges', () => {
    const emitAppEvent = jasmine.createSpy();
    component.setEmitAppEvent(emitAppEvent);

    expect(emitAppEvent).not.toHaveBeenCalled();

    component.ngOnChanges();

    expect(emitAppEvent).toHaveBeenCalledTimes(1);
    expect(emitAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: AppEventType.TABBED_VIEW_SWITCHED,
      } as AppEvent)
    );
  });

  const getVisibleTabContents = () => {
    const contents: HTMLElement[] = [];
    htmlElement.querySelectorAll('.trace-view-content div').forEach((content) => {
      if ((content as HTMLElement).style.display !== 'none') {
        contents.push(content as HTMLElement);
      }
    });
    return contents;
  };

  const updateFileNameInput = (inputEl: Element, button: Element, name: string) => {
    (inputEl as HTMLInputElement).value = name;
    inputEl.dispatchEvent(new Event('input'));
    button.dispatchEvent(new Event('click'));
    fixture.detectChanges();
  };
});

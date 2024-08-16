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

import {ClipboardModule} from '@angular/cdk/clipboard';
import {DragDropModule} from '@angular/cdk/drag-drop';
import {CdkMenuModule} from '@angular/cdk/menu';
import {ChangeDetectionStrategy, Component, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
} from 'app/components/bottomnav/bottom_drawer_component';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {TimeRange} from 'common/time';
import {
  ActiveTraceChanged,
  ExpandedTimelineToggled,
  TracePositionUpdate,
  WinscopeEvent,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {DefaultTimelineRowComponent} from './expanded-timeline/default_timeline_row_component';
import {ExpandedTimelineComponent} from './expanded-timeline/expanded_timeline_component';
import {TransitionTimelineComponent} from './expanded-timeline/transition_timeline_component';
import {MiniTimelineComponent} from './mini-timeline/mini_timeline_component';
import {SliderComponent} from './mini-timeline/slider_component';
import {TimelineComponent} from './timeline_component';

describe('TimelineComponent', () => {
  const time90 = TimestampConverterUtils.makeRealTimestamp(90n);
  const time100 = TimestampConverterUtils.makeRealTimestamp(100n);
  const time101 = TimestampConverterUtils.makeRealTimestamp(101n);
  const time105 = TimestampConverterUtils.makeRealTimestamp(105n);
  const time110 = TimestampConverterUtils.makeRealTimestamp(110n);
  const time112 = TimestampConverterUtils.makeRealTimestamp(112n);

  const time2000 = TimestampConverterUtils.makeRealTimestamp(2000n);
  const time3000 = TimestampConverterUtils.makeRealTimestamp(3000n);
  const time4000 = TimestampConverterUtils.makeRealTimestamp(4000n);
  const time6000 = TimestampConverterUtils.makeRealTimestamp(6000n);
  const time8000 = TimestampConverterUtils.makeRealTimestamp(8000n);

  const position90 = TracePosition.fromTimestamp(time90);
  const position100 = TracePosition.fromTimestamp(time100);
  const position105 = TracePosition.fromTimestamp(time105);
  const position110 = TracePosition.fromTimestamp(time110);
  const position112 = TracePosition.fromTimestamp(time112);

  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatSelectModule,
        MatTooltipModule,
        ReactiveFormsModule,
        BrowserAnimationsModule,
        DragDropModule,
        ClipboardModule,
        CdkMenuModule,
      ],
      declarations: [
        TestHostComponent,
        ExpandedTimelineComponent,
        DefaultTimelineRowComponent,
        MatDrawer,
        MatDrawerContainer,
        MatDrawerContent,
        MiniTimelineComponent,
        TimelineComponent,
        SliderComponent,
        TransitionTimelineComponent,
      ],
    })
      .overrideComponent(TimelineComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can be expanded', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .build();
    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();

    const timelineComponent = assertDefined(component.timeline);

    const button = assertDefined(
      htmlElement.querySelector(`.${timelineComponent.TOGGLE_BUTTON_CLASS}`),
    );

    // initially not expanded
    let expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeFalsy();

    let isExpanded = false;
    timelineComponent.setEmitEvent(async (event: WinscopeEvent) => {
      expect(event).toBeInstanceOf(ExpandedTimelineToggled);
      isExpanded = (event as ExpandedTimelineToggled).isTimelineExpanded;
    });

    button.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeTruthy();
    expect(isExpanded).toBeTrue();

    button.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeFalsy();
    expect(isExpanded).toBeFalse();
  });

  it('handles empty traces', () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.SURFACE_FLINGER, [])
      .build();
    assertDefined(assertDefined(component.timelineData)).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();

    expect(htmlElement.querySelector('.time-selector')).toBeNull();
    expect(htmlElement.querySelector('.trace-selector')).toBeNull();

    const errorMessageContainer = assertDefined(
      htmlElement.querySelector('.no-timeline-msg'),
    );
    expect(errorMessageContainer.textContent).toContain('No timeline to show!');
    expect(errorMessageContainer.textContent).toContain(
      'All loaded traces contain no timestamps.',
    );

    checkNoTimelineNavigation();
  });

  it('handles some empty traces and some with one timestamp', async () => {
    await loadTracesWithOneTimestamp();

    expect(htmlElement.querySelector('#time-selector')).toBeTruthy();
    const shownSelection = assertDefined(
      htmlElement.querySelector('#trace-selector .shown-selection'),
    );
    expect(shownSelection.innerHTML).toContain('Window Manager');
    expect(shownSelection.innerHTML).not.toContain('Surface Flinger');

    const errorMessageContainer = assertDefined(
      htmlElement.querySelector('.no-timeline-msg'),
    );
    expect(errorMessageContainer.textContent).toContain('No timeline to show!');
    expect(errorMessageContainer.textContent).toContain(
      'Only a single timestamp has been recorded.',
    );

    checkNoTimelineNavigation();
  });

  it('processes active trace input and updates selected traces', async () => {
    loadAllTraces();
    fixture.detectChanges();

    const timelineComponent = assertDefined(component.timeline);
    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    ) as HTMLElement;
    const prevEntryButton = assertDefined(
      htmlElement.querySelector('#prev_entry_button'),
    ) as HTMLElement;

    timelineComponent.selectedTraces = [
      getLoadedTrace(TraceType.SURFACE_FLINGER),
    ];
    fixture.detectChanges();
    checkActiveTraceSurfaceFlinger(nextEntryButton, prevEntryButton);

    // setting same trace as active does not affect selected traces
    await updateActiveTrace(TraceType.SURFACE_FLINGER);
    expectSelectedTraceTypes([TraceType.SURFACE_FLINGER]);
    checkActiveTraceSurfaceFlinger(nextEntryButton, prevEntryButton);

    await updateActiveTrace(TraceType.SCREEN_RECORDING);
    expectSelectedTraceTypes([
      TraceType.SURFACE_FLINGER,
      TraceType.SCREEN_RECORDING,
    ]);
    testCurrentTimestampOnButtonClick(prevEntryButton, position110, 110n);

    await updateActiveTrace(TraceType.WINDOW_MANAGER);
    expectSelectedTraceTypes([
      TraceType.SURFACE_FLINGER,
      TraceType.SCREEN_RECORDING,
      TraceType.WINDOW_MANAGER,
    ]);
    checkActiveTraceWindowManager(nextEntryButton, prevEntryButton);

    await updateActiveTrace(TraceType.PROTO_LOG);
    expectSelectedTraceTypes([
      TraceType.SURFACE_FLINGER,
      TraceType.SCREEN_RECORDING,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
    ]);
    testCurrentTimestampOnButtonClick(nextEntryButton, position100, 100n);
    checkActiveTraceHasOneEntry(nextEntryButton, prevEntryButton);

    // setting active trace that is already selected does not affect selection
    await updateActiveTrace(TraceType.SCREEN_RECORDING);
    expectSelectedTraceTypes([
      TraceType.SURFACE_FLINGER,
      TraceType.SCREEN_RECORDING,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
    ]);
    testCurrentTimestampOnButtonClick(nextEntryButton, position110, 110n);
    checkActiveTraceHasOneEntry(nextEntryButton, prevEntryButton);
  });

  it('handles undefined active trace input', async () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SCREEN_RECORDING, [time100, time110])
      .build();

    const timelineData = assertDefined(component.timelineData);
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    timelineData.setPosition(position100);
    fixture.detectChanges();
    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    ) as HTMLElement;
    const prevEntryButton = assertDefined(
      htmlElement.querySelector('#prev_entry_button'),
    ) as HTMLElement;
    expect(timelineData.getActiveTrace()).toBeUndefined();
    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );

    expect(prevEntryButton.getAttribute('disabled')).toEqual('true');
    expect(nextEntryButton.getAttribute('disabled')).toEqual('true');
  });

  it('handles ActiveTraceChanged event', async () => {
    loadSfWmTraces();
    fixture.detectChanges();

    const timelineComponent = assertDefined(component.timeline);
    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    ) as HTMLElement;
    const prevEntryButton = assertDefined(
      htmlElement.querySelector('#prev_entry_button'),
    ) as HTMLElement;
    const spy = spyOn(
      assertDefined(timelineComponent.miniTimeline?.drawer),
      'draw',
    );

    await updateActiveTrace(TraceType.SURFACE_FLINGER);
    fixture.detectChanges();
    checkActiveTraceSurfaceFlinger(nextEntryButton, prevEntryButton);
    expect(spy).toHaveBeenCalled();
  });

  it('updates trace selection using selector', async () => {
    const allTraceTypes = [
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
    ];
    loadAllTraces();
    expectSelectedTraceTypes(allTraceTypes);

    await openSelectPanel();

    const matOptions = assertDefined(
      document.documentElement.querySelectorAll('mat-option'),
    );
    const sfOption = assertDefined(matOptions.item(1)) as HTMLInputElement;
    expect(sfOption.textContent).toContain('Surface Flinger');
    expect(sfOption.ariaDisabled).toEqual('true');
    for (const i of [0, 2, 3]) {
      expect((matOptions.item(i) as HTMLInputElement).ariaDisabled).toEqual(
        'false',
      );
    }

    (matOptions.item(2) as HTMLElement).click();
    fixture.detectChanges();
    expectSelectedTraceTypes([
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
      TraceType.PROTO_LOG,
    ]);
    const icons = htmlElement.querySelectorAll(
      '#trace-selector .shown-selection .mat-icon',
    );
    expect(
      Array.from(icons)
        .map((icon) => icon.textContent?.trim())
        .slice(1),
    ).toEqual([
      TRACE_INFO[TraceType.SCREEN_RECORDING].icon,
      TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
      TRACE_INFO[TraceType.PROTO_LOG].icon,
    ]);

    (matOptions.item(2) as HTMLElement).click();
    fixture.detectChanges();
    expectSelectedTraceTypes(allTraceTypes);
    const newIcons = htmlElement.querySelectorAll(
      '#trace-selector .shown-selection .mat-icon',
    );
    expect(
      Array.from(newIcons)
        .map((icon) => icon.textContent?.trim())
        .slice(1),
    ).toEqual([
      TRACE_INFO[TraceType.SCREEN_RECORDING].icon,
      TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
      TRACE_INFO[TraceType.WINDOW_MANAGER].icon,
      TRACE_INFO[TraceType.PROTO_LOG].icon,
    ]);
  });

  it('update name and disables option for dumps', async () => {
    loadAllTraces(component, fixture, false);
    await openSelectPanel();

    const matOptions = assertDefined(
      document.documentElement.querySelectorAll('mat-option'),
    ); // [WM, SF, SR, ProtoLog]

    for (const i of [0, 2]) {
      expect((matOptions.item(i) as HTMLInputElement).ariaDisabled).toEqual(
        'false',
      );
    }
    for (const i of [1, 3]) {
      expect((matOptions.item(i) as HTMLInputElement).ariaDisabled).toEqual(
        'true',
      );
    }
    expect(matOptions.item(3).textContent).toContain('ProtoLog Dump');
  });

  it('next button disabled if no next entry', () => {
    loadSfWmTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );

    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    );
    expect(nextEntryButton.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(nextEntryButton.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(nextEntryButton.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(nextEntryButton.getAttribute('disabled')).toBeTruthy();
  });

  it('prev button disabled if no prev entry', () => {
    loadSfWmTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );
    const prevEntryButton = assertDefined(
      htmlElement.querySelector('#prev_entry_button'),
    );
    expect(prevEntryButton.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(prevEntryButton.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(prevEntryButton.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(prevEntryButton.getAttribute('disabled')).toBeFalsy();
  });

  it('next button enabled for different active viewers', async () => {
    loadSfWmTraces();
    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    );

    expect(nextEntryButton.getAttribute('disabled')).toBeNull();

    await updateActiveTrace(TraceType.WINDOW_MANAGER);
    fixture.detectChanges();

    expect(nextEntryButton.getAttribute('disabled')).toBeNull();
  });

  it('changes timestamp on next entry button press', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const nextEntryButton = assertDefined(
      htmlElement.querySelector('#next_entry_button'),
    ) as HTMLElement;

    testCurrentTimestampOnButtonClick(nextEntryButton, position105, 110n);

    testCurrentTimestampOnButtonClick(nextEntryButton, position100, 110n);

    testCurrentTimestampOnButtonClick(nextEntryButton, position90, 100n);

    // No change when we are already on the last timestamp of the active trace
    testCurrentTimestampOnButtonClick(nextEntryButton, position110, 110n);

    // No change when we are after the last entry of the active trace
    testCurrentTimestampOnButtonClick(nextEntryButton, position112, 112n);
  });

  it('changes timestamp on previous entry button press', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const prevEntryButton = assertDefined(
      htmlElement.querySelector('#prev_entry_button'),
    ) as HTMLElement;

    // In this state we are already on the first entry at timestamp 100, so
    // there is no entry to move to before and we just don't update the timestamp
    testCurrentTimestampOnButtonClick(prevEntryButton, position105, 105n);

    testCurrentTimestampOnButtonClick(prevEntryButton, position110, 100n);

    // Active entry here should be 110 so moving back means moving to 100.
    testCurrentTimestampOnButtonClick(prevEntryButton, position112, 100n);

    // No change when we are already on the first timestamp of the active trace
    testCurrentTimestampOnButtonClick(prevEntryButton, position100, 100n);

    // No change when we are before the first entry of the active trace
    testCurrentTimestampOnButtonClick(prevEntryButton, position90, 90n);
  });

  it('performs expected action on arrow key press depending on input form focus', () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);

    const spyNextEntry = spyOn(timelineComponent, 'moveToNextEntry');
    const spyPrevEntry = spyOn(timelineComponent, 'moveToPreviousEntry');

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowRight'}));
    fixture.detectChanges();
    expect(spyNextEntry).toHaveBeenCalled();

    const formElement = htmlElement.querySelector('.time-input input');
    const focusInEvent = new FocusEvent('focusin');
    Object.defineProperty(focusInEvent, 'target', {value: formElement});
    document.dispatchEvent(focusInEvent);
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    fixture.detectChanges();
    expect(spyPrevEntry).not.toHaveBeenCalled();

    const focusOutEvent = new FocusEvent('focusout');
    Object.defineProperty(focusOutEvent, 'target', {value: formElement});
    document.dispatchEvent(focusOutEvent);
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    fixture.detectChanges();
    expect(spyPrevEntry).toHaveBeenCalled();
  });

  it('updates position based on ns input field', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);

    const timeInputField = assertDefined(
      document.querySelector('.time-input.nano'),
    ) as HTMLInputElement;

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position105,
      '110 ns',
      110n,
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position100,
      '110 ns',
      110n,
    );

    testCurrentTimestampOnTimeInput(timeInputField, position90, '100 ns', 100n);

    // No change when we are already on the last timestamp of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position110,
      '110 ns',
      110n,
    );

    // No change when we are after the last entry of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position112,
      '112 ns',
      112n,
    );
  });

  it('updates position based on human time input field using date time format', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);

    const timeInputField = assertDefined(
      document.querySelector('.time-input.human'),
    ) as HTMLInputElement;

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position105,
      '1970-01-01, 00:00:00.000000110',
      110n,
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position100,
      '1970-01-01, 00:00:00.000000110',
      110n,
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position90,
      '1970-01-01, 00:00:00.000000100',
      100n,
    );

    // No change when we are already on the last timestamp of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position110,
      '1970-01-01, 00:00:00.000000110',
      110n,
    );

    // No change when we are after the last entry of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position112,
      '1970-01-01, 00:00:00.000000112',
      112n,
    );
  });

  it('updates position based on human time input field using ISO timestamp format', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.valueOf(),
    ).toEqual(100n);

    const timeInputField = assertDefined(
      document.querySelector('.time-input.human'),
    ) as HTMLInputElement;

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position90,
      '1970-01-01T00:00:00.000000100',
      100n,
    );
  });

  it('updates position based on human time input field using time-only format', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.valueOf(),
    ).toEqual(100n);

    const timeInputField = assertDefined(
      document.querySelector('.time-input.human'),
    ) as HTMLInputElement;

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position105,
      '00:00:00.000000110',
      110n,
    );
  });

  it('sets initial zoom of mini timeline from first non-SR viewer to end of all traces', () => {
    loadAllTraces();
    const timelineComponent = assertDefined(component.timeline);
    expect(timelineComponent.initialZoom).toEqual(
      new TimeRange(time100, time112),
    );
  });

  it('stores manual trace deselection and applies on new load', async () => {
    loadAllTraces();
    const firstTimeline = assertDefined(component.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      firstTimeline,
    );
    await openSelectPanel();
    clickTraceFromSelectPanel(2);
    clickTraceFromSelectPanel(3);
    expectSelectedTraceTypes(
      [TraceType.SCREEN_RECORDING, TraceType.SURFACE_FLINGER],
      firstTimeline,
    );

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(secondHost, secondFixture);
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [TraceType.SCREEN_RECORDING, TraceType.SURFACE_FLINGER],
      secondTimeline,
    );

    clickTraceFromSelectPanel(2);
    expectSelectedTraceTypes(
      [TraceType.SCREEN_RECORDING, TraceType.SURFACE_FLINGER],
      secondTimeline,
    );

    const thirdFixture = TestBed.createComponent(TestHostComponent);
    const thirdHost = thirdFixture.componentInstance;
    loadAllTraces(thirdHost, thirdFixture);
    const thirdTimeline = assertDefined(thirdHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      thirdTimeline,
    );
  });

  it('does not apply stored trace deselection on active trace', async () => {
    loadAllTraces();
    const firstTimeline = assertDefined(component.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      firstTimeline,
    );
    await updateActiveTrace(TraceType.PROTO_LOG);
    await openSelectPanel();
    clickTraceFromSelectPanel(1);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      firstTimeline,
    );

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(secondHost, secondFixture);
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      secondTimeline,
    );
  });

  it('does not apply stored trace deselection if only one timestamp available', async () => {
    loadAllTraces();
    await updateActiveTrace(TraceType.PROTO_LOG);
    await openSelectPanel();
    clickTraceFromSelectPanel(2);

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    const secondElement = secondFixture.nativeElement;
    await loadTracesWithOneTimestamp(secondHost, secondFixture);

    const shownSelection = assertDefined(
      secondElement.querySelector('#trace-selector .shown-selection'),
    );
    expect(shownSelection.innerHTML).toContain('Window Manager');
    expect(shownSelection.textContent).not.toContain('Surface Flinger');
  });

  it('does not store traces based on active view trace type', async () => {
    loadAllTraces();
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      component.timeline,
    );
    await openSelectPanel();
    clickTraceFromSelectPanel(3);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      component.timeline,
    );
    await updateActiveTrace(TraceType.PROTO_LOG);
    fixture.detectChanges();
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      component.timeline,
    );

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(secondHost, secondFixture);
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      secondTimeline,
    );
  });

  it('applies stored trace deselection between non-consecutive applicable sessions', async () => {
    loadAllTraces();
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.PROTO_LOG,
      ],
      component.timeline,
    );
    await openSelectPanel();
    clickTraceFromSelectPanel(3);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      component.timeline,
    );

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadSfWmTraces(secondHost, secondFixture);
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER],
      secondTimeline,
    );

    const thirdFixture = TestBed.createComponent(TestHostComponent);
    const thirdHost = thirdFixture.componentInstance;
    loadAllTraces(thirdHost, thirdFixture);
    const thirdTimeline = assertDefined(thirdHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      thirdTimeline,
    );
  });

  it('shows all traces in new session that were not present (so not deselected) in previous session', async () => {
    loadSfWmTraces();
    expectSelectedTraceTypes(
      [TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER],
      component.timeline,
    );
    await openSelectPanel();
    clickTraceFromSelectPanel(1);
    expectSelectedTraceTypes([TraceType.SURFACE_FLINGER], component.timeline);

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(secondHost, secondFixture);
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.PROTO_LOG,
      ],
      secondTimeline,
    );
  });

  it('toggles bookmark of current position', () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    expect(timelineComponent.bookmarks).toEqual([]);
    expect(timelineComponent.currentPositionBookmarked()).toBeFalse();

    const bookmarkIcon = assertDefined(
      htmlElement.querySelector('.bookmark-icon'),
    ) as HTMLElement;
    bookmarkIcon.click();
    fixture.detectChanges();

    expect(timelineComponent.bookmarks).toEqual([time100]);
    expect(timelineComponent.currentPositionBookmarked()).toBeTrue();

    bookmarkIcon.click();
    fixture.detectChanges();
    expect(timelineComponent.bookmarks).toEqual([]);
    expect(timelineComponent.currentPositionBookmarked()).toBeFalse();
  });

  it('toggles same bookmark if click within range', () => {
    loadTracesWithLargeTimeRange();

    const timelineComponent = assertDefined(component.timeline);
    expect(timelineComponent.bookmarks.length).toEqual(0);

    openContextMenu();
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(1);

    // click within marker y-pos, x-pos close enough to remove bookmark
    openContextMenu(5);
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(0);

    openContextMenu();
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(1);

    // click within marker y-pos, x-pos too large so new bookmark added
    openContextMenu(20);
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(2);

    openContextMenu(20);
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(1);

    // click below marker y-pos, x-pos now too large so new bookmark added
    openContextMenu(5, true);
    clickToggleBookmarkOption();
    expect(timelineComponent.bookmarks.length).toEqual(2);
  });

  it('removes all bookmarks', () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    timelineComponent.bookmarks = [time100, time101, time112];
    fixture.detectChanges();

    openContextMenu();
    clickRemoveAllBookmarksOption();
    expect(timelineComponent.bookmarks).toEqual([]);
  });

  it('updates active trace then trace position on mini timeline click', async () => {
    loadAllTraces();
    const timelineComponent = assertDefined(component.timeline);

    let firstEvent: WinscopeEvent | undefined;
    let activeTrace: Trace<object> | undefined;
    let position: TracePosition | undefined;
    timelineComponent.setEmitEvent(async (event: WinscopeEvent) => {
      if (!firstEvent) {
        expect(event).toBeInstanceOf(ActiveTraceChanged);
        firstEvent = event;
        activeTrace = (event as ActiveTraceChanged).trace;
      } else {
        expect(event).toBeInstanceOf(TracePositionUpdate);
        position = (event as TracePositionUpdate).position;
      }
    });
    const miniTimelineComponent = assertDefined(timelineComponent.miniTimeline);
    const trace = assertDefined(
      component.timelineData.getTraces().getTrace(TraceType.WINDOW_MANAGER),
    );
    spyOn(
      assertDefined(miniTimelineComponent.drawer),
      'getTraceClicked',
    ).and.returnValue(Promise.resolve(trace));
    const canvas = miniTimelineComponent.getCanvas();
    canvas.dispatchEvent(new MouseEvent('mousedown'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(activeTrace).toEqual(trace);
    expect(position).toBeDefined();
  });

  function loadSfWmTraces(hostComponent = component, hostFixture = fixture) {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [
        time90,
        time101,
        time110,
        time112,
      ])
      .build();

    const timelineData = assertDefined(hostComponent.timelineData);
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    timelineData.setPosition(position100);
    hostComponent.allTraces = hostComponent.timelineData.getTraces();
    hostFixture.detectChanges();
  }

  function loadAllTraces(
    hostComponent = component,
    hostFixture = fixture,
    loadAllTraces = true,
  ) {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [
        time90,
        time101,
        time110,
        time112,
      ])
      .setTimestamps(TraceType.SCREEN_RECORDING, [time110])
      .setTimestamps(TraceType.PROTO_LOG, [time100])
      .build();

    let timelineDataTraces: Traces | undefined;
    if (loadAllTraces) {
      timelineDataTraces = traces;
    } else {
      timelineDataTraces = new Traces();
      traces.forEachTrace((trace) => {
        if (trace.type !== TraceType.PROTO_LOG) {
          assertDefined(timelineDataTraces).addTrace(trace);
        }
      });
    }

    assertDefined(hostComponent.timelineData).initialize(
      timelineDataTraces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    hostComponent.allTraces = traces;
    hostFixture.detectChanges();
  }

  function loadTracesWithLargeTimeRange() {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [
        time100,
        time2000,
        time3000,
        time4000,
      ])
      .setTimestamps(TraceType.WINDOW_MANAGER, [
        time2000,
        time4000,
        time6000,
        time8000,
      ])
      .build();

    const timelineData = assertDefined(component.timelineData);
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    timelineData.setPosition(position100);
    component.allTraces = timelineData.getTraces();
    fixture.detectChanges();
  }

  function getLoadedTrace(type: TraceType): Trace<object> {
    const timelineData = assertDefined(component.timelineData);
    const trace = assertDefined(
      timelineData.getTraces().getTrace(type),
    ) as Trace<object>;
    return trace;
  }

  async function loadTracesWithOneTimestamp(
    hostComponent = component,
    hostFixture = fixture,
  ) {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time100])
      .build();
    assertDefined(hostComponent.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    hostComponent.allTraces = traces;
    hostFixture.detectChanges();
    await hostFixture.whenStable();
    hostFixture.detectChanges();
  }

  async function updateActiveTrace(type: TraceType) {
    const trace = getLoadedTrace(type);
    const timelineData = assertDefined(component.timelineData);
    timelineData.trySetActiveTrace(trace);

    const timelineComponent = assertDefined(component.timeline);
    await timelineComponent.onWinscopeEvent(new ActiveTraceChanged(trace));
  }

  function expectSelectedTraceTypes(
    expected: TraceType[],
    timelineComponent?: TimelineComponent,
  ) {
    const timeline = assertDefined(timelineComponent ?? component.timeline);
    const actual = timeline.selectedTraces.map((trace) => trace.type);
    expect(actual).toEqual(expected);
  }

  function testCurrentTimestampOnButtonClick(
    button: HTMLElement,
    pos: TracePosition,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    fixture.detectChanges();
    button.click();
    fixture.detectChanges();
    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }

  function testCurrentTimestampOnTimeInput(
    inputField: HTMLInputElement,
    pos: TracePosition,
    textInput: string,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    fixture.detectChanges();

    inputField.value = textInput;
    inputField.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }

  async function openSelectPanel() {
    const selectTrigger = assertDefined(
      htmlElement.querySelector('.mat-select-trigger'),
    );
    (selectTrigger as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function clickTraceFromSelectPanel(index: number) {
    const matOptions = assertDefined(
      document.documentElement.querySelectorAll('mat-option'),
    );
    (matOptions.item(index) as HTMLElement).click();
    fixture.detectChanges();
  }

  function checkActiveTraceSurfaceFlinger(
    nextEntryButton: HTMLElement,
    prevEntryButton: HTMLElement,
  ) {
    testCurrentTimestampOnButtonClick(prevEntryButton, position110, 100n);
    expect(prevEntryButton.getAttribute('disabled')).toEqual('true');
    expect(nextEntryButton.getAttribute('disabled')).toBeNull();
    testCurrentTimestampOnButtonClick(nextEntryButton, position100, 110n);
    expect(prevEntryButton.getAttribute('disabled')).toBeNull();
    expect(nextEntryButton.getAttribute('disabled')).toEqual('true');
  }

  function checkActiveTraceWindowManager(
    nextEntryButton: HTMLElement,
    prevEntryButton: HTMLElement,
  ) {
    testCurrentTimestampOnButtonClick(prevEntryButton, position90, 90n);
    expect(prevEntryButton.getAttribute('disabled')).toEqual('true');
    expect(nextEntryButton.getAttribute('disabled')).toBeNull();
    testCurrentTimestampOnButtonClick(nextEntryButton, position90, 101n);
    expect(prevEntryButton.getAttribute('disabled')).toBeNull();
    expect(nextEntryButton.getAttribute('disabled')).toBeNull();
    testCurrentTimestampOnButtonClick(nextEntryButton, position110, 112n);
    expect(prevEntryButton.getAttribute('disabled')).toBeNull();
    expect(nextEntryButton.getAttribute('disabled')).toEqual('true');
  }

  function checkActiveTraceHasOneEntry(
    nextEntryButton: HTMLElement,
    prevEntryButton: HTMLElement,
  ) {
    expect(prevEntryButton.getAttribute('disabled')).toEqual('true');
    expect(nextEntryButton.getAttribute('disabled')).toEqual('true');
  }

  function checkNoTimelineNavigation() {
    const timelineComponent = assertDefined(component.timeline);
    // no expand button
    expect(
      htmlElement.querySelector(`.${timelineComponent.TOGGLE_BUTTON_CLASS}`),
    ).toBeNull();

    // no timelines shown
    const miniTimelineElement = fixture.debugElement.query(
      By.directive(MiniTimelineComponent),
    );
    expect(miniTimelineElement).toBeFalsy();

    // arrow key presses don't do anything
    const spyNextEntry = spyOn(timelineComponent, 'moveToNextEntry');
    const spyPrevEntry = spyOn(timelineComponent, 'moveToPreviousEntry');

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowRight'}));
    fixture.detectChanges();
    expect(spyNextEntry).not.toHaveBeenCalled();

    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    fixture.detectChanges();
    expect(spyPrevEntry).not.toHaveBeenCalled();
  }

  function openContextMenu(xOffset = 0, clickBelowMarker = false) {
    const miniTimelineCanvas = assertDefined(
      htmlElement.querySelector('#mini-timeline-canvas'),
    ) as HTMLElement;
    const yOffset = clickBelowMarker
      ? assertDefined(component.timeline?.miniTimeline?.drawer?.getHeight()) /
          6 +
        1
      : 0;

    const event = new MouseEvent('contextmenu');
    spyOnProperty(event, 'offsetX').and.returnValue(
      miniTimelineCanvas.offsetLeft +
        miniTimelineCanvas.offsetWidth / 2 +
        xOffset,
    );
    spyOnProperty(event, 'offsetY').and.returnValue(
      miniTimelineCanvas.offsetTop + yOffset,
    );
    miniTimelineCanvas.dispatchEvent(event);
    fixture.detectChanges();
  }

  function clickToggleBookmarkOption() {
    const menu = assertDefined(document.querySelector('.context-menu'));
    const toggleOption = assertDefined(
      menu.querySelector('.context-menu-item'),
    ) as HTMLElement;
    toggleOption.click();
    fixture.detectChanges();
  }

  function clickRemoveAllBookmarksOption() {
    const menu = assertDefined(document.querySelector('.context-menu'));
    const options = assertDefined(menu.querySelectorAll('.context-menu-item'));
    (options.item(1) as HTMLElement).click();
    fixture.detectChanges();
  }

  @Component({
    selector: 'host-component',
    template: `
      <timeline
        [allTraces]="allTraces"
        [timelineData]="timelineData"
        [store]="store"></timeline>
    `,
  })
  class TestHostComponent {
    timelineData = new TimelineData();
    allTraces = new Traces();
    store = new PersistentStore();

    @ViewChild(TimelineComponent)
    timeline: TimelineComponent | undefined;

    ngOnDestroy() {
      if (this.timeline) {
        this.store.clear(this.timeline.storeKeyDeselectedTraces);
      }
    }
  }
});

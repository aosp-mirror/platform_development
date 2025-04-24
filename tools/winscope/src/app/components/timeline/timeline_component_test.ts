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
import {TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatDrawer,
  MatDrawerContainer,
  MatDrawerContent,
} from 'app/components/bottomnav/bottom_drawer_component';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/store/persistent_store';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeRange} from 'common/time/time';
import {
  ActiveTraceChanged,
  ExpandedTimelineToggled,
  InitializeTraceSearchRequest,
  TraceAddRequest,
  TracePositionUpdate,
  TraceRemoveRequest,
  TraceSearchCompleted,
  TraceSearchInitialized,
  TraceSearchRequest,
  WinscopeEvent,
} from 'messaging/winscope_event';
import {checkTooltips, DOMTestHelper} from 'test/unit/dom_test_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {makeEmptyTrace} from 'test/unit/trace_utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {makeSearchTraceSpies} from 'trace_processor/test_utils';
import {CanvasDrawer} from './expanded-timeline/canvas_drawer';
import {DefaultTimelineRowComponent} from './expanded-timeline/default_timeline_row_component';
import {ExpandedTimelineComponent} from './expanded-timeline/expanded_timeline_component';
import {TransitionTimelineComponent} from './expanded-timeline/transition_timeline_component';
import {MiniTimelineDrawerImpl} from './mini-timeline/drawer/mini_timeline_drawer_impl';
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

  const nextEntrySelector = '#next_entry_button';
  const prevEntrySelector = '#prev_entry_button';

  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

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
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
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
    dom.detectChanges();

    const timelineComponent = assertDefined(component.timeline);

    // initially not expanded
    let expandedTimelineElement = dom.findByDirective(
      ExpandedTimelineComponent,
    );
    expect(expandedTimelineElement).toBeUndefined();

    let isExpanded = false;
    timelineComponent.setEmitEvent(async (event: WinscopeEvent) => {
      expect(event).toBeInstanceOf(ExpandedTimelineToggled);
      isExpanded = (event as ExpandedTimelineToggled).isTimelineExpanded;
    });

    const button = dom.findAndClick(
      `.${timelineComponent.TOGGLE_BUTTON_CLASS}`,
    );
    expandedTimelineElement = dom.findByDirective(ExpandedTimelineComponent);
    expect(expandedTimelineElement).toBeDefined();
    expect(isExpanded).toBeTrue();

    button.click();
    expandedTimelineElement = dom.findByDirective(ExpandedTimelineComponent);
    expect(expandedTimelineElement).toBeUndefined();
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
    dom.detectChanges();

    expect(dom.find('.time-selector')).toBeUndefined();
    expect(dom.find('.trace-selector')).toBeUndefined();

    const errorMessageContainer = dom.get('.no-timeline-msg');
    errorMessageContainer.checkText('No timeline to show!');
    errorMessageContainer.checkText('All loaded traces contain no timestamps.');

    checkNoTimelineNavigation();
  });

  it('handles some empty traces and some with one timestamp', async () => {
    await loadTracesWithOneTimestamp();

    expect(dom.find('#time-selector')).toBeDefined();
    const shownSelection = dom.get('#trace-selector .shown-selection');
    shownSelection.checkInnerHTML('Window Manager');
    shownSelection.checkInnerHTML('Surface Flinger', false);

    const errorMessageContainer = dom.get('.no-timeline-msg');
    errorMessageContainer.checkText('No timeline to show!');
    errorMessageContainer.checkText(
      'Only a single timestamp has been recorded.',
    );

    checkNoTimelineNavigation();
  });

  it('processes active trace input and updates selected traces', async () => {
    loadAllTraces();
    dom.detectChanges();

    const timelineComponent = assertDefined(component.timeline);
    const nextEntryButton = dom.get(nextEntrySelector);
    const prevEntryButton = dom.get(prevEntrySelector);

    timelineComponent.selectedTraces = [
      getLoadedTrace(TraceType.SURFACE_FLINGER),
    ];
    dom.detectChanges();
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
      .setTimestamps(TraceType.EVENT_LOG, [time100, time110])
      .build();

    const timelineData = assertDefined(component.timelineData);
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    timelineData.setPosition(position100);
    dom.detectChanges();
    const nextEntryButton = dom.get(nextEntrySelector);
    const prevEntryButton = dom.get(prevEntrySelector);
    expect(timelineData.getActiveTrace()).toBeUndefined();
    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );

    prevEntryButton.checkDisabled(true);
    nextEntryButton.checkDisabled(true);
  });

  it('handles ActiveTraceChanged event', async () => {
    loadSfWmTraces();
    dom.detectChanges();

    const timelineComponent = assertDefined(component.timeline);
    const nextEntryButton = dom.get(nextEntrySelector);
    const prevEntryButton = dom.get(prevEntrySelector);
    const spy = spyOn(
      assertDefined(timelineComponent.miniTimeline?.drawer),
      'draw',
    );

    await updateActiveTrace(TraceType.SURFACE_FLINGER);
    dom.detectChanges();
    checkActiveTraceSurfaceFlinger(nextEntryButton, prevEntryButton);
    expect(spy).toHaveBeenCalled();
  });

  it('updates trace selection using selector', async () => {
    const allTraceTypes = [
      TraceType.SEARCH,
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
      TraceType.VIEW_CAPTURE,
    ];
    loadAllTraces();
    const [spyQueryResult, spyIter] = makeSearchTraceSpies(time100);
    const searchTrace = new TraceBuilder<QueryResult>()
      .setEntries([spyQueryResult])
      .setTimestamps([time100])
      .setDescriptors(['test query', '0'])
      .setType(TraceType.SEARCH)
      .build();
    await component.timeline?.onWinscopeEvent(new TraceAddRequest(searchTrace));
    expectSelectedTraceTypes(allTraceTypes);

    await dom.openMatSelect();

    const matOptions = dom.getMatSelectPanel().findAll('mat-option');
    await checkTooltips(Array.from(matOptions), [
      'test query, 0',
      'mock_screen_recording',
      'file descriptor',
      'file descriptor',
      'file descriptor',
      'Test Window, mock_view_capture',
    ]);
    matOptions[0].checkText('Search test query');
    const sfOption = matOptions[2];
    sfOption.checkText('Surface Flinger');
    expect(sfOption.getHTMLElement().ariaDisabled).toEqual('true');
    for (const i of [1, 3, 4]) {
      expect(matOptions[1].getHTMLElement().ariaDisabled).toEqual('false');
    }

    matOptions[3].click();
    const expectedTypes = [
      TraceType.SEARCH,
      TraceType.SCREEN_RECORDING,
      TraceType.SURFACE_FLINGER,
      TraceType.PROTO_LOG,
      TraceType.VIEW_CAPTURE,
    ];
    expectSelectedTraceTypes(expectedTypes);
    const traceIcons = dom
      .findAll('#trace-selector .shown-selection .mat-icon')
      .slice(1);
    traceIcons.forEach((el, index) => {
      const expectedType = expectedTypes[index];
      el.checkTextExact(TRACE_INFO[expectedType].icon);
    });
    await checkTooltips(traceIcons, [
      'Search test query',
      'Screen Recording mock_screen_recording',
      TRACE_INFO[TraceType.SURFACE_FLINGER].name,
      TRACE_INFO[TraceType.PROTO_LOG].name,
      'View Capture Test Window',
    ]);

    matOptions[3].click();
    expectSelectedTraceTypes(allTraceTypes);
    const newIcons = dom.findAll('#trace-selector .shown-selection .mat-icon');
    expect(
      Array.from(newIcons)
        .map((icon) => icon.getText())
        .slice(1),
    ).toEqual(allTraceTypes.map((type) => TRACE_INFO[type].icon));
  });

  it('update name and disables option for dumps', async () => {
    loadAllTraces(component, dom, false);
    await dom.openMatSelect();

    const matOptions = dom.getMatSelectPanel().findAll('mat-option'); // [WM, SF, SR, ProtoLog, VC]

    for (const i of [0, 2, 4]) {
      expect(matOptions[i].getHTMLElement().ariaDisabled).toEqual('false');
    }
    for (const i of [1, 3]) {
      expect(matOptions[i].getHTMLElement().ariaDisabled).toEqual('true');
    }
    matOptions[3].checkText('ProtoLog Dump');
    matOptions[4].checkText('View Capture Test Window');
  });

  it('next button disabled if no next entry', () => {
    loadSfWmTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );

    const nextEntryButton = dom.get(nextEntrySelector);
    nextEntryButton.checkDisabled(false);

    timelineData.setPosition(position90);
    dom.detectChanges();
    nextEntryButton.checkDisabled(false);

    timelineData.setPosition(position110);
    dom.detectChanges();
    nextEntryButton.checkDisabled(true);

    timelineData.setPosition(position112);
    dom.detectChanges();
    nextEntryButton.checkDisabled(true);
  });

  it('prev button disabled if no prev entry', () => {
    loadSfWmTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );
    const prevEntryButton = dom.get(prevEntrySelector);
    prevEntryButton.checkDisabled(true);

    timelineData.setPosition(position90);
    dom.detectChanges();
    prevEntryButton.checkDisabled(true);

    timelineData.setPosition(position110);
    dom.detectChanges();
    prevEntryButton.checkDisabled(false);

    timelineData.setPosition(position112);
    dom.detectChanges();
    prevEntryButton.checkDisabled(false);
  });

  it('next button enabled for different active viewers', async () => {
    loadSfWmTraces();
    const nextEntryButton = dom.get(nextEntrySelector);
    nextEntryButton.checkDisabled(false);

    await updateActiveTrace(TraceType.WINDOW_MANAGER);
    dom.detectChanges();
    nextEntryButton.checkDisabled(false);
  });

  it('changes timestamp on next entry button press', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const nextEntryButton = dom.get(nextEntrySelector);

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
    const prevEntryButton = dom.get(prevEntrySelector);

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

    dom.keydownArrowRight(true);
    expect(spyNextEntry).toHaveBeenCalled();

    const formElement = dom.get('.time-input input').getHTMLElement();
    const focusInEvent = new FocusEvent('focusin');
    Object.defineProperty(focusInEvent, 'target', {value: formElement});
    dom.dispatchEventInDocument(focusInEvent);

    dom.keydownArrowLeft(true);
    expect(spyPrevEntry).not.toHaveBeenCalled();

    const focusOutEvent = new FocusEvent('focusout');
    Object.defineProperty(focusOutEvent, 'target', {value: formElement});
    dom.dispatchEventInDocument(focusOutEvent);

    dom.keydownArrowLeft(true);
    expect(spyPrevEntry).toHaveBeenCalled();
  });

  it('updates position based on ns input field', () => {
    loadSfWmTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);

    const timeInputField = dom.get('.time-input.nano');

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

    const timeInputField = dom.get('.time-input.human');

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

    const timeInputField = dom.get('.time-input.human');

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

    const timeInputField = dom.get('.time-input.human');

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
        TraceType.VIEW_CAPTURE,
      ],
      firstTimeline,
    );
    await dom.openMatSelect();
    clickTraceFromSelectPanel(2);
    clickTraceFromSelectPanel(3);
    clickTraceFromSelectPanel(4);
    expectSelectedTraceTypes(
      [TraceType.SCREEN_RECORDING, TraceType.SURFACE_FLINGER],
      firstTimeline,
    );

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );
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
    loadAllTraces(
      thirdHost,
      new DOMTestHelper(thirdFixture, thirdFixture.nativeElement),
    );
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
        TraceType.VIEW_CAPTURE,
      ],
      firstTimeline,
    );
    await updateActiveTrace(TraceType.PROTO_LOG);
    await dom.openMatSelect();
    clickTraceFromSelectPanel(1);
    clickTraceFromSelectPanel(4);
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
    loadAllTraces(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );
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
    await dom.openMatSelect();
    clickTraceFromSelectPanel(2);

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    const secondElement = secondFixture.nativeElement;
    await loadTracesWithOneTimestamp(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );

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
        TraceType.VIEW_CAPTURE,
      ],
      component.timeline,
    );
    await dom.openMatSelect();
    clickTraceFromSelectPanel(3);
    clickTraceFromSelectPanel(4);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ],
      component.timeline,
    );
    await updateActiveTrace(TraceType.PROTO_LOG);
    dom.detectChanges();
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
    loadAllTraces(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );
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
        TraceType.VIEW_CAPTURE,
      ],
      component.timeline,
    );
    await dom.openMatSelect();
    clickTraceFromSelectPanel(3);
    clickTraceFromSelectPanel(4);
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
    loadSfWmTraces(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER],
      secondTimeline,
    );

    const thirdFixture = TestBed.createComponent(TestHostComponent);
    const thirdHost = thirdFixture.componentInstance;
    loadAllTraces(
      thirdHost,
      new DOMTestHelper(thirdFixture, thirdFixture.nativeElement),
    );
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
    await dom.openMatSelect();
    clickTraceFromSelectPanel(1);
    expectSelectedTraceTypes([TraceType.SURFACE_FLINGER], component.timeline);

    const secondFixture = TestBed.createComponent(TestHostComponent);
    const secondHost = secondFixture.componentInstance;
    loadAllTraces(
      secondHost,
      new DOMTestHelper(secondFixture, secondFixture.nativeElement),
    );
    const secondTimeline = assertDefined(secondHost.timeline);
    expectSelectedTraceTypes(
      [
        TraceType.SCREEN_RECORDING,
        TraceType.SURFACE_FLINGER,
        TraceType.PROTO_LOG,
        TraceType.VIEW_CAPTURE,
      ],
      secondTimeline,
    );
  });

  it('toggles bookmark of current position', () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    expect(timelineComponent.bookmarks).toEqual([]);
    expect(timelineComponent.currentPositionBookmarked()).toBeFalse();

    const bookmarkIcon = dom.findAndClick('.bookmark-icon');

    expect(timelineComponent.bookmarks).toEqual([time100]);
    expect(timelineComponent.currentPositionBookmarked()).toBeTrue();

    bookmarkIcon.click();
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
    dom.detectChanges();

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
    await dom.detectChangesAndWaitStable();
    await dom.detectChangesAndWaitStable();

    expect(activeTrace).toEqual(trace);
    expect(position).toBeDefined();
  });

  it('adds/removes trace and redraws timeline', async () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    const initialTraces = timelineComponent.sortedTraces.slice();
    const spy = spyOn(
      assertDefined(timelineComponent.miniTimeline?.drawer),
      'draw',
    );
    const trace = makeEmptyTrace(TraceType.SEARCH);

    await timelineComponent.onWinscopeEvent(new TraceAddRequest(trace));
    expect(spy).toHaveBeenCalledTimes(1);
    expect(timelineComponent.sortedTraces).not.toEqual(initialTraces);
    expect(timelineComponent.sortedTraces[0]).toEqual(trace);

    await timelineComponent.onWinscopeEvent(new TraceRemoveRequest(trace));
    expect(spy).toHaveBeenCalledTimes(2);
    expect(timelineComponent.sortedTraces).toEqual(initialTraces);
  });

  it('disables or enables timeline on winscope events', async () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    checkTimelineEnabled();

    await timelineComponent.onWinscopeEvent(new InitializeTraceSearchRequest());
    checkTimelineDisabled();
    await timelineComponent.onWinscopeEvent(new TraceSearchInitialized([]));
    checkTimelineEnabled();

    await timelineComponent.onWinscopeEvent(new TraceSearchRequest(''));
    checkTimelineDisabled();
    await timelineComponent.onWinscopeEvent(new TraceSearchCompleted());
    checkTimelineEnabled();
  });

  it('does not handle arrow key presses if component disabled', () => {
    loadSfWmTraces();
    const timelineComponent = assertDefined(component.timeline);
    timelineComponent.isDisabled = true;
    dom.detectChanges();

    const spyNextEntry = spyOn(timelineComponent, 'moveToNextEntry');
    dom.keydownArrowRight(true);
    expect(spyNextEntry).not.toHaveBeenCalled();
  });

  it('redraws both timelines on scroll', () => {
    loadSfWmTraces();
    openExpandedTimeline();
    const expandedDrawSpy = spyOn(CanvasDrawer.prototype, 'drawRect');
    const miniDrawSpy = spyOn(MiniTimelineDrawerImpl.prototype, 'draw');

    // scroll from expanded timeline
    const wheelEvent = new WheelEvent('wheel');
    spyOnProperty(wheelEvent, 'deltaY').and.returnValue(-200);
    spyOnProperty(wheelEvent, 'deltaX').and.returnValue(0);
    spyOnProperty(wheelEvent, 'y').and.returnValue(10);
    dom.get('single-timeline').dispatchEvent(wheelEvent);
    expect(expandedDrawSpy).toHaveBeenCalledTimes(5); // 3 entries total + 2 selected
    expect(miniDrawSpy).toHaveBeenCalledTimes(1); // all on one canvas so spy called once

    // scroll from mini timeline
    expandedDrawSpy.calls.reset();
    miniDrawSpy.calls.reset();
    spyOnProperty(wheelEvent, 'target').and.returnValue(
      dom.get('#mini-timeline-canvas').getHTMLElement(),
    );
    dom.get('mini-timeline').dispatchEvent(wheelEvent);
    expect(expandedDrawSpy).toHaveBeenCalledTimes(4); // 2 entries total + 2 selected
    expect(miniDrawSpy).toHaveBeenCalledTimes(1);
  });

  it('redraws both timelines on new position from expanded timeline click', () => {
    loadSfWmTraces();
    openExpandedTimeline();
    const expandedDrawSpy = spyOn(CanvasDrawer.prototype, 'drawRect');
    const miniDrawSpy = spyOn(MiniTimelineDrawerImpl.prototype, 'draw');

    const clickEvent = new MouseEvent('mousedown');
    spyOnProperty(clickEvent, 'offsetX').and.returnValue(0);
    spyOnProperty(clickEvent, 'offsetY').and.returnValue(0);
    dom.get('single-timeline #canvas').dispatchEvent(clickEvent);
    expect(expandedDrawSpy).toHaveBeenCalledTimes(3); // redraws SF timeline row
    expect(miniDrawSpy).toHaveBeenCalledTimes(1); // all on one canvas so spy called once
  });

  function loadSfWmTraces(hostComponent = component, domHelper = dom) {
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
    domHelper.detectChanges();
  }

  function loadAllTraces(
    hostComponent = component,
    domHelper = dom,
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
      .setTimestamps(
        TraceType.SCREEN_RECORDING,
        [time110],
        ['mock_screen_recording'],
      )
      .setTimestamps(TraceType.PROTO_LOG, [time100])
      .setTimestamps(
        TraceType.VIEW_CAPTURE,
        [time100],
        ['Test Window', 'mock_view_capture'],
      )
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
    domHelper.detectChanges();
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
    dom.detectChanges();
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
    domHelper = dom,
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
    await domHelper.detectChangesAndWaitStable();
    domHelper.detectChanges();
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
    button: DOMTestHelper<TestHostComponent>,
    pos: TracePosition,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    dom.detectChanges();
    button.click();
    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }

  function testCurrentTimestampOnTimeInput(
    inputField: DOMTestHelper<TestHostComponent>,
    pos: TracePosition,
    textInput: string,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    dom.detectChanges();

    inputField.updateValue(textInput);
    inputField.dispatchEvent(new Event('change'));

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }

  function clickTraceFromSelectPanel(index: number) {
    dom.getMatSelectPanel().findAndClickByIndex('mat-option', index);
  }

  function checkActiveTraceSurfaceFlinger(
    nextEntryButton: DOMTestHelper<TestHostComponent>,
    prevEntryButton: DOMTestHelper<TestHostComponent>,
  ) {
    testCurrentTimestampOnButtonClick(prevEntryButton, position110, 100n);
    prevEntryButton.checkDisabled(true);
    nextEntryButton.checkDisabled(false);
    testCurrentTimestampOnButtonClick(nextEntryButton, position100, 110n);
    prevEntryButton.checkDisabled(false);
    nextEntryButton.checkDisabled(true);
  }

  function checkActiveTraceWindowManager(
    nextEntryButton: DOMTestHelper<TestHostComponent>,
    prevEntryButton: DOMTestHelper<TestHostComponent>,
  ) {
    testCurrentTimestampOnButtonClick(prevEntryButton, position90, 90n);
    prevEntryButton.checkDisabled(true);
    nextEntryButton.checkDisabled(false);
    testCurrentTimestampOnButtonClick(nextEntryButton, position90, 101n);
    prevEntryButton.checkDisabled(false);
    nextEntryButton.checkDisabled(false);
    testCurrentTimestampOnButtonClick(nextEntryButton, position110, 112n);
    prevEntryButton.checkDisabled(false);
    nextEntryButton.checkDisabled(true);
  }

  function checkActiveTraceHasOneEntry(
    nextEntryButton: DOMTestHelper<TestHostComponent>,
    prevEntryButton: DOMTestHelper<TestHostComponent>,
  ) {
    prevEntryButton.checkDisabled(true);
    nextEntryButton.checkDisabled(true);
  }

  function checkNoTimelineNavigation() {
    const timelineComponent = assertDefined(component.timeline);
    // no expand button
    expect(
      dom.find(`.${timelineComponent.TOGGLE_BUTTON_CLASS}`),
    ).toBeUndefined();

    // no timelines shown
    const miniTimelineElement = dom.findByDirective(MiniTimelineComponent);
    expect(miniTimelineElement).toBeUndefined();

    // arrow key presses don't do anything
    const spyNextEntry = spyOn(timelineComponent, 'moveToNextEntry');
    const spyPrevEntry = spyOn(timelineComponent, 'moveToPreviousEntry');

    dom.keydownArrowRight(true);
    expect(spyNextEntry).not.toHaveBeenCalled();

    dom.keydownArrowLeft(true);
    expect(spyPrevEntry).not.toHaveBeenCalled();
  }

  function openContextMenu(xOffset = 0, clickBelowMarker = false) {
    const miniTimelineCanvas = dom.get('#mini-timeline-canvas');
    const canvasEl = miniTimelineCanvas.getHTMLElement();
    const yOffset = clickBelowMarker
      ? assertDefined(component.timeline?.miniTimeline?.drawer?.getHeight()) /
          6 +
        1
      : 0;

    const event = new MouseEvent('contextmenu');
    spyOnProperty(event, 'offsetX').and.returnValue(
      canvasEl.offsetLeft + canvasEl.offsetWidth / 2 + xOffset,
    );
    spyOnProperty(event, 'offsetY').and.returnValue(
      canvasEl.offsetTop + yOffset,
    );
    miniTimelineCanvas.dispatchEvent(event);
  }

  function clickToggleBookmarkOption() {
    const menu = dom.getInDocument('.context-menu');
    menu.findAndClick('.context-menu-item');
  }

  function clickRemoveAllBookmarksOption() {
    const menu = dom.getInDocument('.context-menu');
    menu.findAndClickByIndex('.context-menu-item', 1);
  }

  function checkTimelineEnabled() {
    expect(dom.find('.disabled-component')).toBeUndefined();
    expect(dom.find('.disabled-message')).toBeUndefined();
  }

  function checkTimelineDisabled() {
    expect(dom.find('.disabled-component')).toBeDefined();
    expect(dom.find('.disabled-message')).toBeDefined();
  }

  function openExpandedTimeline() {
    const timelineComponent = assertDefined(component.timeline);
    dom.findAndClick(`.${timelineComponent.TOGGLE_BUTTON_CLASS}`);
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

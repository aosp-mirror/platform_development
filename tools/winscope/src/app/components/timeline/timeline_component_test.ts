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

import {DragDropModule} from '@angular/cdk/drag-drop';
import {ChangeDetectionStrategy, DebugElement} from '@angular/core';
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
import {TRACE_INFO} from 'app/trace_info';
import {assertDefined} from 'common/assert_utils';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {DefaultTimelineRowComponent} from './expanded-timeline/default_timeline_row_component';
import {ExpandedTimelineComponent} from './expanded-timeline/expanded_timeline_component';
import {TransitionTimelineComponent} from './expanded-timeline/transition_timeline_component';
import {MiniTimelineComponent} from './mini-timeline/mini_timeline_component';
import {SliderComponent} from './mini-timeline/slider_component';
import {TimelineComponent} from './timeline_component';

describe('TimelineComponent', () => {
  const time90 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(90n);
  const time100 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n);
  const time101 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(101n);
  const time105 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(105n);
  const time110 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(110n);
  const time112 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(112n);

  const position90 = TracePosition.fromTimestamp(time90);
  const position100 = TracePosition.fromTimestamp(time100);
  const position105 = TracePosition.fromTimestamp(time105);
  const position110 = TracePosition.fromTimestamp(time110);
  const position112 = TracePosition.fromTimestamp(time112);

  let fixture: ComponentFixture<TimelineComponent>;
  let component: TimelineComponent;
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
      ],
      declarations: [
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
    fixture = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.timelineData = new TimelineData();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can be expanded', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .build();
    assertDefined(component.timelineData).initialize(traces, undefined);
    fixture.detectChanges();

    const button = assertDefined(
      htmlElement.querySelector(`.${component.TOGGLE_BUTTON_CLASS}`),
    );

    // initially not expanded
    let expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeFalsy();

    button.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeTruthy();

    button.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent),
    );
    expect(expandedTimelineElement).toBeFalsy();
  });

  it('handles empty traces', () => {
    const traces = new TracesBuilder()
      .setEntries(TraceType.SURFACE_FLINGER, [])
      .build();
    assertDefined(assertDefined(component.timelineData)).initialize(
      traces,
      undefined,
    );
    fixture.detectChanges();

    // no expand button
    const button = htmlElement.querySelector(
      `.${component.TOGGLE_BUTTON_CLASS}`,
    );
    expect(button).toBeFalsy();

    // no timelines shown
    const miniTimelineElement = fixture.debugElement.query(
      By.directive(MiniTimelineComponent),
    );
    expect(miniTimelineElement).toBeFalsy();

    // error message shown
    const errorMessageContainer = assertDefined(
      htmlElement.querySelector('.no-timestamps-msg'),
    );
    expect(errorMessageContainer.textContent).toContain('No timeline to show!');

    // arrow key presses don't do anything
    const spyNextEntry = spyOn(component, 'moveToNextEntry');
    const spyPrevEntry = spyOn(component, 'moveToPreviousEntry');

    component.handleKeyboardEvent(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    fixture.detectChanges();
    expect(spyNextEntry).not.toHaveBeenCalled();

    component.handleKeyboardEvent(
      new KeyboardEvent('keydown', {key: 'ArrowLeft'}),
    );
    fixture.detectChanges();
    expect(spyPrevEntry).not.toHaveBeenCalled();
  });

  it('handles some empty traces', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time100])
      .build();
    assertDefined(component.timelineData).initialize(traces, undefined);
    fixture.detectChanges();
  });

  it('processes active trace input and updates selected traces', () => {
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    // setting same trace as active does not affect selected traces
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = [TraceType.TRANSACTIONS];
    expect(component.internalActiveTrace).toEqual(TraceType.TRANSACTIONS);
    expect(component.selectedTraces).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
    ]);

    component.activeViewTraceTypes = [TraceType.WINDOW_MANAGER];
    expect(component.internalActiveTrace).toEqual(TraceType.WINDOW_MANAGER);
    expect(component.selectedTraces).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
    ]);

    // oldest trace to be selected is replaced (SF)
    component.activeViewTraceTypes = [TraceType.PROTO_LOG];
    expect(component.internalActiveTrace).toEqual(TraceType.PROTO_LOG);
    expect(component.selectedTraces).toEqual([
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
    ]);

    // setting active trace that is already selected causes it to become most recent selection
    component.activeViewTraceTypes = [TraceType.TRANSACTIONS];
    expect(component.internalActiveTrace).toEqual(TraceType.TRANSACTIONS);
    expect(component.selectedTraces).toEqual([
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
      TraceType.TRANSACTIONS,
    ]);

    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([
      TraceType.PROTO_LOG,
      TraceType.TRANSACTIONS,
      TraceType.SURFACE_FLINGER,
    ]);
  });

  it('handles undefined active trace input', () => {
    component.activeViewTraceTypes = undefined;
    expect(component.internalActiveTrace).toBeUndefined();
    expect(component.selectedTraces).toEqual([]);

    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = undefined;
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);
  });

  it('sorts selected traces correctly for display in selection trigger', () => {
    loadTracesForSelectorTest();

    fixture.whenStable().then(() => {
      let selectionTriggerIcons = Array.from(
        htmlElement.querySelectorAll('.mat-select-trigger .mat-icon'),
      );
      expect(selectionTriggerIcons.length).toEqual(1);
      expect(selectionTriggerIcons[0].innerHTML).toContain(
        TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
      );

      component.activeViewTraceTypes = [TraceType.TRANSACTIONS];
      selectionTriggerIcons = Array.from(
        htmlElement.querySelectorAll('.mat-select-trigger mat-icon'),
      );
      expect(selectionTriggerIcons.length).toEqual(2);
      expect(selectionTriggerIcons[0].innerHTML).toContain(
        TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
      );
      expect(selectionTriggerIcons[1].innerHTML).toContain(
        TRACE_INFO[TraceType.TRANSACTIONS].icon,
      );

      component.activeViewTraceTypes = [TraceType.WINDOW_MANAGER];
      selectionTriggerIcons = Array.from(
        htmlElement.querySelectorAll('.mat-select-trigger mat-icon'),
      );
      expect(selectionTriggerIcons.length).toEqual(3);
      expect(selectionTriggerIcons[0].innerHTML).toContain(
        TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
      );
      expect(selectionTriggerIcons[1].innerHTML).toContain(
        TRACE_INFO[TraceType.WINDOW_MANAGER].icon,
      );
      expect(selectionTriggerIcons[2].innerHTML).toContain(
        TRACE_INFO[TraceType.TRANSACTIONS].icon,
      );

      component.activeViewTraceTypes = [TraceType.PROTO_LOG];
      selectionTriggerIcons = Array.from(
        htmlElement.querySelectorAll('.mat-select-trigger mat-icon'),
      );
      expect(selectionTriggerIcons.length).toEqual(3);
      expect(selectionTriggerIcons[0].innerHTML).toContain(
        TRACE_INFO[TraceType.WINDOW_MANAGER].icon,
      );
      expect(selectionTriggerIcons[1].innerHTML).toContain(
        TRACE_INFO[TraceType.TRANSACTIONS].icon,
      );
      expect(selectionTriggerIcons[2].innerHTML).toContain(
        TRACE_INFO[TraceType.PROTO_LOG].icon,
      );
    });
  });

  it('updates trace selection using selector', () => {
    loadTracesForSelectorTest();

    const selectTrigger = assertDefined(
      htmlElement.querySelector('.mat-select-trigger'),
    );
    (selectTrigger as HTMLElement).click();
    fixture.detectChanges();

    fixture.whenStable().then(() => {
      const matOptions = Array.from(
        assertDefined(htmlElement.querySelectorAll('mat-select mat-option')),
      );

      expect((matOptions[0] as HTMLInputElement).value).toEqual(
        `${TraceType.SURFACE_FLINGER}`,
      );
      expect((matOptions[0] as HTMLInputElement).disabled).toBeTrue();
      for (let i = 1; i < 4; i++) {
        expect((matOptions[i] as HTMLInputElement).disabled).toBeFalse();
      }

      (matOptions[1] as HTMLElement).click();
      (matOptions[2] as HTMLElement).click();
      expect(component.selectedTraces).toEqual([
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
        TraceType.SCREEN_RECORDING,
      ]);

      expect((matOptions[3] as HTMLInputElement).value).toEqual(
        `${TraceType.PROTO_LOG}`,
      );
      expect((matOptions[3] as HTMLInputElement).disabled).toBeTrue();
    });
  });

  it('next button disabled if no next entry', () => {
    loadTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );

    const nextEntryButton = assertDefined(
      fixture.debugElement.query(By.css('#next_entry_button')),
    );
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();
  });

  it('prev button disabled if no prev entry', () => {
    loadTraces();
    const timelineData = assertDefined(component.timelineData);

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      100n,
    );
    const prevEntryButton = assertDefined(
      fixture.debugElement.query(By.css('#prev_entry_button')),
    );
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();
  });

  it('next button enabled for different active viewers', () => {
    loadTraces();

    const nextEntryButton = assertDefined(
      fixture.debugElement.query(By.css('#next_entry_button')),
    );
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    component.activeViewTraceTypes = [TraceType.WINDOW_MANAGER];
    component.internalActiveTrace = TraceType.WINDOW_MANAGER;
    fixture.detectChanges();

    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();
  });

  it('changes timestamp on next entry button press', () => {
    loadTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const nextEntryButton = assertDefined(
      fixture.debugElement.query(By.css('#next_entry_button')),
    );

    testCurrentTimestampOnButtonClick(nextEntryButton, position105, 110n);

    testCurrentTimestampOnButtonClick(nextEntryButton, position100, 110n);

    testCurrentTimestampOnButtonClick(nextEntryButton, position90, 100n);

    // No change when we are already on the last timestamp of the active trace
    testCurrentTimestampOnButtonClick(nextEntryButton, position110, 110n);

    // No change when we are after the last entry of the active trace
    testCurrentTimestampOnButtonClick(nextEntryButton, position112, 112n);
  });

  it('changes timestamp on previous entry button press', () => {
    loadTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const prevEntryButton = assertDefined(
      fixture.debugElement.query(By.css('#prev_entry_button')),
    );

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

  //TODO(b/304982982): find a way to test via dom interactions, not calling listener directly
  it('performs expected action on arrow key press depending on input form focus', () => {
    loadTraces();

    const spyNextEntry = spyOn(component, 'moveToNextEntry');
    const spyPrevEntry = spyOn(component, 'moveToPreviousEntry');

    component.handleKeyboardEvent(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    expect(spyNextEntry).toHaveBeenCalled();

    const formElement = htmlElement.querySelector('.time-input input');
    const focusInEvent = new FocusEvent('focusin');
    Object.defineProperty(focusInEvent, 'target', {value: formElement});
    component.handleFocusInEvent(focusInEvent);
    fixture.detectChanges();

    component.handleKeyboardEvent(
      new KeyboardEvent('keydown', {key: 'ArrowLeft'}),
    );
    fixture.detectChanges();
    expect(spyPrevEntry).not.toHaveBeenCalled();

    const focusOutEvent = new FocusEvent('focusout');
    Object.defineProperty(focusOutEvent, 'target', {value: formElement});
    component.handleFocusOutEvent(focusOutEvent);
    fixture.detectChanges();

    component.handleKeyboardEvent(
      new KeyboardEvent('keydown', {key: 'ArrowLeft'}),
    );
    fixture.detectChanges();
    expect(spyPrevEntry).toHaveBeenCalled();
  });

  it('updates position based on ns input field', () => {
    loadTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const timeInputField = assertDefined(
      fixture.debugElement.query(By.css('.time-input.nano')),
    );

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

  it('updates position based on real time input field', () => {
    loadTraces();

    expect(
      assertDefined(component.timelineData)
        .getCurrentPosition()
        ?.timestamp.getValueNs(),
    ).toEqual(100n);
    const timeInputField = assertDefined(
      fixture.debugElement.query(By.css('.time-input.real')),
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position105,
      '1970-01-01T00:00:00.000000110',
      110n,
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position100,
      '1970-01-01T00:00:00.000000110',
      110n,
    );

    testCurrentTimestampOnTimeInput(
      timeInputField,
      position90,
      '1970-01-01T00:00:00.000000100',
      100n,
    );

    // No change when we are already on the last timestamp of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position110,
      '1970-01-01T00:00:00.000000110',
      110n,
    );

    // No change when we are after the last entry of the active trace
    testCurrentTimestampOnTimeInput(
      timeInputField,
      position112,
      '1970-01-01T00:00:00.000000112',
      112n,
    );
  });

  function loadTraces() {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [
        time90,
        time101,
        time110,
        time112,
      ])
      .build();

    const timelineData = assertDefined(component.timelineData);
    timelineData.initialize(traces, undefined);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    timelineData.setPosition(position100);
    fixture.detectChanges();
  }

  function loadTracesForSelectorTest() {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time100, time110])
      .setTimestamps(TraceType.SCREEN_RECORDING, [time110])
      .setTimestamps(TraceType.PROTO_LOG, [time100])
      .build();
    assertDefined(component.timelineData).initialize(traces, undefined);
    component.availableTraces = [
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.SCREEN_RECORDING,
      TraceType.PROTO_LOG,
    ];
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    fixture.detectChanges();
  }

  function testCurrentTimestampOnButtonClick(
    button: DebugElement,
    pos: TracePosition,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    fixture.detectChanges();
    button.nativeElement.click();
    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }

  function testCurrentTimestampOnTimeInput(
    inputField: DebugElement,
    pos: TracePosition,
    textInput: string,
    expectedNs: bigint,
  ) {
    const timelineData = assertDefined(component.timelineData);
    timelineData.setPosition(pos);
    fixture.detectChanges();

    inputField.nativeElement.value = textInput;
    inputField.nativeElement.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(
      expectedNs,
    );
  }
});

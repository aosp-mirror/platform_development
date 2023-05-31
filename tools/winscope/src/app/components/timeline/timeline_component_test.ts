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

import {ChangeDetectionStrategy} from '@angular/core';
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
import {TracesBuilder} from 'test/unit/traces_builder';
import {RealTimestamp} from 'trace/timestamp';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {ExpandedTimelineComponent} from './expanded_timeline_component';
import {MiniTimelineComponent} from './mini_timeline_component';
import {SingleTimelineComponent} from './single_timeline_component';
import {TimelineComponent} from './timeline_component';

describe('TimelineComponent', () => {
  const time90 = new RealTimestamp(90n);
  const time100 = new RealTimestamp(100n);
  const time101 = new RealTimestamp(101n);
  const time110 = new RealTimestamp(110n);
  const time112 = new RealTimestamp(112n);

  const position90 = TracePosition.fromTimestamp(time90);
  const position100 = TracePosition.fromTimestamp(time100);
  const position105 = TracePosition.fromTimestamp(new RealTimestamp(105n));
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
      ],
      declarations: [
        ExpandedTimelineComponent,
        SingleTimelineComponent,
        MatDrawer,
        MatDrawerContainer,
        MatDrawerContent,
        MiniTimelineComponent,
        TimelineComponent,
      ],
    })
      .overrideComponent(TimelineComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    const timelineData = new TimelineData();
    component.timelineData = timelineData;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can be expanded', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .build();
    component.timelineData.initialize(traces, undefined);
    fixture.detectChanges();

    const button = htmlElement.querySelector(`.${component.TOGGLE_BUTTON_CLASS}`);
    expect(button).toBeTruthy();

    // initially not expanded
    let expandedTimelineElement = fixture.debugElement.query(
      By.directive(ExpandedTimelineComponent)
    );
    expect(expandedTimelineElement).toBeFalsy();

    button!.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(By.directive(ExpandedTimelineComponent));
    expect(expandedTimelineElement).toBeTruthy();

    button!.dispatchEvent(new Event('click'));
    expandedTimelineElement = fixture.debugElement.query(By.directive(ExpandedTimelineComponent));
    expect(expandedTimelineElement).toBeFalsy();
  });

  it('handles empty traces', () => {
    const traces = new TracesBuilder().setEntries(TraceType.SURFACE_FLINGER, []).build();
    component.timelineData.initialize(traces, undefined);
    fixture.detectChanges();

    // no expand button
    const button = htmlElement.querySelector(`.${component.TOGGLE_BUTTON_CLASS}`);
    expect(button).toBeFalsy();

    // no timelines shown
    const miniTimelineElement = fixture.debugElement.query(By.directive(MiniTimelineComponent));
    expect(miniTimelineElement).toBeFalsy();

    // error message shown
    const errorMessageContainer = htmlElement.querySelector('.no-timestamps-msg');
    expect(errorMessageContainer).toBeTruthy();
    expect(errorMessageContainer!.textContent).toContain('No timeline to show!');
  });

  it('handles some empty traces', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time100])
      .build();
    component.timelineData.initialize(traces, undefined);
    fixture.detectChanges();
  });

  it('processes active trace input and updates selected traces', () => {
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    expect(component.internalActiveTrace).toEqual(TraceType.SURFACE_FLINGER);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER]);

    component.activeViewTraceTypes = [TraceType.TRANSACTIONS];
    expect(component.internalActiveTrace).toEqual(TraceType.TRANSACTIONS);
    expect(component.selectedTraces).toEqual([TraceType.SURFACE_FLINGER, TraceType.TRANSACTIONS]);

    component.activeViewTraceTypes = [TraceType.WINDOW_MANAGER];
    expect(component.internalActiveTrace).toEqual(TraceType.WINDOW_MANAGER);
    expect(component.selectedTraces).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
    ]);

    component.activeViewTraceTypes = [TraceType.PROTO_LOG];
    expect(component.internalActiveTrace).toEqual(TraceType.PROTO_LOG);
    expect(component.selectedTraces).toEqual([
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
      TraceType.PROTO_LOG,
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

  it('next button disabled if no next entry', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time90, time101, time110, time112])
      .build();
    component.timelineData.initialize(traces, undefined);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    component.timelineData.setPosition(position100);
    fixture.detectChanges();

    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);

    const nextEntryButton = fixture.debugElement.query(By.css('#next_entry_button'));
    expect(nextEntryButton).toBeTruthy();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    component.timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    component.timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    component.timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(nextEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();
  });

  it('prev button disabled if no prev entry', () => {
    const builder = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time90, time101, time110, time112]);
    const traces = builder.build();
    component.timelineData.initialize(traces, undefined);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    component.timelineData.setPosition(position100);
    fixture.detectChanges();

    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);
    const prevEntryButton = fixture.debugElement.query(By.css('#prev_entry_button'));
    expect(prevEntryButton).toBeTruthy();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    component.timelineData.setPosition(position90);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeTruthy();

    component.timelineData.setPosition(position110);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();

    component.timelineData.setPosition(position112);
    fixture.detectChanges();
    expect(prevEntryButton.nativeElement.getAttribute('disabled')).toBeFalsy();
  });

  it('changes timestamp on next entry button press', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time90, time101, time110, time112])
      .build();
    component.timelineData.initialize(traces, undefined);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    component.timelineData.setPosition(position100);
    fixture.detectChanges();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);
    const nextEntryButton = fixture.debugElement.query(By.css('#next_entry_button'));
    expect(nextEntryButton).toBeTruthy();

    component.timelineData.setPosition(position105);
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(110n);

    component.timelineData.setPosition(position100);
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(110n);

    component.timelineData.setPosition(position90);
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);

    // No change when we are already on the last timestamp of the active trace
    component.timelineData.setPosition(position110);
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(110n);

    // No change when we are after the last entry of the active trace
    component.timelineData.setPosition(position112);
    fixture.detectChanges();
    nextEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(112n);
  });

  it('changes timestamp on previous entry button press', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [time100, time110])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time90, time101, time110, time112])
      .build();
    component.timelineData.initialize(traces, undefined);
    component.activeViewTraceTypes = [TraceType.SURFACE_FLINGER];
    component.timelineData.setPosition(position100);
    fixture.detectChanges();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);
    const prevEntryButton = fixture.debugElement.query(By.css('#prev_entry_button'));
    expect(prevEntryButton).toBeTruthy();

    // In this state we are already on the first entry at timestamp 100, so
    // there is no entry to move to before and we just don't update the timestamp
    component.timelineData.setPosition(position105);
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(105n);

    component.timelineData.setPosition(position110);
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);

    // Active entry here should be 110 so moving back means moving to 100.
    component.timelineData.setPosition(position112);
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);

    // No change when we are already on the first timestamp of the active trace
    component.timelineData.setPosition(position100);
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(100n);

    // No change when we are before the first entry of the active trace
    component.timelineData.setPosition(position90);
    fixture.detectChanges();
    prevEntryButton.nativeElement.click();
    expect(component.timelineData.getCurrentPosition()?.timestamp.getValueNs()).toEqual(90n);
  });
});

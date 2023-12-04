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
import {ChangeDetectionStrategy} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {RealTimestamp} from 'common/time';
import {Transition} from 'flickerlib/common';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {DefaultTimelineRowComponent} from './default_timeline_row_component';
import {ExpandedTimelineComponent} from './expanded_timeline_component';
import {TransitionTimelineComponent} from './transition_timeline_component';

describe('ExpandedTimelineComponent', () => {
  let fixture: ComponentFixture<ExpandedTimelineComponent>;
  let component: ExpandedTimelineComponent;
  let htmlElement: HTMLElement;
  let timelineData: TimelineData;

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
        TransitionTimelineComponent,
        DefaultTimelineRowComponent,
      ],
    })
      .overrideComponent(ExpandedTimelineComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(ExpandedTimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    timelineData = new TimelineData();
    const traces = new TracesBuilder()
      .setEntries(TraceType.SURFACE_FLINGER, [{}])
      .setTimestamps(TraceType.SURFACE_FLINGER, [new RealTimestamp(10n)])
      .setEntries(TraceType.WINDOW_MANAGER, [{}])
      .setTimestamps(TraceType.WINDOW_MANAGER, [new RealTimestamp(11n)])
      .setEntries(TraceType.TRANSACTIONS, [{}])
      .setTimestamps(TraceType.TRANSACTIONS, [new RealTimestamp(12n)])
      .setEntries(TraceType.TRANSITION, [
        {
          createTime: {unixNanos: 10n},
          finishTime: {unixNanos: 30n},
        } as Transition,
        {
          createTime: {unixNanos: 60n},
          finishTime: {unixNanos: 110n},
        } as Transition,
      ])
      .setTimestamps(TraceType.TRANSITION, [new RealTimestamp(10n), new RealTimestamp(60n)])
      .setTimestamps(TraceType.PROTO_LOG, [])
      .build();
    timelineData.initialize(traces, undefined);
    component.timelineData = timelineData;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders all timelines', () => {
    fixture.detectChanges();

    const timelineElements = htmlElement.querySelectorAll('.timeline.row single-timeline');
    expect(timelineElements.length).toEqual(4);

    const transitionElement = htmlElement.querySelectorAll('.timeline.row transition-timeline');
    expect(transitionElement.length).toEqual(1);
  });

  it('passes initial selectedEntry of correct type into each timeline', () => {
    fixture.detectChanges();

    const singleTimelines = assertDefined(component.singleTimelines);
    expect(singleTimelines.length).toBe(4);

    // initially only first entry of SF is set
    singleTimelines.forEach((timeline) => {
      if (timeline.trace.type === TraceType.SURFACE_FLINGER) {
        const entry = assertDefined(timeline.selectedEntry);
        expect(entry.getFullTrace().type).toBe(TraceType.SURFACE_FLINGER);
      } else {
        expect(timeline.selectedEntry).toBeUndefined();
      }
    });

    const transitionTimeline = assertDefined(component.transitionTimelines).first;
    assertDefined(transitionTimeline.selectedEntry);
  });

  it('passes selectedEntry of correct type into each timeline on position change', () => {
    // 3 out of the 5 traces have timestamps before or at 11n
    component.timelineData.setPosition(TracePosition.fromTimestamp(new RealTimestamp(11n)));
    fixture.detectChanges();

    const singleTimelines = assertDefined(component.singleTimelines);
    expect(singleTimelines.length).toBe(4);

    singleTimelines.forEach((timeline) => {
      // protolog and transactions traces have no timestamps before current position
      if (
        timeline.trace.type === TraceType.PROTO_LOG ||
        timeline.trace.type === TraceType.TRANSACTIONS
      ) {
        expect(timeline.selectedEntry).toBeUndefined();
      } else {
        const selectedEntry = assertDefined(timeline.selectedEntry);
        expect(selectedEntry.getFullTrace().type).toEqual(timeline.trace.type);
      }
    });

    const transitionTimeline = assertDefined(component.transitionTimelines).first;
    const selectedEntry = assertDefined(transitionTimeline.selectedEntry);
    expect(selectedEntry.getFullTrace().type).toEqual(transitionTimeline.trace.type);
  });

  it('getAllLoadedTraces causes timelines to render in correct order', () => {
    // traces in timelineData are in order of being set in Traces API
    expect(component.timelineData.getTraces().mapTrace((trace) => trace.type)).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.TRANSACTIONS,
      TraceType.TRANSITION,
      TraceType.PROTO_LOG,
    ]);

    // getAllLoadedTraces returns traces in enum order
    expect(component.getTracesSortedByDisplayOrder().map((trace) => trace.type)).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.TRANSACTIONS,
      TraceType.PROTO_LOG,
      TraceType.TRANSITION,
    ]);
  });
});

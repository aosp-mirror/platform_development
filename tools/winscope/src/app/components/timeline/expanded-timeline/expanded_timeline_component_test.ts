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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
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
  const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const time11 = TimestampConverterUtils.makeRealTimestamp(11n);
  const time12 = TimestampConverterUtils.makeRealTimestamp(12n);
  const time30 = TimestampConverterUtils.makeRealTimestamp(30n);
  const time60 = TimestampConverterUtils.makeRealTimestamp(60n);
  const time110 = TimestampConverterUtils.makeRealTimestamp(110n);

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
      .setTimestamps(TraceType.SURFACE_FLINGER, [time10])
      .setEntries(TraceType.WINDOW_MANAGER, [{}])
      .setTimestamps(TraceType.WINDOW_MANAGER, [time11])
      .setEntries(TraceType.TRANSACTIONS, [{}])
      .setTimestamps(TraceType.TRANSACTIONS, [time12])
      .setEntries(TraceType.TRANSITION, [
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time30}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time10}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time110}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time60}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps(TraceType.TRANSITION, [time10, time60])
      .setTimestamps(TraceType.PROTO_LOG, [time12])
      .build();
    await timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    component.timelineData = timelineData;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders all timelines', () => {
    fixture.detectChanges();

    const timelineElements = htmlElement.querySelectorAll(
      '.timeline.row single-timeline',
    );
    expect(timelineElements.length).toEqual(4);

    const transitionElement = htmlElement.querySelectorAll(
      '.timeline.row transition-timeline',
    );
    expect(transitionElement.length).toEqual(1);
  });

  it('passes initial selectedEntry of correct type into each timeline', () => {
    fixture.detectChanges();

    const singleTimelines = assertDefined(component.singleTimelines);
    expect(singleTimelines.length).toBe(4);

    // initially only first entry of SF is set
    singleTimelines.forEach((timeline) => {
      if (assertDefined(timeline.trace).type === TraceType.SURFACE_FLINGER) {
        const entry = assertDefined(timeline.selectedEntry);
        expect(entry.getFullTrace().type).toBe(TraceType.SURFACE_FLINGER);
      } else {
        expect(timeline.selectedEntry).toBeUndefined();
      }
    });

    const transitionTimeline = assertDefined(
      component.transitionTimelines,
    ).first;
    assertDefined(transitionTimeline.selectedEntry);
  });

  it('passes selectedEntry of correct type into each timeline on position change', () => {
    // 3 out of the 5 traces have timestamps before or at 11n
    assertDefined(component.timelineData).setPosition(
      TracePosition.fromTimestamp(time11),
    );
    fixture.detectChanges();

    const singleTimelines = assertDefined(component.singleTimelines);
    expect(singleTimelines.length).toBe(4);

    singleTimelines.forEach((timeline) => {
      // protolog and transactions traces have no timestamps before current position
      if (
        assertDefined(timeline.trace).type === TraceType.PROTO_LOG ||
        assertDefined(timeline.trace).type === TraceType.TRANSACTIONS
      ) {
        expect(timeline.selectedEntry).toBeUndefined();
      } else {
        const selectedEntry = assertDefined(timeline.selectedEntry);
        expect(selectedEntry.getFullTrace().type).toEqual(
          assertDefined(timeline.trace).type,
        );
      }
    });

    const transitionTimeline = assertDefined(
      component.transitionTimelines,
    ).first;
    const selectedEntry = assertDefined(transitionTimeline.selectedEntry);
    expect(selectedEntry.getFullTrace().type).toEqual(
      assertDefined(transitionTimeline.trace).type,
    );
  });

  it('getAllLoadedTraces causes timelines to render in correct order', () => {
    // traces in timelineData are in order of being set in Traces API
    expect(
      assertDefined(component.timelineData)
        .getTraces()
        .mapTrace((trace) => trace.type),
    ).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.TRANSACTIONS,
      TraceType.TRANSITION,
      TraceType.PROTO_LOG,
    ]);

    // getAllLoadedTraces returns traces in enum order
    expect(
      component.getTracesSortedByDisplayOrder().map((trace) => trace.type),
    ).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
      TraceType.TRANSACTIONS,
      TraceType.PROTO_LOG,
      TraceType.TRANSITION,
    ]);
  });
});

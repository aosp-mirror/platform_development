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
import {RealTimestamp} from 'common/time';
import {Transition} from 'flickerlib/common';
import {TracesBuilder} from 'test/unit/traces_builder';
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
      .build();
    timelineData.initialize(traces, undefined);
    component.timelineData = timelineData;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders all timelines', () => {
    fixture.detectChanges();

    const timelines = htmlElement.querySelectorAll('.timeline.row');
    expect(timelines.length).toEqual(4);
  });
});

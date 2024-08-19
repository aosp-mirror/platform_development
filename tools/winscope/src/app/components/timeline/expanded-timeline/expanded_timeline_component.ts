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

import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
  QueryList,
  ViewChildren,
} from '@angular/core';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {Trace} from 'trace/trace';
import {TRACE_INFO} from 'trace/trace_info';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {AbstractTimelineRowComponent} from './abstract_timeline_row_component';
import {DefaultTimelineRowComponent} from './default_timeline_row_component';
import {TransitionTimelineComponent} from './transition_timeline_component';

@Component({
  selector: 'expanded-timeline',
  template: `
    <div id="expanded-timeline-wrapper" #expandedTimelineWrapper>
      <div
          *ngFor="let trace of getTracesSortedByDisplayOrder(); trackBy: trackTraceByType"
          class="timeline row">
        <div class="icon-wrapper">
          <mat-icon
              class="icon"
              [matTooltip]="TRACE_INFO[trace.type].name"
              [style]="{color: TRACE_INFO[trace.type].color}">
            {{ TRACE_INFO[trace.type].icon }}
          </mat-icon>
        </div>
        <transition-timeline
            *ngIf="trace.type === TraceType.TRANSITION"
            [color]="TRACE_INFO[trace.type].color"
            [trace]="trace"
            [transitionEntries]="timelineData.getTransitionEntries()"
            [selectedEntry]="timelineData.findCurrentEntryFor(trace)"
            [selectionRange]="timelineData.getSelectionTimeRange()"
            [fullRange]="timelineData.getFullTimeRange()"
            [timestampConverter]="timelineData.getTimestampConverter()"
            [isActive]="isActiveTrace(trace)"
            (onTracePositionUpdate)="onTracePositionUpdate.emit($event)"
            (onScrollEvent)="updateScroll($event)"
            (onTraceClicked)="onTraceClicked.emit($event)"
            (onMouseXRatioUpdate)="onMouseXRatioUpdate.emit($event)"
            class="single-timeline">
        </transition-timeline>
        <single-timeline
            *ngIf="trace.type !== TraceType.TRANSITION"
            [color]="TRACE_INFO[trace.type].color"
            [trace]="trace"
            [selectedEntry]="timelineData.findCurrentEntryFor(trace)"
            [selectionRange]="timelineData.getSelectionTimeRange()"
            [timestampConverter]="timelineData.getTimestampConverter()"
            [isActive]="isActiveTrace(trace)"
            (onTracePositionUpdate)="onTracePositionUpdate.emit($event)"
            (onScrollEvent)="updateScroll($event)"
            (onTraceClicked)="onTraceClicked.emit($event)"
            (onMouseXRatioUpdate)="onMouseXRatioUpdate.emit($event)"
            class="single-timeline">
        </single-timeline>

        <div class="icon-wrapper">
          <mat-icon class="icon placeholder-icon"></mat-icon>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      #expanded-timeline-wrapper {
        display: flex;
        flex-direction: column;
        height: 100%;
        position: relative;
      }
      #pointer-overlay {
        pointer-events: none;
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        display: flex;
        align-items: stretch;
      }
      .timeline {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: center;
        width: 100%;
      }
      .timeline.row {
        border-bottom: 1px solid var(--drawer-block-primary);
      }
      .timeline .single-timeline {
        flex-grow: 1;
      }
      .selection-cursor {
        flex-grow: 1;
      }
      .icon-wrapper {
        align-self: stretch;
        display: flex;
        justify-content: center;
        background-color: var(--drawer-block-primary);
      }
      .icon {
        margin: 1rem;
        align-self: center;
      }
      .units-row {
        flex-grow: 1;
        align-self: baseline;
      }
      .units-row .placeholder-icon {
        visibility: hidden;
      }
    `,
  ],
})
export class ExpandedTimelineComponent {
  @Input() timelineData: TimelineData | undefined;
  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();
  @Output() readonly onScrollEvent = new EventEmitter<WheelEvent>();
  @Output() readonly onTraceClicked = new EventEmitter<Trace<object>>();
  @Output() readonly onMouseXRatioUpdate = new EventEmitter<
    number | undefined
  >();

  @ViewChildren(DefaultTimelineRowComponent)
  singleTimelines: QueryList<DefaultTimelineRowComponent> | undefined;

  @ViewChildren(TransitionTimelineComponent)
  transitionTimelines: QueryList<TransitionTimelineComponent> | undefined;

  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.resizeCanvases();
  }

  trackTraceByType = (index: number, trace: Trace<{}>): TraceType => {
    return trace.type;
  };

  getTracesSortedByDisplayOrder(): Array<Trace<{}>> {
    const traces = assertDefined(this.timelineData)
      .getTraces()
      .mapTrace((trace) => trace);
    return traces.sort((a, b) =>
      TraceTypeUtils.compareByDisplayOrder(a.type, b.type),
    );
  }

  updateScroll(event: WheelEvent) {
    this.onScrollEvent.emit(event);
  }

  isActiveTrace(trace: Trace<object>) {
    return trace === this.timelineData?.getActiveTrace();
  }

  private resizeCanvases() {
    // Reset any size before computing new size to avoid it interfering with size computations.
    // Needs to be done together because otherwise the sizes of each timeline will interfere with
    // each other, since if one timeline is still too big the container will stretch to that size.
    const timelines = [
      ...(this.transitionTimelines as QueryList<
        AbstractTimelineRowComponent<{}>
      >),
      ...(this.singleTimelines as QueryList<AbstractTimelineRowComponent<{}>>),
    ];
    for (const timeline of timelines) {
      timeline.getCanvas().width = 0;
      timeline.getCanvas().height = 0;
      timeline.getCanvas().style.width = 'auto';
      timeline.getCanvas().style.height = 'auto';
    }

    for (const timeline of timelines) {
      timeline.initializeCanvas();
      timeline.getCanvas().height = 0;
      timeline.getCanvas().style.width = 'auto';
      timeline.getCanvas().style.height = 'auto';
    }
  }
}

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
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  QueryList,
  ViewChild,
  ViewChildren,
} from '@angular/core';
import {TimelineData} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {Timestamp} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {SingleTimelineComponent} from './single_timeline_component';

@Component({
  selector: 'expanded-timeline',
  template: `
    <div id="expanded-timeline-wrapper" #expandedTimelineWrapper>
      <div
        *ngFor="let trace of getTraces(); trackBy: trackTraceBySelectedTimestamp"
        class="timeline">
        <div class="icon-wrapper">
          <mat-icon
            class="icon"
            [matTooltip]="TRACE_INFO[trace.type].name"
            [style]="{color: TRACE_INFO[trace.type].color}">
            {{ TRACE_INFO[trace.type].icon }}
          </mat-icon>
        </div>
        <single-timeline
          [color]="TRACE_INFO[trace.type].color"
          [trace]="trace"
          [selectedEntry]="timelineData.findCurrentEntryFor(trace.type)"
          [selectionRange]="timelineData.getSelectionTimeRange()"
          (onTracePositionUpdate)="onTracePositionUpdate.emit($event)"
          class="single-timeline"></single-timeline>
        <div class="icon-wrapper">
          <mat-icon class="icon placeholder-icon"></mat-icon>
        </div>
      </div>

      <!-- A filler row matching the format and colors of filled rows but with no content -->
      <div class="timeline units-row">
        <div class="icon-wrapper">
          <mat-icon class="icon placeholder-icon"></mat-icon>
        </div>
        <div class="single-timeline"></div>
        <div class="icon-wrapper">
          <mat-icon class="icon placeholder-icon"></mat-icon>
        </div>
      </div>

      <!-- TODO: Implement properly later when we have more time -->
      <!-- <div id="pointer-overlay" class="timeline">
        <div class="icon-wrapper" [style]="{ visibility: 'hidden' }">
          <mat-icon class="icon placeholder-icon">home</mat-icon>
        </div>
        <selection-cursor
          class="selection-cursor"
          [currentTimestamp]="currentTimestamp"
          [from]="presenter.selection.from"
          [to]="presenter.selection.to"
        ></selection-cursor>
      </div> -->
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
      .timeline .single-timeline {
        flex-grow: 1;
      }
      .selection-cursor {
        flex-grow: 1;
      }
      .timeline {
        border-bottom: 1px solid #f1f3f4;
      }
      .icon-wrapper {
        background-color: #f1f3f4;
        align-self: stretch;
        display: flex;
        justify-content: center;
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
  @Input() timelineData!: TimelineData;
  @Output() onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('expandedTimelineWrapper', {static: false}) warpperRef!: ElementRef;
  @ViewChildren(SingleTimelineComponent) singleTimelines!: QueryList<SingleTimelineComponent>;

  TRACE_INFO = TRACE_INFO;

  getTraces(): Array<Trace<{}>> {
    const traces = new Array<Trace<{}>>();
    this.timelineData.getTraces().forEachTrace((trace) => {
      traces.push(trace);
    });
    return traces;
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.resizeCanvases();
  }

  trackTraceBySelectedTimestamp = (index: number, trace: Trace<{}>): Timestamp | undefined => {
    return this.timelineData.findCurrentEntryFor(trace.type)?.getTimestamp();
  };

  private resizeCanvases() {
    // Reset any size before computing new size to avoid it interfering with size computations.
    // Needs to be done together because otherwise the sizes of each timeline will interfere with
    // each other, since if one timeline is still too big the container will stretch to that size.
    for (const timeline of this.singleTimelines) {
      timeline.canvas.width = 0;
      timeline.canvas.height = 0;
      timeline.canvas.style.width = 'auto';
      timeline.canvas.style.height = 'auto';
    }

    for (const timeline of this.singleTimelines) {
      timeline.initializeCanvas();
      timeline.canvas.height = 0;
      timeline.canvas.style.width = 'auto';
      timeline.canvas.style.height = 'auto';
    }
  }
}

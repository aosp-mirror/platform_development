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
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry_types';
import {Rect} from 'common/rect';
import {TimeRange, Timestamp} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {AbstractTimelineRowComponent} from './abstract_timeline_row_component';

@Component({
  selector: 'single-timeline',
  template: `
    <div class="single-timeline" #wrapper>
      <canvas #canvas></canvas>
    </div>
  `,
  styles: [
    `
      .single-timeline {
        height: 2rem;
        padding: 1rem 0;
      }
    `,
  ],
})
export class DefaultTimelineRowComponent extends AbstractTimelineRowComponent<{}> {
  @Input() color = '#AF5CF7';
  @Input() trace: Trace<{}> | undefined;
  @Input() selectedEntry: TraceEntry<{}> | undefined;
  @Input() selectionRange: TimeRange | undefined;

  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) canvasRef!: ElementRef;
  @ViewChild('wrapper', {static: false}) wrapperRef!: ElementRef;

  hoveringEntry?: Timestamp;
  hoveringSegment?: TimeRange;

  getEntryWidth() {
    return this.canvasDrawer.getScaledCanvasHeight();
  }

  getAvailableWidth() {
    return Math.floor(
      this.canvasDrawer.getScaledCanvasWidth() - this.getEntryWidth(),
    );
  }

  ngOnInit() {
    if (!this.trace || !this.selectionRange) {
      throw Error('Not all required inputs have been set');
    }
  }

  override onHover(mousePoint: Point) {
    this.drawEntryHover(mousePoint);
  }

  override handleMouseOut(e: MouseEvent) {
    if (this.hoveringEntry || this.hoveringSegment) {
      // If undefined there is no current hover effect so no need to clear
      this.redraw();
    }
    this.hoveringEntry = undefined;
    this.hoveringSegment = undefined;
  }

  override async drawTimeline() {
    assertDefined(this.trace)
      .sliceTime(
        assertDefined(this.selectionRange).from,
        assertDefined(this.selectionRange).to,
      )
      .forEachTimestamp((entry) => {
        this.drawEntry(entry);
      });
    this.drawSelectedEntry();
  }

  protected override async getEntryAt(
    mousePoint: Point,
  ): Promise<TraceEntry<{}> | undefined> {
    const timestampOfClick = this.getTimestampOf(mousePoint.x);
    const candidateEntry = assertDefined(this.trace).findLastLowerOrEqualEntry(
      timestampOfClick,
    );

    if (candidateEntry !== undefined) {
      const timestamp = candidateEntry.getTimestamp();
      const rect = this.entryRect(timestamp);
      if (rect.containsPoint(mousePoint)) {
        return candidateEntry;
      }
    }

    return undefined;
  }

  private async drawEntryHover(mousePoint: Point) {
    const currentHoverEntry = (
      await this.getEntryAt(mousePoint)
    )?.getTimestamp();

    if (this.hoveringEntry === currentHoverEntry) {
      return;
    }

    if (this.hoveringEntry) {
      // If null there is no current hover effect so no need to clear
      this.canvasDrawer.clear();
      this.drawTimeline();
    }

    this.hoveringEntry = currentHoverEntry;

    if (!this.hoveringEntry) {
      return;
    }

    const rect = this.entryRect(this.hoveringEntry);

    this.canvasDrawer.drawRect(rect, this.color, 1.0);
    this.canvasDrawer.drawRectBorder(rect);
  }

  private entryRect(entry: Timestamp, padding = 0): Rect {
    const xPos = this.getXPosOf(entry);

    return new Rect(
      xPos + padding,
      padding,
      this.getEntryWidth() - 2 * padding,
      this.getEntryWidth() - 2 * padding,
    );
  }

  private getXPosOf(entry: Timestamp): number {
    const start = assertDefined(this.selectionRange).from.getValueNs();
    const end = assertDefined(this.selectionRange).to.getValueNs();

    return Number(
      (BigInt(this.getAvailableWidth()) * (entry.getValueNs() - start)) /
        (end - start),
    );
  }

  private getTimestampOf(x: number): Timestamp {
    const start = assertDefined(this.selectionRange).from.getValueNs();
    const end = assertDefined(this.selectionRange).to.getValueNs();
    const ts =
      (BigInt(Math.floor(x)) * (end - start)) /
        BigInt(this.getAvailableWidth()) +
      start;
    return NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(
      assertDefined(this.selectionRange).from.getType(),
      ts,
      0n,
    );
  }

  private drawEntry(entry: Timestamp) {
    const rect = this.entryRect(entry);

    this.canvasDrawer.drawRect(rect, this.color, 0.2);
  }

  private drawSelectedEntry() {
    if (this.selectedEntry === undefined) {
      return;
    }

    const rect = this.entryRect(this.selectedEntry.getTimestamp(), 1);
    this.canvasDrawer.drawRect(rect, this.color, 1.0);
    this.canvasDrawer.drawRectBorder(rect);
  }
}

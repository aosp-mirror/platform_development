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

import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {GeometryUtils, Point, Rect} from 'common/geometry_utils';
import {TimeRange, Timestamp} from 'common/time';
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
  @Input() trace!: Trace<{}>;
  @Input() selectedEntry: TraceEntry<{}> | undefined = undefined;
  @Input() selectionRange!: TimeRange;

  @Output() onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) canvasRef!: ElementRef;
  @ViewChild('wrapper', {static: false}) wrapperRef!: ElementRef;

  hoveringEntry?: Timestamp;
  hoveringSegment?: TimeRange;

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

  private async drawEntryHover(mousePoint: Point) {
    const currentHoverEntry = (await this.getEntryAt(mousePoint))?.getTimestamp();

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

  protected override async getEntryAt(mousePoint: Point): Promise<TraceEntry<{}> | undefined> {
    const timestampOfClick = this.getTimestampOf(mousePoint.x);
    const candidateEntry = this.trace.findLastLowerOrEqualEntry(timestampOfClick);

    if (candidateEntry !== undefined) {
      const timestamp = candidateEntry.getTimestamp();
      const rect = this.entryRect(timestamp);
      if (GeometryUtils.isPointInRect(mousePoint, rect)) {
        return candidateEntry;
      }
    }

    return undefined;
  }

  get entryWidth() {
    return this.canvasDrawer.getScaledCanvasHeight();
  }

  get availableWidth() {
    return Math.floor(this.canvasDrawer.getScaledCanvasWidth() - this.entryWidth);
  }

  private entryRect(entry: Timestamp, padding = 0): Rect {
    const xPos = this.getXPosOf(entry);

    return {
      x: xPos + padding,
      y: padding,
      w: this.entryWidth - 2 * padding,
      h: this.entryWidth - 2 * padding,
    };
  }

  private getXPosOf(entry: Timestamp): number {
    const start = this.selectionRange.from.getValueNs();
    const end = this.selectionRange.to.getValueNs();

    return Number((BigInt(this.availableWidth) * (entry.getValueNs() - start)) / (end - start));
  }

  private getTimestampOf(x: number): Timestamp {
    const start = this.selectionRange.from.getValueNs();
    const end = this.selectionRange.to.getValueNs();
    const ts = (BigInt(Math.floor(x)) * (end - start)) / BigInt(this.availableWidth) + start;
    return new Timestamp(this.selectionRange.from.getType(), ts);
  }

  override async drawTimeline() {
    this.trace
      .sliceTime(this.selectionRange.from, this.selectionRange.to)
      .forEachTimestamp((entry) => {
        this.drawEntry(entry);
      });
    this.drawSelectedEntry();
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

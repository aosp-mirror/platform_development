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

import {Component, Input} from '@angular/core';
import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry/point';
import {Rect} from 'common/geometry/rect';
import {TimeRange, Timestamp} from 'common/time';
import {AbsoluteEntryIndex, Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractTimelineRowComponent} from './abstract_timeline_row_component';

@Component({
  selector: 'transition-timeline',
  template: `
    <div
      class="transition-timeline"
      matTooltip="Some or all transitions will not be rendered in timeline due to unknown dispatch and finish or abort time"
      [matTooltipDisabled]="shouldNotRenderEntries.length === 0"
      [style.background-color]="getBackgroundColor()"
      (click)="onTimelineClick($event)"
      #wrapper>
      <canvas
        id="canvas"
        (mousemove)="trackMousePos($event)"
        (mouseleave)="onMouseLeave($event)" #canvas></canvas>
    </div>
  `,
  styles: [
    `
      .transition-timeline {
        height: 4rem;
      }
      .transition-timeline:hover {
        background-color: var(--hover-element-color);
        cursor: pointer;
      }
    `,
  ],
})
export class TransitionTimelineComponent extends AbstractTimelineRowComponent<PropertyTreeNode> {
  @Input() selectedEntry: TraceEntry<PropertyTreeNode> | undefined;
  @Input() trace: Trace<PropertyTreeNode> | undefined;
  @Input() transitionEntries: Array<PropertyTreeNode | undefined> | undefined;
  @Input() fullRange: TimeRange | undefined;

  hoveringEntry?: TraceEntry<PropertyTreeNode>;
  rowsToUse = new Map<number, number>();
  maxRowsRequires = 0;
  shouldNotRenderEntries: number[] = [];

  ngOnInit() {
    assertDefined(this.trace);
    assertDefined(this.selectionRange);
    assertDefined(this.transitionEntries);
    assertDefined(this.fullRange);
    this.computeRowsToUse();
  }

  getAvailableWidth() {
    return this.canvasDrawer.getScaledCanvasWidth();
  }

  override onHover(mousePoint: Point) {
    this.drawSegmentHover(mousePoint);
  }

  override handleMouseOut(e: MouseEvent) {
    if (this.hoveringEntry) {
      // If undefined there is no current hover effect so no need to clear
      this.redraw();
    }
    this.hoveringEntry = undefined;
  }

  override drawTimeline() {
    let selectedRect: Rect | undefined;
    assertDefined(this.trace).forEachEntry((entry) => {
      const index = entry.getIndex();
      const rect = this.getRectFromIndex(entry.getIndex());
      if (!rect) {
        return;
      }
      const transition = assertDefined(this.transitionEntries?.at(index));
      this.drawSegment(rect, transition);
      if (index === this.selectedEntry?.getIndex()) {
        selectedRect = rect;
      }
    });
    if (selectedRect) {
      this.canvasDrawer.drawRectBorder(selectedRect);
    }
  }

  private getRectFromIndex(entryIndex: AbsoluteEntryIndex): Rect | undefined {
    if (this.shouldNotRenderEntries.includes(entryIndex)) {
      return undefined;
    }
    const transition = this.transitionEntries?.at(entryIndex);
    if (!transition) {
      return undefined;
    }
    const timeRange = TimelineUtils.getTimeRangeForTransition(
      transition,
      assertDefined(this.selectionRange),
      assertDefined(this.timestampConverter),
    );
    if (!timeRange) {
      return undefined;
    }
    const row = this.getRowToUseFor(entryIndex);
    return this.getSegmentRect(timeRange.from, timeRange.to, row);
  }

  protected override getEntryAt(
    mousePoint: Point,
  ): TraceEntry<PropertyTreeNode> | undefined {
    if (assertDefined(this.trace).type !== TraceType.TRANSITION) {
      return undefined;
    }

    const transitionEntries = assertDefined(this.trace).mapEntry(
      (entry) => entry,
    );

    for (const entry of transitionEntries) {
      const rect = this.getRectFromIndex(entry.getIndex());
      if (rect?.containsPoint(mousePoint)) {
        return entry;
      }
    }
    return undefined;
  }

  private drawSegmentHover(mousePoint: Point) {
    const currentHoverEntry = this.getEntryAt(mousePoint);

    if (this.hoveringEntry) {
      this.redraw();
    }

    this.hoveringEntry = currentHoverEntry;

    if (!this.hoveringEntry) {
      return;
    }

    const rect = this.getRectFromIndex(this.hoveringEntry.getIndex());
    if (rect) {
      this.canvasDrawer.drawRectBorder(rect);
    }
  }

  private getXPosOf(entry: Timestamp): number {
    const start = assertDefined(this.selectionRange).from.getValueNs();
    const end = assertDefined(this.selectionRange).to.getValueNs();

    return Number(
      (BigInt(this.getAvailableWidth()) * (entry.getValueNs() - start)) /
        (end - start),
    );
  }

  private getSegmentRect(
    start: Timestamp,
    end: Timestamp,
    rowToUse: number,
  ): Rect {
    const xPosStart = this.getXPosOf(start);
    const selectionStart = assertDefined(this.selectionRange).from.getValueNs();
    const selectionEnd = assertDefined(this.selectionRange).to.getValueNs();

    const borderPadding = 5;
    let totalRowHeight =
      (this.canvasDrawer.getScaledCanvasHeight() - 2 * borderPadding) /
      this.maxRowsRequires;
    if (totalRowHeight < 10) {
      totalRowHeight = 10;
    }
    if (this.maxRowsRequires === 1) {
      totalRowHeight = 30;
    }

    const padding = 5;
    const rowHeight = totalRowHeight - padding;

    const width = Math.max(
      Number(
        (BigInt(this.getAvailableWidth()) *
          (end.getValueNs() - start.getValueNs())) /
          (selectionEnd - selectionStart),
      ),
      rowHeight,
    );

    return new Rect(
      xPosStart,
      borderPadding + rowToUse * totalRowHeight,
      width,
      rowHeight,
    );
  }

  private drawSegment(rect: Rect, transition: PropertyTreeNode) {
    const aborted = assertDefined(
      transition.getChildByName('aborted'),
    ).getValue();
    const alpha = aborted ? 0.25 : 1.0;

    const hasUnknownStart =
      TimelineUtils.isTransitionWithUnknownStart(transition);
    const hasUnknownEnd = TimelineUtils.isTransitionWithUnknownEnd(transition);
    this.canvasDrawer.drawRect(
      rect,
      this.color,
      alpha,
      hasUnknownStart,
      hasUnknownEnd,
    );
  }

  private getRowToUseFor(entryIndex: AbsoluteEntryIndex): number {
    const rowToUse = this.rowsToUse.get(entryIndex);
    if (rowToUse === undefined) {
      throw new Error(`Could not find entry ${entryIndex} in rowsToUse`);
    }
    return rowToUse;
  }

  private computeRowsToUse(): void {
    const rowAvailableFrom: Array<bigint | undefined> = [];
    assertDefined(this.trace).forEachEntry((entry) => {
      const index = entry.getIndex();
      const transition = this.transitionEntries?.at(entry.getIndex());
      if (!transition) {
        return;
      }

      const timeRange = TimelineUtils.getTimeRangeForTransition(
        transition,
        assertDefined(this.fullRange),
        assertDefined(this.timestampConverter),
      );

      if (timeRange === undefined) {
        this.shouldNotRenderEntries.push(index);
        return;
      }

      let rowToUse = 0;
      while (
        (rowAvailableFrom[rowToUse] ?? 0n) >
        (timeRange.from.getValueNs() ??
          assertDefined(this.fullRange).from.getValueNs())
      ) {
        rowToUse++;
      }

      rowAvailableFrom[rowToUse] =
        timeRange.to.getValueNs() ??
        assertDefined(this.fullRange).to.getValueNs();

      if (rowToUse + 1 > this.maxRowsRequires) {
        this.maxRowsRequires = rowToUse + 1;
      }
      this.rowsToUse.set(index, rowToUse);
    });
  }
}

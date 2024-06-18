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
import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry_types';
import {Rect} from 'common/rect';
import {TimeRange, Timestamp} from 'common/time';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractTimelineRowComponent} from './abstract_timeline_row_component';

@Component({
  selector: 'transition-timeline',
  template: `
    <div
      class="transition-timeline"
      matTooltip="Some or all transitions will not be rendered in timeline due to unknown dispatch time"
      [matTooltipDisabled]="shouldNotRenderEntries.length === 0"
      #wrapper>
      <canvas #canvas></canvas>
    </div>
  `,
  styles: [
    `
      .transition-timeline {
        height: 4rem;
      }
    `,
  ],
})
export class TransitionTimelineComponent extends AbstractTimelineRowComponent<PropertyTreeNode> {
  @Input() color = '#AF5CF7';
  @Input() trace: Trace<PropertyTreeNode> | undefined;
  @Input() selectedEntry: TraceEntry<PropertyTreeNode> | undefined;
  @Input() selectionRange: TimeRange | undefined;

  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) override canvasRef:
    | ElementRef
    | undefined;
  @ViewChild('wrapper', {static: false}) override wrapperRef:
    | ElementRef
    | undefined;

  hoveringEntry?: TraceEntry<PropertyTreeNode>;
  rowsToUse = new Map<number, number>();
  maxRowsRequires = 0;
  shouldNotRenderEntries: number[] = [];

  ngOnInit() {
    if (!this.trace || !this.selectionRange) {
      throw Error('Not all required inputs have been set');
    }
    this.computeRowsForEntries();
  }

  private async computeRowsForEntries(): Promise<void> {
    const rowAvailableFrom: Array<bigint | undefined> = [];
    await Promise.all(
      (this.trace as Trace<PropertyTreeNode>).mapEntry(async (entry) => {
        const transition = await entry.getValue();
        const index = entry.getIndex();

        const timeRange = TimelineUtils.getTimeRangeForTransition(
          transition,
          entry.getTimestamp().getType(),
          assertDefined(this.selectionRange),
        );

        if (!timeRange) {
          this.shouldNotRenderEntries.push(index);
        }

        let rowToUse = 0;
        while (
          (rowAvailableFrom[rowToUse] ?? 0n) >
          (timeRange?.from.getValueNs() ??
            assertDefined(this.selectionRange).from.getValueNs())
        ) {
          rowToUse++;
        }

        rowAvailableFrom[rowToUse] =
          timeRange?.to.getValueNs() ??
          assertDefined(this.selectionRange).to.getValueNs();

        if (rowToUse + 1 > this.maxRowsRequires) {
          this.maxRowsRequires = rowToUse + 1;
        }
        this.rowsToUse.set(index, rowToUse);
      }),
    );
  }

  private getRowToUseFor(entry: TraceEntry<PropertyTreeNode>): number {
    const rowToUse = this.rowsToUse.get(entry.getIndex());
    if (rowToUse === undefined) {
      console.error('Failed to find', entry, 'in', this.rowsToUse);
      throw new Error('Could not find entry in rowsToUse');
    }
    return rowToUse;
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

  private async drawSegmentHover(mousePoint: Point) {
    const currentHoverEntry = await this.getEntryAt(mousePoint);

    if (this.hoveringEntry) {
      this.canvasDrawer.clear();
      this.drawTimeline();
    }

    this.hoveringEntry = currentHoverEntry;

    if (!this.hoveringEntry) {
      return;
    }

    const transition = await this.hoveringEntry.getValue();
    const timeRange = TimelineUtils.getTimeRangeForTransition(
      transition,
      this.hoveringEntry.getTimestamp().getType(),
      assertDefined(this.selectionRange),
    );

    if (!timeRange) {
      return;
    }

    const rowToUse = this.getRowToUseFor(this.hoveringEntry);
    const rect = this.getSegmentRect(timeRange.from, timeRange.to, rowToUse);
    this.canvasDrawer.drawRectBorder(rect);
  }

  protected override async getEntryAt(
    mousePoint: Point,
  ): Promise<TraceEntry<PropertyTreeNode> | undefined> {
    if (assertDefined(this.trace).type !== TraceType.TRANSITION) {
      return undefined;
    }

    const transitionEntries: Array<
      Promise<TraceEntry<PropertyTreeNode> | undefined>
    > = [];
    assertDefined(this.trace).forEachEntry((entry) => {
      transitionEntries.push(
        (async () => {
          const transition = await entry.getValue();
          const timeRange = TimelineUtils.getTimeRangeForTransition(
            transition,
            entry.getTimestamp().getType(),
            assertDefined(this.selectionRange),
          );

          if (!timeRange) {
            return undefined;
          }
          const rowToUse = this.getRowToUseFor(entry);
          const rect = this.getSegmentRect(
            timeRange.from,
            timeRange.to,
            rowToUse,
          );
          if (rect.containsPoint(mousePoint)) {
            return entry;
          }
          return undefined;
        })(),
      );
    });

    for (const entryPromise of transitionEntries) {
      const entry = await entryPromise;
      if (entry) {
        return entry;
      }
    }

    return undefined;
  }

  get entryWidth() {
    return this.canvasDrawer.getScaledCanvasHeight();
  }

  get availableWidth() {
    return this.canvasDrawer.getScaledCanvasWidth();
  }

  private getXPosOf(entry: Timestamp): number {
    const start = assertDefined(this.selectionRange).from.getValueNs();
    const end = assertDefined(this.selectionRange).to.getValueNs();

    return Number(
      (BigInt(this.availableWidth) * (entry.getValueNs() - start)) /
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

    const width = Number(
      (BigInt(this.availableWidth) * (end.getValueNs() - start.getValueNs())) /
        (selectionEnd - selectionStart),
    );

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

    return new Rect(
      xPosStart,
      borderPadding + rowToUse * totalRowHeight,
      width,
      rowHeight,
    );
  }

  override async drawTimeline() {
    await Promise.all(
      (this.trace as Trace<PropertyTreeNode>).mapEntry(async (entry) => {
        const transition = await entry.getValue();
        const timeRange = TimelineUtils.getTimeRangeForTransition(
          transition,
          entry.getTimestamp().getType(),
          assertDefined(this.selectionRange),
        );
        if (!timeRange) {
          return;
        }
        const rowToUse = this.getRowToUseFor(entry);
        const aborted = assertDefined(
          transition.getChildByName('aborted'),
        ).getValue();
        this.drawSegment(timeRange.from, timeRange.to, rowToUse, aborted);
      }),
    );
    this.drawSelectedTransitionEntry();
  }

  private drawSegment(
    start: Timestamp,
    end: Timestamp,
    rowToUse: number,
    aborted: boolean,
  ) {
    const rect = this.getSegmentRect(start, end, rowToUse);
    const alpha = aborted ? 0.25 : 1.0;
    this.canvasDrawer.drawRect(rect, this.color, alpha);
  }

  private async drawSelectedTransitionEntry() {
    if (this.selectedEntry === undefined) {
      return;
    }
    const transition = await this.selectedEntry.getValue();
    const timeRange = TimelineUtils.getTimeRangeForTransition(
      transition,
      this.selectedEntry.getTimestamp().getType(),
      assertDefined(this.selectionRange),
    );
    if (!timeRange) {
      return;
    }

    const rowIndex = this.getRowToUseFor(this.selectedEntry);
    const rect = this.getSegmentRect(timeRange.from, timeRange.to, rowIndex);
    const alpha = transition.getChildByName('aborted') ? 0.25 : 1.0;
    this.canvasDrawer.drawRect(rect, this.color, alpha);
    this.canvasDrawer.drawRectBorder(rect);
  }
}

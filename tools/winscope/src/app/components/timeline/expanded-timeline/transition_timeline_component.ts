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
import {isPointInRect} from 'common/geometry_utils';
import {ElapsedTimestamp, RealTimestamp, TimeRange, Timestamp, TimestampType} from 'common/time';
import {Transition} from 'flickerlib/common';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {AbstractTimelineRowComponent} from './abstract_timeline_row_component';

@Component({
  selector: 'transition-timeline',
  template: `
    <div class="transition-timeline" #wrapper>
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
export class TransitionTimelineComponent extends AbstractTimelineRowComponent<Transition> {
  @Input() color = '#AF5CF7';
  @Input() trace!: Trace<Transition>;
  @Input() selectedEntry: TraceEntry<Transition> | undefined = undefined;
  @Input() selectionRange!: TimeRange;

  @Output() onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) override canvasRef!: ElementRef;
  @ViewChild('wrapper', {static: false}) override wrapperRef!: ElementRef;

  hoveringEntry?: TraceEntry<Transition>;

  rowsToUse = new Map<number, number>();
  maxRowsRequires = 0;

  ngOnInit() {
    if (!this.trace || !this.selectionRange) {
      throw Error('Not all required inputs have been set');
    }
    this.computeRowsForEntries();
  }

  private async computeRowsForEntries(): Promise<void> {
    const rowAvailableFrom: Array<bigint | undefined> = [];
    await Promise.all(
      (this.trace as Trace<Transition>).mapEntry(async (entry) => {
        const transition = await entry.getValue();
        let rowToUse = 0;
        while (
          (rowAvailableFrom[rowToUse] ?? 0n) > BigInt(transition.createTime.unixNanos.toString())
        ) {
          rowToUse++;
        }

        rowAvailableFrom[rowToUse] = BigInt(transition.finishTime.unixNanos.toString());

        if (rowToUse + 1 > this.maxRowsRequires) {
          this.maxRowsRequires = rowToUse + 1;
        }
        this.rowsToUse.set(entry.getIndex(), rowToUse);
      })
    );
  }

  private getRowToUseFor(entry: TraceEntry<Transition>): number {
    const rowToUse = this.rowsToUse.get(entry.getIndex());
    if (rowToUse === undefined) {
      console.error('Failed to find', entry, 'in', this.rowsToUse);
      throw new Error(`Could not find entry in rowsToUse`);
    }
    return rowToUse;
  }

  override onHover(mouseX: number, mouseY: number) {
    this.drawSegmentHover(mouseX, mouseY);
  }

  override handleMouseOut(e: MouseEvent) {
    if (this.hoveringEntry) {
      // If undefined there is no current hover effect so no need to clear
      this.redraw();
    }
    this.hoveringEntry = undefined;
  }

  private async drawSegmentHover(mouseX: number, mouseY: number) {
    const currentHoverEntry = await this.getEntryAt(mouseX, mouseY);

    if (this.hoveringEntry) {
      this.canvasDrawer.clear();
      this.drawTimeline();
    }

    this.hoveringEntry = currentHoverEntry;

    if (!this.hoveringEntry) {
      return;
    }

    const hoveringSegment = await this.getSegmentForTransition(this.hoveringEntry);
    const rowToUse = this.getRowToUseFor(this.hoveringEntry);
    const {x, y, w, h} = this.getSegmentRect(hoveringSegment.from, hoveringSegment.to, rowToUse);
    this.canvasDrawer.drawRectBorder(x, y, w, h);
  }

  private async getSegmentAt(mouseX: number, mouseY: number): Promise<TimeRange | undefined> {
    const transitionEntry = await this.getEntryAt(mouseX, mouseY);
    if (transitionEntry) {
      return this.getSegmentForTransition(transitionEntry);
    }
    return undefined;
  }

  protected override async getEntryAt(
    mouseX: number,
    mouseY: number
  ): Promise<TraceEntry<Transition> | undefined> {
    if (this.trace.type !== TraceType.TRANSITION) {
      return undefined;
    }

    const transitionEntries: Array<Promise<TraceEntry<Transition> | undefined>> = [];
    this.trace.forEachEntry((entry) => {
      transitionEntries.push(
        (async () => {
          const transitionSegment = await this.getSegmentForTransition(entry);
          const rowToUse = this.getRowToUseFor(entry);
          const {x, y, w, h} = this.getSegmentRect(
            transitionSegment.from,
            transitionSegment.to,
            rowToUse
          );
          if (isPointInRect({x: mouseX, y: mouseY}, {x, y, w, h})) {
            return entry;
          }
          return undefined;
        })()
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
    const start = this.selectionRange.from.getValueNs();
    const end = this.selectionRange.to.getValueNs();

    return Number((BigInt(this.availableWidth) * (entry.getValueNs() - start)) / (end - start));
  }

  private getSegmentRect(start: Timestamp, end: Timestamp, rowToUse: number) {
    const xPosStart = this.getXPosOf(start);
    const selectionStart = this.selectionRange.from.getValueNs();
    const selectionEnd = this.selectionRange.to.getValueNs();

    const width = Number(
      (BigInt(this.availableWidth) * (end.getValueNs() - start.getValueNs())) /
        (selectionEnd - selectionStart)
    );

    const borderPadding = 5;
    let totalRowHeight =
      (this.canvasDrawer.getScaledCanvasHeight() - 2 * borderPadding) / this.maxRowsRequires;
    if (totalRowHeight < 10) {
      totalRowHeight = 10;
    }
    if (this.maxRowsRequires === 1) {
      totalRowHeight = 30;
    }

    const padding = 5;
    const rowHeight = totalRowHeight - padding;

    return {x: xPosStart, y: borderPadding + rowToUse * totalRowHeight, w: width, h: rowHeight};
  }

  override async drawTimeline() {
    await Promise.all(
      (this.trace as Trace<Transition>).mapEntry(async (entry) => {
        const transitionSegment = await this.getSegmentForTransition(entry);
        const rowToUse = this.getRowToUseFor(entry);
        const aborted = (await entry.getValue()).aborted;
        this.drawSegment(transitionSegment.from, transitionSegment.to, rowToUse, aborted);
      })
    );
    this.drawSelectedTransitionEntry();
  }

  private async getSegmentForTransition(entry: TraceEntry<Transition>): Promise<TimeRange> {
    const transition = await entry.getValue();

    let createTime: Timestamp;
    let finishTime: Timestamp;
    if (entry.getTimestamp().getType() === TimestampType.REAL) {
      createTime = new RealTimestamp(BigInt(transition.createTime.unixNanos.toString()));
      finishTime = new RealTimestamp(BigInt(transition.finishTime.unixNanos.toString()));
    } else if (entry.getTimestamp().getType() === TimestampType.ELAPSED) {
      createTime = new ElapsedTimestamp(BigInt(transition.createTime.elapsedNanos.toString()));
      finishTime = new ElapsedTimestamp(BigInt(transition.finishTime.elapsedNanos.toString()));
    } else {
      throw new Error('Unspported timestamp type');
    }

    return {from: createTime, to: finishTime};
  }

  private drawSegment(start: Timestamp, end: Timestamp, rowToUse: number, aborted: boolean) {
    const {x, y, w, h} = this.getSegmentRect(start, end, rowToUse);
    const alpha = aborted ? 0.25 : 1.0;
    this.canvasDrawer.drawRect({x, y, w, h, color: this.color, alpha});
  }

  private async drawSelectedTransitionEntry() {
    if (this.selectedEntry === undefined) {
      return;
    }

    const transitionSegment = await this.getSegmentForTransition(this.selectedEntry);

    const transition = await this.selectedEntry.getValue();
    const rowIndex = this.getRowToUseFor(this.selectedEntry);
    const {x, y, w, h} = this.getSegmentRect(
      transitionSegment.from,
      transitionSegment.to,
      rowIndex
    );
    const alpha = transition.aborted ? 0.25 : 1.0;
    this.canvasDrawer.drawRect({x, y, w, h, color: this.color, alpha});
    this.canvasDrawer.drawRectBorder(x, y, w, h);
  }
}

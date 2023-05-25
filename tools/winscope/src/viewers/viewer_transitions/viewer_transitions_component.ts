/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Component, ElementRef, Inject, Input} from '@angular/core';
import {TimeUtils} from 'common/time_utils';
import {Transition} from 'trace/flickerlib/common';
import {ElapsedTimestamp, TimestampType} from 'trace/timestamp';
import {Terminal} from 'viewers/common/ui_tree_utils';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-transitions',
  template: `
    <div class="card-grid container">
      <div class="top-viewer">
        <div class="entries">
          <div class="table-header table-row">
            <div class="id">Id</div>
            <div class="type">Type</div>
            <div class="send-time">Send Time</div>
            <div class="duration">Duration</div>
            <div class="status">Status</div>
          </div>
          <cdk-virtual-scroll-viewport itemSize="53" class="scroll">
            <div
              *cdkVirtualFor="let transition of uiData.entries; let i = index"
              class="entry table-row"
              [class.current]="isCurrentTransition(transition)"
              (click)="onTransitionClicked(transition)">
              <div class="id">
                <span class="mat-body-1">{{ transition.id }}</span>
              </div>
              <div class="type">
                <span class="mat-body-1">{{ transition.type }}</span>
              </div>
              <div class="send-time">
                <span *ngIf="!transition.sendTime.isMin" class="mat-body-1">{{
                  formattedTime(transition.sendTime, uiData.timestampType)
                }}</span>
                <span *ngIf="transition.sendTime.isMin"> n/a </span>
              </div>
              <div class="duration">
                <span
                  *ngIf="!transition.sendTime.isMin && !transition.finishTime.isMax"
                  class="mat-body-1"
                  >{{
                    formattedTimeDiff(
                      transition.sendTime,
                      transition.finishTime,
                      uiData.timestampType
                    )
                  }}</span
                >
                <span *ngIf="transition.sendTime.isMin || transition.finishTime.isMax">n/a</span>
              </div>
              <div class="status">
                <div *ngIf="transition.mergedInto">
                  <span>MERGED</span>
                  <mat-icon aria-hidden="false" fontIcon="merge" matTooltip="merged" icon-gray>
                  </mat-icon>
                </div>

                <div *ngIf="transition.aborted && !transition.mergedInto">
                  <span>ABORTED</span>
                  <mat-icon
                    aria-hidden="false"
                    fontIcon="close"
                    matTooltip="aborted"
                    style="color: red"
                    icon-red></mat-icon>
                </div>

                <div *ngIf="transition.played && !transition.aborted && !transition.mergedInto">
                  <span>PLAYED</span>
                  <mat-icon
                    aria-hidden="false"
                    fontIcon="check"
                    matTooltip="played"
                    style="color: green"
                    *ngIf="
                      transition.played && !transition.aborted && !transition.mergedInto
                    "></mat-icon>
                </div>
              </div>
            </div>
          </cdk-virtual-scroll-viewport>
        </div>

        <mat-divider [vertical]="true"></mat-divider>

        <div class="container-properties">
          <h3 class="properties-title mat-title">Selected Transition</h3>
          <tree-view
            [item]="uiData.selectedTransitionPropertiesTree"
            [showNode]="showNode"
            [isLeaf]="isLeaf"
            [isAlwaysCollapsed]="true">
          </tree-view>
          <div *ngIf="!uiData.selectedTransitionPropertiesTree">
            No selected transition.<br />
            Select the tranitions below.
          </div>
        </div>
      </div>

      <div class="bottom-viewer">
        <div class="transition-timeline">
          <div *ngFor="let row of timelineRows()" class="row">
            <svg width="100%" [attr.height]="transitionHeight">
              <rect
                *ngFor="let transition of transitionsOnRow(row)"
                [attr.width]="widthOf(transition)"
                [attr.height]="transitionHeight"
                [attr.style]="transitionRectStyle(transition)"
                rx="5"
                [attr.x]="startOf(transition)"
                (click)="onTransitionClicked(transition)" />
              <rect
                *ngFor="let transition of transitionsOnRow(row)"
                [attr.width]="transitionDividerWidth"
                [attr.height]="transitionHeight"
                [attr.style]="transitionDividerRectStyle(transition)"
                [attr.x]="sendOf(transition)" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .container {
        display: flex;
        flex-grow: 1;
        flex-direction: column;
      }

      .top-viewer {
        display: flex;
        flex-grow: 1;
        flex: 3;
        border-bottom: solid 1px rgba(0, 0, 0, 0.12);
      }

      .bottom-viewer {
        display: flex;
        flex-shrink: 1;
      }

      .transition-timeline {
        flex-grow: 1;
        padding: 1.5rem 1rem;
      }

      .entries {
        flex: 3;
        display: flex;
        flex-direction: column;
        padding: 16px;
      }

      .container-properties {
        flex: 1;
        padding: 16px;
      }

      .entries .scroll {
        height: 100%;
      }

      .entries .table-header {
        flex: 1;
      }

      .table-row {
        display: flex;
        flex-direction: row;
        cursor: pointer;
        border-bottom: solid 1px rgba(0, 0, 0, 0.12);
      }

      .table-header.table-row {
        font-weight: bold;
        border-bottom: solid 1px rgba(0, 0, 0, 0.5);
      }

      .scroll .entry.current {
        color: white;
        background-color: #365179;
      }

      .table-row > div {
        padding: 16px;
      }

      .table-row .id {
        flex: 1;
      }

      .table-row .type {
        flex: 2;
      }

      .table-row .send-time {
        flex: 4;
      }

      .table-row .duration {
        flex: 3;
      }

      .table-row .status {
        flex: 2;
      }

      .status > div {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 5px;
      }

      .current .status mat-icon {
        color: white !important;
      }

      .transition-timeline .row svg rect {
        cursor: pointer;
      }

      .label {
        width: 300px;
        padding: 1rem;
      }

      .lines {
        flex-grow: 1;
        padding: 0.5rem;
      }

      .selected-transition {
        padding: 1rem;
        border-bottom: solid 1px rgba(0, 0, 0, 0.12);
        flex-grow: 1;
      }
    `,
  ],
})
export class ViewerTransitionsComponent {
  transitionHeight = '20px';
  transitionDividerWidth = '3px';

  constructor(@Inject(ElementRef) elementRef: ElementRef) {
    this.elementRef = elementRef;
  }

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
  }

  getMinOfRanges(): bigint {
    if (this.uiData.entries.length === 0) {
      return 0n;
    }
    const minOfRange = bigIntMin(
      ...this.uiData.entries
        .filter((it) => !it.createTime.isMin)
        .map((it) => BigInt(it.createTime.elapsedNanos.toString()))
    );
    return minOfRange;
  }

  getMaxOfRanges(): bigint {
    if (this.uiData.entries.length === 0) {
      return 0n;
    }
    const maxOfRange = bigIntMax(
      ...this.uiData.entries
        .filter((it) => !it.finishTime.isMax)
        .map((it) => BigInt(it.finishTime.elapsedNanos.toString()))
    );
    return maxOfRange;
  }

  formattedTime(time: any, timestampType: TimestampType): string {
    return TimeUtils.formattedKotlinTimestamp(time, timestampType);
  }

  formattedTimeDiff(time1: any, time2: any, timestampType: TimestampType): string {
    const timeDiff = new ElapsedTimestamp(
      BigInt(time2.elapsedNanos.toString()) - BigInt(time1.elapsedNanos.toString())
    );
    return TimeUtils.format(timeDiff);
  }

  widthOf(transition: Transition) {
    const fullRange = this.getMaxOfRanges() - this.getMinOfRanges();

    let finish = BigInt(transition.finishTime.elapsedNanos.toString());
    if (transition.finishTime.elapsedNanos.isMax) {
      finish = this.getMaxOfRanges();
    }

    let start = BigInt(transition.createTime.elapsedNanos.toString());
    if (transition.createTime.elapsedNanos.isMin) {
      start = this.getMinOfRanges();
    }

    const minWidthPercent = 0.5;
    return `${Math.max(minWidthPercent, Number((finish - start) * 100n) / Number(fullRange))}%`;
  }

  startOf(transition: Transition) {
    const fullRange = this.getMaxOfRanges() - this.getMinOfRanges();
    return `${
      Number(
        (BigInt(transition.createTime.elapsedNanos.toString()) - this.getMinOfRanges()) * 100n
      ) / Number(fullRange)
    }%`;
  }

  sendOf(transition: Transition) {
    const fullRange = this.getMaxOfRanges() - this.getMinOfRanges();
    return `${
      Number((BigInt(transition.sendTime.elapsedNanos.toString()) - this.getMinOfRanges()) * 100n) /
      Number(fullRange)
    }%`;
  }

  onTransitionClicked(transition: Transition): void {
    this.emitEvent(Events.TransitionSelected, transition);
  }

  transitionRectStyle(transition: Transition): string {
    if (this.uiData.selectedTransition === transition) {
      return 'fill:rgb(0, 0, 230)';
    } else if (transition.aborted) {
      return 'fill:rgb(255, 0, 0)';
    } else {
      return 'fill:rgb(78, 205, 230)';
    }
  }

  transitionDividerRectStyle(transition: Transition): string {
    return 'fill:rgb(255, 0, 0)';
  }

  showNode(item: any) {
    return (
      !(item instanceof Terminal) &&
      !(item.name instanceof Terminal) &&
      !(item.propertyKey instanceof Terminal)
    );
  }

  isLeaf(item: any) {
    return (
      !item.children ||
      item.children.length === 0 ||
      item.children.filter((c: any) => !(c instanceof Terminal)).length === 0
    );
  }

  isCurrentTransition(transition: Transition): boolean {
    return this.uiData.selectedTransition === transition;
  }

  assignRowsToTransitions(): Map<Transition, number> {
    const fullRange = this.getMaxOfRanges() - this.getMinOfRanges();
    const assignedRows = new Map<Transition, number>();

    const sortedTransitions = [...this.uiData.entries].sort((t1, t2) => {
      const diff =
        BigInt(t1.createTime.elapsedNanos.toString()) -
        BigInt(t2.createTime.elapsedNanos.toString());
      if (diff < 0) {
        return -1;
      }
      if (diff > 0) {
        return 1;
      }
      return 0;
    });

    const rowFirstAvailableTime = new Map<number, bigint>();
    let rowsUsed = 1;
    rowFirstAvailableTime.set(0, 0n);

    for (const transition of sortedTransitions) {
      const start = BigInt(transition.createTime.elapsedNanos.toString());
      const end = BigInt(transition.finishTime.elapsedNanos.toString());

      let rowIndexWithSpace = undefined;
      for (let rowIndex = 0; rowIndex < rowsUsed; rowIndex++) {
        if (start > rowFirstAvailableTime.get(rowIndex)!) {
          // current row has space
          rowIndexWithSpace = rowIndex;
          break;
        }
      }

      if (rowIndexWithSpace === undefined) {
        rowIndexWithSpace = rowsUsed;
        rowsUsed++;
      }

      assignedRows.set(transition, rowIndexWithSpace);

      const minimumPaddingBetweenEntries = fullRange / 100n;

      rowFirstAvailableTime.set(rowIndexWithSpace, end + minimumPaddingBetweenEntries);
    }

    return assignedRows;
  }

  timelineRows(): number[] {
    return [...new Set(this.assignRowsToTransitions().values())];
  }

  transitionsOnRow(row: number): Transition[] {
    const transitions = [];
    const assignedRows = this.assignRowsToTransitions();

    for (const transition of assignedRows.keys()) {
      if (row === assignedRows.get(transition)) {
        transitions.push(transition);
      }
    }

    return transitions;
  }

  rowsRequiredForTransitions(): number {
    return Math.max(...this.assignRowsToTransitions().values());
  }

  private emitEvent(event: string, data: any) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: data,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }

  uiData: UiData = UiData.EMPTY;
  private elementRef: ElementRef;
}

const bigIntMax = (...args: Array<bigint>) => args.reduce((m, e) => (e > m ? e : m));
const bigIntMin = (...args: Array<bigint>) => args.reduce((m, e) => (e < m ? e : m));

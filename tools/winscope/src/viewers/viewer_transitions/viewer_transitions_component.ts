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
import {Transition} from 'trace/transition';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {Events} from './events';
import {UiData} from './ui_data';

@Component({
  selector: 'viewer-transitions',
  template: `
    <div class="card-grid container">
      <div class="entries">
        <div class="table-header table-row">
          <div class="id mat-body-2">Id</div>
          <div class="type mat-body-2">Type</div>
          <div class="send-time mat-body-2">Send Time</div>
          <div class="duration mat-body-2">Duration</div>
          <div class="status mat-body-2">Status</div>
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
              <span *ngIf="transition.sendTime" class="mat-body-1">{{ transition.sendTime }}</span>
              <span *ngIf="!transition.sendTime" class="mat-body-1"> n/a </span>
            </div>
            <div class="duration">
              <span *ngIf="transition.duration" class="mat-body-1">{{ transition.duration }}</span>
              <span *ngIf="!transition.duration" class="mat-body-1"> n/a </span>
            </div>
            <div class="status">
              <div *ngIf="transition.merged">
                <span class="mat-body-1">MERGED</span>
                <mat-icon aria-hidden="false" fontIcon="merge" matTooltip="merged" icon-gray>
                </mat-icon>
              </div>

              <div *ngIf="transition.aborted && !transition.merged">
                <span class="mat-body-1">ABORTED</span>
                <mat-icon
                  aria-hidden="false"
                  fontIcon="close"
                  matTooltip="aborted"
                  style="color: red"
                  icon-red></mat-icon>
              </div>

              <div *ngIf="transition.played && !transition.aborted && !transition.merged">
                <span class="mat-body-1">PLAYED</span>
                <mat-icon
                  aria-hidden="false"
                  fontIcon="check"
                  matTooltip="played"
                  style="color: green"
                  *ngIf="transition.played && !transition.aborted && !transition.merged"></mat-icon>
              </div>
            </div>
          </div>
        </cdk-virtual-scroll-viewport>
      </div>

      <mat-divider [vertical]="true"></mat-divider>

      <div class="container-properties">
        <h3 class="properties-title mat-title">Selected Transition</h3>
        <tree-view [node]="uiData.selectedTransition"></tree-view>
        <div *ngIf="!uiData.selectedTransition" class="mat-body-1">No selected transition.</div>
      </div>
    </div>
  `,
  styles: [
    `
      .container {
        display: flex;
        flex-grow: 1;
        flex-direction: row;
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
        overflow-y: scroll;
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
  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  @Input()
  set inputData(data: UiData) {
    this.uiData = data;
  }

  onTransitionClicked(transition: Transition): void {
    this.emitEvent(Events.TransitionSelected, transition.propertiesTree);
  }

  isCurrentTransition(transition: Transition): boolean {
    return (
      transition.id ===
        this.uiData.selectedTransition
          ?.getChildByName('wmData')
          ?.getChildByName('id')
          ?.getValue() ||
      transition.id ===
        this.uiData.selectedTransition
          ?.getChildByName('shellData')
          ?.getChildByName('id')
          ?.getValue()
    );
  }

  emitEvent(event: string, propertiesTree: PropertyTreeNode) {
    const customEvent = new CustomEvent(event, {
      bubbles: true,
      detail: propertiesTree,
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
  }

  uiData: UiData = UiData.EMPTY;
}

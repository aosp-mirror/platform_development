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
import { Component, Input, Inject, ElementRef } from "@angular/core";
import { UserOptions } from "viewers/common/user_options";
import { PersistentStore } from "common/persistent_store";
import { Tree, diffClass, isHighlighted } from "viewers/common/tree_utils";
import { nodeStyles } from "viewers/styles/node.styles";
import { ViewerEvents } from "viewers/common/viewer_events";
import { TraceType } from "common/trace/trace_type";

@Component({
  selector: "hierarchy-view",
  template: `
    <mat-card-header class="view-header">
      <mat-card-title class="title-filter">
        <span class="hierarchy-title">Hierarchy</span>
        <mat-form-field class="filter-field">
          <mat-label>Filter...</mat-label>
          <input
            matInput
            [(ngModel)]="filterString"
            (ngModelChange)="filterTree()"
            name="filter"
          />
        </mat-form-field>
      </mat-card-title>
      <div class="view-controls">
        <mat-checkbox
          *ngFor="let option of objectKeys(userOptions)"
          class="trace-box"
          [(ngModel)]="userOptions[option].enabled"
          (ngModelChange)="updateTree()"
        >{{userOptions[option].name}}</mat-checkbox>
      </div>
      <div class="pinned-items" *ngIf="pinnedItems.length > 0">
        <tree-node
          *ngFor="let pinnedItem of pinnedItems"
          class="node"
          [class]="diffClass(pinnedItem)"
          [class.selected]="isHighlighted(pinnedItem, highlightedItems)"
          [class.clickable]="true"
          [item]="pinnedItem"
          [isPinned]="true"
          [isInPinnedSection]="true"
          (pinNodeChange)="pinnedItemChange($event)"
          (click)="onPinnedNodeClick($event, pinnedItem.id)"
        ></tree-node>
      </div>
    </mat-card-header>
    <mat-card-content class="hierarchy-content" [style]="maxHierarchyHeight()">
      <div class="tree-wrapper">
        <tree-view
          class="tree-view"
          *ngIf="tree"
          [isFlattened]="isFlattened()"
          [isShaded]="true"
          [item]="tree"
          [dependencies]="dependencies"
          [store]="store"
          [useGlobalCollapsedState]="true"
          [itemsClickable]="true"
          [highlightedItems]="highlightedItems"
          [pinnedItems]="pinnedItems"
          (highlightedItemChange)="highlightedItemChange($event)"
          (pinnedItemChange)="pinnedItemChange($event)"
        ></tree-view>
      </div>
    </mat-card-content>
  `,
  styles: [
    `
      .view-header {
        display: block;
        width: 100%;
        min-height: 3.75rem;
        align-items: center;
        border-bottom: 1px solid lightgrey;
      }

      .title-filter {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
      }

      .hierarchy-title {
        font-size: 16px;
      }

      .filter-field {
        font-size: 16px;
        transform: scale(0.7);
        right: 0px;
        position: absolute
      }

      .view-controls {
        display: inline-block;
        font-size: 12px;
        font-weight: normal;
        margin-left: 5px
      }

      .hierarchy-content{
        display: flex;
        flex-direction: column;
        overflow-y: auto;
        overflow-x:hidden
      }

      .tree-view {
        white-space: pre-line;
        flex: 1 0 0;
        height: 100%;
        overflow-y: auto
      }

      .pinned-items {
        border: 2px solid yellow;
        position: relative;
        display: block;
        width: 100%;
      }
    `,
    nodeStyles
  ],
})

export class HierarchyComponent {
  objectKeys = Object.keys;
  filterString = "";
  diffClass = diffClass;
  isHighlighted = isHighlighted;

  @Input() tree!: Tree | null;
  @Input() dependencies: Array<TraceType> = [];
  @Input() highlightedItems: Array<string> = [];
  @Input() pinnedItems: Array<Tree> = [];
  @Input() store!: PersistentStore;
  @Input() userOptions: UserOptions = {};

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {}

  isFlattened() {
    return this.userOptions["flat"]?.enabled;
  }

  maxHierarchyHeight() {
    const headerHeight = this.elementRef.nativeElement.querySelector(".view-header").clientHeight;
    return {
      height: `${800 - headerHeight}px`
    };
  }

  onPinnedNodeClick(event: MouseEvent, pinnedItemId: string) {
    event.preventDefault();
    if (window.getSelection()?.type === "range") {
      return;
    }
    this.highlightedItemChange(`${pinnedItemId}`);
  }

  updateTree() {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyUserOptionsChange,
      {
        bubbles: true,
        detail: { userOptions: this.userOptions }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  filterTree() {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyFilterChange,
      {
        bubbles: true,
        detail: { filterString: this.filterString }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  highlightedItemChange(newId: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: newId }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  pinnedItemChange(item: Tree) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyPinnedChange,
      {
        bubbles: true,
        detail: { pinnedItem: item }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}

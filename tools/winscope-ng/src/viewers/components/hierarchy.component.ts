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
import { UiTreeUtils, HierarchyTreeNode, UiTreeNode } from "viewers/common/ui_tree_utils";
import { nodeStyles } from "viewers/components/styles/node.styles";
import { ViewerEvents } from "viewers/common/viewer_events";
import { TraceType } from "common/trace/trace_type";
import { TableProperties } from "viewers/common/table_properties";

@Component({
  selector: "hierarchy-view",
  template: `
    <div class="view-header">
      <div class="title-filter">
        <h2 class="hierarchy-title mat-title">Hierarchy</h2>
        <mat-form-field>
          <mat-label>Filter...</mat-label>
          <input
            matInput
            [(ngModel)]="filterString"
            (ngModelChange)="filterTree()"
            name="filter"
          />
        </mat-form-field>
      </div>
      <div class="view-controls">
        <mat-checkbox
          color="primary"
          *ngFor="let option of objectKeys(userOptions)"
          class="trace-box"
          [(ngModel)]="userOptions[option].enabled"
          (ngModelChange)="updateTree()"
        >{{userOptions[option].name}}</mat-checkbox>
      </div>
      <properties-table
        *ngIf="tableProperties"
        class="properties-table"
        [properties]="tableProperties"
      ></properties-table>
      <div *ngIf="pinnedItems.length > 0" class="pinned-items">
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
          (click)="onPinnedNodeClick($event, pinnedItem)"
        ></tree-node>
      </div>
    </div>
    <div class="hierarchy-content">
      <div class="tree-wrapper">
        <tree-view
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
          (selectedTreeChange)="selectedTreeChange($event)"
        ></tree-view>
      </div>
    </div>
  `,
  styles: [
    `
      .view-header {
        display: flex;
        flex-direction: column;
        border-bottom: 1px solid var(--default-border);
        padding-bottom: 12px;
      }

      .title-filter {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        justify-content: space-between;
      }

      .view-controls {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
      }

      .properties-table {
        padding-top: 5px;
      }

      .hierarchy-content {
        height: 0;
        flex-grow: 1;
        overflow-y: auto;
      }

      .pinned-items {
        width: 100%;
        box-sizing: border-box;
        border: 2px solid yellow;
      }

      .tree-wrapper {
        overflow-y: auto
      }
    `,
    nodeStyles
  ],
})

export class HierarchyComponent {
  objectKeys = Object.keys;
  filterString = "";
  diffClass = UiTreeUtils.diffClass;
  isHighlighted = UiTreeUtils.isHighlighted;

  @Input() tree!: HierarchyTreeNode | null;
  @Input() tableProperties?: TableProperties | null;
  @Input() dependencies: Array<TraceType> = [];
  @Input() highlightedItems: Array<string> = [];
  @Input() pinnedItems: Array<HierarchyTreeNode> = [];
  @Input() store!: PersistentStore;
  @Input() userOptions: UserOptions = {};

  constructor(
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {}

  public isFlattened() {
    return this.userOptions["flat"]?.enabled;
  }

  public onPinnedNodeClick(event: MouseEvent, pinnedItem: HierarchyTreeNode) {
    event.preventDefault();
    if (window.getSelection()?.type === "range") {
      return;
    }
    if (pinnedItem.id) this.highlightedItemChange(`${pinnedItem.id}`);
    this.selectedTreeChange(pinnedItem);
  }

  public updateTree() {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyUserOptionsChange,
      {
        bubbles: true,
        detail: { userOptions: this.userOptions }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  public filterTree() {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyFilterChange,
      {
        bubbles: true,
        detail: { filterString: this.filterString }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  public highlightedItemChange(newId: string) {
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: newId }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  public selectedTreeChange(item: UiTreeNode) {
    if (!(item instanceof HierarchyTreeNode)) {
      return;
    }
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.SelectedTreeChange,
      {
        bubbles: true,
        detail: { selectedItem: item }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  public pinnedItemChange(item: UiTreeNode) {
    if (!(item instanceof HierarchyTreeNode)) {
      return;
    }
    const event: CustomEvent = new CustomEvent(
      ViewerEvents.HierarchyPinnedChange,
      {
        bubbles: true,
        detail: { pinnedItem: item }
      });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}

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
import {Component, ElementRef, Inject, Input} from '@angular/core';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {TableProperties} from 'viewers/common/table_properties';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {
  HierarchyTreeNodeLegacy,
  UiTreeNode,
  UiTreeUtilsLegacy,
} from 'viewers/common/ui_tree_utils_legacy';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {nodeStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'hierarchy-view-legacy',
  template: `
    <div class="view-header">
      <div class="title-filter">
        <h2 class="hierarchy-title mat-title">Hierarchy</h2>
        <mat-form-field (keydown.enter)="$event.target.blur()">
          <mat-label>Filter...</mat-label>
          <input matInput [(ngModel)]="filterString" (ngModelChange)="filterTree()" name="filter" />
        </mat-form-field>
      </div>
      <div class="view-controls">
        <mat-checkbox
          *ngFor="let option of objectKeys(userOptions)"
          color="primary"
          [(ngModel)]="userOptions[option].enabled"
          [disabled]="userOptions[option].isUnavailable ?? false"
          (ngModelChange)="updateTree()"
          >{{ userOptions[option].name }}</mat-checkbox
        >
      </div>
      <properties-table
        *ngIf="tableProperties"
        class="properties-table"
        [properties]="tableProperties"></properties-table>
      <div *ngIf="pinnedItems.length > 0" class="pinned-items">
        <tree-node-legacy
          *ngFor="let pinnedItem of pinnedLegacyItems()"
          class="node"
          [class]="diffClassLegacy(pinnedItem)"
          [class.selected]="isHighlightedLegacy(pinnedItem, highlightedItem)"
          [class.clickable]="true"
          [item]="pinnedItem"
          [isPinned]="true"
          [isInPinnedSection]="true"
          [isSelected]="isHighlightedLegacy(pinnedItem, highlightedItem)"
          (pinNodeChange)="onPinnedItemChange($event)"
          (click)="onPinnedLegacyNodeClick($event, pinnedItem)"></tree-node-legacy>
        <tree-node
          *ngFor="let pinnedItem of pinnedNewItems()"
          class="node"
          [class]="node.getDiff()"
          [class.selected]="isHighlighted(pinnedItem, highlightedItem)"
          [class.clickable]="true"
          [node]="pinnedItem"
          [isPinned]="true"
          [isInPinnedSection]="true"
          [isSelected]="isHighlighted(pinnedItem, highlightedItem)"
          (pinNodeChange)="onPinnedItemChange($event)"
          (click)="onPinnedNodeClick($event, pinnedItem)"></tree-node>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="hierarchy-content">
      <tree-view-legacy
        *ngIf="tree"
        [isFlattened]="isFlattened()"
        [item]="tree"
        [dependencies]="dependencies"
        [store]="store"
        [useStoredExpandedState]="true"
        [itemsClickable]="true"
        [highlightedItem]="highlightedItem"
        [pinnedItems]="pinnedItems"
        (highlightedChange)="onHighlightedItemChange($event)"
        (pinnedItemChange)="onPinnedItemChange($event)"
        (selectedTreeChange)="onSelectedTreeChange($event)"></tree-view-legacy>

      <div class="children">
        <tree-view
          *ngFor="let subtree of subtrees; trackBy: trackById"
          class="childrenTree"
          [node]="subtree"
          [store]="store"
          [dependencies]="dependencies"
          [isFlattened]="isFlattened()"
          [useStoredExpandedState]="true"
          [initialDepth]="1"
          [highlightedItem]="highlightedItem"
          [pinnedItems]="pinnedItems"
          [itemsClickable]="true"
          (highlightedChange)="onHighlightedItemChange($event)"
          (pinnedItemChange)="onPinnedItemChange($event)"
          (selectedTreeChange)="onSelectedTreeChange($event)"></tree-view>
      </div>
    </div>
  `,
  styles: [
    `
      .view-header {
        display: flex;
        flex-direction: column;
        margin-bottom: 12px;
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
        column-gap: 10px;
      }

      .properties-table {
        padding-top: 5px;
      }

      .hierarchy-content {
        height: 100%;
        overflow: auto;
      }

      .pinned-items {
        width: 100%;
        box-sizing: border-box;
        border: 2px solid #ffd58b;
      }

      tree-view-legacy {
        overflow: auto;
      }
    `,
    nodeStyles,
  ],
})
export class HierarchyComponentLegacy {
  objectKeys = Object.keys;
  filterString = '';
  diffClassLegacy = UiTreeUtilsLegacy.diffClass;
  isHighlightedLegacy = UiTreeUtilsLegacy.isHighlighted;
  isHighlighted = UiTreeUtils.isHighlighted;

  @Input() tree!: HierarchyTreeNodeLegacy | null;
  @Input() subtrees: UiHierarchyTreeNode[] = [];
  @Input() tableProperties?: TableProperties | null;
  @Input() dependencies: TraceType[] = [];
  @Input() highlightedItem: string = '';
  @Input() pinnedItems: Array<HierarchyTreeNodeLegacy | UiHierarchyTreeNode> = [];
  @Input() store!: PersistentStore;
  @Input() userOptions: UserOptions = {};

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  trackById(index: number, child: UiHierarchyTreeNode): string {
    return child.id;
  }

  isFlattened() {
    return this.userOptions['flat']?.enabled;
  }

  pinnedLegacyItems() {
    return this.pinnedItems.filter((item) => item instanceof HierarchyTreeNodeLegacy);
  }

  pinnedNewItems() {
    return this.pinnedItems.filter((item) => item instanceof UiHierarchyTreeNode);
  }

  onPinnedLegacyNodeClick(event: MouseEvent, pinnedItem: HierarchyTreeNodeLegacy) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }
    if (pinnedItem.stableId) this.onHighlightedItemChange(`${pinnedItem.stableId}`);
    this.onSelectedTreeChange(pinnedItem);
  }

  onPinnedNodeClick(event: MouseEvent, pinnedItem: UiHierarchyTreeNode) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }
    this.onHighlightedItemChange(pinnedItem.id);
    this.onSelectedTreeChange(pinnedItem);
  }

  updateTree() {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HierarchyUserOptionsChange, {
      bubbles: true,
      detail: {userOptions: this.userOptions},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  filterTree() {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HierarchyFilterChange, {
      bubbles: true,
      detail: {filterString: this.filterString},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onHighlightedItemChange(newId: string) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HighlightedChange, {
      bubbles: true,
      detail: {id: newId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onSelectedTreeChange(item: UiTreeNode | UiHierarchyTreeNode) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.SelectedTreeChange, {
      bubbles: true,
      detail: {selectedItem: item},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onPinnedItemChange(item: UiTreeNode) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HierarchyPinnedChange, {
      bubbles: true,
      detail: {pinnedItem: item},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}

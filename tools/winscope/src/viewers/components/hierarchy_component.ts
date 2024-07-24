/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {nodeStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'hierarchy-view',
  template: `
    <div class="view-header">
      <div class="title-filter">
        <h2 class="hierarchy-title mat-title">HIERARCHY</h2>
        <mat-form-field (keydown.enter)="$event.target.blur()">
          <mat-label>Filter...</mat-label>
          <input
            matInput
            [(ngModel)]="filterString"
            (ngModelChange)="onFilterChange()"
            name="filter" />
        </mat-form-field>
      </div>
      <div class="view-controls">
        <mat-checkbox
          *ngFor="let option of objectKeys(userOptions)"
          color="primary"
          [(ngModel)]="userOptions[option].enabled"
          [disabled]="userOptions[option].isUnavailable ?? false"
          (ngModelChange)="onUserOptionChange()"
          >{{ userOptions[option].name }}</mat-checkbox
        >
      </div>
      <properties-table
        *ngIf="tableProperties"
        class="properties-table"
        [properties]="tableProperties"></properties-table>
      <div *ngIf="pinnedItems.length > 0" class="pinned-items">
        <tree-node
          *ngFor="let pinnedItem of pinnedItems"
          class="node"
          [class]="pinnedItem.getDiff()"
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
    <div class="hierarchy-content tree-wrapper">
      <tree-view
        *ngIf="tree"
        [isFlattened]="isFlattened()"
        [node]="tree"
        [dependencies]="dependencies"
        [store]="store"
        [useStoredExpandedState]="true"
        [itemsClickable]="true"
        [highlightedItem]="highlightedItem"
        [pinnedItems]="pinnedItems"
        (highlightedChange)="onHighlightedItemChange($event)"
        (pinnedItemChange)="onPinnedItemChange($event)"
        (selectedTreeChange)="onSelectedTreeChange($event)"></tree-view>

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
      .mat-title {
        padding-top: 16px;
      }

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

      tree-view {
        overflow: auto;
      }
    `,
    nodeStyles,
  ],
})
export class HierarchyComponent {
  objectKeys = Object.keys;
  filterString = '';
  isHighlighted = UiTreeUtils.isHighlighted;

  @Input() tree: UiHierarchyTreeNode | undefined;
  @Input() subtrees: UiHierarchyTreeNode[] = [];
  @Input() tableProperties: TableProperties | undefined;
  @Input() dependencies: TraceType[] = [];
  @Input() highlightedItem = '';
  @Input() pinnedItems: UiHierarchyTreeNode[] = [];
  @Input() store: PersistentStore | undefined;
  @Input() userOptions: UserOptions = {};

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  trackById(index: number, child: UiHierarchyTreeNode): string {
    return child.id;
  }

  isFlattened() {
    return this.userOptions['flat']?.enabled;
  }

  onPinnedNodeClick(event: MouseEvent, pinnedItem: UiHierarchyTreeNode) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }
    this.onHighlightedItemChange(pinnedItem.id);
    this.onSelectedTreeChange(pinnedItem);
  }

  onUserOptionChange() {
    const event = new CustomEvent(ViewerEvents.HierarchyUserOptionsChange, {
      bubbles: true,
      detail: {userOptions: this.userOptions},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onFilterChange() {
    const event = new CustomEvent(ViewerEvents.HierarchyFilterChange, {
      bubbles: true,
      detail: {filterString: this.filterString},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onHighlightedItemChange(newId: string) {
    const event = new CustomEvent(ViewerEvents.HighlightedChange, {
      bubbles: true,
      detail: {id: newId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onSelectedTreeChange(item: UiHierarchyTreeNode) {
    const event = new CustomEvent(ViewerEvents.SelectedTreeChange, {
      bubbles: true,
      detail: {selectedItem: item},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onPinnedItemChange(item: UiHierarchyTreeNode) {
    const event = new CustomEvent(ViewerEvents.HierarchyPinnedChange, {
      bubbles: true,
      detail: {pinnedItem: item},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}

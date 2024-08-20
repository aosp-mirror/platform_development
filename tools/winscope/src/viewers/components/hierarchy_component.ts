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
import {
  Component,
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  Output,
} from '@angular/core';
import {Color} from 'app/colors';
import {PersistentStore} from 'common/persistent_store';
import {Analytics} from 'logging/analytics';
import {TraceType} from 'trace/trace_type';
import {RectShowState} from 'viewers/common/rect_show_state';
import {TableProperties} from 'viewers/common/table_properties';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {nodeStyles} from 'viewers/components/styles/node.styles';
import {searchBoxStyle} from './styles/search_box.styles';
import {viewerCardInnerStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'hierarchy-view',
  template: `
    <div class="view-header">
      <div class="title-section">
        <collapsible-section-title
          class="hierarchy-title"
          title="HIERARCHY"
          (collapseButtonClicked)="collapseButtonClicked.emit()"></collapsible-section-title>
        <mat-form-field class="search-box" (keydown.enter)="$event.target.blur()">
          <mat-label>Search</mat-label>
          <input
            matInput
            [(ngModel)]="filterString"
            (ngModelChange)="onFilterChange()"
            name="filter" />
        </mat-form-field>
      </div>
      <user-options
        class="view-controls"
        [userOptions]="userOptions"
        [eventType]="ViewerEvents.HierarchyUserOptionsChange"
        [traceType]="dependencies[0]"
        [logCallback]="Analytics.Navigation.logHierarchySettingsChanged">
      </user-options>
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
    <span class="mat-body-1 placeholder-text" *ngIf="showPlaceholderText()"> {{ placeholderText }} </span>
    <div class="hierarchy-content tree-wrapper">
      <tree-view
        *ngIf="tree"
        [isFlattened]="isFlattened()"
        [node]="tree"
        [useStoredExpandedState]="true"
        [itemsClickable]="true"
        [highlightedItem]="highlightedItem"
        [pinnedItems]="pinnedItems"
        [rectIdToShowState]="rectIdToShowState"
        (highlightedChange)="onHighlightedItemChange($event)"
        (pinnedItemChange)="onPinnedItemChange($event)"
        (selectedTreeChange)="onSelectedTreeChange($event)"></tree-view>

      <div class="subtrees">
        <tree-view
          *ngFor="let subtree of subtrees; trackBy: trackById"
          class="subtree"
          [node]="subtree"
          [isFlattened]="isFlattened()"
          [useStoredExpandedState]="true"
          [highlightedItem]="highlightedItem"
          [pinnedItems]="pinnedItems"
          [itemsClickable]="true"
          [rectIdToShowState]="rectIdToShowState"
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
      }

      .properties-table {
        padding-top: 5px;
      }

      .hierarchy-content {
        height: 100%;
        overflow: auto;
        padding: 0px 12px;
      }

      .pinned-items {
        width: 100%;
        box-sizing: border-box;
        border: 2px solid ${Color.PINNED_ITEM_BORDER};
      }

      tree-view {
        overflow: auto;
      }
    `,
    nodeStyles,
    searchBoxStyle,
    viewerCardInnerStyle,
  ],
})
export class HierarchyComponent {
  filterString = '';
  isHighlighted = UiTreeUtils.isHighlighted;
  ViewerEvents = ViewerEvents;
  Analytics = Analytics;

  @Input() tree: UiHierarchyTreeNode | undefined;
  @Input() subtrees: UiHierarchyTreeNode[] = [];
  @Input() tableProperties: TableProperties | undefined;
  @Input() dependencies: TraceType[] = [];
  @Input() highlightedItem = '';
  @Input() pinnedItems: UiHierarchyTreeNode[] = [];
  @Input() store: PersistentStore | undefined;
  @Input() userOptions: UserOptions = {};
  @Input() rectIdToShowState?: Map<string, RectShowState>;
  @Input() placeholderText = 'No entry found.';

  @Output() collapseButtonClicked = new EventEmitter();

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  trackById(index: number, child: UiHierarchyTreeNode): string {
    return child.id;
  }

  isFlattened(): boolean {
    return this.userOptions['flat']?.enabled;
  }

  showPlaceholderText(): boolean {
    return !this.tree && (this.subtrees?.length ?? 0) === 0;
  }

  onPinnedNodeClick(event: MouseEvent, pinnedItem: UiHierarchyTreeNode) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }
    this.onHighlightedItemChange(pinnedItem);
  }

  onFilterChange() {
    const event = new CustomEvent(ViewerEvents.HierarchyFilterChange, {
      bubbles: true,
      detail: {filterString: this.filterString},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onHighlightedItemChange(node: UiHierarchyTreeNode) {
    const event = new CustomEvent(ViewerEvents.HighlightedNodeChange, {
      bubbles: true,
      detail: {node},
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

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
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertiesTreeNodeLegacy, Terminal} from 'viewers/common/ui_tree_utils_legacy';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {nodeStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'properties-view-legacy',
  template: `
    <div class="view-header" [class.view-header-with-property-groups]="displayPropertyGroups">
      <div class="title-filter">
        <h2 class="properties-title mat-title">Properties</h2>
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
          [matTooltip]="userOptions[option].tooltip ?? ''"
          >{{ userOptions[option].name }}</mat-checkbox
        >
      </div>
    </div>

    <mat-divider></mat-divider>

    <div class="properties-content">
      <h3
        *ngIf="isProtoDump && (isValidTree() || isValidLegacyTree())"
        class="properties-title mat-subheading-2">
        Properties - Proto Dump
      </h3>

      <div class="tree-wrapper">
        <tree-view-legacy
          *ngIf="isValidLegacyTree()"
          [item]="propertiesTree"
          [showNode]="showNode"
          [store]="store"
          [useStoredExpandedState]="true"
          [itemsClickable]="true"
          [highlightedItem]="highlightedProperty"
          (highlightedChange)="onHighlightedPropertyChange($event)"
          [isLeaf]="isLeaf"></tree-view-legacy>

        <tree-view
          *ngIf="isValidTree()"
          [node]="propertiesTree"
          [store]="store"
          [useStoredExpandedState]="true"
          [itemsClickable]="true"
          [highlightedItem]="highlightedProperty"
          (highlightedChange)="onHighlightedPropertyChange($event)"></tree-view>
      </div>
    </div>
  `,
  styles: [
    `
      .view-header {
        display: flex;
        flex-direction: column;
        overflow-y: auto;
        margin-bottom: 12px;
      }

      .view-header-with-property-groups {
        flex: 3;
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
        margin-bottom: 16px;
      }

      .properties-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow-y: auto;
      }

      .property-groups {
        height: 100%;
        overflow-y: auto;
      }

      .tree-wrapper {
        overflow: auto;
      }
    `,
    nodeStyles,
  ],
})
export class PropertiesComponentLegacy {
  objectKeys = Object.keys;
  filterString = '';

  @Input() userOptions: UserOptions = {};
  @Input() propertiesTree: PropertiesTreeNodeLegacy | PropertyTreeNode = {};
  @Input() highlightedProperty: string = '';
  @Input() selectedItem: TraceTreeNode | null = null;
  @Input() displayPropertyGroups = false;
  @Input() isProtoDump = false;
  @Input() traceType: TraceType | undefined;
  @Input() store!: PersistentStore;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  filterTree() {
    const event: CustomEvent = new CustomEvent(ViewerEvents.PropertiesFilterChange, {
      bubbles: true,
      detail: {filterString: this.filterString},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  onHighlightedPropertyChange(newId: string) {
    const event: CustomEvent = new CustomEvent(ViewerEvents.HighlightedPropertyChange, {
      bubbles: true,
      detail: {id: newId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  updateTree() {
    const event: CustomEvent = new CustomEvent(ViewerEvents.PropertiesUserOptionsChange, {
      bubbles: true,
      detail: {userOptions: this.userOptions},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
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

  itemIsSelected() {
    return this.selectedItem && Object.keys(this.selectedItem).length > 0;
  }

  isSurfaceFlinger(): boolean {
    return this.traceType === TraceType.SURFACE_FLINGER;
  }

  isValidLegacyTree(): boolean {
    return (
      this.propertiesTree &&
      !(this.propertiesTree instanceof PropertyTreeNode) &&
      this.objectKeys(this.propertiesTree).length > 0
    );
  }

  isValidTree(): boolean {
    return this.propertiesTree instanceof PropertyTreeNode;
  }
}

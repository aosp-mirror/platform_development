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
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DiffType, HierarchyTreeNode, UiTreeNode, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {nodeInnerItemStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'tree-node',
  template: `
    <button *ngIf="showChevron()" class="icon-button toggle-tree-btn" (click)="toggleTree($event)">
      <mat-icon>
        {{ isCollapsed ? 'arrow_drop_down' : 'chevron_right' }}
      </mat-icon>
    </button>

    <div *ngIf="showLeafNodeIcon()" class="leaf-node-icon-wrapper">
      <mat-icon class="leaf-node-icon"></mat-icon>
    </div>

    <button *ngIf="showPinNodeIcon()" class="icon-button pin-node-btn" (click)="pinNode($event)">
      <mat-icon>
        {{ isPinned ? 'star' : 'star_border' }}
      </mat-icon>
    </button>

    <div class="description">
      <tree-node-data-view *ngIf="!isPropertiesTreeNode()" [item]="item"></tree-node-data-view>
      <tree-node-properties-data-view
        *ngIf="isPropertiesTreeNode()"
        [item]="item"></tree-node-properties-data-view>
    </div>

    <button
      *ngIf="hasChildren && !isCollapsed"
      class="icon-button expand-tree-btn"
      [class]="collapseDiffClass"
      (click)="expandTree($event)">
      <mat-icon aria-hidden="true"> more_horiz </mat-icon>
    </button>
  `,
  styles: [nodeInnerItemStyles],
})
export class TreeNodeComponent {
  @Input() item!: UiTreeNode;
  @Input() isLeaf?: boolean;
  @Input() flattened?: boolean;
  @Input() isCollapsed?: boolean;
  @Input() hasChildren?: boolean = false;
  @Input() isPinned?: boolean = false;
  @Input() isInPinnedSection?: boolean = false;
  @Input() isAlwaysCollapsed?: boolean;

  @Output() toggleTreeChange = new EventEmitter<void>();
  @Output() expandTreeChange = new EventEmitter<boolean>();
  @Output() pinNodeChange = new EventEmitter<UiTreeNode>();

  collapseDiffClass = '';

  ngOnChanges() {
    this.collapseDiffClass = this.updateCollapseDiffClass();
  }

  isPropertiesTreeNode() {
    return !(this.item instanceof HierarchyTreeNode);
  }

  showPinNodeIcon() {
    return (
      (!this.isPropertiesTreeNode() && !UiTreeUtils.isParentNode(this.item.kind ?? '')) ?? false
    );
  }

  toggleTree(event: MouseEvent) {
    if (!this.isAlwaysCollapsed) {
      event.stopPropagation();
      this.toggleTreeChange.emit();
    }
  }

  showChevron() {
    return !this.isLeaf && !this.flattened && !this.isInPinnedSection;
  }

  showLeafNodeIcon() {
    return !this.showChevron() && !this.isInPinnedSection;
  }

  expandTree(event: MouseEvent) {
    event.stopPropagation();
    this.expandTreeChange.emit();
  }

  pinNode(event: MouseEvent) {
    event.stopPropagation();
    this.pinNodeChange.emit(this.item);
  }

  updateCollapseDiffClass() {
    if (this.isCollapsed) {
      return '';
    }

    const childrenDiffClasses = this.getAllDiffTypesOfChildren(this.item);

    childrenDiffClasses.delete(DiffType.NONE);
    childrenDiffClasses.delete(undefined);

    if (childrenDiffClasses.size === 0) {
      return '';
    }
    if (childrenDiffClasses.size === 1) {
      const diffType = childrenDiffClasses.values().next().value;
      return diffType;
    }
    return DiffType.MODIFIED;
  }

  private getAllDiffTypesOfChildren(item: UiTreeNode) {
    if (!item.children) {
      return new Set();
    }

    const classes = new Set();
    for (const child of item.children) {
      if (child.diffType) {
        classes.add(child.diffType);
      }
      for (const diffClass of this.getAllDiffTypesOfChildren(child)) {
        classes.add(diffClass);
      }
    }

    return classes;
  }
}

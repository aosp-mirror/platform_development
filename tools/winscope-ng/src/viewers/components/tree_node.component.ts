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
import { Component, Input, Output, EventEmitter } from "@angular/core";
import { nodeInnerItemStyles } from "viewers/components/styles/node.styles";
import { PropertiesTree, Tree, DiffType, isParentNode } from "viewers/common/tree_utils";

@Component({
  selector: "tree-node",
  template: `
    <button
      class="icon-button toggle-tree-btn"
      (click)="toggleTree($event)"
      *ngIf="showChevron()"
    >
      <mat-icon class="icon-button">
        {{isCollapsed ? "arrow_drop_down" : "chevron_right"}}
      </mat-icon>
    </button>

    <div
      class="leaf-node-icon-wrapper"
      *ngIf="showLeafNodeIcon()"
    >
      <mat-icon class="leaf-node-icon"></mat-icon>
    </div>

    <button
      class="icon-button pin-node-btn"
      (click)="pinNode($event)"
      *ngIf="showPinNodeIcon()"
    >
      <mat-icon class="icon-button">
        {{isPinned ? "star" : "star_border"}}
      </mat-icon>
    </button>

    <div class="description">
      <tree-node-data-view
        [item]="item"
        *ngIf="!isPropertiesTreeNode"
      ></tree-node-data-view>
      <tree-node-properties-data-view
        [item]="item"
        *ngIf="isPropertiesTreeNode"
      ></tree-node-properties-data-view>
    </div>

    <button
      *ngIf="hasChildren && !isCollapsed"
      (click)="expandTree($event)"
      class="icon-button expand-tree-btn"
      [class]="collapseDiffClass"
    >
      <mat-icon
        aria-hidden="true"
        class="icon-button"
      >
        more_horiz
      </mat-icon>
    </button>
  `,
  styles: [nodeInnerItemStyles]
})

export class TreeNodeComponent {
  @Input() item!: Tree | PropertiesTree;
  @Input() isLeaf?: boolean;
  @Input() flattened?: boolean;
  @Input() isCollapsed?: boolean;
  @Input() hasChildren?: boolean = false;
  @Input() isPinned?: boolean = false;
  @Input() isInPinnedSection?: boolean = false;
  @Input() isPropertiesTreeNode?: boolean;
  @Input() isAlwaysCollapsed?: boolean;

  @Output() toggleTreeChange = new EventEmitter<void>();
  @Output() expandTreeChange = new EventEmitter<boolean>();
  @Output() pinNodeChange = new EventEmitter<Tree>();

  collapseDiffClass = "";

  ngOnChanges() {
    this.collapseDiffClass = this.updateCollapseDiffClass();
  }

  public showPinNodeIcon() {
    return (!this.isPropertiesTreeNode && !isParentNode(this.item.kind)) ?? false;
  }

  public toggleTree(event: MouseEvent) {
    if (!this.isAlwaysCollapsed) {
      event.stopPropagation();
      this.toggleTreeChange.emit();
    }
  }

  public showChevron() {
    return !this.isLeaf && !this.flattened && !this.isInPinnedSection;
  }

  public showLeafNodeIcon() {
    return !this.showChevron() && !this.isInPinnedSection;
  }

  public expandTree(event: MouseEvent) {
    event.stopPropagation();
    this.expandTreeChange.emit();
  }

  public pinNode(event: MouseEvent) {
    event.stopPropagation();
    this.pinNodeChange.emit(this.item);
  }

  public updateCollapseDiffClass() {
    if (this.isCollapsed) {
      return "";
    }

    const childrenDiffClasses = this.getAllDiffTypesOfChildren(this.item);

    childrenDiffClasses.delete(DiffType.NONE);
    childrenDiffClasses.delete(undefined);

    if (childrenDiffClasses.size === 0) {
      return "";
    }
    if (childrenDiffClasses.size === 1) {
      const diffType = childrenDiffClasses.values().next().value;
      return diffType;
    }
    return DiffType.MODIFIED;
  }

  private getAllDiffTypesOfChildren(item: Tree | PropertiesTree) {
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

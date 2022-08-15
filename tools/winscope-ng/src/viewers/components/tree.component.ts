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
import { Component, Inject, Input, Output, ElementRef, EventEmitter } from "@angular/core";
import { PersistentStore } from "common/persistent_store";
import { nodeStyles, treeNodeStyles } from "viewers/styles/node.styles";
import { Tree, diffClass, isHighlighted } from "viewers/common/tree_utils";
import { TraceType } from "common/trace/trace_type";

@Component({
  selector: "tree-view",
  template: `
      <div class="tree-view">
        <tree-node
          class="node"
          [class.leaf]="isLeaf()"
          [class.selected]="isHighlighted(item, highlightedItems)"
          [class.clickable]="isClickable()"
          [class.shaded]="isShaded"
          [class.hover]="nodeHover"
          [class.childHover]="childHover"
          [class]="diffClass(item)"
          [style]="nodeOffsetStyle()"
          [item]="item"
          [flattened]="isFlattened"
          [isLeaf]="isLeaf()"
          [isCollapsed]="isCollapsed()"
          [hasChildren]="hasChildren()"
          [isPinned]="isPinned()"
          (toggleTreeChange)="toggleTree()"
          (click)="onNodeClick($event)"
          (expandTreeChange)="expandTree()"
          (pinNodeChange)="sendNewPinnedItemToHierarchy($event)"
        ></tree-node>

        <div class="children" *ngIf="hasChildren()" [hidden]="isCollapsed()" [style]="childrenIndentation()">
          <ng-container *ngFor="let child of children()">
            <tree-view
                class="childrenTree"
                [item]="child"
                [store]="store"
                [dependencies]="dependencies"
                [isFlattened]="isFlattened"
                [isShaded]="!isShaded"
                [useGlobalCollapsedState]="useGlobalCollapsedState"
                [initialDepth]="initialDepth + 1"
                [highlightedItems]="highlightedItems"
                [pinnedItems]="pinnedItems"
                (highlightedItemChange)="sendNewHighlightedItemToHierarchy($event)"
                (pinnedItemChange)="sendNewPinnedItemToHierarchy($event)"
                [itemsClickable]="itemsClickable"
                (hoverStart)="childHover = true"
                (hoverEnd)="childHover = false"
              ></tree-view>
          </ng-container>
        </div>
      </div>
  `,
  styles: [nodeStyles, treeNodeStyles]
})

export class TreeComponent {
  diffClass = diffClass;
  isHighlighted = isHighlighted;

  @Input() item!: Tree;
  @Input() dependencies: Array<TraceType> = [];
  @Input() store!: PersistentStore;
  @Input() isFlattened? = false;
  @Input() isShaded? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItems: Array<string> = [];
  @Input() pinnedItems?: Array<Tree> = [];
  @Input() itemsClickable?: boolean;
  @Input() useGlobalCollapsedState?: boolean;

  @Output() highlightedItemChange = new EventEmitter<string>();
  @Output() pinnedItemChange = new EventEmitter<Tree>();
  @Output() hoverStart = new EventEmitter<void>();
  @Output() hoverEnd = new EventEmitter<void>();

  isCollapsedByDefault = true;
  localCollapsedState = this.isCollapsedByDefault;
  nodeHover = false;
  childHover = false;
  readonly levelOffset = 24;
  nodeElement: HTMLElement;

  constructor(
    @Inject(ElementRef) elementRef: ElementRef,
  ) {
    this.nodeElement = elementRef.nativeElement.querySelector(".node");
    this.nodeElement?.addEventListener("mousedown", this.nodeMouseDownEventListener);
    this.nodeElement?.addEventListener("mouseenter", this.nodeMouseEnterEventListener);
    this.nodeElement?.addEventListener("mouseleave", this.nodeMouseLeaveEventListener);
  }

  ngOnDestroy() {
    this.nodeElement?.removeEventListener("mousedown", this.nodeMouseDownEventListener);
    this.nodeElement?.removeEventListener("mouseenter", this.nodeMouseEnterEventListener);
    this.nodeElement?.removeEventListener("mouseleave", this.nodeMouseLeaveEventListener);
  }

  onNodeClick(event: MouseEvent) {
    event.preventDefault();
    if (window.getSelection()?.type === "range") {
      return;
    }

    if (!this.isLeaf() && event.detail % 2 === 0) {
      // Double click collapsable node
      event.preventDefault();
      this.toggleTree();
    } else {
      this.updateHighlightedItems();
    }
  }

  nodeOffsetStyle() {
    const offset = this.levelOffset * (this.initialDepth) + "px";

    return {
      marginLeft: "-" + offset,
      paddingLeft: offset,
    };
  }

  updateHighlightedItems() {
    if (this.item && this.item.id) {
      this.highlightedItemChange.emit(`${this.item.id}`);
    }
  }

  isPinned() {
    if (this.item) {
      return this.pinnedItems?.map((item: Tree) => `${item.id}`).includes(`${this.item.id}`);
    }
    return false;
  }

  sendNewHighlightedItemToHierarchy(newId: string) {
    this.highlightedItemChange.emit(newId);
  }

  sendNewPinnedItemToHierarchy(newPinnedItem: Tree) {
    this.pinnedItemChange.emit(newPinnedItem);
  }

  isLeaf() {
    return !this.item.children || this.item.children.length === 0;
  }

  isClickable() {
    return !this.isLeaf() || this.itemsClickable;
  }

  toggleTree() {
    this.setCollapseValue(!this.isCollapsed());
  }

  expandTree() {
    this.setCollapseValue(false);
  }

  isCollapsed() {
    if (this.isLeaf()) {
      return false;
    }

    if (this.useGlobalCollapsedState) {
      return this.store.getFromStore(`collapsedState.item.${this.dependencies}.${this.item.id}`)==="true"
        ?? this.isCollapsedByDefault;
    }

    return this.localCollapsedState;
  }

  children() {
    return this.item.children;
  }

  hasChildren() {
    const isParentEntryInFlatView = this.item.kind === "entry" && this.isFlattened;
    return (!this.isFlattened || isParentEntryInFlatView) && !this.isLeaf();
  }

  setCollapseValue(isCollapsed:boolean) {
    if (this.useGlobalCollapsedState) {
      this.store.addToStore(`collapsedState.item.${this.dependencies}.${this.item.id}`, `${isCollapsed}`);
    } else {
      this.localCollapsedState = isCollapsed;
    }
  }

  childrenIndentation() {
    if (this.isFlattened) {
      return {
        marginLeft: "0px",
        paddingLeft: "0px",
        marginTop: "0px",
      };
    } else {
      // Aligns border with collapse arrows
      return {
        marginLeft: "12px",
        paddingLeft: "11px",
        borderLeft: "1px solid rgb(238, 238, 238)",
        marginTop: "0px",
      };
    }
  }

  nodeMouseDownEventListener = (event:MouseEvent) => {
    if (event.detail > 1) {
      event.preventDefault();
      return false;
    }
    return true;
  };

  nodeMouseEnterEventListener = () => {
    this.nodeHover = true;
    this.hoverStart.emit();
  };

  nodeMouseLeaveEventListener = () => {
    this.nodeHover = false;
    this.hoverEnd.emit();
  };
}

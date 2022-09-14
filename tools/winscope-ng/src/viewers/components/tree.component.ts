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
import { nodeStyles, treeNodeDataViewStyles } from "viewers/components/styles/node.styles";
import { Tree, diffClass, isHighlighted, PropertiesTree, Terminal } from "viewers/common/tree_utils";
import { TraceType } from "common/trace/trace_type";
import { TreeNodePropertiesDataViewComponent } from "./tree_node_properties_data_view.component";

@Component({
  selector: "tree-view",
  template: `
      <div class="tree-view">
        <tree-node
          class="node"
          *ngIf="showNode(item)"
          [class.leaf]="isLeaf(this.item)"
          [class.selected]="isHighlighted(item, highlightedItems)"
          [class.clickable]="isClickable()"
          [class.shaded]="isShaded"
          [class.hover]="nodeHover"
          [class.childHover]="childHover"
          [isAlwaysCollapsed]="isAlwaysCollapsed"
          [class]="diffClass(item)"
          [style]="nodeOffsetStyle()"
          [item]="item"
          [flattened]="isFlattened"
          [isLeaf]="isLeaf(this.item)"
          [isCollapsed]="isAlwaysCollapsed ?? isCollapsed()"
          [isPropertiesTreeNode]="isPropertiesTree"
          [hasChildren]="hasChildren()"
          [isPinned]="isPinned()"
          (toggleTreeChange)="toggleTree()"
          (click)="onNodeClick($event)"
          (expandTreeChange)="expandTree()"
          (pinNodeChange)="propagateNewPinnedItem($event)"
        ></tree-node>

        <div class="children" *ngIf="hasChildren()" [hidden]="!isCollapsed()" [style]="childrenIndentation()">
          <ng-container *ngFor="let child of children()">
            <tree-view
                class="childrenTree"
                [item]="child"
                [store]="store"
                [showNode]="showNode"
                [isLeaf]="isLeaf"
                [dependencies]="dependencies"
                [isFlattened]="isFlattened"
                [isPropertiesTree]="isPropertiesTree"
                [isShaded]="!isShaded"
                [useGlobalCollapsedState]="useGlobalCollapsedState"
                [initialDepth]="initialDepth + 1"
                [highlightedItems]="highlightedItems"
                [pinnedItems]="pinnedItems"
                (highlightedItemChange)="propagateNewHighlightedItem($event)"
                (pinnedItemChange)="propagateNewPinnedItem($event)"
                (selectedTreeChange)="propagateNewSelectedTree($event)"
                [itemsClickable]="itemsClickable"
                (hoverStart)="childHover = true"
                (hoverEnd)="childHover = false"
              ></tree-view>
          </ng-container>
        </div>
      </div>
  `,
  styles: [nodeStyles, treeNodeDataViewStyles]
})

export class TreeComponent {
  diffClass = diffClass;
  isHighlighted = isHighlighted;

  @Input() item!: Tree | PropertiesTree | Terminal;
  @Input() dependencies: Array<TraceType> = [];
  @Input() store!: PersistentStore;
  @Input() isFlattened? = false;
  @Input() isShaded? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItems: Array<string> = [];
  @Input() pinnedItems?: Array<Tree> = [];
  @Input() itemsClickable?: boolean;
  @Input() useGlobalCollapsedState?: boolean;
  @Input() isPropertiesTree?: boolean;
  @Input() isAlwaysCollapsed?: boolean;
  @Input() showNode: (item?: any) => boolean = () => true;
  @Input() isLeaf: (item: any) => boolean = (item: any) => !item.children || item.children.length === 0;

  @Output() highlightedItemChange = new EventEmitter<string>();
  @Output() selectedTreeChange = new EventEmitter<Tree>();
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
    @Inject(ElementRef) public elementRef: ElementRef,
  ) {
    this.nodeElement = elementRef.nativeElement.querySelector(".node");
    this.nodeElement?.addEventListener("mousedown", this.nodeMouseDownEventListener);
    this.nodeElement?.addEventListener("mouseenter", this.nodeMouseEnterEventListener);
    this.nodeElement?.addEventListener("mouseleave", this.nodeMouseLeaveEventListener);
  }

  ngOnInit() {
    if (this.isCollapsedByDefault) {
      this.setCollapseValue(this.isCollapsedByDefault);
    }
  }

  ngOnChanges() {
    if (isHighlighted(this.item, this.highlightedItems)) {
      this.selectedTreeChange.emit(this.item);
    }
  }

  ngOnDestroy() {
    this.nodeElement?.removeEventListener("mousedown", this.nodeMouseDownEventListener);
    this.nodeElement?.removeEventListener("mouseenter", this.nodeMouseEnterEventListener);
    this.nodeElement?.removeEventListener("mouseleave", this.nodeMouseLeaveEventListener);
  }

  public onNodeClick(event: MouseEvent) {
    event.preventDefault();
    if (window.getSelection()?.type === "range") {
      return;
    }

    if (!this.isLeaf(this.item) && event.detail % 2 === 0) {
      // Double click collapsable node
      event.preventDefault();
      this.toggleTree();
    } else {
      this.updateHighlightedItems();
    }
  }

  public nodeOffsetStyle() {
    const offset = this.levelOffset * (this.initialDepth) + "px";

    return {
      marginLeft: "-" + offset,
      paddingLeft: offset,
    };
  }

  private updateHighlightedItems() {
    if (this.item && this.item.id) {
      this.highlightedItemChange.emit(`${this.item.id}`);
    } else if (!this.item.id) {
      this.selectedTreeChange.emit(this.item);
    }
  }

  public isPinned() {
    if (this.item) {
      return this.pinnedItems?.map((item: Tree) => `${item.id}`).includes(`${this.item.id}`);
    }
    return false;
  }

  public propagateNewHighlightedItem(newId: string) {
    this.highlightedItemChange.emit(newId);
  }

  public propagateNewPinnedItem(newPinnedItem: Tree) {
    this.pinnedItemChange.emit(newPinnedItem);
  }

  public propagateNewSelectedTree(newTree: Tree) {
    this.selectedTreeChange.emit(newTree);
  }

  public isClickable() {
    return !this.isLeaf(this.item) || this.itemsClickable;
  }

  public toggleTree() {
    this.setCollapseValue(!this.isCollapsed());
  }

  public expandTree() {
    this.setCollapseValue(true);
  }

  public isCollapsed() {
    if (this.isAlwaysCollapsed || this.isLeaf(this.item)) {
      return true;
    }

    if (this.useGlobalCollapsedState) {
      return this.store.getFromStore(`collapsedState.item.${this.dependencies}.${this.item.id}`)==="true"
        ?? this.isCollapsedByDefault;
    }
    return this.localCollapsedState;
  }

  public children() {
    return this.item.children;
  }

  public hasChildren() {
    const isParentEntryInFlatView = this.item.kind === "entry" && this.isFlattened;
    return (!this.isFlattened || isParentEntryInFlatView) && !this.isLeaf(this.item);
  }

  public childrenIndentation() {
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

  private setCollapseValue(isCollapsed: boolean) {
    if (this.useGlobalCollapsedState) {
      this.store.addToStore(`collapsedState.item.${this.dependencies}.${this.item.id}`, `${isCollapsed}`);
    } else {
      this.localCollapsedState = isCollapsed;
    }
  }

  private nodeMouseDownEventListener = (event:MouseEvent) => {
    if (event.detail > 1) {
      event.preventDefault();
      return false;
    }
    return true;
  };

  private nodeMouseEnterEventListener = () => {
    this.nodeHover = true;
    this.hoverStart.emit();
  };

  private nodeMouseLeaveEventListener = () => {
    this.nodeHover = false;
    this.hoverEnd.emit();
  };
}

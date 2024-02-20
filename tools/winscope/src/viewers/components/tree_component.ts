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
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Inject,
  Input,
  Output,
  SimpleChanges,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {TraceType} from 'trace/trace_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {
  nodeStyles,
  treeNodeDataViewStyles,
} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'tree-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tree-node
      *ngIf="node && showNode(node)"
      [id]="'node' + node.name"
      class="node"
      [id]="'node' + node.name"
      [class.leaf]="isLeaf(node)"
      [class.selected]="isHighlighted(node, highlightedItem)"
      [class.clickable]="isClickable()"
      [class.child-selected]="hasSelectedChild()"
      [class.hover]="nodeHover"
      [class.childHover]="childHover"
      [class]="node.getDiff()"
      [style]="nodeOffsetStyle()"
      [node]="node"
      [flattened]="isFlattened"
      [isLeaf]="isLeaf(node)"
      [isExpanded]="isExpanded()"
      [isPinned]="isPinned()"
      [isSelected]="isHighlighted(node, highlightedItem)"
      (toggleTreeChange)="toggleTree()"
      (click)="onNodeClick($event)"
      (expandTreeChange)="expandTree()"
      (pinNodeChange)="propagateNewPinnedItem($event)"></tree-node>

    <div
      *ngIf="!isLeaf(node)"
      class="children"
      [class.flattened]="isFlattened"
      [hidden]="!isExpanded()">
      <tree-view
        *ngFor="let child of node.children.values(); trackBy: childTrackById"
        class="childrenTree"
        [node]="child"
        [store]="store"
        [showNode]="showNode"
        [dependencies]="dependencies"
        [isFlattened]="isFlattened"
        [useStoredExpandedState]="useStoredExpandedState"
        [initialDepth]="initialDepth + 1"
        [highlightedItem]="highlightedItem"
        [pinnedItems]="pinnedItems"
        [itemsClickable]="itemsClickable"
        (highlightedChange)="propagateNewHighlightedItem($event)"
        (pinnedItemChange)="propagateNewPinnedItem($event)"
        (selectedTreeChange)="propagateNewSelectedTree($event)"
        (hoverStart)="childHover = true"
        (hoverEnd)="childHover = false"></tree-view>
    </div>
  `,
  styles: [nodeStyles, treeNodeDataViewStyles],
})
export class TreeComponent {
  isHighlighted = UiTreeUtils.isHighlighted;

  // TODO (b/263779536): this array is passed down from viewers/presenters and is used to generate
  //  an identifier supposed to be unique for each viewer. Let's just use a proper identifier
  //  instead. Each viewer/presenter could pass down a random magic number, an UUID, ...
  @Input() dependencies: TraceType[] = [];

  @Input() node?: UiPropertyTreeNode | UiHierarchyTreeNode;
  @Input() store?: PersistentStore;
  @Input() isFlattened? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItem = '';
  @Input() pinnedItems?: UiHierarchyTreeNode[] = [];
  @Input() itemsClickable?: boolean;

  // Conditionally use stored states. Some traces (e.g. transactions) do not provide items with the "stable id" field needed to search values in the storage.
  @Input() useStoredExpandedState = false;

  @Input() showNode = (node: UiPropertyTreeNode | UiHierarchyTreeNode) => true;

  @Output() readonly highlightedChange = new EventEmitter<string>();
  @Output() readonly selectedTreeChange =
    new EventEmitter<UiHierarchyTreeNode>();
  @Output() readonly pinnedItemChange = new EventEmitter<UiHierarchyTreeNode>();
  @Output() readonly hoverStart = new EventEmitter<void>();
  @Output() readonly hoverEnd = new EventEmitter<void>();

  localExpandedState = true;
  nodeHover = false;
  childHover = false;
  readonly levelOffset = 24;
  nodeElement: HTMLElement;

  private storeKeyExpandedState = '';

  childTrackById(
    index: number,
    child: UiPropertyTreeNode | UiHierarchyTreeNode,
  ): string {
    return child.id;
  }

  constructor(@Inject(ElementRef) public elementRef: ElementRef) {
    this.nodeElement = elementRef.nativeElement.querySelector('.node');
    this.nodeElement?.addEventListener(
      'mousedown',
      this.nodeMouseDownEventListener,
    );
    this.nodeElement?.addEventListener(
      'mouseenter',
      this.nodeMouseEnterEventListener,
    );
    this.nodeElement?.addEventListener(
      'mouseleave',
      this.nodeMouseLeaveEventListener,
    );
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['node'] && this.node) {
      this.storeKeyExpandedState = `treeView.expandedState.node.${this.dependencies}.${this.node.id}`;
      if (this.store) {
        this.setExpandedValue(
          true,
          assertDefined(this.store).get(this.storeKeyExpandedState) ===
            undefined,
        );
      } else {
        this.setExpandedValue(true);
      }
    }
    if (
      this.node instanceof UiHierarchyTreeNode &&
      UiTreeUtils.isHighlighted(this.node, this.highlightedItem)
    ) {
      this.selectedTreeChange.emit(this.node);
    }
  }

  ngOnDestroy() {
    this.nodeElement?.removeEventListener(
      'mousedown',
      this.nodeMouseDownEventListener,
    );
    this.nodeElement?.removeEventListener(
      'mouseenter',
      this.nodeMouseEnterEventListener,
    );
    this.nodeElement?.removeEventListener(
      'mouseleave',
      this.nodeMouseLeaveEventListener,
    );
  }

  isLeaf(node?: UiPropertyTreeNode | UiHierarchyTreeNode): boolean {
    if (node === undefined) return true;
    if (node instanceof UiHierarchyTreeNode) {
      return node.getAllChildren().length === 0;
    }
    return (
      node.formattedValue().length > 0 || node.getAllChildren().length === 0
    );
  }

  onNodeClick(event: MouseEvent) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }

    const isDoubleClick = event.detail % 2 === 0;
    if (!this.isLeaf(this.node) && isDoubleClick) {
      event.preventDefault();
      this.toggleTree();
    } else {
      this.updateHighlightedItem();
    }
  }

  nodeOffsetStyle() {
    const offset = this.levelOffset * this.initialDepth + 'px';

    return {
      marginLeft: '-' + offset,
      paddingLeft: offset,
    };
  }

  isPinned() {
    if (this.node instanceof UiHierarchyTreeNode) {
      return this.pinnedItems?.map((item) => item.id).includes(this.node!.id);
    }
    return false;
  }

  propagateNewHighlightedItem(newId: string) {
    this.highlightedChange.emit(newId);
  }

  propagateNewPinnedItem(newPinnedItem: UiHierarchyTreeNode) {
    this.pinnedItemChange.emit(newPinnedItem);
  }

  propagateNewSelectedTree(newTree: UiHierarchyTreeNode) {
    this.selectedTreeChange.emit(newTree);
  }

  isClickable() {
    return !this.isLeaf(this.node) || this.itemsClickable;
  }

  toggleTree() {
    this.setExpandedValue(!this.isExpanded());
  }

  expandTree() {
    this.setExpandedValue(true);
  }

  isExpanded() {
    if (this.isLeaf(this.node)) {
      return true;
    }

    if (this.useStoredExpandedState) {
      return (
        assertDefined(this.store).get(this.storeKeyExpandedState) === 'true' ??
        false
      );
    }

    return this.localExpandedState;
  }

  hasSelectedChild() {
    if (this.isLeaf(this.node)) {
      return false;
    }
    for (const child of this.node!.getAllChildren()) {
      if (this.highlightedItem === child.id) {
        return true;
      }
    }
    return false;
  }

  private updateHighlightedItem() {
    if (this.node) this.highlightedChange.emit(this.node.id);
  }

  private setExpandedValue(
    isExpanded: boolean,
    shouldUpdateStoredState = true,
  ) {
    if (this.useStoredExpandedState && shouldUpdateStoredState) {
      assertDefined(this.store).add(
        this.storeKeyExpandedState,
        `${isExpanded}`,
      );
    } else {
      this.localExpandedState = isExpanded;
    }
  }

  private nodeMouseDownEventListener = (event: MouseEvent) => {
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

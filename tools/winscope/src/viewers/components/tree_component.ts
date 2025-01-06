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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {
  nodeInnerItemStyles,
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
      [class.child-hover]="childHover"
      [class.full-opacity]="showFullOpacity(node)"
      [class]="node.getDiff()"
      [style]="nodeOffsetStyle()"
      [node]="node"
      [flattened]="isFlattened"
      [isLeaf]="isLeaf(node)"
      [isExpanded]="isExpanded()"
      [isPinned]="isPinned()"
      [isSelected]="isHighlighted(node, highlightedItem)"
      [showStateIcon]="getShowStateIcon(node)"
      (toggleTreeChange)="toggleTree()"
      (rectShowStateChange)="toggleRectShowState()"
      (click)="onNodeClick($event)"
      (expandTreeChange)="expandTree()"
      (pinNodeChange)="propagateNewPinnedItem($event)"></tree-node>

    <div
      *ngIf="!isLeaf(node)"
      class="children"
      [class.flattened]="isFlattened"
      [class.with-gutter]="addGutter()"
      [hidden]="!isExpanded()">
      <tree-view
        *ngFor="let child of node.children.values(); trackBy: childTrackById"
        class="subtree"
        [node]="child"
        [store]="store"
        [showNode]="showNode"
        [isFlattened]="isFlattened"
        [useStoredExpandedState]="useStoredExpandedState"
        [initialDepth]="initialDepth + 1"
        [highlightedItem]="highlightedItem"
        [pinnedItems]="pinnedItems"
        [itemsClickable]="itemsClickable"
        [rectIdToShowState]="rectIdToShowState"
        (highlightedChange)="propagateNewHighlightedItem($event)"
        (pinnedItemChange)="propagateNewPinnedItem($event)"
        (hoverStart)="childHover = true"
        (hoverEnd)="childHover = false"></tree-view>
    </div>
  `,
  styles: [nodeStyles, treeNodeDataViewStyles, nodeInnerItemStyles],
})
export class TreeComponent {
  isHighlighted = UiTreeUtils.isHighlighted;

  @Input() node?: UiPropertyTreeNode | UiHierarchyTreeNode;
  @Input() store: InMemoryStorage | undefined;
  @Input() isFlattened? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItem = '';
  @Input() pinnedItems?: UiHierarchyTreeNode[] = [];
  @Input() itemsClickable?: boolean;
  @Input() rectIdToShowState?: Map<string, RectShowState>;

  // Conditionally use stored states. Some traces (e.g. transactions) do not provide items with the "stable id" field needed to search values in the storage.
  @Input() useStoredExpandedState = false;

  @Input() showNode = (node: UiPropertyTreeNode | UiHierarchyTreeNode) => true;

  @Output() readonly highlightedChange = new EventEmitter<
    UiHierarchyTreeNode | UiPropertyTreeNode
  >();
  @Output() readonly pinnedItemChange = new EventEmitter<UiHierarchyTreeNode>();
  @Output() readonly hoverStart = new EventEmitter<void>();
  @Output() readonly hoverEnd = new EventEmitter<void>();

  localExpandedState = true;
  childHover = false;
  readonly levelOffset = 24;
  nodeElement: HTMLElement;

  private storeKeyCollapsedState = '';

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
      if (this.node.isRoot() && !this.store) {
        this.store = new InMemoryStorage();
      }
      this.storeKeyCollapsedState = `${this.node.id}.collapsedState`;
      if (this.store) {
        this.setExpandedValue(!this.isCollapsedInStore());
      } else {
        this.setExpandedValue(true);
      }
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
    if (!this.isFlattened && !this.isLeaf(this.node) && isDoubleClick) {
      event.preventDefault();
      this.toggleTree();
    } else {
      this.updateHighlightedItem();
    }
  }

  nodeOffsetStyle() {
    const offset = this.levelOffset * this.initialDepth;
    const gutterOffset = this.addGutter() ? this.levelOffset / 2 : 0;
    return {
      marginLeft: '-' + offset + 'px',
      paddingLeft: offset + gutterOffset + 'px',
    };
  }

  isPinned() {
    if (this.node instanceof UiHierarchyTreeNode) {
      return this.pinnedItems?.map((item) => item.id).includes(this.node!.id);
    }
    return false;
  }

  propagateNewHighlightedItem(
    newItem: UiPropertyTreeNode | UiHierarchyTreeNode,
  ) {
    this.highlightedChange.emit(newItem);
  }

  propagateNewPinnedItem(newPinnedItem: UiHierarchyTreeNode) {
    this.pinnedItemChange.emit(newPinnedItem);
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

    if (this.useStoredExpandedState && this.store) {
      return !this.isCollapsedInStore();
    }

    return this.localExpandedState;
  }

  hasSelectedChild() {
    if (this.isLeaf(this.node)) {
      return false;
    }
    for (const child of assertDefined(this.node).getAllChildren()) {
      if (this.highlightedItem === child.id) {
        return true;
      }
    }
    return false;
  }

  getShowStateIcon(
    node: UiPropertyTreeNode | UiHierarchyTreeNode,
  ): string | undefined {
    const showState = this.rectIdToShowState?.get(node.id);
    if (showState === undefined || node instanceof UiPropertyTreeNode) {
      return undefined;
    }
    return showState === RectShowState.SHOW ? 'visibility' : 'visibility_off';
  }

  showFullOpacity(node: UiPropertyTreeNode | UiHierarchyTreeNode) {
    if (node instanceof UiPropertyTreeNode) return true;
    if (this.rectIdToShowState === undefined) return true;
    const showState = this.rectIdToShowState.get(node.id);
    return showState === RectShowState.SHOW;
  }

  toggleRectShowState() {
    const nodeId = assertDefined(this.node).id;
    const currentShowState = assertDefined(this.rectIdToShowState?.get(nodeId));
    const newShowState =
      currentShowState === RectShowState.HIDE
        ? RectShowState.SHOW
        : RectShowState.HIDE;
    const event = new CustomEvent(ViewerEvents.RectShowStateChange, {
      bubbles: true,
      detail: {rectId: nodeId, state: newShowState},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }

  addGutter() {
    return (this.rectIdToShowState?.size ?? 0) > 0;
  }

  private updateHighlightedItem() {
    if (this.node) this.highlightedChange.emit(this.node);
  }

  private setExpandedValue(
    isExpanded: boolean,
    shouldUpdateStoredState = true,
  ) {
    if (this.store && this.useStoredExpandedState && shouldUpdateStoredState) {
      if (isExpanded) {
        this.store.clear(this.storeKeyCollapsedState);
      } else {
        this.store.add(this.storeKeyCollapsedState, 'true');
      }
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
    this.hoverStart.emit();
  };

  private nodeMouseLeaveEventListener = () => {
    this.hoverEnd.emit();
  };

  private isCollapsedInStore(): boolean {
    return (
      assertDefined(this.store).get(this.storeKeyCollapsedState) !== undefined
    );
  }
}

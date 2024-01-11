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
import {HierarchyTreeNode, UiTreeNode, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {nodeStyles, treeNodeDataViewStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'tree-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tree-node
      *ngIf="item && showNode(item)"
      class="node"
      [id]="'node' + item.stableId"
      [class.leaf]="isLeaf(this.item)"
      [class.selected]="isHighlighted(item, highlightedItem)"
      [class.clickable]="isClickable()"
      [class.child-selected]="hasSelectedChild()"
      [class.hover]="nodeHover"
      [class.childHover]="childHover"
      [class]="diffClass(item)"
      [style]="nodeOffsetStyle()"
      [item]="item"
      [flattened]="isFlattened"
      [isLeaf]="isLeaf(this.item)"
      [isExpanded]="isExpanded()"
      [hasChildren]="hasChildren()"
      [isPinned]="isPinned()"
      [isSelected]="isHighlighted(item, highlightedItem)"
      (toggleTreeChange)="toggleTree()"
      (click)="onNodeClick($event)"
      (expandTreeChange)="expandTree()"
      (pinNodeChange)="propagateNewPinnedItem($event)"></tree-node>

    <div
      *ngIf="hasChildren()"
      class="children"
      [class.flattened]="isFlattened"
      [hidden]="!isExpanded()">
      <tree-view
        *ngFor="let child of children(); trackBy: childTrackById"
        class="childrenTree"
        [item]="child"
        [store]="store"
        [showNode]="showNode"
        [isLeaf]="isLeaf"
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
  diffClass = UiTreeUtils.diffClass;
  isHighlighted = UiTreeUtils.isHighlighted;

  // TODO (b/263779536): this array is passed down from viewers/presenters and is used to generate
  //  an identifier supposed to be unique for each viewer. Let's just use a proper identifier
  //  instead. Each viewer/presenter could pass down a random magic number, an UUID, ...
  @Input() dependencies: TraceType[] = [];

  @Input() item?: UiTreeNode;
  @Input() store?: PersistentStore;
  @Input() isFlattened? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItem: string = '';
  @Input() pinnedItems: HierarchyTreeNode[] = [];
  @Input() itemsClickable?: boolean;

  // Conditionally use stored states. Some traces (e.g. transactions) do not provide items with the "stable id" field needed to search values in the storage.
  @Input() useStoredExpandedState = false;

  @Input() showNode = (item: UiTreeNode) => true;
  @Input() isLeaf = (item?: UiTreeNode) => {
    return !item || !item.children || item.children.length === 0;
  };

  @Output() highlightedChange = new EventEmitter<string>();
  @Output() selectedTreeChange = new EventEmitter<UiTreeNode>();
  @Output() pinnedItemChange = new EventEmitter<UiTreeNode>();
  @Output() hoverStart = new EventEmitter<void>();
  @Output() hoverEnd = new EventEmitter<void>();

  localExpandedState = true;
  nodeHover = false;
  childHover = false;
  readonly levelOffset = 24;
  nodeElement: HTMLElement;

  private storeKeyExpandedState = '';

  childTrackById(index: number, child: UiTreeNode): string {
    if (child.stableId !== undefined) {
      return child.stableId;
    }
    if (!(child instanceof HierarchyTreeNode) && typeof child.propertyKey === 'string') {
      return child.propertyKey;
    }

    throw Error('Missing stable id or property key on node');
  }

  constructor(@Inject(ElementRef) public elementRef: ElementRef) {
    this.nodeElement = elementRef.nativeElement.querySelector('.node');
    this.nodeElement?.addEventListener('mousedown', this.nodeMouseDownEventListener);
    this.nodeElement?.addEventListener('mouseenter', this.nodeMouseEnterEventListener);
    this.nodeElement?.addEventListener('mouseleave', this.nodeMouseLeaveEventListener);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['item']) {
      this.storeKeyExpandedState = `treeView.expandedState.item.${this.dependencies}.${this.item?.stableId}`;
      if (this.store) {
        this.setExpandedValue(
          true,
          assertDefined(this.store).get(this.storeKeyExpandedState) === undefined
        );
      } else {
        this.setExpandedValue(true);
      }
    }
    if (
      this.item instanceof HierarchyTreeNode &&
      UiTreeUtils.isHighlighted(this.item, this.highlightedItem)
    ) {
      this.selectedTreeChange.emit(this.item);
    }
  }

  ngOnDestroy() {
    this.nodeElement?.removeEventListener('mousedown', this.nodeMouseDownEventListener);
    this.nodeElement?.removeEventListener('mouseenter', this.nodeMouseEnterEventListener);
    this.nodeElement?.removeEventListener('mouseleave', this.nodeMouseLeaveEventListener);
  }

  onNodeClick(event: MouseEvent) {
    event.preventDefault();
    if (window.getSelection()?.type === 'range') {
      return;
    }

    const isDoubleClick = event.detail % 2 === 0;
    if (!this.isLeaf(this.item) && isDoubleClick) {
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
    if (this.item instanceof HierarchyTreeNode) {
      return this.pinnedItems?.map((item) => `${item.stableId}`).includes(`${this.item.stableId}`);
    }
    return false;
  }

  propagateNewHighlightedItem(newId: string) {
    this.highlightedChange.emit(newId);
  }

  propagateNewPinnedItem(newPinnedItem: UiTreeNode) {
    this.pinnedItemChange.emit(newPinnedItem);
  }

  propagateNewSelectedTree(newTree: UiTreeNode) {
    this.selectedTreeChange.emit(newTree);
  }

  isClickable() {
    return !this.isLeaf(this.item) || this.itemsClickable;
  }

  toggleTree() {
    this.setExpandedValue(!this.isExpanded());
  }

  expandTree() {
    this.setExpandedValue(true);
  }

  isExpanded() {
    if (this.isLeaf(this.item)) {
      return true;
    }

    if (this.useStoredExpandedState) {
      return assertDefined(this.store).get(this.storeKeyExpandedState) === 'true' ?? false;
    }

    return this.localExpandedState;
  }

  children(): UiTreeNode[] {
    return this.item?.children ?? [];
  }

  hasChildren() {
    if (!this.item) {
      return false;
    }
    const isParentEntryInFlatView =
      UiTreeUtils.isParentNode(this.item.kind ?? '') && this.isFlattened;
    return (!this.isFlattened || isParentEntryInFlatView) && !this.isLeaf(this.item);
  }

  hasSelectedChild() {
    if (!this.hasChildren()) {
      return false;
    }
    for (const child of assertDefined(this.item?.children)) {
      if (child.stableId && this.highlightedItem === child.stableId) {
        return true;
      }
    }
    return false;
  }

  private updateHighlightedItem() {
    if (this.item?.stableId) {
      this.highlightedChange.emit(`${this.item.stableId}`);
    }
  }

  private setExpandedValue(isExpanded: boolean, shouldUpdateStoredState = true) {
    if (this.useStoredExpandedState && shouldUpdateStoredState) {
      assertDefined(this.store).add(this.storeKeyExpandedState, `${isExpanded}`);
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

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
} from '@angular/core';
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
      [class.leaf]="isLeaf(this.item)"
      [class.selected]="isHighlighted(item, highlightedItems)"
      [class.clickable]="isClickable()"
      [class.hover]="nodeHover"
      [class.childHover]="childHover"
      [isAlwaysCollapsed]="isAlwaysCollapsed"
      [class]="diffClass(item)"
      [style]="nodeOffsetStyle()"
      [item]="item"
      [flattened]="isFlattened"
      [isLeaf]="isLeaf(this.item)"
      [isCollapsed]="isAlwaysCollapsed ?? isCollapsed()"
      [hasChildren]="hasChildren()"
      [isPinned]="isPinned()"
      (toggleTreeChange)="toggleTree()"
      (click)="onNodeClick($event)"
      (expandTreeChange)="expandTree()"
      (pinNodeChange)="propagateNewPinnedItem($event)"></tree-node>

    <div
      *ngIf="hasChildren()"
      class="children"
      [class.flattened]="isFlattened"
      [hidden]="!isCollapsed()">
      <tree-view
        *ngFor="let child of children(); trackBy: childTrackById"
        class="childrenTree"
        [item]="child"
        [store]="store"
        [showNode]="showNode"
        [isLeaf]="isLeaf"
        [dependencies]="dependencies"
        [isFlattened]="isFlattened"
        [useGlobalCollapsedState]="useGlobalCollapsedState"
        [initialDepth]="initialDepth + 1"
        [highlightedItems]="highlightedItems"
        [pinnedItems]="pinnedItems"
        (highlightedItemChange)="propagateNewHighlightedItem($event)"
        (pinnedItemChange)="propagateNewPinnedItem($event)"
        (selectedTreeChange)="propagateNewSelectedTree($event)"
        [itemsClickable]="itemsClickable"
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
  @Input() store!: PersistentStore;
  @Input() isFlattened? = false;
  @Input() initialDepth = 0;
  @Input() highlightedItems: string[] = [];
  @Input() pinnedItems?: HierarchyTreeNode[] = [];
  @Input() itemsClickable?: boolean;
  @Input() useGlobalCollapsedState?: boolean;
  @Input() isAlwaysCollapsed?: boolean;
  @Input() showNode = (item: UiTreeNode) => true;
  @Input() isLeaf = (item?: UiTreeNode) => {
    return !item || !item.children || item.children.length === 0;
  };

  @Output() highlightedItemChange = new EventEmitter<string>();
  @Output() selectedTreeChange = new EventEmitter<UiTreeNode>();
  @Output() pinnedItemChange = new EventEmitter<UiTreeNode>();
  @Output() hoverStart = new EventEmitter<void>();
  @Output() hoverEnd = new EventEmitter<void>();

  isCollapsedByDefault = true;
  localCollapsedState = this.isCollapsedByDefault;
  nodeHover = false;
  childHover = false;
  readonly levelOffset = 24;
  nodeElement: HTMLElement;

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

  ngOnInit() {
    if (this.isCollapsedByDefault) {
      this.setCollapseValue(this.isCollapsedByDefault);
    }
  }

  ngOnChanges() {
    if (
      this.item instanceof HierarchyTreeNode &&
      UiTreeUtils.isHighlighted(this.item, this.highlightedItems)
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
      this.updateHighlightedItems();
    }
  }

  nodeOffsetStyle() {
    const offset = this.levelOffset * this.initialDepth + 'px';

    return {
      marginLeft: '-' + offset,
      paddingLeft: offset,
    };
  }

  private updateHighlightedItems() {
    if (this.item?.stableId) {
      this.highlightedItemChange.emit(`${this.item.stableId}`);
    }
  }

  isPinned() {
    if (this.item instanceof HierarchyTreeNode) {
      return this.pinnedItems?.map((item) => `${item.stableId}`).includes(`${this.item.stableId}`);
    }
    return false;
  }

  propagateNewHighlightedItem(newId: string) {
    this.highlightedItemChange.emit(newId);
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
    this.setCollapseValue(!this.isCollapsed());
  }

  expandTree() {
    this.setCollapseValue(true);
  }

  isCollapsed() {
    if (this.isAlwaysCollapsed || this.isLeaf(this.item)) {
      return true;
    }

    if (this.useGlobalCollapsedState) {
      return (
        this.store.get(`collapsedState.item.${this.dependencies}.${this.item?.stableId}`) ===
          'true' ?? this.isCollapsedByDefault
      );
    }
    return this.localCollapsedState;
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

  private setCollapseValue(isCollapsed: boolean) {
    if (this.useGlobalCollapsedState) {
      this.store.add(
        `collapsedState.item.${this.dependencies}.${this.item?.stableId}`,
        `${isCollapsed}`
      );
    } else {
      this.localCollapsedState = isCollapsed;
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

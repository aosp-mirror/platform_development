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
import {assertDefined} from 'common/assert_utils';
import {DiffType} from 'viewers/common/diff_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {nodeInnerItemStyles} from 'viewers/components/styles/node.styles';

@Component({
  selector: 'tree-node',
  template: `
    <div *ngIf="showStateIcon" class="icon-wrapper-show-state" [style]="getShowStateIconStyle()">
      <button class="icon-button toggle-rect-show-state-btn" (click)="toggleRectShowState($event)">
        <mat-icon class="material-symbols-outlined">
          {{ showStateIcon }}
        </mat-icon>
      </button>
    </div>
    <div *ngIf="showChevron()" class="icon-wrapper">
      <button class="icon-button toggle-tree-btn" (click)="toggleTree($event)">
        <mat-icon>
          {{ isExpanded ? 'arrow_drop_down' : 'chevron_right' }}
        </mat-icon>
      </button>
    </div>

    <div *ngIf="showLeafNodeIcon()" class="icon-wrapper leaf-node-icon-wrapper">
      <mat-icon class="leaf-node-icon"></mat-icon>
    </div>

    <div *ngIf="showPinNodeIcon()" class="icon-wrapper">
      <button class="icon-button pin-node-btn" (click)="pinNode($event)">
        <mat-icon [class.material-symbols-outlined]="!isPinned"> push_pin </mat-icon>
      </button>
    </div>

    <div class="description">
      <hierarchy-tree-node-data-view
        *ngIf="node && !isPropertyTreeNode()"
        [node]="node"></hierarchy-tree-node-data-view>
      <property-tree-node-data-view
        *ngIf="isPropertyTreeNode()"
        [node]="node"></property-tree-node-data-view>
    </div>

    <div *ngIf="!isLeaf && !isExpanded && !isPinned" class="icon-wrapper">
      <button
        class="icon-button expand-tree-btn"
        [class]="collapseDiffClass"
        (click)="expandTree($event)">
        <mat-icon aria-hidden="true"> more_horiz </mat-icon>
      </button>
    </div>
  `,
  styles: [nodeInnerItemStyles],
})
export class TreeNodeComponent {
  @Input() node?: UiHierarchyTreeNode | UiPropertyTreeNode;
  @Input() isLeaf?: boolean;
  @Input() flattened?: boolean;
  @Input() isExpanded?: boolean;
  @Input() isPinned = false;
  @Input() isInPinnedSection = false;
  @Input() isSelected = false;
  @Input() showStateIcon?: string;

  @Output() readonly toggleTreeChange = new EventEmitter<void>();
  @Output() readonly rectShowStateChange = new EventEmitter<void>();
  @Output() readonly expandTreeChange = new EventEmitter<boolean>();
  @Output() readonly pinNodeChange = new EventEmitter<UiHierarchyTreeNode>();

  collapseDiffClass = '';
  private el: HTMLElement;
  private treeWrapper: HTMLElement | undefined;
  private readonly gutterOffset = -13;

  constructor(@Inject(ElementRef) public elementRef: ElementRef) {
    this.el = elementRef.nativeElement;
  }

  ngAfterViewInit() {
    this.treeWrapper = this.getTreeWrapper();
  }

  ngOnChanges() {
    this.collapseDiffClass = this.updateCollapseDiffClass();
    if (!this.isPinned && this.isSelected && !this.isNodeInView()) {
      this.el.scrollIntoView({block: 'center', inline: 'nearest'});
    }
  }

  isNodeInView() {
    if (!this.treeWrapper) {
      return false;
    }
    const rect = this.el.getBoundingClientRect();
    const parentRect = this.treeWrapper.getBoundingClientRect();
    return rect.top >= parentRect.top && rect.bottom <= parentRect.bottom;
  }

  getTreeWrapper(): HTMLElement | undefined {
    let parent = this.el;
    while (
      !parent.className.includes('tree-wrapper') &&
      parent?.parentElement
    ) {
      parent = parent.parentElement;
    }
    if (!parent.className.includes('tree-wrapper')) {
      return undefined;
    }
    return parent;
  }

  isPropertyTreeNode() {
    return this.node instanceof UiPropertyTreeNode;
  }

  showPinNodeIcon() {
    return (
      (this.node instanceof UiHierarchyTreeNode && !this.node.isRoot()) ?? false
    );
  }

  toggleTree(event: MouseEvent) {
    event.stopPropagation();
    this.toggleTreeChange.emit();
  }

  toggleRectShowState(event: MouseEvent) {
    event.stopPropagation();
    this.rectShowStateChange.emit();
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
    this.pinNodeChange.emit(assertDefined(this.node) as UiHierarchyTreeNode);
  }

  updateCollapseDiffClass() {
    if (this.isExpanded) {
      return '';
    }

    const childrenDiffClasses = this.getAllDiffTypesOfChildren(
      assertDefined(this.node),
    );

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

  getShowStateIconStyle() {
    const nodeMargin = this.flattened
      ? 0
      : Number(this.el.style.marginLeft.split('px')[0]);
    return {
      marginLeft: nodeMargin + this.gutterOffset + 'px',
    };
  }

  private getAllDiffTypesOfChildren(
    node: UiHierarchyTreeNode | UiPropertyTreeNode,
  ) {
    const classes = new Set();
    for (const child of node.getAllChildren().values()) {
      classes.add(child.getDiff());
      for (const diffClass of this.getAllDiffTypesOfChildren(child)) {
        classes.add(diffClass);
      }
    }

    return classes;
  }
}

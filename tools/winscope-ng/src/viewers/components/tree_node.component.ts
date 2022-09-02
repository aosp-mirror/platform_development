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
import { nodeInnerItemStyles } from "viewers/styles/node.styles";
import { Tree } from "viewers/common/tree_utils";

@Component({
  selector: "tree-node",
  template: `
    <button
      class="icon-button toggle-tree-btn"
      (click)="toggleTree($event)"
      *ngIf="showChevron()"
    >
      <mat-icon class="icon-button">
        {{isCollapsed ? "chevron_right" : "arrow_drop_down"}}
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
      *ngIf="!isEntryNode()"
    >
      <mat-icon class="icon-button">
        {{isPinned ? "star" : "star_border"}}
      </mat-icon>
    </button>

    <div class="description">
      <tree-element
        [item]="item"
      ></tree-element>
    </div>

    <button
      *ngIf="hasChildren && isCollapsed"
      (click)="expandTree($event)"
      class="icon-button expand-tree-btn"
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
  @Input() item!: Tree | null;
  @Input() isLeaf?: boolean;
  @Input() flattened?: boolean;
  @Input() isCollapsed?: boolean;
  @Input() hasChildren?: boolean = false;
  @Input() isPinned?: boolean = false;
  @Input() isInPinnedSection?: boolean = false;

  @Output() toggleTreeChange = new EventEmitter<void>();
  @Output() expandTreeChange = new EventEmitter<boolean>();
  @Output() pinNodeChange = new EventEmitter<Tree>();

  isEntryNode() {
    return this.item.kind === "entry" ?? false;
  }

  toggleTree(event: MouseEvent) {
    event.stopPropagation();
    this.toggleTreeChange.emit();
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
}

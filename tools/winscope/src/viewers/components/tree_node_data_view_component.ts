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
import {Component, Input} from '@angular/core';
import {Chip} from 'viewers/common/chip';
import {HierarchyTreeNode, Terminal, UiTreeNode} from 'viewers/common/ui_tree_utils';
import {treeNodeDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';

@Component({
  selector: 'tree-node-data-view',
  template: `
    <span class="mat-body-1">
      <span class="mat-body-2">{{ item.kind }}</span>
      <ng-container *ngIf="item.kind && item.name">&ngsp;-&ngsp;</ng-container>
      <span *ngIf="showShortName()" [matTooltip]="itemTooltip()">{{ itemShortName() }}</span>
      <ng-container *ngIf="!showShortName()">{{ item.name }}</ng-container>
      <div *ngFor="let chip of chips()" [class]="chipClass(chip)" [matTooltip]="chip.long">
        {{ chip.short }}
      </div>
    </span>
  `,
  styles: [treeNodeDataViewStyles],
})
export class TreeNodeDataViewComponent {
  @Input() item!: UiTreeNode;

  chips() {
    return this.item instanceof HierarchyTreeNode ? this.item.chips : [];
  }

  itemShortName() {
    return this.item instanceof HierarchyTreeNode && this.item.shortName
      ? this.item.shortName
      : this.item.name;
  }

  itemTooltip() {
    if (this.item.name instanceof Terminal) {
      return '';
    }
    return this.item.name ?? '';
  }

  showShortName() {
    return (
      this.item instanceof HierarchyTreeNode &&
      this.item.simplifyNames &&
      this.item.shortName &&
      this.item.shortName !== this.item.name
    );
  }

  chipClass(chip: Chip) {
    return [
      'tree-view-internal-chip',
      'tree-view-chip',
      'tree-view-chip' + '-' + (chip.type.toString() || 'default'),
    ];
  }
}

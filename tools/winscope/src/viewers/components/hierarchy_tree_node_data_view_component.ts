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
import {Component, Input} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {Chip} from 'viewers/common/chip';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {hierarchyTreeNodeDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';

@Component({
  selector: 'hierarchy-tree-node-data-view',
  template: `
    <span class="mat-body-1" *ngIf="node">
      <span class="mat-body-2" *ngIf="node.heading()">{{ node.heading() }}</span>
      <ng-container *ngIf="node.heading()">&ngsp;-&ngsp;</ng-container>
      <span [matTooltip]="nodeTooltip()">{{ node.getDisplayName() }}</span>
      <div *ngFor="let chip of node.getChips()" [class]="chipClass(chip)" [matTooltip]="chip.long">
        {{ chip.short }}
      </div>
    </span>
  `,
  styles: [hierarchyTreeNodeDataViewStyles],
})
export class HierarchyTreeNodeDataViewComponent {
  @Input() node?: UiHierarchyTreeNode;

  nodeTooltip() {
    return assertDefined(this.node).name;
  }

  chipClass(chip: Chip) {
    return [
      'tree-view-internal-chip',
      'tree-view-chip',
      'tree-view-chip' + '-' + (chip.type.toString() || 'default'),
    ];
  }
}

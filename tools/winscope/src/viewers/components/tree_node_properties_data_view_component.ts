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
import {PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {treeNodePropertiesDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';

@Component({
  selector: 'tree-node-properties-data-view',
  template: `
    <p class="mat-body-1">
      {{ item.propertyKey }}
      <ng-container *ngIf="item.propertyValue">
        :&ngsp;
        <span [class]="[valueClass()]" class="value">{{ item.propertyValue }}</span>
      </ng-container>
    </p>
  `,
  styles: [treeNodePropertiesDataViewStyles],
})
export class TreeNodePropertiesDataViewComponent {
  @Input() item!: PropertiesTreeNode;

  valueClass() {
    if (!this.item.propertyValue) {
      return null;
    }

    if (this.item.propertyValue === 'null') {
      return 'null';
    }

    if (this.item.propertyValue === 'true') {
      return 'true';
    }

    if (this.item.propertyValue === 'false') {
      return 'false';
    }

    if (!isNaN(Number(this.item.propertyValue))) {
      return 'number';
    }

    return null;
  }
}

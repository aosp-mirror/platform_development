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
import {DiffType} from 'viewers/common/diff_type';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {propertyTreeNodeDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';

@Component({
  selector: 'property-tree-node-data-view',
  template: `
    <div class="mat-body-1 node-property" *ngIf="node">
    <span class="property-key"> {{ getKey(node) }} </span>
    <div *ngIf="getValue()" class="property-value">
      <a [class]="[valueClass()]" class="value new">{{ getValue() }}</a>
      <s *ngIf="isModified()" class="old-value">{{ getOldValue() }}</s>
    </div>
    </div>
  `,
  styles: [propertyTreeNodeDataViewStyles],
})
export class PropertyTreeNodeDataViewComponent {
  @Input() node?: UiPropertyTreeNode;

  getKey(node: UiPropertyTreeNode) {
    if (!this.getValue()) {
      return node.getDisplayName();
    }
    return node.getDisplayName() + ': ';
  }

  valueClass() {
    const property = assertDefined(this.node).formattedValue();
    if (!property) {
      return null;
    }

    if (property === 'null') {
      return 'null';
    }

    if (property === 'true') {
      return 'true';
    }

    if (property === 'false') {
      return 'false';
    }

    if (!isNaN(Number(property))) {
      return 'number';
    }

    return null;
  }

  getValue() {
    return assertDefined(this.node).formattedValue();
  }

  isModified() {
    return assertDefined(this.node).getDiff() === DiffType.MODIFIED;
  }

  getOldValue() {
    return assertDefined(this.node).getOldValue();
  }
}

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
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {DiffType} from 'viewers/common/ui_tree_utils_legacy';
import {treeNodePropertiesDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';

@Component({
  selector: 'tree-node-properties-data-view',
  template: `
    <div class="mat-body-1" *ngIf="node">
      {{ node.getDisplayName() }}
      <div *ngIf="value()" class="node-property">
        <span> :&ngsp; </span>
        <div class="property-info">
          <a [class]="[valueClass()]" class="value new">{{ value() }}</a>
          <s *ngIf="isModified()" class="old-value">{{ oldValue() }}</s>
        </div>
      </div>
    </div>
  `,
  styles: [treeNodePropertiesDataViewStyles],
})
export class TreeNodePropertiesDataViewComponent {
  @Input() node?: UiPropertyTreeNode;

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

  value() {
    return assertDefined(this.node).formattedValue();
  }

  isModified() {
    return assertDefined(this.node).getDiff() === DiffType.MODIFIED;
  }

  oldValue() {
    return assertDefined(this.node).getOldValue();
  }
}

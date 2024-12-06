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
import {Component, ElementRef, Inject, Input} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'common/time';
import {DiffType} from 'viewers/common/diff_type';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {TimestampClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {propertyTreeNodeDataViewStyles} from 'viewers/components/styles/tree_node_data_view.styles';
import {timeButtonStyle} from './styles/clickable_property.styles';

@Component({
  selector: 'property-tree-node-data-view',
  template: `
    <div class="node-property" *ngIf="node">
      <span class=" mat-body-1 property-key"> {{ getKey(node) }} </span>
      <div *ngIf="node?.formattedValue()" class="property-value" [class]="[timeClass()]">
        <button
          *ngIf="isTimestamp()"
          class="time-button"
          mat-button
          color="primary"
          (click)="onTimestampClicked(node)">
          {{ node.formattedValue() }}
        </button>
        <a *ngIf="!isTimestamp()" [class]="[valueClass()]" class="mat-body-2 value new">{{ node.formattedValue() }}</a>
        <s *ngIf="isModified()" class="old-value">{{ node.getOldValue() }}</s>
      </div>
    </div>
  `,
  styles: [
    `
      .property-value button {
        white-space: normal;
      }
    `,
    propertyTreeNodeDataViewStyles,
    timeButtonStyle,
  ],
})
export class PropertyTreeNodeDataViewComponent {
  @Input() node?: UiPropertyTreeNode;

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  getKey(node: UiPropertyTreeNode) {
    if (!this.node?.formattedValue()) {
      return node.getDisplayName();
    }
    return node.getDisplayName() + ': ';
  }

  isTimestamp() {
    return this.node?.getValue() instanceof Timestamp;
  }

  onTimestampClicked(timestampNode: UiPropertyTreeNode) {
    const timestamp: Timestamp = timestampNode.getValue();
    const customEvent = new CustomEvent(ViewerEvents.TimestampClick, {
      bubbles: true,
      detail: new TimestampClickDetail(undefined, timestamp),
    });
    this.elementRef.nativeElement.dispatchEvent(customEvent);
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

  timeClass() {
    if (this.isTimestamp()) {
      return 'time';
    }

    return null;
  }

  isModified() {
    return assertDefined(this.node).getDiff() === DiffType.MODIFIED;
  }
}

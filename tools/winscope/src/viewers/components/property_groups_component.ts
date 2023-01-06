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
import {Layer} from 'trace/flickerlib/common';

@Component({
  selector: 'property-groups',
  template: `
    <div class="group">
      <h3 class="group-header mat-subheading-2">Visibility</h3>
      <div class="left-column">
        <p class="mat-body-1 flags">
          <span class="mat-body-2">Flags:</span>
          &ngsp;
          {{ item.verboseFlags ? item.verboseFlags : item.flags }}
        </p>
        <p *ngFor="let reason of summary()" class="mat-body-1">
          <span class="mat-body-2">{{ reason.key }}:</span>
          &ngsp;
          {{ reason.value }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Geometry</h3>
      <div class="left-column">
        <p class="column-header mat-small">Calculated</p>
        <p class="property mat-body-2">Transform:</p>
        <transform-matrix
          [transform]="item.transform"
          [formatFloat]="formatFloat"></transform-matrix>
        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="Raw value read from proto.bounds. This is the buffer size or
              requested crop cropped by parent bounds."
            >Crop:</span
          >
          &ngsp;
          {{ item.bounds }}
        </p>

        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="Raw value read from proto.screenBounds. This is the calculated crop
              transformed."
            >Final Bounds:</span
          >
          &ngsp;
          {{ item.screenBounds }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Requested</p>
        <p class="property mat-body-2">Transform:</p>
        <transform-matrix
          [transform]="item.requestedTransform"
          [formatFloat]="formatFloat"></transform-matrix>
        <p class="mat-body-1">
          <span class="mat-body-2">Crop:</span>
          &ngsp;
          {{ item.crop ? item.crop : '[empty]' }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Buffer</h3>
      <div class="left-column">
        <p class="mat-body-1">
          <span class="mat-body-2">Size:</span>
          &ngsp;
          {{ item.activeBuffer }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Frame Number:</span>
          &ngsp;
          {{ item.currFrame }}
        </p>
        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="Rotates or flips the buffer in place. Used with display transform
              hint to cancel out any buffer transformation when sending to
              HWC."
            >Transform:</span
          >
          &ngsp;
          {{ item.bufferTransform }}
        </p>
      </div>
      <div class="right-column">
        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="Scales buffer to the frame by overriding the requested transform
              for this item."
            >Destination Frame:</span
          >
          &ngsp;
          {{ getDestinationFrame() }}
        </p>
        <p *ngIf="hasIgnoreDestinationFrame()" class="mat-body-1">
          Destination Frame ignored because item has eIgnoreDestinationFrame flag set.
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Hierarchy</h3>
      <div class="left-column">
        <p class="mat-body-1">
          <span class="mat-body-2">z-order:</span>
          &ngsp;
          {{ item.z }}
        </p>
        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="item is z-ordered relative to its relative parents but its bounds
              and other properties are inherited from its parents."
            >relative parent:</span
          >
          &ngsp;
          {{ item.zOrderRelativeOfId == -1 ? 'none' : item.zOrderRelativeOfId }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Effects</h3>
      <div class="left-column">
        <p class="column-header mat-small">Calculated</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Color:</span>
          &ngsp;
          {{ item.color }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Shadow:</span>
          &ngsp;
          {{ item.shadowRadius }} px
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Corner Radius:</span>
          &ngsp;
          {{ formatFloat(item.cornerRadius) }} px
        </p>
        <p class="mat-body-1">
          <span
            class="mat-body-2"
            matTooltip="Crop used to define the bounds of the corner radii. If the bounds
              are greater than the item bounds then the rounded corner will not
              be visible."
            >Corner Radius Crop:</span
          >
          &ngsp;
          {{ item.cornerRadiusCrop }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Blur:</span>
          &ngsp;
          {{ item.proto?.backgroundBlurRadius ? item.proto?.backgroundBlurRadius : 0 }} px
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Requested</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Color:</span>
          &ngsp;
          {{ item.requestedColor }}
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Shadow:</span>
          &ngsp;
          {{ item.proto?.requestedShadowRadius ? item.proto?.requestedShadowRadius : 0 }} px
        </p>
        <p class="mat-body-1">
          <span class="mat-body-2">Corner Radius:</span>
          &ngsp;
          {{
            item.proto?.requestedCornerRadius ? formatFloat(item.proto?.requestedCornerRadius) : 0
          }}
          px
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group">
      <h3 class="group-header mat-subheading-2">Input</h3>
      <ng-container *ngIf="hasInputChannel()">
        <div class="left-column">
          <p class="property mat-body-2">To Display Transform:</p>
          <transform-matrix
            [transform]="item.inputTransform"
            [formatFloat]="formatFloat"></transform-matrix>
          <p class="mat-body-1">
            <span class="mat-body-2">Touchable Region:</span>
            &ngsp;
            {{ item.inputRegion }}
          </p>
        </div>
        <div class="right-column">
          <p class="column-header mat-small">Config</p>
          <p class="mat-body-1">
            <span class="mat-body-2">Focusable:</span>
            &ngsp;
            {{ item.proto?.inputWindowInfo.focusable }}
          </p>
          <p class="mat-body-1">
            <span class="mat-body-2">Crop touch region with item:</span>
            &ngsp;
            {{
              item.proto?.inputWindowInfo.cropLayerId &lt;= 0
                ? "none"
                : item.proto?.inputWindowInfo.cropLayerId
            }}
          </p>
          <p class="mat-body-1">
            <span class="mat-body-2">Replace touch region with crop:</span>
            &ngsp;
            {{ item.proto?.inputWindowInfo.replaceTouchableRegionWithCrop }}
          </p>
        </div>
      </ng-container>
      <div *ngIf="!hasInputChannel()" class="left-column">
        <p class="mat-body-1">
          <span class="mat-body-2">Input channel:</span>
          &ngsp; not set
        </p>
      </div>
    </div>
  `,
  styles: [
    `
      .group {
        display: flex;
        flex-direction: row;
        padding: 8px;
      }

      .group-header {
        width: 80px;
        color: gray;
      }

      .left-column {
        flex: 1;
        padding: 0 5px;
      }

      .right-column {
        flex: 1;
        border: 1px solid var(--border-color);
        border-left-width: 5px;
        padding: 0 5px;
      }

      .column-header {
        color: gray;
      }
    `,
  ],
})
export class PropertyGroupsComponent {
  @Input() item!: Layer;

  hasInputChannel() {
    return this.item.proto?.inputWindowInfo;
  }

  getDestinationFrame() {
    const frame = this.item.proto?.destinationFrame;
    if (frame) {
      return ` left: ${frame.left}, top: ${frame.top}, right: ${frame.right}, bottom: ${frame.bottom}`;
    } else return '';
  }

  hasIgnoreDestinationFrame() {
    return (this.item.flags & 0x400) === 0x400;
  }

  formatFloat(num: number) {
    return Math.round(num * 100) / 100;
  }

  summary(): TreeSummary {
    const summary = [];

    if (this.item?.visibilityReason?.length > 0) {
      let reason = '';
      if (Array.isArray(this.item.visibilityReason)) {
        reason = this.item.visibilityReason.join(', ');
      } else {
        reason = this.item.visibilityReason;
      }

      summary.push({key: 'Invisible due to', value: reason});
    }

    if (this.item?.occludedBy?.length > 0) {
      summary.push({
        key: 'Occluded by',
        value: this.item.occludedBy.map((it: any) => it.id).join(', '),
      });
    }

    if (this.item?.partiallyOccludedBy?.length > 0) {
      summary.push({
        key: 'Partially occluded by',
        value: this.item.partiallyOccludedBy.map((it: any) => it.id).join(', '),
      });
    }

    if (this.item?.coveredBy?.length > 0) {
      summary.push({
        key: 'Covered by',
        value: this.item.coveredBy.map((it: any) => it.id).join(', '),
      });
    }

    return summary;
  }
}

type TreeSummary = Array<{key: string; value: string}>;

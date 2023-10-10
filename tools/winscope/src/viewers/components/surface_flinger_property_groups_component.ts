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
import {Color, Layer, toColor, toCropRect, Transform} from 'flickerlib/common';

@Component({
  selector: 'surface-flinger-property-groups',
  template: `
    <div class="group">
      <h3 class="group-header mat-subheading-2">Visibility</h3>
      <div class="left-column">
        <p class="mat-body-1 flags">
          <span class="mat-body-2">Flags:</span>
          &ngsp;
          {{ properties.flags }}
        </p>
        <p *ngFor="let reason of summary()" class="mat-body-1">
          <span class="mat-body-2">{{ reason.key }}:</span>
          &ngsp;
          {{ reason.value }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group geometry">
      <h3 class="group-header mat-subheading-2">Geometry</h3>
      <div class="left-column">
        <p class="column-header mat-small">Calculated</p>
        <p class="property mat-body-2">Transform:</p>
        <transform-matrix
          *ngIf="properties.calcTransform"
          [transform]="properties.calcTransform"
          [formatFloat]="formatFloat"></transform-matrix>
        <p class="mat-body-1 crop">
          <span
            class="mat-body-2"
            matTooltip="Raw value read from proto.bounds. This is the buffer size or
              requested crop cropped by parent bounds."
            >Crop:</span
          >
          &ngsp;
          {{ properties.calcCrop }}
        </p>

        <p class="mat-body-1 final-bounds">
          <span
            class="mat-body-2"
            matTooltip="Raw value read from proto.screenBounds. This is the calculated crop
              transformed."
            >Final Bounds:</span
          >
          &ngsp;
          {{ properties.finalBounds }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Requested</p>
        <p class="property mat-body-2">Transform:</p>
        <transform-matrix
          *ngIf="properties.reqTransform"
          [transform]="properties.reqTransform"
          [formatFloat]="formatFloat"></transform-matrix>
        <p class="mat-body-1 crop">
          <span class="mat-body-2">Crop:</span>
          &ngsp;
          {{ properties.reqCrop }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group buffer">
      <h3 class="group-header mat-subheading-2">Buffer</h3>
      <div class="left-column">
        <p class="mat-body-1 size">
          <span class="mat-body-2">Size:</span>
          &ngsp;
          {{ properties.bufferSize }}
        </p>
        <p class="mat-body-1 frame-number">
          <span class="mat-body-2">Frame Number:</span>
          &ngsp;
          {{ properties.frameNumber }}
        </p>
        <p class="mat-body-1 transform">
          <span
            class="mat-body-2"
            matTooltip="Rotates or flips the buffer in place. Used with display transform
              hint to cancel out any buffer transformation when sending to
              HWC."
            >Transform:</span
          >
          &ngsp;
          {{ properties.bufferTransform }}
        </p>
      </div>
      <div class="right-column">
        <p class="mat-body-1 dest-frame">
          <span
            class="mat-body-2"
            matTooltip="Scales buffer to the frame by overriding the requested transform
              for this item."
            >Destination Frame:</span
          >
          &ngsp;
          {{ properties.destinationFrame }}
        </p>
        <p *ngIf="hasIgnoreDestinationFrame()" class="mat-body-1">
          Destination Frame ignored because item has eIgnoreDestinationFrame flag set.
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group hierarchy-info">
      <h3 class="group-header mat-subheading-2">Hierarchy</h3>
      <div class="left-column">
        <p class="mat-body-1 z-order">
          <span class="mat-body-2">z-order:</span>
          &ngsp;
          {{ properties.z }}
        </p>
        <p class="mat-body-1 rel-parent">
          <span
            class="mat-body-2"
            matTooltip="item is z-ordered relative to its relative parents but its bounds
              and other properties are inherited from its parents."
            >relative parent:</span
          >
          &ngsp;
          {{ properties.relativeParent }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group effects">
      <h3 class="group-header mat-subheading-2">Effects</h3>
      <div class="left-column">
        <p class="column-header mat-small">Calculated</p>
        <p class="mat-body-1 color">
          <span class="mat-body-2">Color:</span>
          &ngsp;
          {{ properties.calcColor }}
        </p>
        <p class="mat-body-1 shadow">
          <span class="mat-body-2">Shadow:</span>
          &ngsp;
          {{ properties.calcShadowRadius }}
        </p>
        <p class="mat-body-1 corner-radius">
          <span class="mat-body-2">Corner Radius:</span>
          &ngsp;
          {{ properties.calcCornerRadius }}
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
          {{ properties.calcCornerRadiusCrop }}
        </p>
        <p class="mat-body-1 blur">
          <span class="mat-body-2">Blur:</span>
          &ngsp;
          {{ properties.backgroundBlurRadius }}
        </p>
      </div>
      <div class="right-column">
        <p class="column-header mat-small">Requested</p>
        <p class="mat-body-1">
          <span class="mat-body-2">Color:</span>
          &ngsp;
          {{ properties.reqColor }}
        </p>
        <p class="mat-body-1 shadow">
          <span class="mat-body-2">Shadow:</span>
          &ngsp;
          {{ properties.reqShadowRadius }}
        </p>
        <p class="mat-body-1 corner-radius">
          <span class="mat-body-2">Corner Radius:</span>
          &ngsp;
          {{ properties.reqCornerRadius }}
        </p>
      </div>
    </div>
    <mat-divider></mat-divider>
    <div class="group inputs">
      <h3 class="group-header mat-subheading-2">Input</h3>
      <ng-container *ngIf="hasInputChannel()">
        <div class="left-column">
          <p class="property mat-body-2">To Display Transform:</p>
          <transform-matrix
            *ngIf="properties.inputTransform"
            [transform]="properties.inputTransform"
            [formatFloat]="formatFloat"></transform-matrix>
          <p class="mat-body-1">
            <span class="mat-body-2">Touchable Region:</span>
            &ngsp;
            {{ properties.inputRegion }}
          </p>
        </div>
        <div class="right-column">
          <p class="column-header mat-small">Config</p>
          <p class="mat-body-1 focusable">
            <span class="mat-body-2">Focusable:</span>
            &ngsp;
            {{ properties.focusable }}
          </p>
          <p class="mat-body-1 crop-touch-region">
            <span class="mat-body-2">Crop touch region with item:</span>
            &ngsp;
            {{ properties.cropTouchRegionWithItem }}
          </p>
          <p class="mat-body-1 replace-touch-region">
            <span class="mat-body-2">Replace touch region with crop:</span>
            &ngsp;
            {{ properties.replaceTouchRegionWithCrop }}
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
export class SurfaceFlingerPropertyGroupsComponent {
  @Input() item!: Layer;
  properties: any; // TODO(b/278163557): change to proper type when layer's TreeNode data type is defined

  // TODO(b/278163557): move all properties computation into parser and pass properties as @Input, as soon as layer's TreeNode data type is defined
  ngOnChanges() {
    this.properties = {
      flags: this.item.verboseFlags ? this.item.verboseFlags : this.item.flags,
      calcTransform: this.item.transform,
      calcCrop: this.item.bounds,
      finalBounds: this.item.screenBounds,
      reqTransform: this.getReqTransform(),
      reqCrop: this.item.crop ? this.item.crop : '[empty]',
      bufferSize: this.item.activeBuffer,
      frameNumber: this.item.currFrame,
      bufferTransform: this.item.bufferTransform,
      destinationFrame: this.getDestinationFrame(),
      z: this.item.z,
      relativeParent: this.getRelativeParent(),
      calcColor: this.getCalcColor(),
      calcShadowRadius: this.getPixelPropertyValue(this.item.shadowRadius),
      calcCornerRadius: this.getPixelPropertyValue(this.item.cornerRadius),
      calcCornerRadiusCrop: this.getCalcCornerRadiusCrop(),
      backgroundBlurRadius: this.getPixelPropertyValue(this.item.backgroundBlurRadius),
      reqColor: this.getReqColor(),
      reqShadowRadius: this.getPixelPropertyValue(this.item.proto?.requestedShadowRadius),
      reqCornerRadius: this.getPixelPropertyValue(this.item.proto?.requestedCornerRadius),
      inputTransform: this.getInputTransform(),
      inputRegion: this.item.inputRegion,
      focusable: this.item.proto?.inputWindowInfo?.focusable,
      cropTouchRegionWithItem: this.getCropTouchRegionWithItem(),
      replaceTouchRegionWithCrop: this.item.proto?.inputWindowInfo?.replaceTouchableRegionWithCrop,
    };
  }

  hasInputChannel() {
    return this.item.proto?.inputWindowInfo;
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

  private getReqTransform() {
    const proto = this.item.proto;
    return Transform.fromProto(proto?.requestedTransform, proto?.requestedPosition);
  }

  private getDestinationFrame() {
    const frame = this.item.proto?.destinationFrame;
    if (frame) {
      return ` left: ${frame.left}, top: ${frame.top}, right: ${frame.right}, bottom: ${frame.bottom}`;
    }
    return '[empty]';
  }

  private getCalcCornerRadiusCrop() {
    if (this.item.proto?.cornerRadiusCrop) {
      return toCropRect(this.item.proto?.cornerRadiusCrop);
    }
    return '[empty]';
  }

  private getRelativeParent() {
    if (this.item.zOrderRelativeOfId === -1) {
      return 'none';
    }
    return this.item.zOrderRelativeOfId;
  }

  private formatColor(color: Color) {
    if (color.isEmpty) {
      return `[empty], alpha: ${color.a}`;
    } else {
      return `(${color.r}, ${color.g}, ${color.b}, ${color.a})`;
    }
  }

  private getCalcColor() {
    if (this.item.color) {
      return this.formatColor(this.item.color);
    }
    return 'no color found';
  }

  private getReqColor() {
    const proto = this.item.proto;
    if (proto?.requestedColor) {
      return this.formatColor(toColor(proto?.requestedColor));
    }
    return 'no color found';
  }

  private getPixelPropertyValue(propVal: number | undefined) {
    return `${propVal ? this.formatFloat(propVal) : 0} px`;
  }

  private getInputTransform() {
    const inputWindowInfo = this.item.proto?.inputWindowInfo;
    return Transform.fromProto(inputWindowInfo?.transform, /* position */ null);
  }

  private getCropTouchRegionWithItem() {
    if (this.item.proto?.inputWindowInfo?.cropLayerId <= 0) {
      return 'none';
    }
    return this.item.proto?.inputWindowInfo?.cropLayerId;
  }
}

type TreeSummary = Array<{key: string; value: string}>;

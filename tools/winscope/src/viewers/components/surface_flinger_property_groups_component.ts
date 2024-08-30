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
import {SfCuratedProperties} from 'viewers/common/curated_properties';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {inlineButtonStyle} from './styles/clickable_property.styles';
import {viewerCardInnerStyle} from './styles/viewer_card.styles';

@Component({
  selector: 'surface-flinger-property-groups',
  template: `
    <div class="title-section">
      <collapsible-section-title
        title="PROPERTIES"
        (collapseButtonClicked)="collapseButtonClicked.emit()"></collapsible-section-title>
    </div>

    <span class="mat-body-1 placeholder-text" *ngIf="!properties"> Layer not selected. </span>

    <div class="property-groups-content" *ngIf="properties">
      <div class="group">
        <h3 class="group-header mat-subheading-2">Visibility</h3>
        <div class="left-column">
          <p class="mat-body-1 flags">
            <span class="mat-body-2">Flags:</span>
            &ngsp;
            {{ properties.flags }}
          </p>
          <span *ngFor="let summaryProperty of properties.summary" class="mat-body-1 summary inline">
            <span class="mat-body-2" [matTooltip]="summaryProperty.desc" [matTooltipShowDelay]="400">{{ summaryProperty.key }}:</span>
            <ng-container *ngIf="summaryProperty.simpleValue">
              {{ summaryProperty.simpleValue }}
            </ng-container>
            <ng-container *ngIf="summaryProperty.layerValues">
              &ngsp;
              <ng-container *ngFor="let layer of summaryProperty.layerValues; index as i">
                <button
                  mat-button
                  color="primary"
                  [matTooltip]="layer.name"
                  (click)="onIdClicked(layer.nodeId)">
                  {{ layer.layerId }}
                </button>
                {{(i === summaryProperty.layerValues.length - 1) ? '' : ', '}}
              </ng-container>
            </ng-container>
          </span>
        </div>
      </div>

      <mat-divider></mat-divider>

      <div class="group geometry">
        <h3 class="group-header mat-subheading-2">Geometry</h3>
        <div class="left-column">
          <p class="column-header mat-small">Calculated</p>
          <p class="property mat-body-2">Transform:</p>
          <transform-matrix
            *ngIf="properties.calcTransform?.getAllChildren().length > 0"
            [matTooltip]="getTransformType(properties.calcTransform)"
            [matrix]="getTransformMatrix(properties.calcTransform)"></transform-matrix>
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
            *ngIf="properties.reqTransform?.getAllChildren().length > 0"
            [matTooltip]="getTransformType(properties.reqTransform)"
            [matrix]="getTransformMatrix(properties.reqTransform)"></transform-matrix>
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
            {{ properties.bufferTransformType }}
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
          <p *ngIf="properties.ignoreDestinationFrame" class="mat-body-1 ignore-frame">
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
          <p class="mat-body-1 rel-parent inline">
            <span
              class="mat-body-2"
              matTooltip="item is z-ordered relative to its relative parents but its bounds
                  and other properties are inherited from its parents."
              >relative parent:</span>
            &ngsp;
            <ng-container *ngIf="!properties.relativeParent.nodeId">
              {{ properties.relativeParent }}
            </ng-container>
            <ng-container *ngIf="properties.relativeParent.nodeId">
              <button
                mat-button
                color="primary"
                [matTooltip]="properties.relativeParent.name"
                (click)="onIdClicked(properties.relativeParent.nodeId)">
                {{ properties.relativeParent.layerId }}
              </button>
            </ng-container>
          </p>
          <span class="mat-body-1 rel-children inline">
            <span class="mat-body-2">relative children:</span>
            &ngsp;
            <ng-container *ngIf="properties.relativeChildren.length === 0">
              none
            </ng-container>
            <ng-container *ngFor="let layer of properties.relativeChildren; index as i">
              <button
                mat-button
                color="primary"
                [matTooltip]="layer.name"
                (click)="onIdClicked(layer.nodeId)">
                {{ layer.layerId }}
              </button>
              {{(i === properties.relativeChildren.length - 1) ? '' : ', '}}
            </ng-container>
          </span>
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
          <p class="mat-body-1 corner-radius">
            <span class="mat-body-2">Corner Radius:</span>
            &ngsp;
            {{ properties.calcCornerRadius }}
          </p>
          <p class="mat-body-1 shadow">
            <span class="mat-body-2">Shadow:</span>
            &ngsp;
            {{ properties.calcShadowRadius }}
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
        <ng-container *ngIf="properties.hasInputChannel">
          <div class="left-column">
            <p class="property mat-body-2">To Display Transform:</p>
            <transform-matrix
              *ngIf="properties.inputTransform?.getAllChildren().length > 0"
              [matTooltip]="getTransformType(properties.inputTransform)"
              [matrix]="getTransformMatrix(properties.inputTransform)"></transform-matrix>
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
            <p class="mat-body-1 input-config">
              <span class="mat-body-2">Input Config:</span>
              &ngsp;
              {{ properties.inputConfig }}
            </p>
          </div>
        </ng-container>
        <div *ngIf="!properties.hasInputChannel" class="left-column">
          <p class="mat-body-1">
            <span class="mat-body-2">Input channel:</span>
            &ngsp; not set
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      :host collapsible-section-title {
        padding-bottom: 8px;
      }
      .placeholder-text {
        padding: 8px 12px;
      }

      .property-groups-content {
        overflow-y: auto;
        padding: 0px 12px;
      }

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

      .summary {
        display: block;
      }
    `,
    inlineButtonStyle,
    viewerCardInnerStyle,
  ],
})
export class SurfaceFlingerPropertyGroupsComponent {
  @Input() properties: SfCuratedProperties | undefined;

  @Output() collapseButtonClicked = new EventEmitter();

  constructor(@Inject(ElementRef) private elementRef: ElementRef) {}

  getTransformType(transformNode: UiPropertyTreeNode): string {
    const typeFlags = transformNode.formattedValue();
    return typeFlags !== 'null' ? typeFlags : 'IDENTITY';
  }

  getTransformMatrix(transformNode: UiPropertyTreeNode): UiPropertyTreeNode {
    return assertDefined(transformNode.getChildByName('matrix'));
  }

  onIdClicked(layerNodeId: string) {
    const event = new CustomEvent(ViewerEvents.HighlightedIdChange, {
      bubbles: true,
      detail: {id: layerNodeId},
    });
    this.elementRef.nativeElement.dispatchEvent(event);
  }
}

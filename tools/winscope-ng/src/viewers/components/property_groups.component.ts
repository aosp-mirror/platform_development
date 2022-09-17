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
import { Component, Input } from "@angular/core";
import { Layer } from "common/trace/flickerlib/common";

@Component({
  selector: "property-groups",
  template: `
   <div>
    <div class="group">
        <span class="group-header">
          <span class="group-heading">Visibility</span>
        </span>
        <div class="left-column">
          <span class="key">Flags:</span>
          <span class="value">{{ item.flags }}</span>
          <div></div>
          <div *ngIf="summary().length > 0">
            <div *ngFor="let reason of summary()">
              <span class="key">{{ reason.key }}:</span>
              <span class="value">{{ reason.value }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="group">
        <span class="group-header">Geometry</span>
        <div class="left-column">
          <div class="column-header">Calculated</div>
          <span class="key">Transform:</span>
          <transform-matrix [transform]="item.transform" [formatFloat]="formatFloat"></transform-matrix>
          <div></div>
          <span
            class="key"
            matTooltip="Raw value read from proto.bounds. This is the buffer size or
              requested crop cropped by parent bounds."
          >Crop:</span>
          <span class="value">{{ item.bounds }}</span>
          <div></div>
          <span
            class="key"
            matTooltip="Raw value read from proto.screenBounds. This is the calculated crop
              transformed."
          >Final Bounds:</span>
          <span class="value">{{ item.screenBounds }}</span>
        </div>
        <div class="right-column">
          <div class="column-header">Requested</div>
          <span class="key">Transform:</span>
          <transform-matrix [transform]="item.requestedTransform" [formatFloat]="formatFloat"></transform-matrix>
          <div></div>
          <span class="key">Crop:</span>
          <span class="value">{{ item.crop ? item.crop : "[empty]" }}</span>
        </div>
      </div>
      <div class="group">
        <span class="group-header">
          <span class="group-heading">Buffer</span>
        </span>
        <div *ngIf="item.isBufferLayer" class="left-column">
          <div></div>
          <span class="key">Size:</span>
          <span class="value">{{ item.activeBuffer }}</span>
          <div></div>
          <span class="key">Frame Number:</span>
          <span class="value">{{ item.currFrame }}</span>
          <div></div>
          <span
            class="key"
            matTooltip="Rotates or flips the buffer in place. Used with display transform
              hint to cancel out any buffer transformation when sending to
              HWC."
          >Transform:</span>
          <span class="value">{{ item.bufferTransform }}</span>
        </div>
        <div *ngIf="item.isBufferLayer" class="right-column">
          <div></div>
          <span
            class="key"
            matTooltip="Scales buffer to the frame by overriding the requested transform
              for this item."
          >Destination Frame:</span>
          <span class="value">{{ getDestinationFrame() }}</span>
          <div></div>
          <span
            *ngIf="hasIgnoreDestinationFrame()"
            class="value"
            >Destination Frame ignored because item has eIgnoreDestinationFrame
            flag set.
          </span>
        </div>
        <div *ngIf="item.isContainerLayer" class="left-column">
          <span class="key"></span>
          <span class="value">Container item</span>
        </div>
        <div *ngIf="item.isEffectLayer" class="left-column">
          <span class="key"></span>
          <span class="value">Effect item</span>
        </div>
      </div>
      <div class="group">
        <span class="group-header">
          <span class="group-heading">Hierarchy</span>
        </span>
        <div class="left-column">
          <div></div>
          <span class="key">z-order:</span>
          <span class="value">{{ item.z }}</span>
          <div></div>
          <span
            class="key"
            matTooltip="item is z-ordered relative to its relative parents but its bounds
              and other properties are inherited from its parents."
          >relative parent:</span>
          <span class="value">
            {{ item.zOrderRelativeOfId == -1 ? "none" : item.zOrderRelativeOfId }}
          </span>
        </div>
      </div>
      <div class="group">
        <span class="group-header">
          <span class="group-heading">Effects</span>
        </span>
        <div class="left-column">
          <div class="column-header">Calculated</div>
          <span class="key">Color:</span>
          <span class="value">{{ item.color }}</span>
          <div></div>
          <span class="key">Shadow:</span>
          <span class="value">{{ item.shadowRadius }} px</span>
          <div></div>
          <span class="key">Corner Radius:</span>
          <span class="value">{{ formatFloat(item.cornerRadius) }} px</span>
          <div></div>
          <span
            class="key"
            matTooltip="Crop used to define the bounds of the corner radii. If the bounds
              are greater than the item bounds then the rounded corner will not
              be visible."
          >Corner Radius Crop:</span>
          <span class="value">{{ item.cornerRadiusCrop }}</span>
          <div></div>
          <span class="key">Blur:</span>
          <span class="value">
            {{
              item.proto?.backgroundBlurRadius
                ? item.proto?.backgroundBlurRadius
                : 0
            }} px
          </span>
        </div>
        <div class="right-column">
          <div class="column-header">Requested</div>
          <span class="key">Color:</span>
          <span class="value">{{ item.requestedColor }}</span>
          <div></div>
          <span class="key">Shadow:</span>
          <span class="value">
            {{
              item.proto?.requestedShadowRadius
                ? item.proto?.requestedShadowRadius
                : 0
            }} px
          </span>
          <div></div>
          <span class="key">Corner Radius:</span>
          <span class="value">
            {{
              item.proto?.requestedCornerRadius
                ? formatFloat(item.proto?.requestedCornerRadius)
                : 0
            }} px
          </span>
        </div>
      </div>
      <div class="group">
        <span class="group-header">
          <span class="group-heading">Input</span>
        </span>
        <div *ngIf="hasInputChannel()" class="left-column">
          <span class="key">To Display Transform:</span>
          <transform-matrix [transform]="item.inputTransform" [formatFloat]="formatFloat"></transform-matrix>
          <div></div>
          <span class="key">Touchable Region:</span>
          <span class="value">{{ item.inputRegion }}</span>
        </div>
        <div *ngIf="hasInputChannel()" class="right-column">
          <span class="key">Config:</span>
          <span class="value"></span>
          <div></div>
          <span class="key">Focusable:</span>
          <span class="value">{{ item.proto?.inputWindowInfo.focusable }}</span>
          <div></div>
          <span class="key">Crop touch region with item:</span>
          <span class="value">
            {{
              item.proto?.inputWindowInfo.cropLayerId &lt;= 0
                ? "none"
                : item.proto?.inputWindowInfo.cropLayerId
            }}
          </span>
          <div></div>
          <span class="key">Replace touch region with crop:</span>
          <span class="value">
            {{
              item.proto?.inputWindowInfo.replaceTouchableRegionWithCrop
            }}
          </span>
        </div>
        <div *ngIf="!hasInputChannel()" class="left-column">
          <span class="key"></span>
          <span class="value">No input channel set</span>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .group {
        padding: 0.5rem;
        border-bottom: thin solid rgba(0, 0, 0, 0.12);
        flex-direction: row;
        display: flex;
      }

      .group .key {
        font-weight: 500;
        padding-right: 5px;
      }

      .group .value {
        color: rgba(0, 0, 0, 0.75);
      }

      .group-header {
        justify-content: center;
        padding: 0px 5px;
        width: 80px;
        display: inline-block;
        font-size: bigger;
        color: grey;
      }

      .left-column {
        width: 320px;
        max-width: 100%;
        display: inline-block;
        vertical-align: top;
        overflow: auto;
        border-right: 5px solid rgba(#000, 0.12);
        padding-right: 20px;
      }

      .right-column {
        width: 320px;
        max-width: 100%;
        display: inline-block;
        vertical-align: top;
        overflow: auto;
        border: 1px solid rgba(#000, 0.12);
      }

      .column-header {
        font-weight: lighter;
        font-size: smaller;
      }
    `
  ],
})

export class PropertyGroupsComponent {
  @Input() item!: Layer;

  public hasInputChannel() {
    return this.item.proto?.inputWindowInfo;
  }

  public getDestinationFrame() {
    const frame = this.item.proto?.destinationFrame;
    if (frame) {
      return ` left: ${frame.left}, top: ${frame.top}, right: ${frame.right}, bottom: ${frame.bottom}`;
    }
    else return "";
  }

  public hasIgnoreDestinationFrame() {
    return (this.item.flags & 0x400) === 0x400;
  }

  public formatFloat(num: number) {
    return Math.round(num * 100) / 100;
  }


  public summary(): TreeSummary {
    const summary = [];

    if (this.item?.visibilityReason?.length > 0) {
      let reason = "";
      if (Array.isArray(this.item.visibilityReason)) {
        reason = this.item.visibilityReason.join(", ");
      } else {
        reason = this.item.visibilityReason;
      }

      summary.push({key: "Invisible due to", value: reason});
    }

    if (this.item?.occludedBy?.length > 0) {
      summary.push({key: "Occluded by", value: this.item.occludedBy.map((it: any) => it.id).join(", ")});
    }

    if (this.item?.partiallyOccludedBy?.length > 0) {
      summary.push({
        key: "Partially occluded by",
        value: this.item.partiallyOccludedBy.map((it: any) => it.id).join(", "),
      });
    }

    if (this.item?.coveredBy?.length > 0) {
      summary.push({key: "Covered by", value: this.item.coveredBy.map((it: any) => it.id).join(", ")});
    }

    return summary;
  }

}


type TreeSummary = Array<{key: string, value: string}>
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
import {
  Component,
  Input,
} from "@angular/core";
import { UiData } from "./ui_data";
import { TRACE_INFO } from "app/trace_info";
import { TraceType } from "common/trace/trace_type";
import { PersistentStore } from "common/persistent_store";

@Component({
  selector: "viewer-surface-flinger",
  template: `
      <div fxLayout="row wrap" fxLayoutGap="10px grid" class="card-grid">
        <mat-card id="sf-rects-view" class="rects-view">
          <rects-view
            [rects]="inputData?.rects ?? []"
            [highlightedItems]="inputData?.highlightedItems ?? []"
            [displayIds]="inputData?.displayIds ?? []"
            [forceRefresh]="active"
            [hasVirtualDisplays]="inputData?.hasVirtualDisplays ?? false"
          ></rects-view>
        </mat-card>
        <div fxLayout="row wrap" fxLayoutGap="10px grid" class="card-grid">
          <mat-card id="sf-hierarchy-view" class="hierarchy-view">
            <hierarchy-view
              [tree]="inputData?.tree ?? null"
              [dependencies]="inputData?.dependencies ?? []"
              [highlightedItems]="inputData?.highlightedItems ?? []"
              [pinnedItems]="inputData?.pinnedItems ?? []"
              [store]="store"
              [userOptions]="inputData?.hierarchyUserOptions ?? {}"
            ></hierarchy-view>
          </mat-card>
          <mat-card id="sf-properties-view" class="properties-view">
            <properties-view
              [userOptions]="inputData?.propertiesUserOptions ?? {}"
              [propertiesTree]="inputData?.propertiesTree ?? {}"
              [selectedFlickerItem]="inputData?.selectedLayer ?? {}"
              [propertyGroups]="true"
            ></properties-view>
          </mat-card>
        </div>
      </div>
  `,
  styles: [
    `
      @import 'https://fonts.googleapis.com/icon?family=Material+Icons';
      :root {
        --default-border: #DADCE0;
      }

      mat-icon {
        margin: 5px
      }

      .icon-button {
        background: none;
        border: none;
        display: inline-block;
        vertical-align: middle;
      }

      viewer-surface-flinger {
        font-family: Arial, Helvetica, sans-serif;
      }

      .header-button {
        background: none;
        border: none;
        display: inline-block;
        vertical-align: middle;
      }

      .card-grid {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: row;
        overflow: auto;
      }

      .rects-view {
        font: inherit;
        flex: none;
        width: 350px;
        height: 52.5rem;
        margin: 0px;
        border-top: 1px solid var(--default-border);
        border-right: 1px solid var(--default-border);
        border-radius: 0;
      }

      .hierarchy-view {
        font: inherit;
        margin: 0px;
        width: 50%;
        height: 52.5rem;
        border-radius: 0;
        border-top: 1px solid var(--default-border);
        border-right: 1px solid var(--default-border);
        border-left: 1px solid var(--default-border);
      }

      .properties-view {
        font: inherit;
        margin: 0px;
        width: 50%;
        height: 52.5rem;
        border-radius: 0;
        border-top: 1px solid var(--default-border);
        border-left: 1px solid var(--default-border);
      }
    `,
  ]
})
export class ViewerSurfaceFlingerComponent {
  @Input() inputData: UiData | null = null;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;
  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;
}

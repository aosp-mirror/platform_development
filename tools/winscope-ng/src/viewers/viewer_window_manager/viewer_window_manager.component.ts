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
import {UiData} from "./ui_data";
import { TRACE_INFO } from "app/trace_info";
import { TraceType } from "common/trace/trace_type";
import { PersistentStore } from "common/persistent_store";

@Component({
  selector: "viewer-window-manager",
  template: `
      <div class="card-grid">
        <rects-view
          id="wm-rects-view"
          class="rects-view"
          [rects]="inputData?.rects ?? []"
          [displayIds]="inputData?.displayIds ?? []"
          [highlightedItems]="inputData?.highlightedItems ?? []"
          [forceRefresh]="active"
        ></rects-view>
        <hierarchy-view
          id="wm-hierarchy-view"
          class="hierarchy-view"
          [tree]="inputData?.tree ?? null"
          [dependencies]="inputData?.dependencies ?? []"
          [highlightedItems]="inputData?.highlightedItems ?? []"
          [pinnedItems]="inputData?.pinnedItems ?? []"
          [store]="store"
          [userOptions]="inputData?.hierarchyUserOptions ?? {}"
        ></hierarchy-view>
        <properties-view
          id="wm-properties-view"
          class="properties-view"
          [userOptions]="inputData?.propertiesUserOptions ?? {}"
          [propertiesTree]="inputData?.propertiesTree ?? {}"
          [isProtoDump]="true"
        ></properties-view>
      </div>
  `,
  styles: [
    `
      .rects-view {
        flex: 1;
        padding: 16px;
        display: flex;
        flex-direction: column;
        border-top: 1px solid var(--default-border);
        border-right: 1px solid var(--default-border);
      }

      .hierarchy-view {
        flex: 1;
        padding: 16px;
        display: flex;
        flex-direction: column;
        border-top: 1px solid var(--default-border);
        border-right: 1px solid var(--default-border);
      }

      .properties-view {
        flex: 1;
        padding: 16px;
        border-top: 1px solid var(--default-border);
      }
    `,
  ]
})
export class ViewerWindowManagerComponent {
  @Input() inputData: UiData | null = null;
  @Input() store: PersistentStore = new PersistentStore();
  @Input() active = false;
  TRACE_INFO = TRACE_INFO;
  TraceType = TraceType;
}

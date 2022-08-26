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
import { Rectangle, RectMatrix, RectTransform, UiData } from "viewers/viewer_surface_flinger/ui_data";
import { TraceType } from "common/trace/trace_type";

type NotifyViewCallbackType = (uiData: UiData) => void;

class Presenter {
  constructor(notifyViewCallback: NotifyViewCallbackType) {
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData("Initial UI data");
    this.notifyViewCallback(this.uiData);
  }

  updateHighlightedRect(event: CustomEvent) {
    this.highlighted = event.detail.layerId;
    this.uiData.highlighted = this.highlighted;
    console.log("changed highlighted rect: ", this.uiData.highlighted);
    this.notifyViewCallback(this.uiData);
  }

  notifyCurrentTraceEntries(entries: Map<TraceType, any>) {
    const entry = entries.get(TraceType.SURFACE_FLINGER);
    this.uiData = new UiData("New surface flinger ui data");
    const displayRects = entry.displays.map((display: any) => {
      const rect = display.layerStackSpace;
      rect.label = display.name;
      rect.id = display.id;
      rect.displayId = display.layerStackId;
      rect.isDisplay = true;
      rect.isVirtual = display.isVirtual;
      return rect;
    }) ?? [];
    this.uiData.highlighted = this.highlighted;

    this.displayIds = [];
    const rects = entry.visibleLayers
      .sort((a: any, b: any) => (b.absoluteZ > a.absoluteZ) ? 1 : (a.absoluteZ == b.absoluteZ) ? 0 : -1)
      .map((it: any) => {
        const rect = it.rect;
        rect.displayId = it.stackId;
        if (!this.displayIds.includes(it.stackId)) {
          this.displayIds.push(it.stackId);
        }
        return rect;
      });
    this.uiData.rects = this.rectsToUiData(rects.concat(displayRects));
    this.uiData.displayIds = this.displayIds;
    this.notifyViewCallback(this.uiData);
  }

  rectsToUiData(rects: any[]): Rectangle[] {
    const uiRects: Rectangle[] = [];
    rects.forEach((rect: any) => {
      let t = null;
      if (rect.transform && rect.transform.matrix) {
        t = rect.transform.matrix;
      } else if (rect.transform) {
        t = rect.transform;
      }
      let transform: RectTransform | null = null;
      if (t !== null) {
        const matrix: RectMatrix = {
          dsdx: t.dsdx,
          dsdy: t.dsdy,
          dtdx: t.dtdx,
          dtdy: t.dtdy,
          tx: t.tx,
          ty: -t.ty
        };
        transform = {
          matrix: matrix,
        };
      }

      let isVisible = false, isDisplay = false;
      if (rect.ref && rect.ref.isVisible) {
        isVisible = rect.ref.isVisible;
      }
      if (rect.isDisplay) {
        isDisplay = rect.isDisplay;
      }

      const newRect: Rectangle = {
        topLeft: {x: rect.left, y: rect.top},
        bottomRight: {x: rect.right, y: -rect.bottom},
        height: rect.height,
        width: rect.width,
        label: rect.label,
        transform: transform,
        isVisible: isVisible,
        isDisplay: isDisplay,
        ref: rect.ref,
        id: rect.id ?? rect.ref.id,
        displayId: rect.displayId ?? rect.ref.stackId,
        isVirtual: rect.isVirtual
      };
      uiRects.push(newRect);
    });
    return uiRects;
  }

  private readonly notifyViewCallback: NotifyViewCallbackType;
  private uiData: UiData;
  private highlighted = "";
  private displayIds: Array<number> = [];
}

export {Presenter};

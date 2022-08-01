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
import { Rectangle, RectMatrix, UiDataSurfaceFlinger } from "ui_data/ui_data_surface_flinger";
import { Presenter } from "./presenter";
import { UiDataCallbackType } from "./presenter";
import { TraceType } from "common/trace/trace_type";

class PresenterSurfaceFlinger extends Presenter {
  constructor(uiDataCallback: UiDataCallbackType) {
    super(uiDataCallback);
    this.uiDataCallback = uiDataCallback;
    this.uiData = new UiDataSurfaceFlinger("Initial UI data");
    this.uiDataCallback(this.uiData);
  }

  updateHighlightedRect(event: any) {
    this.highlighted = event.detail.layerId;
    this.uiData.highlighted = this.highlighted;
    console.log("changed highlighted rect: ", this.uiData.highlighted);
    this.uiDataCallback(this.uiData);
  }

  override notifyCurrentTraceEntries(entries: Map<TraceType, any>) {
    const entry = entries.get(TraceType.SURFACE_FLINGER);
    this.uiData = new UiDataSurfaceFlinger("New surface flinger ui data");
    this.uiData.rects = [];
    const displayRects = entry.displays.map((display: any) => {
      const rect = display.layerStackSpace;
      rect.label = display.name;
      rect.id = display.id;
      rect.stackId = display.layerStackId;
      rect.isDisplay = true;
      rect.isVirtual = display.isVirtual;
      return rect;
    });
    this.uiData.highlighted = this.highlighted;
    this.rectToUiData(entry.rects.concat(displayRects));
    this.uiDataCallback(this.uiData);
  }

  rectToUiData(rects: any[]) {
    rects.forEach((rect: any) => {
      let t = null;
      if (rect.transform && rect.transform.matrix) {
        t = rect.transform.matrix;
      } else if (rect.transform) {
        t = rect.transform;
      }
      let transform = null;
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
        stackId: rect.stackId ?? rect.ref.stackId,
        isVirtual: rect.isVirtual
      };
      this.uiData.rects?.push(newRect);
    });
  }

  override readonly uiDataCallback: UiDataCallbackType;
  override uiData: UiDataSurfaceFlinger;
  private highlighted = "";
}

export {PresenterSurfaceFlinger};

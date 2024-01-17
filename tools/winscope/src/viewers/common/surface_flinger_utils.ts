/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {TransformMatrix} from 'common/geometry_types';
import {Layer, LayerTraceEntry} from 'flickerlib/common';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';
import {UserOptions} from './user_options';

export class SurfaceFlingerUtils {
  static makeRects(
    entry: LayerTraceEntry,
    viewCapturePackageNames: string[],
    hierarchyUserOptions: UserOptions
  ): UiRect[] {
    const layerRects = SurfaceFlingerUtils.makeLayerRects(
      entry,
      viewCapturePackageNames,
      hierarchyUserOptions
    );
    const displayRects = SurfaceFlingerUtils.makeDisplayRects(entry);
    return layerRects.concat(displayRects);
  }

  private static makeLayerRects(
    entry: LayerTraceEntry,
    viewCapturePackageNames: string[],
    hierarchyUserOptions: UserOptions
  ): UiRect[] {
    return entry.flattenedLayers
      .filter((layer: Layer) => {
        return SurfaceFlingerUtils.isLayerToRenderInRectsComponent(layer, hierarchyUserOptions);
      })
      .sort(SurfaceFlingerUtils.compareLayerZ)
      .map((it: Layer) => {
        const transform: TransformMatrix = it.rect.transform?.matrix ?? it.rect.transform;
        return new UiRectBuilder()
          .setX(it.rect.left)
          .setY(it.rect.top)
          .setWidth(it.rect.right - it.rect.left)
          .setHeight(it.rect.bottom - it.rect.top)
          .setLabel(it.rect.label)
          .setTransform(transform)
          .setIsVisible(it.isVisible)
          .setIsDisplay(false)
          .setId(it.stableId)
          .setDisplayId(it.stackId)
          .setIsVirtual(false)
          .setIsClickable(true)
          .setCornerRadius(it.cornerRadius)
          .setHasContent(
            viewCapturePackageNames.includes(it.rect.label.substring(0, it.rect.label.indexOf('/')))
          )
          .build();
      });
  }

  private static makeDisplayRects(entry: LayerTraceEntry): UiRect[] {
    if (!entry.displays) {
      return [];
    }

    return entry.displays?.map((display: any) => {
      const transform: TransformMatrix = display.transform?.matrix ?? display.transform;
      return new UiRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(display.size.width)
        .setHeight(display.size.height)
        .setLabel('Display')
        .setTransform(transform)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setId(`Display - ${display.id}`)
        .setDisplayId(display.layerStackId)
        .setIsVirtual(display.isVirtual ?? false)
        .setIsClickable(false)
        .setCornerRadius(0)
        .setHasContent(false)
        .build();
    });
  }

  private static isLayerToRenderInRectsComponent(
    layer: Layer,
    hierarchyUserOptions: UserOptions
  ): boolean {
    const onlyVisible = hierarchyUserOptions['onlyVisible']?.enabled ?? false;
    // Show only visible layers or Visible + Occluded layers. Don't show all layers
    // (flattenedLayers) because container layers are never meant to be displayed
    return layer.isVisible || (!onlyVisible && layer.occludedBy.length > 0);
  }

  static compareLayerZ(a: Layer, b: Layer): number {
    const zipLength = Math.min(a.zOrderPath.length, b.zOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = a.zOrderPath[i];
      const zOrderB = b.zOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    // When z-order is the same, the layer with larger ID is on top
    return a.id > b.id ? -1 : 1;
  }
}

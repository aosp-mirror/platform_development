/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {Canvas, colorToCss} from 'viewers/components/rects/canvas';

export interface RectSpec {
  type: TraceRectType;
  icon: string;
  multiple: boolean;
  legend: RectLegendOption[];
}

export enum TraceRectType {
  LAYERS = 'layers',
  INPUT_WINDOWS = 'input windows',
  VIEWS = 'views',
  WINDOW_STATES = 'window states',
}

export interface RectLegendOption {
  fill?: string;
  border: string;
  desc: string;
  showInWireFrameMode: boolean;
}

export class RectLegendFactory {
  private static readonly VISIBLE = colorToCss(Canvas.RECT_COLOR_VISIBLE);
  private static readonly NOT_VISIBLE = colorToCss(
    Canvas.RECT_COLOR_NOT_VISIBLE,
  );
  private static readonly SELECTED = 'var(--selected-element-color)';
  private static readonly HAS_CONTENT = colorToCss(
    Canvas.RECT_COLOR_HAS_CONTENT,
  );
  private static readonly DEFAULT_BORDER = 'var(--default-text-color)';
  private static readonly NOT_VISIBLE_OPTION = {
    fill: RectLegendFactory.NOT_VISIBLE,
    desc: 'Not visible',
    border: RectLegendFactory.DEFAULT_BORDER,
    showInWireFrameMode: false,
  };
  private static readonly SELECTED_OPTION = {
    fill: RectLegendFactory.SELECTED,
    desc: 'Selected',
    border: RectLegendFactory.DEFAULT_BORDER,
    showInWireFrameMode: true,
  };

  static makeLegendForLayerRects(withHierarchy: boolean): RectLegendOption[] {
    const legend: RectLegendOption[] = [
      RectLegendFactory.makeOptWithFill(RectLegendFactory.VISIBLE, 'Visible'),
      RectLegendFactory.NOT_VISIBLE_OPTION,
      RectLegendFactory.SELECTED_OPTION,
      RectLegendFactory.makeOptWithFill(
        RectLegendFactory.HAS_CONTENT,
        'Has view content',
      ),
    ];
    if (withHierarchy) {
      legend.push(...RectLegendFactory.makeLegendForPinnedRects());
    }
    return legend;
  }

  static makeLegendForInputWindowRects(
    withHierarchy: boolean,
  ): RectLegendOption[] {
    const legend: RectLegendOption[] = [
      RectLegendFactory.makeOptWithFill(
        RectLegendFactory.VISIBLE,
        'Visible and touchable',
      ),
      RectLegendFactory.NOT_VISIBLE_OPTION,
      {
        fill: '',
        border: RectLegendFactory.DEFAULT_BORDER,
        desc: 'Visible but not touchable',
        showInWireFrameMode: false,
      },
      RectLegendFactory.SELECTED_OPTION,
    ];
    if (withHierarchy) {
      legend.push(...RectLegendFactory.makeLegendForPinnedRects());
    } else {
      legend.push(
        RectLegendFactory.makeOptWithFill(
          RectLegendFactory.HAS_CONTENT,
          'Has input',
        ),
      );
    }
    return legend;
  }

  static makeLegendForWindowStateRects(): RectLegendOption[] {
    const legend: RectLegendOption[] = [
      RectLegendFactory.makeOptWithFill(RectLegendFactory.VISIBLE, 'Visible'),
      RectLegendFactory.NOT_VISIBLE_OPTION,
      RectLegendFactory.SELECTED_OPTION,
    ];
    legend.push(...RectLegendFactory.makeLegendForPinnedRects());
    return legend;
  }

  static makeLegendForViewRects(): RectLegendOption[] {
    const legend: RectLegendOption[] = [
      RectLegendFactory.makeOptWithFill(
        RectLegendFactory.HAS_CONTENT,
        'Visible',
      ),
      RectLegendFactory.NOT_VISIBLE_OPTION,
      RectLegendFactory.SELECTED_OPTION,
    ];
    legend.push(...RectLegendFactory.makeLegendForPinnedRects());
    return legend;
  }

  private static makeLegendForPinnedRects(): RectLegendOption[] {
    return [
      {
        border: colorToCss(Canvas.RECT_EDGE_COLOR_PINNED),
        desc: 'Pinned',
        showInWireFrameMode: true,
      },
      {
        border: colorToCss(Canvas.RECT_EDGE_COLOR_PINNED_ALT),
        desc: 'Pinned',
        showInWireFrameMode: true,
      },
    ];
  }

  private static makeOptWithFill(fill: string, desc: string): RectLegendOption {
    const border = RectLegendFactory.DEFAULT_BORDER;
    return {fill, desc, border, showInWireFrameMode: false};
  }
}

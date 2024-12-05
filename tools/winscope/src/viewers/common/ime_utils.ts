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
import {assertDefined} from 'common/assert_utils';
import {FilterFlag} from 'common/filter_flag';
import {Timestamp} from 'common/time';
import {Item} from 'trace/item';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WindowType} from 'trace/window_type';
import {TextFilter} from 'viewers/common/text_filter';
import {WmImeUtils} from 'viewers/common/wm_ime_utils';
import {TreeNodeFilter, UiTreeUtils} from './ui_tree_utils';

interface WmStateProperties {
  timestamp: string | undefined;
  focusedApp: string | undefined;
  focusedWindow: string | undefined;
  focusedActivity: string | undefined;
  isInputMethodWindowVisible: boolean;
  imeInputTarget: PropertyTreeNode | undefined;
  imeLayeringTarget: PropertyTreeNode | undefined;
  imeInsetsSourceProvider: PropertyTreeNode | undefined;
  imeControlTarget: PropertyTreeNode | undefined;
}

export class ProcessedWindowManagerState implements Item {
  constructor(
    readonly id: string,
    readonly name: string,
    readonly wmStateProperties: WmStateProperties,
    readonly hierarchyTree: HierarchyTreeNode,
  ) {}
}

export interface ImeContainerProperties {
  id: string;
  zOrderRelativeOfId: number;
  z: number;
}

export interface InputMethodSurfaceProperties {
  id: string;
  isVisible: boolean;
  screenBounds?: PropertyTreeNode;
  rect?: PropertyTreeNode;
}

interface RootImeProperties {
  timestamp: string;
}

interface ImeLayerProperties {
  imeContainer: ImeContainerProperties | undefined;
  inputMethodSurface: InputMethodSurfaceProperties | undefined;
  focusedWindowColor: PropertyTreeNode | undefined;
  root: RootImeProperties | undefined;
}

export class ImeLayers implements Item {
  constructor(
    readonly id: string,
    readonly name: string,
    readonly properties: ImeLayerProperties,
    readonly taskLayerOfImeContainer: HierarchyTreeNode | undefined,
    readonly taskLayerOfImeSnapshot: HierarchyTreeNode | undefined,
  ) {}
}

class ImeAdditionalPropertiesUtils {
  private isInputMethodSurface = UiTreeUtils.makeNodeFilter(
    new TextFilter('InputMethod').getFilterPredicate(),
  );
  private isImeContainer = UiTreeUtils.makeNodeFilter(
    new TextFilter('ImeContainer').getFilterPredicate(),
  );

  processWindowManagerTraceEntry(
    entry: HierarchyTreeNode,
    wmEntryTimestamp: Timestamp | undefined,
  ): ProcessedWindowManagerState {
    const displayContent = entry.getAllChildren()[0];

    const props: WmStateProperties = {
      timestamp: wmEntryTimestamp ? wmEntryTimestamp.format() : undefined,
      focusedApp: entry.getEagerPropertyByName('focusedApp')?.getValue(),
      focusedWindow: this.getFocusedWindowString(entry),
      focusedActivity: this.getFocusedActivityString(entry),
      isInputMethodWindowVisible: this.isInputMethodVisible(displayContent),
      imeInputTarget: this.getImeInputTargetProperty(displayContent),
      imeLayeringTarget: this.getImeLayeringTargetProperty(displayContent),
      imeInsetsSourceProvider: displayContent.getEagerPropertyByName(
        'imeInsetsSourceProvider',
      ),
      imeControlTarget: this.getImeControlTargetProperty(displayContent),
    };

    return new ProcessedWindowManagerState(entry.id, entry.name, props, entry);
  }

  getImeLayers(
    entryTree: HierarchyTreeNode,
    processedWindowManagerState: ProcessedWindowManagerState,
    sfEntryTimestamp: Timestamp | undefined,
  ): ImeLayers | undefined {
    const imeContainerLayer = entryTree.findDfs(this.isImeContainer);

    if (!imeContainerLayer) {
      return undefined;
    }

    const imeContainerProps: ImeContainerProperties = {
      id: imeContainerLayer.id,
      zOrderRelativeOfId: assertDefined(
        imeContainerLayer.getEagerPropertyByName('zOrderRelativeOf'),
      ).getValue(),
      z: assertDefined(
        imeContainerLayer.getEagerPropertyByName('z'),
      ).getValue(),
    };

    const inputMethodSurfaceLayer = imeContainerLayer.findDfs(
      this.isInputMethodSurface,
    );

    if (!inputMethodSurfaceLayer) {
      return undefined;
    }

    const inputMethodSurfaceProps: InputMethodSurfaceProperties = {
      id: inputMethodSurfaceLayer.id,
      isVisible: assertDefined(
        inputMethodSurfaceLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
      screenBounds:
        inputMethodSurfaceLayer.getEagerPropertyByName('screenBounds'),
      rect: inputMethodSurfaceLayer.getEagerPropertyByName('bounds'),
    };

    let focusedWindowLayer: HierarchyTreeNode | undefined;
    const focusedWindowToken =
      processedWindowManagerState.wmStateProperties.focusedWindow
        ?.split(' ')[0]
        .slice(1);
    if (focusedWindowToken) {
      const isFocusedWindow = UiTreeUtils.makeNodeFilter(
        new TextFilter(focusedWindowToken).getFilterPredicate(),
      );
      focusedWindowLayer = entryTree.findDfs(isFocusedWindow);
    }

    const focusedWindowColor = focusedWindowLayer
      ? focusedWindowLayer.getEagerPropertyByName('color')
      : undefined;

    // we want to see both ImeContainer and IME-snapshot if there are
    // cases where both exist
    const taskLayerOfImeContainer = this.findAncestorTaskLayerOfImeLayer(
      entryTree,
      this.isImeContainer,
    );

    const taskLayerOfImeSnapshot = this.findAncestorTaskLayerOfImeLayer(
      entryTree,
      UiTreeUtils.makeNodeFilter(
        new TextFilter('IME-snapshot').getFilterPredicate(),
      ),
    );

    const rootProperties = sfEntryTimestamp
      ? {timestamp: sfEntryTimestamp.format()}
      : undefined;

    return new ImeLayers(
      entryTree.id,
      entryTree.name,
      {
        imeContainer: imeContainerProps,
        inputMethodSurface: inputMethodSurfaceProps,
        focusedWindowColor,
        root: rootProperties,
      },
      taskLayerOfImeContainer,
      taskLayerOfImeSnapshot,
    );
  }

  private getFocusedWindowString(entry: HierarchyTreeNode): string | undefined {
    let focusedWindowString = undefined;
    const focusedWindow = WmImeUtils.getFocusedWindow(entry);
    if (focusedWindow) {
      const token = assertDefined(
        focusedWindow.getEagerPropertyByName('token'),
      ).getValue();
      const windowTypeSuffix = this.getWindowTypeSuffix(
        assertDefined(
          focusedWindow.getEagerPropertyByName('windowType'),
        ).getValue(),
      );
      const type = assertDefined(
        focusedWindow
          .getEagerPropertyByName('attributes')
          ?.getChildByName('type'),
      ).formattedValue();
      const windowFrames = assertDefined(
        focusedWindow.getEagerPropertyByName('windowFrames'),
      );
      const containingFrame = assertDefined(
        windowFrames.getChildByName('containingFrame')?.formattedValue(),
      );
      const parentFrame = assertDefined(
        windowFrames.getChildByName('parentFrame')?.formattedValue(),
      );

      focusedWindowString = `{${token} ${focusedWindow.name}${windowTypeSuffix}} type=${type} cf=${containingFrame} pf=${parentFrame}`;
    }
    return focusedWindowString;
  }

  private getFocusedActivityString(entry: HierarchyTreeNode): string {
    let focusedActivityString = 'null';
    const focusedActivity = WmImeUtils.getFocusedActivity(entry);
    if (focusedActivity) {
      const token = assertDefined(
        focusedActivity.getEagerPropertyByName('token'),
      ).getValue();
      const state = assertDefined(
        focusedActivity.getEagerPropertyByName('state'),
      ).getValue();
      const isVisible =
        focusedActivity
          .getEagerPropertyByName('isComputedVisible')
          ?.getValue() ?? false;

      focusedActivityString = `{${token} ${focusedActivity.name}} state=${state} visible=${isVisible}`;
    }
    return focusedActivityString;
  }

  private getWindowTypeSuffix(windowType: number): string {
    switch (windowType) {
      case WindowType.STARTING:
        return ' STARTING';
      case WindowType.EXITING:
        return ' EXITING';
      case WindowType.DEBUGGER:
        return ' DEBUGGER';
      default:
        return '';
    }
  }

  private findAncestorTaskLayerOfImeLayer(
    entryTree: HierarchyTreeNode,
    isTargetImeLayer: TreeNodeFilter,
  ): HierarchyTreeNode | undefined {
    const imeLayer = entryTree.findDfs(isTargetImeLayer);

    if (!imeLayer) {
      return undefined;
    }

    const isTaskLayer = UiTreeUtils.makeNodeFilter(
      new TextFilter('Task|ImePlaceholder', [
        FilterFlag.USE_REGEX,
      ]).getFilterPredicate(),
    );
    const taskLayer = imeLayer.findAncestor(isTaskLayer);
    if (!taskLayer) {
      return undefined;
    }

    return taskLayer;
  }

  private getImeControlTargetProperty(
    displayContent: HierarchyTreeNode,
  ): PropertyTreeNode | undefined {
    return displayContent.getEagerPropertyByName('inputMethodControlTarget');
  }

  private getImeInputTargetProperty(
    displayContent: HierarchyTreeNode,
  ): PropertyTreeNode | undefined {
    return displayContent.getEagerPropertyByName('inputMethodInputTarget');
  }

  private getImeLayeringTargetProperty(
    displayContent: HierarchyTreeNode,
  ): PropertyTreeNode | undefined {
    return displayContent.getEagerPropertyByName('inputMethodTarget');
  }

  private isInputMethodVisible(displayContent: HierarchyTreeNode): boolean {
    const inputMethodWindowOrLayer = displayContent.findDfs(
      this.isInputMethodSurface,
    );
    return inputMethodWindowOrLayer
      ?.getEagerPropertyByName('isComputedVisible')
      ?.getValue();
  }
}

export const ImeUtils = new ImeAdditionalPropertiesUtils();

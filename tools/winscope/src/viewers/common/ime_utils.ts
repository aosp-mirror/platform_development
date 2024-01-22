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
import {Timestamp} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {TreeUtils} from 'common/tree_utils';
import {WindowContainer} from 'flickerlib/common';
import {Activity} from 'flickerlib/windows/Activity';
import {WindowManagerState} from 'flickerlib/windows/WindowManagerState';
import {WindowState} from 'flickerlib/windows/WindowState';
import {Item} from 'trace/item';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNodeFilter, UiTreeUtils} from './ui_tree_utils';

class ProcessedWindowManagerState {
  constructor(
    public name: string,
    public stableId: string,
    public focusedApp: string,
    public focusedWindow: WindowState,
    public focusedActivity: Activity,
    public isInputMethodWindowVisible: boolean,
    public protoImeControlTarget: any,
    public protoImeInputTarget: any,
    public protoImeLayeringTarget: any,
    public protoImeInsetsSourceProvider: any,
    public proto: any
  ) {}
}

interface ImeContainerProperties {
  id: string;
  zOrderRelativeOfId: number;
  z: number;
}

interface InputMethodSurfaceProperties {
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

class ImeLayers implements Item {
  constructor(
    public id: string,
    public name: string,
    public properties: ImeLayerProperties,
    public taskLayerOfImeContainer: HierarchyTreeNode | undefined,
    public taskLayerOfImeSnapshot: HierarchyTreeNode | undefined
  ) {}
}

class ImeUtils {
  static processWindowManagerTraceEntry(entry: WindowManagerState): ProcessedWindowManagerState {
    const displayContent = entry.root.children[0];

    return new ProcessedWindowManagerState(
      entry.name,
      entry.stableId,
      entry.focusedApp,
      entry.focusedWindow,
      entry.focusedActivity,
      ImeUtils.isInputMethodVisible(displayContent),
      ImeUtils.getImeControlTargetProperty(displayContent.proto),
      ImeUtils.getImeInputTargetProperty(displayContent.proto),
      ImeUtils.getImeLayeringTargetProperty(displayContent.proto),
      displayContent.proto.imeInsetsSourceProvider,
      entry.proto
    );
  }

  static getImeLayers(
    entryTree: HierarchyTreeNode,
    processedWindowManagerState: ProcessedWindowManagerState,
    sfEntryTimestamp: Timestamp | undefined
  ): ImeLayers | undefined {
    const isImeContainer = UiTreeUtils.makeNodeFilter('ImeContainer');
    const imeContainerLayer = entryTree.findDfs(isImeContainer);

    if (!imeContainerLayer) {
      return undefined;
    }

    const imeContainerProps: ImeContainerProperties = {
      id: imeContainerLayer.id,
      zOrderRelativeOfId: assertDefined(
        imeContainerLayer.getEagerPropertyByName('zOrderRelativeOf')
      ).getValue(),
      z: assertDefined(imeContainerLayer.getEagerPropertyByName('z')).getValue(),
    };

    const isInputMethodSurface = UiTreeUtils.makeNodeFilter('InputMethod');
    const inputMethodSurfaceLayer = imeContainerLayer.findDfs(isInputMethodSurface);

    if (!inputMethodSurfaceLayer) {
      return undefined;
    }

    const inputMethodSurfaceProps: InputMethodSurfaceProperties = {
      id: inputMethodSurfaceLayer.id,
      isVisible: assertDefined(
        inputMethodSurfaceLayer.getEagerPropertyByName('isComputedVisible')
      ).getValue(),
      screenBounds: inputMethodSurfaceLayer.getEagerPropertyByName('screenBounds'),
      rect: inputMethodSurfaceLayer.getEagerPropertyByName('bounds'),
    };

    let focusedWindowLayer: HierarchyTreeNode | undefined;
    const focusedWindowToken = processedWindowManagerState.focusedWindow?.token;
    if (focusedWindowToken) {
      const isFocusedWindow = UiTreeUtils.makeNodeFilter(focusedWindowToken);
      focusedWindowLayer = entryTree.findDfs(isFocusedWindow);
    }

    const focusedWindowColor = focusedWindowLayer
      ? focusedWindowLayer.getEagerPropertyByName('color')
      : undefined;

    // we want to see both ImeContainer and IME-snapshot if there are
    // cases where both exist
    const taskLayerOfImeContainer = ImeUtils.findAncestorTaskLayerOfImeLayer(
      entryTree,
      UiTreeUtils.makeNodeFilter('ImeContainer')
    );

    const taskLayerOfImeSnapshot = ImeUtils.findAncestorTaskLayerOfImeLayer(
      entryTree,
      UiTreeUtils.makeNodeFilter('IME-snapshot')
    );

    const rootProperties = sfEntryTimestamp
      ? {timestamp: TimeUtils.format(sfEntryTimestamp)}
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
      taskLayerOfImeSnapshot
    );
  }

  static transformInputConnectionCall(entry: any) {
    const obj = Object.assign({}, entry);
    if (obj.inputConnectionCall) {
      Object.getOwnPropertyNames(obj.inputConnectionCall).forEach((name) => {
        const value = Object.getOwnPropertyDescriptor(obj.inputConnectionCall, name);
        if (!value?.value) delete obj.inputConnectionCall[name];
      });
    }
    return obj;
  }

  private static findAncestorTaskLayerOfImeLayer(
    entryTree: HierarchyTreeNode,
    isTargetImeLayer: TreeNodeFilter
  ): HierarchyTreeNode | undefined {
    const imeLayer = entryTree.findDfs(isTargetImeLayer);

    if (!imeLayer) {
      return undefined;
    }

    const isTaskLayer = UiTreeUtils.makeNodeFilter('Task|ImePlaceholder');
    const taskLayer = imeLayer.findAncestor(isTaskLayer);
    if (!taskLayer) {
      return undefined;
    }

    return taskLayer;
  }

  private static getImeControlTargetProperty(displayContentProto: any): any {
    const POSSIBLE_NAMES = ['inputMethodControlTarget', 'imeControlTarget'];
    return ImeUtils.findAnyPropertyWithMatchingName(displayContentProto, POSSIBLE_NAMES);
  }

  private static getImeInputTargetProperty(displayContentProto: any): any {
    const POSSIBLE_NAMES = ['inputMethodInputTarget', 'imeInputTarget'];
    return ImeUtils.findAnyPropertyWithMatchingName(displayContentProto, POSSIBLE_NAMES);
  }

  private static getImeLayeringTargetProperty(displayContentProto: any): any {
    const POSSIBLE_NAMES = ['inputMethodTarget', 'imeLayeringTarget'];
    return ImeUtils.findAnyPropertyWithMatchingName(displayContentProto, POSSIBLE_NAMES);
  }

  private static findAnyPropertyWithMatchingName(object: any, possible_names: string[]): any {
    const key = Object.keys(object).find((key) => possible_names.includes(key));
    return key ? object[key] : undefined;
  }

  private static isInputMethodVisible(displayContent: WindowContainer): boolean {
    const isInputMethod = TreeUtils.makeNodeFilter('InputMethod');
    const inputMethodWindowOrLayer = TreeUtils.findDescendantNode(
      displayContent,
      isInputMethod
    ) as WindowContainer;
    return inputMethodWindowOrLayer?.isVisible === true;
  }
}

export {ImeUtils, ProcessedWindowManagerState, ImeLayers};

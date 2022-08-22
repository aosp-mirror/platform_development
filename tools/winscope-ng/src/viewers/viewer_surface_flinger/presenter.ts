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
import { UserOptions } from "viewers/common/user_options";
import { getFilter, FilterType, Tree, TreeSummary } from "viewers/common/tree_utils";
import { TreeGenerator } from "viewers/common/tree_generator";
import { TreeTransformer } from "viewers/common/tree_transformer";

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  constructor(notifyViewCallback: NotifyViewCallbackType) {
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData();
    this.notifyViewCallback(this.uiData);
  }

  public updatePinnedItems(pinnedItem: Tree) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map(item => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter(pinned => `${pinned.id}` != pinnedId);
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData.pinnedItems = this.pinnedItems;
    this.notifyViewCallback(this.uiData);
  }

  public updateHighlightedItems(id: string) {
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter(hl => hl != id);
    } else {
      this.highlightedItems = []; //if multi-select implemented, remove this line
      this.highlightedItems.push(id);
    }
    this.uiData.highlightedItems = this.highlightedItems;
    this.notifyViewCallback(this.uiData);
  }

  public updateHierarchyTree(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public filterHierarchyTree(filterString: string) {
    this.hierarchyFilter = getFilter(filterString);
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public updatePropertiesTree(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.updateSelectedTreeUiData();
  }

  public filterPropertiesTree(filterString: string) {
    this.propertiesFilter = getFilter(filterString);
    this.updateSelectedTreeUiData();
  }

  public newPropertiesTree(selectedItem: any) {
    this.selectedTree = selectedItem;
    this.updateSelectedTreeUiData();
  }

  private updateSelectedTreeUiData() {
    this.uiData.selectedTree = this.getTreeWithTransformedProperties(this.selectedTree);
    this.uiData.selectedTreeSummary = this.getSelectedTreeSummary(this.selectedTree);
    this.notifyViewCallback(this.uiData);
  }

  private getSelectedTreeSummary(layer: Tree): TreeSummary | undefined {
    const summary = [];

    if (layer?.visibilityReason?.length > 0) {
      let reason = "";
      if (Array.isArray(layer.visibilityReason)) {
        reason = layer.visibilityReason.join(", ");
      } else {
        reason = layer.visibilityReason;
      }

      summary.push({key: "Invisible due to", value: reason});
    }

    if (layer?.occludedBy?.length > 0) {
      summary.push({key: "Occluded by", value: layer.occludedBy.map((it:Tree) => it.id).join(", ")});
    }

    if (layer?.partiallyOccludedBy?.length > 0) {
      summary.push({
        key: "Partially occluded by",
        value: layer.partiallyOccludedBy.map((it:Tree) => it.id).join(", "),
      });
    }

    if (layer?.coveredBy?.length > 0) {
      summary.push({key: "Covered by", value: layer.coveredBy.map((it:Tree) => it.id).join(", ")});
    }

    if (summary.length === 0) {
      return undefined;
    }

    return summary;
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, any>) {
    this.uiData = new UiData();
    const entry = entries.get(TraceType.SURFACE_FLINGER)[0];
    this.previousEntry = entries.get(TraceType.SURFACE_FLINGER)[1];

    this.uiData = new UiData();

    this.uiData.highlightedItems = this.highlightedItems;

    const displayRects = entry.displays.map((display: any) => {
      const rect = display.layerStackSpace;
      rect.label = display.name;
      rect.id = display.id;
      rect.displayId = display.layerStackId;
      rect.isDisplay = true;
      rect.isVirtual = display.isVirtual ?? false;
      return rect;
    }) ?? [];
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

    this.entry = entry;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.uiData.tree = this.generateTree();

    this.notifyViewCallback(this.uiData);
  }

  private generateTree() {
    if (!this.entry) {
      return null;
    }

    const generator = new TreeGenerator(this.entry, this.hierarchyFilter, this.pinnedIds)
      .setIsOnlyVisibleView(this.hierarchyUserOptions["onlyVisible"]?.enabled)
      .setIsSimplifyNames(this.hierarchyUserOptions["simplifyNames"]?.enabled)
      .setIsFlatView(this.hierarchyUserOptions["flat"]?.enabled)
      .withUniqueNodeId();
    let tree: Tree;
    if (!this.hierarchyUserOptions["showDiff"]?.enabled) {
      tree = generator.generateTree();
    } else {
      tree = generator.compareWith(this.previousEntry)
        .withModifiedCheck()
        .generateFinalDiffTree();
    }
    this.pinnedItems = generator.getPinnedItems();
    this.uiData.pinnedItems = this.pinnedItems;
    return tree;
  }

  private rectsToUiData(rects: any[]): Rectangle[] {
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

      const newRect: Rectangle = {
        topLeft: {x: rect.left, y: rect.top},
        bottomRight: {x: rect.right, y: -rect.bottom},
        height: rect.height,
        width: rect.width,
        label: rect.label,
        transform: transform,
        isVisible: rect.ref?.isVisible ?? false,
        isDisplay: rect.isDisplay ?? false,
        ref: rect.ref,
        id: rect.id ?? rect.ref.id,
        displayId: rect.displayId ?? rect.ref.stackId,
        isVirtual: rect.isVirtual ?? false
      };
      uiRects.push(newRect);
    });
    return uiRects;
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter(pinned => pinned != newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private getTreeWithTransformedProperties(selectedTree: Tree) {
    const transformer = new TreeTransformer(selectedTree, this.propertiesFilter)
      .setIsShowDefaults(this.propertiesUserOptions["showDefaults"]?.enabled)
      .setIsShowDiff(this.propertiesUserOptions["showDiff"]?.enabled)
      .setTransformerOptions({skip: selectedTree.skip})
      .setDiffProperties(this.previousEntry);
    this.uiData.selectedLayer = transformer.getOriginalLayer(this.entry, selectedTree.stableId);
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  private readonly notifyViewCallback: NotifyViewCallbackType;
  private uiData: UiData;
  private hierarchyFilter: FilterType = getFilter("");
  private propertiesFilter: FilterType = getFilter("");
  private highlightedItems: Array<string> = [];
  private displayIds: Array<number> = [];
  private pinnedItems: Array<Tree> = [];
  private pinnedIds: Array<string> = [];
  private selectedTree: any = null;
  private previousEntry: any = null;
  private entry: any = null;
  private hierarchyUserOptions: UserOptions = {
    showDiff: {
      name: "Show diff",
      enabled: false
    },
    simplifyNames: {
      name: "Simplify names",
      enabled: true
    },
    onlyVisible: {
      name: "Only visible",
      enabled: false
    },
    flat: {
      name: "Flat",
      enabled: false
    }
  };

  private propertiesUserOptions: UserOptions = {
    showDiff: {
      name: "Show diff",
      enabled: false
    },
    showDefaults: {
      name: "Show defaults",
      enabled: true,
      tooltip: `
                If checked, shows the value of all properties.
                Otherwise, hides all properties whose value is
                the default for its data type.
              `
    },
  };
}

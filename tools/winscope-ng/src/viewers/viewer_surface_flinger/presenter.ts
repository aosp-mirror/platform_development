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
import { TreeGenerator, getFilter, FilterType, Tree } from "viewers/common/tree_utils";

type NotifyViewCallbackType = (uiData: UiData) => void;

class Presenter {
  constructor(notifyViewCallback: NotifyViewCallbackType) {
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData();
    this.notifyViewCallback(this.uiData);
  }

  public updatePinnedItems(event: CustomEvent) {
    const pinnedItem = event.detail.pinnedItem;
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

  public updateHighlightedItems(event: CustomEvent) {
    const id = `${event.detail.id}`;
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter(hl => hl != id);
    } else {
      this.highlightedItems = []; //if multi-select implemented, remove this line
      this.highlightedItems.push(id);
    }
    this.uiData.highlightedItems = this.highlightedItems;
    this.notifyViewCallback(this.uiData);
  }

  public updateHierarchyTree(event: CustomEvent) {
    this.hierarchyUserOptions = event.detail.userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public filterHierarchyTree(event: CustomEvent) {
    this.hierarchyFilter = getFilter(event.detail.filterString);
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, any>) {
    this.uiData = new UiData();
    const entry = entries.get(TraceType.SURFACE_FLINGER)[0];
    this.uiData.rects = [];
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
    this.uiData.highlightedItems = this.highlightedItems;
    this.uiData.rects = this.rectsToUiData(entry.rects.concat(displayRects));
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.previousEntry = entries.get(TraceType.SURFACE_FLINGER)[1];
    this.entry = entry;

    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  private generateTree() {
    if (!this.entry) {
      return null;
    }
    const generator = new TreeGenerator(this.entry, this.hierarchyUserOptions, this.hierarchyFilter, this.pinnedIds)
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

  private readonly notifyViewCallback: NotifyViewCallbackType;
  private uiData: UiData;
  private displayIds: Array<number> = [];
  private hierarchyFilter: FilterType = getFilter("");
  private highlightedItems: Array<string> = [];
  private pinnedItems: Array<Tree> = [];
  private pinnedIds: Array<string> = [];
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
}

export {Presenter};

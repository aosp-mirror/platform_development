/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {Rect} from 'common/rect';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DiffType} from 'viewers/common/diff_type';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiDataHierarchy} from './ui_data_hierarchy';

export abstract class AbstractHierarchyViewerPresenterTest {
  execute() {
    describe('AbstractHierarchyViewerPresenter', () => {
      let uiData: UiDataHierarchy;
      let presenter: AbstractHierarchyViewerPresenter;
      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(() => {
        presenter = this.createPresenter((newData) => {
          uiData = newData;
        });
      });

      it('is robust to empty trace', async () => {
        const notifyViewCallback = (newData: UiDataHierarchy) => {
          uiData = newData;
        };
        const presenter =
          this.createPresenterWithEmptyTrace(notifyViewCallback);

        const positionUpdateWithoutTraceEntry =
          TracePositionUpdate.fromTimestamp(
            TimestampConverterUtils.makeRealTimestamp(0n),
          );
        await presenter.onAppEvent(positionUpdateWithoutTraceEntry);

        expect(Object.keys(uiData.hierarchyUserOptions).length).toBeGreaterThan(
          0,
        );
        expect(
          Object.keys(uiData.propertiesUserOptions).length,
        ).toBeGreaterThan(0);
        expect(uiData.hierarchyTrees).toBeFalsy();
        if (this.shouldExecuteRectTests) {
          expect(
            Object.keys(assertDefined(uiData?.rectsUserOptions)).length,
          ).toBeGreaterThan(0);
        }
      });

      it('processes trace position updates', async () => {
        await assertDefined(presenter).onAppEvent(
          assertDefined(this.getPositionUpdate()),
        );

        expect(uiData.highlightedItem?.length).toEqual(0);
        expect(Object.keys(uiData.hierarchyUserOptions).length).toBeGreaterThan(
          0,
        );
        expect(
          Object.keys(uiData.propertiesUserOptions).length,
        ).toBeGreaterThan(0);
        assertDefined(uiData.hierarchyTrees).forEach((tree) => {
          expect(tree.getAllChildren().length > 0).toBeTrue();
        });
        if (this.shouldExecuteRectTests) {
          expect(
            Object.keys(assertDefined(uiData.rectsUserOptions)).length,
          ).toBeGreaterThan(0);
          expect(uiData.rectsToDraw?.length).toBeGreaterThan(0);
          expect(uiData.displays?.length).toBeGreaterThan(0);
        }
      });

      if (this.shouldExecuteShowDiffTests) {
        it('disables show diff and generates non-diff tree if no prev entry available', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());

          const hierarchyOpts = assertDefined(uiData.hierarchyUserOptions);
          expect(hierarchyOpts['showDiff'].isUnavailable).toBeTrue();

          const propertyOpts = assertDefined(uiData.propertiesUserOptions);
          expect(propertyOpts['showDiff'].isUnavailable).toBeTrue();

          assertDefined(uiData.hierarchyTrees).forEach((tree) => {
            expect(tree.getAllChildren().length > 0).toBeTrue();
          });
        });
      }

      it('updates pinned items', () => {
        expect(uiData.pinnedItems).toEqual([]);

        const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
          id: 'TestItem 4',
          name: 'FirstPinnedItem',
        });

        presenter.onPinnedItemChange(pinnedItem);
        expect(uiData.pinnedItems).toContain(pinnedItem);
      });

      it('updates highlighted property', () => {
        expect(uiData.highlightedProperty).toEqual('');
        const id = '4';
        presenter.onHighlightedPropertyChange(id);
        expect(uiData.highlightedProperty).toEqual(id);
      });

      it('filters hierarchy tree by visibility', async () => {
        const userOptions: UserOptions = {
          showOnlyVisible: {
            name: 'Show only',
            chip: VISIBLE_CHIP,
            enabled: false,
          },
          flat: {
            name: 'Flat',
            enabled: true,
          },
        };

        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHierarchyUserOptionsChange(userOptions);

        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.getExpectedChildrenBeforeVisibilityFilter(),
        );

        userOptions['showOnlyVisible'].enabled = true;
        await presenter.onHierarchyUserOptionsChange(userOptions);
        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.getExpectedChildrenAfterVisibilityFilter(),
        );
      });

      if (this.shouldExecuteFlatTreeTest) {
        it('flattens hierarchy tree', async () => {
          //change flat view to true
          const userOptions: UserOptions = {
            showDiff: {
              name: 'Show diff',
              enabled: false,
            },
            simplifyNames: {
              name: 'Simplify names',
              enabled: false,
            },
            showOnlyVisible: {
              name: 'Show only',
              chip: VISIBLE_CHIP,
              enabled: false,
            },
            flat: {
              name: 'Flat',
              enabled: true,
            },
          };

          await presenter.onAppEvent(this.getPositionUpdate());
          expect(this.getTotalHierarchyChildren(uiData)).toEqual(
            this.getExpectedChildrenBeforeFlatFilter!(),
          );

          await presenter.onHierarchyUserOptionsChange(userOptions);
          expect(uiData.hierarchyUserOptions).toEqual(userOptions);
          expect(this.getTotalHierarchyChildren(uiData)).toEqual(
            this.getExpectedChildrenAfterFlatFilter!(),
          );
          assertDefined(uiData.hierarchyTrees).forEach((tree) => {
            tree.getAllChildren().forEach((child) => {
              expect(child.getAllChildren().length).toEqual(0);
            });
          });
        });
      }

      if (this.shouldExecuteSimplifyNamesTest) {
        it('simplifies names in hierarchy tree', async () => {
          const longName = assertDefined(this.treeNodeLongName);
          const shortName = assertDefined(this.treeNodeShortName);
          //change flat view to true
          const userOptions: UserOptions = {
            showDiff: {
              name: 'Show diff',
              enabled: false,
            },
            simplifyNames: {
              name: 'Simplify names',
              enabled: false,
            },
            showOnlyVisible: {
              name: 'Show only',
              chip: VISIBLE_CHIP,
              enabled: false,
            },
            flat: {
              name: 'Flat',
              enabled: false,
            },
          };

          await presenter.onAppEvent(this.getPositionUpdate());
          let nodeWithLongName = assertDefined(
            assertDefined(uiData.hierarchyTrees)[0].findDfs(
              UiTreeUtils.makeIdFilter(longName),
            ),
          );
          expect(nodeWithLongName.getDisplayName()).toEqual(shortName);

          await presenter.onHierarchyUserOptionsChange(userOptions);
          expect(uiData.hierarchyUserOptions).toEqual(userOptions);
          nodeWithLongName = assertDefined(
            assertDefined(uiData.hierarchyTrees)[0].findDfs(
              UiTreeUtils.makeIdFilter(longName),
            ),
          );
          expect(longName).toContain(nodeWithLongName.getDisplayName());
        });
      }

      it('filters hierarchy tree by search string', async () => {
        const userOptions: UserOptions = {
          showDiff: {
            name: 'Show diff',
            enabled: false,
          },
          simplifyNames: {
            name: 'Simplify names',
            enabled: true,
          },
          showOnlyVisible: {
            name: 'Show only',
            chip: VISIBLE_CHIP,
            enabled: false,
          },
          flat: {
            name: 'Flat',
            enabled: true,
          },
        };
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHierarchyUserOptionsChange(userOptions);
        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.getExpectedHierarchyChildrenBeforeStringFilter(),
        );

        await presenter.onHierarchyFilterChange(this.hierarchyFilterString);
        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.expectedHierarchyChildrenAfterStringFilter,
        );
      });

      it('sets properties tree and associated ui data from tree node', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());

        const selectedTree = this.getSelectedTree();
        await presenter.onHighlightedNodeChange(selectedTree);
        const propertiesTree = assertDefined(uiData.propertiesTree);
        expect(propertiesTree.id).toContain(selectedTree.id);
        expect(propertiesTree.getAllChildren().length).toEqual(
          this.numberOfNonDefaultProperties,
        );
        if (this.executeSpecializedChecksForPropertiesFromNode) {
          this.executeSpecializedChecksForPropertiesFromNode(uiData);
        }
      });

      it('after highlighting a node, updates properties tree on position update', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedNodeChange(
          this.getSelectedTreeAfterPositionUpdate(),
        );
        this.executeChecksForPropertiesTreeAfterPositionUpdate(uiData);

        const secondUpdate = this.getSecondPositionUpdate();
        if (secondUpdate) {
          await presenter.onAppEvent(secondUpdate);
          assertDefined(
            this.executeChecksForPropertiesTreeAfterSecondPositionUpdate,
          )(uiData);
        }
      });

      if (this.shouldExecuteShowDiffTests) {
        it('updates properties tree to show diffs', async () => {
          //change flat view to true
          const userOptions: UserOptions = {
            showDiff: {
              name: 'Show diff',
              enabled: true,
            },
          };

          await presenter.onAppEvent(this.getShowDiffPositionUpdate());
          await presenter.onHighlightedNodeChange(this.getSelectedTree());
          const propertyName = assertDefined(this.propertyWithDiff);
          expect(
            assertDefined(
              uiData.propertiesTree?.getChildByName(propertyName),
            ).getDiff(),
          ).toEqual(DiffType.NONE);

          await presenter.onPropertiesUserOptionsChange(userOptions);
          expect(uiData.propertiesUserOptions).toEqual(userOptions);
          expect(
            assertDefined(
              uiData.propertiesTree?.getChildByName(propertyName),
            ).getDiff(),
          ).toEqual(assertDefined(this.expectedPropertyDiffType));
        });
      }

      it('shows/hides defaults', async () => {
        const userOptions: UserOptions = {
          showDiff: {
            name: 'Show diff',
            enabled: true,
          },
          showDefaults: {
            name: 'Show defaults',
            enabled: true,
          },
        };

        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedNodeChange(this.getSelectedTree());
        expect(
          assertDefined(uiData.propertiesTree).getAllChildren().length,
        ).toEqual(this.numberOfNonDefaultProperties);

        await presenter.onPropertiesUserOptionsChange(userOptions);
        expect(uiData.propertiesUserOptions).toEqual(userOptions);
        expect(
          assertDefined(uiData.propertiesTree).getAllChildren().length,
        ).toEqual(
          this.numberOfNonDefaultProperties + this.numberOfDefaultProperties,
        );
      });

      it('filters properties tree', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedNodeChange(this.getSelectedTree());
        expect(
          assertDefined(uiData.propertiesTree).getAllChildren().length,
        ).toEqual(this.numberOfNonDefaultProperties);

        await presenter.onPropertiesFilterChange(this.propertiesFilterString);
        expect(
          assertDefined(uiData.propertiesTree).getAllChildren().length,
        ).toEqual(this.numberOfFilteredProperties);
      });

      if (this.shouldExecuteRectTests) {
        const totalRects = assertDefined(this.expectedTotalRects);
        const visibleRects = assertDefined(this.expectedVisibleRects);

        it('creates input data for rects view', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());
          const rectsToDraw = assertDefined(uiData.rectsToDraw);
          const expectedFirstRect = assertDefined(this.expectedFirstRect);
          expect(rectsToDraw[0].x).toEqual(expectedFirstRect.x);
          expect(rectsToDraw[0].y).toEqual(expectedFirstRect.y);
          expect(rectsToDraw[0].w).toEqual(expectedFirstRect.w);
          expect(rectsToDraw[0].h).toEqual(expectedFirstRect.h);
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects);
        });

        it('filters rects by visibility', async () => {
          const userOptions: UserOptions = {
            showOnlyVisible: {
              name: 'Show only',
              chip: VISIBLE_CHIP,
              enabled: false,
            },
          };

          await presenter.onAppEvent(this.getPositionUpdate());
          presenter.onRectsUserOptionsChange(userOptions);
          expect(uiData.rectsUserOptions).toEqual(userOptions);
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects);

          userOptions['showOnlyVisible'].enabled = true;
          presenter.onRectsUserOptionsChange(userOptions);
          this.checkRectUiData(uiData, visibleRects, totalRects, visibleRects);
        });

        it('filters rects by show/hide state', async () => {
          const userOptions: UserOptions = {
            ignoreNonHidden: {
              name: 'Ignore',
              icon: 'visibility',
              enabled: true,
            },
          };
          presenter.onRectsUserOptionsChange(userOptions);
          await presenter.onAppEvent(this.getPositionUpdate());
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects);

          await presenter.onRectShowStateChange(
            assertDefined(uiData.rectsToDraw)[0].id,
            RectShowState.HIDE,
          );
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects - 1);

          userOptions['ignoreNonHidden'].enabled = false;
          presenter.onRectsUserOptionsChange(userOptions);
          this.checkRectUiData(
            uiData,
            totalRects - 1,
            totalRects,
            totalRects - 1,
          );
        });

        it('handles both visibility and show/hide state in rects', async () => {
          const userOptions: UserOptions = {
            ignoreNonHidden: {
              name: 'Ignore',
              icon: 'visibility',
              enabled: true,
            },
            showOnlyVisible: {
              name: 'Show only',
              chip: VISIBLE_CHIP,
              enabled: false,
            },
          };
          presenter.onRectsUserOptionsChange(userOptions);
          await presenter.onAppEvent(this.getPositionUpdate());
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects);

          await presenter.onRectShowStateChange(
            assertDefined(uiData.rectsToDraw)[0].id,
            RectShowState.HIDE,
          );
          this.checkRectUiData(uiData, totalRects, totalRects, totalRects - 1);

          userOptions['ignoreNonHidden'].enabled = false;
          presenter.onRectsUserOptionsChange(userOptions);
          this.checkRectUiData(
            uiData,
            totalRects - 1,
            totalRects,
            totalRects - 1,
          );

          userOptions['showOnlyVisible'].enabled = true;
          presenter.onRectsUserOptionsChange(userOptions);
          this.checkRectUiData(
            uiData,
            visibleRects - 1,
            totalRects,
            visibleRects - 1,
          );

          userOptions['ignoreNonHidden'].enabled = true;
          presenter.onRectsUserOptionsChange(userOptions);
          this.checkRectUiData(
            uiData,
            visibleRects,
            totalRects,
            visibleRects - 1,
          );
        });

        it('sets properties tree and associated ui data from rect', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());

          const rect = assertDefined(uiData.rectsToDraw?.at(2));
          await presenter.onHighlightedIdChange(rect.id);
          const propertiesTree = assertDefined(uiData.propertiesTree);
          expect(propertiesTree.id).toEqual(rect.id);
          expect(propertiesTree.getAllChildren().length).toBeGreaterThan(0);

          if (this.executeSpecializedChecksForPropertiesFromRect) {
            this.executeSpecializedChecksForPropertiesFromRect(uiData);
          }
        });

        it('after highlighting a rect, updates properties tree on position update', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());
          await presenter.onHighlightedIdChange(
            this.getSelectedTreeAfterPositionUpdate().id,
          );
          this.executeChecksForPropertiesTreeAfterPositionUpdate(uiData);

          const secondUpdate = this.getSecondPositionUpdate();
          if (secondUpdate) {
            await presenter.onAppEvent(secondUpdate);
            assertDefined(
              this.executeChecksForPropertiesTreeAfterSecondPositionUpdate,
            )(uiData);
          }
        });
      }
    });

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  private getTotalHierarchyChildren(uiData: UiDataHierarchy) {
    const children = assertDefined(uiData.hierarchyTrees).reduce(
      (tot, tree) => (tot += tree.getAllChildren().length),
      0,
    );
    return children;
  }

  private checkRectUiData(
    uiData: UiDataHierarchy,
    rectsToDraw: number,
    allRects: number,
    shownRects: number,
  ) {
    expect(assertDefined(uiData.rectsToDraw).length).toEqual(rectsToDraw);
    const showStates = Array.from(
      assertDefined(uiData.rectIdToShowState).values(),
    );
    expect(showStates.length).toEqual(allRects);
    expect(showStates.filter((s) => s === RectShowState.SHOW).length).toEqual(
      shownRects,
    );
  }

  abstract readonly shouldExecuteFlatTreeTest: boolean;
  abstract readonly shouldExecuteRectTests: boolean;
  abstract readonly shouldExecuteShowDiffTests: boolean;
  abstract readonly shouldExecuteSimplifyNamesTest: boolean;
  abstract readonly numberOfDefaultProperties: number;
  abstract readonly numberOfNonDefaultProperties: number;
  abstract readonly propertiesFilterString: string;
  abstract readonly numberOfFilteredProperties: number;
  abstract readonly hierarchyFilterString: string;
  abstract readonly expectedHierarchyChildrenAfterStringFilter: number;

  readonly expectedFirstRect?: Rect;
  readonly expectedTotalRects?: number;
  readonly expectedVisibleRects?: number;
  readonly treeNodeLongName?: string;
  readonly treeNodeShortName?: string;
  readonly propertyWithDiff?: string;
  readonly expectedPropertyDiffType?: DiffType;

  abstract setUpTestEnvironment(): Promise<void>;
  abstract createPresenter(
    callback: NotifyHierarchyViewCallbackType,
  ): AbstractHierarchyViewerPresenter;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType,
  ): AbstractHierarchyViewerPresenter;
  abstract getPositionUpdate(): TracePositionUpdate;
  abstract getSecondPositionUpdate(): TracePositionUpdate | undefined;
  abstract getShowDiffPositionUpdate(): TracePositionUpdate;
  abstract getSelectedTree(): UiHierarchyTreeNode;
  abstract getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode;

  abstract getExpectedChildrenBeforeVisibilityFilter(): number;
  abstract getExpectedChildrenAfterVisibilityFilter(): number;
  abstract getExpectedHierarchyChildrenBeforeStringFilter(): number;
  abstract executeChecksForPropertiesTreeAfterPositionUpdate(
    uiData: UiDataHierarchy,
  ): void;

  getExpectedChildrenBeforeFlatFilter?(): number;
  getExpectedChildrenAfterFlatFilter?(): number;
  executeSpecializedChecksForPropertiesFromNode?(uiData: UiDataHierarchy): void;
  executeChecksForPropertiesTreeAfterSecondPositionUpdate?(
    uiData: UiDataHierarchy,
  ): void;
  executeSpecializedChecksForPropertiesFromRect?(uiData: UiDataHierarchy): void;
  executeSpecializedTests?(): void;
}

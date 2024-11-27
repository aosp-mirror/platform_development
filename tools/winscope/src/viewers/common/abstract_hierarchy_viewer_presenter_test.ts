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
import {Rect} from 'common/geometry/rect';
import {InMemoryStorage} from 'common/in_memory_storage';
import {Store} from 'common/store';
import {
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {TraceType} from 'trace/trace_type';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DiffType} from 'viewers/common/diff_type';
import {RectShowState} from 'viewers/common/rect_show_state';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiDataHierarchy} from './ui_data_hierarchy';
import {ViewerEvents} from './viewer_events';

export abstract class AbstractHierarchyViewerPresenterTest<
  UiData extends UiDataHierarchy,
> {
  execute() {
    describe('AbstractHierarchyViewerPresenter', () => {
      let uiData: UiDataHierarchy;
      let presenter: AbstractHierarchyViewerPresenter<UiData>;
      let userNotifierChecker: UserNotifierChecker;
      let storage: InMemoryStorage;

      beforeAll(async () => {
        jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
        userNotifierChecker = new UserNotifierChecker();
        await this.setUpTestEnvironment();
      });

      beforeEach(() => {
        storage = new InMemoryStorage();
        presenter = this.createPresenter((newData) => {
          uiData = newData;
        }, storage);
      });

      afterEach(() => {
        userNotifierChecker.expectNone();
        userNotifierChecker.reset();
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
        expect(uiData.hierarchyTrees).toBeUndefined();
        if (this.shouldExecuteRectTests) {
          expect(
            Object.keys(assertDefined(uiData?.rectsUserOptions)).length,
          ).toBeGreaterThan(0);
        }
      });

      it('clears ui data before throwing error on corrupted trace', async () => {
        const notifyViewCallback = (newData: UiDataHierarchy) => {
          uiData = newData;
        };
        const presenter =
          this.createPresenterWithCorruptedTrace(notifyViewCallback);

        try {
          await presenter.onAppEvent(
            TracePositionUpdate.fromTimestamp(
              TimestampConverterUtils.makeRealTimestamp(0n),
            ),
          );
        } catch (e) {
          expect(
            Object.keys(uiData.hierarchyUserOptions).length,
          ).toBeGreaterThan(0);
          expect(
            Object.keys(uiData.propertiesUserOptions).length,
          ).toBeGreaterThan(0);
          expect(uiData.hierarchyTrees).toBeUndefined();
          expect(uiData.propertiesTree).toBeUndefined();
          expect(uiData.highlightedItem).toEqual('');
          expect(uiData.highlightedProperty).toEqual('');
          expect(uiData.pinnedItems.length).toEqual(0);
          if (this.shouldExecuteRectTests) {
            expect(
              Object.keys(assertDefined(uiData?.rectsUserOptions)).length,
            ).toBeGreaterThan(0);
            expect(uiData.rectsToDraw).toEqual([]);
            expect(uiData.rectIdToShowState).toBeUndefined();
          }
        }
      });

      it('adds events listeners', () => {
        const element = document.createElement('div');
        presenter.addEventListeners(element);

        let spy: jasmine.Spy = spyOn(presenter, 'onPinnedItemChange');
        const node = TreeNodeUtils.makeUiHierarchyNode({name: 'test'});
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HierarchyPinnedChange, {
            detail: {pinnedItem: node},
          }),
        );
        expect(spy).toHaveBeenCalledWith(node);

        spy = spyOn(presenter, 'onHighlightedIdChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HighlightedIdChange, {
            detail: {id: 'test'},
          }),
        );
        expect(spy).toHaveBeenCalledWith('test');

        spy = spyOn(presenter, 'onHighlightedPropertyChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HighlightedPropertyChange, {
            detail: {id: 'test'},
          }),
        );
        expect(spy).toHaveBeenCalledWith('test');

        spy = spyOn(presenter, 'onHierarchyUserOptionsChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HierarchyUserOptionsChange, {
            detail: {userOptions: {}},
          }),
        );
        expect(spy).toHaveBeenCalledWith({});

        spy = spyOn(presenter, 'onHierarchyFilterChange');
        const filter = new TextFilter();
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HierarchyFilterChange, {detail: filter}),
        );
        expect(spy).toHaveBeenCalledWith(filter);

        spy = spyOn(presenter, 'onPropertiesUserOptionsChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.PropertiesUserOptionsChange, {
            detail: {userOptions: {}},
          }),
        );
        expect(spy).toHaveBeenCalledWith({});

        spy = spyOn(presenter, 'onPropertiesFilterChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.PropertiesFilterChange, {
            detail: filter,
          }),
        );
        expect(spy).toHaveBeenCalledWith(filter);

        spy = spyOn(presenter, 'onHighlightedNodeChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HighlightedNodeChange, {detail: {node}}),
        );
        expect(spy).toHaveBeenCalledWith(node);

        if (this.shouldExecuteRectTests) {
          spy = spyOn(presenter, 'onRectShowStateChange');
          element.dispatchEvent(
            new CustomEvent(ViewerEvents.RectShowStateChange, {
              detail: {rectId: 'test', state: RectShowState.HIDE},
            }),
          );
          expect(spy).toHaveBeenCalledWith('test', RectShowState.HIDE);

          spy = spyOn(presenter, 'onRectsUserOptionsChange');
          element.dispatchEvent(
            new CustomEvent(ViewerEvents.RectsUserOptionsChange, {
              detail: {userOptions: {}},
            }),
          );
          expect(spy).toHaveBeenCalledWith({});
        }
      });

      it('processes trace position updates', async () => {
        pinNode(this.getSelectedTree());

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
        expect(uiData.pinnedItems.length).toBeGreaterThan(0);

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

      if (this.shouldExecuteDumpTests) {
        it('shows correct hierarchy tree name for entry', async () => {
          const update = this.getPositionUpdate();
          const spy = spyOn(
            assertDefined(update.position.entry?.getFullTrace()),
            'isDumpWithoutTimestamp',
          );

          spy.and.returnValue(false);
          await presenter.onAppEvent(update);
          const entryNode = assertDefined(uiData.hierarchyTrees?.at(0));
          expect(entryNode.getDisplayName()).toContain(
            update.position.timestamp.format(),
          );

          pinNode(entryNode);

          spy.and.returnValue(true);
          await presenter.onAppEvent(update);
          const newEntryNode = assertDefined(uiData.hierarchyTrees?.at(0));
          expect(newEntryNode.getDisplayName()).toContain('Dump');
          expect(uiData.pinnedItems).toEqual([newEntryNode]);
        });
      }

      it('handles pinned item change', () => {
        expect(uiData.pinnedItems).toEqual([]);

        const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
          id: 'TestItem 4',
          name: 'FirstPinnedItem',
        });

        presenter.onPinnedItemChange(pinnedItem);
        expect(uiData.pinnedItems).toEqual([pinnedItem]);

        presenter.onPinnedItemChange(pinnedItem);
        expect(uiData.pinnedItems).toEqual([]);
      });

      it('updates highlighted property', () => {
        expect(uiData.highlightedProperty).toEqual('');
        const id = '4';
        presenter.onHighlightedPropertyChange(id);
        expect(uiData.highlightedProperty).toEqual(id);
        presenter.onHighlightedPropertyChange(id);
        expect(uiData.highlightedProperty).toEqual('');
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

        const nonVisibleNode = assertDefined(
          uiData.hierarchyTrees
            ?.at(0)
            ?.findDfs(
              (node) =>
                !node.isRoot() &&
                !node.getEagerPropertyByName('isComputedVisible')?.getValue(),
            ),
        );
        pinNode(nonVisibleNode);
        expect(uiData.pinnedItems).toEqual([nonVisibleNode]);

        userOptions['showOnlyVisible'].enabled = true;
        await presenter.onHierarchyUserOptionsChange(userOptions);
        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.getExpectedChildrenAfterVisibilityFilter(),
        );
        expect(uiData.pinnedItems).toEqual([nonVisibleNode]);
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
          const longNameFilter = UiTreeUtils.makeNodeFilter(
            new TextFilter(longName).getFilterPredicate(),
          );
          let nodeWithLongName = assertDefined(
            assertDefined(uiData.hierarchyTrees)[0].findDfs(longNameFilter),
          );
          expect(nodeWithLongName.getDisplayName()).toEqual(shortName);
          pinNode(nodeWithLongName);
          expect(uiData.pinnedItems).toEqual([nodeWithLongName]);

          await presenter.onHierarchyUserOptionsChange(userOptions);
          expect(uiData.hierarchyUserOptions).toEqual(userOptions);
          nodeWithLongName = assertDefined(
            assertDefined(uiData.hierarchyTrees)[0].findDfs(longNameFilter),
          );
          expect(longName).toContain(nodeWithLongName.getDisplayName());
          expect(uiData.pinnedItems).toEqual([nodeWithLongName]);
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

        const nonMatchNode = assertDefined(
          uiData.hierarchyTrees
            ?.at(0)
            ?.findDfs(
              (node) =>
                !node.isRoot() &&
                !node.id.includes(this.hierarchyFilter.filterString),
            ),
        );
        pinNode(nonMatchNode);
        expect(uiData.pinnedItems).toEqual([nonMatchNode]);

        await presenter.onHierarchyFilterChange(this.hierarchyFilter);
        expect(this.getTotalHierarchyChildren(uiData)).toEqual(
          this.expectedHierarchyChildrenAfterStringFilter,
        );
        expect(uiData.pinnedItems).toEqual([nonMatchNode]);
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
        const selectedTree = this.getSelectedTreeAfterPositionUpdate();
        await presenter.onHighlightedNodeChange(selectedTree);
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

        await presenter.onPropertiesFilterChange(this.propertiesFilter);
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
            ignoreRectShowState: {
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

          userOptions['ignoreRectShowState'].enabled = false;
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
            ignoreRectShowState: {
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

          userOptions['ignoreRectShowState'].enabled = false;
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

          userOptions['ignoreRectShowState'].enabled = true;
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
          expect(uiData.highlightedItem).toEqual(rect.id);
          const propertiesTree = assertDefined(uiData.propertiesTree);
          expect(propertiesTree.id).toEqual(rect.id);
          expect(propertiesTree.getAllChildren().length).toBeGreaterThan(0);

          if (this.executeSpecializedChecksForPropertiesFromRect) {
            this.executeSpecializedChecksForPropertiesFromRect(uiData);
          }

          await presenter.onHighlightedIdChange(rect.id);
          expect(uiData.highlightedItem).toEqual('');
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
      } else {
        it('is robust to attempts to change rect user data', async () => {
          expect(() =>
            presenter.onRectsUserOptionsChange({}),
          ).not.toThrowError();
          await expectAsync(
            presenter.onRectShowStateChange('', RectShowState.SHOW),
          ).not.toBeRejected();
        });
      }

      it('handles filter preset requests', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        const saveEvent = new FilterPresetSaveRequest(
          'TestPreset',
          TraceType.TEST_TRACE_STRING,
        );
        expect(storage.get(saveEvent.name)).toBeUndefined();
        await presenter.onAppEvent(saveEvent);
        expect(storage.get(saveEvent.name)).toBeDefined();

        await presenter.onHierarchyFilterChange(new TextFilter('Test Filter'));
        await presenter.onHierarchyUserOptionsChange({});
        await presenter.onPropertiesUserOptionsChange({});
        await presenter.onPropertiesFilterChange(new TextFilter('Test Filter'));

        if (this.shouldExecuteRectTests) {
          presenter.onRectsUserOptionsChange({});
          await presenter.onRectShowStateChange(
            assertDefined(uiData.rectsToDraw)[0].id,
            RectShowState.HIDE,
          );
        }
        const currentUiData = uiData;

        const applyEvent = new FilterPresetApplyRequest(
          saveEvent.name,
          TraceType.TEST_TRACE_STRING,
        );
        await presenter.onAppEvent(applyEvent);
        expect(uiData).not.toEqual(currentUiData);
      });

      function pinNode(node: UiHierarchyTreeNode) {
        presenter.onPinnedItemChange(node);
        expect(uiData.pinnedItems).toEqual([node]);
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
  abstract readonly shouldExecuteDumpTests: boolean;
  abstract readonly shouldExecuteSimplifyNamesTest: boolean;
  abstract readonly numberOfDefaultProperties: number;
  abstract readonly numberOfNonDefaultProperties: number;
  abstract readonly propertiesFilter: TextFilter;
  abstract readonly numberOfFilteredProperties: number;
  abstract readonly hierarchyFilter: TextFilter;
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
    callback: NotifyHierarchyViewCallbackType<UiData>,
    storage: Store,
  ): AbstractHierarchyViewerPresenter<UiData>;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): AbstractHierarchyViewerPresenter<UiData>;
  abstract createPresenterWithCorruptedTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): AbstractHierarchyViewerPresenter<UiData>;
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

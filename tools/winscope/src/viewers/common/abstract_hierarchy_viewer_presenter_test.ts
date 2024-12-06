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
import {InMemoryStorage} from 'common/in_memory_storage';
import {Store} from 'common/store';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Chip} from './chip';
import {UiDataHierarchy} from './ui_data_hierarchy';

export abstract class AbstractHierarchyViewerPresenterTest<
  UiData extends UiDataHierarchy,
> {
  execute() {
    describe('Common tests', () => {
      let uiData: UiDataHierarchy;
      let presenter: AbstractHierarchyViewerPresenter<UiData>;
      let userNotifierChecker: UserNotifierChecker;
      let storage: InMemoryStorage;

      beforeAll(async () => {
        jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
        jasmine.addCustomEqualityTester(chipEqualityTester);
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

      it('has expected user options', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.hierarchyUserOptions).toEqual(this.expectedHierarchyOpts);
        expect(uiData.propertiesUserOptions).toEqual(
          this.expectedPropertiesOpts,
        );
        expect(uiData.rectsUserOptions).toEqual(this.expectedRectsOpts);
      });

      it('after highlighting a node, updates properties on position update', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        const selectedTree = this.getSelectedTreeAfterPositionUpdate();
        await presenter.onHighlightedNodeChange(selectedTree);
        this.executePropertiesChecksAfterPositionUpdate(uiData);

        const secondUpdate = this.getSecondPositionUpdate();
        if (secondUpdate) {
          await presenter.onAppEvent(secondUpdate);
          assertDefined(this.executePropertiesChecksAfterSecondPositionUpdate)(
            uiData,
          );
        }
      });

      it('after highlighting by id, updates properties tree on position update', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedIdChange(
          this.getSelectedTreeAfterPositionUpdate().id,
        );
        this.executePropertiesChecksAfterPositionUpdate(uiData);

        const secondUpdate = this.getSecondPositionUpdate();
        if (secondUpdate) {
          await presenter.onAppEvent(secondUpdate);
          assertDefined(this.executePropertiesChecksAfterSecondPositionUpdate)(
            uiData,
          );
        }
      });

      it('correctly keeps/discards calculated properties', async () => {
        await presenter.onPropertiesUserOptionsChange({
          showDefaults: {name: '', enabled: true},
        });
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedIdChange(
          assertDefined(uiData.hierarchyTrees)[0].id,
        );
        const calculatedPropertyInRoot = uiData.propertiesTree?.findDfs(
          (node) => node.source === PropertySource.CALCULATED,
        );
        expect(calculatedPropertyInRoot !== undefined).toEqual(
          this.keepCalculatedPropertiesInRoot,
        );

        await presenter.onHighlightedIdChange(
          this.getSelectedTreeAfterPositionUpdate().id,
        );
        const calculatedPropertyInChild = uiData.propertiesTree?.findDfs(
          (node) => node.source === PropertySource.CALCULATED,
        );
        expect(calculatedPropertyInChild !== undefined).toEqual(
          this.keepCalculatedPropertiesInChild,
        );
      });

      if (this.shouldExecuteRectTests) {
        it('sets properties tree and associated ui data from rect', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());

          const rect = assertDefined(uiData.rectsToDraw?.at(2));
          await presenter.onHighlightedIdChange(rect.id);
          expect(uiData.highlightedItem).toEqual(rect.id);
          const propertiesTree = assertDefined(uiData.propertiesTree);
          expect(propertiesTree.id).toEqual(rect.id);
          expect(propertiesTree.getAllChildren().length).toBeGreaterThan(0);
          assertDefined(this.executeSpecializedChecksForPropertiesFromRect)(
            uiData,
          );

          await presenter.onHighlightedIdChange(rect.id);
          expect(uiData.highlightedItem).toEqual('');
        });
      }

      if (this.shouldExecuteSimplifyNamesTest) {
        it('simplifies names in hierarchy tree', async () => {
          const longName = assertDefined(this.treeNodeLongName);
          const shortName = assertDefined(this.treeNodeShortName);
          const userOptions: UserOptions = {
            simplifyNames: {
              name: 'Simplify names',
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
          presenter.onPinnedItemChange(nodeWithLongName);
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

      function chipEqualityTester(
        first: any,
        second: any,
      ): boolean | undefined {
        if (first instanceof Chip || second instanceof Chip) {
          return (
            first.short === second.short &&
            first.long === second.long &&
            first.type === second.type
          );
        }
        return undefined;
      }
    });

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  abstract readonly shouldExecuteRectTests: boolean;
  abstract readonly shouldExecuteSimplifyNamesTest: boolean;
  abstract readonly keepCalculatedPropertiesInChild: boolean;
  abstract readonly keepCalculatedPropertiesInRoot: boolean;
  abstract readonly expectedHierarchyOpts: UserOptions;
  abstract readonly expectedPropertiesOpts: UserOptions;

  readonly expectedRectsOpts?: UserOptions;
  readonly treeNodeLongName?: string;
  readonly treeNodeShortName?: string;

  abstract setUpTestEnvironment(): Promise<void>;
  abstract createPresenter(
    callback: NotifyHierarchyViewCallbackType<UiData>,
    storage: Store,
  ): AbstractHierarchyViewerPresenter<UiData>;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): AbstractHierarchyViewerPresenter<UiData>;
  abstract getPositionUpdate(): TracePositionUpdate;
  abstract getSecondPositionUpdate(): TracePositionUpdate | undefined;
  abstract getSelectedTree(): UiHierarchyTreeNode;
  abstract getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode;
  abstract executePropertiesChecksAfterPositionUpdate(
    uiData: UiDataHierarchy,
  ): void;

  executeSpecializedChecksForPropertiesFromRect?(uiData: UiDataHierarchy): void;
  executePropertiesChecksAfterSecondPositionUpdate?(
    uiData: UiDataHierarchy,
  ): void;
  executeSpecializedTests?(): void;
}

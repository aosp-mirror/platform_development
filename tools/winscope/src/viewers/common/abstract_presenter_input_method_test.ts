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
 * limitations under the License.d
 */

import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Traces} from 'trace/traces';
import {ImeTraceType, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {PresenterInputMethodClients} from 'viewers/viewer_input_method_clients/presenter_input_method_clients';
import {PresenterInputMethodManagerService} from 'viewers/viewer_input_method_manager_service/presenter_input_method_manager_service';
import {PresenterInputMethodService} from 'viewers/viewer_input_method_service/presenter_input_method_service';
import {NotifyHierarchyViewCallbackType} from './abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from './abstract_hierarchy_viewer_presenter_test';
import {AbstractPresenterInputMethod} from './abstract_presenter_input_method';
import {UiDataHierarchy} from './ui_data_hierarchy';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export abstract class AbstractPresenterInputMethodTest extends AbstractHierarchyViewerPresenterTest<ImeUiData> {
  private traces: Traces | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;
  private entries: Map<TraceType, HierarchyTreeNode> | undefined;

  override readonly shouldExecuteFlatTreeTest = true;
  override readonly shouldExecuteRectTests = false;
  override readonly shouldExecuteShowDiffTests = false;
  override readonly shouldExecuteDumpTests = true;
  override readonly shouldExecuteSimplifyNamesTest = false;

  override readonly hierarchyFilterString = 'Reject all';
  override readonly expectedHierarchyChildrenAfterStringFilter = 0;

  override async setUpTestEnvironment(): Promise<void> {
    let secondEntries: Map<TraceType, HierarchyTreeNode>;
    [this.entries, secondEntries] = await UnitTestUtils.getImeTraceEntries();
    this.traces = new Traces();
    const traceEntries = [assertDefined(this.entries.get(this.imeTraceType))];
    const secondEntry = secondEntries.get(this.imeTraceType);
    if (secondEntry) {
      traceEntries.push(secondEntry);
    }

    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(this.imeTraceType)
      .setEntries(traceEntries)
      .setFrame(0, 0)
      .build();
    this.traces.addTrace(trace);

    const sfEntry = this.entries.get(TraceType.SURFACE_FLINGER);
    if (sfEntry) {
      this.traces.addTrace(
        new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.SURFACE_FLINGER)
          .setEntries([sfEntry])
          .setFrame(0, 0)
          .build(),
      );
    }

    const wmEntry = this.entries.get(TraceType.WINDOW_MANAGER);
    if (wmEntry) {
      this.traces.addTrace(
        new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.WINDOW_MANAGER)
          .setEntries([wmEntry])
          .setFrame(0, 0)
          .build(),
      );
    }

    const entry = trace.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(entry);
    this.secondPositionUpdate = secondEntry
      ? TracePositionUpdate.fromTraceEntry(trace.getEntry(1))
      : undefined;

    this.selectedTree = UiHierarchyTreeNode.from(this.getSelectedNode());
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<ImeUiData>,
  ): AbstractPresenterInputMethod {
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(this.imeTraceType)
      .setEntries([])
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    return new this.PresenterInputMethod(
      trace,
      traces,
      new InMemoryStorage(),
      callback,
    );
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType<ImeUiData>,
  ): AbstractPresenterInputMethod {
    const traces = assertDefined(this.traces);
    const trace = assertDefined(traces.getTrace(this.imeTraceType));
    return new this.PresenterInputMethod(
      trace,
      traces,
      new InMemoryStorage(),
      callback,
    );
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate | undefined {
    return this.secondPositionUpdate;
  }

  override getShowDiffPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getExpectedChildrenBeforeVisibilityFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getExpectedChildrenAfterVisibilityFilter(): number {
    return this.numberOfVisibleChildren;
  }

  override getExpectedChildrenBeforeFlatFilter(): number {
    return this.numberOfNestedChildren;
  }

  override getExpectedChildrenAfterFlatFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getExpectedHierarchyChildrenBeforeStringFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getSelectedTree(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override executeChecksForPropertiesTreeAfterPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    const trees = assertDefined(uiData.hierarchyTrees);
    expect(trees.length).toEqual(this.numberOfNestedChildren);
  }

  override executeChecksForPropertiesTreeAfterSecondPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    const trees = assertDefined(uiData.hierarchyTrees);
    expect(trees.length).toEqual(1);
  }

  override executeSpecializedTests() {
    describe('AbstractPresenterInputMethod', () => {
      let presenter: AbstractPresenterInputMethod;
      let uiData: ImeUiData;
      let traces: Traces;
      let entries: Map<TraceType, HierarchyTreeNode>;
      let Presenter:
        | typeof PresenterInputMethodClients
        | typeof PresenterInputMethodService
        | typeof PresenterInputMethodManagerService;
      let imeTraceType: ImeTraceType;

      beforeAll(async () => {
        jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
        Presenter = this.PresenterInputMethod;
        imeTraceType = this.imeTraceType;
        await this.setUpTestEnvironment();
        entries = assertDefined(this.entries);
        await loadTraces();
      });

      it('is robust to traces without SF', async () => {
        setUpPresenter([imeTraceType, TraceType.WINDOW_MANAGER]);
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.hierarchyUserOptions).toBeTruthy();
        expect(uiData.propertiesUserOptions).toBeTruthy();
        expect(uiData.hierarchyTrees).toBeDefined();
      });

      it('is robust to traces without WM', async () => {
        setUpPresenter([imeTraceType, TraceType.SURFACE_FLINGER]);
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.hierarchyUserOptions).toBeTruthy();
        expect(uiData.propertiesUserOptions).toBeTruthy();
        expect(uiData.hierarchyTrees).toBeDefined();
      });

      it('is robust to traces without WM and SF', async () => {
        setUpPresenter([imeTraceType]);
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.hierarchyUserOptions).toBeTruthy();
        expect(uiData.propertiesUserOptions).toBeTruthy();
        expect(uiData.hierarchyTrees).toBeDefined();
      });

      it('can set new additional properties tree and associated ui data from hierarchy tree node', async () => {
        setUpPresenter([imeTraceType, TraceType.WINDOW_MANAGER]);
        expect(uiData.propertiesTree).toBeUndefined();
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onAdditionalPropertySelected({
          name: 'Test Tree',
          treeNode: this.getSelectedTree(),
        });
        const propertiesTree = assertDefined(uiData.propertiesTree);
        expect(propertiesTree.getDisplayName()).toEqual('Test Tree');
        expect(uiData.highlightedItem).toEqual(this.getSelectedTree().id);
      });

      if (this.getPropertiesTree) {
        it('can set new additional properties tree and associated ui data from property tree node', async () => {
          const selectedPropertyTree = assertDefined(this.getPropertiesTree)();
          if (!selectedPropertyTree) {
            return;
          }
          setUpPresenter([imeTraceType]);
          expect(uiData.propertiesTree).toBeUndefined();
          await presenter.onAppEvent(this.getPositionUpdate());
          await presenter.onAdditionalPropertySelected({
            name: 'Additional Properties Tree',
            treeNode: selectedPropertyTree,
          });
          const propertiesTree = assertDefined(uiData.propertiesTree);
          expect(propertiesTree.getDisplayName()).toEqual(
            'Additional Properties Tree',
          );
          expect(propertiesTree).toEqual(
            UiPropertyTreeNode.from(selectedPropertyTree),
          );
          expect(uiData.highlightedItem).toEqual(selectedPropertyTree.id);
        });
      }

      function setUpPresenter(traceTypes: TraceType[]) {
        traceTypes.forEach((traceType) => {
          const trace = new TraceBuilder<HierarchyTreeNode>()
            .setType(traceType)
            .setEntries([assertDefined(entries.get(traceType))])
            .setFrame(0, 0)
            .build();

          assertDefined(traces).addTrace(trace);
        });
        presenter = createPresenter(traces);
      }

      function createPresenter(traces: Traces): AbstractPresenterInputMethod {
        const callback = (newData: ImeUiData) => {
          uiData = newData;
        };
        const trace = assertDefined(traces.getTrace(imeTraceType));
        return new Presenter(
          trace,
          traces,
          new InMemoryStorage(),
          callback as NotifyHierarchyViewCallbackType<ImeUiData>,
        );
      }

      async function loadTraces() {
        traces = new Traces();
        entries = (await UnitTestUtils.getImeTraceEntries())[0];
      }
    });
  }

  protected getPropertiesTree?(): PropertyTreeNode;
  protected abstract getSelectedNode(): HierarchyTreeNode;

  protected abstract readonly numberOfFlattenedChildren: number;
  protected abstract readonly numberOfVisibleChildren: number;
  protected abstract readonly numberOfNestedChildren: number;
  protected abstract readonly PresenterInputMethod:
    | typeof PresenterInputMethodClients
    | typeof PresenterInputMethodService
    | typeof PresenterInputMethodManagerService;
  protected abstract readonly imeTraceType: ImeTraceType;
}

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
import {InMemoryStorage} from 'common/in_memory_storage';
import {Rect} from 'common/rect';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from 'viewers/common/abstract_hierarchy_viewer_presenter_test';
import {DiffType} from 'viewers/common/diff_type';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterSurfaceFlingerTest extends AbstractHierarchyViewerPresenterTest {
  private traceSf: Trace<HierarchyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private positionUpdateMultiDisplayEntry: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;
  private selectedTreeAfterPositionUpdate: UiHierarchyTreeNode | undefined;

  override readonly shouldExecuteFlatTreeTest = true;
  override readonly shouldExecuteRectTests = true;
  override readonly shouldExecuteShowDiffTests = true;
  override readonly shouldExecuteDumpTests = true;
  override readonly shouldExecuteSimplifyNamesTest = true;

  override readonly numberOfDefaultProperties = 32;
  override readonly numberOfNonDefaultProperties = 24;
  override readonly expectedFirstRect = new Rect(0, 0, 1080, 2400);
  override readonly propertiesFilterString = 'bound';
  override readonly expectedTotalRects = 7;
  override readonly expectedVisibleRects = 6;
  override readonly treeNodeLongName =
    'ActivityRecord{64953af u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity#96';
  override readonly treeNodeShortName =
    'ActivityRecord{64953af u0 com.google.(...).NexusLauncherActivity#96';
  override readonly numberOfFilteredProperties = 3;
  override readonly hierarchyFilterString = 'Wallpaper';
  override readonly expectedHierarchyChildrenAfterStringFilter = 4;
  override readonly propertyWithDiff = 'bounds';
  override readonly expectedPropertyDiffType = DiffType.ADDED;

  private readonly numberOfFlattenedChildren = 94;
  private readonly numberOfVisibleChildren = 6;
  private readonly numberOfNestedChildren = 3;

  override async setUpTestEnvironment(): Promise<void> {
    this.traceSf = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([
        await UnitTestUtils.getLayerTraceEntry(0),
        await UnitTestUtils.getMultiDisplayLayerTraceEntry(),
        await UnitTestUtils.getLayerTraceEntry(1),
      ])
      .build();

    const firstEntry = this.traceSf.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    this.positionUpdateMultiDisplayEntry = TracePositionUpdate.fromTraceEntry(
      this.traceSf.getEntry(1),
    );
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.traceSf.getEntry(2),
    );

    const firstEntryDataTree = await firstEntry.getValue();
    const layer = assertDefined(
      firstEntryDataTree.findDfs(
        UiTreeUtils.makeIdMatchFilter(
          '163 Surface(name=b48baf1 InputMethod)/@0x3a7bd57 - animation-leash of insets_animation#163',
        ),
      ),
    );
    const selectedTreeParent = UiHierarchyTreeNode.from(
      assertDefined(layer.getZParent()),
    );
    this.selectedTree = assertDefined(
      selectedTreeParent.getChildByName(
        'Surface(name=b48baf1 InputMethod)/@0x3a7bd57 - animation-leash of insets_animation#163',
      ),
    );
    this.selectedTreeAfterPositionUpdate = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(
          UiTreeUtils.makeIdMatchFilter('79 Wallpaper BBQ wrapper#79'),
        ),
      ),
    );
    const rect = assertDefined(
      this.selectedTreeAfterPositionUpdate.getRects()?.at(0),
    );
    Object.assign(rect, {isVisible: false});
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType,
  ): Presenter {
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([])
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType,
  ): Presenter {
    const traces = new Traces();
    const trace = assertDefined(this.traceSf);
    traces.addTrace(trace);
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
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

  override executeSpecializedChecksForPropertiesFromNode(
    uiData: UiDataHierarchy,
  ) {
    expect(assertDefined((uiData as UiData).curatedProperties).flags).toEqual(
      'HIDDEN (0x1)',
    );
  }

  override getSelectedTree(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTreeAfterPositionUpdate);
  }

  override executeChecksForPropertiesTreeAfterPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    expect(
      assertDefined(
        uiData.propertiesTree
          ?.getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('2919');
  }

  override executeChecksForPropertiesTreeAfterSecondPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    expect(
      assertDefined(
        uiData.propertiesTree
          ?.getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('44517');
  }

  override executeSpecializedChecksForPropertiesFromRect(
    uiData: UiDataHierarchy,
  ) {
    const curatedProperties = assertDefined(
      (uiData as UiData).curatedProperties,
    );
    expect(curatedProperties.flags).toEqual('ENABLE_BACKPRESSURE (0x100)');
    expect(curatedProperties.summary).toEqual([
      {
        key: 'Covered by',
        layerValues: [
          {
            layerId: '65',
            nodeId: '65 ScreenDecorOverlayBottom#65',
            name: 'ScreenDecorOverlayBottom#65',
          },
          {
            layerId: '62',
            nodeId: '62 ScreenDecorOverlay#62',
            name: 'ScreenDecorOverlay#62',
          },
          {
            layerId: '85',
            nodeId: '85 NavigationBar0#85',
            name: 'NavigationBar0#85',
          },
          {
            layerId: '89',
            nodeId: '89 StatusBar#89',
            name: 'StatusBar#89',
          },
        ],
      },
    ]);
  }

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let presenter: Presenter;
      let uiData: UiData;

      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(() => {
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        presenter = this.createPresenter(
          notifyViewCallback as NotifyHierarchyViewCallbackType,
        );
      });

      it('handles displays with no visible layers', async () => {
        await presenter?.onAppEvent(
          assertDefined(this.positionUpdateMultiDisplayEntry),
        );
        expect(uiData?.displays?.length).toEqual(5);
        // we want the displays to be sorted by name
        expect(uiData?.displays).toEqual([
          {
            displayId: '11529215046312967684',
            groupId: 5,
            name: 'ClusterOsDouble-VD',
          },
          {displayId: '4619827259835644672', groupId: 0, name: 'EMU_display_0'},
          {displayId: '4619827551948147201', groupId: 2, name: 'EMU_display_1'},
          {displayId: '4619827124781842690', groupId: 3, name: 'EMU_display_2'},
          {displayId: '4619827540095559171', groupId: 4, name: 'EMU_display_3'},
        ]);
      });

      it('updates view capture package names', async () => {
        const traceVc = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.VIEW_CAPTURE)
          .setEntries([await UnitTestUtils.getViewCaptureEntry()])
          .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
            packageName: 'com.google.android.apps.nexuslauncher',
            windowName: 'not_used',
          })
          .build();
        const traces = new Traces();

        const traceSf = assertDefined(this.traceSf);
        traces.addTrace(traceSf);
        traces.addTrace(traceVc);
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = new Presenter(
          traceSf,
          traces,
          new InMemoryStorage(),
          notifyViewCallback as NotifyHierarchyViewCallbackType,
        );

        const firstEntry = traceSf.getEntry(0);
        const positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

        await presenter.onAppEvent(positionUpdate);
        expect(
          uiData.rectsToDraw.filter((rect) => rect.hasContent).length,
        ).toEqual(1);
      });

      it('keeps alpha and transform type regardless of show/hide defaults', async () => {
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

        const treeForAlphaCheck = this.getSelectedTree();
        const treeForTransformCheck = this.getSelectedTreeAfterPositionUpdate();

        await presenter.onAppEvent(this.getPositionUpdate());
        await checkColorAndTransformProperties(
          treeForAlphaCheck,
          treeForTransformCheck,
        );

        await presenter.onPropertiesUserOptionsChange(userOptions);
        await checkColorAndTransformProperties(
          treeForAlphaCheck,
          treeForTransformCheck,
        );
      });

      async function checkColorAndTransformProperties(
        treeForAlphaCheck: UiHierarchyTreeNode,
        treeForTransformCheck: UiHierarchyTreeNode,
      ) {
        await presenter.onHighlightedNodeChange(treeForAlphaCheck);
        expect(
          uiData.propertiesTree?.getChildByName('color')?.formattedValue(),
        ).toEqual(`${EMPTY_OBJ_STRING}, alpha: 0`);

        await presenter.onHighlightedNodeChange(treeForTransformCheck);
        expect(
          uiData.propertiesTree
            ?.getChildByName('requestedTransform')
            ?.formattedValue(),
        ).toEqual('IDENTITY');
      }
    });
  }
}

describe('PresenterSurfaceFlinger', () => {
  new PresenterSurfaceFlingerTest().execute();
});

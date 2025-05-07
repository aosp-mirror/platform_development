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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {Store} from 'common/store/store';
import {
  TabbedViewSwitchRequest,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {makeEmptyTrace} from 'test/unit/trace_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {EMPTY_OBJ_STRING} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from 'viewers/common/abstract_hierarchy_viewer_presenter_test';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {TextFilter} from 'viewers/common/text_filter';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {TraceRectType} from 'viewers/components/rects/rect_spec';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterSurfaceFlingerTest extends AbstractHierarchyViewerPresenterTest<UiData> {
  private traceSf: Trace<HierarchyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;
  private selectedTreeAfterPositionUpdate: UiHierarchyTreeNode | undefined;

  override readonly shouldExecuteRectTests = true;
  override readonly shouldExecuteSimplifyNamesTest = true;
  override readonly keepCalculatedPropertiesInChild = false;
  override readonly keepCalculatedPropertiesInRoot = true;
  override readonly expectedHierarchyOpts = {
    showDiff: {
      name: 'Show diff',
      enabled: false,
      isUnavailable: true,
    },
    showOnlyVisible: {
      name: 'Show only',
      chip: VISIBLE_CHIP,
      enabled: false,
    },
    simplifyNames: {
      name: 'Simplify names',
      enabled: true,
    },
    flat: {
      name: 'Flat',
      enabled: false,
    },
  };
  override readonly expectedPropertiesOpts = {
    showDiff: {
      name: 'Show diff',
      enabled: false,
      isUnavailable: true,
    },
    showDefaults: {
      name: 'Show defaults',
      enabled: false,
      tooltip: `If checked, shows the value of all properties.
Otherwise, hides all properties whose value is
the default for its data type.`,
    },
  };
  override readonly expectedRectsOpts = {
    ignoreRectShowState: {
      name: 'Ignore',
      icon: 'visibility',
      enabled: false,
    },
    showOnlyVisible: {
      name: 'Show only',
      chip: VISIBLE_CHIP,
      enabled: false,
    },
  };

  override readonly rectIndex = 15;
  override readonly expectedInitialRectSpec = {
    type: TraceRectType.LAYERS,
    icon: TRACE_INFO[TraceType.SURFACE_FLINGER].icon,
    legend: [
      {
        fill: '#c8e8b7',
        desc: 'Visible',
        border: 'var(--default-text-color)',
        showInWireFrameMode: false,
      },
      {
        fill: '#dcdcdc',
        desc: 'Not visible',
        border: 'var(--default-text-color)',
        showInWireFrameMode: false,
      },
      {
        fill: 'var(--selected-element-color)',
        desc: 'Selected',
        border: 'var(--default-text-color)',
        showInWireFrameMode: true,
      },
      {
        fill: '#ad42f5',
        desc: 'Has view content',
        border: 'var(--default-text-color)',
        showInWireFrameMode: false,
      },
      {border: '#ffc24b', desc: 'Pinned', showInWireFrameMode: true},
      {border: '#b34a24', desc: 'Pinned', showInWireFrameMode: true},
    ],
  };
  readonly expectedInputWindowsSpec = {
    type: TraceRectType.INPUT_WINDOWS,
    icon: TRACE_INFO[TraceType.INPUT_EVENT_MERGED].icon,
    legend: [
      {
        fill: '#c8e8b7',
        desc: 'Visible and touchable',
        border: 'var(--default-text-color)',
        showInWireFrameMode: false,
      },
      {
        fill: '#dcdcdc',
        desc: 'Not visible',
        border: 'var(--default-text-color)',
        showInWireFrameMode: false,
      },
      {
        fill: '',
        border: 'var(--default-text-color)',
        desc: 'Visible but not touchable',
        showInWireFrameMode: false,
      },
      {
        fill: 'var(--selected-element-color)',
        desc: 'Selected',
        border: 'var(--default-text-color)',
        showInWireFrameMode: true,
      },
      {border: '#ffc24b', desc: 'Pinned', showInWireFrameMode: true},
      {border: '#b34a24', desc: 'Pinned', showInWireFrameMode: true},
    ],
  };
  override readonly treeNodeLongName =
    'com.google.android.apps.maps/com.google.android.maps.LimitedMapsActivity#630';
  override readonly treeNodeShortName =
    'com.google.(...).LimitedMapsActivity#630';

  override async setUpTestEnvironment(): Promise<void> {
    const parser = await new LegacyParserProvider()
      .addFilename(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger_multidisplay.pb',
      )
      .setConvertToPerfetto(true)
      .getParser<HierarchyTreeNode>();

    this.traceSf = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([
        await parser.getEntry(0),
        await parser.getEntry(1),
        await parser.getEntry(2),
      ])
      .build();

    const firstEntry = this.traceSf.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.traceSf.getEntry(1),
    );

    const firstEntryDataTree = await firstEntry.getValue();

    const layer = assertDefined(
      firstEntryDataTree.findDfs(
        UiTreeUtils.makeIdMatchFilter(
          '576 com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher#576',
        ),
      ),
    );
    const selectedTreeParent = UiHierarchyTreeNode.from(
      assertDefined(layer.getZParent()),
    );
    this.selectedTree = assertDefined(
      selectedTreeParent.getChildByName(
        'com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher#576',
      ),
    );

    const treeAfterPositionUpdateParent = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree
          .findDfs(
            UiTreeUtils.makeIdMatchFilter(
              '630 com.google.android.apps.maps/com.google.android.maps.LimitedMapsActivity#630',
            ),
          )
          ?.getZParent(),
      ),
    );
    this.selectedTreeAfterPositionUpdate = assertDefined(
      treeAfterPositionUpdateParent.getChildByName(
        'com.google.android.apps.maps/com.google.android.maps.LimitedMapsActivity#630',
      ),
    );
    const rect = assertDefined(
      this.selectedTreeAfterPositionUpdate.getRects()?.at(0),
    );
    Object.assign(rect, {isVisible: false});
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): Presenter {
    const trace = makeEmptyTrace(TraceType.SURFACE_FLINGER);
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType<UiData>,
    storage: Store,
  ): Presenter {
    const traces = new Traces();
    const traceSf = assertDefined(this.traceSf);
    traces.addTrace(traceSf);
    return new Presenter(traceSf, traces, storage, callback);
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override getSelectedTree(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override executePropertiesChecksAfterPositionUpdate(uiData: UiDataHierarchy) {
    expect(
      uiData.propertiesTree?.getChildByName('screenBounds')?.formattedValue(),
    ).toEqual('(0, 0) - (1080, 600)');
    expect(
      assertDefined(
        uiData.propertiesTree?.getChildByName('damageRegion'),
      ).formattedValue(),
    ).toEqual('SkRegion((0, 0, 1080, 600))');
    expect(uiData.displays?.at(0)).toEqual({
      displayId: '4619827259835644672',
      groupId: 0,
      name: 'EMU_display_0',
      isActive: true,
    });
    expect(assertDefined((uiData as UiData).curatedProperties).flags).toEqual(
      'ENABLE_BACKPRESSURE (0x100)',
    );
    expect((uiData as UiData).rectSpec).toEqual(this.expectedInitialRectSpec);
    expect((uiData as UiData).allRectSpecs).toEqual([
      this.expectedInitialRectSpec,
      this.expectedInputWindowsSpec,
    ]);
  }

  override executePropertiesChecksAfterSecondPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    expect(
      uiData.propertiesTree?.getChildByName('damageRegion'),
    ).toBeUndefined();
  }

  override executeSpecializedChecksForPropertiesFromRect(
    uiData: UiDataHierarchy,
  ) {
    const curatedProperties = assertDefined(
      (uiData as UiData).curatedProperties,
    );
    expect(curatedProperties.flags).toEqual(
      'OPAQUE | ENABLE_BACKPRESSURE (0x102)',
    );
    expect(curatedProperties.summary).toEqual([
      {
        key: 'Covered by',
        desc: 'Partially or fully covered by these likely translucent layers',
        layerValues: [
          {
            layerId: '174',
            nodeId: '174 BottomCarSystemBar#174',
            name: 'BottomCarSystemBar#174',
          },
          {
            layerId: '164',
            nodeId: '164 TopCarSystemBar#164',
            name: 'TopCarSystemBar#164',
          },
          {
            layerId: '576',
            nodeId:
              '576 com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher#576',
            name: 'com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher#576',
          },
        ],
      },
    ]);
  }

  override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let presenter: Presenter;
      let uiData: UiData;
      let userNotifierChecker: UserNotifierChecker;

      beforeAll(async () => {
        userNotifierChecker = new UserNotifierChecker();
        await this.setUpTestEnvironment();
      });

      beforeEach(() => {
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        presenter = this.createPresenter(
          notifyViewCallback as NotifyHierarchyViewCallbackType<UiData>,
          new InMemoryStorage(),
        );
      });

      afterEach(() => {
        userNotifierChecker.expectNone();
        userNotifierChecker.reset();
      });

      it('adds event listeners', async () => {
        const el = document.createElement('div');
        presenter.addEventListeners(el);

        let spy: jasmine.Spy = spyOn(presenter, 'onRectDoubleClick');
        const testId = 'test';
        el.dispatchEvent(
          new CustomEvent(ViewerEvents.RectsDblClick, {
            detail: {clickedRectId: testId},
          }),
        );
        expect(spy).toHaveBeenCalledWith(testId);

        spy = spyOn(presenter, 'onRectTypeButtonClicked');
        el.dispatchEvent(
          new CustomEvent(ViewerEvents.RectTypeButtonClick, {
            detail: {type: TraceRectType.LAYERS},
          }),
        );
        expect(spy).toHaveBeenCalledOnceWith(TraceRectType.LAYERS);
      });

      it('handles displays with no visible layers', async () => {
        await presenter?.onAppEvent(assertDefined(this.positionUpdate));
        expect(uiData?.displays?.length).toEqual(5);
        // we want the displays to be sorted by name
        expect(uiData?.displays).toEqual([
          {
            displayId: '4619827259835644672',
            groupId: 0,
            name: 'EMU_display_0',
            isActive: true,
          },
          {
            displayId: '4619827551948147201',
            groupId: 2,
            name: 'EMU_display_1',
            isActive: true,
          },
          {
            displayId: '4619827540095559171',
            groupId: 4,
            name: 'EMU_display_3',
            isActive: true,
          },
          {
            displayId: '4619827124781842690',
            groupId: 3,
            name: 'EMU_display_2',
            isActive: true,
          },
          {
            displayId: '-6917529027396583932',
            groupId: 5,
            name: 'ClusterOsDouble-VD',
            isActive: false,
          },
        ]);
      });

      it('uses WM focused display id to determine active display', async () => {
        const traces = new Traces();
        const traceSf = assertDefined(this.traceSf);
        const traceWm = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.WINDOW_MANAGER)
          .setEntries([
            new HierarchyTreeBuilder()
              .setId('WindowManagerState entry')
              .setName('root')
              .setProperties({focusedDisplayId: 3})
              .build(),
          ])
          .build();
        traces.addTrace(traceSf);
        traces.addTrace(traceWm);
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = new Presenter(
          traceSf,
          traces,
          new InMemoryStorage(),
          notifyViewCallback,
        );
        const positionUpdate = TracePositionUpdate.fromTraceEntry(
          traceSf.getEntry(0),
        );
        await presenter.onAppEvent(positionUpdate);
        expect(uiData?.displays[0]).toEqual({
          displayId: '4619827259835644672',
          groupId: 0,
          name: 'EMU_display_0',
          isActive: false,
        });
      });

      it('updates view capture package names', async () => {
        await createPresenterWithViewCapture(assertDefined(this.traceSf));
        expect(
          uiData.rectsToDraw.filter((rect) => rect.hasContent).length,
        ).toEqual(1);
      });

      it('handles rect double click if view capture trace present', async () => {
        const [presenter, traceVc] = await createPresenterWithViewCapture(
          assertDefined(this.traceSf),
        );
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);

        await presenter.onRectDoubleClick('not in package');
        expect(spy).not.toHaveBeenCalled();
        await presenter.onRectDoubleClick('com.android.car.carlauncher');
        expect(spy).toHaveBeenCalledOnceWith(
          new TabbedViewSwitchRequest(traceVc),
        );
      });

      it('robust to rect double click if view capture trace not present', async () => {
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);
        await presenter.onRectDoubleClick('not in package');
        expect(spy).not.toHaveBeenCalled();
      });

      it('keeps alpha and transform type regardless of show/hide defaults', async () => {
        const treeForAlphaCheck = this.getSelectedTree();
        const treeForTransformCheck = this.getSelectedTreeAfterPositionUpdate();
        await presenter.onAppEvent(this.getPositionUpdate());
        await checkColorAndTransform(treeForAlphaCheck, treeForTransformCheck);
        await presenter.onPropertiesUserOptionsChange({
          showDefaults: {name: '', enabled: true},
        });
        await checkColorAndTransform(treeForAlphaCheck, treeForTransformCheck);
      });

      it('clears curated properties on position update if no properties tree found', async () => {
        const trace = assertDefined(this.traceSf);
        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(2)),
        );

        await presenter.onHighlightedIdChange(
          '744 1d30e3b VolumeDialogImpl#744',
        );
        expect(uiData.propertiesTree).toBeDefined();
        expect(uiData.curatedProperties).toBeDefined();

        await presenter.onAppEvent(assertDefined(this.positionUpdate));
        expect(uiData.propertiesTree).toBeUndefined();
        expect(uiData.curatedProperties).toBeUndefined();
      });

      it('updates zOrderRelativeOf formatter and rel-z curated properties correctly', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());

        const nodeWithRelZChild = this.getSelectedTree();
        const nodeWithRelZParent = assertDefined(
          assertDefined(uiData.hierarchyTrees)[0].findDfs(
            UiTreeUtils.makeNodeFilter(
              new TextFilter(
                '626 SurfaceView[com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher]#626',
              ).getFilterPredicate(),
            ),
          ),
        );

        await presenter.onHighlightedNodeChange(nodeWithRelZChild);
        const secondRelZChildName =
          'Background for SurfaceView[com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher]#628';
        expect(uiData.curatedProperties?.relativeParent).toEqual('none');
        expect(uiData.curatedProperties?.relativeChildren).toEqual([
          {
            layerId: '626',
            nodeId: nodeWithRelZParent.id,
            name: nodeWithRelZParent.name,
          },
          {
            layerId: '628',
            nodeId: '628 ' + secondRelZChildName,
            name: secondRelZChildName,
          },
        ]);

        await presenter.onHighlightedNodeChange(nodeWithRelZParent);
        expect(uiData.curatedProperties?.relativeParent).toEqual({
          layerId: '576',
          nodeId: nodeWithRelZChild.id,
          name: nodeWithRelZChild.name,
        });
        expect(uiData.curatedProperties?.relativeChildren).toEqual([]);
      });

      it('sets properties tree but no curated properties for root node', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onHighlightedIdChange(
          assertDefined(uiData.hierarchyTrees)[0].id,
        );
        expect(uiData.propertiesTree?.getDisplayName()).toEqual(
          '1970-01-01, 00:00:00.000',
        );
        expect(uiData.curatedProperties).toBeUndefined();
      });

      it('formats summary, color, pixel and crop correctly in curated properties', async () => {
        const tree = new HierarchyTreeBuilder()
          .setId('LayerTraceEntry')
          .setName('root')
          .setChildren([
            {
              id: '1',
              name: 'layer1',
              properties: {
                occludedBy: ['0 layer0'],
                partiallyOccludedBy: ['2 layer2'],
                coveredBy: ['3 layer3'],
                flags: null,
                zOrderRelativeOf: null,
                bounds: null,
                screenBounds: null,
                activeBuffer: null,
                currFrame: null,
                destinationFrame: {left: 0, right: 1, top: 0, bottom: 1},
                z: null,
                color: {r: 0, g: 0, b: 0, a: 1},
                shadowRadius: 1,
                cornerRadius: null,
                cornerRadiusCrop: null,
                backgroundBlurRadius: null,
                requestedColor: null,
                requestedCornerRadius: null,
              },
            },
          ])
          .build();
        const traces = new Traces();
        const traceSf = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.SURFACE_FLINGER)
          .setEntries([tree])
          .build();
        traces.addTrace(traceSf);
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = new Presenter(
          traceSf,
          traces,
          new InMemoryStorage(),
          notifyViewCallback,
        );
        const positionUpdate = TracePositionUpdate.fromTraceEntry(
          traceSf.getEntry(0),
        );
        await presenter.onAppEvent(positionUpdate);
        await presenter.onHighlightedIdChange(
          assertDefined(tree.getChildByName('layer1')).id,
        );
        expect(uiData.curatedProperties?.summary).toEqual([
          {
            key: 'Occluded by',
            desc: 'Fully occluded by these opaque layers',
            layerValues: [{layerId: '0', nodeId: '0 layer0', name: 'layer0'}],
          },
          {
            key: 'Partially occluded by',
            desc: 'Partially occluded by these opaque layers',
            layerValues: [{layerId: '2', nodeId: '2 layer2', name: 'layer2'}],
          },
          {
            key: 'Covered by',
            desc: 'Partially or fully covered by these likely translucent layers',
            layerValues: [{layerId: '3', nodeId: '3 layer3', name: 'layer3'}],
          },
        ]);
        expect(uiData.curatedProperties?.calcColor).toEqual(
          '(0, 0, 0), alpha: 1',
        );
        expect(uiData.curatedProperties?.reqColor).toEqual('no color found');
        expect(uiData.curatedProperties?.calcShadowRadius).toEqual('1 px');
        expect(uiData.curatedProperties?.calcCornerRadius).toEqual('0 px');
        expect(uiData.curatedProperties?.destinationFrame).toEqual(
          '(0, 0) - (1, 1)',
        );
        expect(uiData.curatedProperties?.calcCrop).toEqual(EMPTY_OBJ_STRING);
      });

      it('draws input windows', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.rectsToDraw.length).toEqual(27);
        expect(uiData.rectsToDraw[6].label).toEqual(
          'Bounds for - com.android.car.carlauncher/com.android.car.carlauncher.CarLauncher#577',
        );
        presenter.onRectTypeButtonClicked(TraceRectType.INPUT_WINDOWS);
        expect(uiData.rectsToDraw.length).toEqual(15);
        expect(uiData.rectsToDraw[6].label).toEqual(
          'com.google.android.apps.maps/com.google.android.maps.LimitedMapsActivity#630',
        );
        expect(uiData.rectSpec).toEqual(this.expectedInputWindowsSpec);
        expect(uiData.allRectSpecs).toEqual([
          this.expectedInitialRectSpec,
          this.expectedInputWindowsSpec,
        ]);
      });
      async function checkColorAndTransform(
        treeForAlphaCheck: UiHierarchyTreeNode,
        treeForTransformCheck: UiHierarchyTreeNode,
      ) {
        await presenter.onHighlightedNodeChange(treeForAlphaCheck);
        expect(
          uiData.propertiesTree?.getChildByName('color')?.formattedValue(),
        ).toEqual(`${EMPTY_OBJ_STRING}, alpha: 1`);

        await presenter.onHighlightedNodeChange(treeForTransformCheck);
        expect(
          uiData.propertiesTree
            ?.getChildByName('requestedTransform')
            ?.formattedValue(),
        ).toEqual('IDENTITY');
      }

      async function createPresenterWithViewCapture(
        traceSf: Trace<HierarchyTreeNode>,
      ): Promise<[Presenter, Trace<HierarchyTreeNode>]> {
        const traceVc = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.VIEW_CAPTURE)
          .setEntries([
            TreeNodeUtils.makeHierarchyNode({id: 'vc id', name: 'vc node'}),
          ])
          .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
            packageName: 'com.android.car.carlauncher',
            windowName: 'not_used',
          })
          .build();
        const traces = new Traces();

        traces.addTrace(traceSf);
        traces.addTrace(traceVc);
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = new Presenter(
          traceSf,
          traces,
          new InMemoryStorage(),
          notifyViewCallback as NotifyHierarchyViewCallbackType<UiData>,
        );

        const firstEntry = traceSf.getEntry(0);
        const positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

        await presenter.onAppEvent(positionUpdate);
        return [presenter, traceVc];
      }
    });
  }
}

describe('PresenterSurfaceFlinger', () => {
  new PresenterSurfaceFlingerTest().execute();
});

/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {
  getFixtureFile,
  getPerfettoParser,
  LegacyParserProvider,
} from 'test/unit/fixture_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {makeEmptyTrace} from 'test/unit/trace_utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from 'viewers/common/abstract_hierarchy_viewer_presenter_test';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {TraceRectType} from 'viewers/components/rects/rect_spec';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';

class PresenterViewCaptureTest extends AbstractHierarchyViewerPresenterTest<UiData> {
  private traces: Traces | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;

  override readonly shouldExecuteRectTests = true;
  override readonly shouldExecuteSimplifyNamesTest = true;
  override readonly keepCalculatedPropertiesInChild = false;
  override readonly keepCalculatedPropertiesInRoot = false;
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

  override readonly rectIndex = 9;
  override readonly expectedInitialRectSpec = {
    type: TraceRectType.VIEWS,
    icon: TRACE_INFO[TraceType.VIEW_CAPTURE].icon,
    legend: [
      {
        fill: '#ad42f5',
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
      {border: '#ffc24b', desc: 'Pinned', showInWireFrameMode: true},
      {border: '#b34a24', desc: 'Pinned', showInWireFrameMode: true},
    ],
  };
  override readonly treeNodeLongName =
    'com.google.android.apps.nexuslauncher.allapps.SearchContainerView@53568094';
  override readonly treeNodeShortName = 'SearchContainerView@53568094';

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await getPerfettoParser(
      TraceType.VIEW_CAPTURE,
      'traces/perfetto/viewcapture.perfetto-trace',
    )) as Parser<HierarchyTreeNode>;

    const trace = Trace.fromParser(parser);
    this.traces = new Traces();
    this.traces.addTrace(trace);

    const firstEntry = trace.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(1),
    );

    const firstEntryDataTree = await firstEntry.getValue();

    this.selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree
          .findDfs(
            UiTreeUtils.makeIdMatchFilter(
              'ViewNode44 ' + this.treeNodeLongName,
            ),
          )
          ?.getParent(),
      ),
    ).getChildByName(this.treeNodeLongName);
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): Presenter {
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE)
      .setEntries([])
      .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
        packageName: 'the_package_name',
        windowName: 'the_window_name',
      })
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(traces, new InMemoryStorage(), callback);
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType<UiData>,
    storage: Store,
  ): Presenter {
    return new Presenter(assertDefined(this.traces), storage, callback);
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
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('786.506');
    expect(propertiesTree.getChildByName('translationX')).toBeUndefined();
    expect(uiData.displays).toEqual([
      {displayId: 0, groupId: 0, name: 'PhoneWindow@4f9be60', isActive: true},
    ]);
    const curatedProperties = assertDefined(
      (uiData as UiData).curatedProperties,
    );
    expect(curatedProperties.translationY).toEqual('786.506');
    expect(curatedProperties.translationX).toEqual('0');
  }

  override executePropertiesChecksAfterSecondPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('785.500');
    expect(
      assertDefined((uiData as UiData).curatedProperties).translationY,
    ).toEqual('785.500');
  }

  override executeSpecializedChecksForPropertiesFromRect(
    uiData: UiDataHierarchy,
  ) {
    const curatedProperties = assertDefined(
      (uiData as UiData).curatedProperties,
    );
    expect(curatedProperties.translationX).toEqual('-9.800');
    expect(curatedProperties.translationY).toEqual('210.700');
    expect(curatedProperties.alpha).toEqual('0');
    expect(curatedProperties.willNotDraw).toEqual('true');
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
          notifyViewCallback as NotifyHierarchyViewCallbackType<UiData>,
          new InMemoryStorage(),
        );
      });

      it('adds event listeners', async () => {
        const element = document.createElement('div');
        presenter.addEventListeners(element);
        const spy: jasmine.Spy = spyOn(presenter, 'onMiniRectsDoubleClick');
        element.dispatchEvent(new CustomEvent(ViewerEvents.MiniRectsDblClick));
        expect(spy).toHaveBeenCalledTimes(1);
      });

      it('exposes all VC traces', () => {
        const traces = new Traces();
        const vcTraces = [
          makeEmptyTrace(TraceType.VIEW_CAPTURE),
          makeEmptyTrace(TraceType.VIEW_CAPTURE),
        ];
        vcTraces.forEach((trace) => traces.addTrace(trace));
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = new Presenter(
          traces,
          new InMemoryStorage(),
          notifyViewCallback,
        );
        expect(presenter.getTraces()).toEqual(vcTraces);
      });

      it('extracts rects from SF trace', async () => {
        const perfettoFile = new TraceFile(
          await getFixtureFile('traces/perfetto/viewcapture.perfetto-trace'),
        );
        const sfTrace = Trace.fromParser(
          await new LegacyParserProvider()
            .addFilename('traces/elapsed_timestamp/SurfaceFlinger.pb')
            .setExistingPerfettoFile(perfettoFile)
            .setConvertToPerfetto(true)
            .getParser<HierarchyTreeNode>(),
        );
        const presenterWithSfTrace = createPresenterWithSfTrace(
          assertDefined(this.traces),
          sfTrace,
        );
        await presenterWithSfTrace.onAppEvent(
          assertDefined(this.positionUpdate),
        );
        expect(assertDefined(uiData.sfRects).length).toBeGreaterThan(0);
      });

      it('handles double click if SF trace present', async () => {
        const sfTrace = makeEmptyTrace(TraceType.SURFACE_FLINGER);
        const presenterWithSfTrace = createPresenterWithSfTrace(
          assertDefined(this.traces),
          sfTrace,
        );
        const spy = jasmine.createSpy();
        presenterWithSfTrace.setEmitEvent(spy);

        await presenterWithSfTrace.onMiniRectsDoubleClick();
        expect(spy).toHaveBeenCalledOnceWith(
          new TabbedViewSwitchRequest(sfTrace),
        );
      });

      it('robust to double click if SF trace not present', async () => {
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);
        await presenter.onMiniRectsDoubleClick();
        expect(spy).not.toHaveBeenCalled();
      });

      it('clears curated properties on position update if no properties tree found', async () => {
        await presenter.onAppEvent(assertDefined(this.positionUpdate));
        const nodeName =
          'com.android.launcher3.allapps.AllAppsRecyclerView@188184411';
        await presenter.onHighlightedIdChange(nodeName);
        expect(uiData.propertiesTree).toBeDefined();
        expect(uiData.curatedProperties).toBeDefined();

        await presenter.onAppEvent(assertDefined(this.secondPositionUpdate));
        expect(uiData.propertiesTree).toBeUndefined();
        expect(uiData.curatedProperties).toBeUndefined();
      });

      function createPresenterWithSfTrace(
        vcTraces: Traces,
        sfTrace: Trace<HierarchyTreeNode>,
      ): Presenter {
        const traces = new Traces();
        vcTraces.forEachTrace((trace) => traces.addTrace(trace));
        traces.addTrace(sfTrace);

        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        return new Presenter(traces, new InMemoryStorage(), notifyViewCallback);
      }
    });
  }
}

describe('PresenterViewCapture', () => {
  new PresenterViewCaptureTest().execute();
});

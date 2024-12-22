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
import {FunctionUtils} from 'common/function_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TimezoneInfo} from 'common/time';
import {TimestampConverter} from 'common/timestamp_converter';
import {CrossToolProtocol} from 'cross_tool/cross_tool_protocol';
import {ProgressListener} from 'messaging/progress_listener';
import {ProgressListenerStub} from 'messaging/progress_listener_stub';
import {UserWarning} from 'messaging/user_warning';
import {
  FailedToCreateTracesParser,
  IncompleteFrameMapping,
  InvalidLegacyTrace,
  NoTraceTargetsSelected,
  NoValidFiles,
  UnsupportedFileFormat,
} from 'messaging/user_warnings';
import {
  ActiveTraceChanged,
  AppFilesCollected,
  AppFilesUploaded,
  AppInitialized,
  AppRefreshDumpsRequest,
  AppResetRequest,
  AppTraceViewRequest,
  DarkModeToggled,
  ExpandedTimelineToggled,
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  NoTraceTargetsSelected as NoTraceTargetsSelectedEvent,
  RemoteToolDownloadStart,
  RemoteToolFilesReceived,
  RemoteToolTimestampReceived,
  TabbedViewSwitched,
  TabbedViewSwitchRequest,
  TracePositionUpdate,
  ViewersLoaded,
  ViewersUnloaded,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {WinscopeEventEmitter} from 'messaging/winscope_event_emitter';
import {WinscopeEventEmitterStub} from 'messaging/winscope_event_emitter_stub';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {WinscopeEventListenerStub} from 'messaging/winscope_event_listener_stub';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {ViewType} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const TIMESTAMP_10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const TIMESTAMP_11 = TimestampConverterUtils.makeRealTimestamp(11n);

  const POSITION_10 = TracePosition.fromTimestamp(TIMESTAMP_10);
  const POSITION_11 = TracePosition.fromTimestamp(TIMESTAMP_11);

  const traceSf = new TraceBuilder<HierarchyTreeNode>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps([TIMESTAMP_10])
    .build();
  const traceWm = new TraceBuilder<HierarchyTreeNode>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps([TIMESTAMP_11])
    .build();
  const traceDump = new TraceBuilder<HierarchyTreeNode>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
    .build();

  let inputFiles: File[];
  let eventLogFile: File;
  let tracePipeline: TracePipeline;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: WinscopeEventEmitter & WinscopeEventListener;
  let crossToolProtocol: CrossToolProtocol;
  let appComponent: WinscopeEventListener;
  let timelineComponent: WinscopeEventEmitter & WinscopeEventListener;
  let uploadTracesComponent: ProgressListenerStub;
  let collectTracesComponent: ProgressListenerStub &
    WinscopeEventEmitterStub &
    WinscopeEventListenerStub;
  let traceViewComponent: WinscopeEventEmitter & WinscopeEventListener;
  let mediator: Mediator;
  let spies: Array<jasmine.Spy<jasmine.Func>>;
  let userNotifierChecker: UserNotifierChecker;

  const viewerStub0 = new ViewerStub('Title0', undefined, traceSf);
  const viewerStub1 = new ViewerStub('Title1', undefined, traceWm);
  const viewerOverlay = new ViewerStub(
    'TitleOverlay',
    undefined,
    traceWm,
    ViewType.OVERLAY,
  );
  const viewerDump = new ViewerStub('TitleDump', undefined, traceDump);
  const viewers = [viewerStub0, viewerStub1, viewerOverlay, viewerDump];
  let tracePositionUpdateListeners: WinscopeEventListener[];

  beforeAll(async () => {
    inputFiles = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
      ),
    ];
    eventLogFile = await UnitTestUtils.getFixtureFile(
      'traces/eventlog_no_cujs.winscope',
    );
    userNotifierChecker = new UserNotifierChecker();
  });

  beforeEach(() => {
    userNotifierChecker.reset();
    jasmine.addCustomEqualityTester(tracePositionUpdateEqualityTester);
    tracePipeline = new TracePipeline();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    crossToolProtocol = new CrossToolProtocol(
      tracePipeline.getTimestampConverter(),
    );
    appComponent = new WinscopeEventListenerStub();
    timelineComponent = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    uploadTracesComponent = new ProgressListenerStub();
    collectTracesComponent = FunctionUtils.mixin(
      FunctionUtils.mixin(
        new ProgressListenerStub(),
        new WinscopeEventListenerStub(),
      ),
      new WinscopeEventEmitterStub(),
    );
    traceViewComponent = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    mediator = new Mediator(
      tracePipeline,
      timelineData,
      abtChromeExtensionProtocol,
      crossToolProtocol,
      appComponent,
      new InMemoryStorage(),
    );
    mediator.setTimelineComponent(timelineComponent);
    mediator.setUploadTracesComponent(uploadTracesComponent);
    mediator.setCollectTracesComponent(collectTracesComponent);
    mediator.setTraceViewComponent(traceViewComponent);

    tracePositionUpdateListeners = [
      ...viewers,
      timelineComponent,
      crossToolProtocol,
    ];

    spyOn(ViewerFactory.prototype, 'createViewers').and.returnValue(viewers);

    spies = [
      spyOn(abtChromeExtensionProtocol, 'onWinscopeEvent'),
      spyOn(appComponent, 'onWinscopeEvent'),
      spyOn(collectTracesComponent, 'onOperationFinished'),
      spyOn(collectTracesComponent, 'onProgressUpdate'),
      spyOn(collectTracesComponent, 'onWinscopeEvent'),
      spyOn(crossToolProtocol, 'onWinscopeEvent'),
      spyOn(timelineComponent, 'onWinscopeEvent'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(traceViewComponent, 'onWinscopeEvent'),
      spyOn(uploadTracesComponent, 'onProgressUpdate'),
      spyOn(uploadTracesComponent, 'onOperationFinished'),
      spyOn(viewerStub0, 'onWinscopeEvent'),
      spyOn(viewerStub1, 'onWinscopeEvent'),
      spyOn(viewerOverlay, 'onWinscopeEvent'),
      spyOn(viewerDump, 'onWinscopeEvent'),
    ];
  });

  it('notifies ABT chrome extension about app initialization', async () => {
    expect(abtChromeExtensionProtocol.onWinscopeEvent).not.toHaveBeenCalled();

    await mediator.onWinscopeEvent(new AppInitialized());
    expect(abtChromeExtensionProtocol.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new AppInitialized(),
    );
  });

  it('handles uploaded traces from Winscope', async () => {
    await mediator.onWinscopeEvent(new AppFilesUploaded(inputFiles));

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).not.toHaveBeenCalled();
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
    expect(viewerStub0.onWinscopeEvent).not.toHaveBeenCalled();

    resetSpyCalls();
    await mediator.onWinscopeEvent(new AppTraceViewRequest());
    await checkLoadTraceViewEvents(uploadTracesComponent);
    userNotifierChecker.expectNotified([]);
  });

  it('handles collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({requested: [], collected: inputFiles}),
    );
    userNotifierChecker.expectNotified([]);
    await checkLoadTraceViewEvents(collectTracesComponent);
  });

  it('handles invalid collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({
        requested: [],
        collected: [await UnitTestUtils.getFixtureFile('traces/empty.pb')],
      }),
    );
    expect(
      userNotifierChecker.expectNotified([
        new UnsupportedFileFormat('empty.pb'),
      ]),
    );
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
  });

  it('handles collected traces with no entries from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({
        requested: [],
        collected: [
          await UnitTestUtils.getFixtureFile(
            'traces/no_entries_InputMethodClients.pb',
          ),
        ],
      }),
    );
    expect(
      userNotifierChecker.expectNotified([
        new InvalidLegacyTrace(
          'no_entries_InputMethodClients.pb',
          'Trace has no entries',
        ),
      ]),
    );
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
  });

  it('handles collected trace with no visualization from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({
        requested: [],
        collected: [eventLogFile],
      }),
    );
    expect(
      userNotifierChecker.expectNotified([
        new FailedToCreateTracesParser(
          TraceType.CUJS,
          'eventlog_no_cujs.winscope has no relevant entries',
        ),
      ]),
    );
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
  });

  it('handles empty collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({
        requested: [],
        collected: [],
      }),
    );
    expect(userNotifierChecker.expectNotified([new NoValidFiles()]));
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
  });

  it('handles requested traces with missing collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(
      new AppFilesCollected({
        requested: [
          {
            name: 'Collected Trace',
            types: [TraceType.EVENT_LOG, TraceType.CUJS],
          },
          {
            name: 'Uncollected Trace',
            types: [TraceType.TRANSITION],
          },
        ],
        collected: inputFiles.concat([eventLogFile]),
      }),
    );
    expect(
      userNotifierChecker.expectNotified([
        new NoValidFiles(['Uncollected Trace']),
      ]),
    );
    expect(appComponent.onWinscopeEvent).toHaveBeenCalled();
  });

  it('handles app reset request', async () => {
    await mediator.onWinscopeEvent(new AppFilesUploaded(inputFiles));
    const clearSpies = [
      spyOn(tracePipeline, 'clear'),
      spyOn(timelineData, 'clear'),
    ];
    await mediator.onWinscopeEvent(new AppResetRequest());
    clearSpies.forEach((spy) => expect(spy).toHaveBeenCalled());
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new ViewersUnloaded(),
    );
  });

  it('handles request to refresh dumps', async () => {
    const dumpFiles = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb',
      ),
      await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb'),
    ];
    await loadFiles(dumpFiles);
    await mediator.onWinscopeEvent(new AppTraceViewRequest());
    await checkLoadTraceViewEvents(uploadTracesComponent);

    await mediator.onWinscopeEvent(new AppRefreshDumpsRequest());
    expect(collectTracesComponent.onWinscopeEvent).toHaveBeenCalled();
  });

  //TODO: test "bugreport data from cross-tool protocol" when FileUtils is fully compatible with
  //      Node.js (b/262269229). FileUtils#unzipFile() currently can't execute on Node.js.

  //TODO: test "data from ABT chrome extension" when FileUtils is fully compatible with Node.js
  //      (b/262269229).

  it('handles start download event from remote tool', async () => {
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(0);

    await mediator.onWinscopeEvent(new RemoteToolDownloadStart());
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(1);
  });

  it('handles empty downloaded files from remote tool', async () => {
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(0);

    // Pass files even if empty so that the upload component will update the progress bar
    // and display error messages
    await mediator.onWinscopeEvent(new RemoteToolFilesReceived([]));
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(1);
  });

  it('notifies overlay viewer of expanded timeline toggle change', async () => {
    await loadFiles();
    await loadTraceView();
    const event = new ExpandedTimelineToggled(true);
    await mediator.onWinscopeEvent(new ExpandedTimelineToggled(true));
    expect(viewerOverlay.onWinscopeEvent).toHaveBeenCalledWith(event);
  });

  it('propagates trace position update', async () => {
    await loadFiles();
    await loadTraceView();

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      POSITION_10,
    );

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_11));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      POSITION_11,
    );
  });

  it('propagates trace position update according to timezone', async () => {
    const timezoneInfo: TimezoneInfo = {
      timezone: 'Asia/Kolkata',
      locale: 'en-US',
    };
    const converter = new TimestampConverter(timezoneInfo, 0n);
    spyOn(tracePipeline, 'getTimestampConverter').and.returnValue(converter);
    await loadFiles();
    await loadTraceView();

    // notify position
    resetSpyCalls();
    const expectedPosition = TracePosition.fromTimestamp(
      converter.makeTimestampFromRealNs(10n),
    );
    await mediator.onWinscopeEvent(new TracePositionUpdate(expectedPosition));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      expectedPosition,
      POSITION_10,
    );
  });

  it('propagates trace position update and updates timeline data', async () => {
    await loadFiles();
    await loadTraceView();

    // notify position
    resetSpyCalls();
    const finalTimestampNs = timelineData.getFullTimeRange().to.getValueNs();
    const timestamp =
      TimestampConverterUtils.makeRealTimestamp(finalTimestampNs);
    const position = TracePosition.fromTimestamp(timestamp);

    await mediator.onWinscopeEvent(new TracePositionUpdate(position, true));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      position,
    );
    expect(
      assertDefined(timelineData.getCurrentPosition()).timestamp.getValueNs(),
    ).toEqual(finalTimestampNs);
  });

  it("initializes viewers' trace position also when loaded traces have no valid timestamps", async () => {
    const dumpFile = await UnitTestUtils.getFixtureFile(
      'traces/dump_WindowManager.pb',
    );
    await mediator.onWinscopeEvent(new AppFilesUploaded([dumpFile]));

    resetSpyCalls();
    await mediator.onWinscopeEvent(new AppTraceViewRequest());
    await checkLoadTraceViewEvents(uploadTracesComponent);
    userNotifierChecker.expectNotified([]);
  });

  it('filters traces without visualization on loading viewers', async () => {
    const fileWithoutVisualization = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    await loadFiles();
    await mediator.onWinscopeEvent(
      new AppFilesUploaded([fileWithoutVisualization]),
    );
    await loadTraceView();
  });

  it('warns user if frame mapping fails', async () => {
    const errorMsg = 'frame mapping failed';
    spyOn(tracePipeline, 'buildTraces').and.throwError(errorMsg);
    const dumpFile = await UnitTestUtils.getFixtureFile(
      'traces/dump_WindowManager.pb',
    );
    await mediator.onWinscopeEvent(new AppFilesUploaded([dumpFile]));

    resetSpyCalls();
    await mediator.onWinscopeEvent(new AppTraceViewRequest());
    await checkLoadTraceViewEvents(uploadTracesComponent);
    userNotifierChecker.expectNotified([new IncompleteFrameMapping(errorMsg)]);
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      tracePipeline.getTimestampConverter().setRealToMonotonicTimeOffsetNs(0n);
      await loadFiles();
      await loadTraceView();
      const traceSfEntry = assertDefined(
        tracePipeline.getTraces().getTrace(TraceType.SURFACE_FLINGER),
      ).getEntry(2);

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(() => traceSfEntry.getTimestamp()),
      );

      checkTracePositionUpdateEvents(
        [viewerStub0, viewerOverlay, timelineComponent],
        [],
        TracePosition.fromTraceEntry(traceSfEntry),
      );
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      tracePipeline.getTimestampConverter().setRealToMonotonicTimeOffsetNs(0n);
      await loadFiles();
      await loadTraceView();

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(() => TIMESTAMP_10),
      );
      checkTracePositionUpdateEvents(
        [viewerStub0, viewerOverlay, timelineComponent],
        [],
      );
    });

    it('defers trace position propagation till traces are loaded and visualized', async () => {
      // ensure converter has been used to create real timestamps
      tracePipeline.getTimestampConverter().makeTimestampFromRealNs(0n);

      // load files but do not load trace view
      await loadFiles();
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();
      const traceSf = assertDefined(
        tracePipeline.getTraces().getTrace(TraceType.SURFACE_FLINGER),
      );

      // keep timestamp for later
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(() =>
          traceSf.getEntry(1).getTimestamp(),
        ),
      );
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();

      // keep timestamp for later (replace previous one)
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(() =>
          traceSf.getEntry(2).getTimestamp(),
        ),
      );
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();

      // apply timestamp
      await loadTraceView();

      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledWith(
        makeExpectedTracePositionUpdate(
          TracePosition.fromTraceEntry(traceSf.getEntry(2)),
        ),
      );
    });
  });

  describe('tab view switches', () => {
    it('forwards switch notifications', async () => {
      await loadFiles();
      await loadTraceView();
      resetSpyCalls();

      const view = viewerStub1.getViews()[0];
      await mediator.onWinscopeEvent(new TabbedViewSwitched(view));
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledWith(
        new ActiveTraceChanged(view.traces[0]),
      );
      userNotifierChecker.expectNotified([]);
      userNotifierChecker.reset();
      const viewDump = viewerDump.getViews()[0];
      await mediator.onWinscopeEvent(new TabbedViewSwitched(viewDump));
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalledWith(
        new ActiveTraceChanged(viewDump.traces[0]),
      );
      userNotifierChecker.expectNotified([]);
    });

    it('forwards switch requests from viewers to trace view component', async () => {
      await loadFiles();
      await loadTraceView();
      expect(traceViewComponent.onWinscopeEvent).not.toHaveBeenCalled();

      await viewerStub0.emitAppEventForTesting(
        new TabbedViewSwitchRequest(traceSf),
      );
      expect(traceViewComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TabbedViewSwitchRequest(traceSf),
      );
      userNotifierChecker.expectNotified([]);
    });
  });

  it('notifies only visible viewers about trace position updates', async () => {
    await loadFiles();
    await loadTraceView();

    // Position update -> update only visible viewers
    // Note: Viewer 0 is visible (gets focus) upon UI initialization
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      POSITION_10,
    );

    // Tab switch -> update only newly visible viewers
    // Note: overlay viewer is considered always visible
    resetSpyCalls();
    await mediator.onWinscopeEvent(
      new TabbedViewSwitched(viewerStub1.getViews()[0]),
    );
    checkTracePositionUpdateEvents(
      [viewerStub1, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
      undefined,
      undefined,
      true,
    );
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledWith(
      new ActiveTraceChanged(viewerStub1.getViews()[0].traces[0]),
    );

    // Position update -> update only visible viewers
    // Note: overlay viewer is considered always visible
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    checkTracePositionUpdateEvents(
      [viewerStub1, viewerOverlay, timelineComponent, crossToolProtocol],
      [],
    );
  });

  it('notifies timeline of dark mode toggle', async () => {
    const event = new DarkModeToggled(true);
    await mediator.onWinscopeEvent(event);
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(event);
  });

  it('notifies timeline of active trace change', async () => {
    expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();

    await mediator.onWinscopeEvent(new ActiveTraceChanged(traceWm));
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new ActiveTraceChanged(traceWm),
    );
  });

  it('notifies user of no trace targets selected', async () => {
    await mediator.onWinscopeEvent(new NoTraceTargetsSelectedEvent());
    userNotifierChecker.expectNotified([new NoTraceTargetsSelected()]);
  });

  it('notifies correct viewer of filter preset requests', async () => {
    await loadFiles();
    await loadTraceView();
    resetSpyCalls();

    const saveRequest = new FilterPresetSaveRequest(
      'test_preset',
      TraceType.SURFACE_FLINGER,
    );
    await mediator.onWinscopeEvent(saveRequest);

    const applyRequest = new FilterPresetApplyRequest(
      'test_preset',
      TraceType.WINDOW_MANAGER,
    );
    await mediator.onWinscopeEvent(applyRequest);

    await mediator.onWinscopeEvent(
      new FilterPresetSaveRequest('test_preset', TraceType.PROTO_LOG),
    );
    await mediator.onWinscopeEvent(
      new FilterPresetApplyRequest('test_preset', TraceType.PROTO_LOG),
    );

    expect(viewerStub0.onWinscopeEvent).toHaveBeenCalledOnceWith(saveRequest);
    expect(viewerStub1.onWinscopeEvent).toHaveBeenCalledOnceWith(applyRequest);
  });

  async function loadFiles(files = inputFiles) {
    await mediator.onWinscopeEvent(new AppFilesUploaded(files));
    userNotifierChecker.expectNone();
    reassignViewerStubTrace(viewerStub0);
    reassignViewerStubTrace(viewerStub1);
  }

  function reassignViewerStubTrace(viewerStub: ViewerStub) {
    const viewerStubTraces = viewerStub.getViews()[0].traces;
    viewerStubTraces[0] = tracePipeline
      .getTraces()
      .getTrace(viewerStubTraces[0].type) as Trace<object>;
  }

  async function loadTraceView() {
    // Simulate "View traces" button click
    resetSpyCalls();
    await mediator.onWinscopeEvent(new AppTraceViewRequest());

    await checkLoadTraceViewEvents(uploadTracesComponent);

    // Simulate notification of TraceViewComponent about initially selected/focused tab
    resetSpyCalls();
    await mediator.onWinscopeEvent(
      new TabbedViewSwitched(viewerStub0.getViews()[0]),
    );

    expect(viewerStub0.onWinscopeEvent).toHaveBeenCalledOnceWith(
      makeExpectedTracePositionUpdate(),
    );
    expect(viewerStub1.onWinscopeEvent).not.toHaveBeenCalled();
    userNotifierChecker.expectNotified([]);
  }

  async function checkLoadTraceViewEvents(progressListener: ProgressListener) {
    expect(progressListener.onProgressUpdate).toHaveBeenCalled();
    expect(progressListener.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new ViewersLoaded([viewerStub0, viewerStub1, viewerOverlay, viewerDump]),
    );

    // Mediator triggers the viewers initialization
    // by sending them a "trace position update" event
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerStub1, viewerOverlay, viewerDump, timelineComponent],
      [],
    );
  }

  function checkTracePositionUpdateEvents(
    listenersToBeNotified: WinscopeEventListener[],
    userNotifications: UserWarning[],
    position?: TracePosition,
    crossToolProtocolPosition = position,
    multipleTimelineEvents = false,
  ) {
    userNotifierChecker.expectNotified(userNotifications);
    const event = makeExpectedTracePositionUpdate(position);
    const crossToolProtocolEvent =
      crossToolProtocolPosition !== position
        ? makeExpectedTracePositionUpdate(crossToolProtocolPosition)
        : event;
    tracePositionUpdateListeners.forEach((listener) => {
      const isVisible = listenersToBeNotified.includes(listener);
      if (isVisible) {
        const expected =
          listener === crossToolProtocol ? crossToolProtocolEvent : event;
        if (multipleTimelineEvents && listener === timelineComponent) {
          expect(listener.onWinscopeEvent).toHaveBeenCalledWith(expected);
        } else {
          expect(listener.onWinscopeEvent).toHaveBeenCalledOnceWith(expected);
        }
      } else {
        expect(listener.onWinscopeEvent).not.toHaveBeenCalled();
      }
    });
  }

  function resetSpyCalls() {
    spies.forEach((spy) => {
      spy.calls.reset();
    });
    userNotifierChecker.reset();
  }

  function makeExpectedTracePositionUpdate(
    tracePosition?: TracePosition,
  ): WinscopeEvent {
    if (tracePosition !== undefined) {
      return new TracePositionUpdate(tracePosition);
    }
    return {type: WinscopeEventType.TRACE_POSITION_UPDATE} as WinscopeEvent;
  }

  function tracePositionUpdateEqualityTester(
    first: any,
    second: any,
  ): boolean | undefined {
    if (
      first instanceof TracePositionUpdate &&
      second instanceof TracePositionUpdate
    ) {
      return testTracePositionUpdates(first, second);
    }
    if (
      first instanceof TracePositionUpdate &&
      second.type === WinscopeEventType.TRACE_POSITION_UPDATE
    ) {
      return first.type === second.type;
    }
    return undefined;
  }

  function testTracePositionUpdates(
    event: TracePositionUpdate,
    expectedEvent: TracePositionUpdate,
  ): boolean {
    if (event.type !== expectedEvent.type) return false;
    if (
      event.position.timestamp.getValueNs() !==
      expectedEvent.position.timestamp.getValueNs()
    ) {
      return false;
    }
    if (event.position.frame !== expectedEvent.position.frame) return false;
    return true;
  }
});

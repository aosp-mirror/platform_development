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
import {
  NO_TIMEZONE_OFFSET_FACTORY,
  TimestampFactory,
} from 'common/timestamp_factory';
import {ProgressListener} from 'messaging/progress_listener';
import {ProgressListenerStub} from 'messaging/progress_listener_stub';
import {UserNotificationListener} from 'messaging/user_notification_listener';
import {UserNotificationListenerStub} from 'messaging/user_notification_listener_stub';
import {
  AppFilesCollected,
  AppFilesUploaded,
  AppInitialized,
  AppTraceViewRequest,
  BuganizerAttachmentsDownloaded,
  BuganizerAttachmentsDownloadStart,
  RemoteToolTimestampReceived,
  TabbedViewSwitched,
  TabbedViewSwitchRequest,
  TracePositionUpdate,
  ViewersLoaded,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {WinscopeEventEmitter} from 'messaging/winscope_event_emitter';
import {WinscopeEventEmitterStub} from 'messaging/winscope_event_emitter_stub';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {WinscopeEventListenerStub} from 'messaging/winscope_event_listener_stub';
import {MockStorage} from 'test/unit/mock_storage';
import {UnitTestUtils} from 'test/unit/utils';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {ViewType} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const viewerStub0 = new ViewerStub('Title0', undefined, [
    TraceType.SURFACE_FLINGER,
  ]);
  const viewerStub1 = new ViewerStub('Title1', undefined, [
    TraceType.WINDOW_MANAGER,
  ]);
  const viewerOverlay = new ViewerStub(
    'TitleOverlay',
    undefined,
    [TraceType.WINDOW_MANAGER],
    ViewType.OVERLAY,
  );

  let inputFiles: File[];
  let userNotificationListener: UserNotificationListener;
  let tracePipeline: TracePipeline;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: WinscopeEventEmitter & WinscopeEventListener;
  let crossToolProtocol: WinscopeEventEmitter & WinscopeEventListener;
  let appComponent: WinscopeEventListener;
  let timelineComponent: WinscopeEventEmitter & WinscopeEventListener;
  let uploadTracesComponent: ProgressListenerStub;
  let collectTracesComponent: ProgressListenerStub;
  let traceViewComponent: WinscopeEventEmitter & WinscopeEventListener;
  let mediator: Mediator;
  let spies: Array<jasmine.Spy<any>>;

  const viewers = [viewerStub0, viewerStub1, viewerOverlay];
  let tracePositionUpdateListeners: WinscopeEventListener[];

  const TIMESTAMP_10 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n);
  const TIMESTAMP_11 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(11n);

  const POSITION_10 = TracePosition.fromTimestamp(TIMESTAMP_10);
  const POSITION_11 = TracePosition.fromTimestamp(TIMESTAMP_11);

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
  });

  beforeEach(async () => {
    jasmine.addCustomEqualityTester(tracePositionUpdateEqualityTester);
    userNotificationListener = new UserNotificationListenerStub();
    tracePipeline = new TracePipeline();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    crossToolProtocol = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    appComponent = new WinscopeEventListenerStub();
    timelineComponent = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub(),
    );
    uploadTracesComponent = new ProgressListenerStub();
    collectTracesComponent = new ProgressListenerStub();
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
      userNotificationListener,
      new MockStorage(),
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
      spyOn(crossToolProtocol, 'onWinscopeEvent'),
      spyOn(timelineComponent, 'onWinscopeEvent'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(traceViewComponent, 'onWinscopeEvent'),
      spyOn(uploadTracesComponent, 'onProgressUpdate'),
      spyOn(uploadTracesComponent, 'onOperationFinished'),
      spyOn(userNotificationListener, 'onErrors'),
      spyOn(viewerStub0, 'onWinscopeEvent'),
      spyOn(viewerStub1, 'onWinscopeEvent'),
      spyOn(viewerOverlay, 'onWinscopeEvent'),
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
  });

  it('handles collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(new AppFilesCollected(inputFiles));
    await checkLoadTraceViewEvents(collectTracesComponent);
  });

  //TODO: test "bugreport data from cross-tool protocol" when FileUtils is fully compatible with
  //      Node.js (b/262269229). FileUtils#unzipFile() currently can't execute on Node.js.

  //TODO: test "data from ABT chrome extension" when FileUtils is fully compatible with Node.js
  //      (b/262269229).

  it('handles start download event from ABT chrome extension', async () => {
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(0);

    await mediator.onWinscopeEvent(new BuganizerAttachmentsDownloadStart());
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(1);
  });

  it('handles empty downloaded files from ABT chrome extension', async () => {
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(0);

    // Pass files even if empty so that the upload component will update the progress bar
    // and display error messages
    await mediator.onWinscopeEvent(new BuganizerAttachmentsDownloaded([]));
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(1);
  });

  it('propagates trace position update', async () => {
    await loadFiles();
    await loadTraceView();

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      POSITION_10,
    );

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_11));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
      POSITION_11,
    );
  });

  it('propagates trace position update according to timezone', async () => {
    const timezoneInfo = {
      timezone: 'Asia/Kolkata',
      locale: 'en-US',
    };
    const factory = new TimestampFactory(timezoneInfo);
    spyOn(tracePipeline, 'getTimestampFactory').and.returnValue(factory);
    await loadFiles();
    await loadTraceView();

    // notify position
    resetSpyCalls();
    const expectedPosition = TracePosition.fromTimestamp(
      factory.makeRealTimestamp(10n),
    );
    await mediator.onWinscopeEvent(new TracePositionUpdate(expectedPosition));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
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
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(finalTimestampNs);
    const position = TracePosition.fromTimestamp(timestamp);

    await mediator.onWinscopeEvent(new TracePositionUpdate(position, true));
    checkTracePositionUpdateEvents(
      [viewerStub0, viewerOverlay, timelineComponent, crossToolProtocol],
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
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      await loadFiles();
      await loadTraceView();

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(TIMESTAMP_10.getValueNs()),
      );
      checkTracePositionUpdateEvents(
        [viewerStub0, viewerOverlay, timelineComponent],
        POSITION_10,
      );

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(TIMESTAMP_11.getValueNs()),
      );
      checkTracePositionUpdateEvents(
        [viewerStub0, viewerOverlay, timelineComponent],
        POSITION_11,
      );
    });

    it('propagates trace position update according to timezone', async () => {
      const timezoneInfo = {
        timezone: 'Asia/Kolkata',
        locale: 'en-US',
      };
      const factory = new TimestampFactory(timezoneInfo);
      spyOn(tracePipeline, 'getTimestampFactory').and.returnValue(factory);
      await loadFiles();
      await loadTraceView();

      // receive timestamp
      resetSpyCalls();

      const expectedPosition = TracePosition.fromTimestamp(
        factory.makeRealTimestamp(10n),
      );
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(10n));
      checkTracePositionUpdateEvents(
        [viewerStub0, viewerOverlay, timelineComponent],
        expectedPosition,
      );
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      await loadFiles();
      await loadTraceView();

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(TIMESTAMP_10.getValueNs()),
      );
      checkTracePositionUpdateEvents([
        viewerStub0,
        viewerOverlay,
        timelineComponent,
      ]);
    });

    it('defers trace position propagation till traces are loaded and visualized', async () => {
      // keep timestamp for later
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(TIMESTAMP_10.getValueNs()),
      );
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();

      // keep timestamp for later (replace previous one)
      await mediator.onWinscopeEvent(
        new RemoteToolTimestampReceived(TIMESTAMP_11.getValueNs()),
      );
      expect(timelineComponent.onWinscopeEvent).not.toHaveBeenCalled();

      // apply timestamp
      await loadFiles();
      await loadTraceView();

      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledWith(
        makeExpectedTracePositionUpdate(POSITION_11),
      );
    });
  });

  describe('tab view switches', () => {
    it('forwards switch notifications', async () => {
      await loadFiles();
      await loadTraceView();
      resetSpyCalls();

      const view = viewerStub0.getViews()[0];
      await mediator.onWinscopeEvent(new TabbedViewSwitched(view));
      expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TabbedViewSwitched(view),
      );
    });

    it('forwards switch requests from viewers to trace view component', async () => {
      await loadFiles();
      await loadTraceView();
      expect(traceViewComponent.onWinscopeEvent).not.toHaveBeenCalled();

      await viewerStub0.emitAppEventForTesting(
        new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE),
      );
      expect(traceViewComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE),
      );
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
      POSITION_10,
    );

    // Tab switch -> update only newly visible viewers
    // Note: overlay viewer is considered always visible
    resetSpyCalls();
    await mediator.onWinscopeEvent(
      new TabbedViewSwitched(viewerStub1.getViews()[0]),
    );
    checkTracePositionUpdateEvents([
      viewerStub1,
      viewerOverlay,
      timelineComponent,
      crossToolProtocol,
    ]);

    // Position update -> update only visible viewers
    // Note: overlay viewer is considered always visible
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    checkTracePositionUpdateEvents([
      viewerStub1,
      viewerOverlay,
      timelineComponent,
      crossToolProtocol,
    ]);
  });

  async function loadFiles() {
    await mediator.onWinscopeEvent(new AppFilesUploaded(inputFiles));
    expect(userNotificationListener.onErrors).not.toHaveBeenCalled();
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
  }

  async function checkLoadTraceViewEvents(progressListener: ProgressListener) {
    expect(progressListener.onProgressUpdate).toHaveBeenCalled();
    expect(progressListener.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new ViewersLoaded([viewerStub0, viewerStub1, viewerOverlay]),
    );

    // Mediator triggers the viewers initialization
    // by sending them a "trace position update" event
    checkTracePositionUpdateEvents([
      viewerStub0,
      viewerStub1,
      viewerOverlay,
      timelineComponent,
    ]);
  }

  function checkTracePositionUpdateEvents(
    listenersToBeNotified: WinscopeEventListener[],
    position?: TracePosition,
    crossToolProtocolPosition = position,
  ) {
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
        expect(listener.onWinscopeEvent).toHaveBeenCalledOnceWith(expected);
      } else {
        expect(listener.onWinscopeEvent).not.toHaveBeenCalled();
      }
    });
  }

  function resetSpyCalls() {
    spies.forEach((spy) => {
      spy.calls.reset();
    });
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

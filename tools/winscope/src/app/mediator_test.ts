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

import {FunctionUtils} from 'common/function_utils';
import {RealTimestamp} from 'common/time';
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
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const viewerStub = new ViewerStub('Title');
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

  const TIMESTAMP_10 = new RealTimestamp(10n);
  const TIMESTAMP_11 = new RealTimestamp(11n);

  const POSITION_10 = TracePosition.fromTimestamp(TIMESTAMP_10);
  const POSITION_11 = TracePosition.fromTimestamp(TIMESTAMP_11);

  beforeAll(async () => {
    inputFiles = [
      await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb'),
      await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/WindowManager.pb'),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
      ),
    ];
  });

  beforeEach(async () => {
    userNotificationListener = new UserNotificationListenerStub();
    tracePipeline = new TracePipeline();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub()
    );
    crossToolProtocol = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub()
    );
    appComponent = new WinscopeEventListenerStub();
    timelineComponent = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub()
    );
    uploadTracesComponent = new ProgressListenerStub();
    collectTracesComponent = new ProgressListenerStub();
    traceViewComponent = FunctionUtils.mixin(
      new WinscopeEventEmitterStub(),
      new WinscopeEventListenerStub()
    );
    mediator = new Mediator(
      tracePipeline,
      timelineData,
      abtChromeExtensionProtocol,
      crossToolProtocol,
      appComponent,
      userNotificationListener,
      new MockStorage()
    );
    mediator.setTimelineComponent(timelineComponent);
    mediator.setUploadTracesComponent(uploadTracesComponent);
    mediator.setCollectTracesComponent(collectTracesComponent);
    mediator.setTraceViewComponent(traceViewComponent);

    spyOn(ViewerFactory.prototype, 'createViewers').and.returnValue([viewerStub]);

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
      spyOn(viewerStub, 'onWinscopeEvent'),
    ];
  });

  it('notifies ABT chrome extension about app initialization', async () => {
    expect(abtChromeExtensionProtocol.onWinscopeEvent).not.toHaveBeenCalled();

    await mediator.onWinscopeEvent(new AppInitialized());
    expect(abtChromeExtensionProtocol.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new AppInitialized()
    );
  });

  it('handles uploaded traces from Winscope', async () => {
    await mediator.onWinscopeEvent(new AppFilesUploaded(inputFiles));

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).not.toHaveBeenCalled();
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();
    expect(viewerStub.onWinscopeEvent).not.toHaveBeenCalled();

    resetSpyCalls();
    await mediator.onWinscopeEvent(new AppTraceViewRequest());

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(new ViewersLoaded([viewerStub]));

    // propagates trace position on viewers creation
    expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TRACE_POSITION_UPDATE,
      } as WinscopeEvent)
    );
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: WinscopeEventType.TRACE_POSITION_UPDATE,
      } as WinscopeEvent)
    );
    expect(crossToolProtocol.onWinscopeEvent).toHaveBeenCalledTimes(0);
  });

  it('handles collected traces from Winscope', async () => {
    await mediator.onWinscopeEvent(new AppFilesCollected(inputFiles));

    expect(collectTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(collectTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(new ViewersLoaded([viewerStub]));

    // propagates trace position on viewers creation
    expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
      makeExpectedEvent(WinscopeEventType.TRACE_POSITION_UPDATE)
    );
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      makeExpectedEvent(WinscopeEventType.TRACE_POSITION_UPDATE)
    );
    expect(crossToolProtocol.onWinscopeEvent).toHaveBeenCalledTimes(0);
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
    await mediator.onWinscopeEvent(new AppTraceViewRequest());

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_10));
    expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_10)
    );
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_10)
    );
    expect(crossToolProtocol.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_10)
    );

    // notify position
    resetSpyCalls();
    await mediator.onWinscopeEvent(new TracePositionUpdate(POSITION_11));
    expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_11)
    );
    expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_11)
    );
    expect(crossToolProtocol.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TracePositionUpdate(POSITION_11)
    );
  });

  it("initializes viewers' trace position also when loaded traces have no valid timestamps", async () => {
    const dumpFile = await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb');
    await mediator.onWinscopeEvent(new AppFilesUploaded([dumpFile]));
    await mediator.onWinscopeEvent(new AppTraceViewRequest());

    expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
      makeExpectedEvent(WinscopeEventType.TRACE_POSITION_UPDATE)
    );
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      await loadFiles();
      await mediator.onWinscopeEvent(new AppTraceViewRequest());

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(TIMESTAMP_10));
      expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_10)
      );
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_10)
      );

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(TIMESTAMP_11));
      expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_11)
      );
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_11)
      );
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      await loadFiles();
      await mediator.onWinscopeEvent(new AppTraceViewRequest());

      // receive timestamp
      resetSpyCalls();
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(TIMESTAMP_10));
      expect(viewerStub.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_10)
      );
      expect(crossToolProtocol.onWinscopeEvent).toHaveBeenCalledTimes(0);
    });

    it('defers trace position propagation till traces are loaded and visualized', async () => {
      // keep timestamp for later
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(TIMESTAMP_10));
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledTimes(0);

      // keep timestamp for later (replace previous one)
      await mediator.onWinscopeEvent(new RemoteToolTimestampReceived(TIMESTAMP_11));
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledTimes(0);

      // apply timestamp
      await loadFiles();
      await mediator.onWinscopeEvent(new AppTraceViewRequest());
      expect(timelineComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
        new TracePositionUpdate(POSITION_11)
      );
    });
  });

  it("forwards 'switched view' events", async () => {
    const view = viewerStub.getViews()[0];
    expect(appComponent.onWinscopeEvent).not.toHaveBeenCalled();

    await mediator.onWinscopeEvent(new TabbedViewSwitched(view));
    expect(appComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(new TabbedViewSwitched(view));
  });

  it("forwards 'switch view' requests from viewers to trace view component", async () => {
    await mediator.onWinscopeEvent(new AppTraceViewRequest());
    expect(traceViewComponent.onWinscopeEvent).not.toHaveBeenCalled();

    await viewerStub.emitAppEventForTesting(new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE));
    expect(traceViewComponent.onWinscopeEvent).toHaveBeenCalledOnceWith(
      new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE)
    );
  });

  async function loadFiles() {
    await mediator.onWinscopeEvent(new AppFilesUploaded(inputFiles));
    expect(userNotificationListener.onErrors).not.toHaveBeenCalled();
  }

  function resetSpyCalls() {
    spies.forEach((spy) => {
      spy.calls.reset();
    });
  }

  function makeExpectedEvent(type: WinscopeEventType): jasmine.ObjectContaining<any> {
    return jasmine.objectContaining({
      type,
    } as WinscopeEvent);
  }
});

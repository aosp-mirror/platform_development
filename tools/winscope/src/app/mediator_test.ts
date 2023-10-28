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

import {AbtChromeExtensionProtocolStub} from 'abt_chrome_extension/abt_chrome_extension_protocol_stub';
import {RealTimestamp} from 'common/time';
import {CrossToolProtocolStub} from 'cross_tool/cross_tool_protocol_stub';
import {AppEventListenerEmitterStub} from 'interfaces/app_event_listener_emitter_stub';
import {ProgressListenerStub} from 'interfaces/progress_listener_stub';
import {UserNotificationListenerStub} from 'interfaces/user_notification_listener_stub';
import {MockStorage} from 'test/unit/mock_storage';
import {UnitTestUtils} from 'test/unit/utils';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {AppEvent, AppEventType, TabbedViewSwitched, TabbedViewSwitchRequest} from './app_event';
import {AppComponentStub} from './components/app_component_stub';
import {SnackBarOpenerStub} from './components/snack_bar_opener_stub';
import {TimelineComponentStub} from './components/timeline/timeline_component_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const viewerStub = new ViewerStub('Title');
  let inputFiles: File[];
  let userNotificationListener: UserNotificationListenerStub;
  let tracePipeline: TracePipeline;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: AbtChromeExtensionProtocolStub;
  let crossToolProtocol: CrossToolProtocolStub;
  let appComponent: AppComponentStub;
  let timelineComponent: TimelineComponentStub;
  let uploadTracesComponent: ProgressListenerStub;
  let collectTracesComponent: ProgressListenerStub;
  let traceViewComponent: AppEventListenerEmitterStub;
  let snackBarOpener: SnackBarOpenerStub;
  let mediator: Mediator;

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
    tracePipeline = new TracePipeline(userNotificationListener);
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = new AbtChromeExtensionProtocolStub();
    crossToolProtocol = new CrossToolProtocolStub();
    appComponent = new AppComponentStub();
    timelineComponent = new TimelineComponentStub();
    uploadTracesComponent = new ProgressListenerStub();
    collectTracesComponent = new ProgressListenerStub();
    traceViewComponent = new AppEventListenerEmitterStub();
    snackBarOpener = new SnackBarOpenerStub();
    mediator = new Mediator(
      tracePipeline,
      timelineData,
      abtChromeExtensionProtocol,
      crossToolProtocol,
      appComponent,
      snackBarOpener,
      new MockStorage()
    );
    mediator.setTimelineComponent(timelineComponent);
    mediator.setUploadTracesComponent(uploadTracesComponent);
    mediator.setCollectTracesComponent(collectTracesComponent);
    mediator.setTraceViewComponent(traceViewComponent);

    spyOn(ViewerFactory.prototype, 'createViewers').and.returnValue([viewerStub]);
  });

  it('handles uploaded traces from Winscope', async () => {
    const spies = [
      spyOn(uploadTracesComponent, 'onProgressUpdate'),
      spyOn(uploadTracesComponent, 'onOperationFinished'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(appComponent, 'onTraceDataLoaded'),
      spyOn(viewerStub, 'onAppEvent'),
      spyOn(timelineComponent, 'onTracePositionUpdate'),
      spyOn(crossToolProtocol, 'sendTimestamp'),
    ];

    await mediator.onWinscopeFilesUploaded(inputFiles);

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).not.toHaveBeenCalled();
    expect(appComponent.onTraceDataLoaded).not.toHaveBeenCalled();
    expect(viewerStub.onAppEvent).not.toHaveBeenCalled();

    spies.forEach((spy) => {
      spy.calls.reset();
    });
    await mediator.onWinscopeViewTracesRequest();

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);

    // propagates trace position on viewers creation
    expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TRACE_POSITION_UPDATE,
      } as AppEvent)
    );
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
  });

  it('handles collected traces from Winscope', async () => {
    const spies = [
      spyOn(collectTracesComponent, 'onProgressUpdate'),
      spyOn(collectTracesComponent, 'onOperationFinished'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(appComponent, 'onTraceDataLoaded'),
      spyOn(viewerStub, 'onAppEvent'),
      spyOn(timelineComponent, 'onTracePositionUpdate'),
      spyOn(crossToolProtocol, 'sendTimestamp'),
    ];

    await mediator.onWinscopeFilesCollected(inputFiles);

    expect(collectTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(collectTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);

    // propagates trace position on viewers creation
    expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TRACE_POSITION_UPDATE,
      } as AppEvent)
    );
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
  });

  //TODO: test "bugreport data from cross-tool protocol" when FileUtils is fully compatible with
  //      Node.js (b/262269229). FileUtils#unzipFile() currently can't execute on Node.js.

  //TODO: test "data from ABT chrome extension" when FileUtils is fully compatible with Node.js
  //      (b/262269229).

  it('handles start download event from ABT chrome extension', () => {
    spyOn(uploadTracesComponent, 'onProgressUpdate');
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(0);

    abtChromeExtensionProtocol.onBuganizerAttachmentsDownloadStart();
    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalledTimes(1);
  });

  it('handles empty downloaded files from ABT chrome extension', async () => {
    spyOn(uploadTracesComponent, 'onOperationFinished');
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(0);

    // Pass files even if empty so that the upload component will update the progress bar
    // and display error messages
    await abtChromeExtensionProtocol.onBuganizerAttachmentsDownloaded([]);
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalledTimes(1);
  });

  it('propagates trace position update from timeline component', async () => {
    await loadFiles();
    await mediator.onWinscopeViewTracesRequest();

    spyOn(viewerStub, 'onAppEvent');
    spyOn(timelineComponent, 'onTracePositionUpdate');
    spyOn(crossToolProtocol, 'sendTimestamp');
    expect(viewerStub.onAppEvent).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // notify position
    await mediator.onTimelineTracePositionUpdate(POSITION_10);
    expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TRACE_POSITION_UPDATE,
      } as AppEvent)
    );
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify position
    await mediator.onTimelineTracePositionUpdate(POSITION_11);
    expect(viewerStub.onAppEvent).toHaveBeenCalledTimes(2);
    expect(viewerStub.onAppEvent).toHaveBeenCalledWith(
      jasmine.objectContaining({
        type: AppEventType.TRACE_POSITION_UPDATE,
      } as AppEvent)
    );
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(2);
  });

  it("initializes viewers' trace position also when loaded traces have no valid timestamps", async () => {
    spyOn(viewerStub, 'onAppEvent');

    const dumpFile = await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb');
    await mediator.onWinscopeFilesUploaded([dumpFile]);
    await mediator.onWinscopeViewTracesRequest();

    expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TRACE_POSITION_UPDATE,
      } as AppEvent)
    );
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      await loadFiles();
      await mediator.onWinscopeViewTracesRequest();

      spyOn(viewerStub, 'onAppEvent');
      spyOn(timelineComponent, 'onTracePositionUpdate');
      expect(viewerStub.onAppEvent).toHaveBeenCalledTimes(0);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({
          type: AppEventType.TRACE_POSITION_UPDATE,
        } as AppEvent)
      );
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_11);
      expect(viewerStub.onAppEvent).toHaveBeenCalledTimes(2);
      expect(viewerStub.onAppEvent).toHaveBeenCalledWith(
        jasmine.objectContaining({
          type: AppEventType.TRACE_POSITION_UPDATE,
        } as AppEvent)
      );
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      await loadFiles();
      await mediator.onWinscopeViewTracesRequest();

      spyOn(viewerStub, 'onAppEvent');
      spyOn(crossToolProtocol, 'sendTimestamp');

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onAppEvent).toHaveBeenCalledOnceWith(
        jasmine.objectContaining({
          type: AppEventType.TRACE_POSITION_UPDATE,
        } as AppEvent)
      );
      expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
    });

    it('defers trace position propagation till traces are loaded and visualized', async () => {
      spyOn(timelineComponent, 'onTracePositionUpdate');

      // keep timestamp for later
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // keep timestamp for later (replace previous one)
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_11);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // apply timestamp
      await loadFiles();
      await mediator.onWinscopeViewTracesRequest();
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledWith(POSITION_11);
    });
  });

  it("forwards 'switched view' events", async () => {
    //TODO (after integrating also the timeline with AppEvent):
    // spyOn(timelineComponent, 'onAppEvent') + checks

    spyOn(appComponent, 'onAppEvent');
    expect(appComponent.onAppEvent).not.toHaveBeenCalled();

    const event = new TabbedViewSwitched(viewerStub.getViews()[0]);
    await mediator.onTraceViewAppEvent(event);
    expect(appComponent.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TABBED_VIEW_SWITCHED,
      } as AppEvent)
    );
  });

  it("forwards 'switch view' requests from viewers to trace view component", async () => {
    await mediator.onWinscopeViewTracesRequest();

    spyOn(traceViewComponent, 'onAppEvent');
    expect(traceViewComponent.onAppEvent).not.toHaveBeenCalled();

    await viewerStub.emitAppEventForTesting(new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE));

    expect(traceViewComponent.onAppEvent).toHaveBeenCalledOnceWith(
      jasmine.objectContaining({
        type: AppEventType.TABBED_VIEW_SWITCH_REQUEST,
      } as AppEvent)
    );
  });

  async function loadFiles() {
    const parserErrorsSpy = spyOn(userNotificationListener, 'onParserErrors');
    await tracePipeline.loadFiles(inputFiles);
    expect(parserErrorsSpy).not.toHaveBeenCalled();
  }
});

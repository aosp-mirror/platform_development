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
import {CrossToolProtocolStub} from 'cross_tool/cross_tool_protocol_stub';
import {ProgressListenerStub} from 'interfaces/progress_listener_stub';
import {MockStorage} from 'test/unit/mock_storage';
import {UnitTestUtils} from 'test/unit/utils';
import {RealTimestamp} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TracePosition} from 'trace/trace_position';
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {AppComponentStub} from './components/app_component_stub';
import {SnackBarOpenerStub} from './components/snack_bar_opener_stub';
import {TimelineComponentStub} from './components/timeline/timeline_component_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const viewerStub = new ViewerStub('Title');
  let inputFiles: File[];
  let tracePipeline: TracePipeline;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: AbtChromeExtensionProtocolStub;
  let crossToolProtocol: CrossToolProtocolStub;
  let appComponent: AppComponentStub;
  let timelineComponent: TimelineComponentStub;
  let uploadTracesComponent: ProgressListenerStub;
  let collectTracesComponent: ProgressListenerStub;
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
    tracePipeline = new TracePipeline();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = new AbtChromeExtensionProtocolStub();
    crossToolProtocol = new CrossToolProtocolStub();
    appComponent = new AppComponentStub();
    timelineComponent = new TimelineComponentStub();
    uploadTracesComponent = new ProgressListenerStub();
    collectTracesComponent = new ProgressListenerStub();
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

    spyOn(ViewerFactory.prototype, 'createViewers').and.returnValue([viewerStub]);
  });

  it('handles uploaded traces from Winscope', async () => {
    const spies = [
      spyOn(uploadTracesComponent, 'onProgressUpdate'),
      spyOn(uploadTracesComponent, 'onOperationFinished'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(appComponent, 'onTraceDataLoaded'),
      spyOn(viewerStub, 'onTracePositionUpdate'),
      spyOn(timelineComponent, 'onTracePositionUpdate'),
      spyOn(crossToolProtocol, 'sendTimestamp'),
    ];

    await mediator.onWinscopeFilesUploaded(inputFiles);

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).not.toHaveBeenCalled();
    expect(appComponent.onTraceDataLoaded).not.toHaveBeenCalled();
    expect(viewerStub.onTracePositionUpdate).not.toHaveBeenCalled();

    spies.forEach((spy) => {
      spy.calls.reset();
    });
    await mediator.onWinscopeViewTracesRequest();

    expect(uploadTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(uploadTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);

    // propagates trace position on viewers creation
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
  });

  it('handles collected traces from Winscope', async () => {
    const spies = [
      spyOn(collectTracesComponent, 'onProgressUpdate'),
      spyOn(collectTracesComponent, 'onOperationFinished'),
      spyOn(timelineData, 'initialize').and.callThrough(),
      spyOn(appComponent, 'onTraceDataLoaded'),
      spyOn(viewerStub, 'onTracePositionUpdate'),
      spyOn(timelineComponent, 'onTracePositionUpdate'),
      spyOn(crossToolProtocol, 'sendTimestamp'),
    ];

    await mediator.onWinscopeFilesCollected(inputFiles);

    expect(collectTracesComponent.onProgressUpdate).toHaveBeenCalled();
    expect(collectTracesComponent.onOperationFinished).toHaveBeenCalled();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);

    // propagates trace position on viewers creation
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
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
    await loadTraceFiles();
    await mediator.onWinscopeViewTracesRequest();

    spyOn(viewerStub, 'onTracePositionUpdate');
    spyOn(timelineComponent, 'onTracePositionUpdate');
    spyOn(crossToolProtocol, 'sendTimestamp');
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // notify position
    await mediator.onTimelineTracePositionUpdate(POSITION_10);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify position
    await mediator.onTimelineTracePositionUpdate(POSITION_11);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(2);
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      await loadTraceFiles();
      await mediator.onWinscopeViewTracesRequest();

      spyOn(viewerStub, 'onTracePositionUpdate');
      spyOn(timelineComponent, 'onTracePositionUpdate');
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(0);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_11);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(2);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      await loadTraceFiles();
      await mediator.onWinscopeViewTracesRequest();

      spyOn(viewerStub, 'onTracePositionUpdate');
      spyOn(crossToolProtocol, 'sendTimestamp');

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
      expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
    });

    it('defers propagation till traces are loaded and visualized', async () => {
      spyOn(timelineComponent, 'onTracePositionUpdate');

      // keep timestamp for later
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // keep timestamp for later (replace previous one)
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_11);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // apply timestamp
      await loadTraceFiles();
      await mediator.onWinscopeViewTracesRequest();
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledWith(POSITION_11);
    });
  });

  const loadTraceFiles = async () => {
    const traceFiles = inputFiles.map((file) => new TraceFile(file));
    const errors = await tracePipeline.loadTraceFiles(traceFiles);
    expect(errors).toEqual([]);
  };
});

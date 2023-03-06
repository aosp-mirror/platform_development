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
import {MockStorage} from 'test/unit/mock_storage';
import {UnitTestUtils} from 'test/unit/utils';
import {RealTimestamp} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TracePosition} from 'trace/trace_position';
import {ViewerFactory} from 'viewers/viewer_factory';
import {ViewerStub} from 'viewers/viewer_stub';
import {AppComponentStub} from './components/app_component_stub';
import {TimelineComponentStub} from './components/timeline/timeline_component_stub';
import {UploadTracesComponentStub} from './components/upload_traces_component_stub';
import {Mediator} from './mediator';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

describe('Mediator', () => {
  const viewerStub = new ViewerStub('Title');
  let tracePipeline: TracePipeline;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: AbtChromeExtensionProtocolStub;
  let crossToolProtocol: CrossToolProtocolStub;
  let appComponent: AppComponentStub;
  let timelineComponent: TimelineComponentStub;
  let uploadTracesComponent: UploadTracesComponentStub;
  let mediator: Mediator;

  const TIMESTAMP_10 = new RealTimestamp(10n);
  const TIMESTAMP_11 = new RealTimestamp(11n);
  const POSITION_10 = TracePosition.fromTimestamp(TIMESTAMP_10);
  const POSITION_11 = TracePosition.fromTimestamp(TIMESTAMP_11);

  beforeEach(async () => {
    timelineComponent = new TimelineComponentStub();
    tracePipeline = new TracePipeline();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = new AbtChromeExtensionProtocolStub();
    crossToolProtocol = new CrossToolProtocolStub();
    appComponent = new AppComponentStub();
    timelineComponent = new TimelineComponentStub();
    uploadTracesComponent = new UploadTracesComponentStub();
    mediator = new Mediator(
      tracePipeline,
      timelineData,
      abtChromeExtensionProtocol,
      crossToolProtocol,
      appComponent,
      new MockStorage()
    );
    mediator.setTimelineComponent(timelineComponent);
    mediator.setUploadTracesComponent(uploadTracesComponent);

    spyOn(ViewerFactory.prototype, 'createViewers').and.returnValue([viewerStub]);
  });

  it('handles data load event from Winscope', async () => {
    spyOn(timelineData, 'initialize').and.callThrough();
    spyOn(appComponent, 'onTraceDataLoaded');
    spyOn(viewerStub, 'onTracePositionUpdate');

    await loadTraces();
    expect(timelineData.initialize).toHaveBeenCalledTimes(0);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledTimes(0);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(0);

    mediator.onWinscopeTraceDataLoaded();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);
    // notifies viewer about current timestamp on creation
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
  });

  //TODO: test "bugreport data from cross-tool protocol" when FileUtils is fully compatible with
  //      Node.js (b/262269229). FileUtils#unzipFile() currently can't execute on Node.js.

  //TODO: test "data from ABT chrome extension" when FileUtils is fully compatible with Node.js
  //      (b/262269229).

  it('handles start download event from ABT chrome extension', () => {
    spyOn(uploadTracesComponent, 'onFilesDownloadStart');
    expect(uploadTracesComponent.onFilesDownloadStart).toHaveBeenCalledTimes(0);

    abtChromeExtensionProtocol.onBuganizerAttachmentsDownloadStart();
    expect(uploadTracesComponent.onFilesDownloadStart).toHaveBeenCalledTimes(1);
  });

  it('handles empty downloaded files from ABT chrome extension', async () => {
    spyOn(uploadTracesComponent, 'onFilesDownloaded');
    expect(uploadTracesComponent.onFilesDownloaded).toHaveBeenCalledTimes(0);

    // Pass files even if empty so that the upload component will update the progress bar
    // and display error messages
    await abtChromeExtensionProtocol.onBuganizerAttachmentsDownloaded([]);
    expect(uploadTracesComponent.onFilesDownloaded).toHaveBeenCalledTimes(1);
  });

  it('propagates trace position update from timeline data', async () => {
    await loadTraces();
    mediator.onWinscopeTraceDataLoaded();

    spyOn(viewerStub, 'onTracePositionUpdate');
    spyOn(timelineComponent, 'onTracePositionUpdate');
    spyOn(crossToolProtocol, 'sendTimestamp');
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // notify timestamp
    timelineData.setPosition(POSITION_10);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify same timestamp again (ignored, no timestamp change)
    timelineData.setPosition(POSITION_10);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify another timestamp
    timelineData.setPosition(POSITION_11);
    expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(2);
  });

  describe('timestamp received from remote tool', () => {
    it('propagates trace position update', async () => {
      await loadTraces();
      mediator.onWinscopeTraceDataLoaded();

      spyOn(viewerStub, 'onTracePositionUpdate');
      spyOn(timelineComponent, 'onTracePositionUpdate');
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(0);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(0);

      // receive timestamp
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);

      // receive same timestamp again (ignored, no timestamp change)
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_10);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(1);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(1);

      // receive another
      await crossToolProtocol.onTimestampReceived(TIMESTAMP_11);
      expect(viewerStub.onTracePositionUpdate).toHaveBeenCalledTimes(2);
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledTimes(2);
    });

    it("doesn't propagate timestamp back to remote tool", async () => {
      await loadTraces();
      mediator.onWinscopeTraceDataLoaded();

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
      await loadTraces();
      mediator.onWinscopeTraceDataLoaded();
      expect(timelineComponent.onTracePositionUpdate).toHaveBeenCalledWith(POSITION_11);
    });
  });

  const loadTraces = async () => {
    const files = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb')
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/WindowManager.pb')
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
        )
      ),
    ];
    const errors = await tracePipeline.loadTraceFiles(files);
    expect(errors).toEqual([]);
  };
});

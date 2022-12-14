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

import {AppComponentStub} from "./components/app_component_stub";
import {TimelineComponentStub} from "./components/timeline/timeline_component_stub";
import {Mediator} from "./mediator";
import {AbtChromeExtensionProtocolStub} from "abt_chrome_extension/abt_chrome_extension_protocol_stub";
import {CrossToolProtocolStub} from "cross_tool/cross_tool_protocol_stub";
import {RealTimestamp} from "common/trace/timestamp";
import {UnitTestUtils} from "test/unit/utils";
import {ViewerFactory} from "viewers/viewer_factory";
import {ViewerStub} from "viewers/viewer_stub";
import {TimelineData} from "./timeline_data";
import {TraceData} from "./trace_data";
import {MockStorage} from "test/unit/mock_storage";

describe("Mediator", () => {
  const viewerStub = new ViewerStub("Title");
  let traceData: TraceData;
  let timelineData: TimelineData;
  let abtChromeExtensionProtocol: AbtChromeExtensionProtocolStub;
  let crossToolProtocol: CrossToolProtocolStub;
  let appComponent: AppComponentStub;
  let timelineComponent: TimelineComponentStub;
  let mediator: Mediator;

  beforeEach(async () => {
    timelineComponent = new TimelineComponentStub();
    traceData = new TraceData();
    timelineData = new TimelineData();
    abtChromeExtensionProtocol = new AbtChromeExtensionProtocolStub();
    crossToolProtocol = new CrossToolProtocolStub();
    appComponent = new AppComponentStub();
    timelineComponent = new TimelineComponentStub();
    mediator = new Mediator(
      traceData,
      timelineData,
      abtChromeExtensionProtocol,
      crossToolProtocol,
      appComponent,
      new MockStorage()
    );
    mediator.setTimelineComponent(timelineComponent);

    spyOn(ViewerFactory.prototype, "createViewers").and.returnValue([viewerStub]);
  });

  it("handles data load event from Winscope", async () => {
    spyOn(timelineData, "initialize").and.callThrough();
    spyOn(appComponent, "onTraceDataLoaded");
    spyOn(viewerStub, "notifyCurrentTraceEntries");

    await loadTraces();
    expect(timelineData.initialize).toHaveBeenCalledTimes(0);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledTimes(0);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(0);

    mediator.onWinscopeTraceDataLoaded();
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
    expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);
    // notifies viewer about current timestamp on creation
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
  });


  //TODO: enable/adapt this test once FileUtils is fully compatible with Node.js (b/262269229).
  //      FileUtils#unzipFile() currently can't execute on Node.js.
  //it("processes bugreport message from remote tool", async () => {
  //  spyOn(traceData, "loadTraces").and.callThrough();
  //  spyOn(timelineData, "initialize").and.callThrough();
  //  spyOn(appComponent, "onTraceDataLoaded");
  //  spyOn(viewerStub, "notifyCurrentTraceEntries");

  //  const bugreport = await UnitTestUtils.getFixtureFile("bugreports/bugreport_stripped.zip");
  //  const timestamp = new RealTimestamp(10n);
  //  await crossToolProtocol.onBugreportReceived(bugreport, timestamp);

  //  expect(traceData.loadTraces).toHaveBeenCalledOnceWith([bugreport]);
  //  expect(timelineData.initialize).toHaveBeenCalledTimes(1);
  //  expect(appComponent.onTraceDataLoaded).toHaveBeenCalledOnceWith([viewerStub]);
  //  expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
  //});

  it("propagates current timestamp changed through timeline", async () => {
    const timestamp10 = new RealTimestamp(10n);
    const timestamp11 = new RealTimestamp(11n);

    await loadTraces();
    mediator.onWinscopeTraceDataLoaded();

    spyOn(viewerStub, "notifyCurrentTraceEntries");
    spyOn(timelineComponent, "onCurrentTimestampChanged");
    spyOn(crossToolProtocol, "sendTimestamp");
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // notify timestamp
    timelineData.setCurrentTimestamp(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify same timestamp again (ignored, no timestamp change)
    timelineData.setCurrentTimestamp(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(1);

    // notify another timestamp
    timelineData.setCurrentTimestamp(timestamp11);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(2);
  });

  it("propagates timestamp received from remote tool", async () => {
    const timestamp10 = new RealTimestamp(10n);
    const timestamp11 = new RealTimestamp(11n);

    await loadTraces();
    mediator.onWinscopeTraceDataLoaded();

    spyOn(viewerStub, "notifyCurrentTraceEntries");
    spyOn(timelineComponent, "onCurrentTimestampChanged");
    spyOn(crossToolProtocol, "sendTimestamp");
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // receive timestamp
    await crossToolProtocol.onTimestampReceived(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // receive same timestamp again (ignored, no timestamp change)
    await crossToolProtocol.onTimestampReceived(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);

    // receive another
    await crossToolProtocol.onTimestampReceived(timestamp11);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);
    expect(crossToolProtocol.sendTimestamp).toHaveBeenCalledTimes(0);
  });

  const loadTraces = async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await traceData.loadTraces(traces);
  };
});

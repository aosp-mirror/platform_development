/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {
  ExpandedTimelineToggled,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {TraceType} from 'trace/trace_type';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('PresenterMediaBased', () => {
  const entries = [
    new MediaBasedTraceEntry(10, new Blob(), false),
    new MediaBasedTraceEntry(15, new Blob(), false),
  ];
  const timestamps = [
    TimestampConverterUtils.makeRealTimestamp(10n),
    TimestampConverterUtils.makeRealTimestamp(15n),
  ];
  const trace1 = new TraceBuilder<MediaBasedTraceEntry>()
    .setType(TraceType.SCREEN_RECORDING)
    .setDescriptors(['recording 1'])
    .setEntries(entries)
    .setTimestamps(timestamps)
    .build();
  const trace2 = new TraceBuilder<MediaBasedTraceEntry>()
    .setType(TraceType.SCREEN_RECORDING)
    .setDescriptors(['recording 2'])
    .setEntries(entries)
    .setTimestamps(timestamps)
    .build();

  const traces = [trace1, trace2];

  let presenter: Presenter;
  let uiData: UiData;

  beforeEach(() => {
    presenter = new Presenter(traces, (newData) => {
      uiData = newData;
    });
  });

  it('initializes titles from trace descriptors', () => {
    expect(uiData.titles).toEqual(['recording 1', 'recording 2']);
  });

  it('processes trace position updates', async () => {
    const positionUpdate1 = TracePositionUpdate.fromTimestamp(timestamps[1]);
    await presenter.onAppEvent(positionUpdate1);
    expect(uiData.currentTraceEntries).toEqual([entries[1], entries[1]]);

    const positionUpdate0 = TracePositionUpdate.fromTimestamp(timestamps[0]);
    await presenter.onAppEvent(positionUpdate0);
    expect(uiData.currentTraceEntries).toEqual([entries[0], entries[0]]);
  });

  it('updates force minimize state on expanded timeline toggle', async () => {
    expect(uiData.forceMinimize).toBeFalse();
    await presenter.onAppEvent(new ExpandedTimelineToggled(true));
    expect(uiData.forceMinimize).toBeTrue();
    await presenter.onAppEvent(new ExpandedTimelineToggled(false));
    expect(uiData.forceMinimize).toBeFalse();
  });
});

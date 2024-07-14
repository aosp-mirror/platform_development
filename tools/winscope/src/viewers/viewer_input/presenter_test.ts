/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {LogFieldType, UiDataLog} from 'viewers/common/ui_data_log';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterInputTest extends AbstractLogViewerPresenterTest {
  private trace: Trace<PropertyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;

  override readonly shouldExecuteHeaderTests = true;
  override readonly shouldExecuteFilterTests = false;
  override readonly shouldExecuteCurrentIndexTests = false;
  override readonly shouldExecutePropertiesTests = true;

  override readonly totalOutputEntries = 8;
  override readonly expectedIndexOfFirstPositionUpdate = 0;
  override readonly expectedIndexOfSecondPositionUpdate = 2;
  override readonly logEntryClickIndex = 3;

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/perfetto/input-events.perfetto-trace',
    ])) as Parser<PropertyTreeNode>;

    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.INPUT_EVENT_MERGED)
      .setParser(parser)
      .build();

    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(2),
    );
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType,
  ): Presenter {
    const traces = new TracesBuilder()
      .setEntries(TraceType.INPUT_EVENT_MERGED, [])
      .build();
    return PresenterInputTest.createPresenterWithTraces(traces, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType,
  ): Promise<Presenter> {
    const trace = assertDefined(this.trace);
    const traces = new Traces();
    traces.addTrace(trace);
    const presenter = PresenterInputTest.createPresenterWithTraces(
      traces,
      callback,
    );
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  private static createPresenterWithTraces(
    traces: Traces,
    callback: NotifyLogViewCallbackType,
  ): Presenter {
    return new Presenter(
      traces,
      assertDefined(traces.getTrace(TraceType.INPUT_EVENT_MERGED)),
      new InMemoryStorage(),
      callback as NotifyLogViewCallbackType,
    );
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override executePropertiesChecksForEmptyTrace(uiDataLog: UiDataLog) {
    const uiData = uiDataLog as UiData;
    expect(uiData.highlightedProperty).toBeFalsy();
    expect(uiData.dispatchPropertiesTree).toBeUndefined();
  }

  override executePropertiesChecksAfterPositionUpdate(
    uiDataLog: UiDataLog,
  ): void {
    expect(uiDataLog.entries.length).toEqual(8);
    expect(uiDataLog.selectedIndex).toEqual(0);
    const curEntry = uiDataLog.entries[0];
    const expectedFields = [
      {
        type: LogFieldType.INPUT_TYPE,
        value: 'MOTION',
      },
      {
        type: LogFieldType.INPUT_SOURCE,
        value: 'TOUCHSCREEN',
      },
      {
        type: LogFieldType.INPUT_ACTION,
        value: 'DOWN',
      },
      {
        type: LogFieldType.INPUT_EVENT_DETAILS,
        value: '[0: (431, 624)]',
      },
    ];
    expectedFields.forEach((field) => {
      expect(curEntry.fields).toContain(field);
    });

    const motionEvent = assertDefined(uiDataLog.propertiesTree);
    expect(motionEvent.getChildByName('eventId')?.getValue()).toEqual(
      330184796,
    );
    expect(motionEvent.getChildByName('action')?.formattedValue()).toEqual(
      'ACTION_DOWN',
    );

    const uiData = uiDataLog as UiData;
    const dispatchProperties = assertDefined(uiData.dispatchPropertiesTree);
    expect(dispatchProperties.getAllChildren().length).toEqual(5);
    expect(
      dispatchProperties
        .getChildByName('0')
        ?.getChildByName('windowId')
        ?.getValue(),
    ).toEqual(212n);
  }

  private expectEventPresented(
    uiData: UiData,
    eventId: number,
    action: string,
  ) {
    const properties = assertDefined(uiData.propertiesTree);
    expect(properties.getChildByName('action')?.formattedValue()).toEqual(
      action,
    );
    expect(properties.getChildByName('eventId')?.getValue()).toEqual(eventId);
  }

  override executeSpecializedTests() {
    describe('Specialized tests', async () => {
      const time0 = TimestampConverterUtils.makeRealTimestamp(0n);
      const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
      const time19 = TimestampConverterUtils.makeRealTimestamp(19n);
      const time20 = TimestampConverterUtils.makeRealTimestamp(20n);
      const time25 = TimestampConverterUtils.makeRealTimestamp(25n);
      const time30 = TimestampConverterUtils.makeRealTimestamp(30n);
      const time35 = TimestampConverterUtils.makeRealTimestamp(35n);
      const time36 = TimestampConverterUtils.makeRealTimestamp(36n);

      let uiData: UiData = UiData.createEmpty();

      beforeAll(async () => {
        uiData = UiData.createEmpty();
        await this.setUpTestEnvironment();
      });

      it('updates selected entry', async () => {
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );

        const keyEntry = assertDefined(this.trace).getEntry(7);
        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(keyEntry),
        );

        this.expectEventPresented(uiData, 894093732, 'ACTION_UP');

        const motionEntry = assertDefined(this.trace).getEntry(1);
        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(motionEntry),
        );

        this.expectEventPresented(uiData, 1327679296, 'ACTION_OUTSIDE');

        const motionDispatchProperties = assertDefined(
          uiData.dispatchPropertiesTree,
        );
        expect(motionDispatchProperties.getAllChildren().length).toEqual(1);
        expect(
          motionDispatchProperties
            .getChildByName('0')
            ?.getChildByName('windowId')
            ?.getValue(),
        ).toEqual(98n);
      });

      it('finds entry by time', async () => {
        const traces = new Traces();
        traces.addTrace(assertDefined(this.trace));

        const lastMotion = await assertDefined(this.trace).getEntry(5);
        const firstKey = await assertDefined(this.trace).getEntry(6);
        const diffNs =
          firstKey.getTimestamp().getValueNs() -
          lastMotion.getTimestamp().getValueNs();
        const belowLastMotionTime = lastMotion.getTimestamp().minus(1n);
        const midpointTime = lastMotion.getTimestamp().add(diffNs / 2n);
        const aboveFirstKeyTime = firstKey.getTimestamp().add(1n);

        const otherTrace = new TraceBuilder<string>()
          .setType(TraceType.TEST_TRACE_STRING)
          .setEntries(['event-log-00', 'event-log-01', 'event-log-02'])
          .setTimestamps([belowLastMotionTime, midpointTime, aboveFirstKeyTime])
          .build();
        traces.addTrace(otherTrace);
        const presenter = PresenterInputTest.createPresenterWithTraces(
          traces,
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(0)),
        );
        this.expectEventPresented(uiData, 313395000, 'ACTION_MOVE');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(1)),
        );
        this.expectEventPresented(uiData, 436499943, 'ACTION_UP');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(2)),
        );
        this.expectEventPresented(uiData, 759309047, 'ACTION_DOWN');
      });

      it('finds closest input event by frame', async () => {
        const parser = assertDefined(this.trace).getParser();
        const traces = new Traces();

        // FRAME:            0        1       2
        // TEST(time):       0       19      35
        // INPUT(time):     10    20,25   30,36
        const trace = new TraceBuilder<PropertyTreeNode>()
          .setType(TraceType.INPUT_EVENT_MERGED)
          .setEntries([
            await parser.getEntry(0),
            await parser.getEntry(1),
            await parser.getEntry(2),
            await parser.getEntry(3),
            await parser.getEntry(4),
          ])
          .setTimestamps([time10, time20, time25, time30, time36])
          .setFrame(0, 0)
          .setFrame(1, 1)
          .setFrame(2, 1)
          .setFrame(3, 2)
          .setFrame(4, 2)
          .build();
        traces.addTrace(trace);

        const otherTrace = new TraceBuilder<string>()
          .setType(TraceType.TEST_TRACE_STRING)
          .setEntries(['sf-event-00', 'sf-event-01', 'sf-event-02'])
          .setTimestamps([time0, time19, time35])
          .setFrame(0, 0)
          .setFrame(1, 1)
          .setFrame(2, 2)
          .build();
        traces.addTrace(otherTrace);

        const presenter = PresenterInputTest.createPresenterWithTraces(
          traces,
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(0)),
        );
        this.expectEventPresented(uiData, 330184796, 'ACTION_DOWN');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(1)),
        );
        this.expectEventPresented(uiData, 1327679296, 'ACTION_OUTSIDE');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(otherTrace.getEntry(2)),
        );
        this.expectEventPresented(uiData, 106022695, 'ACTION_MOVE');
      });
    });
  }
}

describe('PresenterInput', async () => {
  new PresenterInputTest().execute();
});

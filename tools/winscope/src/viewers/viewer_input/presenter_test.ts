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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  TabbedViewSwitchRequest,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {Transform} from 'parsers/surface_flinger/transform_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {NotifyLogViewCallbackType} from 'viewers/common/abstract_log_viewer_presenter';
import {AbstractLogViewerPresenterTest} from 'viewers/common/abstract_log_viewer_presenter_test';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {TextFilter} from 'viewers/common/text_filter';
import {LogField, LogHeader} from 'viewers/common/ui_data_log';
import {UserOptions} from 'viewers/common/user_options';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {TraceRectType} from 'viewers/components/rects/rect_spec';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterInputTest extends AbstractLogViewerPresenterTest<UiData> {
  override readonly expectedHeaders = [
    {
      header: new LogHeader(
        {
          name: 'Type',
          cssClass: 'input-type inline',
        },
        new LogSelectFilter(['MOTION', 'KEY'], false, '80', '100%'),
      ),
    },
    {
      header: new LogHeader(
        {
          name: 'Source',
          cssClass: 'input-source',
        },
        new LogSelectFilter(['TOUCHSCREEN', 'KEYBOARD'], false, '200', '100%'),
      ),
    },
    {
      header: new LogHeader(
        {
          name: 'Action',
          cssClass: 'input-action',
        },
        new LogSelectFilter(
          ['DOWN', 'OUTSIDE', 'MOVE', 'UP'],
          false,
          '100',
          '100%',
        ),
      ),
    },
    {
      header: new LogHeader(
        {
          name: 'Device',
          cssClass: 'input-device-id right-align',
        },
        new LogSelectFilter(['4', '2'], false, '80', '100%'),
      ),
    },
    {
      header: new LogHeader(
        {
          name: 'Display',
          cssClass: 'input-display-id right-align',
        },
        new LogSelectFilter(['0', '-1'], false, '80', '100%'),
      ),
    },
    {
      header: new LogHeader({
        name: 'Details',
        cssClass: 'input-details',
      }),
    },
    {
      header: new LogHeader(
        {
          name: 'Target Windows',
          cssClass: 'input-windows',
        },
        new LogSelectFilter(
          Array.from({length: 6}, () => ''),
          true,
          '100',
          '100%',
        ),
      ),
      options: [
        this.wrappedName('win-212'),
        this.wrappedName('win-64'),
        this.wrappedName('win-82'),
        this.wrappedName('win-75'),
        this.wrappedName('win-zero-not-98'),
        this.wrappedName('98'),
      ],
    },
  ];
  private trace: Trace<PropertyTreeNode> | undefined;
  private surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private layerIdToName: Array<{id: number; name: string}> = [
    {id: 0, name: 'win-zero-not-98'},
    {id: 212, name: 'win-212'},
    {id: 64, name: 'win-64'},
    {id: 82, name: 'win-82'},
    {id: 75, name: 'win-75'},
    // The layer name for window with id 98 is omitted to test incomplete mapping.
  ];

  override async setUpTestEnvironment(): Promise<void> {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/perfetto/input-events.perfetto-trace',
    ])) as Parser<PropertyTreeNode>;

    this.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.INPUT_EVENT_MERGED)
      .setParser(parser)
      .build();

    this.surfaceFlingerTrace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([])
      .setParserCustomQueryResult(
        CustomQueryType.SF_LAYERS_ID_AND_NAME,
        this.layerIdToName,
      )
      .build();

    this.positionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(0),
    );
  }

  override async createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const traces = new TracesBuilder()
      .setEntries(TraceType.INPUT_EVENT_MERGED, [])
      .build();
    if (this.surfaceFlingerTrace !== undefined) {
      traces.addTrace(this.surfaceFlingerTrace);
    }
    return PresenterInputTest.createPresenterWithTraces(traces, callback);
  }

  override async createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<Presenter> {
    const traces = new Traces();
    traces.addTrace(assertDefined(this.trace));
    if (this.surfaceFlingerTrace !== undefined) {
      traces.addTrace(this.surfaceFlingerTrace);
    }
    const presenter = PresenterInputTest.createPresenterWithTraces(
      traces,
      callback,
    );
    await presenter.onAppEvent(this.getPositionUpdate()); // trigger initialization
    return presenter;
  }

  private static createPresenterWithTraces(
    traces: Traces,
    callback: NotifyLogViewCallbackType<UiData>,
  ): Presenter {
    return new Presenter(
      traces,
      assertDefined(traces.getTrace(TraceType.INPUT_EVENT_MERGED)),
      new InMemoryStorage(),
      callback,
    );
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override executePropertiesChecksForEmptyTrace(uiData: UiData) {
    expect(uiData.highlightedProperty).toBeFalsy();
    expect(uiData.dispatchPropertiesTree).toBeUndefined();
    expect(uiData.dispatchPropertiesFilter).toBeDefined();
  }

  override executePropertiesChecksAfterPositionUpdate(uiData: UiData): void {
    expect(uiData.entries.length).toEqual(8);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toBeUndefined();
    const curEntry = uiData.entries[0];
    const expectedFields: LogField[] = [
      {
        spec: uiData.headers[0].spec,
        value: 'MOTION',
        propagateEntryTimestamp: true,
      },
      {spec: uiData.headers[1].spec, value: 'TOUCHSCREEN'},
      {spec: uiData.headers[2].spec, value: 'DOWN'},
      {spec: uiData.headers[3].spec, value: 4},
      {spec: uiData.headers[4].spec, value: 0},
      {spec: uiData.headers[5].spec, value: '[212, 64, 82, 75]'},
      {
        spec: uiData.headers[6].spec,
        value: [
          this.wrappedName('win-212'),
          this.wrappedName('win-64'),
          this.wrappedName('win-82'),
          this.wrappedName('win-75'),
          this.wrappedName('win-zero-not-98'),
        ].join(', '),
      },
    ];
    expectedFields.forEach((field) => {
      expect(curEntry.fields).toContain(field);
    });

    const motionEvent = assertDefined(uiData.propertiesTree);
    expect(motionEvent.getChildByName('eventId')?.getValue()).toEqual(
      330184796,
    );
    expect(motionEvent.getChildByName('action')?.formattedValue()).toEqual(
      'ACTION_DOWN',
    );

    const dispatchProperties = assertDefined(uiData.dispatchPropertiesTree);
    expect(dispatchProperties.getAllChildren().length).toEqual(5);

    expect(dispatchProperties.getChildByName('0')?.getDisplayName()).toEqual(
      'win-212',
    );
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
    describe('Specialized tests', () => {
      const time0 = TimestampConverterUtils.makeRealTimestamp(0n);
      const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
      const time19 = TimestampConverterUtils.makeRealTimestamp(19n);
      const time20 = TimestampConverterUtils.makeRealTimestamp(20n);
      const time25 = TimestampConverterUtils.makeRealTimestamp(25n);
      const time30 = TimestampConverterUtils.makeRealTimestamp(30n);
      const time35 = TimestampConverterUtils.makeRealTimestamp(35n);
      const time36 = TimestampConverterUtils.makeRealTimestamp(36n);
      const layerRect = new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('layerRect')
        .setName('layerRect')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build();
      const inputRect = new TraceRectBuilder()
        .setX(2)
        .setY(2)
        .setWidth(3)
        .setHeight(3)
        .setId('inputRect')
        .setName('inputRect')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build();
      const sfEntry0 = new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('root')
        .setChildren([
          {
            id: 1,
            name: 'layer1',
            rects: [layerRect],
            secondaryRects: [inputRect],
          },
        ])
        .build();
      const sfEntry1 = new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('root')
        .setChildren([
          {
            id: 1,
            name: 'layer1',
            rects: [layerRect],
            secondaryRects: [inputRect],
            children: [
              {
                id: 2,
                name: 'layer2',
                rects: [layerRect],
                secondaryRects: [inputRect],
              },
            ],
          },
        ])
        .build();
      const sfEntry2 = new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('root')
        .setChildren([
          {
            id: 1,
            name: 'layer1',
            rects: [layerRect],
            secondaryRects: [inputRect],
            children: [
              {
                id: 2,
                name: 'layer2',
                rects: [layerRect],
                secondaryRects: [inputRect],
              },
            ],
          },
          {
            id: 3,
            name: 'layer3',
            rects: [layerRect],
            secondaryRects: [inputRect],
          },
        ])
        .build();

      let uiData: UiData = UiData.createEmpty();

      beforeEach(async () => {
        uiData = UiData.createEmpty();
        await this.setUpTestEnvironment();
      });

      it('adds event listeners', async () => {
        const element = document.createElement('div');
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        presenter.addEventListeners(element);

        const testId = 'testId';

        let spy: jasmine.Spy = spyOn(presenter, 'onHighlightedPropertyChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HighlightedPropertyChange, {
            detail: {id: testId},
          }),
        );
        expect(spy).toHaveBeenCalledWith(testId);

        spy = spyOn(presenter, 'onHighlightedIdChange');
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.HighlightedIdChange, {
            detail: {id: testId},
          }),
        );
        expect(spy).toHaveBeenCalledWith(testId);

        spy = spyOn(presenter, 'onRectsUserOptionsChange');
        const userOptions = {};
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.RectsUserOptionsChange, {
            detail: {userOptions},
          }),
        );
        expect(spy).toHaveBeenCalledWith(userOptions);

        spy = spyOn(presenter, 'onRectDoubleClick');
        element.dispatchEvent(new CustomEvent(ViewerEvents.RectsDblClick));
        expect(spy).toHaveBeenCalled();

        spy = spyOn(presenter, 'onDispatchPropertiesFilterChange');
        const filter = new TextFilter();
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.DispatchPropertiesFilterChange, {
            detail: filter,
          }),
        );
        expect(spy).toHaveBeenCalledWith(filter);
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

      it('no rects defined without SF trace', async () => {
        this.surfaceFlingerTrace = undefined;

        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.rectsToDraw).toBeUndefined();
        checkRectSpec();
      });

      it('empty trace no rects defined without SF trace', async () => {
        this.surfaceFlingerTrace = undefined;

        const presenter = await this.createPresenterWithEmptyTrace(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.rectsToDraw).toBeUndefined();
        checkRectSpec();
      });

      it('rects defined with SF trace', async () => {
        assertDefined(this.surfaceFlingerTrace);
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.rectsToDraw).toBeDefined();
        expect(uiData.rectsToDraw).toEqual([]);
        checkRectSpec();
      });

      it('empty trace rects defined with SF trace', async () => {
        assertDefined(this.surfaceFlingerTrace);
        const presenter = await this.createPresenterWithEmptyTrace(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.rectsToDraw).toBeDefined();
        expect(uiData.rectsToDraw).toEqual([]);
      });

      it('extracts corresponding input rects from SF trace', async () => {
        const parser = assertDefined(this.trace).getParser();
        const traces = await getTracesWithSf(parser, this.layerIdToName);
        const trace = assertDefined(
          traces.getTrace(TraceType.INPUT_EVENT_MERGED),
        );

        const presenter = PresenterInputTest.createPresenterWithTraces(
          traces,
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(0)),
        );
        expect(uiData.rectsToDraw).toEqual([]);

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(1)),
        );
        expect(uiData.rectsToDraw).toHaveSize(1);
        expect(uiData.rectsToDraw?.at(0)?.id).toEqual('inputRect');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(2)),
        );
        expect(uiData.rectsToDraw).toHaveSize(1);
        expect(uiData.rectsToDraw?.at(0)?.id).toEqual('inputRect');

        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(3)),
        );
        expect(uiData.rectsToDraw).toHaveSize(3);
        uiData.rectsToDraw?.forEach((rect) =>
          expect(rect.id).toEqual('inputRect'),
        );
      });

      it('filters dispatch properties tree', async () => {
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(this.getPositionUpdate());
        await presenter.onLogEntryClick(3);
        expect(
          assertDefined(uiData.dispatchPropertiesTree).getAllChildren().length,
        ).toEqual(5);
        await presenter.onDispatchPropertiesFilterChange(new TextFilter('212'));
        expect(
          assertDefined(uiData.dispatchPropertiesTree).getAllChildren().length,
        ).toEqual(1);
      });

      it('updates highlighted property', async () => {
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        expect(uiData.highlightedProperty).toEqual('');
        const id = '4';
        presenter.onHighlightedPropertyChange(id);
        expect(uiData.highlightedProperty).toEqual(id);
        presenter.onHighlightedPropertyChange(id);
        expect(uiData.highlightedProperty).toEqual('');
      });

      it('updates highlighted rect', async () => {
        const parser = assertDefined(this.trace).getParser();
        const traces = await getTracesWithSf(parser, this.layerIdToName);
        const trace = assertDefined(
          traces.getTrace(TraceType.INPUT_EVENT_MERGED),
        );
        const presenter = PresenterInputTest.createPresenterWithTraces(
          traces,
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(1)),
        );
        expect(uiData.rectsToDraw).toHaveSize(1);

        const rect = assertDefined(uiData.rectsToDraw)[0];
        await presenter.onHighlightedIdChange(rect.id);
        expect(uiData.highlightedRect).toEqual(rect.id);
        await presenter.onHighlightedIdChange(rect.id);
        expect(uiData.highlightedRect).toEqual('');
      });

      it('filters rects by having content or visibility', async () => {
        const userOptions: UserOptions = {
          showOnlyVisible: {
            name: 'Show only',
            chip: VISIBLE_CHIP,
            enabled: false,
          },
          showOnlyWithContent: {
            name: 'Has input',
            icon: 'pan_tool_alt',
            enabled: true,
          },
        };
        const parser = assertDefined(this.trace).getParser();
        const traces = await getTracesWithSf(parser, this.layerIdToName);
        const trace = assertDefined(
          traces.getTrace(TraceType.INPUT_EVENT_MERGED),
        );
        const presenter = PresenterInputTest.createPresenterWithTraces(
          traces,
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        await presenter.onAppEvent(
          TracePositionUpdate.fromTraceEntry(trace.getEntry(1)),
        );
        expect(uiData.rectsToDraw).toHaveSize(1);

        await presenter.onRectsUserOptionsChange(userOptions);
        expect(uiData.rectsUserOptions).toEqual(userOptions);
        expect(uiData.rectsToDraw).toHaveSize(0);

        userOptions['showOnlyVisible'].enabled = true;
        userOptions['showOnlyWithContent'].enabled = false;
        await presenter.onRectsUserOptionsChange(userOptions);
        expect(uiData.rectsToDraw).toHaveSize(1);
      });

      it('emits event on rect double click', async () => {
        const presenter = await this.createPresenter(
          (uiDataLog) => (uiData = uiDataLog as UiData),
        );
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);
        await presenter.onRectDoubleClick();
        expect(spy).toHaveBeenCalledWith(
          new TabbedViewSwitchRequest(assertDefined(this.surfaceFlingerTrace)),
        );
      });

      async function getTracesWithSf(
        parser: Parser<PropertyTreeNode>,
        layerIdToName: Array<{
          id: number;
          name: string;
        }>,
      ) {
        const traces = new Traces();

        // FRAME:         0     1   2   3
        // INPUT(index):  0   1,2   -   3
        // SF(index):     -     0   1   2
        const trace = new TraceBuilder<PropertyTreeNode>()
          .setType(TraceType.INPUT_EVENT_MERGED)
          .setEntries([
            await parser.getEntry(0),
            await parser.getEntry(1),
            await parser.getEntry(2),
            await parser.getEntry(3),
          ])
          .setTimestamps([time10, time20, time25, time30])
          .setFrame(0, 0)
          .setFrame(1, 1)
          .setFrame(2, 1)
          .setFrame(3, 3)
          .build();
        traces.addTrace(trace);

        const sfTrace = new TraceBuilder<HierarchyTreeNode>()
          .setType(TraceType.SURFACE_FLINGER)
          .setEntries([sfEntry0, sfEntry1, sfEntry2])
          .setTimestamps([time0, time19, time35])
          .setFrame(0, 1)
          .setFrame(1, 2)
          .setFrame(2, 3)
          .setParserCustomQueryResult(
            CustomQueryType.SF_LAYERS_ID_AND_NAME,
            layerIdToName,
          )
          .build();
        traces.addTrace(sfTrace);
        return traces;
      }

      function checkRectSpec() {
        expect(uiData.rectSpec).toEqual({
          type: TraceRectType.INPUT_WINDOWS,
          icon: TRACE_INFO[TraceType.INPUT_EVENT_MERGED].icon,
          legend: [
            {
              fill: '#c8e8b7',
              desc: 'Visible and touchable',
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
              fill: '',
              border: 'var(--default-text-color)',
              desc: 'Visible but not touchable',
              showInWireFrameMode: false,
            },
            {
              fill: 'var(--selected-element-color)',
              desc: 'Selected',
              border: 'var(--default-text-color)',
              showInWireFrameMode: true,
            },
            {
              fill: '#ad42f5',
              desc: 'Has input',
              border: 'var(--default-text-color)',
              showInWireFrameMode: false,
            },
          ],
          multiple: false,
        });
      }
    });
  }

  private wrappedName(name: string): string {
    return `\u{200C}${name}\u{200C}`;
  }
}

describe('PresenterInput', async () => {
  new PresenterInputTest().execute();
});

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

import {assertDefined} from 'common/assert_utils';
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  ActiveTraceChanged,
  DarkModeToggled,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {MockPresenter} from 'test/unit/mock_log_viewer_presenter';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {DEFAULT_PROPERTY_FORMATTER} from 'trace/tree_node/formatters';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {LogSelectFilter, LogTextFilter} from './log_filters';
import {LogHeader, UiDataLog} from './ui_data_log';
import {UserOptions} from './user_options';
import {
  LogFilterChangeDetail,
  LogTextFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from './viewer_events';

describe('AbstractLogViewerPresenter', () => {
  let uiData: UiDataLog;
  let presenter: MockPresenter;
  let trace: Trace<PropertyTreeNode>;
  let positionUpdate: TracePositionUpdate;
  let secondPositionUpdate: TracePositionUpdate;
  let lastEntryPositionUpdate: TracePositionUpdate;

  beforeAll(async () => {
    const timestamp1 = TimestampConverterUtils.makeElapsedTimestamp(1n);
    const timestamp2 = TimestampConverterUtils.makeElapsedTimestamp(2n);
    const timestamp3 = TimestampConverterUtils.makeElapsedTimestamp(3n);
    const timestamp4 = TimestampConverterUtils.makeElapsedTimestamp(4n);
    trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSACTIONS)
      .setEntries([
        new PropertyTreeBuilder()
          .setRootId('Test Trace')
          .setName('entry 1')
          .setChildren([
            {
              name: 'pass1',
              value: 'pass',
              formatter: DEFAULT_PROPERTY_FORMATTER,
            },
            {
              name: 'pass2',
              value: 'fail',
              formatter: DEFAULT_PROPERTY_FORMATTER,
              source: PropertySource.DEFAULT,
            },
            {
              name: 'fail1',
              value: 'pass',
              formatter: DEFAULT_PROPERTY_FORMATTER,
            },
            {
              name: 'fail2',
              value: 'fail',
              formatter: DEFAULT_PROPERTY_FORMATTER,
            },
          ])
          .build(),
        new PropertyTreeBuilder()
          .setRootId('Test Trace')
          .setName('entry 2')
          .build(),
        new PropertyTreeBuilder()
          .setRootId('Test Trace')
          .setName('entry 3')
          .build(),
        new PropertyTreeBuilder()
          .setRootId('Test Trace')
          .setName('entry 4')
          .build(),
      ])
      .setTimestamps([timestamp1, timestamp2, timestamp3, timestamp4])
      .build();
    positionUpdate = TracePositionUpdate.fromTraceEntry(trace.getEntry(0));
    secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(1),
    );
    lastEntryPositionUpdate = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(3),
    );
  });

  beforeEach(() => {
    presenter = new MockPresenter(trace, new InMemoryStorage(), (newData) => {
      uiData = newData;
    });
  });

  it('adds events listeners', async () => {
    const element = makeElement();
    presenter.addEventListeners(element);

    const testHeader = new LogHeader(
      {name: 'Test Column', cssClass: 'test-class'},
      new LogSelectFilter([]),
    );

    let spy: jasmine.Spy = spyOn(presenter, 'onSelectFilterChange');
    const filterDetail = new LogFilterChangeDetail(testHeader, ['']);
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.LogFilterChange, {
        detail: filterDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(testHeader, filterDetail.value);

    spy = spyOn(presenter, 'onTextFilterChange');
    const textFilterDetail = new LogTextFilterChangeDetail(
      testHeader,
      new TextFilter(),
    );
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.LogTextFilterChange, {
        detail: textFilterDetail,
      }),
    );
    expect(spy).toHaveBeenCalledWith(testHeader, textFilterDetail.filter);

    spy = spyOn(presenter, 'onLogEntryClick');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.LogEntryClick, {
        detail: 0,
      }),
    );
    expect(spy).toHaveBeenCalledWith(0);

    spy = spyOn(presenter, 'onArrowDownPress');
    element.dispatchEvent(new CustomEvent(ViewerEvents.ArrowDownPress));
    expect(spy).toHaveBeenCalled();

    spy = spyOn(presenter, 'onArrowUpPress');
    element.dispatchEvent(new CustomEvent(ViewerEvents.ArrowUpPress));
    expect(spy).toHaveBeenCalled();

    await presenter.onAppEvent(positionUpdate);
    spy = spyOn(presenter, 'onLogTimestampClick');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.TimestampClick, {
        detail: new TimestampClickDetail(uiData.entries[0].traceEntry),
      }),
    );
    expect(spy).toHaveBeenCalledWith(uiData.entries[0].traceEntry);

    spy = spyOn(presenter, 'onRawTimestampClick');
    const ts = TimestampConverterUtils.makeZeroTimestamp();
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.TimestampClick, {
        detail: new TimestampClickDetail(undefined, ts),
      }),
    );
    expect(spy).toHaveBeenCalledWith(ts);

    spy = spyOn(presenter, 'onPropertiesUserOptionsChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.PropertiesUserOptionsChange, {
        detail: {userOptions: {}},
      }),
    );
    expect(spy).toHaveBeenCalledWith({});

    spy = spyOn(presenter, 'onPropertiesFilterChange');
    const filter = new TextFilter();
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.PropertiesFilterChange, {
        detail: filter,
      }),
    );
    expect(spy).toHaveBeenCalledWith(filter);

    spy = spyOn(presenter, 'onPositionChangeByKeyPress');
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    pressRightArrowKey();
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
    expect(spy).not.toHaveBeenCalled();

    document.body.append(element);
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    pressRightArrowKey();
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('initializes entries and filters with options', async () => {
    expect(uiData.scrollToIndex).toBeUndefined();
    expect(uiData.currentIndex).toBeUndefined();
    expect(uiData.selectedIndex).toBeUndefined();
    expect(uiData.entries.length).toEqual(0);
    expect(uiData.propertiesTree).toBeUndefined();
    expect(uiData.headers).toEqual([]);

    await assertDefined(presenter).onAppEvent(positionUpdate);

    expect(uiData.scrollToIndex).toBeDefined();
    expect(uiData.currentIndex).toBeDefined();
    expect(uiData.selectedIndex).toBeUndefined();
    expect(uiData.entries.length).toEqual(4);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[0].propertiesTree).id,
    );
    expect(uiData.headers.length).toEqual(3);
    expect((uiData.headers[0].filter as LogSelectFilter).options).toEqual([
      'stringValue',
      'differentValue',
    ]);
  });

  it('processes trace position update and updates ui data', async () => {
    await assertDefined(presenter).onAppEvent(secondPositionUpdate);
    expect(uiData.currentIndex).toEqual(1);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[1].propertiesTree).id,
    );
  });

  it('allows arrow keydown event to propagate if presenter trace not active or current index not defined', async () => {
    const element = makeElement();
    document.body.append(element);
    presenter.addEventListeners(element);
    const listenerSpy = jasmine.createSpy();
    document.addEventListener('keydown', listenerSpy);

    await presenter.onAppEvent(
      new TracePositionUpdate(
        TracePosition.fromTimestamp(
          TimestampConverterUtils.makeElapsedTimestamp(-1n),
        ),
      ),
    );
    expect(uiData.currentIndex).toBeUndefined();

    pressRightArrowKey();
    expect(listenerSpy).toHaveBeenCalledTimes(1);

    await presenter.onAppEvent(
      new ActiveTraceChanged(
        assertDefined(positionUpdate.position.entry).getFullTrace(),
      ),
    );
    pressRightArrowKey();
    expect(listenerSpy).toHaveBeenCalledTimes(2);

    await presenter.onAppEvent(positionUpdate);
    pressRightArrowKey();
    expect(listenerSpy).toHaveBeenCalledTimes(2);

    await presenter.onAppEvent(
      new ActiveTraceChanged(
        UnitTestUtils.makeEmptyTrace(TraceType.TRANSACTIONS),
      ),
    );
    pressRightArrowKey();
    expect(listenerSpy).toHaveBeenCalledTimes(3);

    document.removeEventListener('keydown', listenerSpy);
  });

  it('propagates position with next trace entry of different timestamp on right arrow key press', async () => {
    const positionUpdateEntry = assertDefined(positionUpdate.position.entry);
    const trace = positionUpdateEntry.getFullTrace();
    await presenter.onAppEvent(new ActiveTraceChanged(trace));

    const emitEventSpy = jasmine.createSpy();
    presenter.setEmitEvent(emitEventSpy);
    await presenter.onAppEvent(positionUpdate);

    await presenter.onPositionChangeByKeyPress(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    const nextEntry = assertDefined(
      uiData.entries.find(
        (entry) =>
          entry.traceEntry.getTimestamp() > positionUpdateEntry.getTimestamp(),
      ),
    );
    expect(emitEventSpy).toHaveBeenCalledWith(
      new TracePositionUpdate(
        TracePosition.fromTraceEntry(nextEntry.traceEntry),
        true,
      ),
    );
  });

  it('does not propagate any position on right arrow key press if on last entry', async () => {
    const trace = assertDefined(
      lastEntryPositionUpdate.position.entry,
    ).getFullTrace();
    await presenter.onAppEvent(new ActiveTraceChanged(trace));

    const emitEventSpy = jasmine.createSpy();
    presenter.setEmitEvent(emitEventSpy);
    await presenter.onAppEvent(lastEntryPositionUpdate);

    await presenter.onPositionChangeByKeyPress(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    expect(emitEventSpy).not.toHaveBeenCalled();
  });

  it('propagates position with first prev trace entry with valid timestamp on left arrow key press', async () => {
    const trace = assertDefined(
      lastEntryPositionUpdate.position.entry,
    ).getFullTrace();
    await presenter.onAppEvent(new ActiveTraceChanged(trace));

    const emitEventSpy = jasmine.createSpy();
    presenter.setEmitEvent(emitEventSpy);
    await presenter.onAppEvent(lastEntryPositionUpdate);

    const prevIndex = assertDefined(uiData.currentIndex) - 1;
    spyOn(
      uiData.entries[prevIndex].traceEntry,
      'hasValidTimestamp',
    ).and.returnValue(false);
    await presenter.onPositionChangeByKeyPress(
      new KeyboardEvent('keydown', {key: 'ArrowLeft'}),
    );
    expect(emitEventSpy).toHaveBeenCalledWith(
      new TracePositionUpdate(
        TracePosition.fromTraceEntry(uiData.entries[prevIndex - 1].traceEntry),
        true,
      ),
    );
  });

  it('does not propagate any position on left arrow key press if on first entry', async () => {
    const trace = assertDefined(positionUpdate.position.entry).getFullTrace();
    await presenter.onAppEvent(new ActiveTraceChanged(trace));

    const emitEventSpy = jasmine.createSpy();
    presenter.setEmitEvent(emitEventSpy);
    await presenter.onAppEvent(positionUpdate);

    await presenter.onPositionChangeByKeyPress(
      new KeyboardEvent('keydown', {key: 'ArrowLeft'}),
    );
    expect(emitEventSpy).not.toHaveBeenCalled();
  });

  it('filters entries on select filter change', async () => {
    await presenter.onAppEvent(positionUpdate);
    const header = uiData.headers[1];

    await presenter.onSelectFilterChange(header, ['0']);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[1].value)),
    ).toEqual(new Set([0]));

    await presenter.onSelectFilterChange(header, ['0', '2', '3']);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[1].value)),
    ).toEqual(new Set([0, 2, 3]));

    await presenter.onSelectFilterChange(header, []);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[1].value)),
    ).toEqual(new Set([0, 1, 2, 3]));
  });

  it('filters entries on text filter change', async () => {
    await presenter.onAppEvent(positionUpdate);
    const header = uiData.headers[0];
    const filter = header.filter as LogTextFilter;

    filter.updateFilterValue(['stringValue']);
    await presenter.onTextFilterChange(header, filter.textFilter);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[0].value)),
    ).toEqual(new Set(['stringValue']));

    filter.updateFilterValue(['value']);
    await presenter.onTextFilterChange(header, filter.textFilter);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[0].value)),
    ).toEqual(new Set(['stringValue', 'differentValue']));

    filter.updateFilterValue(['']);
    await presenter.onTextFilterChange(header, filter.textFilter);
    expect(
      new Set(uiData.entries.map((entry) => entry.fields[0].value)),
    ).toEqual(new Set(['stringValue', 'differentValue']));
  });

  it('updates indices when filters change', async () => {
    await presenter.onAppEvent(lastEntryPositionUpdate);
    presenter.onLogEntryClick(1);
    expect(uiData.currentIndex).toEqual(3);
    expect(uiData.selectedIndex).toEqual(1);

    const header = uiData.headers[1];
    await presenter.onSelectFilterChange(header, ['0']);
    expect(uiData.currentIndex).toEqual(0);
    expect(uiData.selectedIndex).toEqual(0);

    await presenter.onSelectFilterChange(header, ['0', '2']);
    expect(uiData.currentIndex).toEqual(1);
    expect(uiData.selectedIndex).toEqual(0);

    await presenter.onSelectFilterChange(header, []);
    expect(uiData.currentIndex).toEqual(3);
    expect(uiData.selectedIndex).toEqual(0);
  });

  it('updates properties tree when entry clicked', async () => {
    await presenter.onAppEvent(positionUpdate);

    await presenter.onLogEntryClick(2);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[2].propertiesTree).id,
    );

    // does not remove selection when entry clicked again
    await presenter.onLogEntryClick(2);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[2].propertiesTree).id,
    );
  });

  it('updates properties tree when changed by key press', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onLogEntryClick(0);

    await presenter.onArrowDownPress();
    expect(uiData.selectedIndex).toEqual(1);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[1].propertiesTree).id,
    );

    await presenter.onArrowUpPress();
    expect(uiData.selectedIndex).toEqual(0);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[0].propertiesTree).id,
    );

    // does not remove selection if index out of range
    await presenter.onArrowUpPress();
    expect(uiData.selectedIndex).toEqual(0);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[0].propertiesTree).id,
    );

    // does not remove selection if index out of range
    await presenter.onLogEntryClick(3);
    await presenter.onArrowDownPress();
    expect(uiData.selectedIndex).toEqual(3);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      assertDefined(uiData.entries[3].propertiesTree).id,
    );
  });

  it('emits event on log timestamp click', async () => {
    await presenter.onAppEvent(positionUpdate);
    const spy = jasmine.createSpy();
    presenter.setEmitEvent(spy);

    await presenter.onLogTimestampClick(uiData.entries[0].traceEntry);
    expect(spy).toHaveBeenCalledWith(
      TracePositionUpdate.fromTraceEntry(uiData.entries[0].traceEntry, true),
    );
  });

  it('emits event on raw timestamp click', async () => {
    await presenter.onAppEvent(positionUpdate);
    const spy = jasmine.createSpy();
    presenter.setEmitEvent(spy);

    const ts = TimestampConverterUtils.makeZeroTimestamp();
    await presenter.onRawTimestampClick(ts);
    expect(spy).toHaveBeenCalledWith(
      TracePositionUpdate.fromTimestamp(ts, true),
    );
  });

  it('filters properties tree', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(3);
    await presenter.onPropertiesFilterChange(new TextFilter('pass'));
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(2);
  });

  it('shows/hides defaults', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(3);
    const userOptions: UserOptions = {
      showDefaults: {
        name: 'Show defaults',
        enabled: true,
      },
    };
    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(4);
  });

  it('updates dark mode', async () => {
    expect(uiData.isDarkMode).toBeFalse();
    await presenter.onAppEvent(new DarkModeToggled(true));
    expect(uiData.isDarkMode).toBeTrue();
  });

  it('is robust to empty trace', async () => {
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.TRANSACTIONS);
    const presenter = new MockPresenter(
      trace,
      new InMemoryStorage(),
      (newData) => (uiData = newData),
    );

    await presenter.onAppEvent(
      TracePositionUpdate.fromTimestamp(
        TimestampConverterUtils.makeRealTimestamp(0n),
      ),
    );

    expect(uiData.entries).toEqual([]);
    expect(uiData.selectedIndex).toBeUndefined();
    expect(uiData.scrollToIndex).toBeUndefined();
    expect(uiData.currentIndex).toBeUndefined();
    expect(uiData.headers.length).toEqual(3);
    expect(uiData.propertiesTree).toBeUndefined();
    expect(uiData.propertiesUserOptions).toBeDefined();
    expect(uiData.propertiesFilter).toBeDefined();
  });

  function makeElement(): HTMLElement {
    const element = document.createElement('div');
    element.style.height = '5px';
    element.style.width = '5px';
    return element;
  }

  function pressRightArrowKey() {
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowRight'}));
  }
});

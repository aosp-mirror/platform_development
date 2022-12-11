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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Parser} from "parsers/parser";
import {Presenter} from "./presenter";
import {UnitTestUtils} from "test/unit/utils";
import {UiData, UiDataEntryType} from "./ui_data";
import {TransactionsTraceEntry} from "../../common/trace/transactions";

describe("ViewerTransactionsPresenter", () => {
  let parser: Parser;
  let presenter: Presenter;
  let inputTraceEntryElapsed: TransactionsTraceEntry;
  let inputTraceEntriesElapsed: Map<TraceType, any>;
  let inputTraceEntryReal: TransactionsTraceEntry;
  let inputTraceEntriesReal: Map<TraceType, any>;
  let outputUiData: undefined|UiData;
  const TOTAL_OUTPUT_ENTRIES = 1504;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/elapsed_and_real_timestamp/Transactions.pb");
  });

  beforeEach(() => {
    const elapsedTimestamp = new Timestamp(TimestampType.ELAPSED, 2450981445n);
    inputTraceEntryElapsed = parser.getTraceEntry(elapsedTimestamp)!;
    inputTraceEntriesElapsed = new Map<TraceType, any>();
    inputTraceEntriesElapsed.set(TraceType.TRANSACTIONS, [inputTraceEntryElapsed]);

    const realTimestamp = new Timestamp(TimestampType.REAL, 16595075386004995520n);
    inputTraceEntryReal = parser.getTraceEntry(realTimestamp)!;
    inputTraceEntriesReal = new Map<TraceType, any>();
    inputTraceEntriesReal.set(TraceType.TRANSACTIONS, [inputTraceEntryReal]);

    outputUiData = undefined;

    presenter = new Presenter((data: UiData) => {
      outputUiData = data;
    });
  });

  it("is robust to undefined trace entry", () => {
    inputTraceEntriesElapsed = new Map<TraceType, any>();
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData).toEqual(UiData.EMPTY);
  });

  it("processes trace entry and computes output UI data", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    expect(outputUiData!.allPids).toEqual(["N/A", "0", "515", "1593", "2022", "2322", "2463", "3300"]);
    expect(outputUiData!.allUids).toEqual(["N/A", "1000", "1003", "10169", "10235", "10239"]);
    expect(outputUiData!.allTypes).toEqual(["DISPLAY_CHANGED", "LAYER_ADDED", "LAYER_CHANGED", "LAYER_HANDLE_REMOVED", "LAYER_REMOVED"]);
    expect(outputUiData!.allIds.length).toEqual(115);

    expect(outputUiData!.entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    expect(outputUiData?.currentEntryIndex).toEqual(0);
    expect(outputUiData?.selectedEntryIndex).toBeUndefined();
    expect(outputUiData?.scrollToIndex).toEqual(0);
    expect(outputUiData?.currentPropertiesTree).toBeDefined();
  });

  it("ignores undefined trace entry and doesn't discard previously computed UI data", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);

    presenter.notifyCurrentTraceEntries(new Map<TraceType, any>());
    expect(outputUiData!.entries.length).toEqual(TOTAL_OUTPUT_ENTRIES);
  });

  it("processes trace entry and updates current entry and scroll position", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.currentEntryIndex).toEqual(0);
    expect(outputUiData!.scrollToIndex).toEqual(0);

    (<TransactionsTraceEntry>inputTraceEntriesElapsed.get(TraceType.TRANSACTIONS)[0]).currentEntryIndex = 10;
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.currentEntryIndex).toEqual(13);
    expect(outputUiData!.scrollToIndex).toEqual(13);
  });

  it("filters entries according to PID filter", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    presenter.onPidFilterChanged([]);
    expect(new Set(outputUiData!.entries.map(entry => entry.pid)))
      .toEqual(new Set(["N/A", "0", "515", "1593", "2022", "2322", "2463", "3300"]));

    presenter.onPidFilterChanged(["0"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.pid)))
      .toEqual(new Set(["0"]));

    presenter.onPidFilterChanged(["0", "515"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.pid)))
      .toEqual(new Set(["0", "515"]));
  });

  it("filters entries according to UID filter", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    presenter.onUidFilterChanged([]);
    expect(new Set(outputUiData!.entries.map(entry => entry.uid)))
      .toEqual(new Set(["N/A", "1000", "1003", "10169", "10235", "10239"]));

    presenter.onUidFilterChanged(["1000"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.uid)))
      .toEqual(new Set(["1000"]));

    presenter.onUidFilterChanged(["1000", "1003"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.uid)))
      .toEqual(new Set(["1000", "1003"]));
  });

  it("filters entries according to type filter", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    presenter.onTypeFilterChanged([]);
    expect(new Set(outputUiData!.entries.map(entry => entry.type)))
      .toEqual(new Set([
        UiDataEntryType.DisplayChanged,
        UiDataEntryType.LayerAdded,
        UiDataEntryType.LayerChanged,
        UiDataEntryType.LayerRemoved,
        UiDataEntryType.LayerHandleRemoved
      ]));

    presenter.onTypeFilterChanged([UiDataEntryType.LayerAdded]);
    expect(new Set(outputUiData!.entries.map(entry => entry.type)))
      .toEqual(new Set([UiDataEntryType.LayerAdded]));

    presenter.onTypeFilterChanged([UiDataEntryType.LayerAdded, UiDataEntryType.LayerRemoved]);
    expect(new Set(outputUiData!.entries.map(entry => entry.type)))
      .toEqual(new Set([UiDataEntryType.LayerAdded, UiDataEntryType.LayerRemoved]));
  });

  it("filters entries according to ID filter", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    presenter.onIdFilterChanged([]);
    expect(new Set(outputUiData!.entries.map(entry => entry.id)).size)
      .toBeGreaterThan(20);

    presenter.onIdFilterChanged(["1"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.id)))
      .toEqual(new Set(["1"]));

    presenter.onIdFilterChanged(["1", "3"]);
    expect(new Set(outputUiData!.entries.map(entry => entry.id)))
      .toEqual(new Set(["1", "3"]));
  });

  it ("updates selected entry and properties tree when entry is clicked", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.currentEntryIndex).toEqual(0);
    expect(outputUiData!.selectedEntryIndex).toBeUndefined();
    expect(outputUiData!.scrollToIndex).toEqual(0);
    expect(outputUiData!.currentPropertiesTree)
      .toEqual(outputUiData!.entries[0].propertiesTree);

    presenter.onEntryClicked(10);
    expect(outputUiData!.currentEntryIndex).toEqual(0);
    expect(outputUiData!.selectedEntryIndex).toEqual(10);
    expect(outputUiData!.scrollToIndex).toBeUndefined(); // no scrolling
    expect(outputUiData!.currentPropertiesTree)
      .toEqual(outputUiData!.entries[10].propertiesTree);

    // remove selection when selected entry is clicked again
    presenter.onEntryClicked(10);
    expect(outputUiData!.currentEntryIndex).toEqual(0);
    expect(outputUiData!.selectedEntryIndex).toBeUndefined();
    expect(outputUiData!.scrollToIndex).toBeUndefined(); // no scrolling
    expect(outputUiData!.currentPropertiesTree)
      .toEqual(outputUiData!.entries[0].propertiesTree);
  });

  it("computes current entry index", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.currentEntryIndex).toEqual(0);

    (<TransactionsTraceEntry>inputTraceEntriesElapsed.get(TraceType.TRANSACTIONS)[0]).currentEntryIndex = 10;
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);
    expect(outputUiData!.currentEntryIndex).toEqual(13);
  });

  it("updates current entry index when filters change", () => {
    (<TransactionsTraceEntry>inputTraceEntriesElapsed.get(TraceType.TRANSACTIONS)[0]).currentEntryIndex = 10;
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    presenter.onPidFilterChanged([]);
    expect(outputUiData!.currentEntryIndex).toEqual(13);

    presenter.onPidFilterChanged(["0"]);
    expect(outputUiData!.currentEntryIndex).toEqual(10);

    presenter.onPidFilterChanged(["0", "515"]);
    expect(outputUiData!.currentEntryIndex).toEqual(11);

    presenter.onPidFilterChanged(["0", "515", "N/A"]);
    expect(outputUiData!.currentEntryIndex).toEqual(13);
  });

  it("formats real time", () => {
    (<TransactionsTraceEntry>inputTraceEntriesReal.get(TraceType.TRANSACTIONS)[0]).currentEntryIndex = 10;
    presenter.notifyCurrentTraceEntries(inputTraceEntriesReal);

    expect(outputUiData!.entries[0].time).toEqual("2022-08-03T06:19:01.051480997");
  });

  it("formats elapsed time", () => {
    (<TransactionsTraceEntry>inputTraceEntriesElapsed.get(TraceType.TRANSACTIONS)[0]).currentEntryIndex = 10;
    presenter.notifyCurrentTraceEntries(inputTraceEntriesElapsed);

    expect(outputUiData!.entries[0].time).toEqual("2s450ms981445ns");
  });
});

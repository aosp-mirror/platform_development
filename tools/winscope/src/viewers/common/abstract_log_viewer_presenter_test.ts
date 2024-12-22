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
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from './abstract_log_viewer_presenter';
import {TextFilter} from './text_filter';
import {LogEntry, LogFieldType, LogFieldValue, UiDataLog} from './ui_data_log';
import {
  LogFilterChangeDetail,
  LogTextFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from './viewer_events';

export abstract class AbstractLogViewerPresenterTest<UiData extends UiDataLog> {
  execute() {
    describe('AbstractLogViewerPresenterTest', () => {
      let uiData: UiData;
      let presenter: AbstractLogViewerPresenter<UiData>;
      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(async () => {
        presenter = await this.createPresenter((newData) => {
          uiData = newData;
        });
      });

      it('is robust to empty trace', async () => {
        const notifyViewCallback = (newData: UiData) => {
          uiData = newData;
        };
        const presenter = await this.createPresenterWithEmptyTrace(
          notifyViewCallback,
        );

        const positionUpdateWithoutTraceEntry =
          TracePositionUpdate.fromTimestamp(
            TimestampConverterUtils.makeRealTimestamp(0n),
          );
        await presenter.onAppEvent(positionUpdateWithoutTraceEntry);

        expect(uiData.entries).toEqual([]);
        expect(uiData.selectedIndex).toBeUndefined();
        expect(uiData.scrollToIndex).toBeUndefined();
        expect(uiData.currentIndex).toBeUndefined();

        if (this.shouldExecuteFilterTests) {
          expect(uiData.filters?.length).toBeGreaterThan(0);
        }

        if (this.shouldExecuteHeaderTests) {
          expect(uiData.headers?.length).toBeGreaterThan(0);
        }

        if (this.shouldExecutePropertiesTests) {
          expect(uiData.propertiesTree).toBeUndefined();
          expect(uiData.propertiesUserOptions).toBeDefined();
          if (this.executePropertiesChecksForEmptyTrace) {
            this.executePropertiesChecksForEmptyTrace(uiData);
          }
        }
      });

      it('adds events listeners', async () => {
        const element = document.createElement('div');
        presenter.addEventListeners(element);

        let spy: jasmine.Spy = spyOn(presenter, 'onFilterChange');
        const filterDetail = new LogFilterChangeDetail(
          LogFieldType.CUJ_TYPE,
          '',
        );
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.LogFilterChange, {
            detail: filterDetail,
          }),
        );
        expect(spy).toHaveBeenCalledWith(filterDetail.type, filterDetail.value);

        spy = spyOn(presenter, 'onTextFilterChange');
        const textFilterDetail = new LogTextFilterChangeDetail(
          LogFieldType.CUJ_TYPE,
          new TextFilter('', []),
        );
        element.dispatchEvent(
          new CustomEvent(ViewerEvents.LogTextFilterChange, {
            detail: textFilterDetail,
          }),
        );
        expect(spy).toHaveBeenCalledWith(
          textFilterDetail.type,
          textFilterDetail.filter,
        );

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

        await presenter.onAppEvent(this.getPositionUpdate());
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
      });

      it('processes trace position updates', async () => {
        await assertDefined(presenter).onAppEvent(
          assertDefined(this.getPositionUpdate()),
        );
        expect(uiData.scrollToIndex).toEqual(
          this.expectedIndexOfFirstPositionUpdate,
        );
        expect(uiData.currentIndex).toEqual(
          this.expectedIndexOfFirstPositionUpdate,
        );
        expect(uiData.selectedIndex).toBeUndefined();
        expect(uiData.entries.length).toEqual(this.totalOutputEntries);

        if (this.shouldExecutePropertiesTests) {
          expect(assertDefined(uiData.propertiesTree).id).toEqual(
            assertDefined(uiData.entries[0].propertiesTree).id,
          );
          if (this.executePropertiesChecksAfterPositionUpdate) {
            this.executePropertiesChecksAfterPositionUpdate(uiData);
          }
        }

        if (this.shouldExecuteFilterTests) {
          for (const [logFieldType, expectedOptions] of assertDefined(
            this.expectedInitialFilterOptions,
          )) {
            const options = assertDefined(
              uiData.filters?.find((f) => f.type === logFieldType)?.options,
            );
            if (Array.isArray(expectedOptions)) {
              expect(options).toEqual(expectedOptions);
            } else {
              expect(options.length).toEqual(expectedOptions);
            }
          }
        }
      });

      it('processes trace position update and updates current entry and scroll position', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        this.checkInitialTracePositionUpdate(uiData);

        await presenter.onAppEvent(this.getSecondPositionUpdate());
        expect(uiData.currentIndex).toEqual(
          this.expectedIndexOfSecondPositionUpdate,
        );
        expect(uiData.selectedIndex).toBeUndefined();

        if (this.shouldExecutePropertiesTests) {
          expect(assertDefined(uiData.propertiesTree).id).toEqual(
            assertDefined(
              uiData.entries[this.expectedIndexOfSecondPositionUpdate]
                .propertiesTree,
            ).id,
          );
        }
      });

      if (this.shouldExecuteFilterTests) {
        it('filters entries', async () => {
          for (const [type, valuesToSet] of assertDefined(
            this.filterValuesToSet,
          )) {
            const expected = assertDefined(
              this.expectedFieldValuesAfterFilter?.get(type),
            );
            for (let i = 0; i < valuesToSet.length; i++) {
              const filterValues = valuesToSet[i];
              const expectedFieldValues = expected[i];

              await presenter.onFilterChange(type, filterValues);
              const fieldValues = uiData.entries.map((entry) =>
                assertDefined(this.getFieldValue(entry, type)),
              );
              if (Array.isArray(expectedFieldValues)) {
                expect(new Set(fieldValues)).toEqual(
                  new Set(expectedFieldValues),
                );
              } else {
                expect(fieldValues.length).toEqual(expectedFieldValues);
              }
              await presenter.onFilterChange(type, []);
            }
          }
        });

        it('updates indices when filters change', async () => {
          await presenter.onAppEvent(this.getSecondPositionUpdate());
          const filterName = assertDefined(this.filterNameForCurrentIndexTest);
          await presenter.onFilterChange(filterName, []);
          expect(uiData.currentIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
          expect(uiData.scrollToIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
          expect(uiData.selectedIndex).toEqual(undefined);

          const finalEntryIndex = uiData.entries.length - 1;
          await presenter.onLogEntryClick(finalEntryIndex);
          expect(uiData.selectedIndex).toEqual(finalEntryIndex);

          await presenter.onFilterChange(
            filterName,
            assertDefined(this.filterChangeForCurrentIndexTest),
          );
          expect(uiData.currentIndex).toEqual(
            this.expectedCurrentIndexAfterFilterChange,
          );
          expect(uiData.scrollToIndex).toEqual(
            this.expectedCurrentIndexAfterFilterChange,
          );

          let expectedSelectedIndex =
            finalEntryIndex <= uiData.entries.length - 1
              ? finalEntryIndex
              : this.expectedCurrentIndexAfterFilterChange;
          expect(uiData.selectedIndex).toEqual(expectedSelectedIndex);

          await presenter.onFilterChange(
            filterName,
            assertDefined(this.secondFilterChangeForCurrentIndexTest),
          );
          expect(uiData.currentIndex).toEqual(
            this.expectedCurrentIndexAfterSecondFilterChange,
          );
          expect(uiData.scrollToIndex).toEqual(
            this.expectedCurrentIndexAfterSecondFilterChange,
          );

          const prevStillValid =
            expectedSelectedIndex !== undefined &&
            expectedSelectedIndex <= uiData.entries.length - 1;
          expectedSelectedIndex = prevStillValid
            ? expectedSelectedIndex
            : this.expectedCurrentIndexAfterSecondFilterChange;
          expect(uiData.selectedIndex).toEqual(expectedSelectedIndex);
        });
      }

      it('updates selected entry ui data when entry clicked', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        this.checkInitialTracePositionUpdate(uiData);

        await presenter.onLogEntryClick(this.logEntryClickIndex);
        this.checkSelectedEntryUiData(uiData, this.logEntryClickIndex);
        expect(uiData.scrollToIndex).toEqual(undefined); // no scrolling

        // does not remove selection when entry clicked again
        await presenter.onLogEntryClick(this.logEntryClickIndex);
        this.checkSelectedEntryUiData(uiData, this.logEntryClickIndex);
        expect(uiData.scrollToIndex).toEqual(undefined); // no scrolling
      });

      it('updates selected entry ui data when entry changed by key press', async () => {
        await presenter.onLogEntryClick(0);

        await presenter.onArrowDownPress();
        this.checkSelectedAndScrollIndices(uiData, 1);

        await presenter.onArrowUpPress();
        this.checkSelectedAndScrollIndices(uiData, 0);

        // robust to attempt to go before first entry
        await presenter.onArrowUpPress();
        this.checkSelectedAndScrollIndices(uiData, 0);

        // robust to attempt to go beyond last entry
        const finalIndex = uiData.entries.length - 1;
        await presenter.onLogEntryClick(finalIndex);
        await presenter.onArrowDownPress();
        expect(uiData.selectedIndex).toEqual(finalIndex);
      });

      it('computes index based on trace position update', async () => {
        await presenter.onAppEvent(this.getPositionUpdate());
        expect(uiData.currentIndex).toEqual(
          this.expectedIndexOfFirstPositionUpdate,
        );

        await presenter.onAppEvent(this.getSecondPositionUpdate());
        expect(uiData.currentIndex).toEqual(
          this.expectedIndexOfSecondPositionUpdate,
        );
      });

      it('emits event on log timestamp click', async () => {
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);

        await presenter.onLogTimestampClick(uiData.entries[0].traceEntry);
        expect(spy).toHaveBeenCalledWith(
          TracePositionUpdate.fromTraceEntry(
            uiData.entries[0].traceEntry,
            true,
          ),
        );
      });

      it('emits event on raw timestamp click', async () => {
        const spy = jasmine.createSpy();
        presenter.setEmitEvent(spy);

        const ts = TimestampConverterUtils.makeZeroTimestamp();
        await presenter.onRawTimestampClick(ts);
        expect(spy).toHaveBeenCalledWith(
          TracePositionUpdate.fromTimestamp(ts, true),
        );
      });

      if (!this.shouldExecutePropertiesTests) {
        it('is robust to attempts to change properties', async () => {
          await presenter.onAppEvent(this.getPositionUpdate());
          await presenter.onLogEntryClick(this.logEntryClickIndex);

          await presenter.onPropertiesUserOptionsChange({});
          expect(uiData.propertiesUserOptions).toBeUndefined();
          expect(uiData.propertiesTree).toBeUndefined();
        });
      }
    });

    if (this.executeSpecializedTests) {
      this.executeSpecializedTests();
    }
  }

  checkInitialTracePositionUpdate(uiData: UiDataLog) {
    expect(uiData.entries.length).toEqual(this.totalOutputEntries);
    expect(uiData.scrollToIndex).toEqual(
      this.expectedIndexOfFirstPositionUpdate,
    );
    expect(uiData.currentIndex).toEqual(
      this.expectedIndexOfFirstPositionUpdate,
    );
    expect(uiData.selectedIndex).toBeUndefined();

    if (this.shouldExecuteFilterTests) {
      expect(uiData.filters?.length).toBeGreaterThan(0);
    }

    if (this.shouldExecuteHeaderTests) {
      expect(uiData.headers?.length).toBeGreaterThan(0);
    }

    if (this.shouldExecutePropertiesTests) {
      expect(uiData.propertiesTree).toBeDefined();
      expect(uiData.propertiesUserOptions).toBeDefined();
    }
  }

  getFieldValue(entry: LogEntry, logFieldType: LogFieldType) {
    return entry.fields.find((f) => f.type === logFieldType)?.value;
  }

  checkSelectedEntryUiData(uiData: UiDataLog, newIndex: number | undefined) {
    expect(uiData.selectedIndex).toEqual(newIndex);
    expect(uiData.currentIndex).toEqual(
      this.expectedIndexOfFirstPositionUpdate,
    );

    if (this.shouldExecutePropertiesTests) {
      if (newIndex !== undefined) {
        expect(assertDefined(uiData.propertiesTree).id).toEqual(
          assertDefined(uiData.entries[newIndex].propertiesTree).id,
        );
      } else {
        expect(uiData.propertiesTree).toBeUndefined();
      }
    }
  }

  checkSelectedAndScrollIndices(uiData: UiDataLog, index: number | undefined) {
    this.checkSelectedEntryUiData(uiData, index);
    expect(uiData.scrollToIndex).toEqual(index);
  }

  abstract readonly shouldExecuteHeaderTests: boolean;
  abstract readonly shouldExecuteFilterTests: boolean;
  abstract readonly shouldExecutePropertiesTests: boolean;

  abstract readonly totalOutputEntries: number;
  abstract readonly expectedIndexOfFirstPositionUpdate: number;
  abstract readonly expectedIndexOfSecondPositionUpdate: number;
  abstract readonly logEntryClickIndex: number;

  readonly expectedInitialFilterOptions?: Map<LogFieldType, string[] | number>;
  readonly filterValuesToSet?: Map<LogFieldType, Array<string[] | string>>;
  readonly expectedFieldValuesAfterFilter?: Map<
    LogFieldType,
    Array<LogFieldValue[] | number>
  >;
  readonly filterNameForCurrentIndexTest?: LogFieldType;
  readonly filterChangeForCurrentIndexTest?: string[];
  readonly expectedCurrentIndexAfterFilterChange?: number;
  readonly secondFilterChangeForCurrentIndexTest?: string[];
  readonly expectedCurrentIndexAfterSecondFilterChange?: number;

  abstract setUpTestEnvironment(): Promise<void>;
  abstract createPresenter(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<AbstractLogViewerPresenter<UiData>>;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType<UiData>,
  ): Promise<AbstractLogViewerPresenter<UiData>>;
  abstract getPositionUpdate(): TracePositionUpdate;
  abstract getSecondPositionUpdate(): TracePositionUpdate;

  executePropertiesChecksForEmptyTrace?(uiData: UiDataLog): void;
  executePropertiesChecksAfterPositionUpdate?(uiData: UiDataLog): void;
  executeSpecializedTests?(): void;
}

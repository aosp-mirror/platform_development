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
import {LogEntry, LogFieldName, LogFieldValue, UiDataLog} from './ui_data_log';

export abstract class AbstractLogViewerPresenterTest {
  execute() {
    describe('AbstractLogViewerPresenterTest', () => {
      let uiData: UiDataLog;
      let presenter: AbstractLogViewerPresenter;
      beforeAll(async () => {
        await this.setUpTestEnvironment();
      });

      beforeEach(async () => {
        presenter = await this.createPresenter((newData) => {
          uiData = newData;
        });
      });

      it('is robust to empty trace', async () => {
        const notifyViewCallback = (newData: UiDataLog) => {
          uiData = newData;
        };
        const presenter =
          this.createPresenterWithEmptyTrace(notifyViewCallback);

        const positionUpdateWithoutTraceEntry =
          TracePositionUpdate.fromTimestamp(
            TimestampConverterUtils.makeRealTimestamp(0n),
          );
        await presenter.onAppEvent(positionUpdateWithoutTraceEntry);

        expect(uiData.entries).toEqual([]);
        expect(uiData.selectedIndex).toBeUndefined();
        expect(uiData.scrollToIndex).toBeUndefined();

        if (this.shouldExecuteFilterTests) {
          expect(uiData.filters?.length).toBeGreaterThan(0);
        }

        if (this.shouldExecuteHeaderTests) {
          expect(uiData.headers?.length).toBeGreaterThan(0);
        }

        if (this.shouldExecuteCurrentIndexTests) {
          expect(uiData.currentIndex).toBeUndefined();
        }

        if (this.shouldExecutePropertiesTests) {
          expect(uiData.propertiesTree).toBeUndefined();
          expect(uiData.propertiesUserOptions).toBeDefined();
        }
      });

      it('processes trace position updates', async () => {
        await assertDefined(presenter).onAppEvent(
          assertDefined(this.getPositionUpdate()),
        );
        expect(uiData.scrollToIndex).toEqual(
          this.expectedIndexOfFirstPositionUpdate,
        );

        if (this.shouldExecuteCurrentIndexTests) {
          expect(uiData.currentIndex).toEqual(
            this.expectedIndexOfFirstPositionUpdate,
          );
          expect(uiData.selectedIndex).toBeUndefined();
        } else {
          expect(uiData.selectedIndex).toEqual(
            this.expectedIndexOfFirstPositionUpdate,
          );
        }

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
          for (const [logFieldName, expectedOptions] of assertDefined(
            this.expectedInitialFilterOptions,
          )) {
            const options = assertDefined(
              uiData.filters?.find((f) => f.name === logFieldName)?.options,
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
        if (this.shouldExecuteCurrentIndexTests) {
          expect(uiData.currentIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
          expect(uiData.selectedIndex).toBeUndefined();
        } else {
          expect(uiData.selectedIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
        }

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
          for (const [name, valuesToSet] of assertDefined(
            this.filterValuesToSet,
          )) {
            const expected = assertDefined(
              this.expectedFieldValuesAfterFilter?.get(name),
            );
            for (let i = 0; i < valuesToSet.length; i++) {
              const filterValues = valuesToSet[i];
              const expectedFieldValues = expected[i];

              await presenter.onFilterChange(name, filterValues);
              const fieldValues = uiData.entries.map((entry) =>
                assertDefined(this.getFieldValue(entry, name)),
              );
              if (Array.isArray(expectedFieldValues)) {
                expect(new Set(fieldValues)).toEqual(
                  new Set(expectedFieldValues),
                );
              } else {
                expect(fieldValues.length).toEqual(expectedFieldValues);
              }
              await presenter.onFilterChange(name, []);
            }
          }
        });

        if (this.shouldExecuteCurrentIndexTests) {
          it('updates current index when filters change', async () => {
            await presenter.onAppEvent(this.getSecondPositionUpdate());
            const filterName = assertDefined(
              this.filterNameForCurrentIndexTest,
            );
            await presenter.onFilterChange(filterName, []);
            expect(uiData.currentIndex).toEqual(
              this.expectedIndexOfSecondPositionUpdate,
            );

            await presenter.onFilterChange(
              filterName,
              assertDefined(this.filterChangeForCurrentIndexTest),
            );
            expect(uiData.currentIndex).toEqual(
              this.expectedCurrentIndexAfterFilterChange,
            );

            await presenter.onFilterChange(
              filterName,
              assertDefined(this.secondFilterChangeForCurrentIndexTest),
            );
            expect(uiData.currentIndex).toEqual(
              this.expectedCurrentIndexAfterSecondFilterChange,
            );
          });
        }
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
        if (this.shouldExecuteCurrentIndexTests) {
          expect(uiData.currentIndex).toEqual(
            this.expectedIndexOfFirstPositionUpdate,
          );
        } else {
          expect(uiData.selectedIndex).toEqual(
            this.expectedIndexOfFirstPositionUpdate,
          );
        }

        await presenter.onAppEvent(this.getSecondPositionUpdate());
        if (this.shouldExecuteCurrentIndexTests) {
          expect(uiData.currentIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
        } else {
          expect(uiData.selectedIndex).toEqual(
            this.expectedIndexOfSecondPositionUpdate,
          );
        }
      });
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

    if (this.shouldExecuteFilterTests) {
      expect(uiData.filters?.length).toBeGreaterThan(0);
    }

    if (this.shouldExecuteHeaderTests) {
      expect(uiData.headers?.length).toBeGreaterThan(0);
    }

    if (this.shouldExecuteCurrentIndexTests) {
      expect(uiData.currentIndex).toEqual(
        this.expectedIndexOfFirstPositionUpdate,
      );
      expect(uiData.selectedIndex).toBeUndefined();
    } else {
      expect(uiData.selectedIndex).toEqual(
        this.expectedIndexOfFirstPositionUpdate,
      );
    }

    if (this.shouldExecutePropertiesTests) {
      expect(uiData.propertiesTree).toBeDefined();
      expect(uiData.propertiesUserOptions).toBeDefined();
    }
  }

  getFieldValue(entry: LogEntry, logFieldName: LogFieldName) {
    return entry.fields.find((f) => f.name === logFieldName)?.value;
  }

  checkSelectedEntryUiData(uiData: UiDataLog, newIndex: number | undefined) {
    expect(uiData.selectedIndex).toEqual(newIndex);
    if (this.shouldExecuteCurrentIndexTests) {
      expect(uiData.currentIndex).toEqual(0);
    }
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
  abstract readonly shouldExecuteCurrentIndexTests: boolean;
  abstract readonly shouldExecutePropertiesTests: boolean;

  abstract readonly totalOutputEntries: number;
  abstract readonly expectedIndexOfFirstPositionUpdate: number;
  abstract readonly expectedIndexOfSecondPositionUpdate: number;
  abstract readonly logEntryClickIndex: number;

  readonly expectedInitialFilterOptions?: Map<LogFieldName, string[] | number>;
  readonly filterValuesToSet?: Map<LogFieldName, Array<string[] | string>>;
  readonly expectedFieldValuesAfterFilter?: Map<
    LogFieldName,
    Array<LogFieldValue[] | number>
  >;
  readonly filterNameForCurrentIndexTest?: LogFieldName;
  readonly filterChangeForCurrentIndexTest?: string[];
  readonly expectedCurrentIndexAfterFilterChange?: number;
  readonly secondFilterChangeForCurrentIndexTest?: string[];
  readonly expectedCurrentIndexAfterSecondFilterChange?: number;

  abstract setUpTestEnvironment(): Promise<void>;
  abstract createPresenter(
    callback: NotifyLogViewCallbackType,
  ): Promise<AbstractLogViewerPresenter>;
  abstract createPresenterWithEmptyTrace(
    callback: NotifyLogViewCallbackType,
  ): AbstractLogViewerPresenter;
  abstract getPositionUpdate(): TracePositionUpdate;
  abstract getSecondPositionUpdate(): TracePositionUpdate;

  executePropertiesChecksAfterPositionUpdate?(uiData: UiDataLog): void;
  executeSpecializedTests?(): void;
}

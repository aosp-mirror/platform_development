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
import {LogMessage, ProtoLogTraceEntry} from 'trace/protolog';
import {TraceType} from 'trace/trace_type';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('ViewerProtoLogPresenter', () => {
  let presenter: Presenter;
  let inputMessages: LogMessage[];
  let inputTraceEntries: Map<TraceType, any>;
  let outputUiData: undefined | UiData;

  beforeEach(async () => {
    inputMessages = [
      new LogMessage('text0', 'time', 'tag0', 'level0', 'sourcefile0', 10n),
      new LogMessage('text1', 'time', 'tag1', 'level1', 'sourcefile1', 10n),
      new LogMessage('text2', 'time', 'tag2', 'level2', 'sourcefile2', 10n),
    ];
    inputTraceEntries = new Map<TraceType, any>();
    inputTraceEntries.set(TraceType.PROTO_LOG, [new ProtoLogTraceEntry(inputMessages, 0)]);

    outputUiData = undefined;

    presenter = new Presenter((data: UiData) => {
      outputUiData = data;
    });
  });

  it('is robust to undefined trace entry', () => {
    presenter.notifyCurrentTraceEntries(new Map<TraceType, any>());
    expect(outputUiData!.messages).toEqual([]);
    expect(outputUiData!.currentMessageIndex).toBeUndefined();
  });

  it("ignores undefined trace entry and doesn't discard displayed messages", () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.notifyCurrentTraceEntries(new Map<TraceType, any>());
    expect(outputUiData!.messages).toEqual(inputMessages);
  });

  it('processes current trace entries', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);

    expect(outputUiData!.allLogLevels).toEqual(['level0', 'level1', 'level2']);
    expect(outputUiData!.allTags).toEqual(['tag0', 'tag1', 'tag2']);
    expect(outputUiData!.allSourceFiles).toEqual(['sourcefile0', 'sourcefile1', 'sourcefile2']);
    expect(outputUiData!.messages).toEqual(inputMessages);
    expect(outputUiData!.currentMessageIndex).toEqual(0);
  });

  it('updated displayed messages according to log levels filter', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onLogLevelsFilterChanged([]);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onLogLevelsFilterChanged(['level1']);
    expect(outputUiData!.messages).toEqual([inputMessages[1]]);

    presenter.onLogLevelsFilterChanged(['level0', 'level1', 'level2']);
    expect(outputUiData!.messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to tags filter', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onTagsFilterChanged([]);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onTagsFilterChanged(['tag1']);
    expect(outputUiData!.messages).toEqual([inputMessages[1]]);

    presenter.onTagsFilterChanged(['tag0', 'tag1', 'tag2']);
    expect(outputUiData!.messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to source files filter', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onSourceFilesFilterChanged([]);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onSourceFilesFilterChanged(['sourcefile1']);
    expect(outputUiData!.messages).toEqual([inputMessages[1]]);

    presenter.onSourceFilesFilterChanged(['sourcefile0', 'sourcefile1', 'sourcefile2']);
    expect(outputUiData!.messages).toEqual(inputMessages);
  });

  it('updates displayed messages according to search string filter', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('');
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('text');
    expect(outputUiData!.messages).toEqual(inputMessages);

    presenter.onSearchStringFilterChanged('text0');
    expect(outputUiData!.messages).toEqual([inputMessages[0]]);

    presenter.onSearchStringFilterChanged('text1');
    expect(outputUiData!.messages).toEqual([inputMessages[1]]);
  });

  it('computes current message index', () => {
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    presenter.onLogLevelsFilterChanged([]);
    expect(outputUiData!.currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level0']);
    expect(outputUiData!.currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged([]);
    expect(outputUiData!.currentMessageIndex).toEqual(0);

    (inputTraceEntries.get(TraceType.PROTO_LOG)[0] as ProtoLogTraceEntry).currentMessageIndex = 1;
    presenter.notifyCurrentTraceEntries(inputTraceEntries);
    presenter.onLogLevelsFilterChanged([]);
    expect(outputUiData!.currentMessageIndex).toEqual(1);

    presenter.onLogLevelsFilterChanged(['level0']);
    expect(outputUiData!.currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level1']);
    expect(outputUiData!.currentMessageIndex).toEqual(0);

    presenter.onLogLevelsFilterChanged(['level0', 'level1']);
    expect(outputUiData!.currentMessageIndex).toEqual(1);
  });
});

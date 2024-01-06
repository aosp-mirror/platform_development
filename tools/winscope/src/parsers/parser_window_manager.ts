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
import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {WindowManagerState} from 'flickerlib/windows/WindowManagerState';
import root from 'protos/windowmanager/latest/root';
import {com} from 'protos/windowmanager/latest/types';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';
import {ParserWindowManagerUtils} from './parser_window_manager_utils';

export class ParserWindowManager extends AbstractParser {
  private static readonly WindowManagerTraceFileProto = root.lookupType(
    'com.android.server.wm.WindowManagerTraceFileProto'
  );

  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  override getTraceType(): TraceType {
    return TraceType.WINDOW_MANAGER;
  }

  override getMagicNumber(): number[] {
    return ParserWindowManager.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): com.android.server.wm.IWindowManagerTraceProto[] {
    const decoded = ParserWindowManager.WindowManagerTraceFileProto.decode(
      buffer
    ) as com.android.server.wm.IWindowManagerTraceFileProto;
    const timeOffset = BigInt(decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0');
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  override getTimestamp(
    type: TimestampType,
    entry: com.android.server.wm.IWindowManagerTraceProto
  ): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()));
    } else if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new Timestamp(
        type,
        this.realToElapsedTimeOffsetNs +
          BigInt(assertDefined(entry.elapsedRealtimeNanos).toString())
      );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: com.android.server.wm.IWindowManagerTraceProto
  ): WindowManagerState {
    return WindowManagerState.fromProto(
      entry.windowManagerService,
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
      entry.where,
      this.realToElapsedTimeOffsetNs,
      timestampType === TimestampType.ELAPSED /*useElapsedTime*/
    );
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE, () => {
        const result: CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE] =
          [];
        this.decodedEntries
          .slice(entriesRange.start, entriesRange.end)
          .forEach((windowManagerTraceProto) => {
            ParserWindowManagerUtils.parseWindowsTokenAndTitle(
              windowManagerTraceProto?.windowManagerService?.rootWindowContainer,
              result
            );
          });
        return Promise.resolve(result);
      })
      .getResult();
  }

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WINTRACE
}

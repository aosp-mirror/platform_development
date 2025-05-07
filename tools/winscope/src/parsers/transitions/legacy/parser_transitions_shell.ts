/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {Timestamp} from 'common/time/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import root from 'protos/transitions/udc/json';
import {com} from 'protos/transitions/udc/static';
import {TraceType} from 'trace/trace_type';

type TransitionProto = com.android.wm.shell.ITransition;
type HandlerProto = com.android.wm.shell.IHandlerMapping;

export class ParserTransitionsShell extends AbstractParser<
  TransitionProto,
  TransitionProto
> {
  private static readonly WmShellTransitionsTraceProto = root.lookupType(
    'com.android.wm.shell.WmShellTransitionTraceProto',
  );

  private realToBootTimeOffsetNs: bigint | undefined;
  private handlerMapping: undefined | HandlerProto[];

  override getTraceType(): TraceType {
    return TraceType.SHELL_TRANSITION;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(traceBuffer: Uint8Array): TransitionProto[] {
    const decodedProto =
      ParserTransitionsShell.WmShellTransitionsTraceProto.decode(
        traceBuffer,
      ) as unknown as com.android.wm.shell.IWmShellTransitionTraceProto;
    const timeOffset = BigInt(
      decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    this.handlerMapping = decodedProto.handlerMappings ?? [];
    return decodedProto.transitions ?? [];
  }

  getShellHandlerMapping(): HandlerProto[] {
    return assertDefined(this.handlerMapping);
  }

  override processDecodedEntry(
    index: number,
    entryProto: TransitionProto,
  ): TransitionProto {
    return entryProto;
  }

  protected override getTimestamp(entry: TransitionProto): Timestamp {
    return entry.dispatchTimeNs
      ? this.timestampConverter.makeTimestampFromBootTimeNs(
          BigInt(entry.dispatchTimeNs.toString()),
        )
      : this.timestampConverter.makeZeroTimestamp();
  }

  protected getMagicNumber(): number[] | undefined {
    return [0x09, 0x57, 0x4d, 0x53, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WMSTRACE
  }
}

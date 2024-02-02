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
import root from 'protos/protolog/latest/json';
import {com} from 'protos/protolog/latest/static';
import {FormattedLogMessage, LogMessage, UnformattedLogMessage} from 'trace/protolog';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import configJson from '../../../../../frameworks/base/data/etc/services.core.protolog.json';
import {AbstractParser} from './abstract_parser';

class ParserProtoLog extends AbstractParser {
  private static readonly ProtoLogFileProto = root.lookupType(
    'com.android.internal.protolog.ProtoLogFileProto'
  );

  constructor(trace: TraceFile) {
    super(trace);
  }

  override getTraceType(): TraceType {
    return TraceType.PROTO_LOG;
  }

  override getMagicNumber(): number[] {
    return ParserProtoLog.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): com.android.internal.protolog.IProtoLogMessage[] {
    const fileProto = ParserProtoLog.ProtoLogFileProto.decode(
      buffer
    ) as com.android.internal.protolog.IProtoLogFileProto;

    if (fileProto.version !== ParserProtoLog.PROTOLOG_VERSION) {
      const message = 'Unsupported ProtoLog trace version';
      console.log(message);
      throw new TypeError(message);
    }

    if (configJson.version !== ParserProtoLog.PROTOLOG_VERSION) {
      const message = 'Unsupported ProtoLog JSON config version';
      console.log(message);
      throw new TypeError(message);
    }

    this.realToElapsedTimeOffsetNs =
      BigInt(assertDefined(fileProto.realTimeToElapsedTimeOffsetMillis).toString()) * 1000000n;

    if (!fileProto.log) {
      return [];
    }

    fileProto.log.sort(
      (
        a: com.android.internal.protolog.IProtoLogMessage,
        b: com.android.internal.protolog.IProtoLogMessage
      ) => {
        return Number(a.elapsedRealtimeNanos) - Number(b.elapsedRealtimeNanos);
      }
    );

    return fileProto.log;
  }

  override getTimestamp(
    type: TimestampType,
    entry: com.android.internal.protolog.IProtoLogMessage
  ): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()));
    }
    if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new Timestamp(
        type,
        BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()) +
          this.realToElapsedTimeOffsetNs
      );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: com.android.internal.protolog.IProtoLogMessage
  ): LogMessage {
    const message = (configJson as any).messages[assertDefined(entry.messageHash)];
    if (!message) {
      return new FormattedLogMessage(entry, timestampType, this.realToElapsedTimeOffsetNs);
    }

    try {
      return new UnformattedLogMessage(
        entry,
        timestampType,
        this.realToElapsedTimeOffsetNs,
        message
      );
    } catch (error) {
      if (error instanceof FormatStringMismatchError) {
        return new FormattedLogMessage(entry, timestampType, this.realToElapsedTimeOffsetNs);
      }
      throw error;
    }
  }

  private realToElapsedTimeOffsetNs: undefined | bigint = undefined;
  private static readonly MAGIC_NUMBER = [0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47]; // .PROTOLOG
  private static readonly PROTOLOG_VERSION = '1.0.0';
}

class FormatStringMismatchError extends Error {
  constructor(message: string) {
    super(message);
  }
}

export {ParserProtoLog};

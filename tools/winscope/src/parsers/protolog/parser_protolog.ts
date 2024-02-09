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
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import configJson from '../../../../../../frameworks/base/data/etc/services.core.protolog.json';
import {AbstractParser} from '../abstract_parser';
import {LogMessage} from './log_message';
import {ParserProtologUtils} from './parser_protolog_utils';

class ParserProtoLog extends AbstractParser {
  private static readonly ProtoLogFileProto = root.lookupType(
    'com.android.internal.protolog.ProtoLogFileProto'
  );
  private static readonly MAGIC_NUMBER = [0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47]; // .PROTOLOG
  private static readonly PROTOLOG_VERSION = '1.0.0';

  protected override shouldAddDefaultsToProto: boolean = false;
  private realToElapsedTimeOffsetNs: bigint | undefined;

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
  ): PropertyTreeNode {
    const message: ConfigMessage | undefined = (configJson as ProtologConfig).messages[
      assertDefined(entry.messageHash)
    ];
    const logMessage = this.makeLogMessage(entry, message);
    return ParserProtologUtils.makeMessagePropertiesTree(
      logMessage,
      timestampType,
      this.realToElapsedTimeOffsetNs
    );
  }

  private makeLogMessage(
    entry: com.android.internal.protolog.IProtoLogMessage,
    message: ConfigMessage | undefined
  ): LogMessage {
    if (!message) {
      return this.makeLogMessageWithoutFormat(entry);
    }
    try {
      return this.makeLogMessageWithFormat(entry, message);
    } catch (error) {
      if (error instanceof FormatStringMismatchError) {
        return this.makeLogMessageWithoutFormat(entry);
      }
      throw error;
    }
  }

  private makeLogMessageWithFormat(
    entry: com.android.internal.protolog.IProtoLogMessage,
    message: ConfigMessage
  ): LogMessage {
    let text = '';

    const strParams: string[] = assertDefined(entry.strParams);
    let strParamsIdx = 0;
    const sint64Params: Array<bigint> = assertDefined(entry.sint64Params).map((param) =>
      BigInt(param.toString())
    );
    let sint64ParamsIdx = 0;
    const doubleParams: number[] = assertDefined(entry.doubleParams);
    let doubleParamsIdx = 0;
    const booleanParams: boolean[] = assertDefined(entry.booleanParams);
    let booleanParamsIdx = 0;

    const messageFormat = message.message;
    for (let i = 0; i < messageFormat.length; ) {
      if (messageFormat[i] === '%') {
        if (i + 1 >= messageFormat.length) {
          // Should never happen - protologtool checks for that
          throw new Error('Invalid format string');
        }
        switch (messageFormat[i + 1]) {
          case '%':
            text += '%';
            break;
          case 'd':
            text += this.getParam(sint64Params, sint64ParamsIdx++).toString(10);
            break;
          case 'o':
            text += this.getParam(sint64Params, sint64ParamsIdx++).toString(8);
            break;
          case 'x':
            text += this.getParam(sint64Params, sint64ParamsIdx++).toString(16);
            break;
          case 'f':
            text += this.getParam(doubleParams, doubleParamsIdx++).toFixed(6);
            break;
          case 'e':
            text += this.getParam(doubleParams, doubleParamsIdx++).toExponential();
            break;
          case 'g':
            text += this.getParam(doubleParams, doubleParamsIdx++).toString();
            break;
          case 's':
            text += this.getParam(strParams, strParamsIdx++);
            break;
          case 'b':
            text += this.getParam(booleanParams, booleanParamsIdx++).toString();
            break;
          default:
            // Should never happen - protologtool checks for that
            throw new Error('Invalid format string conversion: ' + messageFormat[i + 1]);
        }
        i += 2;
      } else {
        text += messageFormat[i];
        i += 1;
      }
    }

    return {
      text,
      tag: (configJson as ProtologConfig).groups[message.group].tag,
      level: message.level,
      at: message.at,
      timestamp: BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    };
  }

  private getParam<T>(arr: T[], idx: number): T {
    if (arr.length <= idx) {
      throw new Error('No param for format string conversion');
    }
    return arr[idx];
  }

  private makeLogMessageWithoutFormat(
    entry: com.android.internal.protolog.IProtoLogMessage
  ): LogMessage {
    const text =
      assertDefined(entry.messageHash).toString() +
      ' - [' +
      assertDefined(entry.strParams).toString() +
      '] [' +
      assertDefined(entry.sint64Params).toString() +
      '] [' +
      assertDefined(entry.doubleParams).toString() +
      '] [' +
      assertDefined(entry.booleanParams).toString() +
      ']';

    return {
      text,
      tag: 'INVALID',
      level: 'invalid',
      at: '',
      timestamp: BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    };
  }
}

class FormatStringMismatchError extends Error {
  constructor(message: string) {
    super(message);
  }
}

interface ProtologConfig {
  messages: {[key: number]: ConfigMessage};
  groups: {[key: string]: {tag: string}};
}

interface ConfigMessage {
  message: string;
  level: string;
  group: string;
  at: string;
}

export {ParserProtoLog};

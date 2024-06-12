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
import {Timestamp} from 'common/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {LogMessage} from 'parsers/protolog/log_message';
import {ParserProtologUtils} from 'parsers/protolog/parser_protolog_utils';
import root from 'protos/protolog/udc/json';
import {com} from 'protos/protolog/udc/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import configJson32 from '../../../../configs/services.core.protolog32.json';
import configJson64 from '../../../../configs/services.core.protolog64.json';

class ParserProtoLog extends AbstractParser {
  private static readonly ProtoLogFileProto = root.lookupType(
    'com.android.internal.protolog.ProtoLogFileProto',
  );
  private static readonly MAGIC_NUMBER = [
    0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47,
  ]; // .PROTOLOG
  private static readonly PROTOLOG_32_BIT_VERSION = '1.0.0';
  private static readonly PROTOLOG_64_BIT_VERSION = '2.0.0';

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.PROTO_LOG;
  }

  override getMagicNumber(): number[] {
    return ParserProtoLog.MAGIC_NUMBER;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): com.android.internal.protolog.IProtoLogMessage[] {
    const fileProto = ParserProtoLog.ProtoLogFileProto.decode(
      buffer,
    ) as com.android.internal.protolog.IProtoLogFileProto;

    if (fileProto.version === ParserProtoLog.PROTOLOG_32_BIT_VERSION) {
      if (configJson32.version !== ParserProtoLog.PROTOLOG_32_BIT_VERSION) {
        const message = `Unsupported ProtoLog JSON config version ${configJson32.version} expected ${ParserProtoLog.PROTOLOG_32_BIT_VERSION}`;
        console.log(message);
        throw new TypeError(message);
      }
    } else if (fileProto.version === ParserProtoLog.PROTOLOG_64_BIT_VERSION) {
      if (configJson64.version !== ParserProtoLog.PROTOLOG_64_BIT_VERSION) {
        const message = `Unsupported ProtoLog JSON config version ${configJson64.version} expected ${ParserProtoLog.PROTOLOG_64_BIT_VERSION}`;
        console.log(message);
        throw new TypeError(message);
      }
    } else {
      const message = 'Unsupported ProtoLog trace version';
      console.log(message);
      throw new TypeError(message);
    }

    this.realToBootTimeOffsetNs =
      BigInt(
        assertDefined(fileProto.realTimeToElapsedTimeOffsetMillis).toString(),
      ) * 1000000n;

    if (!fileProto.log) {
      return [];
    }

    fileProto.log.sort(
      (
        a: com.android.internal.protolog.IProtoLogMessage,
        b: com.android.internal.protolog.IProtoLogMessage,
      ) => {
        return Number(a.elapsedRealtimeNanos) - Number(b.elapsedRealtimeNanos);
      },
    );

    return fileProto.log;
  }

  protected override getTimestamp(
    entry: com.android.internal.protolog.IProtoLogMessage,
  ): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  override processDecodedEntry(
    index: number,
    entry: com.android.internal.protolog.IProtoLogMessage,
  ): PropertyTreeNode {
    let messageHash = assertDefined(entry.messageHash).toString();
    let config: ProtologConfig | undefined = undefined;
    if (messageHash !== null && messageHash !== '0') {
      config = assertDefined(configJson64) as ProtologConfig;
    } else {
      messageHash = assertDefined(entry.messageHashLegacy).toString();
      config = assertDefined(configJson32) as ProtologConfig;
    }

    const message: ConfigMessage | undefined = config.messages[messageHash];
    const tag: string | undefined = message
      ? config.groups[message.group].tag
      : undefined;

    const logMessage = this.makeLogMessage(entry, message, tag);
    return ParserProtologUtils.makeMessagePropertiesTree(
      logMessage,
      this.timestampConverter,
      this.getRealToMonotonicTimeOffsetNs() !== undefined,
    );
  }

  private makeLogMessage(
    entry: com.android.internal.protolog.IProtoLogMessage,
    message: ConfigMessage | undefined,
    tag: string | undefined,
  ): LogMessage {
    if (!message || !tag) {
      return this.makeLogMessageWithoutFormat(entry);
    }
    try {
      return this.makeLogMessageWithFormat(entry, message, tag);
    } catch (error) {
      if (error instanceof FormatStringMismatchError) {
        return this.makeLogMessageWithoutFormat(entry);
      }
      throw error;
    }
  }

  private makeLogMessageWithFormat(
    entry: com.android.internal.protolog.IProtoLogMessage,
    message: ConfigMessage,
    tag: string,
  ): LogMessage {
    let text = '';

    const strParams: string[] = assertDefined(entry.strParams);
    let strParamsIdx = 0;
    const sint64Params: Array<bigint> = assertDefined(entry.sint64Params).map(
      (param) => BigInt(param.toString()),
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
            text += this.getParam(
              doubleParams,
              doubleParamsIdx++,
            ).toExponential();
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
            throw new Error(
              'Invalid format string conversion: ' + messageFormat[i + 1],
            );
        }
        i += 2;
      } else {
        text += messageFormat[i];
        i += 1;
      }
    }

    return {
      text,
      tag,
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
    entry: com.android.internal.protolog.IProtoLogMessage,
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
  version: string;
  messages: {[key: string]: ConfigMessage};
  groups: {[key: string]: {tag: string}};
}

interface ConfigMessage {
  message: string;
  level: string;
  group: string;
  at: string;
}

export {ParserProtoLog};

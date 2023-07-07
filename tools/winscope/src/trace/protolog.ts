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

import {TimeUtils} from 'common/time_utils';
import configJson from '../../../../../frameworks/base/data/etc/services.core.protolog.json';
import {ElapsedTimestamp, RealTimestamp, TimestampType} from './timestamp';

class LogMessage {
  text: string;
  time: string;
  tag: string;
  level: string;
  at: string;
  timestamp: bigint;

  constructor(
    text: string,
    time: string,
    tag: string,
    level: string,
    at: string,
    timestamp: bigint
  ) {
    this.text = text;
    this.time = time;
    this.tag = tag;
    this.level = level;
    this.at = at;
    this.timestamp = timestamp;
  }
}

class FormattedLogMessage extends LogMessage {
  constructor(
    proto: any,
    timestampType: TimestampType,
    realToElapsedTimeOffsetNs: bigint | undefined
  ) {
    const text =
      proto.messageHash.toString() +
      ' - [' +
      proto.strParams.toString() +
      '] [' +
      proto.sint64Params.toString() +
      '] [' +
      proto.doubleParams.toString() +
      '] [' +
      proto.booleanParams.toString() +
      ']';

    let time: string;
    let timestamp: bigint;
    if (
      timestampType === TimestampType.REAL &&
      realToElapsedTimeOffsetNs !== undefined &&
      realToElapsedTimeOffsetNs !== 0n
    ) {
      timestamp = realToElapsedTimeOffsetNs + BigInt(proto.elapsedRealtimeNanos);
      time = TimeUtils.format(new RealTimestamp(timestamp));
    } else {
      timestamp = BigInt(proto.elapsedRealtimeNanos);
      time = TimeUtils.format(new ElapsedTimestamp(timestamp));
    }

    super(text, time, 'INVALID', 'invalid', '', timestamp);
  }
}

class UnformattedLogMessage extends LogMessage {
  constructor(
    proto: any,
    timestampType: TimestampType,
    realToElapsedTimeOffsetNs: bigint | undefined,
    message: any
  ) {
    let time: string;
    let timestamp: bigint;
    if (
      timestampType === TimestampType.REAL &&
      realToElapsedTimeOffsetNs !== undefined &&
      realToElapsedTimeOffsetNs !== 0n
    ) {
      timestamp = realToElapsedTimeOffsetNs + BigInt(proto.elapsedRealtimeNanos);
      time = TimeUtils.format(
        new RealTimestamp(realToElapsedTimeOffsetNs + BigInt(proto.elapsedRealtimeNanos))
      );
    } else {
      timestamp = BigInt(proto.elapsedRealtimeNanos);
      time = TimeUtils.format(new ElapsedTimestamp(timestamp));
    }

    super(
      formatText(message.message, proto),
      time,
      (configJson as any).groups[message.group].tag,
      message.level,
      message.at,
      timestamp
    );
  }
}

function formatText(messageFormat: any, data: any) {
  let out = '';

  const strParams: string[] = data.strParams;
  let strParamsIdx = 0;
  const sint64Params: number[] = data.sint64Params;
  let sint64ParamsIdx = 0;
  const doubleParams: number[] = data.doubleParams;
  let doubleParamsIdx = 0;
  const booleanParams: number[] = data.booleanParams;
  let booleanParamsIdx = 0;

  for (let i = 0; i < messageFormat.length; ) {
    if (messageFormat[i] === '%') {
      if (i + 1 >= messageFormat.length) {
        // Should never happen - protologtool checks for that
        throw new Error('Invalid format string');
      }
      switch (messageFormat[i + 1]) {
        case '%':
          out += '%';
          break;
        case 'd':
          out += getParam(sint64Params, sint64ParamsIdx++).toString(10);
          break;
        case 'o':
          out += getParam(sint64Params, sint64ParamsIdx++).toString(8);
          break;
        case 'x':
          out += getParam(sint64Params, sint64ParamsIdx++).toString(16);
          break;
        case 'f':
          out += getParam(doubleParams, doubleParamsIdx++).toFixed(6);
          break;
        case 'e':
          out += getParam(doubleParams, doubleParamsIdx++).toExponential();
          break;
        case 'g':
          out += getParam(doubleParams, doubleParamsIdx++).toString();
          break;
        case 's':
          out += getParam(strParams, strParamsIdx++);
          break;
        case 'b':
          out += getParam(booleanParams, booleanParamsIdx++).toString();
          break;
        default:
          // Should never happen - protologtool checks for that
          throw new Error('Invalid format string conversion: ' + messageFormat[i + 1]);
      }
      i += 2;
    } else {
      out += messageFormat[i];
      i += 1;
    }
  }
  return out;
}

function getParam<T>(arr: T[], idx: number): T {
  if (arr.length <= idx) {
    throw new Error('No param for format string conversion');
  }
  return arr[idx];
}

export {FormattedLogMessage, LogMessage, UnformattedLogMessage};

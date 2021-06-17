/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { FILE_TYPES, TRACE_TYPES } from '@/decode.js';
import TraceBase from './TraceBase';

import { nanos_to_string } from '@/transform.js';
import viewerConfig from
  '@/../../../../frameworks/base/data/etc/services.core.protolog.json';

export default class ProtoLog extends TraceBase {
  protoLogFile: any;

  constructor(files) {
    const protoLogFile = files[FILE_TYPES.PROTO_LOG];
    super(protoLogFile.data, protoLogFile.timeline, files);

    this.protoLogFile = protoLogFile;
  }

  get type() {
    return TRACE_TYPES.PROTO_LOG;
  }
}

export class LogMessage {
  text: String;
  time: String;
  tag: String;
  level: String;
  at: String;
  timestamp: Number;

  constructor({ text, time, tag, level, at, timestamp }) {
    this.text = text;
    this.time = time;
    this.tag = tag;
    this.level = level;
    this.at = at;
    this.timestamp = timestamp;
  }
}

export class FormattedLogMessage extends LogMessage {
  constructor(entry) {
    super({
      text: (entry.messageHash.toString() +
        ' - [' + entry.strParams.toString() +
        '] [' + entry.sint64Params.toString() +
        '] [' + entry.doubleParams.toString() +
        '] [' + entry.booleanParams.toString() + ']'),
      time: nanos_to_string(entry.elapsedRealtimeNanos),
      tag: 'INVALID',
      level: 'invalid',
      at: '',
      timestamp: entry.elapsedRealtimeNanos,
    });
  }
}

export class UnformattedLogMessage extends LogMessage {
  constructor(entry, message) {
    super({
      text: formatText(message.message, entry),
      time: nanos_to_string(entry.elapsedRealtimeNanos),
      tag: viewerConfig.groups[message.group].tag,
      level: message.level,
      at: message.at,
      timestamp: entry.elapsedRealtimeNanos,
    });
  }
}

function formatText(messageFormat, data) {
  let out = '';
  const strParams = data.strParams;
  let strParamsIdx = 0;
  const sint64Params = data.sint64Params;
  let sint64ParamsIdx = 0;
  const doubleParams = data.doubleParams;
  let doubleParamsIdx = 0;
  const booleanParams = data.booleanParams;
  let booleanParamsIdx = 0;
  for (let i = 0; i < messageFormat.length;) {
    if (messageFormat[i] == '%') {
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
          throw new Error('Invalid format string conversion: ' +
            messageFormat[i + 1]);
      }
      i += 2;
    } else {
      out += messageFormat[i];
      i += 1;
    }
  }
  return out;
}

function getParam(arr, idx) {
  if (arr.length <= idx) {
    throw new Error('No param for format string conversion');
  }
  return arr[idx];
}
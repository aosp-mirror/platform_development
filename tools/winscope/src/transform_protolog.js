import viewerConfig from "../../../../frameworks/base/data/etc/services.core.protolog.json"

import { nanos_to_string } from './transform.js'

const PROTOLOG_VERSION = "1.0.0"

class FormatStringMismatchError extends Error {
  constructor(message) {
    super(message);
  }
}

function get_param(arr, idx) {
  if (arr.length <= idx) {
    throw new FormatStringMismatchError('No param for format string conversion');
  }
  return arr[idx];
}

function format_text(messageFormat, data) {
  let out = ""
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
        throw new Error("Invalid format string")
      }
      switch (messageFormat[i + 1]) {
        case '%':
          out += '%';
          break;
        case 'd':
          out += get_param(sint64Params, sint64ParamsIdx++).toString(10);
          break;
        case 'o':
          out += get_param(sint64Params, sint64ParamsIdx++).toString(8);
          break;
        case 'x':
          out += get_param(sint64Params, sint64ParamsIdx++).toString(16);
          break;
        case 'f':
          out += get_param(doubleParams, doubleParamsIdx++).toFixed(6);
          break;
        case 'e':
          out += get_param(doubleParams, doubleParamsIdx++).toExponential();
          break;
        case 'g':
          out += get_param(doubleParams, doubleParamsIdx++).toString();
          break;
        case 's':
          out += get_param(strParams, strParamsIdx++);
          break;
        case 'b':
          out += get_param(booleanParams, booleanParamsIdx++).toString();
          break;
        default:
          // Should never happen - protologtool checks for that
          throw new Error("Invalid format string conversion: " + messageFormat[i + 1]);
      }
      i += 2;
    } else {
      out += messageFormat[i];
      i += 1;
    }
  }
  return out;
}

function transform_unformatted(entry) {
  return {
    text: (entry.messageHash.toString() + ' - [' + entry.strParams.toString() +
      '] [' + entry.sint64Params.toString() + '] [' + entry.doubleParams.toString() +
      '] [' + entry.booleanParams.toString() + ']'),
    time: nanos_to_string(entry.elapsedRealtimeNanos),
    tag: "INVALID",
    at: "",
    timestamp: entry.elapsedRealtimeNanos,
  };
}

function transform_formatted(entry, message) {
  return {
    text: format_text(message.message, entry),
    time: nanos_to_string(entry.elapsedRealtimeNanos),
    tag: viewerConfig.groups[message.group].tag,
    at: message.at,
    timestamp: entry.elapsedRealtimeNanos,
  };
}

function transform_message(entry) {
  let message = viewerConfig.messages[entry.messageHash]
  if (message === undefined) {
    return transform_unformatted(entry);
  } else {
    try {
      return transform_formatted(entry, message);
    } catch (err) {
      if (err instanceof FormatStringMismatchError) {
        return transform_unformatted(entry);
      }
      throw err;
    }
  }
}

function transform_protolog(log) {
  if (log.version !== PROTOLOG_VERSION) {
    throw new Error('Unsupported log version');
  }
  if (viewerConfig.version !== PROTOLOG_VERSION) {
    throw new Error('Unsupported viewer config version');
  }

  let data = log.log.map(entry => (transform_message(entry)))
  data.sort(function(a, b) { return a.timestamp - b.timestamp })
  let transformed = {
    children: data
  }
  return transformed
}

export { transform_protolog };

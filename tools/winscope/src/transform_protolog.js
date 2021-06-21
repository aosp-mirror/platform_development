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

import viewerConfig
  from '../../../../frameworks/base/data/etc/services.core.protolog.json';

import {FormattedLogMessage, UnformattedLogMessage} from '@/traces/ProtoLog.ts';

const PROTOLOG_VERSION = '1.0.0';

class FormatStringMismatchError extends Error {
  constructor(message) {
    super(message);
  }
}

function transformMessage(entry) {
  const message = viewerConfig.messages[entry.messageHash];
  if (message === undefined) {
    return new FormattedLogMessage(entry);
  } else {
    try {
      return new UnformattedLogMessage(entry, message);
    } catch (err) {
      if (err instanceof FormatStringMismatchError) {
        return new FormattedLogMessage(entry);
      }
      throw err;
    }
  }
}

function transformProtolog(log) {
  if (log.version !== PROTOLOG_VERSION) {
    throw new Error('Unsupported log version');
  }
  if (viewerConfig.version !== PROTOLOG_VERSION) {
    throw new Error('Unsupported viewer config version');
  }

  const data = log.log.map((entry) => (transformMessage(entry)));
  data.sort(function(a, b) {
    return a.timestamp - b.timestamp;
  });
  const transformed = {
    children: data,
  };
  return transformed;
}

export {transformProtolog};

/*
 * Copyright 2019, The Android Open Source Project
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

import {nanos_to_string} from './transform.js';

function transform_transaction(transaction, layerIdToName) {
  const transactions = [];

  for (const surfaceChange of transaction.surfaceChange) {
    transactions.push(Object.freeze({
      type: 'surfaceChange',
      obj: surfaceChange,
      layerName: layerIdToName[surfaceChange.id],
    }));
  }

  for (const displayChange of transaction.displayChange) {
    transactions.push(Object.freeze({
      type: 'displayChange',
      obj: displayChange,
      layerName: layerIdToName[displayChange.id],
    }));
  }

  return transactions;
}

function transform_entry(entry, layerIdToName) {
  const type = entry.increment;
  const timestamp = entry.timeStamp;
  const time = nanos_to_string(timestamp);

  switch (type) {
    case 'transaction':

      return Object.freeze({
        type,
        // TODO: Rename to changes
        transactions: transform_transaction(entry.transaction, layerIdToName),
        synchronous: entry.transaction.synchronous,
        animation: entry.transaction.animation,
        identifier: entry.transaction.id,
        time,
        origin: entry.transaction.origin,
        timestamp,
      });

    case 'surfaceCreation':
      // NOTE: There is no break on purpose — we want to fall through to default
      layerIdToName[entry[type].id] = entry[type].name;

    default:
      return Object.freeze({
        type,
        obj: entry[type],
        layerName: entry[type].name ?? layerIdToName[entry[type].id],
        time,
        timestamp,
      });
  }
}

/**
 * @deprecated This trace has been replaced by the new transactions trace
 */
function transform_transaction_trace_legacy(entries) {
  const layerIdToName = {};
  const data = entries.increment.map((entry) => transform_entry(entry, layerIdToName));

  return {children: data};
}

export {transform_transaction_trace_legacy};

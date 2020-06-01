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

import { transform, nanos_to_string } from './transform.js'

function transform_transaction(transaction) {
  const transactions = [];

  for (const surfaceChange of transaction.surfaceChange) {
    transactions.push(Object.freeze({
      type: 'surfaceChange',
      obj: surfaceChange,
    }));
  }
  for (const displayChange of transaction.displayChange) {
    transactions.push(Object.freeze({
      type: 'displayChange',
      obj: displayChange,
    }));
  }

  return transactions;
}

function transform_entry(entry) {
  const type = entry.increment;
  const timestamp = entry.timeStamp;
  const time = nanos_to_string(timestamp);

  switch (type) {
    case "transaction":
      return Object.freeze({
        type,
        transactions: transform_transaction(entry.transaction),
        time,
        timestamp,
      });

    default:
      return Object.freeze({
        type,
        obj: entry[type],
        time,
        timestamp,
      })
  }
}

function transform_transaction_trace(entries) {
  const data = entries.increment.map(transform_entry);

  return { children: data };
}

export { transform_transaction_trace };

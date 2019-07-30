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

import {transform, nanos_to_string} from './transform.js'

function transform_transaction(transaction) {
  return transform({
    obj: transaction,
    kind: 'transaction',
    children:[[transaction.surfaceChange, transform_entry_type('surfaceChange')],
        [transaction.displayChange, transform_entry_type('displayChange')]],
    rects: [],
    visible: false,
  })
}

function transform_entry_type(transactionType) {
  function return_transform(item) {
    return Object.freeze({
      obj: item,
      kind: transactionType,
      rects: [],
      visible: false,
      name: item.name || item.id || nanos_to_string(item.when),
    });
  }
  return transactionType === 'transaction' ? transform_transaction : return_transform;
}

function transform_entry(entry) {
  var transactionType = entry.increment;
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.timeStamp),
    children: [[[entry[transactionType]], transform_entry_type(transactionType)]],
    timestamp: entry.timeStamp,
  });
}

function transform_transaction_trace(entries) {
  var r = transform({
    obj: entries,
    kind: 'entries',
    name: 'transactionstrace',
    children: [
      [entries.increment, transform_entry],
    ],
  })
  return r;
}

export {transform_transaction_trace};

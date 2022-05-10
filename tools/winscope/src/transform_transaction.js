/*
 * Copyright 2021, The Android Open Source Project
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

import {nanos_to_string, transform} from './transform.js'

function transform_change(change_data) {
  const kind = change_data.__proto__.$type.name;
  const name = change_data.layerId || change_data.id;
  return transform({
    kind: kind,
    name: name,
    stableId: kind + name,
    obj: change_data,
    children: [],
    isVisible: true,
  });
}

function transform_transaction_state(transaction_state) {
  const obj = Object.assign({}, transaction_state)
  if (obj.displayChanges) delete obj.displayChanges;
  if (obj.layerChanges) delete obj.layerChanges;
  const stableId = 'pid=' + transaction_state.pid +
  ' uid=' + transaction_state.uid +
  ' postTime=' + transaction_state.postTime;
  return transform({
    kind: 'TransactionState',
    name: stableId,
    stableId: stableId,
    obj: obj,
    children: [
      [
        [...transaction_state.layerChanges, ...transaction_state.displayChanges],
        transform_change]
      ]
  });
}

function transform_transaction_trace_entry(entry) {
  const obj = Object.assign({}, entry)
  if (obj.transactions) delete obj.transactions;

  return transform({
    obj: obj,
    kind: 'entry',
    stableId: 'entry',
    timestamp: entry.elapsedRealtimeNanos,
    name: nanos_to_string(entry.elapsedRealtimeNanos),
    children: [
      [entry.transactions, transform_transaction_state]
    ],
  });
}

function transform_transaction_trace(trace) {
  const data = trace.entry.map((entry) => transform_transaction_trace_entry(entry));
  return {children: data};
}

export {transform_transaction_trace};

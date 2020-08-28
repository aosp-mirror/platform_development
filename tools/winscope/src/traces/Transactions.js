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

import { FILE_TYPES, TRACE_TYPES } from "@/decode.js";
import TraceBase from './TraceBase.js';

export default class Transactions extends TraceBase {
  constructor(files) {
    const transactionsFile = files[FILE_TYPES.TRANSACTIONS_TRACE];

    // There should be one file for each process which recorded transaction events
    const transactionsEventsFiles = files[FILE_TYPES.TRANSACTION_EVENTS_TRACE];

    super(transactionsFile.data, transactionsFile.timeline);

    const transactions = transactionsFile.data
      .filter(e => e.type === "transaction")
      .map(e => e.transactions)
      .flat();

    this.transactionsFile = transactionsFile;
    this.transactionsEventsFiles = transactionsEventsFiles;

    this.transactionHistory = new TransactionHistory(transactionsEventsFiles);
  }

  get type() {
    return TRACE_TYPES.TRANSACTION;
  }
}

class TransactionHistory {
  constructor(transactionsEventsFiles) {
    this.history = {};
    this.applied = {};

    if (!transactionsEventsFiles) {
      return;
    }

    for (const eventsFile of transactionsEventsFiles) {
      for (const event of eventsFile.data) {
        if (event.merge) {
          const merge = event.merge;
          const originalId = merge.originalTransaction.identifier.id;
          const mergedId = merge.mergedTransaction.identifier.id;

          this.addMerge(originalId, mergedId);
        } else if (event.apply) {
          this.addApply(event.apply.identifier.id);
        }
      }
    }
  }

  addMerge(originalId, mergedId) {
    const merge = new Merge(originalId, mergedId, this.history);
    this.addToHistoryOf(originalId, merge);
  }

  addApply(transactionId) {
    this.applied[transactionId] = true;
    this.addToHistoryOf(transactionId, new Apply(transactionId));
  }

  addToHistoryOf(transactionId, event) {
    if (!this.history[transactionId]) {
      this.history[transactionId] = [];
    }
    this.history[transactionId].push(event);
  }

  generateHistoryTreesOf(transactionId) {
    return this._generateHistoryTree(transactionId);
  }

  _generateHistoryTree(transactionId, upTo) {
    if (!this.history[transactionId]) {
      return [];
    }

    const children = [];
    const events = this.history[transactionId];
    for (let i = 0; i < (upTo ?? events.length); i++) {
      const event = events[i];

      if (event instanceof Merge) {
        const historyTree = this._generateHistoryTree(event.mergedId, event.mergedAt);
        const mergeTreeNode = new MergeTreeNode(event.mergedId, historyTree);
        children.push(mergeTreeNode);
      } else if (event instanceof Apply) {
        children.push(new ApplyTreeNode());
      } else {
        throw new Error("Unhandled event type");
      }
    }

    return children;
  }

  getMergeTreeOf(transactionId) {
    return this.mergeTrees[transactionId];
  }
}

class MergeTreeNode {
  constructor(mergedId, mergedTransactionHistory) {
    this.mergedId = mergedId;
    this.mergedTransactionHistory = mergedTransactionHistory;
    this.children = mergedTransactionHistory;
  }

  get type() {
    return "merge";
  }
}

class ApplyTreeNode {
  constructor() {
    this.children = [];
  }

  get type() {
    return "apply";
  }
}

class Merge {
  constructor(originalId, mergedId, history) {
    this.originalId = originalId;
    this.mergedId = mergedId;
    // Specifies how long the merge chain of the merged transaction was at the time is was merged.
    this.mergedAt = history[mergedId]?.length ?? 0;
  }
}

class Apply {
  constructor(transactionId) {
    this.transactionId = transactionId;
  }
}
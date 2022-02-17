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

/**
 * @deprecated This trace has been replaced by the new transactions trace
 */
export default class TransactionsTraceLegacy extends TraceBase {
  transactionsFile: Object;
  transactionHistory: TransactionHistory;

  constructor(files: any[]) {
    const transactionsFile = files[FILE_TYPES.TRANSACTIONS_TRACE_LEGACY];

    super(transactionsFile.data, transactionsFile.timeline, files);

    this.transactionsFile = transactionsFile;

    // Create new transaction history
    this.transactionHistory = new TransactionHistory(transactionsFile);
  }

  get type() {
    return TRACE_TYPES.TRANSACTION_LEGACY;
  }
}

class TransactionHistory {
  history: Object;
  applied: Object;
  mergeTrees: any;

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
          const originalId = merge.originalTransaction.id;
          const mergedId = merge.mergedTransaction.id;

          this.addMerge(originalId, mergedId);
        } else if (event.apply) {
          this.addApply(event.apply.tx_id);
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

  _generateHistoryTree(transactionId, upTo = null) {
    if (!this.history[transactionId]) {
      return [];
    }

    const children = [];
    const events = this.history[transactionId];
    for (let i = 0; i < (upTo ?? events.length); i++) {
      const event = events[i];

      if (event instanceof Merge) {
        const historyTree = this.
          _generateHistoryTree(event.mergedId, event.mergedAt);
        const mergeTreeNode = new MergeTreeNode(event.mergedId, historyTree);
        children.push(mergeTreeNode);
      } else if (event instanceof Apply) {
        children.push(new ApplyTreeNode());
      } else {
        throw new Error('Unhandled event type');
      }
    }

    return children;
  }

  /**
   * Generates the list of all the transactions that have ever been merged into
   * the target transaction directly or indirectly through the merges of
   * transactions that ended up being merged into the transaction.
   * This includes both merges that occur before and after the transaction is
   * applied.
   * @param {Number} transactionId - The id of the transaction we want the list
   *                                 of transactions merged in for
   * @return {Set<Number>} a set of all the transaction ids that are in the
   *                       history of merges of the transaction
   */
  allTransactionsMergedInto(transactionId) {
    const allTransactionsMergedIn = new Set();

    let event;
    const toVisit = this.generateHistoryTreesOf(transactionId);
    while (event = toVisit.pop()) {
      if (event instanceof MergeTreeNode) {
        allTransactionsMergedIn.add(event.mergedId);
        for (const child of event.children) {
          toVisit.push(child);
        }
      }
    }

    return allTransactionsMergedIn;
  }

  /**
   * Generated the list of transactions that have been directly merged into the
   * target transaction those are transactions that have explicitly been merged
   * in the code with a call to merge.
   * @param {Number} transactionId - The id of the target transaction.
   * @return {Array<Number>} an array of the transaction ids of the transactions
   *                        directly merged into the target transaction
   */
  allDirectMergesInto(transactionId) {
    return (this.history[transactionId] ?? [])
      .filter((event) => event instanceof Merge)
      .map((merge) => merge.mergedId);
  }
}

class MergeTreeNode {
  mergedId: Number;
  mergedTransactionHistory: TransactionHistory;
  children: TransactionHistory[];

  constructor(mergedId, mergedTransactionHistory) {
    this.mergedId = mergedId;
    this.mergedTransactionHistory = mergedTransactionHistory;
    this.children = mergedTransactionHistory;
  }

  get type() {
    return 'merge';
  }
}

class ApplyTreeNode {
  children: any[];

  constructor() {
    this.children = [];
  }

  get type() {
    return 'apply';
  }
}

class Merge {
  originalId: Number;
  mergedId: Number;
  mergedAt: Number;

  constructor(originalId, mergedId, history) {
    this.originalId = originalId;
    this.mergedId = mergedId;
    // Specifies how long the merge chain of the merged transaction was at the
    // time is was merged.
    this.mergedAt = history[mergedId]?.length ?? 0;
  }
}

class Apply {
  transactionId: Number;

  constructor(transactionId) {
    this.transactionId = transactionId;
  }
}

/**
 * Converts the transactionId to the values that compose the identifier.
 * The top 32 bits is the PID of the process that created the transaction
 * and the bottom 32 bits is the ID of the transaction unique within that
 * process.
 * @param {Number} transactionId
 * @return {Object} An object containing the id and pid of the transaction.
 */
export function expandTransactionId(transactionId) {
  // Can't use bit shift operation because it isn't a 32 bit integer...
  // Because js uses floating point numbers for everything, maths isn't 100%
  // accurate so we need to round...
  return Object.freeze({
    id: Math.round(transactionId % Math.pow(2, 32)),
    pid: Math.round(transactionId / Math.pow(2, 32)),
  });
}

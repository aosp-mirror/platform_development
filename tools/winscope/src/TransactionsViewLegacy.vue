<!-- Copyright (C) 2020 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<template>
  <md-card-content class="container">

    <flat-card class="changes card">
      <md-content
        md-tag="md-toolbar"
        md-elevation="0"
        class="card-toolbar md-transparent md-dense"
      >
        <h2 class="md-title" style="flex: 1">Transactions</h2>
      </md-content>
      <div class="filters">
        <div class="input">
          <md-field>
            <label>Transaction Type</label>
            <md-select v-model="selectedTransactionTypes" multiple>
              <md-option
                v-for="type in transactionTypes"
                :value="type"
                v-bind:key="type">
                {{ type }}
              </md-option>
            </md-select>
          </md-field>
        </div>

        <div class="input">
          <div>
          <md-field>
            <label>Changed property</label>
            <md-select v-model="selectedProperties" multiple>
              <md-option
                v-for="property in properties"
                :value="property"
                v-bind:key="property">
                {{ property }}
              </md-option>
            </md-select>
          </md-field>
          </div>
        </div>

        <div class="input">
          <md-field>
            <label>Origin PID</label>
            <md-select v-model="selectedPids" multiple>
              <md-option v-for="pid in pids" :value="pid" v-bind:key="pid">
                {{ pid }}
              </md-option>
            </md-select>
          </md-field>
        </div>

        <div class="input">
          <md-field>
            <label>Origin UID</label>
            <md-select v-model="selectedUids" multiple>
              <md-option v-for="uid in uids" :value="uid" v-bind:key="uid">
                {{ uid }}
              </md-option>
            </md-select>
          </md-field>
        </div>

        <div class="input">
          <md-chips
            v-model="filters"
            md-placeholder="Add surface id or name..."
          >
            <div class="md-helper-text">Press enter to add</div>
          </md-chips>
        </div>

        <md-checkbox v-model="trace.simplifyNames">
            Simplify names
        </md-checkbox>

      </div>

      <virtual-list style="height: 600px; overflow-y: auto;"
        :data-key="'timestamp'"
        :data-sources="filteredData"
        :data-component="transactionEntryComponent"
        :extra-props="{
          onClick: transactionSelected,
          selectedTransaction,
          transactionsTrace,
          prettifyTransactionId,
          simplifyNames: trace.simplifyNames,
        }"
        ref="loglist"
      />
    </flat-card>

    <flat-card class="changes card">
      <md-content
        md-tag="md-toolbar"
        md-elevation="0"
        class="card-toolbar md-transparent md-dense"
      >
        <h2 class="md-title" style="flex: 1">Changes</h2>
      </md-content>
      <div class="changes-content" v-if="selectedTree">
        <div
          v-if="selectedTransaction.type === 'transaction'"
          class="transaction-events"
        >
          <div
            v-for="(event, i) in transactionHistory(selectedTransaction)"
            v-bind:key="`${selectedTransaction.identifier}-${i}`"
            class="transaction-event"
          >
            <div v-if="event.type === 'apply'" class="applied-event">
              applied
            </div>
            <div v-if="event.type === 'merge'" class="merged-event">
              <!-- eslint-disable-next-line max-len -->
              {{ prettifyTransactionId(event.mergedId) }}
            </div>
          </div>
        </div>
        <tree-view
          :item="selectedTree"
          :collapseChildren="true"
          :useGlobalCollapsedState="true"
        />
      </div>
      <div class="no-properties" v-else>
        <i class="material-icons none-icon">
          filter_none
        </i>
        <span>No transaction selected.</span>
      </div>
    </flat-card>

  </md-card-content>
</template>
<script>
import TreeView from './TreeView.vue';
import VirtualList from '../libs/virtualList/VirtualList';
import TransactionEntryLegacy from './TransactionEntryLegacy.vue';
import FlatCard from './components/FlatCard.vue';

import {ObjectTransformer} from './transform.js';
import {expandTransactionId} from '@/traces/TransactionsLegacy.ts';

/**
 * @deprecated This trace has been replaced by the new transactions trace
 */
export default {
  name: 'transactionsviewlegacy',
  props: ['trace'],
  data() {
    const transactionTypes = new Set();
    const properties = new Set();
    const pids = new Set();
    const uids = new Set();
    const transactionsTrace = this.trace;
    for (const entry of transactionsTrace.data) {
      if (entry.type == 'transaction') {
        for (const transaction of entry.transactions) {
          transactionTypes.add(transaction.type);
          Object.keys(transaction.obj).forEach((item) => properties.add(item));
        }
      } else {
        transactionTypes.add(entry.type);
        Object.keys(entry.obj).forEach((item) => properties.add(item));
      }

      if (entry.origin) {
        pids.add(entry.origin.pid);
        uids.add(entry.origin.uid);
      }
    }

    // Remove vsync from being transaction types that can be filtered
    // We want to always show vsyncs
    transactionTypes.delete('vsyncEvent');

    return {
      transactionTypes: Array.from(transactionTypes),
      properties: Array.from(properties),
      pids: Array.from(pids),
      uids: Array.from(uids),
      selectedTransactionTypes: [],
      selectedPids: [],
      selectedUids: [],
      searchInput: '',
      selectedTree: null,
      filters: [],
      selectedProperties: [],
      selectedTransaction: null,
      transactionEntryComponent: TransactionEntryLegacy,
      transactionsTrace,
      expandTransactionId,
    };
  },
  computed: {
    data() {
      // Record analytics event
      this.recordOpenTraceEvent("TransactionsTrace");
      return this.transactionsTrace.data;
    },
    filteredData() {
      let filteredData = this.data;

      if (this.selectedTransactionTypes.length > 0) {
        filteredData = filteredData.filter(
            this.filterTransactions((transaction) =>
              transaction.type === 'vsyncEvent' ||
              this.selectedTransactionTypes.includes(transaction.type)));
      }

      if (this.selectedPids.length > 0) {
        filteredData = filteredData.filter((entry) =>
          this.selectedPids.includes(entry.origin?.pid));
      }

      if (this.selectedUids.length > 0) {
        filteredData = filteredData.filter((entry) =>
          this.selectedUids.includes(entry.origin?.uid));
      }

      if (this.filters.length > 0) {
        filteredData = filteredData.filter(
            this.filterTransactions((transaction) => {
              for (const filter of this.filters) {
                if (isNaN(filter)) {
                  // If filter isn't a number then check if the transaction's
                  // target surface's name matches the filter — if so keep it.
                  const regexFilter = new RegExp(filter, "i");
                  if (regexFilter.test(transaction.layerName)) {
                    return true;
                  }
                }
                if (filter == transaction.obj.id) {
                // If filteter is a number then check if the filter matches
                // the transaction's target surface id — if so keep it.
                  return true;
                }
              }

              // Exclude transaction if it fails to match filter.
              return false;
            }),
        );
      }

      if (this.selectedProperties.length > 0) {
        const regexFilter = new RegExp(this.selectedProperties.join("|"), "i");
        filteredData = filteredData.filter(
            this.filterTransactions((transaction) => {
              for (const key in transaction.obj) {
                if (this.isMeaningfulChange(transaction.obj, key) && regexFilter.test(key)) {
                  return true;
                }
              }

              return false;
            }),
        );
      }

      // We quish vsyncs because otherwise the lazy list will not load enough
      // elements if there are many vsyncs in a row since vsyncs take up no
      // space.
      return this.squishVSyncs(filteredData);
    },

  },
  methods: {
    removeNullFields(changeObject) {
      for (const key in changeObject) {
        if (changeObject[key] === null) {
          delete changeObject[key];
        }
      }

      return changeObject;
    },
    transactionSelected(transaction) {
      this.selectedTransaction = transaction;

      const META_DATA_KEY = 'metadata';

      let obj;
      let name;
      if (transaction.type == 'transaction') {
        name = 'changes';
        obj = {};

        const [surfaceChanges, displayChanges] =
          this.aggregateTransactions(transaction.transactions);

        // Prepare the surface and display changes to be passed through
        // the ObjectTransformer — in particular, remove redundant properties
        // and add metadata that can be accessed post transformation
        const perpareForTreeViewTransform = (change) => {
          this.removeNullFields(change);
          change[META_DATA_KEY] = {
            layerName: change.layerName,
          };
          // remove redundant properties
          delete change.layerName;
          delete change.id;
        };

        for (const changeId in surfaceChanges) {
          if (surfaceChanges.hasOwnProperty(changeId)) {
            perpareForTreeViewTransform(surfaceChanges[changeId]);
          }
        }
        for (const changeId in displayChanges) {
          if (displayChanges.hasOwnProperty(changeId)) {
            perpareForTreeViewTransform(displayChanges[changeId]);
          }
        }

        if (Object.keys(surfaceChanges).length > 0) {
          obj.surfaceChanges = surfaceChanges;
        }

        if (Object.keys(displayChanges).length > 0) {
          obj.displayChanges = displayChanges;
        }
      } else {
        obj = this.removeNullFields(transaction.obj);
        name = transaction.type;
      }

      // Transform the raw JS object to be TreeView compatible
      const transactionUniqueId = transaction.timestamp;
      let tree = new ObjectTransformer(
          obj,
          name,
          transactionUniqueId,
      ).setOptions({
        formatter: () => {},
      }).transform({
        keepOriginal: true,
        metadataKey: META_DATA_KEY,
        freeze: false,
      });

      // Add the layer name as the kind of the object to be shown in the
      // TreeView
      const addLayerNameAsKind = (tree) => {
        for (const layerChanges of tree.children) {
          layerChanges.kind = layerChanges.metadata.layerName;
        }
      };

      if (transaction.type == 'transaction') {
        for (const child of tree.children) {
          // child = surfaceChanges or displayChanges tree node
          addLayerNameAsKind(child);
        }
      }

      // If there are only surfaceChanges or only displayChanges and not both
      // remove the extra top layer node which is meant to hold both types of
      // changes when both are present
      if (tree.name == 'changes' && tree.children.length === 1) {
        tree = tree.children[0];
      }

      this.selectedTree = tree;
    },
    filterTransactions(condition) {
      return (entry) => {
        if (entry.type == 'transaction') {
          for (const transaction of entry.transactions) {
            if (condition(transaction)) {
              return true;
            }
          }

          return false;
        } else {
          return condition(entry);
        }
      };
    },
    isMeaningfulChange(object, key) {
      // TODO (b/159799733): Handle cases of non null objects but meaningless
      // change
      return object[key] !== null && object.hasOwnProperty(key);
    },
    mergeChanges(a, b) {
      const res = {};

      for (const key in a) {
        if (this.isMeaningfulChange(a, key)) {
          res[key] = a[key];
        }
      }

      for (const key in b) {
        if (this.isMeaningfulChange(b, key)) {
          if (res.hasOwnProperty(key) && key != 'id') {
            throw new Error(`Merge failed – key '${key}' already present`);
          }
          res[key] = b[key];
        }
      }

      return res;
    },
    aggregateTransactions(transactions) {
      const surfaceChanges = {};
      const displayChanges = {};

      for (const transaction of transactions) {
        const obj = transaction.obj;

        // Create a new base object to merge all changes into
        const newBaseObj = () => {
          return {
            layerName: transaction.layerName,
          };
        };

        switch (transaction.type) {
          case 'surfaceChange':
            surfaceChanges[obj.id] =
              this.mergeChanges(surfaceChanges[obj.id] ?? newBaseObj(), obj);
            break;

          case 'displayChange':
            displayChanges[obj.id] =
              this.mergeChanges(displayChanges[obj.id] ?? newBaseObj(), obj);
            break;

          default:
            throw new Error(`Unhandled transaction type ${transaction.type}`);
        }
      }

      return [surfaceChanges, displayChanges];
    },

    transactionHistory(selectedTransaction) {
      const transactionId = selectedTransaction.identifier;
      const history = this.transactionsTrace.transactionHistory
          .generateHistoryTreesOf(transactionId);

      return history;
    },

    prettifyTransactionId(transactionId) {
      const expandedId = expandTransactionId(transactionId);
      return `${expandedId.pid}.${expandedId.id}`;
    },

    squishVSyncs(data) {
      return data.filter((event, i) => {
        return !(event.type === 'vsyncEvent' &&
          data[i + 1]?.type === 'vsyncEvent');
      });
    },
  },
  components: {
    'virtual-list': VirtualList,
    'tree-view': TreeView,
    'flat-card': FlatCard,
  },
};

</script>
<style scoped>
.container {
  display: flex;
  flex-wrap: wrap;
}

.transaction-table,
.changes {
  flex: 1 1 0;
  width: 0;
  margin: 8px;
}

.scrollBody {
  width: 100%;
  height: 100%;
  overflow: scroll;
}

.filters {
  margin-bottom: 15px;
  width: 100%;
  padding: 15px 5px;
  display: flex;
  flex-wrap: wrap;
}

.filters .input {
  max-width: 300px;
  margin: 0 10px;
  flex-grow: 1;
}

.changes-content {
  padding: 18px;
  height: 550px;
  overflow: auto;
}

.no-properties {
  display: flex;
  flex-direction: column;
  align-self: center;
  align-items: center;
  justify-content: center;
  height: calc(100% - 50px);
  padding: 50px 25px;
}

.no-properties .none-icon {
  font-size: 35px;
  margin-bottom: 10px;
}

.no-properties span {
  font-weight: 100;
}

.transaction-event {
  display: inline-flex;
}
</style>

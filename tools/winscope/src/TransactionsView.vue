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
      <div class="filters">
        <md-field>
          <label>Transaction Type</label>
          <md-select v-model="selectedTransactionTypes" multiple>
            <md-option v-for="type in transactionTypes" :value="type">{{ type }}</md-option>
          </md-select>
        </md-field>

        <div>
          <md-autocomplete v-model="selectedProperty" :md-options="properties">
            <label>Changed property</label>
          </md-autocomplete>
          <!-- TODO(b/159582192): Add way to select value a property has changed to,
                figure out how to handle properties that are objects... -->
        </div>

        <md-chips v-model="filters" md-placeholder="Add surface id or name...">
          <div class="md-helper-text">Press enter to add</div>
        </md-chips>
      </div>

      <virtual-list style="height: 600px; overflow-y: auto;"
        :data-key="'timestamp'"
        :data-sources="filteredData"
        :data-component="transactionEntryComponent"
        :extra-props="{onClick: transactionSelected, selectedTransaction }"
        ref="loglist"
      />
    </flat-card>

    <flat-card class="changes card">
      <md-content md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
        <h2 class="md-title" style="flex: 1">Changes</h2>
      </md-content>
      <div class="changes-content" v-if="selectedTree">
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
import TransactionEntry from './TransactionEntry.vue';
import FlatCard from './components/FlatCard.vue';

import { ObjectTransformer } from './transform.js';
import { stableIdCompatibilityFixup } from './utils/utils.js';

export default {
  name: 'transactionsview',
  props: ['data'],
  data() {
    const transactionTypes = new Set();
    const properties = new Set();
    for (const entry of this.data) {
      if (entry.type == "transaction") {
        for (const transaction of entry.transactions) {
          transactionTypes.add(transaction.type);
          Object.keys(transaction.obj).forEach(item => properties.add(item));
        }
      } else {
        transactionTypes.add(entry.type);
        Object.keys(entry.obj).forEach(item => properties.add(item));
      }
    }

    return {
      transactionTypes: Array.from(transactionTypes),
      properties: Array.from(properties),
      selectedTransactionTypes: [],
      searchInput: "",
      selectedTree: null,
      filters: [],
      selectedProperty: null,
      selectedTransaction: null,
      transactionEntryComponent: TransactionEntry,
    };
  },
  computed: {
    filteredData() {
      let filteredData = this.data;

      if (this.selectedTransactionTypes.length > 0) {
        filteredData = filteredData.filter(
          this.filterTransactions(transaction =>
            this.selectedTransactionTypes.includes(transaction.type)));
      }

      if (this.filters.length > 0) {
        filteredData = filteredData.filter(
          this.filterTransactions(transaction => {
            for (const filter of this.filters) {
              if (isNaN(filter) && transaction.obj?.name?.includes(filter)) {
                // If filter isn't a number then check if the transaction's
                // target surface's name matches the filter — if so keep it.
                return true;
              }
              if (filter == transaction.obj.id) {
                // If filteter is a number then check if the filter matches
                // the transaction's target surface id — if so keep it.
                return true;
              }
            }

            // Exclude transaction if it fails to match filter.
            return false;
          })
        );
      }

      if (this.selectedProperty) {
        filteredData = filteredData.filter(
          this.filterTransactions(transaction => {
            for (const key in transaction.obj) {
              if (this.isMeaningfulChange(transaction.obj, key)
                    && key === this.selectedProperty) {
                return true;
              }
            }

            return false;
          })
        );
      }

      return filteredData;
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

      const META_DATA_KEY = "metadata"

      let obj;
      let name;
      if (transaction.type == "transaction") {
        name = "changes";
        obj = {};

        const [surfaceChanges, displayChanges] =
          this.aggregateTransactions(transaction.transactions);

        // Prepare the surface and display changes to be passed through
        // the ObjectTransformer — in particular, remove redundant properties
        // and add metadata that can be accessed post transformation
        const perpareForTreeViewTransform = (change) => {
          this.removeNullFields(change);
          change[META_DATA_KEY] = {
            // TODO (b/162402459): Shorten layer name
            layerName: change.layerName,
          }
          // remove redundant properties
          delete change.layerName;
          delete change.id;

          console.log(change)
        };

        for (const changeId in surfaceChanges) {
          perpareForTreeViewTransform(surfaceChanges[changeId])
        }
        for (const changeId in displayChanges) {
          perpareForTreeViewTransform(displayChanges[changeId])
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
        transactionUniqueId
      ).setOptions({
        formatter: () => {},
      }).transform({
        keepOriginal: true,
        metadataKey: META_DATA_KEY,
        freeze: false
      });

      // Add the layer name as the kind of the object to be shown in the TreeView
      const addLayerNameAsKind = (tree) => {
        for (const layerChanges of tree.children) {
          layerChanges.kind = layerChanges.metadata.layerName;
        }
      }

      if (transaction.type == "transaction") {
        for (const child of tree.children) {
          // child = surfaceChanges or displayChanges tree node
          addLayerNameAsKind(child)
        }
      }

      // If there are only surfaceChanges or only displayChanges and not both
      // remove the extra top layer node which is meant to hold both types of
      // changes when both are present
      if (tree.name == "changes" && tree.children.length === 1) {
        tree = tree.children[0];
      }

      this.selectedTree = tree;
    },
    filterTransactions(condition) {
      return (entry) => {
        if (entry.type == "transaction") {
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
      // TODO (b/159799733): Handle cases of non null objects but meaningless change
      return object[key] !== null && object.hasOwnProperty(key)
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
          if (res.hasOwnProperty(key) && key != "id") {
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
          }
        }

        switch (transaction.type) {
          case "surfaceChange":
            surfaceChanges[obj.id] =
              this.mergeChanges(surfaceChanges[obj.id] ?? newBaseObj(), obj);
            break;

          case "displayChange":
            displayChanges[obj.id] =
              this.mergeChanges(displayChanges[obj.id] ?? newBaseObj(), obj);
            break;

          default:
            throw new Error(`Unhandled transaction type ${transaction.type}`);
        }
      }

      return [surfaceChanges, displayChanges];
    },
  },
  components: {
    'virtual-list': VirtualList,
    'tree-view': TreeView,
    'flat-card': FlatCard,
  }
}

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
</style>

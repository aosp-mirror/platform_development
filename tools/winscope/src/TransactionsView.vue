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
    <md-table v-model="filteredData" class="transaction-table card" md-card md-fixed-header>
      <md-table-toolbar>
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
      </md-table-toolbar>

      <md-table-row class="row" slot="md-table-row" slot-scope="{ item }" @click="transactionSelected(item)">
        <md-table-cell md-label="Time">{{ item.time }}</md-table-cell>
        <md-table-cell md-label="Type(s)">
          {{ transactionTypeOf(item) }}
        </md-table-cell>
        <md-table-cell md-label="Affected Surfaces">
          <span v-for="(surface, index) in sufacesAffectedBy(item)">
            {{surface.id}}<span v-if="surface.name"> ({{ surface.name }})</span>
            <span v-if="index + 1 < sufacesAffectedBy(item).length">,&nbsp;</span>
          </span>
        </md-table-cell>
      </md-table-row>

    </md-table>

    <md-card class="changes card">
      <md-content md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
        <h2 class="md-title" style="flex: 1">Changes</h2>
      </md-content>
      <div class="changes-content">
        <tree-view
          :item="selectedTree"
          :collapseChildren="true"
          :useGlobalCollapsedState="true"
        />
      </div>
    </md-card>

  </md-card-content>
</template>
<script>
import TreeView from './TreeView.vue';

import { transform_json } from './transform.js';
import { stableIdCompatibilityFixup } from './utils/utils.js'

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
      let obj = this.removeNullFields(transaction.obj);
      let name = transaction.type;

      if (transaction.type == "transaction") {
        name = "changes";
        obj = {};

        const [surfaceChanges, displayChanges] =
          this.aggregateTransactions(transaction.transactions);

        for (const changeId in surfaceChanges) {
          this.removeNullFields(surfaceChanges[changeId]);
        }
        for (const changeId in displayChanges) {
          this.removeNullFields(displayChanges[changeId]);
        }

        if (Object.keys(surfaceChanges).length > 0) {
          obj.surfaceChanges = surfaceChanges;
        }

        if (Object.keys(displayChanges).length > 0) {
          obj.displayChanges = displayChanges;
        }
      }

      const transactionUnique = transaction.timestamp;
      let tree = transform_json(obj, name, transactionUnique, {
        formatter: () => {}
      });

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
      // TODO: Handle cases of non null objects but meaningless change
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

        switch (transaction.type) {
          case "surfaceChange":
            surfaceChanges[obj.id] =
              this.mergeChanges(surfaceChanges[obj.id] ?? {}, obj);
            break;

          case "displayChange":
            displayChanges[obj.id] =
              this.mergeChanges(displayChanges[obj.id] ?? {}, obj);
            break;

          default:
            throw new Error(`Unhandled transaction type ${transaction.type}`);
        }
      }

      return [surfaceChanges, displayChanges];
    },
    transactionTypeOf(transaction) {
      if (transaction.type !== 'transaction') {
        return transaction.type;
      }

      if (transaction.transactions.length === 0) {
        return "Empty Transaction";
      }

      const types = new Set();
      transaction.transactions.forEach(t => types.add(t.type));

      return Array.from(types).join(", ");
    },
    sufacesAffectedBy(transaction) {
      if (transaction.type !== 'transaction') {
        return [{name: transaction.layerName, id: transaction.obj.id}];
      }

      const surfaceIds = new Set();
      const affectedSurfaces = [];
      for (const transaction of transaction.transactions) {
        const id = transaction.obj.id;
        if (!surfaceIds.has(id)) {
          surfaceIds.add(id);
          affectedSurfaces.push({name: transaction.layerName, id});
        }
      }

      return affectedSurfaces
    },
  },
  components: {
    'tree-view': TreeView,
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
  flex: 1;
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

</style>

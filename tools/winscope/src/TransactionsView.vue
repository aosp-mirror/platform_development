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
    <md-table class="transaction-table" md-card>
      <md-table-toolbar>
        <md-field>
          <label>Transaction Type</label>
          <md-select v-model="selectedTransactionTypes" multiple>
            <md-option v-for="type in transactionTypes" :value="type">{{ type }}</md-option>
          </md-select>
        </md-field>

        <md-field md-clearable>
          <md-input v-model="searchInput" placeholder="Search by id or name..." />
          <span class="md-helper-text">Comma seperated</span>
        </md-field>

      </md-table-toolbar>

      <div class="scrollBody" ref="tableBody">
        <md-table-row v-for="transaction in filteredData" :key="transaction.timestamp">
          <md-table-cell>{{transaction.time}}</md-table-cell>

          <div v-if="transaction.type == 'transaction'">
            <md-table-cell>
              {{summarizeTranscations(transaction.transactions)}}
            </md-table-cell>
          </div>
          <div v-else>
            <md-table-cell>
              {{transaction.type}}
              <span v-if="transaction.obj.id !== undefined">: {{transaction.obj.id}}</span>
            </md-table-cell>
          </div>
        </md-table-row>
      </div>

    </md-table>
  </md-card-content>
</template>
<script>
export default {
  name: 'transactionsview',
  props: ['data'],
  data() {
    const transactionTypes = new Set();
    for (const entry of this.data) {
      if (entry.type == "transaction") {
        for (const transaction of entry.transactions) {
          transactionTypes.add(transaction.type);
        }
      } else {
        transactionTypes.add(entry.type);
      }
    }

    return {
      transactionTypes: Array.from(transactionTypes),
      selectedTransactionTypes: [],
      searchInput: "",
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

      if (this.searchInput) {
        const terms = this.searchInput.split(/,\s*/);
        filteredData = filteredData.filter(
          this.filterTransactions(transaction =>
            terms.includes("" + transaction.obj.id)));
      }

      return filteredData;
    },

  },
  methods: {
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
    summarizeTranscations(transactions) {
      const surfaceChanges = {};
      const displayChanges = {};

      for (const transaction of transactions) {
        const obj = transaction.obj;

        switch (transaction.type) {
          case "surfaceChange":
            surfaceChanges[obj.id] =
              Object.assign(surfaceChanges[obj.id] ?? {}, obj);
            break;

          case "displayChange":
            displayChanges[obj.id] =
              Object.assign(displayChanges[obj.id] ?? {}, obj);
            break;

          default:
            throw new Error(`Unhandled transaction type ${transaction.type}`);
        }
      }

      const summary = [];

      const surfaceChangesId = Object.keys(surfaceChanges);
      if (surfaceChangesId.length > 0) {
        summary.push(`surfaceChanges: ${surfaceChangesId.join(', ')}`);
      }

      const displayChangesIds = Object.keys(displayChanges);
      if (displayChangesIds.length > 0) {
        summary.push(`displayChanges: ${displayChangesIds.join(', ')}`);
      }

      return summary.join(" | ");
    }
  }
}

</script>
<style scoped>
/* .transaction-table {
  width: 100%;
} */

.scrollBody {
  /* width: 100%; */
  max-height: 75vh;
  overflow: scroll;
}

</style>

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
    <md-table class="transaction-table">
      <div class="scrollBody" ref="tableBody">
        <md-table-row v-for="transaction in data" :key="transaction.timestamp">
          <md-table-cell>{{transaction.time}}</md-table-cell>

          <div v-if="transaction.type == 'transaction'">
            <md-table-cell>
              {{summarizeTranscations(transaction.transactions)}}
            </md-table-cell>
          </div>
          <div v-else>
            <md-table-cell>{{transaction.type}}</md-table-cell>
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
    return {};
  },
  computed: {
    
  },
  methods: {
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

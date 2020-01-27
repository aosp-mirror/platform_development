<!-- Copyright (C) 2019 The Android Open Source Project

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
 <div class="bounds" v-if="visible">
   <md-select v-model="visibleTransactions" name="visibleTransactions" id="visibleTransactions"
           placeholder="Everything Turned Off" md-dense multiple @input="updateFilter()" >
       <md-option value="displayCreation, displayDeletion">Display</md-option>
       <md-option value="powerModeUpdate">Power Mode</md-option>
       <md-option value="surfaceCreation, surfaceDeletion">Surface</md-option>
       <md-option value="transaction">Transaction</md-option>
       <md-option value="vsyncEvent">vsync</md-option>
       <md-option value="bufferUpdate">Buffer</md-option>
   </md-select>
 </div>
</template>
<script>
import { DATA_TYPES } from './decode.js'

export default {
  name: 'datafilter',
  props: ['file'],
  data() {
    return {
      rawData: this.file.data,
      rawTimeline: this.file.timeline,
      visibleTransactions: ["powerModeUpdate", "surfaceCreation, surfaceDeletion",
                    "displayCreation, displayDeletion", "transaction"]
    };
  },
  methods: {
    updateFilter() {
      this.file.data =
              this.rawData.filter(x => this.visibleTransactions.includes(x.obj.increment));
      this.file.timeline =
              this.rawTimeline.filter(x => this.file.data.map(y => y.timestamp).includes(x));
    },
  },
  computed: {
    visible() {
      return this.file.type == DATA_TYPES.TRANSACTION
    },
  }
}
</script>

<style scoped>
    .bounds {
        margin: 1em;
    }
</style>

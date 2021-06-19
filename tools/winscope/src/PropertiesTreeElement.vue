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
  <span>
    <span class="key">{{ key }} </span>
    <span v-if="value">: </span>
    <span class="value" v-if="value" :class="[valueClass]">{{ value }}</span>
  </span>
</template>
<script>
export default {
  name: 'PropertiesTreeElement',
  props: ['item', 'simplify-names'],
  computed: {
    key() {
      if (!this.item.children || this.item.children.length === 0) {
        return this.item.name.split(': ')[0];
      }

      return this.item.name;
    },
    value() {
      if (!this.item.children || this.item.children.length === 0) {
        return this.item.name.split(': ').slice(1).join(': ');
      }

      return null;
    },
    valueClass() {
      if (!this.value) {
        return null;
      }

      if (this.value == 'null') {
        return 'null';
      }

      if (this.value == 'true') {
        return 'true';
      }

      if (this.value == 'false') {
        return 'false';
      }

      if (!isNaN(this.value)) {
        return 'number';
      }
    },
  },
};
</script>
<style scoped>
.key {
  color: #4b4b4b;
}
.value {
  color: #8A2BE2;
}
.value.null {
  color: #e1e1e1;
}
.value.number {
  color: #4c75fd;
}
.value.true {
  color: #2ECC40;
}
.value.false {
  color: #FF4136;
}
</style>

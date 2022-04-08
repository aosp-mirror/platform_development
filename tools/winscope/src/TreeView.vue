<!-- Copyright (C) 2017 The Android Open Source Project

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
  <div class="tree-view">
    <div @click="clicked" :class="computedClass">
      <span class="kind">{{item.kind}}</span><span v-if="item.kind && item.name"> - </span><span>{{item.name}}</span>
      <div v-for="c in item.chips" :title="c.long" :class="chipClassForChip(c)">
        {{c.short}}
      </div>
    </div>
    <div class="children" v-if="children">
      <tree-view v-for="(c,i) in children" :item="c" @item-selected="childItemSelected" :selected="selected" :key="i" :chip-class='chipClass' :filter="childFilter(c)" :flattened="flattened" :force-flattened="applyingFlattened" v-show="filterMatches(c)" ref='children' />
    </div>
  </div>
</template>
<script>
import jsonProtoDefs from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto'
import protobuf from 'protobufjs'

var protoDefs = protobuf.Root.fromJSON(jsonProtoDefs);

export default {
  name: 'tree-view',
  props: ['item', 'selected', 'chipClass', 'filter', 'flattened', 'force-flattened'],
  data() {
    return {};
  },
  methods: {
    selectNext(found, parent) {
      if (found && this.filterMatches(this.item)) {
        this.clicked();
        return false;
      }
      if (this.selected === this.item) {
        found = true;
      }
      if (this.$refs.children) {
        for (var c of this.$refs.children) {
          found = c.selectNext(found);
        }
      }
      return found;
    },
    selectPrev(found) {
      if (this.$refs.children) {
        for (var c of [...this.$refs.children].reverse()) {
          found = c.selectPrev(found);
        }
      }
      if (found && this.filterMatches(this.item)) {
        this.clicked();
        return false;
      }
      if (this.selected === this.item) {
        found = true;
      }
      return found;
    },
    childItemSelected(item) {
      this.$emit('item-selected', item);
    },
    clicked() {
      this.$emit('item-selected', this.item);
    },
    chipClassForChip(c) {
      return ['tree-view-internal-chip', this.chipClassOrDefault,
        this.chipClassOrDefault + '-' + (c.class || 'default')
      ];
    },
    filterMatches(c) {
      // If a filter is set, consider the item matches if the current item or any of its
      // children matches.
      if (this.filter) {
        var thisMatches = this.filter(c);
        const childMatches = (child) => this.filterMatches(child);
        return thisMatches || (!this.applyingFlattened && 
            c.children && c.children.some(childMatches));
      }
      return true;
    },
    childFilter(c) {
      if (this.filter) {
        if (this.filter(c)) {
          // Filter matched c, don't apply further filtering on c's children.
          return undefined;
        }
      }
      return this.filter;
    },
  },
  computed: {
    computedClass() {
      return (this.item == this.selected) ? 'selected' : ''
    },
    chipClassOrDefault() {
      return this.chipClass || 'tree-view-chip';
    },
    applyingFlattened() {
      return this.flattened && this.item.flattened || this.forceFlattened;
    },
    children() {
      return this.applyingFlattened ? this.item.flattened : this.item.children;
    },
  }
}

</script>
<style>
.children {
  margin-left: 24px;
}

.kind {
  color: #333;
}

.selected {
  background-color: #3f51b5;
  color: white;
}

.selected .kind {
  color: #ccc;
}

.tree-view-internal-chip {
  display: inline-block;
}

.tree-view-chip {
  padding: 0 10px;
  border-radius: 10px;
  background-color: #aaa;
  color: black;
}

.tree-view-chip.tree-view-chip-warn {
  background-color: #ffaa6b;
  color: black;
}

.tree-view-chip.tree-view-chip-error {
  background-color: #ff6b6b;
  color: black;
}

.tree-view-chip.tree-view-chip-gpu {
  background-color: #00c853;
  color: black;
}

.tree-view-chip.tree-view-chip-hwc {
  background-color: #448aff;
  color: black;
}

</style>

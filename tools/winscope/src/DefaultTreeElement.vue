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
    <span class="kind">{{item.kind}}</span>
    <span v-if="item.kind && item.name">-</span>
    <span>
    <span v-if="simplifyNames && item.shortName && item.shortName !== item.name">
        {{ item.shortName }}
        <md-tooltip
        md-delay="300"
        md-direction="top"
        style="margin-bottom: -10px"
        >
        {{item.name}}
        </md-tooltip>
    </span>
    <span v-else>
        {{ item.name }}
    </span>
    </span>
    <div
    v-for="c in item.chips"
    v-bind:key="c.long"
    :title="c.long"
    :class="chipClassForChip(c)"
    >
    {{c.short}}
    </div>
  </span>
</template>

<script>
export default {
  name: 'DefaultTreeElement',
  props: ["item", "simplify-names"],
  methods: {
    chipClassForChip(c) {
      return [
        "tree-view-internal-chip",
        "tree-view-chip",
        "tree-view-chip" + "-" + (c.type?.toString() || c.class?.toString() || "default")
      ];
    },
  }
}
</script>

<style scoped>
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
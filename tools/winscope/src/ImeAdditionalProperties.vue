<!-- Copyright (C) 2022 The Android Open Source Project

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
<template class="container">
  <div v-if="entry.wmProperties">
    <div class="group">
      <span class="group-header">Name</span>
      <div class="full-width">
        <span class="value">{{ entry.wmProperties.name }}</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">Focus</span>
      <div class="full-width">
        <div />
        <span class="key">Focused App:</span>
        <span class="value">{{ entry.wmProperties.focusedApp }}</span>
        <div />
        <span class="key">Focused Activity:</span>
        <span class="value">{{ entry.wmProperties.focusedActivity }}</span>
        <div />
        <span class="key">Focused Window:</span>
        <span class="value">{{ entry.wmProperties.focusedWindow }}</span>
        <div />
      </div>
    </div>
    <div class="group">
      <span class="group-header">Insets Source Provider</span>
      <div class="full-width">
        <tree-view
            class="tree-view"
            :collapse-children="true"
            :item="getTransformedProperties(
                entry.wmProperties
                .imeInsetsSourceProvider.insetsSourceProvider)"
        />
      </div>
    </div>
    <div class="group">
      <span class="group-header">IMControl Target</span>
      <div class="full-width">
        <tree-view
            class="tree-view"
            :collapse="true"
            :collapse-children="true"
            :item="getTransformedProperties(
                entry.wmProperties.inputMethodControlTarget)"
        />
      </div>
    </div>
    <div class="group">
      <span class="group-header">IMInput Target</span>
      <div class="full-width">
        <tree-view
            class="tree-view"
            :collapse="true"
            :collapse-children="true"
            :item="getTransformedProperties(
                entry.wmProperties.inputMethodInputTarget)"
        />
      </div>
    </div>
    <div class="group">
      <span class="group-header">IM Target</span>
      <div class="full-width">
        <tree-view
            class="tree-view"
            :collapse="true"
            :collapse-children="true"
            :item="getTransformedProperties(
                entry.wmProperties.inputMethodTarget)"
        />
      </div>
    </div>
  </div>
</template>

<script>

import TreeView from '@/TreeView';
import ObjectFormatter from './flickerlib/ObjectFormatter';
import {ObjectTransformer} from '@/transform';
import {getPropertiesForDisplay} from '@/flickerlib/mixin';
import {stableIdCompatibilityFixup} from '@/utils/utils';

function formatProto(obj) {
  if (obj?.prettyPrint) {
    return obj.prettyPrint();
  }
}

export default {
  name: 'ImeAdditionalProperties',
  components: {TreeView},
  props: ['entry'],
  methods: {
    getTransformedProperties(item) {
      // this function is similar to the one in TraceView.vue,
      // but without 'diff visualisation'
      ObjectFormatter.displayDefaults = this.displayDefaults;
      // There are 2 types of object whose properties can appear in the property
      // list: Flicker objects (WM/SF traces) and dictionaries
      // (IME/Accessibilty/Transactions).
      // While flicker objects have their properties directly in the main object
      // those created by a call to the transform function have their properties
      // inside an obj property. This makes both cases work
      // TODO(209452852) Refactor both flicker and winscope-native objects to
      // implement a common display interface that can be better handled
      const target = item.obj ?? item;
      const transformer = new ObjectTransformer(
          getPropertiesForDisplay(target),
          item.name,
          stableIdCompatibilityFixup(item),
      ).setOptions({
        skip: item.skip,
        formatter: formatProto,
      });

      return transformer.transform();
    },
  },
};
</script>

<style scoped>
.container {
  overflow: auto;
}

.group {
  padding: 0.5rem;
  border-bottom: thin solid rgba(0, 0, 0, 0.12);
  flex-direction: row;
  display: flex;
  max-height: 180px;
}

.group .key {
  font-weight: 500;
}

.group .value {
  color: rgba(0, 0, 0, 0.75);
}

.group-header {
  justify-content: center;
  padding: 0px 5px;
  width: 80px;
  display: inline-block;
  font-size: bigger;
  color: grey;
}

.left-column {
  width: 320px;
  max-width: 100%;
  display: inline-block;
  vertical-align: top;
  overflow: auto;
  padding-right: 20px;
}

.right-column {
  width: 320px;
  max-width: 100%;
  display: inline-block;
  vertical-align: top;
  overflow: auto;
}

.full-width {
  width: 100%;
  display: inline-block;
  vertical-align: top;
  overflow: auto;
}

.column-header {
  font-weight: lighter;
  font-size: smaller;
}

.element-summary {
  padding-top: 1rem;
}

.element-summary .key {
  font-weight: 500;
}

.element-summary .value {
  color: rgba(0, 0, 0, 0.75);
}

.tree-view {
  overflow: auto;
}
</style>

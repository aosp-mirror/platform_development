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
  <div v-if="isImeManagerService">
    <div class="group">
      <span class="group-header">Name</span>
      <div class="full-width">
        <span class="value">{{ entry.wmProperties.name }}</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">IME Insets Source Provider</span>
      <div class="left-column">
        <span class="key">Position:</span>
        <span class="value">{{ wmInsetsSourceProviderPositionOrNull }}</span>
        <div />
        <span class="key">IsLeashReadyForDispatching:</span>
        <span class="value">{{
            wmInsetsSourceProviderIsLeashReadyOrNull }}</span>
        <div />
        <span class="key">Controllable:</span>
        <span class="value">{{
            wmInsetsSourceProviderControllableOrNull }}</span>
        <div />
      </div>
      <div class="right-column">
        <span class="key">Source Frame:</span>
        <span class="value">{{ wmInsetsSourceProviderSourceFrameOrNull }}</span>
        <div />
        <span class="key">Source Visible:</span>
        <span class="value">{{
            wmInsetsSourceProviderSourceVisibleOrNull }}</span>
        <div />
        <span class="key">Source Visible Frame:</span>
        <span class="value">{{
            wmInsetsSourceProviderSourceVisibleFrameOrNull }}</span>
        <div />
      </div>
    </div>
    <div class="group">
      <span class="group-header">IMControl Target</span>
      <div class="full-width">
        <button
            class="text-button"
            v-if="wmIMControlTargetOrNull"
            @click="onSelectItem(wmIMControlTargetOrNull)">
          Input Method Control Target
        </button>
        <span class="value" v-else>null</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">IMInput Target</span>
      <div class="full-width">
        <button
            class="text-button"
            v-if="wmIMInputTargetOrNull"
            @click="onSelectItem(wmIMInputTargetOrNull)">
          Input Method Input Target
        </button>
        <span class="value" v-else>null</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">IM Target</span>
      <div class="full-width">
        <button
            class="text-button"
            v-if="wmIMTargetOrNull"
            @click="onSelectItem(wmIMTargetOrNull)">
          Input Method Target
        </button>
        <span class="value" v-else>null</span>
      </div>
    </div>
  </div>
  <div v-else>
    <!-- Ime Client or Ime Service -->
    <div class="group">
      <span class="group-header">Name</span>
      <div class="full-width">
        <span class="value">{{ entry.wmProperties.name }}</span>
      </div>
    </div>
    <div class="group" v-if="entry.wmProperties">
      <span class="group-header">Focus</span>
      <div class="full-width">
        <span class="key">Focused App:</span>
        <span class="value">{{ entry.wmProperties.focusedApp }}</span>
        <div />
        <span class="key">Focused Activity:</span>
        <span class="value">{{ entry.wmProperties.focusedActivity }}</span>
        <div />
        <span class="key">Focused Window:</span>
        <span class="value">{{ entry.wmProperties.focusedWindow }}</span>
        <div />
        <span class="key">Frame:</span>
        <span class="value">{{ wmControlTargetFrameOrNull }}</span>
        <div />
      </div>
    </div>
    <div class="group" v-if="entry.sfImeContainerProperties">
      <span class="group-header">Ime Container</span>
      <div class="full-width">
        <span class="key">Bounds:</span>
        <span class="value">{{ entry.sfImeContainerProperties.bounds }}</span>
        <div />
        <span class="key">Rect:</span>
        <span class="value">{{ entry.sfImeContainerProperties.rect }}</span>
        <div />
        <span class="key">ZOrderRelativeOfId:</span>
        <span class="value">
          {{ entry.sfImeContainerProperties.zOrderRelativeOfId }}</span>
        <div />
        <span class="key">Z:</span>
        <span class="value">{{ entry.sfImeContainerProperties.z }}</span>
        <div />
      </div>
    </div>
  </div>
</template>

<script>

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
  props: ['entry', 'isImeManagerService', 'onSelectItem'],
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
  computed: {
    wmControlTargetFrameOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.controlTarget?.windowFrames?.frame || 'null';
    },
    wmInsetsSourceProviderPositionOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.control?.position || 'null';
    },
    wmInsetsSourceProviderIsLeashReadyOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.isLeashReadyForDispatching || 'null';
    },
    wmInsetsSourceProviderControllableOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.controllable || 'null';
    },
    wmInsetsSourceProviderSourceFrameOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.source?.frame || 'null';
    },
    wmInsetsSourceProviderSourceVisibleOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.source?.visible || 'null';
    },
    wmInsetsSourceProviderSourceVisibleFrameOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider
          ?.insetsSourceProvider?.source?.visibleFrame || 'null';
    },
    wmIMControlTargetOrNull() {
      return this.entry?.wmProperties?.inputMethodControlTarget ?
          Object.assign({'name': 'Input Method Control Target'},
              this.entry.wmProperties.inputMethodControlTarget) :
          null;
    },
    wmIMInputTargetOrNull() {
      return this.entry?.wmProperties?.inputMethodInputTarget ?
          Object.assign({'name': 'Input Method Input Target'},
              this.entry.wmProperties.inputMethodInputTarget) :
          null;
    },
    wmIMTargetOrNull() {
      return this.entry?.wmProperties?.inputMethodTarget ?
          Object.assign({'name': 'Input Method Target'},
              this.entry.wmProperties.inputMethodTarget) :
          null;
    },
  },
  watch: {
    entry() {
      console.log(this.entry);
    },
    onSelectItem() {
      console.log(this.onSelectItem);
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
  word-break: break-all !important;
}

.group-header {
  justify-content: center;
  padding: 0px 5px;
  width: 90px;
  display: inline-block;
  font-size: bigger;
  color: grey;
  word-break: break-word;
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

.text-button {
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-family: roboto;
  color: blue;
  text-decoration: underline;
  text-decoration-color: blue;
  background-color: inherit;
}

</style>

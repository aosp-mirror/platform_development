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
  <div v-if="isAllPropertiesNull" class="group">
    There is no corresponding WM / SF entry for this IME entry –
    no WM / SF entry is recorded before this IME entry in time.
    View later frames for WM & SF properties.
  </div>
  <div v-else-if="isImeManagerService">
    <div class="group">
      <button
          class="text-button group-header"
          v-if="wmProtoOrNull"
          :class="{ 'selected': isSelected(wmProtoOrNull) }"
          @click="onClickShowInPropertiesPanel(wmProtoOrNull)">
        WMState
      </button>
      <span class="group-header" v-else>WMState</span>
      <div class="full-width">
        <span class="value" v-if="entry.wmProperties">{{
            entry.wmProperties.name }}</span>
        <span v-else>There is no corresponding WMState entry.</span>
      </div>
    </div>
    <div class="group" v-if="wmInsetsSourceProviderOrNull">
      <button
          class="text-button group-header"
          :class="{ 'selected': isSelected(wmInsetsSourceProviderOrNull) }"
          @click="onClickShowInPropertiesPanel(wmInsetsSourceProviderOrNull)">
        IME Insets Source Provider
      </button>
      <div class="full-width">
        <div />
        <span class="key">Source Frame:</span>
        <CoordinatesTable
            :coordinates="wmInsetsSourceProviderSourceFrameOrNull" />
        <div />
        <span class="key">Source Visible:</span>
        <span class="value">{{
            wmInsetsSourceProviderSourceVisibleOrNull }}</span>
        <div />
        <span class="key">Source Visible Frame:</span>
        <CoordinatesTable
            :coordinates="wmInsetsSourceProviderSourceVisibleFrameOrNull" />
        <div />
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
    </div>
    <div class="group" v-if="wmImeControlTargetOrNull">
      <button
          class="text-button group-header"
          :class="{ 'selected': isSelected(wmImeControlTargetOrNull) }"
          @click="onClickShowInPropertiesPanel(wmImeControlTargetOrNull)">
        IME Control Target
      </button>
      <div class="full-width">
        <span class="key" v-if="wmImeControlTargetTitleOrNull">Title:</span>
        <span class="value" v-if="wmImeControlTargetTitleOrNull">{{
            wmImeControlTargetTitleOrNull }}</span>
      </div>
    </div>
    <div class="group" v-if="wmImeInputTargetOrNull">
      <button
          class="text-button group-header"
          :class="{ 'selected': isSelected(wmImeInputTargetOrNull) }"
          @click="onClickShowInPropertiesPanel(wmImeInputTargetOrNull)">
        IME Input Target
      </button>
      <div class="full-width">
        <span class="key" v-if="wmImeInputTargetTitleOrNull">Title:</span>
        <span class="value" v-if="wmImeInputTargetTitleOrNull">{{
            wmImeInputTargetTitleOrNull }}</span>
      </div>
    </div>
    <div class="group" v-if="wmImeLayeringTargetOrNull">
      <button
          class="text-button group-header"
          :class="{ 'selected': isSelected(wmImeLayeringTargetOrNull) }"
          @click="onClickShowInPropertiesPanel(wmImeLayeringTargetOrNull)">
        IME Layering Target
      </button>
      <div class="full-width">
        <span class="key" v-if="wmImeLayeringTargetTitleOrNull">Title:</span>
        <span class="value" v-if="wmImeLayeringTargetTitleOrNull">{{
            wmImeLayeringTargetTitleOrNull }}</span>
      </div>
    </div>
  </div>

  <div v-else>
    <!-- Ime Client or Ime Service -->
    <div class="group">
      <button
          class="text-button group-header"
          v-if="wmProtoOrNull"
          :class="{ 'selected': isSelected(wmProtoOrNull) }"
          @click="onClickShowInPropertiesPanel(wmProtoOrNull)">
        WMState
      </button>
      <span class="group-header" v-else>WMState</span>
      <div class="full-width">
        <span class="value" v-if="entry.wmProperties">{{
            entry.wmProperties.name }}</span>
        <span v-else>There is no corresponding WMState entry.</span>
      </div>
    </div>
    <div class="group">
      <span class="group-header">SFLayer</span>
      <div class="full-width">
        <span class="value" v-if="entry.sfProperties">{{
            entry.sfProperties.name }}</span>
        <span v-else>There is no corresponding SFLayer entry.</span>
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
        <span class="key" v-if="entry.sfProperties">Focused Window Color:</span>
        <span class="value" v-if="entry.sfProperties">{{
            entry.sfProperties.focusedWindowRgba
          }}</span>
        <div />
        <span class="key">Input Control Target Frame:</span>
        <CoordinatesTable :coordinates="wmControlTargetFrameOrNull" />
        <div />
      </div>
    </div>
    <div class="group">
      <span class="group-header">Visibility</span>
      <div class="full-width">
        <span class="key" v-if="entry.wmProperties">InputMethod Window:</span>
        <span class="value" v-if="entry.wmProperties">{{
            entry.wmProperties.isInputMethodWindowVisible
          }}</span>
        <div />
        <span class="key" v-if="entry.sfProperties">InputMethod Surface:</span>
        <span class="value" v-if="entry.sfProperties">{{
            entry.sfProperties.isInputMethodSurfaceVisible }}</span>
        <div />
      </div>
    </div>
    <div class="group" v-if="entry.sfProperties">
      <button
          class="text-button group-header"
          :class="{ 'selected': isSelected(entry.sfProperties.imeContainer) }"
          @click="onClickShowInPropertiesPanel(entry.sfProperties.imeContainer)">
        Ime Container
      </button>
      <div class="full-width">
        <span class="key">ZOrderRelativeOfId:</span>
        <span class="value">{{
            entry.sfProperties.zOrderRelativeOfId
          }}</span>
        <div />
        <span class="key">Z:</span>
        <span class="value">{{ entry.sfProperties.z }}</span>
        <div />
      </div>
    </div>
    <div class="group" v-if="entry.sfProperties">
      <button
          class="text-button group-header"
          :class="{
            'selected': isSelected(entry.sfProperties.inputMethodSurface)
          }"
          @click="onClickShowInPropertiesPanel(
              entry.sfProperties.inputMethodSurface)">
        Input Method Surface
      </button>
      <div class="full-width">
        <span class="key">ScreenBounds:</span>
        <CoordinatesTable
        :coordinates="sfImeContainerScreenBoundsOrNull" />
        <div />
        <span class="key">Rect:</span>
        <CoordinatesTable
        :coordinates="sfImeContainerRectOrNull" />
        <div />
      </div>
    </div>
  </div>
</template>

<script>

import CoordinatesTable from '@/CoordinatesTable';
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
  components: {CoordinatesTable},
  props: ['entry', 'isImeManagerService', 'onSelectItem'],
  data() {
    return {
      selected: null,
    };
  },
  methods: {
    getTransformedProperties(item) {
      // this function is similar to the one in TraceView.vue,
      // but without 'diff visualisation'
      ObjectFormatter.displayDefaults = this.displayDefaults;
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

    onClickShowInPropertiesPanel(item) {
      this.selected = item;
      this.onSelectItem(item);
    },

    isSelected(item) {
      return this.selected === item;
    },

  },
  computed: {
    wmProtoOrNull() {
      return this.entry.wmProperties?.proto;
    },
    wmInsetsSourceProviderOrNull() {
      return this.entry.wmProperties?.imeInsetsSourceProvider ?
          Object.assign({'name': 'Ime Insets Source Provider'},
              this.entry.wmProperties.imeInsetsSourceProvider) :
          null;
    },
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
    wmImeControlTargetOrNull() {
      return this.entry?.wmProperties?.imeControlTarget ?
          Object.assign({'name': 'IME Control Target'},
              this.entry.wmProperties.imeControlTarget) :
          null;
    },
    wmImeControlTargetTitleOrNull() {
      return this.entry?.wmProperties?.imeControlTarget?.windowContainer
          ?.identifier?.title || 'null';
    },
    wmImeInputTargetOrNull() {
      return this.entry?.wmProperties?.imeInputTarget ?
          Object.assign({'name': 'IME Input Target'},
              this.entry.wmProperties.imeInputTarget) :
          null;
    },
    wmImeInputTargetTitleOrNull() {
      return this.entry?.wmProperties?.imeInputTarget?.windowContainer
          ?.identifier?.title || 'null';
    },
    wmImeLayeringTargetOrNull() {
      return this.entry?.wmProperties?.imeLayeringTarget ?
          Object.assign({'name': 'IME Layering Target'},
              this.entry.wmProperties.imeLayeringTarget) :
          null;
    },
    wmImeLayeringTargetTitleOrNull() {
      return this.entry?.wmProperties?.imeLayeringTarget?.windowContainer
          ?.identifier?.title || 'null';
    },
    sfImeContainerScreenBoundsOrNull() {
      return this.entry.sfProperties?.screenBounds || 'null';
    },
    sfImeContainerRectOrNull() {
      return this.entry.sfProperties?.rect || 'null';
    },
    isAllPropertiesNull() {
      if (this.isImeManagerService) {
        return !this.entry.wmProperties;
      } else {
        return !(this.entry.wmProperties ||
            this.entry.sfProperties);
      }
    },
  },
  watch: {
    entry() {
      console.log('Updated IME entry:', this.entry);
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
  text-align: left;
  padding: 0px 5px;
  width: 95px;
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

.text-button:focus {
  color: purple;
}

.text-button.selected {
  color: purple;
}

</style>

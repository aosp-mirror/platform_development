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
  <TraceView
    :store="store"
    :file="file"
    :summarizer="summarizer"
    :presentTags="presentTags"
    :presentErrors="presentErrors"
  />
</template>

<script>
import TraceView from '@/TraceView.vue';

export default {
  name: 'SurfaceFlingerTraceView',
  props: ['store', 'file', 'presentTags', 'presentErrors'],
  components: {
    TraceView,
  },
  methods: {
    summarizer(layer) {
      const summary = [];

      if (layer?.visibilityReason) {
        let reason = "";
        if (Array.isArray(layer.visibilityReason)) {
          reason = layer.visibilityReason.join(", ");
        } else {
          reason = layer.visibilityReason;
        }

        summary.push({key: 'Invisible due to', value: reason});
      }

      if (layer?.occludedBy?.length > 0) {
        summary.push({key: 'Occluded by', value: layer.occludedBy.map(it => it.id).join(', ')});
      }

      if (layer?.partiallyOccludedBy?.length > 0) {
        summary.push({
          key: 'Partially occluded by',
          value: layer.partiallyOccludedBy.map(it => it.id).join(', '),
        });
      }

      if (layer?.coveredBy?.length > 0) {
        summary.push({key: 'Covered by', value: layer.coveredBy.map(it => it.id).join(', ')});
      }

      return summary;
    },
  },
};
</script>

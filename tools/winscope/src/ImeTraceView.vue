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

<template>
  <TraceView
      :store="store"
      :file="file"
      :summarizer="summarizer"
      :presentTags="presentTags"
      :presentErrors="presentErrors"
      :propertyGroups="false"
      :imeAdditionalProperties="this.hasWmSfProperties"
  />
</template>

<script>
import TraceView from "@/TraceView.vue"

export default {
  name: "ImeTraceView",
  props: ["store", "file", "presentTags", "presentErrors"],
  data() {
    return {
      hasWmSfProperties:
          this.file.imeTraceFileClients?.data[0]?.hasWmSfProperties ||
          this.file.imeTraceFileService?.data[0]?.hasWmSfProperties ||
          this.file.imeTraceFileManagerService?.data[0]?.hasWmSfProperties ||
          false,
    };
  },
  components: {
    TraceView,
  },
  methods: {
    summarizer(item) {
      return null; // TODO: Not sure what to do with this
    },
  }
}
</script>

<template>
  <v-row>
    <v-col
      cols="12"
      md="6"
    >
      <OTAOptions
        :targetDetails="targetDetails"
        :incrementalSource="incrementalSource"
        :targetBuild="targetBuild"
        @update:isIncremental="isIncremental = $event"
      />
    </v-col>
    <v-divider vertical />
    <v-col
      cols="12"
      md="6"
      class="library"
    >
      <BuildLibrary
        :isIncremental="isIncremental"
        @update:incrementalSource="incrementalSource = $event"
        @update:targetBuild="targetBuild = $event"
        @update:targetDetails="targetDetails = $event"
      />
    </v-col>
  </v-row>
</template>

<script>
import OTAOptions from '@/components/OTAOptions.vue'
import BuildLibrary from '@/components/BuildLibrary.vue'

export default {
  components: {
    OTAOptions,
    BuildLibrary
  },
  data() {
    return {
      incrementalSource: '',
      targetBuild: '',
      targetDetails: [],
      isIncremental: false
    }
  },
}
</script>

<style scoped>
  .library {
    overflow: scroll;
    height:calc(100vh - 80px);
  }
</style>>
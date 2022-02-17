<template>
  <v-row>
    <v-col
      id="dynamic-component-demo"
      cols="12"
      md="6"
    >
      <button
        v-for="tab in tabs"
        :key="tab.label"
        :class="['tab-button', { active: currentTab === tab.component }]"
        @click="currentTab = tab.component"
      >
        {{ tab.label }}
      </button>
      <component
        :is="currentTab"
        class="tab-component"
        :targetDetails="targetDetails"
        @update:handler="setHandler"
      />
    </v-col>
    <v-divider vertical />
    <v-col
      cols="12"
      md="6"
      class="library"
    >
      <!-- the key-binding refresh has to be used to reload the methods-->
      <BuildLibrary
        :refresh="refresh"
        :isIncremental="checkIncremental"
        @update:incrementalSource="addIncrementalSource"
        @update:targetBuild="addTargetBuild"
        @update:targetDetails="targetDetails = $event"
      />
    </v-col>
  </v-row>
</template>

<script>
import SingleOTAOptions from '@/components/SingleOTAOptions.vue'
import BatchOTAOptions from '@/components/BatchOTAOptions.vue'
import ChainOTAOptions from '@/components/ChainOTAOptions.vue'
import BuildLibrary from '@/components/BuildLibrary.vue'

export default {
  components: {
    SingleOTAOptions,
    BatchOTAOptions,
    ChainOTAOptions,
    BuildLibrary,
  },
  data() {
    return {
      targetDetails: [],
      currentTab: 'SingleOTAOptions',
      refresh: false,
      tabs: [
        {label: 'Single OTA', component: 'SingleOTAOptions'},
        {label: 'Batch OTA', component: 'BatchOTAOptions'},
        {label: 'Chain OTA', component: 'ChainOTAOptions'}
      ],
    }
  },
  computed: {
    checkIncremental() {
      return this.$store.state.otaConfig.isIncremental
    },
  },
  methods: {
    setHandler(addIncrementalSource, addTargetBuild) {
      this.refresh = true,
      this.addIncrementalSource = addIncrementalSource
      this.addTargetBuild = addTargetBuild
      this.refresh = false
    },
    addIncrementalSource: () => {},
    addTargetBuild: () => {}
  }
}
</script>

<style scoped>
.library {
  overflow: scroll;
  height: calc(100vh - 80px);
}

.tab-component {
  border: 3px solid #eee;
  border-radius: 2px;
  padding: 20px;
}

.tab-button {
  padding: 6px 10px;
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
  border: 1px solid #ccc;
  cursor: pointer;
  background: #f0f0f0;
  margin-bottom: -1px;
  margin-right: -1px;
}
.tab-button:hover {
  background: #e0e0e0;
}
.tab-button.active {
  background: #e0e0e0;
}
.demo-tab {
  border: 1px solid #ccc;
  padding: 10px;
}
</style>>
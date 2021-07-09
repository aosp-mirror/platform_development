<template>
  <PartialCheckbox
    v-model="partitionInclude"
    :labels="updatePartitions"
  />
  <button @click="updateChart('blocks')">
    Analyse Installed Blocks (in target build)
  </button>
  <button @click="updateChart('payload')">
    Analyse Payload Composition
  </button>
  <button @click="updateChart('COWmerge')">
    Analyse COW Merge Operations
  </button>
  <BaseFile
    label="Select The Target Android Build"
    @file-select="selectBuild"
  />
  <button
    :disabled="!targetFile"
    @click="updateChart('extensions')"
  >
    Analyse File Extensions
  </button>
  <div v-if="echartsData">
    <PieChart :echartsData="echartsData" />
  </div>
</template>

<script>
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import PieChart from '@/components/PieChart.vue'
import BaseFile from '@/components/BaseFile.vue'
import { analysePartitions } from '../services/payload_composition.js'
import { chromeos_update_engine as update_metadata_pb } from '../services/update_metadata_pb.js'

export default {
  components: {
    PartialCheckbox,
    PieChart,
    BaseFile
  },
  props: {
    manifest: {
      type: update_metadata_pb.DeltaArchiveManifest,
      default: () => [],
    },
  },
  data() {
    return {
      partitionInclude: new Map(),
      echartsData: null,
      listData: '',
      targetFile: null
    }
  },
  computed: {
    updatePartitions() {
      return this.manifest.partitions.map((partition) => {
        return partition.partitionName
      })
    },
  },
  methods: {
    async updateChart(metrics) {
      let partitionSelected = this.manifest.partitions.filter((partition) =>
        this.partitionInclude.get(partition.partitionName)
      )
      try {
        this.echartsData = await analysePartitions(
          metrics,
          partitionSelected,
          this.manifest.blockSize,
          this.targetFile) }
      catch (err) {
        alert('Cannot be processed for the following issue: ', err)
      }
    },
    selectBuild(files) {
      //TODO(lishutong) check the version of target file is same to the OTA target
      this.targetFile = files[0]
    }
  },
}
</script>

<style scoped>
.list-data {
  text-align: center;
}
</style>
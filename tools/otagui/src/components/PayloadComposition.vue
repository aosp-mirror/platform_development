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
  <div v-if="echartsData">
    <PieChart :echartsData="echartsData" />
  </div>
</template>

<script>
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import PieChart from '@/components/PieChart.vue'
import { analysePartitions } from '../services/payload_composition.js'
import { chromeos_update_engine as update_metadata_pb } from '../services/update_metadata_pb.js'

export default {
  components: {
    PartialCheckbox,
    PieChart,
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
    updateChart(metrics) {
      let partitionSelected = this.manifest.partitions.filter((partition) =>
        this.partitionInclude.get(partition.partitionName)
      )
      this.echartsData = analysePartitions(
        metrics,
        partitionSelected,
        this.manifest.blockSize)
    },
  },
}
</script>

<style scoped>
.list-data {
  text-align: center;
}
</style>
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
  <div v-if="echartsData">
    <PieChart :echartsData="echartsData" />
  </div>
</template>

<script>
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import PieChart from '@/components/PieChart.vue'
import {
  operatedBlockStatistics,
  operatedPayloadStatistics,
} from '../services/payload_composition.js'
import { EchartsData } from '../services/echarts_data.js'
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
  mounted() {
    this.manifest.partitions.forEach((partition) => {
      this.partitionInclude.set(partition.partitionName, true)
    })
  },
  methods: {
    updateChart(metrics) {
      let partitionSelected = this.manifest.partitions.filter((partition) =>
        this.partitionInclude.get(partition.partitionName)
      )
      let statisticsData
      switch (metrics) {
      case 'blocks':
        statisticsData = operatedBlockStatistics(partitionSelected)
        this.echartsData = new EchartsData(
          statisticsData,
          'Operated blocks in target build'
        )
        break
      case 'payload':
        statisticsData = operatedPayloadStatistics(partitionSelected)
        this.echartsData = new EchartsData(
          statisticsData,
          'Payload disk usage'
        )
        break
      }
      this.listData = this.echartsData.listData()
    },
  },
}
</script>

<style scoped>
.list-data {
  text-align: center;
}
</style>
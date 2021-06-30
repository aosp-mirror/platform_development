<template>
  <PartialCheckbox
    v-model="partitionInclude"
    :labels="updatePartitions"
  />
  <button @click="updateChart">
    Update the chart
  </button>
  <div
    v-if="listData"
    class="list-data"
  >
    <pre>
      {{ listData }}
    </pre>
  </div>
</template>

<script>
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import { operatedBlockStatistics } from '../services/payload_composition.js'
import { EchartsData } from '../services/echarts_data.js'
import { chromeos_update_engine as update_metadata_pb } from '../services/update_metadata_pb.js'

export default {
  components: {
    PartialCheckbox,
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
    updateChart() {
      let partitionSelected = this.manifest.partitions.filter((partition) =>
        this.partitionInclude.get(partition.partitionName)
      )
      let statisticsData = operatedBlockStatistics(partitionSelected)
      this.echartsData = new EchartsData(statisticsData)
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
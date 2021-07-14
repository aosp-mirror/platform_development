<template>
  <PartialCheckbox
    v-model="partitionInclude"
    :labels="updatePartitions"
  />
  <div v-if="echartsData">
    <PieChart :echartsData="echartsData" />
  </div>
  <v-divider />
  <v-row>
    <v-col cols="6">
      <v-btn
        block
        @click="updateChart('blocks')"
      >
        Analyse Installed Blocks (in target build)
      </v-btn>
    </v-col>
    <v-col cols="6">
      <v-btn
        block
        @click="updateChart('payload')"
      >
        Analyse Payload Composition
      </v-btn>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="6">
      <v-btn
        block
        @click="updateChart('COWmerge')"
      >
        Analyse COW Merge Operations
      </v-btn>
    </v-col>
    <v-col cols="6">
      <v-btn
        block
        :disabled="!targetFile"
        @click="updateChart('extensions')"
      >
        Analyse File Extensions
      </v-btn>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="6" />
    <v-col cols="6">
      <BaseFile
        label="Drag and drop or Select The target Android build"
        @file-select="selectBuild"
      />
    </v-col>
  </v-row>
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
    BaseFile,
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
      targetFile: null,
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
          this.targetFile
        )
      } catch (err) {
        alert('Cannot be processed for the following issue: ', err)
      }
    },
    selectBuild(files) {
      //TODO(lishutong) check the version of target file is same to the OTA target
      this.targetFile = files[0]
    },
  },
}
</script>

<style scoped>
.list-data {
  text-align: center;
}
</style>
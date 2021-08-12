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
    <v-col
      cols="12"
      md="6"
    >
      <v-btn
        block
        @click="updateChart('blocks')"
      >
        Analyse Installed Blocks (in target build)
      </v-btn>
    </v-col>
    <v-col
      cols="12"
      md="6"
    >
      <v-btn
        block
        @click="updateChart('payload')"
      >
        Analyse Payload Composition
      </v-btn>
    </v-col>
  </v-row>
  <v-row>
    <v-col
      cols="12"
      md="6"
      class="tooltip"
    >
      <v-btn
        :disabled="manifest.nonAB"
        block
        @click="updateChart('COWmerge')"
      >
        Analyse COW Merge Operations
      </v-btn>
      <span
        v-if="manifest.nonAB"
        class="tooltiptext"
      >
        This function is only supported in A/B OTA
      </span>
    </v-col>
    <v-col
      cols="12"
      md="6"
    >
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
    <v-col
      cols="12"
      md="6"
    />
    <v-col
      cols="12"
      md="6"
    >
      <BaseFile
        v-if="!demo"
        label="Drag and drop or Select The target Android build"
        @file-select="selectBuild"
      />
    </v-col>
  </v-row>
</template>

<script>
import axios from 'axios'
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
    demo: {
      type: Boolean,
      default: false
    }
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
  async mounted() {
    if (this.demo) {
      try {
        const download = await axios.get(
          './files/cf_x86_target_file_demo.zip',
          {responseType: 'blob'}
        )
        this.targetFile = new File([download.data], 'target_demo.zip')
      } catch (err) {
        console.log('Please put a proper example target file in /public/files/')
      }
    }
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
.tooltip {
  position: relative;
  display: inline-block;
}

.tooltip .tooltiptext {
  visibility: hidden;
  width: 120px;
  background-color: black;
  color: #fff;
  text-align: center;
  border-radius: 6px;
  padding: 5px 0;
  position: absolute;
  z-index: 1;
}

.tooltip:hover .tooltiptext {
  visibility: visible;
}
</style>
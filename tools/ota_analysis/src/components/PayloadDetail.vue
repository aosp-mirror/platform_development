<template>
  <BasicInfo
    :zipFile="zipFile"
    :payload="payload"
    class="mb-5"
  />
  <v-divider />
  <div v-if="payload">
    <h3>Partition List</h3>
    <v-row
      v-if="payload.manifest"
      class="mb-5"
    >
      <v-col
        v-for="partition in payload.manifest.partitions"
        :key="partition.partitionName"
        cols="12"
        md="4"
      >
        <v-card
          elevation="5"
          hover
          shaped
          class="partial-info"
        >
          <PartitionDetail :partition="partition" />
        </v-card>
      </v-col>
    </v-row>
    <v-divider />
    <div
      v-if="payload.metadata_signature && !payload.manifest.nonAB"
      class="signature"
    >
      <h3>Metadata Signature</h3>
      <span style="white-space: pre-wrap">
        {{ octToHex(payload.metadata_signature.signatures[0].data) }}
      </span>
    </div>
  </div>
</template>

<script>
import PartitionDetail from './PartitionDetail.vue'
import BasicInfo from '@/components/BasicInfo.vue'
import { Payload, octToHex } from '@/services/payload.js'

export default {
  components: {
    PartitionDetail,
    BasicInfo,
  },
  props: {
    zipFile: {
      type: File,
      default: null,
    },
    payload: {
      type: Payload,
      default: null,
    },
  },
  methods: {
    octToHex: octToHex,
  },
}
</script>

<style scoped>
.signature {
  overflow: scroll;
  height: 200px;
  width: 100%;
  word-break: break-all;
  text-align: center;
}

.partial-info {
  padding: 5px;
}
</style>
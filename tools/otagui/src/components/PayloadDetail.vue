<template>
  <div v-if="zipFile">
    <h3>File infos</h3>
    <ul>
      <li>File name: {{ zipFile.name }}</li>
      <li>File size: {{ zipFile.size }} Bytes</li>
      <li>File last modified date: {{ zipFile.lastModifiedDate }}</li>
    </ul>
  </div>
  <div v-if="payload">
    <h3>Partition List</h3>
    <ul v-if="payload.manifest">
      <li
        v-for="partition in payload.manifest.partitions"
        :key="partition.partitionName"
      >
        <h4>{{ partition.partitionName }}</h4>
        <p v-if="partition.estimateCowSize">
          Estimate COW Size: {{ partition.estimateCowSize }} Bytes
        </p>
        <p v-else>
          Estimate COW Size: 0 Bytes
        </p>
        <PartitionDetail :partition="partition" />
      </li>
    </ul>
    <h3>Metadata Signature</h3>
    <div
      v-if="payload.metadata_signature"
      class="signature"
    >
      <span style="white-space: pre-wrap">
        {{ octToHex(payload.metadata_signature.signatures[0].data) }}
      </span>
    </div>
  </div>
</template>

<script>
import PartitionDetail from './PartitionDetail.vue'
import { Payload } from '@/services/payload.js'

export default {
  components: {
    PartitionDetail,
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

function octToHex(bufferArray) {
  let hex_table = ''
  for (let i = 0; i < bufferArray.length; i++) {
    hex_table += bufferArray[i].toString(16) + ' '
    if ((i + 1) % 16 == 0) {
      hex_table += '\n'
    }
  }
  return hex_table
}
</script>

<style scoped>
.signature {
  overflow: scroll;
  height: 200px;
  width: 100%;
  word-break: break-all;
}
</style>
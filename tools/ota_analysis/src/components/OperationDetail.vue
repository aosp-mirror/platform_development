<template>
  {{ mapType.get(operation.type) }}
  <p v-if="operation.hasOwnProperty('dataOffset')">
    Data offset: {{ operation.dataOffset }}
  </p>
  <p v-if="operation.hasOwnProperty('dataLength')">
    Data length: {{ operation.dataLength }}
  </p>
  <p v-if="operation.hasOwnProperty('srcExtents')">
    Source: {{ operation.srcExtents.length }} extents ({{ srcTotalBlocks }}
    blocks)
    <br>
    {{ srcBlocks }}
  </p>
  <p v-if="operation.hasOwnProperty('dstExtents')">
    Destination: {{ operation.dstExtents.length }} extents ({{ dstTotalBlocks }}
    blocks)
    <br>
    {{ dstBlocks }}
  </p>
</template>

<script>
import { numBlocks, displayBlocks } from '../services/payload_composition.js'

export default {
  props: {
    operation: {
      type: Object,
      required: true,
    },
    mapType: {
      type: Map,
      required: true,
    },
  },
  data() {
    return {
      srcTotalBlocks: null,
      srcBlocks: null,
      dstTotalBlocks: null,
      dstBlocks: null,
    }
  },
  mounted() {
    if (this.operation.srcExtents) {
      this.srcTotalBlocks = numBlocks(this.operation.srcExtents)
      this.srcBlocks = displayBlocks(this.operation.srcExtents)
    }
    if (this.operation.dstExtents) {
      this.dstTotalBlocks = numBlocks(this.operation.dstExtents)
      this.dstBlocks = displayBlocks(this.operation.dstExtents)
    }
  },
}
</script>
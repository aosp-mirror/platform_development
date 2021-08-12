<template>
  <ul>
    <h5> {{ mapType.getWithDefault(operation.type) }} </h5>
    <li v-if="operation.hasOwnProperty('dataOffset')">
      <strong> Data offset: </strong> {{ operation.dataOffset }}
    </li>
    <li v-if="operation.hasOwnProperty('dataLength')">
      <strong> Data length: </strong> {{ operation.dataLength }}
    </li>
    <li v-if="operation.hasOwnProperty('srcExtents')">
      <strong> Source: </strong> {{ operation.srcExtents.length }} extents ({{ srcTotalBlocks }}
      blocks)
      <br>
      {{ srcBlocks }}
    </li>
    <li v-if="operation.hasOwnProperty('dstExtents')">
      <strong> Destination: </strong> {{ operation.dstExtents.length }} extents ({{ dstTotalBlocks }}
      blocks)
      <br>
      {{ dstBlocks }}
    </li>
  </ul>
  <v-divider />
</template>

<script>
import { numBlocks, displayBlocks } from '../services/payload_composition.js'
import { DefaultMap } from '../services/payload.js'

export default {
  props: {
    operation: {
      type: Object,
      required: true,
    },
    mapType: {
      type: DefaultMap,
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

<style scoped>
ul {
  padding: 5px;
}

li {
  color: black;
  list-style-type: none;
}

</style>
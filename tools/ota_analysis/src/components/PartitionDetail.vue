<template>
  <h4> {{ partition.partitionName }} </h4>
  <p v-if="partition.estimateCowSize">
    <strong> Estimate COW Size: </strong> {{ partition.estimateCowSize }} Bytes
  </p>
  <div
    class="toggle"
    @click="toggle('showInfo')"
  >
    <h4> Partition Infos </h4>
    <ul v-if="showInfo">
      <li v-if="partition.oldPartitionInfo">
        <strong>
          Old Partition Size:
        </strong>
        {{ partition.oldPartitionInfo.size }} Bytes
      </li>
      <li v-if="partition.oldPartitionInfo">
        <strong>
          Old Partition Hash:
        </strong>
        {{ octToHex(partition.oldPartitionInfo.hash, false, 16) }}
      </li>
      <li>
        <strong>
          New Partition Size:
        </strong>
        {{ partition.newPartitionInfo.size }} Bytes
      </li>
      <li v-if="partition.newPartitionInfo.hash">
        <strong>
          New Partition Hash:
        </strong>
        {{ octToHex(partition.newPartitionInfo.hash, false, 16) }}
      </li>
    </ul>
  </div>
  <div
    class="toggle"
    @click="toggle('showOPs')"
  >
    <h4> Total Operations: {{ partition.operations.length }} </h4>
    <ul
      v-if="showOPs"
    >
      <li
        v-for="operation in partition.operations"
        :key="operation.dataSha256Hash"
      >
        <OperationDetail
          :operation="operation"
          :mapType="opType.mapType"
        />
      </li>
    </ul>
  </div>
</template>

<script>
import { OpType, octToHex } from '@/services/payload.js'
import OperationDetail from '@/components/OperationDetail.vue'

export default {
  components: {
    OperationDetail,
  },
  props: {
    partition: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      showOPs: false,
      showInfo: false,
      opType: null,
    }
  },
  created() {
    this.opType = new OpType()
  },
  methods: {
    toggle(key) {
      this[key] = !this[key]
    },
    octToHex: octToHex,
  },
}
</script>

<style scoped>
.toggle {
  display: block;
  cursor: pointer;
  color: #762ace;
}

li {
  list-style-type: none;
}
</style>
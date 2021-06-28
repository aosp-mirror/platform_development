<template>
  <p
    class="toggle"
    @click="toggle()"
  >
    Total Operations: {{ partition.operations.length }}
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
  </p>
</template>

<script>
import { OpType } from '@/services/payload.js'
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
      opType: null,
    }
  },
  created() {
    this.opType = new OpType()
  },
  methods: {
    toggle() {
      this.showOPs = !this.showOPs
    },
  },
}
</script>

<style scoped>
.toggle {
  display: block;
  cursor: pointer;
  color: #00c255;
}
</style>
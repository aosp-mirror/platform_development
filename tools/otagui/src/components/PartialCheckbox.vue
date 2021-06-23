<template>
  <ul v-bind="$attrs">
    <li
      v-for="label in labels"
      :key="label"
    >
      <input
        type="checkbox"
        :value="label"
        :checked="modelValue.get(label)"
        @change="updateSelected($event.target.value)"
      >
      <label v-if="label"> {{ label }} </label>
    </li>
  </ul>
</template>

<script>
export default {
  props: {
    labels: {
      type: Array,
      default: new Array(),
    },
    modelValue: {
      type: Map,
      default: new Map(),
    },
  },
  methods: {
    updateSelected(newSelect) {
      this.modelValue.set(newSelect, !this.modelValue.get(newSelect))
      this.$emit('update:modelValue', this.modelValue)
    },
  },
}
</script>
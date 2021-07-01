<template>
  <ul v-bind="$attrs">
    <button
      type="button"
      @click="revertAllSelection"
      v-text="selectAllText[selectAll]"
    />
    <br>
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
  data() {
    return {
      selectAll: 1,
      selectAllText: ['Select All', 'Unselect All'],
    }
  },
  mounted() {
    // Set the default value to be true once mounted
    for (let key of this.labels) {
      this.modelValue.set(key, true)
    }
  },
  methods: {
    updateSelected(newSelect) {
      this.modelValue.set(newSelect, !this.modelValue.get(newSelect))
      this.$emit('update:modelValue', this.modelValue)
    },
    revertAllSelection() {
      this.selectAll = 1 - this.selectAll
      for (let key of this.modelValue.keys()) {
        this.modelValue.set(key, Boolean(this.selectAll))
      }
    },
  },
}
</script>

<style scoped>
ul > li {
  display: inline-block;
  list-style-type: none;
  margin-left: 5%;
  margin-right: 5%;
  top: 0px;
  height: 50px;
}
</style>
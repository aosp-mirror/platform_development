<template>
  <label
    v-if="label"
    class="checkbox"
  >
    <input
      type="checkbox"
      :checked="modelValue"
      class="field"
      v-bind="$attrs"
      @change="$emit('update:modelValue', $event.target.checked)"
    >
    {{ label }}
  </label>
</template>

<script>
export default {
  props: {
    label: {
      type: String,
      default: '',
    },
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
}
</script>

<style scoped>
.checkbox {
  display: block;
  position: relative;
  padding-left: 35px;
  margin-bottom: 12px;
  cursor: pointer;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  transition: all 0.2s ease;
}
</style>
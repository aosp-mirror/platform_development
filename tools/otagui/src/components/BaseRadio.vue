<template>
  <input
    type="radio"
    :checked="modelValue === value"
    :value="value"
    @change="$emit('update:modelValue', value)"
  >
  <label>{{ label }}</label>
</template>

<script>
export default {
  props: {
    label: {
      type: String,
      default: ''
    },
    modelValue: {
      type: [String, Number],
      default: ''
    },
    value: {
      type: [String, Number],
      required: true
    },
    name: {
      type: String,
      required: true
    }
  },
}
</script>
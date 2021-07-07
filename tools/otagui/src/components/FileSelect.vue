<template>
  <label v-if="label"> {{ label }} </label>
  <select
    :value="modelValue"
    class="field"
    v-bind="$attrs"
    @change="$emit('update:modelValue', $event.target.value)"
  >
    <option
      v-for="option in options"
      :key="option.file_name"
      :value="option.path"
      :selected="option.path === modelValue"
    >
      {{ option.file_name }}
    </option>
  </select>
</template>



<script>
export default {
  props: {
    label: {
      type: String,
      default: '',
    },
    modelValue: {
      type: [String, Number],
      default: ''
    },
    options: {
      type: Array,
      required: true
    }
  }
}
</script>
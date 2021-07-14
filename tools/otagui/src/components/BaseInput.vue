<template>
  <h3> {{ label }} </h3>
  <br>
  <input
    v-bind="$attrs"
    :placeholder="label"
    class="field"
    :value="modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
  >
</template>

<script>
export default{
  props:{
    label:{
      type: String,
      default: ''
    },
    modelValue:{
      type: [String, Number],
      default: ''
    }
  }
}
</script>

<style scoped>
input {
  width: 100%;
  background: #f2f2f2;
}
</style>
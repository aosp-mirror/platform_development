<template>
  <div class="flat-card">
    <slot />
  </div>
</template>
<script>
export default {
  name: 'FlatCard',
}
</script>
<style scoped>
.flat-card {
  word-break: normal;
  tab-size: 4;
  font-size: 14px;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -webkit-tap-highlight-color: rgba(0,0,0,0);
  font-family: Roboto, sans-serif;
  line-height: 1.5;
  background-repeat: no-repeat;
  box-sizing: inherit;
  padding: 0;
  margin: 0;
  display: block;
  max-width: 100%;
  outline: none;
  text-decoration: none;
  transition-property: box-shadow,opacity;
  overflow-wrap: break-word;
  position: relative;
  white-space: normal;
  border: thin solid rgba(0,0,0,.12);
  background-color: #fff;
  color: rgba(0,0,0,.87);
  border-radius: 4px;
}
</style>
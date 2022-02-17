<template>
  <vue-context ref="menu">

  </vue-context>
</template>

<script>
import VueContext from 'vue-context';

export default {
  name: 'NodeContextMenu',
  components: {
    VueContext,
  },
  methods: {
    open(e) {
      this.$refs.menu.open(e);
    },
    close() {
      this.$refs.menu.close();
    },
  },
};
</script>
<style scoped>
.v-context,
.v-context ul {
  background-color: #fff;
  background-clip: padding-box;
  border-radius: .25rem;
  border: 1px solid rgba(0, 0, 0, .15);
  box-shadow:
    0 2px 2px 0 rgba(0, 0, 0, .14),
    0 3px 1px -2px rgba(0, 0, 0, .2),
    0 1px 5px 0 rgba(0, 0, 0, .12);
  display: block;
  margin: 0;
  padding: 10px 0;
  min-width: 10rem;
  z-index: 10;
  position: fixed;
  list-style: none;
  box-sizing: border-box;
  max-height: calc(100% - 50px);
  overflow-y: auto
}

.v-context>li,
.v-context ul>li {
  margin: 0;
  position: relative
}

.v-context>li>a,
.v-context ul>li>a {
  display: block;
  padding: .5rem 1.5rem;
  font-weight: 400;
  color: #212529;
  text-decoration: none;
  white-space: nowrap;
  background-color: transparent;
  border: 0
}

.v-context>li>a:focus,
.v-context>li>a:hover,
.v-context ul>li>a:focus,
.v-context ul>li>a:hover {
  text-decoration: none;
  color: #212529;
  background-color: #f8f9fa
}

.v-context:focus,
.v-context>li>a:focus,
.v-context ul:focus,
.v-context ul>li>a:focus {
  outline: 0
}

.v-context__sub>a:after {
  content: "\2BC8";
  float: right;
  padding-left: 1rem
}

.v-context__sub>ul {
  display: none
}
</style>

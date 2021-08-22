<template>
  <v-app>
    <v-app-bar
      rounded
      color="primary"
    >
      <v-app-bar-title> OTA Analyzer </v-app-bar-title>
      <v-spacer />
      <v-btn
        v-for="link in links"
        :key="`${link}-header-link`"
        :to="{ name: link }"
        class="ml-5"
        color="primary"
      >
        {{ link }}
      </v-btn>
    </v-app-bar>
    <v-main>
      <v-container fluid>
        <router-view />
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      links: ['Analysis', 'Demo', 'About'],
    }
  },
}
</script>

<style>
h3 {
  text-align: center;
}
</style>
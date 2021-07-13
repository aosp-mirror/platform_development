<template>
  <div id="app">
    <div id="nav">
      <router-link :to="{ name: 'Create' }">
        Create Jobs
      </router-link> |
      <router-link :to="{ name: 'JobList' }">
        Jobs Status
      </router-link> |
      <router-link :to="{ name: 'About' }">
        About
      </router-link>
    </div>
    <router-view />
  </div>
</template>
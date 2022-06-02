<template>
  <div class="beta-options md-toolbar md-transparent md-dense">
    <div>
      <h2 class="heading md-title">Beta Features</h2>
      <md-checkbox
          v-model="betaImePanel">
        IME Panel with WM & SF Properties
      </md-checkbox>
    </div>
    <div class="description">
      These features are still in beta and you may experience
      some issues with them. Please select / unselect the checkbox(es)
      BEFORE capturing with ADB or uploading your files.
    </div>
  </div>
</template>

<script>
export default {
  name: 'BetaFeaturesToolbar.vue',
  props: ['setBetaImePanelFlag'],
  data() {
    return {
      betaImePanel: false,
    };
  },
  created() {
    // reset store's flag for use case where user clicks 'Clear'
    this.setBetaImePanelFlag(this.betaImePanel);
  },
  watch: {
    betaImePanel() {
      // this.$store.commit('setBetaImePanelFlag', this.betaImePanel());
      // this.store.betaFeatures.newImePanels = this.betaImePanel();
      this.setBetaImePanelFlag(this.betaImePanel);
    },
  },
};
</script>

<style scoped>
.beta-options {
  border-bottom: 1px solid rgba(0, 0, 0, .12);
  flex-direction: column !important;
  align-content: flex-start !important;
  align-items: flex-start !important;
}

.beta-options .heading {
  padding-right: 15px;
  border-right: 1px solid rgba(0, 0, 0, 0.12);
}

.beta-options .md-checkbox {
  margin: 4px 10px;
  padding-bottom: 7px;
}

.beta-options .description {
  margin: 6px 8px 12px 8px;
}
</style>

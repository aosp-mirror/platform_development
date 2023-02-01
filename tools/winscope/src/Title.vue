<script>
export default {
  name: 'vue-title',
  props: ['appName', 'traceName'],
  watch: {
    traceName: {
      immediate: true,
      handler() {
        this.updatePageTitle();
      }
    },
    appName: {
      immediate: true,
      handler() {
        this.updatePageTitle();
      }
    }
  },
  methods: {
    updatePageTitle() {
      if (this.traceName == null || this.traceName == "") {
        document.title = this.appName;
      } else {
        document.title = this.traceName;
      }
    }
  },
  render () {
  },
}
</script>

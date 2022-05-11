<template>
  <form @submit.prevent="sendForm">
    <FileList
      v-model="targetBuilds"
      label="Target files"
      :movable="true"
    />
    <v-divider />
    <OTAOptions
      :targetDetails="targetDetails"
      :targetBuilds="targetBuilds"
    />
    <v-divider class="my-5" />
    <v-btn
      block
      type="submit"
    >
      Submit
    </v-btn>
  </form>
</template>

<script>
import OTAOptions from '@/components/OTAOptions.vue'
import FileList from '@/components/FileList.vue'

export default {
  components: {
    OTAOptions,
    FileList,
  },
  props: {
    targetDetails: {
      type: Array,
      default: () => [],
    }
  },
  data() {
    return {
    }
  },
  computed: {
    checkIncremental() {
      return this.$store.state.otaConfig.isIncremental
    },
    targetBuilds: {
      get() {
        return this.$store.state.targetBuilds
      },
      set(target) {
        this.$store.commit('SET_TARGETS', target)
      }
    }
  },
  watch: {
    checkIncremental: {
      handler() {
        // The isIncremental flag has to be set false all the time
        this.$store.commit('SET_ISINCREMENTAL', false)
        this.$store.commit('SET_SOURCES', [])
      }
    }
  },
  created() {
    this.$store.commit('SET_ISINCREMENTAL', false)
    this.$store.commit('SET_SOURCES', [])
    this.$emit('update:handler', this.addIncrementalSources, this.addTargetBuilds)
  },
  methods: {
    /**
     * Send the configuration to the backend.
     */
    async sendForm() {
      if (this.targetBuilds.length<2) {
        alert(
          'At least two OTA packeges has to be given!'
        );
        return
      }
      try {
        let response_data = await this.$store.state.otaConfig
          .sendChainForms(this.targetBuilds)
        let response_messages = response_data.map(d => d.msg);
        alert(response_messages.join('\n'))
        this.$store.state.otaConfig.reset()
        this.$store.commit('SET_TARGETS', [])
      } catch (err) {
        alert(
          'Job cannot be started properly for the following reasons: ' + err
        )
      }
    },
    addIncrementalSources (build) {},
    addTargetBuilds (build) {
      if (!this.targetBuilds.includes(build)) {
        this.targetBuilds.push(build)
      }
    }
  },
}
</script>
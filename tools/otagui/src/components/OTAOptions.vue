<template>
  <v-row>
    <v-col
      v-for="flag in basicFlags"
      :key="flag.key"
      cols="12"
      md="4"
    >
      <BaseCheckbox
        v-model="otaConfig[flag.key]"
        :label="flag.label"
      />
    </v-col>
  </v-row>
  <div v-if="otaConfig.isPartial">
    <v-divider />
    <h3>Select Partitions</h3>
    <PartialCheckbox
      v-model="otaConfig.partial"
      :labels="updatablePartitions"
    />
    <v-divider />
  </div>
  <v-btn
    block
    @click="moreOptions = !moreOptions"
  >
    More Options
  </v-btn>
  <v-row v-if="moreOptions">
    <v-col
      v-for="flag in extraFlags"
      :key="flag.key"
      cols="12"
      md="4"
    >
      <BaseCheckbox
        v-model="otaConfig[flag.key]"
        :label="flag.label"
      />
    </v-col>
  </v-row>
  <v-divider class="my-5" />
  <BaseInput
    v-model="otaConfig.extra"
    :label="'Extra Configurations'"
  />
</template>

<script>
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import PartialCheckbox from '@/components/PartialCheckbox.vue'
import { OTAConfiguration, OTABasicFlags, OTAExtraFlags } from '@/services/JobSubmission.js'

export default {
  components: {
    BaseInput,
    BaseCheckbox,
    PartialCheckbox,
  },
  props: {
    targetDetails: {
      type: Array,
      default: () => [],
    },
    targetBuilds: {
      type: Array,
      default: () => []
    }
  },
  data () {
    return {
      otaConfig: new OTAConfiguration(),
      moreOptions: false,
      basicFlags: OTABasicFlags,
      extraFlags: OTAExtraFlags
    }
  },
  computed: {
    /**
     * Return the partition list of the selected target build.
     * @return Array<String>
     */
    updatablePartitions() {
      // TODO(lishutong): currently only give the partition list of first
      // build, should use the intersections of the partitions of
      // all given builds.
      if (!this.targetBuilds | this.targetBuilds.length===0) return []
      if (this.targetBuilds[0] === '') return []
      let target = this.targetDetails.filter(
        (d) => d.path === this.targetBuilds[0]
      )
      return target[0].partitions
    },
  },
  watch: {
    otaConfig: {
      handler () {
        this.$emit('update:otaConfig', this.otaConfig)
      },
      deep: true
    }
  }
}
</script>
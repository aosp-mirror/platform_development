<template>
  <v-row>
    <v-col
      cols="12"
      md="6"
    >
      <BaseFile
        label="Please drag and drop an OTA package or Select one"
        @file-select="unpackOTA"
      />
      <PayloadDetail
        v-if="zipFile && payload"
        :zipFile="zipFile"
        :payload="payload"
      />
    </v-col>
    <v-divider
      vertical
    />
    <v-col
      cols="12"
      md="6"
    >
      <PayloadComposition
        v-if="zipFile && payload.manifest"
        :manifest="payload.manifest"
      />
    </v-col>
  </v-row>
</template>

<script>
import BaseFile from '@/components/BaseFile.vue'
import PayloadDetail from '@/components/PayloadDetail.vue'
import PayloadComposition from '@/components/PayloadComposition.vue'
import { Payload } from '@/services/payload.js'

export default {
  components: {
    BaseFile,
    PayloadDetail,
    PayloadComposition,
  },
  data() {
    return {
      zipFile: null,
      payload: null,
    }
  },
  methods: {
    async unpackOTA(files) {
      this.zipFile = files[0]
      try {
        this.payload = new Payload(this.zipFile)
        await this.payload.init()
      } catch (err) {
        alert('Please check if this is a correct OTA package (.zip).')
        console.log(err)
      }
    },
  },
}
</script>

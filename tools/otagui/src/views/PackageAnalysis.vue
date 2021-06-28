<template>
  <div>
    <BaseFile
      label="Select an OTA package"
      @file-select="unpackOTA"
    />
    <PayloadDetail
      v-if="zipFile && payload"
      :zipFile="zipFile"
      :payload="payload"
    />
  </div>
</template>

<script>
import BaseFile from '@/components/BaseFile.vue'
import PayloadDetail from '@/components/PayloadDetail.vue'
import { Payload } from '@/services/payload.js'

export default {
  components: {
    BaseFile,
    PayloadDetail,
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

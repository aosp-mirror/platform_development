<template>
  <v-row>
    <v-col
      cols="12"
      md="6"
    >
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
        :demo="true"
      />
    </v-col>
  </v-row>
</template>

<script>
import axios from 'axios'
import PayloadDetail from '@/components/PayloadDetail.vue'
import PayloadComposition from '@/components/PayloadComposition.vue'
import { Payload } from '@/services/payload.js'

export default {
  components: {
    PayloadDetail,
    PayloadComposition,
  },
  data() {
    return {
      zipFile: null,
      payload: null,
    }
  },
  async created() {
    // put cf_x86_demo.zip and cf_x86_target_file_demo into
    // this directory: /public/files
    try {
      const download = await axios.get(
        './files/cf_x86_demo.zip',
        {responseType: 'blob'}
      )
      this.zipFile = new File([download.data], 'ota_demo.zip')
      this.payload = new Payload(this.zipFile)
      await this.payload.init()
    } catch (err) {
      console.log('Please put a proper example OTA in /public/files/')
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

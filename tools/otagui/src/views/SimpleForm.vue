<template>
  <div>
    <form @submit.prevent="sendForm">
      <BaseInput
        v-model="input.incremental"
        :disabled="!input.incrementalStatus"
        :label="'Source Package Path'"
        type="text"
      />

      <BaseInput
        v-model="input.target"
        label="Target File path"
        type="text"
      />

      <BaseCheckbox
        v-model="input.verbose"
        :label="'Verbose'"
      />


      <BaseCheckbox
        v-model="input.incrementalStatus"
        :label="'Incremental'"
      />

      <BaseInput
        v-model="input.output"
        label="Output File path"
        type="text"
      />

      <button type="submit">
        Submit
      </button>
    </form>

    <pre> {{ input }} </pre>

    <h3> Response from the server </h3>
    <div> {{ response_message }}</div>
  </div>
</template>

<script>
import BaseInput from '@/components/BaseInput.vue'
import BaseCheckbox from '@/components/BaseCheckbox.vue'
import ApiService from '../services/ApiService.js'

export default {
  components: {
    BaseInput,
    BaseCheckbox
  },
  data() {
    return {
      id: 0,
      input: {
        verbose: false,
        target: '',
        output: '',
        incremental: '',
        incrementalStatus: false
      },
      inputs: [],
      response_message : ''
    }
  },
  methods:{
    sendForm(e) {
      ApiService.postInput(this.input, this.id)
        .then(Response => {
          this.response_message = Response.data
        })
        .catch(err => {
          this.response_message = 'Error! ' + err
        })
      this.input = {
        verbose: false,
        target: '',
        output: '',
        incremental: '',
        incrementalStatus: false
      }
    }
  },
}
</script>
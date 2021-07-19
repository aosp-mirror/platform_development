/**
 * @fileoverview Class OTAInput is used to configure and create a process in
 * the backend to to start OTA package generation.
 * @package vue-uuid
 * @package ApiServices
 */
import { uuid } from 'vue-uuid'
import ApiServices from './ApiService.js'

export class OTAConfiguration {
  /**
   * Initialize the input for the api /run/<id>
   */
  constructor() {
    this.verbose = false,
    this.target = '',
    this.incremental = '',
    this.isIncremental = false,
    this.partial = [],
    this.isPartial = false,
    this.extra = '',
    this.id = uuid.v1()
  }

  /**
   * Start the generation process, will throw an error if not succeed
   */
  async sendForm() {
    try {
      let response = await ApiServices.postInput(JSON.stringify(this), this.id)
      return response.data
    } catch (err) {
      throw err
    }
  }
}
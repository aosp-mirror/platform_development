import axios from 'axios'

const  apiClient = axios.create({
  baseURL: 'http://localhost:8000',
  withCredentials: false,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
})

export default {
  getJobs() {
    return apiClient.get("/check")
  },
  async postInput(input, id) {
    try {
      const response = await apiClient.post(
        '/run/' + id, input)
      console.log('Response:', response)
      return response
    } catch (err) {
      console.log('err:', err)
      return
    }
  }
}

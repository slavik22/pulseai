import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '',
  timeout: 60_000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data?.detail) {
      return Promise.reject(error.response.data)
    }
    return Promise.reject({
      status: 0,
      detail: 'Network error. Please check your connection.',
    })
  }
)

// backward compat aliases
export const ticketClient = apiClient
export const aiClient = apiClient

import { api } from './api'
import { mockGiftRecommendations, delay } from './mockData'

// Check if we should use demo mode (backend not available or 403)
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true'

export const giftsService = {
  async searchGifts(query, options = {}) {
    try {
      return await api.post('/api/gifts/search', { query, ...options })
    } catch (error) {
      // Fallback to demo mode if backend returns 403 or is unavailable
      if (error.status === 403 || error.message.includes('Failed to fetch')) {
        console.warn('⚠️ Backend no disponible, usando modo DEMO')
        await delay(1000) // Simulate API delay
        return mockGiftRecommendations
      }
      throw error
    }
  },

  async getGiftById(id) {
    return await api.get(`/api/gifts/${id}`)
  },

  async getSimilarGifts(id) {
    return await api.get(`/api/gifts/${id}/similar`)
  }
}

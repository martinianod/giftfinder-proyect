import { api } from './api'

export const authService = {
  async login(username, password) {
    const data = await api.post('/api/auth/login', { username, password })
    if (data.token) {
      localStorage.setItem('auth-token', data.token)
    }
    return data
  },

  async signup(email, username, password) {
    const data = await api.post('/api/auth/signup', { email, username, password })
    if (data.token) {
      localStorage.setItem('auth-token', data.token)
    }
    return data
  },

  async forgotPassword(email) {
    const data = await api.post('/api/auth/forgot-password', { email })
    return data
  },

  async resetPassword(token, newPassword) {
    const data = await api.post('/api/auth/reset-password', { token, newPassword })
    return data
  },

  async logout() {
    localStorage.removeItem('auth-token')
  },

  getToken() {
    return localStorage.getItem('auth-token')
  },

  isAuthenticated() {
    return !!this.getToken()
  }
}

// Use relative URL in development (Vite proxy) or env variable in production
// Usa la URL del backend por env o default a localhost:8080
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

class ApiError extends Error {
  constructor(message, status, code, details) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

class ApiClient {
  constructor(baseURL) {
    this.baseURL = baseURL
  }

  async request(endpoint, options = {}) {
    const {
      method = 'GET',
      body,
      headers = {},
      retries = 3,
      timeout = 10000,
      ...fetchOptions
    } = options

    const url = `${this.baseURL}${endpoint}`
    
    for (let attempt = 0; attempt < retries; attempt++) {
      try {
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), timeout)

        const response = await fetch(url, {
          method,
          headers: {
            'Content-Type': 'application/json',
            ...this.getAuthHeaders(),
            ...headers
          },
          body: body ? JSON.stringify(body) : undefined,
          signal: controller.signal,
          ...fetchOptions
        })

        clearTimeout(timeoutId)

        if (!response.ok) {
          // Handle 401 Unauthorized - show login modal instead of browser auth
          if (response.status === 401) {
            // Dynamically import to avoid circular dependency
            const { useAuthStore } = await import('../store/authStore')
            const { setShowLoginModal, setPendingRequest } = useAuthStore.getState()
            
            // Create a promise that resolves when user logs in
            return new Promise((resolve, reject) => {
              // Set the pending request to retry after login
              setPendingRequest(async () => {
                try {
                  // Retry the original request
                  const retryResponse = await this.request(endpoint, options)
                  resolve(retryResponse)
                } catch (error) {
                  reject(error)
                }
              })
              
              // Show the login modal
              setShowLoginModal(true)
            })
          }

          const error = await response.json().catch(() => ({}))
          throw new ApiError(
            error.message || `HTTP ${response.status}`,
            response.status,
            error.code,
            error.details
          )
        }

        return await response.json()
      } catch (error) {
        // Don't retry on client errors (4xx) except 401 which is handled above
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          throw error
        }

        // Retry on network errors or 5xx
        if (attempt === retries - 1) {
          throw error
        }

        // Exponential backoff
        await new Promise(resolve =>
          setTimeout(resolve, Math.min(1000 * 2 ** attempt, 5000))
        )
      }
    }
  }

  getAuthHeaders() {
    const token = localStorage.getItem('auth-token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  get(endpoint, options) {
    return this.request(endpoint, { ...options, method: 'GET' })
  }

  post(endpoint, body, options) {
    return this.request(endpoint, { ...options, method: 'POST', body })
  }

  put(endpoint, body, options) {
    return this.request(endpoint, { ...options, method: 'PUT', body })
  }

  delete(endpoint, options) {
    return this.request(endpoint, { ...options, method: 'DELETE' })
  }
}

export const api = new ApiClient(API_URL)
export { ApiError }

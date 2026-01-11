import { create } from 'zustand'
import { persist, devtools } from 'zustand/middleware'
import { authService } from '../services/auth.service'

export const useAuthStore = create(
  devtools(
    persist(
      (set, get) => ({
        // State
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
        showLoginModal: false,
        authView: 'login', // 'login', 'signup', 'forgot-password'
        pendingRequest: null, // Store request to retry after login

        // Actions
        setShowLoginModal: (show) => set({ showLoginModal: show, error: null, authView: 'login' }),

        setAuthView: (view) => set({ authView: view, error: null }),

        setPendingRequest: (request) => set({ pendingRequest: request }),

        login: async (username, password) => {
          set({ isLoading: true, error: null })
          try {
            const data = await authService.login(username, password)
            set({
              user: data.user,
              token: data.token,
              isAuthenticated: true,
              isLoading: false,
              showLoginModal: false
            })

            // Retry pending request if any
            const { pendingRequest } = get()
            if (pendingRequest) {
              set({ pendingRequest: null })
              return pendingRequest()
            }

            return data
          } catch (error) {
            set({ error: error.message, isLoading: false })
            throw error
          }
        },

        signup: async (email, username, password) => {
          set({ isLoading: true, error: null })
          try {
            const data = await authService.signup(email, username, password)
            set({
              user: data.user,
              token: data.token,
              isAuthenticated: true,
              isLoading: false,
              showLoginModal: false
            })
            return data
          } catch (error) {
            set({ error: error.message, isLoading: false })
            throw error
          }
        },

        logout: async () => {
          await authService.logout()
          set({
            user: null,
            token: null,
            isAuthenticated: false,
            error: null,
            showLoginModal: false,
            pendingRequest: null
          })
        },

        clearError: () => set({ error: null })
      }),
      {
        name: 'auth-storage',
        partialize: (state) => ({
          user: state.user,
          token: state.token,
          isAuthenticated: state.isAuthenticated
        })
      }
    ),
    { name: 'AuthStore' }
  )
)

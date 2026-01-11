import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import { giftsService } from '../services/gifts.service'

export const useGiftsStore = create(
  devtools(
    (set, get) => ({
      // State
      query: '',
      results: null,
      isSearching: false,
      error: null,
      searchHistory: [],

      // Actions
      setQuery: (query) => set({ query }),

      searchGifts: async (query) => {
        set({ isSearching: true, error: null })
        try {
          const data = await giftsService.searchGifts(query)
          set({
            results: data,
            isSearching: false,
            searchHistory: [
              { query, timestamp: Date.now() },
              ...get().searchHistory.slice(0, 9) // Keep last 10
            ]
          })
          return data
        } catch (error) {
          set({ error: error.message, isSearching: false })
          throw error
        }
      },

      clearResults: () => set({ results: null, query: '' }),
      clearError: () => set({ error: null })
    }),
    { name: 'GiftsStore' }
  )
)

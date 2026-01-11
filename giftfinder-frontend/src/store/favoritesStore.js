import { create } from 'zustand'
import { persist, devtools } from 'zustand/middleware'

export const useFavoritesStore = create(
  devtools(
    persist(
      (set, get) => ({
        // State
        favorites: [],
        comparison: [],

        // Favorites actions
        addFavorite: (gift) => {
          const { favorites } = get()
          if (!favorites.find(f => f.id === gift.id)) {
            set({ favorites: [...favorites, { ...gift, addedAt: Date.now() }] })
          }
        },

        removeFavorite: (giftId) => {
          set({ favorites: get().favorites.filter(f => f.id !== giftId) })
        },

        toggleFavorite: (gift) => {
          const { favorites } = get()
          const exists = favorites.find(f => f.id === gift.id)
          if (exists) {
            get().removeFavorite(gift.id)
          } else {
            get().addFavorite(gift)
          }
        },

        isFavorite: (giftId) => {
          return get().favorites.some(f => f.id === giftId)
        },

        clearFavorites: () => set({ favorites: [] }),

        // Comparison actions
        addToComparison: (gift) => {
          const { comparison } = get()
          if (comparison.length < 4 && !comparison.find(g => g.id === gift.id)) {
            set({ comparison: [...comparison, gift] })
          }
        },

        removeFromComparison: (giftId) => {
          set({ comparison: get().comparison.filter(g => g.id !== giftId) })
        },

        clearComparison: () => set({ comparison: [] })
      }),
      {
        name: 'favorites-storage'
      }
    ),
    { name: 'FavoritesStore' }
  )
)

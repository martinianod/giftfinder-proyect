import { useState } from 'react'
import { useFavoritesStore } from '../../../store/favoritesStore'
import Button from '../../ui/Button'
import styles from './GiftCard.module.css'

export default function GiftCard({ gift }) {
  const [imageLoaded, setImageLoaded] = useState(false)
  const { 
    isFavorite, 
    toggleFavorite, 
    addToComparison, 
    removeFromComparison, 
    comparison 
  } = useFavoritesStore()

  const isInFavorites = isFavorite(gift.id)
  const isInComparison = comparison.some(g => g.id === gift.id)
  const canAddToComparison = comparison.length < 4

  const handleFavoriteClick = () => {
    toggleFavorite(gift)
  }

  const handleCompareClick = () => {
    if (isInComparison) {
      removeFromComparison(gift.id)
    } else if (canAddToComparison) {
      addToComparison(gift)
    }
  }

  return (
    <article className={styles.card}>
      <div className={styles.imageContainer}>
        {!imageLoaded && <div className={styles.imageSkeleton} />}
        <img
          src={gift.image || 'https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=400'}
          alt={gift.title}
          className={styles.image}
          loading="lazy"
          onLoad={() => setImageLoaded(true)}
          style={{ display: imageLoaded ? 'block' : 'none' }}
        />
        <button
          className={`${styles.favoriteButton} ${isInFavorites ? styles.isFavorite : ''}`}
          onClick={handleFavoriteClick}
          aria-label={isInFavorites ? 'Quitar de favoritos' : 'Agregar a favoritos'}
          title={isInFavorites ? 'Quitar de favoritos' : 'Agregar a favoritos'}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </button>
      </div>

      <div className={styles.content}>
        <h3 className={styles.title}>{gift.title}</h3>
        <p className={styles.description}>{gift.description}</p>
        
        <div className={styles.priceContainer}>
          <span className={styles.price}>
            {gift.currency} {gift.price?.toLocaleString('es-AR')}
          </span>
          <span className={styles.source}>{gift.source}</span>
        </div>

        <div className={styles.actions}>
          <Button
            variant="primary"
            size="sm"
            fullWidth
            onClick={() => window.open(gift.url, '_blank')}
          >
            Ver en tienda
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" style={{ marginLeft: '4px' }}>
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </Button>
          <button
            className={`${styles.compareButton} ${isInComparison ? styles.isComparing : ''}`}
            onClick={handleCompareClick}
            disabled={!canAddToComparison && !isInComparison}
            aria-label={isInComparison ? 'Quitar de comparación' : 'Agregar a comparación'}
            title={
              isInComparison 
                ? 'Quitar de comparación' 
                : canAddToComparison 
                  ? 'Agregar a comparación' 
                  : 'Máximo 4 productos'
            }
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M9 3v18M15 3v18" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </button>
        </div>
      </div>
    </article>
  )
}

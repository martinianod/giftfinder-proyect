import { Link } from 'react-router-dom'
import Container from '../../components/layout/Container'
import GiftCard from '../../components/gifts/GiftCard'
import Button from '../../components/ui/Button'
import { useFavoritesStore } from '../../store/favoritesStore'
import { Heart, ClipboardList } from "lucide-react"   // <-- ICONOS
import styles from './FavoritesPage.module.css'

export default function FavoritesPage() {
  const { favorites, clearFavorites } = useFavoritesStore()

  if (favorites.length === 0) {
    return (
      <Container>
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>
            <Heart size={42} strokeWidth={2} />
          </div>
          <h2>Aún no tienes favoritos</h2>
          <p>Guarda los regalos que te gusten para verlos después</p>

          <Link to="/">
            <Button variant="primary" size="lg">
              Explorar regalos
            </Button>
          </Link>
        </div>
      </Container>
    )
  }

  return (
    <Container>
      <div className={styles.page}>
        <div className={styles.header}>
          <div>
            <h1 className={styles.title}>Mis Favoritos</h1>
            <p className={styles.count}>
              {favorites.length} {favorites.length === 1 ? 'regalo guardado' : 'regalos guardados'}
            </p>
          </div>

          <div className={styles.actions}>
            {/* LIMPIAR TODO */}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                if (window.confirm('¿Estás seguro de que quieres eliminar todos los favoritos?')) {
                  clearFavorites()
                }
              }}
            >
              Limpiar todo
            </Button>

            {/* COPIAR LISTA */}
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                const text = favorites.map(g => `${g.title} - ${g.url}`).join('\n')
                navigator.clipboard.writeText(text)
                alert('¡Lista copiada al portapapeles!')
              }}
            >
              <ClipboardList size={16} style={{ marginRight: 6 }} />
              Copiar lista
            </Button>
          </div>
        </div>

        <div className={styles.grid}>
          {favorites.map((gift) => (
            <GiftCard key={gift.id} gift={gift} />
          ))}
        </div>
      </div>
    </Container>
  )
}

import Container from '../../components/layout/Container'
import Button from '../../components/ui/Button'
import { useGiftsStore } from '../../store/giftsStore'
import styles from './HomePage.module.css'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Target, Wallet, Heart } from "lucide-react"

export default function HomePage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const { searchGifts, isSearching } = useGiftsStore()

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!query.trim()) return
    
    try {
      await searchGifts(query)
      navigate('/search')
    } catch (error) {
      console.error('Search error:', error)
    }
  }

  return (
    <div className={styles.page}>
      <Container>
        <section className={styles.hero}>
          <h1 className={styles.title}>
            Encuentra el regalo perfecto
            <span className={styles.gradient}> con IA</span>
          </h1>
          <p className={styles.subtitle}>
            Describe a quién le quieres regalar y te sugerimos las mejores opciones
            de múltiples tiendas
          </p>

          <form onSubmit={handleSearch} className={styles.searchForm}>
            <textarea
              className={styles.searchInput}
              rows={3}
              placeholder="Ej: Regalo para mi novia de 27 años, le gustan los libros y el diseño, presupuesto hasta $40.000..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="Describe el regalo que buscas"
            />
            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              loading={isSearching}
              disabled={!query.trim()}
            >
              {isSearching ? 'Buscando...' : '🔍 Buscar regalo'}
            </Button>
          </form>

          <div className={styles.features}>
            <FeatureCard
              icon={<Target size={28} strokeWidth={2} />}
              title="IA Inteligente"
              description="Interpreta tus necesidades y encuentra lo ideal"
            />
            <FeatureCard
              icon={<Wallet size={28} strokeWidth={2} />}
              title="Compara Precios"
              description="Encuentra las mejores ofertas en múltiples tiendas"
            />
            <FeatureCard
              icon={<Heart size={28} strokeWidth={2} />}
              title="Guarda Favoritos"
              description="Organiza tus ideas y compártelas"
            />
          </div>
        </section>

        <section className={styles.popularSection}>
          <h2 className={styles.sectionTitle}>Búsquedas populares</h2>
          <div className={styles.tags}>
            {popularSearches.map((search) => (
              <button
                key={search}
                className={styles.tag}
                onClick={() => setQuery(search)}
              >
                {search}
              </button>
            ))}
          </div>
        </section>
      </Container>
    </div>
  )
}

function FeatureCard({ icon, title, description }) {
  return (
    <div className={styles.featureCard}>
      <span className={styles.featureIcon}>{icon}</span>
      <h3 className={styles.featureTitle}>{title}</h3>
      <p className={styles.featureDescription}>{description}</p>
    </div>
  )
}

const popularSearches = [
  'Regalo para mamá',
  'Cumpleaños 30 años',
  'Aniversario pareja',
  'Día del padre',
  'Regalo tecnológico',
  'Menos de $20.000'
]

import Container from '../../components/layout/Container'
import GiftCard from '../../components/gifts/GiftCard'
import { useGiftsStore } from '../../store/giftsStore'
import { Sparkles, User, PartyPopper, Wallet, Target } from "lucide-react"
import styles from './SearchPage.module.css'

export default function SearchPage() {
  const { results, isSearching, query } = useGiftsStore()

  if (isSearching) {
    return (
      <Container>
        <div className={styles.loading}>
          <div className={styles.spinner} />
          <p>Buscando los mejores regalos...</p>
        </div>
      </Container>
    )
  }

  if (!results) {
    return (
      <Container>
        <div className={styles.empty}>
          <h2>No hay resultados aún</h2>
          <p>Realiza una búsqueda desde la página principal</p>
        </div>
      </Container>
    )
  }

  return (
    <Container>
      <div className={styles.page}>
        <div className={styles.header}>
          <h1 className={styles.title}>Resultados para: "{query}"</h1>

          {results.interpretedIntent && (
            <div className={styles.intent}>
              <h3 className={styles.intentTitle}>
                <Sparkles size={18} style={{ marginRight: 6 }} />
                Interpretación de IA:
              </h3>

              <div className={styles.intentDetails}>
                {results.interpretedIntent.recipient && (
                  <span className={styles.tag}>
                    <User size={14} /> {results.interpretedIntent.recipient}
                  </span>
                )}

                {results.interpretedIntent.occasion && (
                  <span className={styles.tag}>
                    <PartyPopper size={14} /> {results.interpretedIntent.occasion}
                  </span>
                )}

                {results.interpretedIntent.budget && (
                  <span className={styles.tag}>
                    <Wallet size={14} /> ${results.interpretedIntent.budget?.toLocaleString('es-AR')}
                  </span>
                )}

                {results.interpretedIntent.interests &&
                  results.interpretedIntent.interests.length > 0 &&
                  results.interpretedIntent.interests.map((interest, idx) => (
                    <span key={idx} className={styles.tag}>
                      <Target size={14} /> {interest}
                    </span>
                  ))}
              </div>
            </div>
          )}
        </div>

        {results.recommendations && results.recommendations.length > 0 ? (
          <div className={styles.grid}>
            {results.recommendations.map((gift) => (
              <GiftCard key={gift.id} gift={gift} />
            ))}
          </div>
        ) : (
          <div className={styles.empty}>
            <p>No se encontraron resultados</p>
          </div>
        )}
      </div>
    </Container>
  )
}

import { Link } from 'react-router-dom'
import Container from '../../components/layout/Container'
import Button from '../../components/ui/Button'
import { useFavoritesStore } from '../../store/favoritesStore'
import { Scale, X } from "lucide-react"
import styles from './ComparisonPage.module.css'

export default function ComparisonPage() {
  const { comparison, removeFromComparison, clearComparison } = useFavoritesStore()

  if (comparison.length === 0) {
    return (
      <Container>
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>
            <Scale size={42} strokeWidth={2} />
          </div>

          <h2>No hay productos para comparar</h2>
          <p>Agrega hasta 4 productos desde los resultados de búsqueda para compararlos</p>

          <Link to="/">
            <Button variant="primary" size="lg">
              Buscar regalos
            </Button>
          </Link>
        </div>
      </Container>
    )
  }

  return (
    <Container>
      <div className={styles.page}>
        
        {/* HEADER */}
        <div className={styles.header}>
          <div>
            <h1 className={styles.title}>Comparación de Productos</h1>
            <p className={styles.count}>
              {comparison.length} {comparison.length === 1 ? 'producto' : 'productos'}
              {comparison.length < 4 && ` (puedes agregar ${4 - comparison.length} más)`}
            </p>
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              if (window.confirm('¿Estás seguro de que quieres limpiar la comparación?')) {
                clearComparison()
              }
            }}
          >
            Limpiar todo
          </Button>
        </div>

        {/* MOBILE VIEW */}
        <div className={styles.mobileView}>
          {comparison.map((gift) => (
            <div key={gift.id} className={styles.mobileCard}>
              
              {/* REMOVE BUTTON */}
              <button
                className={styles.removeButton}
                onClick={() => removeFromComparison(gift.id)}
                aria-label="Quitar de comparación"
              >
                <X size={16} />
              </button>

              <img src={gift.image} alt={gift.title} className={styles.mobileImage} />
              <h3 className={styles.mobileTitle}>{gift.title}</h3>

              <div className={styles.mobilePrice}>
                {gift.currency} {gift.price?.toLocaleString('es-AR')}
              </div>

              <p className={styles.mobileDescription}>{gift.description}</p>

              <div className={styles.mobileSource}>{gift.source}</div>

              <Button
                variant="primary"
                size="sm"
                fullWidth
                onClick={() => window.open(gift.url, '_blank')}
              >
                Ver en tienda
              </Button>
            </div>
          ))}
        </div>

        {/* DESKTOP TABLE VIEW */}
        <div className={styles.desktopView}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th className={styles.labelColumn}>Característica</th>

                {comparison.map((gift) => (
                  <th key={gift.id} className={styles.productColumn}>
                    <button
                      className={styles.removeButton}
                      onClick={() => removeFromComparison(gift.id)}
                      aria-label="Quitar de comparación"
                    >
                      <X size={16} />
                    </button>
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>

              {/* IMAGEN */}
              <tr>
                <td className={styles.label}>Imagen</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>
                    <img src={gift.image} alt={gift.title} className={styles.productImage} />
                  </td>
                ))}
              </tr>

              {/* PRODUCTO */}
              <tr>
                <td className={styles.label}>Producto</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>
                    <strong className={styles.productName}>{gift.title}</strong>
                  </td>
                ))}
              </tr>

              {/* PRECIO */}
              <tr className={styles.highlightRow}>
                <td className={styles.label}>Precio</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>
                    <span className={styles.price}>
                      {gift.currency} {gift.price?.toLocaleString('es-AR')}
                    </span>
                  </td>
                ))}
              </tr>

              {/* DESCRIPCIÓN */}
              <tr>
                <td className={styles.label}>Descripción</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>
                    <p className={styles.description}>{gift.description}</p>
                  </td>
                ))}
              </tr>

              {/* TIENDA */}
              <tr>
                <td className={styles.label}>Tienda</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>{gift.source}</td>
                ))}
              </tr>

              {/* ACCIÓN */}
              <tr>
                <td className={styles.label}>Acción</td>
                {comparison.map((gift) => (
                  <td key={gift.id}>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => window.open(gift.url, '_blank')}
                    >
                      Ver en tienda
                    </Button>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </Container>
  )
}

import { Gift, Heart, Scale, User } from "lucide-react"
import { Link } from 'react-router-dom'
import { useState, useRef, useEffect } from 'react'
import { useFavoritesStore } from '../../../store/favoritesStore'
import { useAuthStore } from '../../../store/authStore'
import styles from './Header.module.css'

export default function Header() {
  const { comparison, favorites } = useFavoritesStore()
  const { isAuthenticated, user, setShowLoginModal, logout } = useAuthStore()
  const [showUserMenu, setShowUserMenu] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowUserMenu(false)
      }
    }

    if (showUserMenu) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [showUserMenu])

  return (
    <header className={styles.header}>
      <div className={styles.container}>

        {/* LOGO */}
        <Link to="/" className={styles.logo}>
          <Gift size={22} />
          <span className={styles.title}>Findora</span>
        </Link>

        {/* NAV */}
        <nav className={styles.nav}>
          <Link to="/search" className={styles.navLink}>Buscar</Link>

          <Link to="/favorites" className={styles.navLink}>
            <Heart size={18} />
            {favorites.length > 0 && (
              <span className={styles.badge}>{favorites.length}</span>
            )}
          </Link>

          <Link to="/compare" className={styles.navLink}>
            <Scale size={18} />
            {comparison.length > 0 && (
              <span className={styles.badge}>{comparison.length}</span>
            )}
          </Link>

          {/* USER */}
          {isAuthenticated ? (
            <div className={styles.userMenuContainer} ref={menuRef}>
              <button
                className={styles.userButton}
                onClick={() => setShowUserMenu(!showUserMenu)}
              >
                <User size={20} />
                <span>{user?.username}</span>
              </button>

              {showUserMenu && (
                <div className={styles.userMenu}>
                  <div className={styles.userMenuHeader}>
                    <span>{user?.username}</span>
                    {user?.email && <span>{user.email}</span>}
                  </div>
                  <button className={styles.logoutButton} onClick={logout}>
                    Cerrar sesión
                  </button>
                </div>
              )}
            </div>
          ) : (
            <button
              className={styles.loginButton}
              onClick={() => setShowLoginModal(true)}
            >
              Iniciar sesión
            </button>
          )}
        </nav>
      </div>
    </header>
  )
}
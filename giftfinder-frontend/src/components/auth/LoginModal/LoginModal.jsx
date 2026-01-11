import { useState, useEffect, useRef } from 'react'
import { useAuthStore } from '../../../store/authStore'
import { authService } from '../../../services/auth.service'
import Button from '../../ui/Button'
import styles from './LoginModal.module.css'

export default function LoginModal() {
  const { showLoginModal, setShowLoginModal, authView, setAuthView, login, signup, isLoading, error } = useAuthStore()
  const modalRef = useRef(null)
  const firstInputRef = useRef(null)

  // Focus trap and escape key handling
  useEffect(() => {
    if (showLoginModal) {
      // Focus first input when modal opens
      firstInputRef.current?.focus()

      // Handle escape key
      const handleEscape = (e) => {
        if (e.key === 'Escape') {
          handleClose()
        }
      }

      // Trap focus within modal
      const handleTab = (e) => {
        if (e.key === 'Tab') {
          const focusableElements = modalRef.current?.querySelectorAll(
            'button, input, textarea, select, a[href]'
          )
          const firstElement = focusableElements?.[0]
          const lastElement = focusableElements?.[focusableElements.length - 1]

          if (e.shiftKey && document.activeElement === firstElement) {
            e.preventDefault()
            lastElement?.focus()
          } else if (!e.shiftKey && document.activeElement === lastElement) {
            e.preventDefault()
            firstElement?.focus()
          }
        }
      }

      document.addEventListener('keydown', handleEscape)
      document.addEventListener('keydown', handleTab)

      // Prevent body scroll
      document.body.style.overflow = 'hidden'

      return () => {
        document.removeEventListener('keydown', handleEscape)
        document.removeEventListener('keydown', handleTab)
        document.body.style.overflow = ''
      }
    }
  }, [showLoginModal])

  const handleClose = () => {
    setShowLoginModal(false)
  }

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleClose()
    }
  }

  if (!showLoginModal) return null

  return (
    <div 
      className={styles.backdrop} 
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-title"
    >
      <div className={styles.modal} ref={modalRef}>
        <button
          className={styles.closeButton}
          onClick={handleClose}
          aria-label="Cerrar modal"
          type="button"
        >
          ✕
        </button>

        {authView === 'login' && <LoginView firstInputRef={firstInputRef} />}
        {authView === 'signup' && <SignupView firstInputRef={firstInputRef} />}
        {authView === 'forgot-password' && <ForgotPasswordView firstInputRef={firstInputRef} />}
      </div>
    </div>
  )
}

// Login View Component
function LoginView({ firstInputRef }) {
  const { login, isLoading, error, setAuthView } = useAuthStore()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [validationErrors, setValidationErrors] = useState({})

  const validateForm = () => {
    const errors = {}
    
    if (!username.trim()) {
      errors.username = 'El usuario es requerido'
    }
    
    if (!password) {
      errors.password = 'La contraseña es requerida'
    } else if (password.length < 4) {
      errors.password = 'La contraseña debe tener al menos 4 caracteres'
    }

    setValidationErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!validateForm()) {
      return
    }

    try {
      await login(username, password)
      setUsername('')
      setPassword('')
      setValidationErrors({})
    } catch (err) {
      console.error('Login error:', err)
    }
  }

  return (
    <>
      <div className={styles.header}>
        <h2 id="auth-modal-title" className={styles.title}>
          🎁 Iniciar Sesión
        </h2>
        <p className={styles.subtitle}>
          Inicia sesión para guardar tus búsquedas y favoritos
        </p>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.field}>
          <label htmlFor="username" className={styles.label}>
            Usuario
          </label>
          <input
            ref={firstInputRef}
            id="username"
            type="text"
            className={`${styles.input} ${validationErrors.username ? styles.inputError : ''}`}
            value={username}
            onChange={(e) => {
              setUsername(e.target.value)
              if (validationErrors.username) {
                setValidationErrors(prev => ({ ...prev, username: null }))
              }
            }}
            placeholder="Ingresa tu usuario"
            disabled={isLoading}
            autoComplete="username"
          />
          {validationErrors.username && (
            <span className={styles.errorText}>{validationErrors.username}</span>
          )}
        </div>

        <div className={styles.field}>
          <label htmlFor="password" className={styles.label}>
            Contraseña
          </label>
          <input
            id="password"
            type="password"
            className={`${styles.input} ${validationErrors.password ? styles.inputError : ''}`}
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (validationErrors.password) {
                setValidationErrors(prev => ({ ...prev, password: null }))
              }
            }}
            placeholder="Ingresa tu contraseña"
            disabled={isLoading}
            autoComplete="current-password"
          />
          {validationErrors.password && (
            <span className={styles.errorText}>{validationErrors.password}</span>
          )}
        </div>

        <button
          type="button"
          className={styles.forgotLink}
          onClick={() => setAuthView('forgot-password')}
        >
          ¿Olvidaste tu contraseña?
        </button>

        {error && (
          <div className={styles.errorBox} role="alert">
            <span className={styles.errorIcon}>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          loading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
        </Button>
      </form>

      <div className={styles.footer}>
        <p className={styles.footerText}>
          ¿No tienes cuenta?{' '}
          <button 
            className={styles.link}
            onClick={() => setAuthView('signup')}
          >
            Regístrate aquí
          </button>
        </p>
      </div>
    </>
  )
}

// Signup View Component
function SignupView({ firstInputRef }) {
  const { signup, isLoading, error, setAuthView } = useAuthStore()
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false
  })
  const [validationErrors, setValidationErrors] = useState({})

  const validateEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  }

  const getPasswordStrength = (password) => {
    if (password.length === 0) return null
    if (password.length < 6) return 'weak'
    if (password.length < 10) return 'medium'
    return 'strong'
  }

  const validateForm = () => {
    const errors = {}
    
    if (!formData.email.trim()) {
      errors.email = 'El email es requerido'
    } else if (!validateEmail(formData.email)) {
      errors.email = 'Email inválido'
    }
    
    if (!formData.username.trim()) {
      errors.username = 'El usuario es requerido'
    } else if (formData.username.length < 3) {
      errors.username = 'El usuario debe tener al menos 3 caracteres'
    }
    
    if (!formData.password) {
      errors.password = 'La contraseña es requerida'
    } else if (formData.password.length < 6) {
      errors.password = 'La contraseña debe tener al menos 6 caracteres'
    }

    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Las contraseñas no coinciden'
    }

    if (!formData.acceptTerms) {
      errors.acceptTerms = 'Debes aceptar los términos y condiciones'
    }

    setValidationErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!validateForm()) {
      return
    }

    try {
      await signup(formData.email, formData.username, formData.password)
      setFormData({
        email: '',
        username: '',
        password: '',
        confirmPassword: '',
        acceptTerms: false
      })
      setValidationErrors({})
    } catch (err) {
      console.error('Signup error:', err)
    }
  }

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    if (validationErrors[field]) {
      setValidationErrors(prev => ({ ...prev, [field]: null }))
    }
  }

  const passwordStrength = getPasswordStrength(formData.password)

  return (
    <>
      <div className={styles.header}>
        <h2 id="auth-modal-title" className={styles.title}>
          🎁 Crear Cuenta
        </h2>
        <p className={styles.subtitle}>
          Regístrate para guardar tus búsquedas y favoritos
        </p>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.field}>
          <label htmlFor="email" className={styles.label}>
            Email
          </label>
          <input
            ref={firstInputRef}
            id="email"
            type="email"
            className={`${styles.input} ${validationErrors.email ? styles.inputError : ''}`}
            value={formData.email}
            onChange={(e) => handleChange('email', e.target.value)}
            placeholder="tu@email.com"
            disabled={isLoading}
            autoComplete="email"
          />
          {validationErrors.email && (
            <span className={styles.errorText}>{validationErrors.email}</span>
          )}
        </div>

        <div className={styles.field}>
          <label htmlFor="signup-username" className={styles.label}>
            Usuario
          </label>
          <input
            id="signup-username"
            type="text"
            className={`${styles.input} ${validationErrors.username ? styles.inputError : ''}`}
            value={formData.username}
            onChange={(e) => handleChange('username', e.target.value)}
            placeholder="Elige un nombre de usuario"
            disabled={isLoading}
            autoComplete="username"
          />
          {validationErrors.username && (
            <span className={styles.errorText}>{validationErrors.username}</span>
          )}
        </div>

        <div className={styles.field}>
          <label htmlFor="signup-password" className={styles.label}>
            Contraseña
          </label>
          <input
            id="signup-password"
            type="password"
            className={`${styles.input} ${validationErrors.password ? styles.inputError : ''}`}
            value={formData.password}
            onChange={(e) => handleChange('password', e.target.value)}
            placeholder="Mínimo 6 caracteres"
            disabled={isLoading}
            autoComplete="new-password"
          />
          {passwordStrength && (
            <div className={styles.passwordStrength}>
              <div className={`${styles.strengthBar} ${styles[passwordStrength]}`} />
              <span className={styles.strengthText}>
                {passwordStrength === 'weak' && 'Débil'}
                {passwordStrength === 'medium' && 'Media'}
                {passwordStrength === 'strong' && 'Fuerte'}
              </span>
            </div>
          )}
          {validationErrors.password && (
            <span className={styles.errorText}>{validationErrors.password}</span>
          )}
        </div>

        <div className={styles.field}>
          <label htmlFor="confirm-password" className={styles.label}>
            Confirmar Contraseña
          </label>
          <input
            id="confirm-password"
            type="password"
            className={`${styles.input} ${validationErrors.confirmPassword ? styles.inputError : ''}`}
            value={formData.confirmPassword}
            onChange={(e) => handleChange('confirmPassword', e.target.value)}
            placeholder="Repite tu contraseña"
            disabled={isLoading}
            autoComplete="new-password"
          />
          {validationErrors.confirmPassword && (
            <span className={styles.errorText}>{validationErrors.confirmPassword}</span>
          )}
        </div>

        <div className={styles.checkboxField}>
          <input
            id="accept-terms"
            type="checkbox"
            className={styles.checkbox}
            checked={formData.acceptTerms}
            onChange={(e) => handleChange('acceptTerms', e.target.checked)}
            disabled={isLoading}
          />
          <label htmlFor="accept-terms" className={styles.checkboxLabel}>
            Acepto los términos y condiciones
          </label>
        </div>
        {validationErrors.acceptTerms && (
          <span className={styles.errorText}>{validationErrors.acceptTerms}</span>
        )}

        {error && (
          <div className={styles.errorBox} role="alert">
            <span className={styles.errorIcon}>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          loading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Creando cuenta...' : 'Crear Cuenta'}
        </Button>
      </form>

      <div className={styles.footer}>
        <p className={styles.footerText}>
          ¿Ya tienes cuenta?{' '}
          <button 
            className={styles.link}
            onClick={() => setAuthView('login')}
          >
            Inicia sesión
          </button>
        </p>
      </div>
    </>
  )
}

// Forgot Password View Component
function ForgotPasswordView({ firstInputRef }) {
  const { setAuthView } = useAuthStore()
  const [email, setEmail] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [validationError, setValidationError] = useState('')

  const validateEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!email.trim()) {
      setValidationError('El email es requerido')
      return
    }
    
    if (!validateEmail(email)) {
      setValidationError('Email inválido')
      return
    }

    setIsLoading(true)
    setError(null)
    setValidationError('')

    try {
      await authService.forgotPassword(email)
      setSuccess(true)
    } catch (err) {
      setError(err.message || 'Error al enviar el email de recuperación')
    } finally {
      setIsLoading(false)
    }
  }

  if (success) {
    return (
      <>
        <div className={styles.header}>
          <div className={styles.successIcon}>✅</div>
          <h2 id="auth-modal-title" className={styles.title}>
            Email Enviado
          </h2>
          <p className={styles.subtitle}>
            Revisa tu correo para restablecer tu contraseña
          </p>
        </div>

        <div className={styles.successBox}>
          <p>
            Hemos enviado un enlace de recuperación a <strong>{email}</strong>.
            Por favor revisa tu bandeja de entrada y sigue las instrucciones.
          </p>
        </div>

        <Button
          type="button"
          variant="primary"
          size="lg"
          fullWidth
          onClick={() => setAuthView('login')}
        >
          Volver a Iniciar Sesión
        </Button>
      </>
    )
  }

  return (
    <>
      <div className={styles.header}>
        <h2 id="auth-modal-title" className={styles.title}>
          🔑 Recuperar Contraseña
        </h2>
        <p className={styles.subtitle}>
          Ingresa tu email para recibir instrucciones
        </p>
      </div>

      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.field}>
          <label htmlFor="forgot-email" className={styles.label}>
            Email
          </label>
          <input
            ref={firstInputRef}
            id="forgot-email"
            type="email"
            className={`${styles.input} ${validationError ? styles.inputError : ''}`}
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              if (validationError) setValidationError('')
            }}
            placeholder="tu@email.com"
            disabled={isLoading}
            autoComplete="email"
          />
          {validationError && (
            <span className={styles.errorText}>{validationError}</span>
          )}
        </div>

        {error && (
          <div className={styles.errorBox} role="alert">
            <span className={styles.errorIcon}>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          loading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Enviando...' : 'Enviar Email de Recuperación'}
        </Button>
      </form>

      <div className={styles.footer}>
        <p className={styles.footerText}>
          <button 
            className={styles.link}
            onClick={() => setAuthView('login')}
          >
            ← Volver a Iniciar Sesión
          </button>
        </p>
      </div>
    </>
  )
}

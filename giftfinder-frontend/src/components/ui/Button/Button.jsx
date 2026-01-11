import { forwardRef } from 'react'
import styles from './Button.module.css'

const Button = forwardRef(({
  children,
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  disabled = false,
  leftIcon,
  rightIcon,
  onClick,
  type = 'button',
  href,
  ...props
}, ref) => {
  const classNames = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth && styles.fullWidth,
    loading && styles.loading,
    disabled && styles.disabled
  ].filter(Boolean).join(' ')

  const content = (
    <>
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {!loading && leftIcon && <span className={styles.leftIcon}>{leftIcon}</span>}
      <span className={styles.content}>{children}</span>
      {!loading && rightIcon && <span className={styles.rightIcon}>{rightIcon}</span>}
    </>
  )

  if (href) {
    return (
      <a
        ref={ref}
        href={href}
        className={classNames}
        aria-busy={loading}
        {...props}
      >
        {content}
      </a>
    )
  }

  return (
    <button
      ref={ref}
      type={type}
      className={classNames}
      onClick={onClick}
      disabled={disabled || loading}
      aria-busy={loading}
      {...props}
    >
      {content}
    </button>
  )
})

Button.displayName = 'Button'

export default Button

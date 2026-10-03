import type {
  ButtonHTMLAttributes,
  ReactNode,
} from 'react'

import './Button.css'

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'danger'

export type ButtonSize = 'sm' | 'md'

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
  isLoading?: boolean
  loadingLabel?: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  isLoading = false,
  loadingLabel = 'Loading...',
  disabled,
  className,
  children,
  ...buttonProps
}: ButtonProps) {
  const classes = [
    'ui-button',
    `ui-button--${variant}`,
    `ui-button--${size}`,
    fullWidth ? 'ui-button--full-width' : undefined,
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      {...buttonProps}
      className={classes}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
    >
      {isLoading && (
        <span
          className="ui-button__spinner"
          aria-hidden="true"
        />
      )}
      <span className="ui-button__label">
        {isLoading ? loadingLabel : children}
      </span>
    </button>
  )
}

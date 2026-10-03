import type { ReactNode } from 'react'

import './FormField.css'

export type FormFieldProps = {
  htmlFor: string
  label: ReactNode
  children: ReactNode
  hint?: ReactNode
  error?: ReactNode
  required?: boolean
  className?: string
}

export function FormField({
  htmlFor,
  label,
  children,
  hint,
  error,
  required = false,
  className,
}: FormFieldProps) {
  const classes = [
    'ui-form-field',
    error ? 'ui-form-field--error' : undefined,
    className,
  ]
    .filter(Boolean)
    .join(' ')

  const labelClasses = [
    'ui-form-field__label',
    required ? 'ui-form-field__label--required' : undefined,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div className={classes}>
      <label
        className={labelClasses}
        htmlFor={htmlFor}
      >
        {label}
      </label>

      <div className="ui-form-field__control">
        {children}
      </div>

      {error ? (
        <p
          className="ui-form-field__message ui-form-field__message--error"
          role="alert"
        >
          {error}
        </p>
      ) : hint ? (
        <p className="ui-form-field__message">
          {hint}
        </p>
      ) : null}
    </div>
  )
}

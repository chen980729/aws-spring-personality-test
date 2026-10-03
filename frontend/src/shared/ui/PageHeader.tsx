import type { ReactNode } from 'react'

import './PageHeader.css'

export type PageHeaderProps = {
  title: ReactNode
  description?: ReactNode
  eyebrow?: ReactNode
  actions?: ReactNode
  className?: string
}

export function PageHeader({
  title,
  description,
  eyebrow,
  actions,
  className,
}: PageHeaderProps) {
  const classes = ['ui-page-header', className]
    .filter(Boolean)
    .join(' ')

  return (
    <header className={classes}>
      <div className="ui-page-header__copy">
        {eyebrow && (
          <p className="ui-page-header__eyebrow">
            {eyebrow}
          </p>
        )}

        <h1 className="ui-page-header__title">
          {title}
        </h1>

        {description && (
          <p className="ui-page-header__description">
            {description}
          </p>
        )}
      </div>

      {actions && (
        <div className="ui-page-header__actions">
          {actions}
        </div>
      )}
    </header>
  )
}

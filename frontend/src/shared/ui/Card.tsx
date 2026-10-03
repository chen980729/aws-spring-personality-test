import type { HTMLAttributes } from 'react'

import './Card.css'

export type CardVariant = 'default' | 'subtle'
export type CardPadding = 'sm' | 'md' | 'lg'

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  variant?: CardVariant
  padding?: CardPadding
}

export function Card({
  variant = 'default',
  padding = 'md',
  className,
  ...cardProps
}: CardProps) {
  const classes = [
    'ui-card',
    `ui-card--${variant}`,
    `ui-card--padding-${padding}`,
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div
      {...cardProps}
      className={classes}
    />
  )
}

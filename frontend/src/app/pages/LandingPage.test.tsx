import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import {
  describe,
  expect,
  it,
} from 'vitest'

import { renderWithProviders } from '../../test/render'
import { server } from '../../test/msw/server'
import { LandingPage } from './LandingPage'

describe('LandingPage', () => {
  it('renders the primary landing actions and the optimized personality hero artwork', async () => {
    server.use(
      http.get(
        'http://localhost:5173/actuator/health',
        () =>
          HttpResponse.json({
            status: 'UP',
          }),
      ),
    )

    renderWithProviders(<LandingPage />)

    expect(
      screen.getByRole('heading', {
        name: /Understand yourself\. Discover your patterns\./,
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: /Get started/,
      }),
    ).toHaveAttribute(
      'href',
      '/register',
    )

    expect(
      screen.getByRole('link', {
        name: 'I already have an account',
      }),
    ).toHaveAttribute(
      'href',
      '/login',
    )

    const heroImage =
      screen.getByRole('img', {
        name: 'Four illustrated personality profiles representing the assessment experience',
      })

    expect(heroImage).toHaveAttribute(
      'src',
      '/landing-personality-groups.webp',
    )
    expect(heroImage).toHaveAttribute(
      'width',
      '900',
    )
    expect(heroImage).toHaveAttribute(
      'height',
      '859',
    )
    expect(heroImage).toHaveAttribute(
      'loading',
      'eager',
    )
    expect(heroImage).toHaveAttribute(
      'fetchpriority',
      'high',
    )

    expect(
      screen.getByRole('heading', {
        name: 'From first answer to useful result',
      }),
    ).toBeInTheDocument()
  })
})

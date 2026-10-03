import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import {
  Route,
  Routes,
  useLocation,
} from 'react-router'

import { renderWithProviders } from '../../test/render'
import { server } from '../../test/msw/server'
import { AuthenticatedLayout } from './AuthenticatedLayout'

const currentUserEndpoint =
  'http://localhost:5173/api/v1/users/me'

function LocationProbe() {
  const location = useLocation()

  return (
    <p data-testid="location">
      {location.pathname}
      {location.search}
    </p>
  )
}

describe('AuthenticatedLayout', () => {
  it('renders protected content when the session is authenticated', async () => {
    server.use(
      http.get(currentUserEndpoint, () =>
        HttpResponse.json({
          id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
          email: 'user@example.com',
          displayName: 'Portfolio User',
        }),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route element={<AuthenticatedLayout />}>
          <Route
            path="/history"
            element={<p>Protected history</p>}
          />
        </Route>
      </Routes>,
      {
        initialEntries: ['/history'],
      },
    )

    expect(
      screen.getByText('Checking your session...'),
    ).toBeInTheDocument()

    expect(
      await screen.findByText('Protected history'),
    ).toBeInTheDocument()


    expect(
      screen.getByRole('navigation', {
        name: 'Primary navigation',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: 'Assessments',
      }),
    ).toHaveAttribute(
      'href',
      '/assessments',
    )

    expect(
      screen.getByRole('link', {
        name: 'History',
      }),
    ).toHaveAttribute(
      'href',
      '/history',
    )

    expect(
      screen.getByRole('link', {
        name: 'History',
      }),
    ).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('redirects an anonymous session to login and preserves the requested URL', async () => {
    server.use(
      http.get(currentUserEndpoint, () =>
        HttpResponse.json(
          {
            type: '/problems/authentication-required',
            title: 'Authentication required',
            status: 401,
            code: 'AUTHENTICATION_REQUIRED',
          },
          {
            status: 401,
            headers: {
              'Content-Type':
                'application/problem+json',
            },
          },
        ),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route element={<AuthenticatedLayout />}>
          <Route
            path="/history"
            element={<p>Protected history</p>}
          />
        </Route>
        <Route
          path="/login"
          element={<LocationProbe />}
        />
      </Routes>,
      {
        initialEntries: ['/history?page=2'],
      },
    )

    expect(
      await screen.findByTestId('location'),
    ).toHaveTextContent(
      '/login?returnTo=%2Fhistory%3Fpage%3D2',
    )

    expect(
      screen.queryByText('Protected history'),
    ).not.toBeInTheDocument()
  })

  it('shows a recoverable error when session verification fails unexpectedly', async () => {
    server.use(
      http.get(currentUserEndpoint, () =>
        HttpResponse.json(
          {
            status: 500,
          },
          {
            status: 500,
          },
        ),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route element={<AuthenticatedLayout />}>
          <Route
            path="/history"
            element={<p>Protected history</p>}
          />
        </Route>
      </Routes>,
      {
        initialEntries: ['/history'],
      },
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Unable to verify your session',
      }),
    ).toBeInTheDocument()
  })
})

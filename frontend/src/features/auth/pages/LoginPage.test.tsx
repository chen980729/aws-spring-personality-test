import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import { Route, Routes } from 'react-router'

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import {
  createTestQueryClient,
  renderWithProviders,
} from '../../../test/render'
import { server } from '../../../test/msw/server'
import { authQueryKeys } from '../api/authQueryKeys'
import { LoginPage } from './LoginPage'

const origin = 'http://localhost:5173'

describe('LoginPage', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('logs in, refreshes CSRF, caches the current user, and navigates', async () => {
    const user = userEvent.setup()
    const queryClient = createTestQueryClient()
    let csrfRequestCount = 0

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () => {
        csrfRequestCount += 1

        return HttpResponse.json({
          token:
            csrfRequestCount === 1
              ? 'anonymous-csrf'
              : 'authenticated-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        })
      }),
      http.post(
        `${origin}/api/v1/auth/login`,
        async ({ request }) => {
          expect(
            request.headers.get('X-CSRF-TOKEN'),
          ).toBe('anonymous-csrf')

          expect(await request.json()).toEqual({
            email: 'user@example.com',
            password: 'very-secure-password',
          })

          return HttpResponse.json({
            id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
            email: 'user@example.com',
            displayName: 'Portfolio User',
          })
        },
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/login"
          element={<LoginPage />}
        />
        <Route
          path="/assessments"
          element={<p>Assessments destination</p>}
        />
      </Routes>,
      {
        initialEntries: ['/login'],
        queryClient,
      },
    )

    await user.type(
      screen.getByLabelText('Email'),
      'user@example.com',
    )
    await user.type(
      screen.getByLabelText('Password'),
      'very-secure-password',
    )
    await user.click(
      screen.getByRole('button', {
        name: 'Sign in',
      }),
    )

    expect(
      await screen.findByText('Assessments destination'),
    ).toBeInTheDocument()

    expect(csrfRequestCount).toBe(2)

    expect(
      queryClient.getQueryData(authQueryKeys.me),
    ).toEqual({
      id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
      email: 'user@example.com',
      displayName: 'Portfolio User',
    })
  })

  it('returns the user to the originally requested internal route', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(
        `${origin}/api/v1/auth/login`,
        () =>
          HttpResponse.json({
            id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
            email: 'user@example.com',
            displayName: 'Portfolio User',
          }),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/login"
          element={<LoginPage />}
        />
        <Route
          path="/history"
          element={<p>History destination</p>}
        />
      </Routes>,
      {
        initialEntries: [
          '/login?returnTo=%2Fhistory%3Fpage%3D2',
        ],
      },
    )

    await user.type(
      screen.getByLabelText('Email'),
      'user@example.com',
    )
    await user.type(
      screen.getByLabelText('Password'),
      'very-secure-password',
    )
    await user.click(
      screen.getByRole('button', {
        name: 'Sign in',
      }),
    )

    expect(
      await screen.findByText('History destination'),
    ).toBeInTheDocument()
  })

  it('falls back to assessments for an external returnTo value', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(
        `${origin}/api/v1/auth/login`,
        () =>
          HttpResponse.json({
            id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
            email: 'user@example.com',
            displayName: 'Portfolio User',
          }),
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/login"
          element={<LoginPage />}
        />
        <Route
          path="/assessments"
          element={<p>Assessments destination</p>}
        />
      </Routes>,
      {
        initialEntries: [
          '/login?returnTo=https%3A%2F%2Fevil.example',
        ],
      },
    )

    await user.type(
      screen.getByLabelText('Email'),
      'user@example.com',
    )
    await user.type(
      screen.getByLabelText('Password'),
      'very-secure-password',
    )
    await user.click(
      screen.getByRole('button', {
        name: 'Sign in',
      }),
    )

    expect(
      await screen.findByText('Assessments destination'),
    ).toBeInTheDocument()
  })

  it('shows an invalid credentials error without navigating', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'anonymous-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(
        `${origin}/api/v1/auth/login`,
        () =>
          HttpResponse.json(
            {
              type: '/problems/invalid-credentials',
              title: 'Invalid credentials',
              status: 401,
              detail:
                'The email or password is invalid.',
              code: 'INVALID_CREDENTIALS',
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

    renderWithProviders(<LoginPage />, {
      initialEntries: ['/login'],
    })

    await user.type(
      screen.getByLabelText('Email'),
      'user@example.com',
    )
    await user.type(
      screen.getByLabelText('Password'),
      'wrong-password',
    )
    await user.click(
      screen.getByRole('button', {
        name: 'Sign in',
      }),
    )

    expect(
      await screen.findByText(
        'The email or password is incorrect.',
      ),
    ).toBeInTheDocument()
  })
})

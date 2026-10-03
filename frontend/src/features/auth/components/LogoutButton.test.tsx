import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import {
  Route,
  Routes,
} from 'react-router'

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import {
  createTestQueryClient,
  renderWithProviders,
} from '../../../test/render'
import { server } from '../../../test/msw/server'
import { authQueryKeys } from '../api/authQueryKeys'
import { LogoutButton } from './LogoutButton'

const origin = 'http://localhost:5173'

function seedAuthenticatedUser(
  queryClient: ReturnType<typeof createTestQueryClient>,
) {
  queryClient.setQueryData(
    authQueryKeys.me,
    {
      id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
      email: 'user@example.com',
      displayName: 'Portfolio User',
    },
  )
}

describe('LogoutButton', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('logs out, clears the cached user, and navigates to login', async () => {
    const user = userEvent.setup()
    const queryClient = createTestQueryClient()

    seedAuthenticatedUser(queryClient)

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'authenticated-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        `${origin}/api/v1/auth/logout`,
        ({ request }) => {
          expect(
            request.headers.get('X-CSRF-TOKEN'),
          ).toBe('authenticated-csrf')

          return new HttpResponse(null, {
            status: 204,
          })
        },
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/protected"
          element={<LogoutButton />}
        />
        <Route
          path="/login"
          element={<p>Login destination</p>}
        />
      </Routes>,
      {
        initialEntries: ['/protected'],
        queryClient,
      },
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Sign out',
      }),
    )

    expect(
      await screen.findByText('Login destination'),
    ).toBeInTheDocument()

    expect(
      queryClient.getQueryData(authQueryKeys.me),
    ).toBeNull()
  })

  it('treats an already-expired session as successfully logged out', async () => {
    const user = userEvent.setup()
    const queryClient = createTestQueryClient()

    seedAuthenticatedUser(queryClient)

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'anonymous-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        `${origin}/api/v1/auth/logout`,
        () =>
          HttpResponse.json(
            {
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
        <Route
          path="/protected"
          element={<LogoutButton />}
        />
        <Route
          path="/login"
          element={<p>Login destination</p>}
        />
      </Routes>,
      {
        initialEntries: ['/protected'],
        queryClient,
      },
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Sign out',
      }),
    )

    expect(
      await screen.findByText('Login destination'),
    ).toBeInTheDocument()

    expect(
      queryClient.getQueryData(authQueryKeys.me),
    ).toBeNull()
  })
})

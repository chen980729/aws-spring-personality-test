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
import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { LoginPage } from './LoginPage'
import { RegisterPage } from './RegisterPage'

const origin = 'http://localhost:5173'

describe('RegisterPage', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('registers an account and sends the user to login', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'register-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(
        `${origin}/api/v1/auth/register`,
        async ({ request }) => {
          expect(
            request.headers.get('X-CSRF-TOKEN'),
          ).toBe('register-csrf')

          expect(await request.json()).toEqual({
            displayName: 'Portfolio User',
            email: 'user@example.com',
            password: 'very-secure-password',
          })

          return HttpResponse.json(
            {
              id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
              email: 'user@example.com',
              displayName: 'Portfolio User',
              createdAt: '2026-10-02T12:00:00Z',
            },
            {
              status: 201,
            },
          )
        },
      ),
    )

    renderWithProviders(
      <Routes>
        <Route
          path="/register"
          element={<RegisterPage />}
        />
        <Route
          path="/login"
          element={<LoginPage />}
        />
      </Routes>,
      {
        initialEntries: ['/register'],
      },
    )

    await user.type(
      screen.getByLabelText('Display name'),
      'Portfolio User',
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
        name: 'Create account',
      }),
    )

    expect(
      await screen.findByText(
        'Account created. Sign in to continue.',
      ),
    ).toBeInTheDocument()
  })

  it('shows an email error when the account already exists', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'register-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(
        `${origin}/api/v1/auth/register`,
        () =>
          HttpResponse.json(
            {
              type: '/problems/email-already-registered',
              title: 'Email already registered',
              status: 409,
              code: 'EMAIL_ALREADY_REGISTERED',
            },
            {
              status: 409,
              headers: {
                'Content-Type':
                  'application/problem+json',
              },
            },
          ),
      ),
    )

    renderWithProviders(<RegisterPage />, {
      initialEntries: ['/register'],
    })

    await user.type(
      screen.getByLabelText('Display name'),
      'Portfolio User',
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
        name: 'Create account',
      }),
    )

    expect(
      await screen.findByText(
        'An account already exists for this email.',
      ),
    ).toBeInTheDocument()
  })
})

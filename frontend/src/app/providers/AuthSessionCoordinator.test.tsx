import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'

import { authQueryKeys } from '../../features/auth/api/authQueryKeys'
import { csrfTokenManager } from '../../shared/api/csrfTokenManager'
import { postJson } from '../../shared/api/httpClient'
import {
  createTestQueryClient,
  renderWithProviders,
} from '../../test/render'
import { server } from '../../test/msw/server'
import { AuthSessionCoordinator } from './AuthSessionCoordinator'

const origin = 'http://localhost:5173'

describe('AuthSessionCoordinator', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('clears the cached user after stale CSRF recovery ends in authentication required', async () => {
    const queryClient = createTestQueryClient()

    queryClient.setQueryData(
      authQueryKeys.me,
      {
        id: '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
        email: 'user@example.com',
        displayName: 'Portfolio User',
      },
    )

    let csrfRequestCount = 0
    let protectedRequestCount = 0

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () => {
          csrfRequestCount += 1

          return HttpResponse.json({
            token:
              csrfRequestCount === 1
                ? 'stale-csrf'
                : 'fresh-anonymous-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          })
        },
      ),
      http.post(
        `${origin}/api/v1/protected-command`,
        () => {
          protectedRequestCount += 1

          if (protectedRequestCount === 1) {
            return HttpResponse.json(
              {
                status: 403,
                code: 'CSRF_VALIDATION_FAILED',
              },
              {
                status: 403,
                headers: {
                  'Content-Type':
                    'application/problem+json',
                },
              },
            )
          }

          return HttpResponse.json(
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
          )
        },
      ),
    )

    renderWithProviders(
      <AuthSessionCoordinator />,
      {
        queryClient,
      },
    )

    await expect(
      postJson(
        '/api/v1/protected-command',
        {
          value: 'example',
        },
      ),
    ).rejects.toMatchObject({
      status: 401,
      code: 'AUTHENTICATION_REQUIRED',
    })

    expect(csrfRequestCount).toBe(2)
    expect(protectedRequestCount).toBe(2)

    expect(
      queryClient.getQueryData(authQueryKeys.me),
    ).toBeNull()
  })
})

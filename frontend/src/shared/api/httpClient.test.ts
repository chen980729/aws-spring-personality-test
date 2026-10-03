import { HttpResponse, http } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'

import { server } from '../../test/msw/server'
import { ApiError, getJson, postJson } from './httpClient'
import { csrfTokenManager } from './csrfTokenManager'

const origin = 'http://localhost:5173'

describe('httpClient', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('parses Problem Details into ApiError', async () => {
    server.use(
      http.get(`${origin}/api/v1/protected`, () =>
        HttpResponse.json(
          {
            type: '/problems/authentication-required',
            title: 'Authentication required',
            status: 401,
            detail: 'Authentication is required to access this resource.',
            code: 'AUTHENTICATION_REQUIRED',
          },
          {
            status: 401,
            headers: {
              'Content-Type': 'application/problem+json',
            },
          },
        ),
      ),
    )

    try {
      await getJson('/api/v1/protected')
      throw new Error('Expected request to fail')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError)

      const apiError = error as ApiError

      expect(apiError.status).toBe(401)
      expect(apiError.code).toBe('AUTHENTICATION_REQUIRED')
      expect(apiError.problem?.title).toBe('Authentication required')
    }
  })

  it('adds the current CSRF token to unsafe requests', async () => {
    let receivedCsrfHeader: string | null = null

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(`${origin}/api/v1/example`, ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-CSRF-TOKEN')

        return HttpResponse.json({
          accepted: true,
        })
      }),
    )

    const response = await postJson<{ accepted: boolean }>(
      '/api/v1/example',
      {
        value: 'example',
      },
    )

    expect(receivedCsrfHeader).toBe('csrf-token')
    expect(response.accepted).toBe(true)
  })

  it('refreshes CSRF and retries an unsafe request once after CSRF validation failure', async () => {
    let csrfRequestCount = 0
    let postRequestCount = 0

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () => {
        csrfRequestCount += 1

        return HttpResponse.json({
          token:
            csrfRequestCount === 1
              ? 'stale-token'
              : 'fresh-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        })
      }),
      http.post(`${origin}/api/v1/example`, ({ request }) => {
        postRequestCount += 1

        if (postRequestCount === 1) {
          return HttpResponse.json(
            {
              type: '/problems/csrf-validation-failed',
              title: 'CSRF validation failed',
              status: 403,
              detail: 'The CSRF token is missing or invalid.',
              code: 'CSRF_VALIDATION_FAILED',
            },
            {
              status: 403,
              headers: {
                'Content-Type': 'application/problem+json',
              },
            },
          )
        }

        expect(request.headers.get('X-CSRF-TOKEN')).toBe(
          'fresh-token',
        )

        return HttpResponse.json({
          accepted: true,
        })
      }),
    )

    const response = await postJson<{ accepted: boolean }>(
      '/api/v1/example',
      {
        value: 'example',
      },
    )

    expect(response.accepted).toBe(true)
    expect(csrfRequestCount).toBe(2)
    expect(postRequestCount).toBe(2)
  })

  it('does not retry a non-CSRF forbidden response', async () => {
    let postRequestCount = 0

    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () =>
        HttpResponse.json({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
      ),
      http.post(`${origin}/api/v1/example`, () => {
        postRequestCount += 1

        return HttpResponse.json(
          {
            type: '/problems/access-denied',
            title: 'Access denied',
            status: 403,
            detail:
              'You do not have permission to access this resource.',
            code: 'ACCESS_DENIED',
          },
          {
            status: 403,
            headers: {
              'Content-Type': 'application/problem+json',
            },
          },
        )
      }),
    )

    await expect(
      postJson('/api/v1/example', {
        value: 'example',
      }),
    ).rejects.toMatchObject({
      status: 403,
      code: 'ACCESS_DENIED',
    })

    expect(postRequestCount).toBe(1)
  })
})

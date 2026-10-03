import { HttpResponse, http } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'

import { server } from '../../test/msw/server'
import { csrfTokenManager } from './csrfTokenManager'

const csrfEndpoint = 'http://localhost:5173/api/v1/auth/csrf'

describe('csrfTokenManager', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('reuses a cached CSRF token', async () => {
    let requestCount = 0

    server.use(
      http.get(csrfEndpoint, () => {
        requestCount += 1

        return HttpResponse.json({
          token: 'token-1',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        })
      }),
    )

    const first = await csrfTokenManager.getToken()
    const second = await csrfTokenManager.getToken()

    expect(first.token).toBe('token-1')
    expect(second.token).toBe('token-1')
    expect(requestCount).toBe(1)
  })

  it('shares one in-flight refresh between concurrent callers', async () => {
    let requestCount = 0

    server.use(
      http.get(csrfEndpoint, () => {
        requestCount += 1

        return HttpResponse.json({
          token: 'token-1',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        })
      }),
    )

    const [first, second] = await Promise.all([
      csrfTokenManager.getToken(),
      csrfTokenManager.getToken(),
    ])

    expect(first.token).toBe('token-1')
    expect(second.token).toBe('token-1')
    expect(requestCount).toBe(1)
  })

  it('fetches a new token after invalidation', async () => {
    let requestCount = 0

    server.use(
      http.get(csrfEndpoint, () => {
        requestCount += 1

        return HttpResponse.json({
          token: `token-${requestCount}`,
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        })
      }),
    )

    const first = await csrfTokenManager.getToken()

    csrfTokenManager.invalidate()

    const second = await csrfTokenManager.getToken()

    expect(first.token).toBe('token-1')
    expect(second.token).toBe('token-2')
    expect(requestCount).toBe(2)
  })
})

import { createApiError } from './apiError'

export interface CsrfToken {
  token: string
  headerName: string
  parameterName: string
}

let cachedToken: CsrfToken | undefined
let refreshInFlight: Promise<CsrfToken> | undefined

function isCsrfToken(value: unknown): value is CsrfToken {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Record<string, unknown>

  return (
    typeof candidate.token === 'string' &&
    typeof candidate.headerName === 'string' &&
    typeof candidate.parameterName === 'string'
  )
}

async function fetchCsrfToken(): Promise<CsrfToken> {
  const response = await fetch('/api/v1/auth/csrf', {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    credentials: 'same-origin',
  })

  if (!response.ok) {
    throw await createApiError(response)
  }

  const body: unknown = await response.json()

  if (!isCsrfToken(body)) {
    throw new Error('Invalid CSRF token response')
  }

  return body
}

async function refresh(): Promise<CsrfToken> {
  if (refreshInFlight) {
    return refreshInFlight
  }

  refreshInFlight = fetchCsrfToken()
    .then((token) => {
      cachedToken = token
      return token
    })
    .finally(() => {
      refreshInFlight = undefined
    })

  return refreshInFlight
}

export const csrfTokenManager = {
  getToken(): Promise<CsrfToken> {
    if (cachedToken) {
      return Promise.resolve(cachedToken)
    }

    return refresh()
  },

  refresh(): Promise<CsrfToken> {
    cachedToken = undefined
    return refresh()
  },

  invalidate(): void {
    cachedToken = undefined
  },
}

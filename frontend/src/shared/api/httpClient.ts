import { ApiError, createApiError } from './apiError'
import { notifyAuthenticationRequired } from './authenticationRequiredEvents'
import { csrfTokenManager } from './csrfTokenManager'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface RequestJsonOptions {
  method?: HttpMethod
  body?: unknown
  headers?: HeadersInit
  signal?: AbortSignal
}

const unsafeMethods = new Set<HttpMethod>([
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
])

function isUnsafeMethod(method: HttpMethod): boolean {
  return unsafeMethods.has(method)
}

async function executeJsonRequest<T>(
  path: string,
  options: RequestJsonOptions,
  csrfRetryAttempted: boolean,
): Promise<T> {
  const method = options.method ?? 'GET'
  const headers = new Headers(options.headers)

  headers.set('Accept', 'application/json')

  let body: BodyInit | undefined

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }

  if (isUnsafeMethod(method)) {
    const csrfToken = await csrfTokenManager.getToken()
    headers.set(csrfToken.headerName, csrfToken.token)
  }

  const response = await fetch(path, {
    method,
    headers,
    body,
    signal: options.signal,
    credentials: 'same-origin',
  })

  if (!response.ok) {
    const error = await createApiError(response)

    if (
      isUnsafeMethod(method) &&
      !csrfRetryAttempted &&
      error.code === 'CSRF_VALIDATION_FAILED'
    ) {
      await csrfTokenManager.refresh()

      return executeJsonRequest<T>(path, options, true)
    }

    if (error.code === 'AUTHENTICATION_REQUIRED') {
      notifyAuthenticationRequired()
    }

    throw error
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export function requestJson<T>(
  path: string,
  options: RequestJsonOptions = {},
): Promise<T> {
  return executeJsonRequest<T>(path, options, false)
}

export function getJson<T>(
  path: string,
  options: Omit<RequestJsonOptions, 'method' | 'body'> = {},
): Promise<T> {
  return requestJson<T>(path, {
    ...options,
    method: 'GET',
  })
}

export function postJson<T>(
  path: string,
  body?: unknown,
  options: Omit<RequestJsonOptions, 'method' | 'body'> = {},
): Promise<T> {
  return requestJson<T>(path, {
    ...options,
    method: 'POST',
    body,
  })
}

export function putJson<T>(
  path: string,
  body?: unknown,
  options: Omit<RequestJsonOptions, 'method' | 'body'> = {},
): Promise<T> {
  return requestJson<T>(path, {
    ...options,
    method: 'PUT',
    body,
  })
}

export function deleteJson<T>(
  path: string,
  options: Omit<RequestJsonOptions, 'method' | 'body'> = {},
): Promise<T> {
  return requestJson<T>(path, {
    ...options,
    method: 'DELETE',
  })
}

export { ApiError }

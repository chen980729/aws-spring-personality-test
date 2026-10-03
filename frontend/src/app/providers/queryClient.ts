import { QueryClient } from '@tanstack/react-query'

import { isApiError } from '../../shared/api/apiError'

export function shouldRetryQuery(
  failureCount: number,
  error: unknown,
): boolean {
  if (
    isApiError(error) &&
    error.status >= 400 &&
    error.status < 500
  ) {
    return false
  }

  // Retry transient/network and 5xx failures once.
  return failureCount < 1
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: shouldRetryQuery,
    },
  },
})

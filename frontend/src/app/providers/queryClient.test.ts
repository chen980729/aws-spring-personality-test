import { describe, expect, it } from 'vitest'

import { ApiError } from '../../shared/api/apiError'
import { shouldRetryQuery } from './queryClient'

describe('shouldRetryQuery', () => {
  it('does not retry deterministic client errors', () => {
    expect(
      shouldRetryQuery(
        0,
        new ApiError(400),
      ),
    ).toBe(false)

    expect(
      shouldRetryQuery(
        0,
        new ApiError(404),
      ),
    ).toBe(false)
  })

  it('retries a server error once', () => {
    const error = new ApiError(500)

    expect(
      shouldRetryQuery(0, error),
    ).toBe(true)

    expect(
      shouldRetryQuery(1, error),
    ).toBe(false)
  })

  it('retries a non-HTTP failure once', () => {
    const error = new TypeError('Network failure')

    expect(
      shouldRetryQuery(0, error),
    ).toBe(true)

    expect(
      shouldRetryQuery(1, error),
    ).toBe(false)
  })
})

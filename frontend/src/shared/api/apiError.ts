import { parseApiProblem, type ApiProblem } from './apiProblem'

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ApiProblem

  constructor(status: number, problem?: ApiProblem) {
    super(
      problem?.detail ??
        problem?.title ??
        `Request failed with status ${status}`,
    )

    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  get code(): string | undefined {
    return this.problem?.code
  }
}

export async function createApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('content-type')

  if (contentType?.includes('json')) {
    try {
      const body: unknown = await response.json()
      return new ApiError(response.status, parseApiProblem(body))
    } catch {
      return new ApiError(response.status)
    }
  }

  return new ApiError(response.status)
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

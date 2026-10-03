export interface ApiFieldError {
  field: string
  code: string
  message: string
}

export interface ApiProblem {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  code?: string
  fieldErrors?: ApiFieldError[]
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function parseApiProblem(value: unknown): ApiProblem | undefined {
  if (!isRecord(value) || typeof value.status !== 'number') {
    return undefined
  }

  return {
    type: typeof value.type === 'string' ? value.type : undefined,
    title: typeof value.title === 'string' ? value.title : undefined,
    status: value.status,
    detail: typeof value.detail === 'string' ? value.detail : undefined,
    instance: typeof value.instance === 'string' ? value.instance : undefined,
    code: typeof value.code === 'string' ? value.code : undefined,
    fieldErrors: Array.isArray(value.fieldErrors)
      ? value.fieldErrors
          .filter(isRecord)
          .filter(
            (error) =>
              typeof error.field === 'string' &&
              typeof error.code === 'string' &&
              typeof error.message === 'string',
          )
          .map((error) => ({
            field: error.field as string,
            code: error.code as string,
            message: error.message as string,
          }))
      : undefined,
  }
}

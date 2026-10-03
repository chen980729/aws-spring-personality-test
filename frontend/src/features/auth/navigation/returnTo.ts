const defaultReturnTo = '/assessments'

export function sanitizeReturnTo(
  value: string | null | undefined,
): string {
  if (
    !value ||
    !value.startsWith('/') ||
    value.startsWith('//')
  ) {
    return defaultReturnTo
  }

  try {
    const baseUrl = 'http://app.local'
    const url = new URL(value, baseUrl)

    if (url.origin !== baseUrl) {
      return defaultReturnTo
    }

    return `${url.pathname}${url.search}${url.hash}`
  } catch {
    return defaultReturnTo
  }
}

export function buildLoginPath(
  returnTo: string,
): string {
  return `/login?returnTo=${encodeURIComponent(returnTo)}`
}

export interface AssessmentSessionNavigationState {
  historyReturnTo?: string
}

export function buildHistoryReturnTo(
  pathname: string,
  search: string,
): string {
  return `${pathname}${search}`
}

export function readHistoryReturnTo(
  state: unknown,
): string | null {
  if (
    typeof state !== 'object' ||
    state === null ||
    !('historyReturnTo' in state)
  ) {
    return null
  }

  const candidate =
    (state as AssessmentSessionNavigationState)
      .historyReturnTo

  if (
    typeof candidate !== 'string'
  ) {
    return null
  }

  if (
    !/^\/history(?:\?[^#]*)?$/.test(
      candidate,
    )
  ) {
    return null
  }

  return candidate
}

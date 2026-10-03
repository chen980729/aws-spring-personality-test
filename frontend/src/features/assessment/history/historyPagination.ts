export const assessmentHistoryPageSize = 20

export function parseAssessmentHistoryPage(
  value: string | null,
): number {
  if (
    value === null ||
    !/^[1-9]\d*$/.test(value)
  ) {
    return 1
  }

  const page = Number(value)

  if (!Number.isSafeInteger(page)) {
    return 1
  }

  return page
}

export function assessmentHistoryHref(
  page: number,
): string {
  if (page <= 1) {
    return '/history'
  }

  return `/history?page=${page}`
}

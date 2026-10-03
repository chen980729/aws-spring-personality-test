import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  assessmentHistoryHref,
  parseAssessmentHistoryPage,
} from './historyPagination'

describe('historyPagination', () => {
  it('parses positive one-based page numbers', () => {
    expect(
      parseAssessmentHistoryPage('1'),
    ).toBe(1)

    expect(
      parseAssessmentHistoryPage('12'),
    ).toBe(12)
  })

  it('falls back to page one for missing or invalid values', () => {
    expect(
      parseAssessmentHistoryPage(null),
    ).toBe(1)

    expect(
      parseAssessmentHistoryPage('0'),
    ).toBe(1)

    expect(
      parseAssessmentHistoryPage('-1'),
    ).toBe(1)

    expect(
      parseAssessmentHistoryPage('abc'),
    ).toBe(1)

    expect(
      parseAssessmentHistoryPage('1.5'),
    ).toBe(1)
  })

  it('uses a canonical URL without a query parameter for page one', () => {
    expect(
      assessmentHistoryHref(1),
    ).toBe('/history')

    expect(
      assessmentHistoryHref(2),
    ).toBe('/history?page=2')
  })
})

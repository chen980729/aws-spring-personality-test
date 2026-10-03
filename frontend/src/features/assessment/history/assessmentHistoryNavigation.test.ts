import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  buildHistoryReturnTo,
  readHistoryReturnTo,
} from './assessmentHistoryNavigation'

describe('assessmentHistoryNavigation', () => {
  it('preserves the current history page as transient navigation state', () => {
    expect(
      buildHistoryReturnTo(
        '/history',
        '?page=2',
      ),
    ).toBe('/history?page=2')
  })

  it('accepts only assessment history return targets', () => {
    expect(
      readHistoryReturnTo({
        historyReturnTo:
          '/history?page=2',
      }),
    ).toBe('/history?page=2')

    expect(
      readHistoryReturnTo({
        historyReturnTo:
          '/assessments',
      }),
    ).toBeNull()

    expect(
      readHistoryReturnTo({
        historyReturnTo:
          '//example.com',
      }),
    ).toBeNull()

    expect(
      readHistoryReturnTo(null),
    ).toBeNull()
  })
})

import { describe, expect, it } from 'vitest'

import {
  buildLoginPath,
  sanitizeReturnTo,
} from './returnTo'

describe('returnTo navigation', () => {
  it('keeps an internal application path', () => {
    expect(
      sanitizeReturnTo(
        '/assessment-sessions/session-1?tab=result#summary',
      ),
    ).toBe(
      '/assessment-sessions/session-1?tab=result#summary',
    )
  })

  it('falls back for an absolute external URL', () => {
    expect(
      sanitizeReturnTo('https://evil.example/path'),
    ).toBe('/assessments')
  })

  it('falls back for a protocol-relative URL', () => {
    expect(
      sanitizeReturnTo('//evil.example/path'),
    ).toBe('/assessments')
  })

  it('builds a login URL with an encoded return path', () => {
    expect(
      buildLoginPath('/history?page=2'),
    ).toBe(
      '/login?returnTo=%2Fhistory%3Fpage%3D2',
    )
  })
})

import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import type { AssessmentSession, ClarificationInteractionResponse } from '../api/assessmentTypes'
import { ClarificationStage } from './ClarificationStage'

const origin = 'http://localhost:5173'
const sessionId = '9e6a5c52-54df-4e22-8ca6-779d32e4d061'
const base = `${origin}/api/v1/assessment-sessions/${sessionId}/clarifications/EI`

type Mode = 'pending' | 'in-progress' | 'retryable'

function session(mode: Mode): AssessmentSession {
  const status = mode === 'pending' ? 'PENDING'
    : mode === 'retryable' ? 'FAILED_RETRYABLE' : 'IN_PROGRESS'
  return {
    id: sessionId,
    assessment: { code: 'SIXTEEN_PERSONALITY', version: '1.1' },
    status: mode === 'in-progress' ? 'CLARIFICATION_IN_PROGRESS' : 'AWAITING_CLARIFICATION',
    questionnaire: { answers: [], submitted: true, submittedAt: '2026-10-13T00:00:00Z' },
    initialResult: {
      dimensions: [{
        dimensionCode: 'EI', rawScore: 0, questionnairePreference: null,
        ambiguous: true,
        evidence: { poleA: 'E', poleAPercentage: 50, poleB: 'I', poleBPercentage: 50 },
      }],
    },
    clarifications: [{
      dimensionCode: 'EI', status,
      result: null, startedAt: null, acceptedAt: null,
    }],
    tieBreaks: [], finalResult: null,
    workflow: {
      pendingClarificationDimensions: mode === 'pending' ? ['EI'] : [],
      retryableClarificationDimensions: mode === 'retryable' ? ['EI'] : [],
      tieBreakRequiredDimensions: [], completed: false,
    },
    createdAt: '2026-10-13T00:00:00Z', completedAt: null, abandonedAt: null,
  }
}

function interaction(sequence: number, question: string): ClarificationInteractionResponse {
  return {
    sessionId, dimensionCode: 'EI', clarificationStatus: 'IN_PROGRESS',
    assistantSequence: sequence, question, result: null,
    sessionStatus: 'CLARIFICATION_IN_PROGRESS',
    workflow: {
      pendingClarificationDimensions: [], retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [], completed: false,
    },
  }
}

function currentTurn(sequence: number, question: string) {
  return { sessionId, dimensionCode: 'EI', assistantSequence: sequence, question }
}

function problem(status: number, code: string) {
  return HttpResponse.json(
    { status, code, title: 'Clarification error', detail: 'Request cannot be processed.' },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

function renderStage(mode: Mode) {
  return renderWithProviders(
    <ClarificationStage session={session(mode)} dimensionCode="EI" mode={mode} />,
  )
}

describe('AI clarification interaction', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
    server.use(
      http.get(`${origin}/api/v1/auth/csrf`, () => HttpResponse.json({
        token: 'test-csrf-token', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf',
      })),
    )
  })

  it('uses an explicit Start and keeps the deterministic Skip fallback on provider 503', async () => {
    const user = userEvent.setup()
    let startCalls = 0
    server.use(http.post(`${base}/start`, ({ request }) => {
      startCalls += 1
      expect(request.headers.get('X-CSRF-TOKEN')).toBe('test-csrf-token')
      return problem(503, 'AI_CLARIFICATION_UNAVAILABLE')
    }))

    renderStage('pending')
    await user.click(screen.getByRole('button', { name: 'Start AI clarification' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('AI clarification is currently unavailable')
    expect(screen.getByRole('button', { name: 'Skip this dimension' })).toBeEnabled()
    expect(startCalls).toBe(1)
  })

  it('recovers an already persisted assistant question on page mount without invoking Start', async () => {
    let reads = 0
    let starts = 0
    server.use(
      http.get(`${base}/current-turn`, () => {
        reads += 1
        return HttpResponse.json(currentTurn(3, 'How do weekends feel to you?'))
      }),
      http.post(`${base}/start`, () => {
        starts += 1
        return HttpResponse.json(interaction(1, 'Should not be called'))
      }),
    )
    renderStage('in-progress')
    expect(await screen.findByText('How do weekends feel to you?')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Your answer' })).toBeInTheDocument()
    expect(reads).toBe(1)
    expect(starts).toBe(0)
  })

  it('binds Continue to the exact assistantSequence and renders the next saved question', async () => {
    const user = userEvent.setup()
    let postedSequence: number | null = null
    let postedAnswer = ''
    server.use(
      http.get(`${base}/current-turn`, () =>
        HttpResponse.json(currentTurn(1, 'How do you recharge?'))),
      http.post(`${base}/messages`, async ({ request }) => {
        const body = await request.json() as { assistantSequence: number; answer: string }
        postedSequence = body.assistantSequence
        postedAnswer = body.answer
        return HttpResponse.json(interaction(3, 'What about social weekends?'))
      }),
    )
    renderStage('in-progress')
    const input = await screen.findByRole('textbox', { name: 'Your answer' })
    await user.type(input, '  I prefer quiet time.  ')
    await user.click(screen.getByRole('button', { name: 'Send answer' }))

    expect(await screen.findByText('What about social weekends?')).toBeInTheDocument()
    expect(postedSequence).toBe(1)
    expect(postedAnswer).toBe('I prefer quiet time.')
    expect(screen.getByRole('textbox', { name: 'Your answer' })).toHaveValue('')
  })

  it('recovers a new turn after 409 without automatically replaying the previous POST', async () => {
    const user = userEvent.setup()
    let latestSequence = 1
    let postCalls = 0
    server.use(
      http.get(`${base}/current-turn`, () =>
        HttpResponse.json(currentTurn(
          latestSequence,
          latestSequence === 1 ? 'First question?' : 'Second question?',
        ))),
      http.post(`${base}/messages`, () => {
        postCalls += 1
        latestSequence = 3
        return problem(409, 'CLARIFICATION_TURN_CONFLICT')
      }),
    )

    renderStage('in-progress')
    await user.type(await screen.findByRole('textbox', { name: 'Your answer' }), 'My response')
    await user.click(screen.getByRole('button', { name: 'Send answer' }))

    expect(await screen.findByText('Second question?')).toBeInTheDocument()
    await waitFor(() => expect(postCalls).toBe(1))
    expect(screen.getByRole('textbox', { name: 'Your answer' })).toHaveValue('')
  })

  it('lets retryable Sessions explicitly retry but never silently retry a 503', async () => {
    const user = userEvent.setup()
    let retryCalls = 0
    server.use(http.post(`${base}/retry`, () => {
      retryCalls += 1
      return problem(503, 'AI_CLARIFICATION_UNAVAILABLE')
    }))
    renderStage('retryable')
    await user.click(screen.getByRole('button', { name: 'Retry AI clarification' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('AI clarification is currently unavailable')
    expect(retryCalls).toBe(1)
    expect(screen.getByRole('button', { name: 'Skip this dimension' })).toBeEnabled()
  })

  it('shows a recoverable state when Current Turn is not answerable yet', async () => {
    server.use(http.get(`${base}/current-turn`, () =>
      problem(409, 'CLARIFICATION_TURN_CONFLICT')))
    renderStage('in-progress')
    expect(await screen.findByRole('alert')).toHaveTextContent('no answerable question yet')
    expect(screen.getByRole('button', { name: 'Refresh current status' })).toBeEnabled()
    expect(screen.queryByRole('button', { name: 'Send answer' })).not.toBeInTheDocument()
  })
})

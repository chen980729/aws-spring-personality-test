import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import { Route, Routes } from 'react-router'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentSessionPage } from './AssessmentSessionPage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

function createSession(
  status:
    | 'IN_PROGRESS'
    | 'AWAITING_CLARIFICATION'
    | 'CLARIFICATION_IN_PROGRESS'
    | 'COMPLETED'
    | 'ABANDONED',
) {
  return {
    id: sessionId,
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.0',
    },
    status,
    questionnaire: {
      answers: [],
      submitted:
        status !== 'IN_PROGRESS',
      submittedAt:
        status !== 'IN_PROGRESS'
          ? '2026-10-03T02:00:00Z'
          : null,
    },
    initialResult:
      status === 'IN_PROGRESS' ||
      status === 'ABANDONED'
        ? null
        : {
            dimensions: [],
          },
    clarifications: [],
    tieBreaks: [],
    finalResult:
      status === 'COMPLETED'
        ? {
            finalType: 'INTJ',
            dimensions: [],
          }
        : null,
    workflow: {
      pendingClarificationDimensions:
        status === 'AWAITING_CLARIFICATION'
          ? ['EI']
          : [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed:
        status === 'COMPLETED',
    },
    createdAt: '2026-10-03T01:00:00Z',
    completedAt:
      status === 'COMPLETED'
        ? '2026-10-03T03:00:00Z'
        : null,
    abandonedAt:
      status === 'ABANDONED'
        ? '2026-10-03T03:00:00Z'
        : null,
  }
}

function renderSessionPage(
  initialEntry: string,
) {
  return renderWithProviders(
    <Routes>
      <Route
        path="/assessment-sessions/:sessionId"
        element={<AssessmentSessionPage />}
      />
    </Routes>,
    {
      initialEntries: [initialEntry],
    },
  )
}

describe('AssessmentSessionPage', () => {
  it('rejects an invalid session ID before making an API request', async () => {
    renderSessionPage(
      '/assessment-sessions/undefined',
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'This assessment session URL is invalid.',
    )
  })

  it('loads questionnaire UI only for an in-progress session', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession('IN_PROGRESS'),
          ),
      ),
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}/questionnaire`,
        () =>
          HttpResponse.json({
            assessment: {
              code: 'SIXTEEN_PERSONALITY',
              version: '1.0',
            },
            questionnaire: {
              questions: [],
              answerScale: [],
            },
            response: {
              answers: [],
              submitted: false,
              submittedAt: null,
            },
          }),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Questionnaire',
      }),
    ).toBeInTheDocument()
  })

  it('shows pending clarification state after questionnaire submission', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession(
              'AWAITING_CLARIFICATION',
            ),
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Clarification required',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'Pending dimensions: EI',
      ),
    ).toBeInTheDocument()
  })

  it('shows an immediate final result when submission completes without clarification', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession('COMPLETED'),
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('INTJ'),
    ).toBeInTheDocument()
  })

  it('explains that an abandoned session cannot be resumed', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession('ABANDONED'),
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'This session was abandoned',
      }),
    ).toBeInTheDocument()
  })

  it('shows a recoverable error when the session cannot be loaded', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            {
              status: 404,
              code: 'ASSESSMENT_SESSION_NOT_FOUND',
            },
            {
              status: 404,
              headers: {
                'Content-Type':
                  'application/problem+json',
              },
            },
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to load this assessment session.',
    )
  })
})

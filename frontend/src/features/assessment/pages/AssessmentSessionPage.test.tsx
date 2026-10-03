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
  const awaiting =
    status === 'AWAITING_CLARIFICATION'

  const clarificationInProgress =
    status ===
    'CLARIFICATION_IN_PROGRESS'

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
            dimensions: [
              {
                dimensionCode: 'EI',
                rawScore: 0,
                questionnairePreference: null,
                ambiguous: true,
                evidence: {
                  poleA: 'E',
                  poleAPercentage: 50,
                  poleB: 'I',
                  poleBPercentage: 50,
                },
              },
            ],
          },
    clarifications:
      awaiting
        ? [
            {
              dimensionCode: 'EI',
              status: 'PENDING',
              result: null,
              startedAt: null,
              acceptedAt: null,
            },
          ]
        : clarificationInProgress
          ? [
              {
                dimensionCode: 'EI',
                status: 'IN_PROGRESS',
                result: null,
                startedAt:
                  '2026-10-03T02:10:00Z',
                acceptedAt: null,
              },
            ]
          : [],
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
        awaiting ? ['EI'] : [],
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
  } as const
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

  it('shows the first pending clarification from the authoritative read model', async () => {
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

    expect(
      screen.getByText(
        'E: 50.0% · I: 50.0%',
      ),
    ).toBeInTheDocument()
  })

  it('derives the active clarification dimension for CLARIFICATION_IN_PROGRESS', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession(
              'CLARIFICATION_IN_PROGRESS',
            ),
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Clarification in progress',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('EI'),
    ).toBeInTheDocument()
  })

  it('shows an immediate final result when the session is completed', async () => {
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

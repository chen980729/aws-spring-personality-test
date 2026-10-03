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
  status: 'IN_PROGRESS' | 'ABANDONED',
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
      submitted: false,
      submittedAt: null,
    },
    initialResult: null,
    clarifications: [],
    tieBreaks: [],
    finalResult: null,
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed: false,
    },
    createdAt: '2026-10-03T01:00:00Z',
    completedAt: null,
    abandonedAt:
      status === 'ABANDONED'
        ? '2026-10-03T02:00:00Z'
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

    expect(
      screen.getByRole('link', {
        name: 'Return to assessments',
      }),
    ).toHaveAttribute(
      'href',
      '/assessments',
    )
  })

  it('loads the authoritative assessment session from the backend', async () => {
    server.use(
      http.get(
        `${origin}/api/v1/assessment-sessions/${sessionId}`,
        () =>
          HttpResponse.json(
            createSession('IN_PROGRESS'),
          ),
      ),
    )

    renderSessionPage(
      `/assessment-sessions/${sessionId}`,
    )

    expect(
      screen.getByText('Loading session...'),
    ).toBeInTheDocument()

    expect(
      await screen.findByText('IN_PROGRESS'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('SIXTEEN_PERSONALITY'),
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

    expect(
      screen.getByText(
        'A new assessment was started, so this session can no longer be resumed.',
      ),
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

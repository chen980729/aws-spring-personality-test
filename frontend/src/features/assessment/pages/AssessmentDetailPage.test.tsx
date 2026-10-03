import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import {
  Route,
  Routes,
  useNavigate,
} from 'react-router'

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import {
  createTestQueryClient,
  renderWithProviders,
} from '../../../test/render'
import { server } from '../../../test/msw/server'
import { assessmentQueryKeys } from '../api/assessmentQueryKeys'
import { AssessmentDetailPage } from './AssessmentDetailPage'

const origin = 'http://localhost:5173'
const assessmentCode = 'SIXTEEN_PERSONALITY'

const detailsEndpoint =
  `${origin}/api/v1/assessments/${assessmentCode}`

const activeSessionEndpoint =
  `${detailsEndpoint}/sessions/active`

const startSessionEndpoint =
  `${detailsEndpoint}/sessions`

const assessmentDetails = {
  code: assessmentCode,
  name: 'Sixteen Personality Assessment',
  version: '1.0',
  questionCount: 48,
  questionnaire: {
    questions: [],
    answerScale: [],
  },
}

function createSession(
  id: string,
  status = 'IN_PROGRESS',
) {
  return {
    id,
    assessment: {
      code: assessmentCode,
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
    abandonedAt: null,
  }
}

const activeSession = createSession('session-123')
const newSession = createSession('session-456')

function restartResponse() {
  return {
    abandonedSessionId: activeSession.id,
    session: newSession,
  }
}

function BackButton() {
  const navigate = useNavigate()

  return (
    <button
      type="button"
      onClick={() => navigate(-1)}
    >
      Go back
    </button>
  )
}

function renderDetailPage(
  queryClient = createTestQueryClient(),
) {
  return renderWithProviders(
    <Routes>
      <Route
        path="/assessments"
        element={<p>Catalog destination</p>}
      />
      <Route
        path="/assessments/:assessmentCode"
        element={<AssessmentDetailPage />}
      />
      <Route
        path="/assessment-sessions/:sessionId"
        element={
          <>
            <p>Session destination</p>
            <BackButton />
          </>
        }
      />
    </Routes>,
    {
      initialEntries: [
        '/assessments',
        `/assessments/${assessmentCode}`,
      ],
      queryClient,
    },
  )
}

describe('AssessmentDetailPage', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('offers a start action when no active session exists', async () => {
    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
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

    renderDetailPage()

    expect(
      await screen.findByRole('heading', {
        name: 'Sixteen Personality Assessment',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Start assessment',
      }),
    ).toBeInTheDocument()
  })

  it('starts an assessment and navigates to the returned canonical session route', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
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
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'assessment-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        startSessionEndpoint,
        ({ request }) => {
          expect(
            request.headers.get('X-CSRF-TOKEN'),
          ).toBe('assessment-csrf')

          return HttpResponse.json(
            {
              created: true,
              session: activeSession,
            },
            {
              status: 201,
            },
          )
        },
      ),
    )

    renderDetailPage()

    await user.click(
      await screen.findByRole('button', {
        name: 'Start assessment',
      }),
    )

    expect(
      await screen.findByText('Session destination'),
    ).toBeInTheDocument()
  })

  it('uses the returned session even when the backend reports that it already existed', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
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
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'assessment-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        startSessionEndpoint,
        () =>
          HttpResponse.json({
            created: false,
            session: activeSession,
          }),
      ),
    )

    renderDetailPage()

    await user.click(
      await screen.findByRole('button', {
        name: 'Start assessment',
      }),
    )

    expect(
      await screen.findByText('Session destination'),
    ).toBeInTheDocument()
  })

  it('offers resume and start-new actions when an active session exists', async () => {
    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
        HttpResponse.json(activeSession),
      ),
    )

    renderDetailPage()

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment in progress',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('link', {
        name: 'Resume assessment',
      }),
    ).toHaveAttribute(
      'href',
      '/assessment-sessions/session-123',
    )

    expect(
      screen.getByRole('button', {
        name: 'Start new assessment',
      }),
    ).toBeInTheDocument()
  })

  it('requires confirmation before restarting the active assessment', async () => {
    const user = userEvent.setup()
    let restartRequestCount = 0

    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
        HttpResponse.json(activeSession),
      ),
      http.post(
        `${origin}/api/v1/assessment-sessions/session-123/restart`,
        () => {
          restartRequestCount += 1
          return HttpResponse.json(restartResponse(), {
            status: 201,
          })
        },
      ),
    )

    renderDetailPage()

    await user.click(
      await screen.findByRole('button', {
        name: 'Start new assessment',
      }),
    )

    expect(restartRequestCount).toBe(0)

    expect(
      screen.getByRole('heading', {
        name: 'Start over?',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'Your current assessment progress will be abandoned and cannot be resumed.',
      ),
    ).toBeInTheDocument()
  })

  it('restarts the assessment, consumes the backend wrapper, updates the active session cache, and replaces navigation', async () => {
    const user = userEvent.setup()
    const queryClient = createTestQueryClient()

    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
        HttpResponse.json(activeSession),
      ),
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'restart-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        `${origin}/api/v1/assessment-sessions/session-123/restart`,
        ({ request }) => {
          expect(
            request.headers.get('X-CSRF-TOKEN'),
          ).toBe('restart-csrf')

          return HttpResponse.json(restartResponse(), {
            status: 201,
            headers: {
              Location:
                '/api/v1/assessment-sessions/session-456',
            },
          })
        },
      ),
    )

    renderDetailPage(queryClient)

    await user.click(
      await screen.findByRole('button', {
        name: 'Start new assessment',
      }),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm start new assessment',
      }),
    )

    expect(
      await screen.findByText('Session destination'),
    ).toBeInTheDocument()

    expect(
      queryClient.getQueryData(
        assessmentQueryKeys.activeSession(
          assessmentCode,
        ),
      ),
    ).toEqual(newSession)

    await user.click(
      screen.getByRole('button', {
        name: 'Go back',
      }),
    )

    expect(
      await screen.findByText('Catalog destination'),
    ).toBeInTheDocument()
  })

  it('keeps the confirmation visible when restart fails', async () => {
    const user = userEvent.setup()

    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(assessmentDetails),
      ),
      http.get(activeSessionEndpoint, () =>
        HttpResponse.json(activeSession),
      ),
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'restart-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.post(
        `${origin}/api/v1/assessment-sessions/session-123/restart`,
        () =>
          HttpResponse.json(
            {
              status: 500,
            },
            {
              status: 500,
            },
          ),
      ),
    )

    renderDetailPage()

    await user.click(
      await screen.findByRole('button', {
        name: 'Start new assessment',
      }),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm start new assessment',
      }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to start a new assessment right now.',
    )

    expect(
      screen.getByRole('heading', {
        name: 'Start over?',
      }),
    ).toBeInTheDocument()
  })

  it('does not treat assessment-not-found as an absent active session', async () => {
    server.use(
      http.get(detailsEndpoint, () =>
        HttpResponse.json(
          {
            status: 404,
            code: 'ASSESSMENT_NOT_FOUND',
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
      http.get(activeSessionEndpoint, () =>
        HttpResponse.json(
          {
            status: 404,
            code: 'ASSESSMENT_NOT_FOUND',
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

    renderDetailPage()

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to load this assessment right now.',
    )
  })
})

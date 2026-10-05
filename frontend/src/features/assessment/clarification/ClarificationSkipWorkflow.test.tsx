import {
  screen,
  waitFor,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import { Route, Routes } from 'react-router'

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentSessionPage } from '../pages/AssessmentSessionPage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

const sessionEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}`

function dimensionResult(
  dimensionCode: string,
  poleA: string,
  poleB: string,
) {
  return {
    dimensionCode,
    rawScore: 0,
    questionnairePreference: null,
    ambiguous: true,
    evidence: {
      poleA,
      poleAPercentage: 50,
      poleB,
      poleBPercentage: 50,
    },
  }
}

function clarification(
  dimensionCode: string,
  status:
    | 'PENDING'
    | 'SKIPPED'
    | 'FAILED_RETRYABLE',
) {
  return {
    dimensionCode,
    status,
    result: null,
    startedAt: null,
    acceptedAt: null,
  }
}

function awaitingSession(
  clarifications: Array<
    ReturnType<typeof clarification>
  >,
  pending: string[],
  retryable: string[] = [],
  tieBreakRequired: string[] = [],
) {
  return {
    id: sessionId,
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.0',
    },
    status: 'AWAITING_CLARIFICATION',
    questionnaire: {
      answers: [],
      submitted: true,
      submittedAt:
        '2026-10-03T06:00:00Z',
    },
    initialResult: {
      dimensions: [
        dimensionResult(
          'EI',
          'E',
          'I',
        ),
        dimensionResult(
          'SN',
          'S',
          'N',
        ),
      ],
    },
    clarifications,
    tieBreaks: [],
    finalResult: null,
    workflow: {
      pendingClarificationDimensions:
        pending,
      retryableClarificationDimensions:
        retryable,
      tieBreakRequiredDimensions:
        tieBreakRequired,
      completed: false,
    },
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

function completedSession() {
  return {
    ...awaitingSession(
      [
        clarification(
          'EI',
          'SKIPPED',
        ),
        clarification(
          'SN',
          'SKIPPED',
        ),
      ],
      [],
    ),
    status: 'COMPLETED',
    finalResult: {
      finalType: 'INTJ',
      dimensions: [],
    },
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed: true,
    },
    completedAt:
      '2026-10-03T07:00:00Z',
  }
}

function installCsrfHandler() {
  server.use(
    http.get(
      `${origin}/api/v1/auth/csrf`,
      () =>
        HttpResponse.json({
          token: 'clarification-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
    ),
  )
}

function renderSessionPage() {
  return renderWithProviders(
    <Routes>
      <Route
        path="/assessment-sessions/:sessionId"
        element={<AssessmentSessionPage />}
      />
    </Routes>,
    {
      initialEntries: [
        `/assessment-sessions/${sessionId}`,
      ],
    },
  )
}

describe('clarification skip workflow', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('skips the current dimension and immediately resolves the next authoritative clarification', async () => {
    const user = userEvent.setup()
    let skipRequestCount = 0

    const initialSession =
      awaitingSession(
        [
          clarification(
            'EI',
            'PENDING',
          ),
          clarification(
            'SN',
            'PENDING',
          ),
        ],
        ['EI', 'SN'],
      )

    const afterSkip =
      awaitingSession(
        [
          clarification(
            'EI',
            'SKIPPED',
          ),
          clarification(
            'SN',
            'PENDING',
          ),
        ],
        ['SN'],
      )

    installCsrfHandler()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            initialSession,
          ),
      ),
      http.post(
        `${sessionEndpoint}/clarifications/EI/skip`,
        ({ request }) => {
          skipRequestCount += 1

          expect(
            request.headers.get(
              'X-CSRF-TOKEN',
            ),
          ).toBe(
            'clarification-csrf',
          )

          return HttpResponse.json(
            afterSkip,
          )
        },
      ),
    )

    renderSessionPage()

    expect(
      await screen.findByText(
        'Current dimension',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText('EI'),
    ).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', {
        name: 'Skip this dimension',
      }),
    )

    expect(skipRequestCount).toBe(0)

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm skip EI',
      }),
    )

    await waitFor(() => {
      expect(skipRequestCount).toBe(1)
    })

    expect(
      await screen.findByText(
        'Pending dimensions: SN',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText('SN'),
    ).toBeInTheDocument()
  })

  it('skips all remaining clarification work and transitions to completed when the backend finalizes the session', async () => {
    const user = userEvent.setup()

    const initialSession =
      awaitingSession(
        [
          clarification(
            'EI',
            'PENDING',
          ),
          clarification(
            'SN',
            'FAILED_RETRYABLE',
          ),
        ],
        ['EI'],
        ['SN'],
      )

    installCsrfHandler()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            initialSession,
          ),
      ),
      http.post(
        `${sessionEndpoint}/clarifications/skip-remaining`,
        () =>
          HttpResponse.json(
            completedSession(),
          ),
      ),
    )

    renderSessionPage()

    await user.click(
      await screen.findByRole(
        'button',
        {
          name: 'Skip all remaining clarifications',
        },
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm skip all remaining',
      }),
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

  it('moves to tie-break when skip-remaining leaves an exact tie unresolved', async () => {
    const user = userEvent.setup()

    const initialSession =
      awaitingSession(
        [
          clarification(
            'EI',
            'PENDING',
          ),
        ],
        ['EI'],
      )

    const tieBreakSession =
      awaitingSession(
        [
          clarification(
            'EI',
            'SKIPPED',
          ),
        ],
        [],
        [],
        ['EI'],
      )

    installCsrfHandler()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            initialSession,
          ),
      ),
      http.post(
        `${sessionEndpoint}/clarifications/skip-remaining`,
        () =>
          HttpResponse.json(
            tieBreakSession,
          ),
      ),
    )

    renderSessionPage()

    await user.click(
      await screen.findByRole(
        'button',
        {
          name: 'Skip all remaining clarifications',
        },
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm skip all remaining',
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Tie-break required',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('group', {
        name: 'Choose one preference for EI',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'E — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'I — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()
  })
})

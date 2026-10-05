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

function dimension(
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

function tieBreakSession(
  requiredDimensions: string[],
  tieBreaks: Array<{
    dimensionCode: string
    selectedPole: string
    decidedAt: string
  }> = [],
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
        dimension('EI', 'E', 'I'),
        dimension('SN', 'S', 'N'),
      ],
    },
    clarifications: [
      {
        dimensionCode: 'EI',
        status: 'SKIPPED',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
      {
        dimensionCode: 'SN',
        status: 'SKIPPED',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ],
    tieBreaks,
    finalResult: null,
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions:
        requiredDimensions,
      completed: false,
    },
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

function contextualTieBreakSession() {
  return {
    ...tieBreakSession(['EI']),
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.1',
    },
    tieBreaks: [],
  }
}

function completedSession() {
  return {
    ...tieBreakSession(
      [],
      [
        {
          dimensionCode: 'EI',
          selectedPole: 'I',
          decidedAt:
            '2026-10-03T07:00:00Z',
        },
      ],
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

function completedContextualSession() {
  return {
    ...contextualTieBreakSession(),
    status: 'COMPLETED',
    tieBreaks: [
      {
        dimensionCode: 'EI',
        questionId: 'TB-EI-1',
        selectedOptionId: 'TB-EI-02',
        decidedAt:
          '2026-10-05T07:00:00Z',
      },
    ],
    finalResult: {
      finalType: 'INTJ',
      dimensions: [
        {
          dimensionCode: 'EI',
          questionnairePreference: null,
          finalPreference: 'I',
          source: 'TIE_BREAK_QUESTION',
          overrodeBaseline: false,
        },
      ],
    },
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed: true,
    },
    completedAt:
      '2026-10-05T07:00:00Z',
  }
}

function installCsrfHandler() {
  server.use(
    http.get(
      `${origin}/api/v1/auth/csrf`,
      () =>
        HttpResponse.json({
          token: 'tie-break-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
    ),
  )
}

function installContextualEiInteraction() {
  server.use(
    http.get(
      `${sessionEndpoint}/tie-breaks/EI`,
      () =>
        HttpResponse.json({
          interactionType:
            'CONTEXTUAL_QUESTION',
          dimensionCode: 'EI',
          questionId: 'TB-EI-1',
          instruction:
            'Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.',
          prompt:
            'When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?',
          options: [
            {
              optionId: 'TB-EI-01',
              text: 'I start discussing it with someone and often discover what I think while talking.',
            },
            {
              optionId: 'TB-EI-02',
              text: 'I first spend some time thinking it through privately, then share my thoughts once they have taken shape.',
            },
          ],
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

async function chooseAndConfirm(
  user: ReturnType<
    typeof userEvent.setup
  >,
  pole: string,
  dimensionCode: string,
) {
  await user.click(
    await screen.findByLabelText(
      `${pole} — questionnaire evidence 50.0%`,
    ),
  )

  await user.click(
    screen.getByRole('button', {
      name: 'Review tie-break decision',
    }),
  )

  await user.click(
    screen.getByRole('button', {
      name: `Confirm ${pole} for ${dimensionCode}`,
    }),
  )
}

describe('tie-break workflow', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('submits the selected legacy pole with CSRF and transitions to the completed result returned by the backend', async () => {
    const user = userEvent.setup()
    let receivedBody: unknown

    installCsrfHandler()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            tieBreakSession(['EI']),
          ),
      ),
      http.put(
        `${sessionEndpoint}/tie-breaks/EI`,
        async ({ request }) => {
          expect(
            request.headers.get(
              'X-CSRF-TOKEN',
            ),
          ).toBe(
            'tie-break-csrf',
          )

          receivedBody =
            await request.json()

          return HttpResponse.json(
            completedSession(),
          )
        },
      ),
    )

    renderSessionPage()

    expect(
      await screen.findByRole('heading', {
        name: 'Tie-break required',
      }),
    ).toBeInTheDocument()

    await chooseAndConfirm(
      user,
      'I',
      'EI',
    )

    await waitFor(() => {
      expect(receivedBody).toEqual({
        selectedPole: 'I',
      })
    })

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('INTJ'),
    ).toBeInTheDocument()
  })

  it('uses the authoritative response to advance to the next required legacy tie-break', async () => {
    const user = userEvent.setup()

    const initial =
      tieBreakSession([
        'EI',
        'SN',
      ])

    const afterEi =
      tieBreakSession(
        ['SN'],
        [
          {
            dimensionCode: 'EI',
            selectedPole: 'I',
            decidedAt:
              '2026-10-03T07:00:00Z',
          },
        ],
      )

    installCsrfHandler()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(initial),
      ),
      http.put(
        `${sessionEndpoint}/tie-breaks/EI`,
        () =>
          HttpResponse.json(afterEi),
      ),
    )

    renderSessionPage()

    await screen.findByText(
      'Choose one preference for EI',
    )

    await chooseAndConfirm(
      user,
      'I',
      'EI',
    )

    expect(
      await screen.findByText(
        'Choose one preference for SN',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'S — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'N — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()

    const reviewButton =
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      })

    expect(reviewButton).toBeDisabled()

    expect(
      screen.queryByRole('button', {
        name: /Confirm .* for SN/,
      }),
    ).not.toBeInTheDocument()

    await user.click(
      screen.getByLabelText(
        'N — questionnaire evidence 50.0%',
      ),
    )

    expect(reviewButton).toBeEnabled()

    await user.click(reviewButton)

    expect(
      screen.getByRole('button', {
        name: 'Confirm N for SN',
      }),
    ).toBeInTheDocument()
  })

  it('keeps the selected legacy pole available for a safe same-value retry after an uncertain failure', async () => {
    const user = userEvent.setup()
    let requestCount = 0

    installCsrfHandler()
    installLegacyInteraction(
      'EI',
      ['E', 'I'],
    )

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            tieBreakSession(['EI']),
          ),
      ),
      http.put(
        `${sessionEndpoint}/tie-breaks/EI`,
        async ({ request }) => {
          requestCount += 1

          const body =
            await request.json()

          if (requestCount === 1) {
            return HttpResponse.json(
              {
                status: 500,
              },
              {
                status: 500,
              },
            )
          }

          expect(body).toEqual({
            selectedPole: 'I',
          })

          return HttpResponse.json(
            completedSession(),
          )
        },
      ),
    )

    renderSessionPage()

    await screen.findByText(
      'Choose one preference for EI',
    )

    await chooseAndConfirm(
      user,
      'I',
      'EI',
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      'Unable to save the tie-break decision right now.',
    )

    expect(
      screen.getByLabelText(
        'I — questionnaire evidence 50.0%',
      ),
    ).toBeChecked()

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm I for EI',
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(requestCount).toBe(2)
  })

  it('submits a contextual option without exposing pole mapping and transitions to the completed result', async () => {
    const user = userEvent.setup()
    let receivedBody: unknown

    installCsrfHandler()
    installContextualEiInteraction()

    server.use(
      http.get(
        sessionEndpoint,
        () =>
          HttpResponse.json(
            contextualTieBreakSession(),
          ),
      ),
      http.put(
        `${sessionEndpoint}/tie-breaks/EI`,
        async ({ request }) => {
          expect(
            request.headers.get(
              'X-CSRF-TOKEN',
            ),
          ).toBe(
            'tie-break-csrf',
          )

          receivedBody =
            await request.json()

          return HttpResponse.json(
            completedContextualSession(),
          )
        },
      ),
    )

    renderSessionPage()

    expect(
      await screen.findByRole('heading', {
        name: 'When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?',
      }),
    ).toBeInTheDocument()

    expect(
      screen.queryByLabelText(
        /questionnaire evidence/,
      ),
    ).not.toBeInTheDocument()

    await user.click(
      screen.getByLabelText(
        'I first spend some time thinking it through privately, then share my thoughts once they have taken shape.',
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      }),
    )

    expect(
      screen.getByRole('button', {
        name: 'Confirm answer',
      }),
    ).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', {
        name: 'Confirm answer',
      }),
    )

    await waitFor(() => {
      expect(receivedBody).toEqual({
        questionId: 'TB-EI-1',
        selectedOptionId:
          'TB-EI-02',
      })
    })

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('INTJ'),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'Tie-break question',
      ),
    ).toBeInTheDocument()
  })
})

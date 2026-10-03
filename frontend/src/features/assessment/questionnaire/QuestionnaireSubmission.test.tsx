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
import {
  createTestQueryClient,
  renderWithProviders,
} from '../../../test/render'
import { server } from '../../../test/msw/server'
import { AssessmentSessionPage } from '../pages/AssessmentSessionPage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

const sessionEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}`

const questionnaireEndpoint =
  `${sessionEndpoint}/questionnaire`

const submissionEndpoint =
  `${questionnaireEndpoint}/submission`

function sessionResponse(
  status:
    | 'IN_PROGRESS'
    | 'AWAITING_CLARIFICATION'
    | 'COMPLETED',
  answers: Array<{
    questionId: string
    value: number
  }>,
) {
  return {
    id: sessionId,
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.0',
    },
    status,
    questionnaire: {
      answers,
      submitted:
        status !== 'IN_PROGRESS',
      submittedAt:
        status !== 'IN_PROGRESS'
          ? '2026-10-03T06:00:00Z'
          : null,
    },
    initialResult:
      status === 'IN_PROGRESS'
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
    createdAt: '2026-10-03T05:00:00Z',
    completedAt:
      status === 'COMPLETED'
        ? '2026-10-03T06:00:00Z'
        : null,
    abandonedAt: null,
  }
}

const questionnaireResponse = {
  assessment: {
    code: 'SIXTEEN_PERSONALITY',
    version: '1.0',
  },
  questionnaire: {
    questions: [
      {
        questionId: 'Q01',
        position: 1,
        prompt: 'Question one',
      },
      {
        questionId: 'Q02',
        position: 2,
        prompt: 'Question two',
      },
    ],
    answerScale: [
      {
        value: 1,
        label: 'Strongly disagree',
      },
      {
        value: 2,
        label: 'Disagree',
      },
      {
        value: 3,
        label: 'Neutral',
      },
      {
        value: 4,
        label: 'Agree',
      },
      {
        value: 5,
        label: 'Strongly agree',
      },
    ],
  },
  response: {
    answers: [
      {
        questionId: 'Q01',
        value: 4,
      },
    ],
    submitted: false,
    submittedAt: null,
  },
}

function installInitialReadHandlers() {
  server.use(
    http.get(
      sessionEndpoint,
      () =>
        HttpResponse.json(
          sessionResponse(
            'IN_PROGRESS',
            questionnaireResponse.response.answers,
          ),
        ),
    ),
    http.get(
      questionnaireEndpoint,
      () =>
        HttpResponse.json(
          questionnaireResponse,
        ),
    ),
    http.get(
      `${origin}/api/v1/auth/csrf`,
      () =>
        HttpResponse.json({
          token: 'submission-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
    ),
  )
}

function renderSessionPage(
  queryClient = createTestQueryClient(),
) {
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
      queryClient,
    },
  )
}

describe('questionnaire submission', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('keeps submission disabled until every question is answered', async () => {
    installInitialReadHandlers()

    renderSessionPage()

    const submitButton =
      await screen.findByRole('button', {
        name: 'Submit questionnaire',
      })

    expect(submitButton).toBeDisabled()

    expect(
      screen.getByText(
        'Answer every question before submitting.',
      ),
    ).toBeInTheDocument()
  })

  it('submits the complete latest snapshot without waiting for the debounce autosave timer', async () => {
    const user = userEvent.setup()
    let autosaveRequestCount = 0
    let submittedBody: unknown

    installInitialReadHandlers()

    server.use(
      http.put(
        questionnaireEndpoint,
        () => {
          autosaveRequestCount += 1

          return HttpResponse.json(
            sessionResponse(
              'IN_PROGRESS',
              questionnaireResponse.response.answers,
            ),
          )
        },
      ),
      http.post(
        submissionEndpoint,
        async ({ request }) => {
          submittedBody =
            await request.json()

          return HttpResponse.json(
            sessionResponse(
              'AWAITING_CLARIFICATION',
              [
                {
                  questionId: 'Q01',
                  value: 4,
                },
                {
                  questionId: 'Q02',
                  value: 5,
                },
              ],
            ),
          )
        },
      ),
    )

    renderSessionPage()

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Submit questionnaire',
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Clarification required',
      }),
    ).toBeInTheDocument()

    expect(submittedBody).toEqual({
      answers: [
        {
          questionId: 'Q01',
          value: 4,
        },
        {
          questionId: 'Q02',
          value: 5,
        },
      ],
    })

    expect(autosaveRequestCount).toBe(0)
  })

  it('waits for an already in-flight autosave before sending the final submission', async () => {
    const user = userEvent.setup()
    let releaseAutosave:
      | (() => void)
      | undefined

    const autosaveGate =
      new Promise<void>((resolve) => {
        releaseAutosave = resolve
      })

    let submissionRequestCount = 0

    installInitialReadHandlers()

    server.use(
      http.put(
        questionnaireEndpoint,
        async ({ request }) => {
          const body =
            await request.json()

          await autosaveGate

          return HttpResponse.json(
            sessionResponse(
              'IN_PROGRESS',
              (
                body as {
                  answers: Array<{
                    questionId: string
                    value: number
                  }>
                }
              ).answers,
            ),
          )
        },
      ),
      http.post(
        submissionEndpoint,
        async ({ request }) => {
          submissionRequestCount += 1

          const body =
            (await request.json()) as {
              answers: Array<{
                questionId: string
                value: number
              }>
            }

          return HttpResponse.json(
            sessionResponse(
              'COMPLETED',
              body.answers,
            ),
          )
        },
      ),
    )

    renderSessionPage()

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q01"]',
        },
      ),
    )

    await waitFor(
      () => {
        expect(
          screen.getByText('Saving...'),
        ).toBeInTheDocument()
      },
      {
        timeout: 2000,
      },
    )

    await user.click(
      screen.getByLabelText(
        'Agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Submit questionnaire',
      }),
    )

    expect(submissionRequestCount).toBe(0)

    releaseAutosave?.()

    expect(
      await screen.findByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(submissionRequestCount).toBe(1)
  })

  it('recovers authoritative session state when a committed submission is retried', async () => {
    const user = userEvent.setup()
    let recoveryGetCount = 0

    installInitialReadHandlers()

    server.use(
      http.get(
        sessionEndpoint,
        () => {
          recoveryGetCount += 1

          if (recoveryGetCount === 1) {
            return HttpResponse.json(
              sessionResponse(
                'IN_PROGRESS',
                questionnaireResponse.response.answers,
              ),
            )
          }

          return HttpResponse.json(
            sessionResponse(
              'COMPLETED',
              [
                {
                  questionId: 'Q01',
                  value: 4,
                },
                {
                  questionId: 'Q02',
                  value: 5,
                },
              ],
            ),
          )
        },
      ),
      http.post(
        submissionEndpoint,
        () =>
          HttpResponse.json(
            {
              status: 409,
              code: 'ASSESSMENT_ALREADY_SUBMITTED',
            },
            {
              status: 409,
              headers: {
                'Content-Type':
                  'application/problem+json',
              },
            },
          ),
      ),
    )

    renderSessionPage()

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Submit questionnaire',
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

    expect(recoveryGetCount).toBe(2)
  })
})

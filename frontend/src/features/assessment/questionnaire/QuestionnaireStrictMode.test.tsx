import { StrictMode } from 'react'
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

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import { QuestionnaireStage } from './QuestionnaireStage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

const questionnaireEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}/questionnaire`

const submissionEndpoint =
  `${questionnaireEndpoint}/submission`

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

function sessionResponse(
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
    status: 'IN_PROGRESS',
    questionnaire: {
      answers,
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
    createdAt: '2026-10-03T05:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

function installCommonHandlers() {
  server.use(
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
          token: 'strict-mode-csrf',
          headerName: 'X-CSRF-TOKEN',
          parameterName: '_csrf',
        }),
    ),
  )
}

function renderStrictModeQuestionnaire() {
  return renderWithProviders(
    <StrictMode>
      <QuestionnaireStage
        sessionId={sessionId}
      />
    </StrictMode>,
  )
}

describe('QuestionnaireStage in React StrictMode', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('continues autosaving after the StrictMode effect replay', async () => {
    const user = userEvent.setup()
    let saveRequestCount = 0

    installCommonHandlers()

    server.use(
      http.put(
        questionnaireEndpoint,
        async ({ request }) => {
          saveRequestCount += 1

          const body =
            (await request.json()) as {
              answers: Array<{
                questionId: string
                value: number
              }>
            }

          return HttpResponse.json(
            sessionResponse(body.answers),
          )
        },
      ),
    )

    renderStrictModeQuestionnaire()

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    await waitFor(
      () => {
        expect(saveRequestCount).toBe(1)
      },
      {
        timeout: 2000,
      },
    )

    expect(
      await screen.findByText('Saved'),
    ).toBeInTheDocument()
  })

  it('can submit a complete questionnaire after the StrictMode effect replay', async () => {
    const user = userEvent.setup()
    let submissionRequestCount = 0

    installCommonHandlers()

    server.use(
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

          return HttpResponse.json({
            ...sessionResponse(body.answers),
            status: 'AWAITING_CLARIFICATION',
            questionnaire: {
              answers: body.answers,
              submitted: true,
              submittedAt:
                '2026-10-03T06:00:00Z',
            },
            initialResult: {
              dimensions: [],
            },
            workflow: {
              pendingClarificationDimensions: [
                'EI',
              ],
              retryableClarificationDimensions: [],
              tieBreakRequiredDimensions: [],
              completed: false,
            },
          })
        },
      ),
    )

    renderStrictModeQuestionnaire()

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    const submitButton =
      screen.getByRole('button', {
        name: 'Submit questionnaire',
      })

    expect(submitButton).toBeEnabled()

    await user.click(submitButton)

    await waitFor(() => {
      expect(submissionRequestCount).toBe(1)
    })
  })
})

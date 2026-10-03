import {
  screen,
  waitFor,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import {
  createTestQueryClient,
  renderWithProviders,
} from '../../../test/render'
import { server } from '../../../test/msw/server'
import { assessmentQueryKeys } from '../api/assessmentQueryKeys'
import { QuestionnaireStage } from './QuestionnaireStage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

const questionnaireEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}/questionnaire`

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
        prompt: 'I enjoy meeting new people.',
      },
      {
        questionId: 'Q02',
        position: 2,
        prompt: 'I prefer quiet reflection.',
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
    createdAt: '2026-10-03T01:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

function installQuestionnaireReadHandler() {
  server.use(
    http.get(
      questionnaireEndpoint,
      () =>
        HttpResponse.json(
          questionnaireResponse,
        ),
    ),
  )
}

describe('QuestionnaireStage', () => {
  it('renders the session-bound questionnaire and restores persisted answers into the local draft', async () => {
    installQuestionnaireReadHandler()

    renderWithProviders(
      <QuestionnaireStage
        sessionId={sessionId}
      />,
    )

    expect(
      screen.getByText('Loading questionnaire...'),
    ).toBeInTheDocument()

    expect(
      await screen.findByText(
        '1. I enjoy meeting new people.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText('Answered 1 of 2'),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText('Agree', {
        selector: 'input[name="Q01"]',
      }),
    ).toBeChecked()

    expect(
      screen.getByText('Saved'),
    ).toBeInTheDocument()
  })

  it('keeps immediate edits in local draft state before the debounce save runs', async () => {
    const user = userEvent.setup()
    const queryClient = createTestQueryClient()

    installQuestionnaireReadHandler()

    renderWithProviders(
      <QuestionnaireStage
        sessionId={sessionId}
      />,
      {
        queryClient,
      },
    )

    const secondQuestionAgree =
      await screen.findByLabelText(
        'Agree',
        {
          selector: 'input[name="Q02"]',
        },
      )

    await user.click(secondQuestionAgree)

    expect(secondQuestionAgree).toBeChecked()

    expect(
      screen.getByText('Answered 2 of 2'),
    ).toBeInTheDocument()

    expect(
      screen.getByText('Unsaved changes'),
    ).toBeInTheDocument()

    const cachedQuestionnaire =
      queryClient.getQueryData(
        assessmentQueryKeys.questionnaire(
          sessionId,
        ),
      ) as typeof questionnaireResponse

    expect(
      cachedQuestionnaire.response.answers,
    ).toEqual([
      {
        questionId: 'Q01',
        value: 4,
      },
    ])
  })

  it('debounces changes and saves the complete questionnaire snapshot in canonical question order', async () => {
    const user = userEvent.setup()
    const receivedSnapshots: unknown[] = []

    installQuestionnaireReadHandler()

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'questionnaire-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.put(
        questionnaireEndpoint,
        async ({ request }) => {
          const body =
            await request.json()

          receivedSnapshots.push(body)

          return HttpResponse.json(
            sessionResponse([
              {
                questionId: 'Q01',
                value: 4,
              },
              {
                questionId: 'Q02',
                value: 5,
              },
            ]),
          )
        },
      ),
    )

    renderWithProviders(
      <QuestionnaireStage
        sessionId={sessionId}
      />,
    )

    await user.click(
      await screen.findByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    expect(receivedSnapshots).toHaveLength(0)

    await waitFor(
      () => {
        expect(receivedSnapshots).toHaveLength(1)
      },
      {
        timeout: 2000,
      },
    )

    expect(receivedSnapshots[0]).toEqual({
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

    expect(
      await screen.findByText('Saved'),
    ).toBeInTheDocument()
  })

  it('serializes saves so an older snapshot cannot finish after a newer concurrent request', async () => {
    const user = userEvent.setup()
    const receivedSnapshots: Array<{
      answers: Array<{
        questionId: string
        value: number
      }>
    }> = []

    let releaseFirstSave:
      | (() => void)
      | undefined

    const firstSaveGate =
      new Promise<void>((resolve) => {
        releaseFirstSave = resolve
      })

    installQuestionnaireReadHandler()

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'questionnaire-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.put(
        questionnaireEndpoint,
        async ({ request }) => {
          const body =
            (await request.json()) as {
              answers: Array<{
                questionId: string
                value: number
              }>
            }

          receivedSnapshots.push(body)

          if (receivedSnapshots.length === 1) {
            await firstSaveGate
          }

          return HttpResponse.json(
            sessionResponse(body.answers),
          )
        },
      ),
    )

    renderWithProviders(
      <QuestionnaireStage
        sessionId={sessionId}
      />,
    )

    await user.click(
      await screen.findByLabelText(
        'Agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    await waitFor(
      () => {
        expect(receivedSnapshots).toHaveLength(1)
      },
      {
        timeout: 2000,
      },
    )

    await user.click(
      screen.getByLabelText(
        'Strongly agree',
        {
          selector: 'input[name="Q02"]',
        },
      ),
    )

    expect(receivedSnapshots).toHaveLength(1)

    releaseFirstSave?.()

    await waitFor(
      () => {
        expect(receivedSnapshots).toHaveLength(2)
      },
      {
        timeout: 2000,
      },
    )

    expect(receivedSnapshots[0]).toEqual({
      answers: [
        {
          questionId: 'Q01',
          value: 4,
        },
        {
          questionId: 'Q02',
          value: 4,
        },
      ],
    })

    expect(receivedSnapshots[1]).toEqual({
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

    expect(
      await screen.findByText('Saved'),
    ).toBeInTheDocument()
  })

  it('keeps the local draft and allows an explicit retry after autosave failure', async () => {
    const user = userEvent.setup()
    let saveRequestCount = 0

    installQuestionnaireReadHandler()

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'questionnaire-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
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

          if (saveRequestCount === 1) {
            return HttpResponse.json(
              {
                status: 500,
              },
              {
                status: 500,
              },
            )
          }

          return HttpResponse.json(
            sessionResponse(body.answers),
          )
        },
      ),
    )

    renderWithProviders(
      <QuestionnaireStage
        sessionId={sessionId}
      />,
    )

    const secondQuestionAgree =
      await screen.findByLabelText(
        'Agree',
        {
          selector: 'input[name="Q02"]',
        },
      )

    await user.click(secondQuestionAgree)

    expect(
      await screen.findByRole('alert', undefined, {
        timeout: 2000,
      }),
    ).toHaveTextContent(
      'Autosave failed.',
    )

    expect(secondQuestionAgree).toBeChecked()

    await user.click(
      screen.getByRole('button', {
        name: 'Retry autosave',
      }),
    )

    await waitFor(() => {
      expect(saveRequestCount).toBe(2)
    })

    expect(
      await screen.findByText('Saved'),
    ).toBeInTheDocument()
  })
})

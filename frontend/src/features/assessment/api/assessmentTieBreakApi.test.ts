import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'

import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import { server } from '../../../test/msw/server'
import {
  getDimensionTieBreakInteraction,
  submitDimensionTieBreak,
} from './assessmentApi'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'
const interactionEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}/tie-breaks/EI`

describe('assessment tie-break API', () => {
  beforeEach(() => {
    csrfTokenManager.invalidate()
  })

  it('reads a contextual interaction without requiring pole mappings', async () => {
    server.use(
      http.get(
        interactionEndpoint,
        () =>
          HttpResponse.json({
            interactionType:
              'CONTEXTUAL_QUESTION',
            dimensionCode: 'EI',
            questionId: 'TB-EI-1',
            instruction:
              'Choose the option that feels more natural.',
            prompt:
              'Which approach more often helps your thoughts become clear?',
            options: [
              {
                optionId: 'TB-EI-01',
                text: 'Talk it through.',
              },
              {
                optionId: 'TB-EI-02',
                text: 'Think privately first.',
              },
            ],
          }),
      ),
    )

    const interaction =
      await getDimensionTieBreakInteraction(
        sessionId,
        'EI',
      )

    expect(interaction.interactionType)
      .toBe('CONTEXTUAL_QUESTION')

    if (
      interaction.interactionType !==
      'CONTEXTUAL_QUESTION'
    ) {
      throw new Error(
        'Expected contextual interaction',
      )
    }

    expect(interaction.questionId)
      .toBe('TB-EI-1')

    expect(interaction.options).toEqual([
      {
        optionId: 'TB-EI-01',
        text: 'Talk it through.',
      },
      {
        optionId: 'TB-EI-02',
        text: 'Think privately first.',
      },
    ])

    expect(
      'resolvedPole'
      in interaction.options[0],
    ).toBe(false)
  })

  it('submits the contextual request shape with CSRF', async () => {
    let receivedBody: unknown
    let receivedCsrfHeader: string | null =
      null

    server.use(
      http.get(
        `${origin}/api/v1/auth/csrf`,
        () =>
          HttpResponse.json({
            token: 'contextual-csrf',
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
      ),
      http.put(
        interactionEndpoint,
        async ({ request }) => {
          receivedCsrfHeader =
            request.headers.get(
              'X-CSRF-TOKEN',
            )

          receivedBody =
            await request.json()

          return HttpResponse.json({
            id: sessionId,
          })
        },
      ),
    )

    await submitDimensionTieBreak(
      sessionId,
      'EI',
      {
        questionId: 'TB-EI-1',
        selectedOptionId: 'TB-EI-02',
      },
    )

    expect(receivedCsrfHeader)
      .toBe('contextual-csrf')

    expect(receivedBody).toEqual({
      questionId: 'TB-EI-1',
      selectedOptionId: 'TB-EI-02',
    })
  })
})

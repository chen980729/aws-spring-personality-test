import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'

import { renderWithProviders } from '../../../test/render'
import { server } from '../../../test/msw/server'
import type { AssessmentSession } from '../api/assessmentTypes'
import { TieBreakStage } from './TieBreakStage'

const origin = 'http://localhost:5173'
const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'
const interactionEndpoint =
  `${origin}/api/v1/assessment-sessions/${sessionId}/tie-breaks/EI`

function session(
  version = '1.0',
): AssessmentSession {
  return {
    id: sessionId,
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version,
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
    clarifications: [
      {
        dimensionCode: 'EI',
        status: 'SKIPPED',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ],
    tieBreaks: [],
    finalResult: null,
    workflow: {
      pendingClarificationDimensions: [],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [
        'EI',
      ],
      completed: false,
    },
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

function installLegacyInteraction() {
  server.use(
    http.get(
      interactionEndpoint,
      () =>
        HttpResponse.json({
          interactionType:
            'DIRECT_POLE_SELECTION',
          dimensionCode: 'EI',
          allowedPoles: ['E', 'I'],
        }),
    ),
  )
}

describe('TieBreakStage', () => {
  beforeEach(() => {
    installLegacyInteraction()
  })

  it('renders the backend-authorized direct poles with questionnaire evidence', async () => {
    renderWithProviders(
      <TieBreakStage
        session={session()}
        dimensionCode="EI"
      />,
    )

    expect(
      await screen.findByLabelText(
        'E — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'I — questionnaire evidence 50.0%',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      }),
    ).toBeDisabled()
  })

  it('requires an explicit confirmation before sending a legacy final decision', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <TieBreakStage
        session={session()}
        dimensionCode="EI"
      />,
    )

    await user.click(
      await screen.findByLabelText(
        'I — questionnaire evidence 50.0%',
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      }),
    )

    expect(
      screen.getByText(
        'Confirm I for EI? After the Backend accepts this decision, it cannot be changed.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Confirm I for EI',
      }),
    ).toBeInTheDocument()
  })

  it('renders contextual options without exposing pole choices or option ids', async () => {
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

    renderWithProviders(
      <TieBreakStage
        session={session('1.1')}
        dimensionCode="EI"
      />,
    )

    expect(
      await screen.findByRole('heading', {
        name: 'When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'I start discussing it with someone and often discover what I think while talking.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByLabelText(
        'I first spend some time thinking it through privately, then share my thoughts once they have taken shape.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.queryByText(
        'TB-EI-01',
      ),
    ).not.toBeInTheDocument()

    expect(
      screen.queryByLabelText(
        /questionnaire evidence/,
      ),
    ).not.toBeInTheDocument()
  })

  it('confirms a contextual answer by its user-facing text rather than its resolved pole', async () => {
    const user = userEvent.setup()

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
              'Choose the option that feels more natural most of the time.',
            prompt:
              'Which approach feels more natural?',
            options: [
              {
                optionId: 'TB-EI-01',
                text: 'Talk it through with someone.',
              },
              {
                optionId: 'TB-EI-02',
                text: 'Think it through privately first.',
              },
            ],
          }),
      ),
    )

    renderWithProviders(
      <TieBreakStage
        session={session('1.1')}
        dimensionCode="EI"
      />,
    )

    await user.click(
      await screen.findByLabelText(
        'Think it through privately first.',
      ),
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      }),
    )

    expect(
      screen.getByText(
        'Confirm this answer? After the Backend accepts this decision, it cannot be changed.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getAllByText(
        'Think it through privately first.',
      ),
    ).toHaveLength(2)

    expect(
      screen.getByRole('button', {
        name: 'Confirm answer',
      }),
    ).toBeInTheDocument()

    expect(
      screen.queryByRole('button', {
        name: /Confirm [EI] for EI/,
      }),
    ).not.toBeInTheDocument()
  })
})

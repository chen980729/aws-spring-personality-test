import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  describe,
  expect,
  it,
} from 'vitest'

import { renderWithProviders } from '../../../test/render'
import type { AssessmentSession } from '../api/assessmentTypes'
import { TieBreakStage } from './TieBreakStage'

function session(): AssessmentSession {
  return {
    id:
      '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
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

describe('TieBreakStage', () => {
  it('derives the allowed poles from the bound questionnaire evidence', () => {
    renderWithProviders(
      <TieBreakStage
        session={session()}
        dimensionCode="EI"
      />,
    )

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

    expect(
      screen.getByRole('button', {
        name: 'Review tie-break decision',
      }),
    ).toBeDisabled()
  })

  it('requires an explicit confirmation before sending a final decision', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <TieBreakStage
        session={session()}
        dimensionCode="EI"
      />,
    )

    await user.click(
      screen.getByLabelText(
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
})

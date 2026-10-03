import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../../../test/render'
import type { AssessmentSession } from '../api/assessmentTypes'
import { ClarificationStage } from './ClarificationStage'

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
        status: 'PENDING',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ],
    tieBreaks: [],
    finalResult: null,
    workflow: {
      pendingClarificationDimensions: [
        'EI',
      ],
      retryableClarificationDimensions: [],
      tieBreakRequiredDimensions: [],
      completed: false,
    },
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt: null,
    abandonedAt: null,
  }
}

describe('ClarificationStage', () => {
  it('renders persisted clarification state with questionnaire evidence', () => {
    renderWithProviders(
      <ClarificationStage
        session={session()}
        dimensionCode="EI"
        mode="pending"
      />,
    )

    expect(
      screen.getByRole('heading', {
        name: 'Clarification required',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'No baseline preference (exact tie)',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'E: 50.0% · I: 50.0%',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Skip this dimension',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Skip all remaining clarifications',
      }),
    ).toBeInTheDocument()
  })

  it('requires confirmation before skipping a clarification', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <ClarificationStage
        session={session()}
        dimensionCode="EI"
        mode="pending"
      />,
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Skip this dimension',
      }),
    )

    expect(
      screen.getByText(
        'Skip EI? This clarification will become terminal and cannot be resumed.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('button', {
        name: 'Confirm skip EI',
      }),
    ).toBeInTheDocument()
  })

  it('explains that skip-remaining may still lead to tie-break', async () => {
    const user = userEvent.setup()

    renderWithProviders(
      <ClarificationStage
        session={session()}
        dimensionCode="EI"
        mode="pending"
      />,
    )

    await user.click(
      screen.getByRole('button', {
        name: 'Skip all remaining clarifications',
      }),
    )

    expect(
      screen.getByText(
        /Exact ties may still require a tie-break/,
      ),
    ).toBeInTheDocument()
  })
})

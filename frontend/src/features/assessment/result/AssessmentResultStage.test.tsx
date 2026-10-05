import {
  describe,
  expect,
  it,
} from 'vitest'

import { renderWithProviders } from '../../../test/render'
import type { AssessmentSession } from '../api/assessmentTypes'
import { AssessmentResultStage } from './AssessmentResultStage'
import { screen, within } from '@testing-library/react'

function completedSession(): AssessmentSession {
  return {
    id:
      '9e6a5c52-54df-4e22-8ca6-779d32e4d061',
    assessment: {
      code: 'SIXTEEN_PERSONALITY',
      version: '1.0',
    },
    status: 'COMPLETED',
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
        {
          dimensionCode: 'SN',
          rawScore: 2,
          questionnairePreference: 'N',
          ambiguous: true,
          evidence: {
            poleA: 'S',
            poleAPercentage: 45,
            poleB: 'N',
            poleBPercentage: 55,
          },
        },
      ],
    },
    clarifications: [],
    tieBreaks: [
      {
        dimensionCode: 'EI',
        selectedPole: 'I',
        decidedAt:
          '2026-10-03T07:00:00Z',
      },
    ],
    finalResult: {
      finalType: 'INTJ',
      dimensions: [
        {
          dimensionCode: 'EI',
          questionnairePreference: null,
          finalPreference: 'I',
          source: 'USER_TIE_BREAK',
          overrodeBaseline: false,
        },
        {
          dimensionCode: 'SN',
          questionnairePreference: 'N',
          finalPreference: 'N',
          source: 'QUESTIONNAIRE_FALLBACK',
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
    createdAt:
      '2026-10-03T05:00:00Z',
    completedAt:
      '2026-10-03T07:00:00Z',
    abandonedAt: null,
  }
}

describe('AssessmentResultStage', () => {
  it('renders final type and per-dimension decision provenance', () => {
    renderWithProviders(
      <AssessmentResultStage
        session={completedSession()}
      />,
    )

    expect(
      screen.getByRole('heading', {
        name: 'Assessment complete',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('INTJ'),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('img', {
        name: 'INTJ personality illustration',
      }),
    ).toHaveAttribute(
      'src',
      '/personality/intj.png',
    )

    expect(
      screen.getByText(
        /INTJs may prefer independently analyzing complex problems/,
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'What your letters mean',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Introversion',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Intuition',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Thinking',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Judging',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Common strengths',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'strong at long-term planning',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'Possible blind spots',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        /may underestimate the importance of emotion, relationships, or organizational culture/,
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        /preference closer to 50 \/ 50 should be interpreted less strongly/i,
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByRole('heading', {
        name: 'How this result was decided',
      }),
    ).toBeInTheDocument()

    expect(
      screen.getByText('User tie-break'),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'Exact tie — no preference',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'No questionnaire preference existed because the dimension was exactly tied.',
      ),
    ).toBeInTheDocument()

    expect(
      screen.getByText(
        'Questionnaire fallback',
      ),
    ).toBeInTheDocument()

    const eiResult =
        screen.getByRole('article', {
          name: 'EI',
        })

    expect(
        within(eiResult).getByText(
            /Questionnaire evidence:/,
        ),
    ).toHaveTextContent(
        /E\s+50\.0\s*%\s*·\s*I\s+50\.0\s*%/,
    )

    const snResult =
        screen.getByRole('article', {
          name: 'SN',
        })

    expect(
        within(snResult).getByText(
            /Questionnaire evidence:/,
        ),
    ).toHaveTextContent(
        /S\s+45\.0\s*%\s*·\s*N\s+55\.0\s*%/,
    )
  })

  it('shows a recoverable result error when completed response details are inconsistent', () => {
    const session =
      completedSession()

    session.finalResult!.dimensions[0]
      .questionnairePreference = 'E'

    renderWithProviders(
      <AssessmentResultStage
        session={session}
      />,
    )

    expect(
      screen.getByRole('alert'),
    ).toHaveTextContent(
      'final result details could not be resolved',
    )
  })
})

import {
  describe,
  expect,
  it,
} from 'vitest'

import type { AssessmentSession } from '../api/assessmentTypes'
import { buildAssessmentResultReadModel } from './assessmentResultReadModel'

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

describe('buildAssessmentResultReadModel', () => {
  it('distinguishes an exact-tie decision from a matching questionnaire baseline', () => {
    const result =
      buildAssessmentResultReadModel(
        completedSession(),
      )

    expect(result).not.toBeNull()

    expect(result?.dimensions[0]).toMatchObject({
      dimensionCode: 'EI',
      questionnairePreference: null,
      finalPreference: 'I',
      sourceLabel: 'User tie-break',
      baselineRelation: 'no-baseline',
    })

    expect(result?.dimensions[1]).toMatchObject({
      dimensionCode: 'SN',
      questionnairePreference: 'N',
      finalPreference: 'N',
      sourceLabel:
        'Questionnaire fallback',
      baselineRelation:
        'matches-questionnaire',
    })
  })

  it('describes a clarification override separately from a questionnaire match', () => {
    const session =
      completedSession()

    session.initialResult!.dimensions[1]
      .questionnairePreference = 'S'

    session.finalResult!.dimensions[1] = {
      dimensionCode: 'SN',
      questionnairePreference: 'S',
      finalPreference: 'N',
      source: 'AI_CLARIFICATION',
      overrodeBaseline: true,
    }

    const result =
      buildAssessmentResultReadModel(
        session,
      )

    expect(result?.dimensions[1]).toMatchObject({
      sourceLabel: 'AI clarification',
      baselineRelation:
        'overrode-questionnaire',
    })
  })

  it('presents contextual tie-break finalization separately from legacy direct selection', () => {
    const session =
      completedSession()

    session.assessment.version = '1.1'

    session.tieBreaks = [
      {
        dimensionCode: 'EI',
        questionId: 'TB-EI-1',
        selectedOptionId: 'TB-EI-02',
        decidedAt:
          '2026-10-05T07:00:00Z',
      },
    ]

    session.finalResult!.dimensions[0] = {
      dimensionCode: 'EI',
      questionnairePreference: null,
      finalPreference: 'I',
      source: 'TIE_BREAK_QUESTION',
      overrodeBaseline: false,
    }

    const result =
      buildAssessmentResultReadModel(
        session,
      )

    expect(result?.dimensions[0]).toMatchObject({
      source: 'TIE_BREAK_QUESTION',
      sourceLabel: 'Tie-break question',
      baselineRelation: 'no-baseline',
    })
  })

  it('returns null when final and initial questionnaire preferences disagree', () => {
    const session =
      completedSession()

    session.finalResult!.dimensions[1]
      .questionnairePreference = 'S'

    expect(
      buildAssessmentResultReadModel(
        session,
      ),
    ).toBeNull()
  })
})

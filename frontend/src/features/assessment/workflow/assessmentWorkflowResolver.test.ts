import {
  describe,
  expect,
  it,
} from 'vitest'

import type { AssessmentSession } from '../api/assessmentTypes'
import { resolveAssessmentWorkflow } from './assessmentWorkflowResolver'

const sessionId =
  '9e6a5c52-54df-4e22-8ca6-779d32e4d061'

function baseSession(): AssessmentSession {
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
          rawScore: 1,
          questionnairePreference: 'S',
          ambiguous: true,
          evidence: {
            poleA: 'S',
            poleAPercentage: 55,
            poleB: 'N',
            poleBPercentage: 45,
          },
        },
      ],
    },
    clarifications: [],
    tieBreaks: [],
    finalResult: null,
    workflow: {
      pendingClarificationDimensions: [],
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

describe('resolveAssessmentWorkflow', () => {
  it('keeps an unsubmitted IN_PROGRESS session in the questionnaire view', () => {
    const session = baseSession()

    session.status = 'IN_PROGRESS'
    session.questionnaire.submitted = false
    session.questionnaire.submittedAt = null

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'questionnaire',
    })
  })

  it('selects the first clarification in backend dimension order', () => {
    const session = baseSession()

    session.clarifications = [
      {
        dimensionCode: 'EI',
        status: 'PENDING',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
      {
        dimensionCode: 'SN',
        status: 'FAILED_RETRYABLE',
        result: null,
        startedAt:
          '2026-10-03T06:10:00Z',
        acceptedAt: null,
      },
    ]

    session.workflow
      .pendingClarificationDimensions = [
      'EI',
    ]

    session.workflow
      .retryableClarificationDimensions = [
      'SN',
    ]

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'clarification',
      mode: 'pending',
      dimensionCode: 'EI',
    })
  })

  it('resolves a retryable clarification when it is the next actionable dimension', () => {
    const session = baseSession()

    session.clarifications = [
      {
        dimensionCode: 'EI',
        status: 'FAILED_RETRYABLE',
        result: null,
        startedAt:
          '2026-10-03T06:10:00Z',
        acceptedAt: null,
      },
    ]

    session.workflow
      .retryableClarificationDimensions = [
      'EI',
    ]

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'clarification',
      mode: 'retryable',
      dimensionCode: 'EI',
    })
  })

  it('derives the active clarification from the single IN_PROGRESS read model', () => {
    const session = baseSession()

    session.status =
      'CLARIFICATION_IN_PROGRESS'

    session.clarifications = [
      {
        dimensionCode: 'EI',
        status: 'IN_PROGRESS',
        result: null,
        startedAt:
          '2026-10-03T06:10:00Z',
        acceptedAt: null,
      },
      {
        dimensionCode: 'SN',
        status: 'PENDING',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ]

    session.workflow
      .pendingClarificationDimensions = [
      'SN',
    ]

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'clarification',
      mode: 'in-progress',
      dimensionCode: 'EI',
    })
  })

  it('moves to tie-break only after no pending or retryable clarification remains', () => {
    const session = baseSession()

    session.clarifications = [
      {
        dimensionCode: 'EI',
        status: 'SKIPPED',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ]

    session.workflow
      .tieBreakRequiredDimensions = [
      'EI',
    ]

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'tie-break',
      dimensionCode: 'EI',
    })
  })

  it('returns recovery when CLARIFICATION_IN_PROGRESS has no active clarification', () => {
    const session = baseSession()

    session.status =
      'CLARIFICATION_IN_PROGRESS'

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'recovery',
      reason:
        'missing-active-clarification',
    })
  })

  it('returns recovery when Backend workflow projection and clarification state disagree', () => {
    const session = baseSession()

    session.clarifications = [
      {
        dimensionCode: 'EI',
        status: 'PENDING',
        result: null,
        startedAt: null,
        acceptedAt: null,
      },
    ]

    expect(
      resolveAssessmentWorkflow(session),
    ).toEqual({
      kind: 'recovery',
      reason:
        'workflow-clarification-mismatch',
    })
  })

  it('resolves completed and abandoned terminal views', () => {
    const completed = baseSession()
    completed.status = 'COMPLETED'
    completed.workflow.completed = true

    expect(
      resolveAssessmentWorkflow(completed),
    ).toEqual({
      kind: 'completed',
    })

    const abandoned = baseSession()
    abandoned.status = 'ABANDONED'

    expect(
      resolveAssessmentWorkflow(abandoned),
    ).toEqual({
      kind: 'abandoned',
    })
  })
})

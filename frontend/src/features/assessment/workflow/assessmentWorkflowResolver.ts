import type {
  AssessmentSession
} from '../api/assessmentTypes'

export type ClarificationMode =
  | 'pending'
  | 'in-progress'
  | 'retryable'

export type AssessmentWorkflowView =
  | {
      kind: 'questionnaire'
    }
  | {
      kind: 'clarification'
      mode: ClarificationMode
      dimensionCode: string
    }
  | {
      kind: 'tie-break'
      dimensionCode: string
    }
  | {
      kind: 'completed'
    }
  | {
      kind: 'abandoned'
    }
  | {
      kind: 'recovery'
      reason:
        | 'workflow-completion-mismatch'
        | 'submitted-questionnaire-in-progress'
        | 'missing-active-clarification'
        | 'multiple-active-clarifications'
        | 'workflow-clarification-mismatch'
        | 'missing-post-questionnaire-action'
    }

function resolveAwaitingClarification(
  session: AssessmentSession,
): AssessmentWorkflowView {
  const actionableClarification =
    session.clarifications.find(
      (clarification) =>
        clarification.status === 'PENDING' ||
        clarification.status ===
          'FAILED_RETRYABLE',
    )

  if (actionableClarification) {
    const { dimensionCode, status } =
      actionableClarification

    if (status === 'PENDING') {
      if (
        !session.workflow
          .pendingClarificationDimensions
          .includes(dimensionCode)
      ) {
        return {
          kind: 'recovery',
          reason:
            'workflow-clarification-mismatch',
        }
      }

      return {
        kind: 'clarification',
        mode: 'pending',
        dimensionCode,
      }
    }

    if (
      !session.workflow
        .retryableClarificationDimensions
        .includes(dimensionCode)
    ) {
      return {
        kind: 'recovery',
        reason:
          'workflow-clarification-mismatch',
      }
    }

    return {
      kind: 'clarification',
      mode: 'retryable',
      dimensionCode,
    }
  }

  const tieBreakDimension =
    session.workflow
      .tieBreakRequiredDimensions[0]

  if (tieBreakDimension) {
    return {
      kind: 'tie-break',
      dimensionCode: tieBreakDimension,
    }
  }

  return {
    kind: 'recovery',
    reason: 'missing-post-questionnaire-action',
  }
}

function resolveActiveClarification(
  session: AssessmentSession,
): AssessmentWorkflowView {
  const activeClarifications =
    session.clarifications.filter(
      (clarification) =>
        clarification.status === 'IN_PROGRESS',
    )

  if (activeClarifications.length === 0) {
    return {
      kind: 'recovery',
      reason: 'missing-active-clarification',
    }
  }

  if (activeClarifications.length > 1) {
    return {
      kind: 'recovery',
      reason: 'multiple-active-clarifications',
    }
  }

  const [activeClarification] =
    activeClarifications

  return {
    kind: 'clarification',
    mode: 'in-progress',
    dimensionCode:
      activeClarification.dimensionCode,
  }
}

/**
 * Maps authoritative Backend state to a presentation view.
 *
 * This function does not implement Assessment domain transitions.
 * It only decides which UI should represent the state that the
 * Backend has already returned.
 */
export function resolveAssessmentWorkflow(
  session: AssessmentSession,
): AssessmentWorkflowView {
  const sessionCompleted =
    session.status === 'COMPLETED'

  if (
    session.workflow.completed !==
    sessionCompleted
  ) {
    return {
      kind: 'recovery',
      reason: 'workflow-completion-mismatch',
    }
  }

  if (session.status === 'ABANDONED') {
    return {
      kind: 'abandoned',
    }
  }

  if (session.status === 'COMPLETED') {
    return {
      kind: 'completed',
    }
  }

  if (session.status === 'IN_PROGRESS') {
    if (session.questionnaire.submitted) {
      return {
        kind: 'recovery',
        reason:
          'submitted-questionnaire-in-progress',
      }
    }

    return {
      kind: 'questionnaire',
    }
  }

  if (
    session.status ===
    'CLARIFICATION_IN_PROGRESS'
  ) {
    return resolveActiveClarification(
      session,
    )
  }

  if (
    session.status ===
    'AWAITING_CLARIFICATION'
  ) {
    const unexpectedActive =
      session.clarifications.find(
        (clarification) =>
          clarification.status ===
          'IN_PROGRESS',
      )

    if (unexpectedActive) {
      return {
        kind: 'recovery',
        reason:
          'workflow-clarification-mismatch',
      }
    }

    return resolveAwaitingClarification(
      session,
    )
  }

  return {
    kind: 'recovery',
    reason: 'missing-post-questionnaire-action',
  }
}

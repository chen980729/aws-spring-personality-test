import type {
  AssessmentSession,
  ClarificationResult,
  ClarificationStatus,
  QuestionnaireEvidence,
} from '../api/assessmentTypes'

export interface ClarificationReadModel {
  dimensionCode: string
  status: ClarificationStatus
  questionnairePreference: string | null
  evidence: QuestionnaireEvidence
  result: ClarificationResult | null
  startedAt: string | null
  acceptedAt: string | null
}

export function buildClarificationReadModel(
  session: AssessmentSession,
  dimensionCode: string,
): ClarificationReadModel | null {
  const clarification =
    session.clarifications.find(
      (candidate) =>
        candidate.dimensionCode ===
        dimensionCode,
    )

  const initialDimension =
    session.initialResult?.dimensions.find(
      (candidate) =>
        candidate.dimensionCode ===
        dimensionCode,
    )

  if (
    !clarification ||
    !initialDimension
  ) {
    return null
  }

  return {
    dimensionCode,
    status: clarification.status,
    questionnairePreference:
      initialDimension.questionnairePreference,
    evidence: initialDimension.evidence,
    result: clarification.result,
    startedAt: clarification.startedAt,
    acceptedAt: clarification.acceptedAt,
  }
}

import type { AssessmentSession } from '../api/assessmentTypes'

export interface TieBreakOption {
  pole: string
  questionnairePercentage: number
}

export interface TieBreakReadModel {
  dimensionCode: string
  questionnairePreference: string | null
  options: [TieBreakOption, TieBreakOption]
}

export function buildTieBreakReadModel(
  session: AssessmentSession,
  dimensionCode: string,
): TieBreakReadModel | null {
  const dimension =
    session.initialResult?.dimensions.find(
      (candidate) =>
        candidate.dimensionCode ===
        dimensionCode,
    )

  if (!dimension) {
    return null
  }

  return {
    dimensionCode,
    questionnairePreference:
      dimension.questionnairePreference,
    options: [
      {
        pole: dimension.evidence.poleA,
        questionnairePercentage:
          dimension.evidence.poleAPercentage,
      },
      {
        pole: dimension.evidence.poleB,
        questionnairePercentage:
          dimension.evidence.poleBPercentage,
      },
    ],
  }
}

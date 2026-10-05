import type {
  AssessmentSession,
  FinalDecisionSource,
  QuestionnaireEvidence,
} from '../api/assessmentTypes'

export type ResultBaselineRelation =
  | 'no-baseline'
  | 'matches-questionnaire'
  | 'overrode-questionnaire'

export interface ResultDimensionReadModel {
  dimensionCode: string
  questionnairePreference: string | null
  finalPreference: string
  source: FinalDecisionSource
  sourceLabel: string
  sourceDescription: string
  baselineRelation: ResultBaselineRelation
  baselineRelationLabel: string
  evidence: QuestionnaireEvidence
}

export interface AssessmentResultReadModel {
  finalType: string
  dimensions: ResultDimensionReadModel[]
}

function sourcePresentation(
  source: FinalDecisionSource,
): {
  label: string
  description: string
} {
  switch (source) {
    case 'QUESTIONNAIRE':
      return {
        label: 'Questionnaire',
        description:
          'The questionnaire preference was already decisive.',
      }

    case 'QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION':
      return {
        label:
          'Questionnaire confirmed by clarification',
        description:
          'Clarification agreed with the questionnaire preference.',
      }

    case 'AI_CLARIFICATION':
      return {
        label: 'AI clarification',
        description:
          'Clarification supplied the final preference.',
      }

    case 'QUESTIONNAIRE_FALLBACK':
      return {
        label: 'Questionnaire fallback',
        description:
          'Clarification did not resolve the dimension, so the non-tied questionnaire preference was retained.',
      }

    case 'USER_TIE_BREAK':
      return {
        label: 'User tie-break',
        description:
          'The questionnaire was exactly tied, so the persisted user tie-break supplied the final preference.',
      }

    case 'TIE_BREAK_QUESTION':
      return {
        label: 'Tie-break question',
        description:
          'The questionnaire was exactly tied, so a contextual tie-break question supplied the final preference.',
      }
  }
}

function baselineRelation(
  questionnairePreference: string | null,
  overrodeBaseline: boolean,
): {
  relation: ResultBaselineRelation
  label: string
} {
  if (questionnairePreference === null) {
    return {
      relation: 'no-baseline',
      label:
        'No questionnaire preference existed because the dimension was exactly tied.',
    }
  }

  if (overrodeBaseline) {
    return {
      relation: 'overrode-questionnaire',
      label:
        'The final preference differs from the questionnaire preference.',
    }
  }

  return {
    relation: 'matches-questionnaire',
    label:
      'The final preference matches the questionnaire preference.',
  }
}

export function buildAssessmentResultReadModel(
  session: AssessmentSession,
): AssessmentResultReadModel | null {
  if (
    !session.finalResult ||
    !session.initialResult
  ) {
    return null
  }

  const dimensions: ResultDimensionReadModel[] = []

  for (
    const finalDimension
    of session.finalResult.dimensions
  ) {
    const initialDimension =
      session.initialResult.dimensions.find(
        (candidate) =>
          candidate.dimensionCode ===
          finalDimension.dimensionCode,
      )

    if (!initialDimension) {
      return null
    }

    if (
      initialDimension.questionnairePreference !==
      finalDimension.questionnairePreference
    ) {
      return null
    }

    const source =
      sourcePresentation(
        finalDimension.source,
      )

    const relation =
      baselineRelation(
        finalDimension.questionnairePreference,
        finalDimension.overrodeBaseline,
      )

    dimensions.push({
      dimensionCode:
        finalDimension.dimensionCode,
      questionnairePreference:
        finalDimension.questionnairePreference,
      finalPreference:
        finalDimension.finalPreference,
      source: finalDimension.source,
      sourceLabel: source.label,
      sourceDescription:
        source.description,
      baselineRelation:
        relation.relation,
      baselineRelationLabel:
        relation.label,
      evidence: initialDimension.evidence,
    })
  }

  return {
    finalType:
      session.finalResult.finalType,
    dimensions,
  }
}

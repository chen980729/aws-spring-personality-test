export interface AssessmentCatalog {
  items: AssessmentSummary[]
}

export interface AssessmentSummary {
  code: string
  name: string
  availableVersion: string
  questionCount: number
}

export interface AssessmentDetails {
  code: string
  name: string
  version: string
  questionCount: number
  questionnaire: PublicQuestionnaire
}

export interface PublicQuestionnaire {
  questions: PublicQuestion[]
  answerScale: AnswerScaleOption[]
}

export interface PublicQuestion {
  questionId: string
  position: number
  prompt: string
}

export interface AnswerScaleOption {
  value: number
  label: string
}

export interface StartAssessmentResponse {
  created: boolean
  session: AssessmentSession
}

export interface RestartAssessmentResponse {
  abandonedSessionId: string
  session: AssessmentSession
}

export type AssessmentSessionStatus =
  | 'IN_PROGRESS'
  | 'AWAITING_CLARIFICATION'
  | 'CLARIFICATION_IN_PROGRESS'
  | 'COMPLETED'
  | 'ABANDONED'

export interface AssessmentSession {
  id: string
  assessment: BoundAssessment
  status: AssessmentSessionStatus
  questionnaire: QuestionnaireState
  initialResult: InitialAssessmentResult | null
  clarifications: ClarificationState[]
  tieBreaks: TieBreakState[]
  finalResult: FinalAssessmentResult | null
  workflow: AssessmentWorkflow
  createdAt: string
  completedAt: string | null
  abandonedAt: string | null
}

export interface BoundAssessment {
  code: string
  version: string
}

export interface QuestionnaireState {
  answers: QuestionAnswer[]
  submitted: boolean
  submittedAt: string | null
}

export interface QuestionAnswer {
  questionId: string
  value: number
}

export interface InitialAssessmentResult {
  dimensions: InitialDimensionResult[]
}

export interface InitialDimensionResult {
  dimensionCode: string
  rawScore: number
  questionnairePreference: string | null
  ambiguous: boolean
  evidence: QuestionnaireEvidence
}

export interface QuestionnaireEvidence {
  poleA: string
  poleAPercentage: number
  poleB: string
  poleBPercentage: number
}

export interface ClarificationState {
  dimensionCode: string
  status: string
  result: ClarificationResult | null
  startedAt: string | null
  acceptedAt: string | null
}

export interface ClarificationResult {
  resolution: string
  suggestedPole: string | null
  confidence: string
  reasoningSummary: string
}

export interface TieBreakState {
  dimensionCode: string
  selectedPole: string
  decidedAt: string
}

export interface FinalAssessmentResult {
  finalType: string
  dimensions: FinalDimensionConclusion[]
}

export interface FinalDimensionConclusion {
  dimensionCode: string
  questionnairePreference: string | null
  finalPreference: string
  source: string
  overrodeBaseline: boolean
}

export interface AssessmentWorkflow {
  pendingClarificationDimensions: string[]
  retryableClarificationDimensions: string[]
  tieBreakRequiredDimensions: string[]
  completed: boolean
}

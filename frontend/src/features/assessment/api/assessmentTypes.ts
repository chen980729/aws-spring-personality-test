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

export interface SessionQuestionnaire {
  assessment: BoundAssessment
  questionnaire: PublicQuestionnaire
  response: QuestionnaireState
}

export interface QuestionnaireSnapshotRequest {
  answers: QuestionAnswer[]
}

export interface LegacyTieBreakRequest {
  selectedPole: string
  questionId?: never
  selectedOptionId?: never
}

export interface ContextualTieBreakRequest {
  selectedPole?: never
  questionId: string
  selectedOptionId: string
}

export type TieBreakRequest =
  | LegacyTieBreakRequest
  | ContextualTieBreakRequest

export interface DirectPoleTieBreakInteraction {
  interactionType: 'DIRECT_POLE_SELECTION'
  dimensionCode: string
  allowedPoles: string[]
}

export interface ContextualTieBreakInteractionOption {
  optionId: string
  text: string
}

export interface ContextualTieBreakInteraction {
  interactionType: 'CONTEXTUAL_QUESTION'
  dimensionCode: string
  questionId: string
  instruction: string
  prompt: string
  options: ContextualTieBreakInteractionOption[]
}

export type TieBreakInteraction =
  | DirectPoleTieBreakInteraction
  | ContextualTieBreakInteraction

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

export type ClarificationStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'CLARIFIED'
  | 'SKIPPED'
  | 'FAILED_RETRYABLE'

export type ClarificationConfidence =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'

export type ClarificationResult =
  | ResolvedClarificationResult
  | UnclearClarificationResult

export interface ResolvedClarificationResult {
  resolution: 'RESOLVED'
  suggestedPole: string
  confidence: ClarificationConfidence
  reasoningSummary: string
}

export interface UnclearClarificationResult {
  resolution: 'UNCLEAR'
  suggestedPole: null
  confidence: ClarificationConfidence
  reasoningSummary: string
}

export interface ClarificationState {
  dimensionCode: string
  status: ClarificationStatus
  result: ClarificationResult | null
  startedAt: string | null
  acceptedAt: string | null
}

export interface LegacyTieBreakState {
  dimensionCode: string
  selectedPole: string
  questionId?: never
  selectedOptionId?: never
  decidedAt: string
}

export interface ContextualTieBreakState {
  dimensionCode: string
  selectedPole?: never
  questionId: string
  selectedOptionId: string
  decidedAt: string
}

export type TieBreakState =
  | LegacyTieBreakState
  | ContextualTieBreakState

export type FinalDecisionSource =
  | 'QUESTIONNAIRE'
  | 'QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION'
  | 'AI_CLARIFICATION'
  | 'QUESTIONNAIRE_FALLBACK'
  | 'USER_TIE_BREAK'
  | 'TIE_BREAK_QUESTION'

export interface FinalAssessmentResult {
  finalType: string
  dimensions: FinalDimensionConclusion[]
}

export interface FinalDimensionConclusion {
  dimensionCode: string
  questionnairePreference: string | null
  finalPreference: string
  source: FinalDecisionSource
  overrodeBaseline: boolean
}

export interface AssessmentWorkflow {
  pendingClarificationDimensions: string[]
  retryableClarificationDimensions: string[]
  tieBreakRequiredDimensions: string[]
  completed: boolean
}

export interface AssessmentHistoryResponse {
  items: AssessmentHistoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AssessmentHistoryItem {
  sessionId: string
  assessmentCode: string
  assessmentVersion: string
  finalType: string
  completedAt: string
}


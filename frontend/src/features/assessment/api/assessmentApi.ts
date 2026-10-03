import { isApiError } from '../../../shared/api/apiError'
import {
  getJson,
  postJson,
  putJson,
} from '../../../shared/api/httpClient'
import type {
  AssessmentCatalog,
  AssessmentDetails,
  AssessmentSession,
  QuestionnaireSnapshotRequest,
  RestartAssessmentResponse,
  SessionQuestionnaire,
  StartAssessmentResponse,
} from './assessmentTypes'

export function getAssessmentCatalog(): Promise<AssessmentCatalog> {
  return getJson<AssessmentCatalog>(
    '/api/v1/assessments',
  )
}

export function getAssessmentDetails(
  assessmentCode: string,
): Promise<AssessmentDetails> {
  return getJson<AssessmentDetails>(
    `/api/v1/assessments/${encodeURIComponent(assessmentCode)}`,
  )
}

export async function getActiveAssessmentSession(
  assessmentCode: string,
): Promise<AssessmentSession | null> {
  try {
    return await getJson<AssessmentSession>(
      `/api/v1/assessments/${encodeURIComponent(assessmentCode)}/sessions/active`,
    )
  } catch (error) {
    if (
      isApiError(error) &&
      error.code === 'ASSESSMENT_SESSION_NOT_FOUND'
    ) {
      return null
    }

    throw error
  }
}

export function startAssessmentSession(
  assessmentCode: string,
): Promise<StartAssessmentResponse> {
  return postJson<StartAssessmentResponse>(
    `/api/v1/assessments/${encodeURIComponent(assessmentCode)}/sessions`,
  )
}

export function getAssessmentSession(
  sessionId: string,
): Promise<AssessmentSession> {
  return getJson<AssessmentSession>(
    `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}`,
  )
}

export function getSessionQuestionnaire(
  sessionId: string,
): Promise<SessionQuestionnaire> {
  return getJson<SessionQuestionnaire>(
    `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}/questionnaire`,
  )
}

export function saveQuestionnaireProgress(
  sessionId: string,
  request: QuestionnaireSnapshotRequest,
): Promise<AssessmentSession> {
  return putJson<AssessmentSession>(
    `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}/questionnaire`,
    request,
  )
}

export async function submitQuestionnaire(
  sessionId: string,
  request: QuestionnaireSnapshotRequest,
): Promise<AssessmentSession> {
  try {
    return await postJson<AssessmentSession>(
      `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}/questionnaire/submission`,
      request,
    )
  } catch (error) {
    if (
      isApiError(error) &&
      error.code === 'ASSESSMENT_ALREADY_SUBMITTED'
    ) {
      // The first submit may have committed while its HTTP
      // response was lost. Recover authoritative state rather
      // than running submission/scoring a second time.
      return getAssessmentSession(sessionId)
    }

    throw error
  }
}

export function restartAssessmentSession(
  sessionId: string,
): Promise<RestartAssessmentResponse> {
  return postJson<RestartAssessmentResponse>(
    `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}/restart`,
  )
}

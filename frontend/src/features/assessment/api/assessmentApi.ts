import { isApiError } from '../../../shared/api/apiError'
import {
  getJson,
  postJson,
} from '../../../shared/api/httpClient'
import type {
  AssessmentCatalog,
  AssessmentDetails,
  AssessmentSession,
  RestartAssessmentResponse,
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

export function restartAssessmentSession(
  sessionId: string,
): Promise<RestartAssessmentResponse> {
  return postJson<RestartAssessmentResponse>(
    `/api/v1/assessment-sessions/${encodeURIComponent(sessionId)}/restart`,
  )
}

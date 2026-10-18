import { isApiError } from '../../../shared/api/apiError'

import {
  QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import {
  getActiveAssessmentSession,
  getAssessmentCatalog,
  getAssessmentDetails,
  getAssessmentHistory,
  getAssessmentSession,
  getCurrentClarificationTurn,
  startClarification,
  continueClarification,
  retryClarification,
  getDimensionTieBreakInteraction,
  getSessionQuestionnaire,
  restartAssessmentSession,
  saveQuestionnaireProgress,
  skipClarification,
  skipRemainingClarifications,
  startAssessmentSession,
  submitDimensionTieBreak,
  submitQuestionnaire,
} from './assessmentApi'
import { assessmentQueryKeys } from './assessmentQueryKeys'
import type {
  AssessmentSession,
  ClarificationInteractionResponse,
  ClarificationMessageRequest,
  CurrentClarificationTurn,
  QuestionAnswer,
  SessionQuestionnaire,
  TieBreakRequest,
} from './assessmentTypes'

const catalogStaleTime = 5 * 60 * 1000
const detailsStaleTime = 5 * 60 * 1000
const activeSessionStaleTime = 10 * 1000

function isActiveSession(
  session: AssessmentSession,
): boolean {
  return (
    session.status === 'IN_PROGRESS' ||
    session.status === 'AWAITING_CLARIFICATION' ||
    session.status === 'CLARIFICATION_IN_PROGRESS'
  )
}

function cacheAuthoritativeSession(
  queryClient: QueryClient,
  session: AssessmentSession,
) {
  queryClient.setQueryData(
    assessmentQueryKeys.session(session.id),
    session,
  )

  queryClient.setQueryData(
    assessmentQueryKeys.activeSession(
      session.assessment.code,
    ),
    isActiveSession(session)
      ? session
      : null,
  )

  if (session.status === 'COMPLETED') {
    void queryClient.invalidateQueries({
      queryKey:
        assessmentQueryKeys.historyRoot(),
    })
  }
}

export function useAssessmentCatalogQuery() {
  return useQuery({
    queryKey: assessmentQueryKeys.catalog(),
    queryFn: getAssessmentCatalog,
    staleTime: catalogStaleTime,
  })
}

export function useAssessmentDetailsQuery(
  assessmentCode: string,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.details(assessmentCode),
    queryFn: () =>
      getAssessmentDetails(assessmentCode),
    staleTime: detailsStaleTime,
  })
}

export function useActiveAssessmentSessionQuery(
  assessmentCode: string,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.activeSession(
        assessmentCode,
      ),
    queryFn: () =>
      getActiveAssessmentSession(
        assessmentCode,
      ),
    staleTime: activeSessionStaleTime,
  })
}


export function useAssessmentHistoryQuery(
  page: number,
  size: number,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.history(
        page,
        size,
      ),
    queryFn: () =>
      getAssessmentHistory(
        page,
        size,
      ),
    staleTime: 30 * 1000,
  })
}

export function useAssessmentSessionQuery(
  sessionId: string,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.session(sessionId),
    queryFn: () =>
      getAssessmentSession(sessionId),
  })
}

export function useSessionQuestionnaireQuery(
  sessionId: string,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.questionnaire(sessionId),
    queryFn: () =>
      getSessionQuestionnaire(sessionId),
  })
}

export function useSaveQuestionnaireProgressMutation(
  sessionId: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (answers: QuestionAnswer[]) =>
      saveQuestionnaireProgress(
        sessionId,
        {
          answers,
        },
      ),

    onSuccess: (session) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )

      queryClient.setQueryData<SessionQuestionnaire>(
        assessmentQueryKeys.questionnaire(
          sessionId,
        ),
        (current) =>
          current
            ? {
                ...current,
                response: session.questionnaire,
              }
            : current,
      )
    },
  })
}

export function useSubmitQuestionnaireMutation(
  sessionId: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (answers: QuestionAnswer[]) =>
      submitQuestionnaire(
        sessionId,
        {
          answers,
        },
      ),

    onSuccess: (session) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )

      queryClient.setQueryData<SessionQuestionnaire>(
        assessmentQueryKeys.questionnaire(
          sessionId,
        ),
        (current) =>
          current
            ? {
                ...current,
                response: session.questionnaire,
              }
            : current,
      )
    },
  })
}

export function useCurrentClarificationTurnQuery(
  sessionId: string,
  dimensionCode: string,
  enabled: boolean,
) {
  return useQuery({
    queryKey: assessmentQueryKeys.currentClarificationTurn(sessionId, dimensionCode),
    queryFn: () => getCurrentClarificationTurn(sessionId, dimensionCode),
    enabled,
    // 409 means there is no answerable question at the moment, not an
    // instruction to poll or repeat the previous answer.
    retry: false,
  })
}

/**
 * All three commands share the same recovery rule:
 * use the server's new question/version, then reload the authoritative Session.
 * A lost response or 409 must never cause an automatic POST replay.
 */
function useClarificationActionMutation<TPayload>(
  sessionId: string,
  dimensionCode: string,
  execute: (payload: TPayload) => Promise<ClarificationInteractionResponse>,
) {
  const queryClient = useQueryClient()
  const currentTurnKey = assessmentQueryKeys.currentClarificationTurn(
    sessionId, dimensionCode,
  )

  return useMutation({
    mutationFn: execute,
    onSuccess: async (response) => {
      if (
        response.clarificationStatus === 'IN_PROGRESS' &&
        response.assistantSequence !== null &&
        response.question !== null
      ) {
        queryClient.setQueryData<CurrentClarificationTurn>(currentTurnKey, {
          sessionId: response.sessionId,
          dimensionCode: response.dimensionCode,
          assistantSequence: response.assistantSequence,
          question: response.question,
        })
      } else {
        queryClient.removeQueries({ queryKey: currentTurnKey, exact: true })
      }
      await queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.session(sessionId),
        exact: true,
      })
      await queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.activeSessionRoot(),
      })
    },
    onError: async (error) => {
      // For 409/422 or an ambiguous network failure, read from the
      // server before presenting the next action. No automatic retry.
      if (!isApiError(error) || error.status === 409 || error.status === 422) {
        await queryClient.invalidateQueries({
          queryKey: assessmentQueryKeys.session(sessionId),
          exact: true,
        })
        await queryClient.invalidateQueries({
          queryKey: currentTurnKey,
          exact: true,
        })
      }
    },
  })
}

export function useStartClarificationMutation(
  sessionId: string,
  dimensionCode: string,
) {
  return useClarificationActionMutation(
    sessionId,
    dimensionCode,
    () => startClarification(sessionId, dimensionCode),
  )
}

export function useContinueClarificationMutation(
  sessionId: string,
  dimensionCode: string,
) {
  return useClarificationActionMutation(
    sessionId,
    dimensionCode,
    (request: ClarificationMessageRequest) =>
      continueClarification(sessionId, dimensionCode, request),
  )
}

export function useRetryClarificationMutation(
  sessionId: string,
  dimensionCode: string,
) {
  return useClarificationActionMutation(
    sessionId,
    dimensionCode,
    () => retryClarification(sessionId, dimensionCode),
  )
}

export function useSkipClarificationMutation(
  sessionId: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dimensionCode: string) =>
      skipClarification(
        sessionId,
        dimensionCode,
      ),

    onSuccess: (session) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )
    },
  })
}

export function useSkipRemainingClarificationsMutation(
  sessionId: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () =>
      skipRemainingClarifications(sessionId),

    onSuccess: (session) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )
    },
  })
}

export function useDimensionTieBreakInteractionQuery(
  sessionId: string,
  dimensionCode: string,
  enabled = true,
) {
  return useQuery({
    queryKey:
      assessmentQueryKeys.tieBreakInteraction(
        sessionId,
        dimensionCode,
      ),
    queryFn: () =>
      getDimensionTieBreakInteraction(
        sessionId,
        dimensionCode,
      ),
    enabled,
  })
}

export function useSubmitDimensionTieBreakMutation(
  sessionId: string,
  dimensionCode: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: TieBreakRequest) =>
      submitDimensionTieBreak(
        sessionId,
        dimensionCode,
        request,
      ),

    onSuccess: (session) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )

      queryClient.removeQueries({
        queryKey:
          assessmentQueryKeys.tieBreakInteraction(
            sessionId,
            dimensionCode,
          ),
        exact: true,
      })
    },
  })
}

export function useStartAssessmentSessionMutation(
  assessmentCode: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () =>
      startAssessmentSession(assessmentCode),

    onSuccess: ({ session }) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )
    },
  })
}

export function useRestartAssessmentSessionMutation(
  assessmentCode: string,
  currentSessionId: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () =>
      restartAssessmentSession(currentSessionId),

    onSuccess: ({
      abandonedSessionId,
      session,
    }) => {
      cacheAuthoritativeSession(
        queryClient,
        session,
      )

      queryClient.setQueryData(
        assessmentQueryKeys.activeSession(
          assessmentCode,
        ),
        session,
      )

      void queryClient.invalidateQueries({
        queryKey:
          assessmentQueryKeys.session(
            abandonedSessionId,
          ),
        exact: true,
      })
    },
  })
}

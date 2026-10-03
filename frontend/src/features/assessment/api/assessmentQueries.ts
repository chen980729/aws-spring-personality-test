import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import {
  getActiveAssessmentSession,
  getAssessmentCatalog,
  getAssessmentDetails,
  getAssessmentSession,
  restartAssessmentSession,
  startAssessmentSession,
} from './assessmentApi'
import { assessmentQueryKeys } from './assessmentQueryKeys'

const catalogStaleTime = 5 * 60 * 1000
const detailsStaleTime = 5 * 60 * 1000
const activeSessionStaleTime = 10 * 1000

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

export function useStartAssessmentSessionMutation(
  assessmentCode: string,
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () =>
      startAssessmentSession(assessmentCode),

    onSuccess: ({ session }) => {
      queryClient.setQueryData(
        assessmentQueryKeys.session(session.id),
        session,
      )

      queryClient.setQueryData(
        assessmentQueryKeys.activeSession(
          assessmentCode,
        ),
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
      queryClient.setQueryData(
        assessmentQueryKeys.session(session.id),
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

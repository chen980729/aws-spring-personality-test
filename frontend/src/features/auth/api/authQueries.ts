import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'

import {
  getCurrentUser,
  loginUser,
  logoutUser,
  registerUser,
} from './authApi'
import { authQueryKeys } from './authQueryKeys'

export function useCurrentUserQuery() {
  return useQuery({
    queryKey: authQueryKeys.me,
    queryFn: getCurrentUser,
    staleTime: 60_000,
    retry: false,
  })
}

export function useRegisterMutation() {
  return useMutation({
    mutationFn: registerUser,
  })
}

export function useLoginMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: loginUser,
    onSuccess: (currentUser) => {
      queryClient.setQueryData(
        authQueryKeys.me,
        currentUser,
      )
    },
  })
}

export function useLogoutMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: logoutUser,
    onSuccess: () => {
      queryClient.setQueryData(
        authQueryKeys.me,
        null,
      )
    },
  })
}

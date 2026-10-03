import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { authQueryKeys } from '../../features/auth/api/authQueryKeys'
import { subscribeToAuthenticationRequired } from '../../shared/api/authenticationRequiredEvents'

export function AuthSessionCoordinator() {
  const queryClient = useQueryClient()

  useEffect(
    () =>
      subscribeToAuthenticationRequired(() => {
        queryClient.setQueryData(
          authQueryKeys.me,
          null,
        )
      }),
    [queryClient],
  )

  return null
}

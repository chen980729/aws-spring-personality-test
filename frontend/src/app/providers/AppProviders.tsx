import type { PropsWithChildren } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'

import { AuthSessionCoordinator } from './AuthSessionCoordinator'
import { queryClient } from './queryClient'

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthSessionCoordinator />
      {children}
    </QueryClientProvider>
  )
}

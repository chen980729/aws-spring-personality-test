import { useQuery } from '@tanstack/react-query'

import { getBackendHealth } from '../../shared/api/backendHealth'

const backendHealthQueryKey = ['backend-health'] as const

export function BackendConnectionStatus() {
  const { data, isError, isPending } = useQuery({
    queryKey: backendHealthQueryKey,
    queryFn: getBackendHealth,
    retry: false,
    staleTime: 30_000,
  })

  if (isPending) {
    return <p>Backend connection: checking...</p>
  }

  if (isError) {
    return <p>Backend connection: unavailable</p>
  }

  return <p>Backend connection: {data.status}</p>
}

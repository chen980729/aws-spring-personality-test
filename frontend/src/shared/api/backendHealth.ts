import { getJson } from './httpClient'

export interface BackendHealth {
  status: string
}

export function getBackendHealth(): Promise<BackendHealth> {
  return getJson<BackendHealth>('/actuator/health')
}

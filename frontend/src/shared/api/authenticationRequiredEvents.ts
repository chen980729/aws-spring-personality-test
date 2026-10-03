type AuthenticationRequiredListener = () => void

const listeners = new Set<AuthenticationRequiredListener>()

export function subscribeToAuthenticationRequired(
  listener: AuthenticationRequiredListener,
): () => void {
  listeners.add(listener)

  return () => {
    listeners.delete(listener)
  }
}

export function notifyAuthenticationRequired(): void {
  for (const listener of listeners) {
    listener()
  }
}

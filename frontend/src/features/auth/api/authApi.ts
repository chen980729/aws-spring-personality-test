import { isApiError } from '../../../shared/api/apiError'
import { csrfTokenManager } from '../../../shared/api/csrfTokenManager'
import {
  getJson,
  postJson,
} from '../../../shared/api/httpClient'
import type {
  CurrentUser,
  LoginRequest,
  RegisteredUser,
  RegisterRequest,
} from './authTypes'

export function registerUser(
  request: RegisterRequest,
): Promise<RegisteredUser> {
  return postJson<RegisteredUser>(
    '/api/v1/auth/register',
    request,
  )
}

export async function loginUser(
  request: LoginRequest,
): Promise<CurrentUser> {
  const currentUser = await postJson<CurrentUser>(
    '/api/v1/auth/login',
    request,
  )

  // Successful authentication rotates the session/CSRF lifecycle
  // on the Spring Security side. Do not keep using the anonymous
  // session's cached CSRF token.
  await csrfTokenManager.refresh()

  return currentUser
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await getJson<CurrentUser>(
      '/api/v1/users/me',
    )
  } catch (error) {
    if (
      isApiError(error) &&
      error.code === 'AUTHENTICATION_REQUIRED'
    ) {
      return null
    }

    throw error
  }
}

export async function logoutUser(): Promise<void> {
  try {
    await postJson<void>(
      '/api/v1/auth/logout',
    )
  } catch (error) {
    // From the user's perspective an already-expired session is
    // already logged out, so treat this specific response as success.
    if (
      !isApiError(error) ||
      error.code !== 'AUTHENTICATION_REQUIRED'
    ) {
      throw error
    }
  }

  // Logout invalidates the authenticated HttpSession. Any cached
  // token belongs to the old session and must not be reused.
  csrfTokenManager.invalidate()
}

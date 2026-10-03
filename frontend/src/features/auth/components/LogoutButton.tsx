import { useState } from 'react'
import { useNavigate } from 'react-router'

import { useLogoutMutation } from '../api/authQueries'

export function LogoutButton() {
  const navigate = useNavigate()
  const logoutMutation = useLogoutMutation()
  const [errorMessage, setErrorMessage] =
    useState<string>()

  async function handleLogout() {
    setErrorMessage(undefined)

    try {
      await logoutMutation.mutateAsync()

      navigate('/login', {
        replace: true,
      })
    } catch {
      setErrorMessage(
        'Unable to sign out right now. Please try again.',
      )
    }
  }

  return (
    <div>
      <button
        type="button"
        onClick={handleLogout}
        disabled={logoutMutation.isPending}
      >
        {logoutMutation.isPending
          ? 'Signing out...'
          : 'Sign out'}
      </button>

      {errorMessage && (
        <p role="alert">
          {errorMessage}
        </p>
      )}
    </div>
  )
}

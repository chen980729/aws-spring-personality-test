import {
  Navigate,
  Outlet,
  useLocation,
} from 'react-router'

import { useCurrentUserQuery } from '../../features/auth/api/authQueries'
import { LogoutButton } from '../../features/auth/components/LogoutButton'
import { buildLoginPath } from '../../features/auth/navigation/returnTo'

export function AuthenticatedLayout() {
  const location = useLocation()
  const currentUserQuery = useCurrentUserQuery()

  if (currentUserQuery.isPending) {
    return <p>Checking your session...</p>
  }

  if (currentUserQuery.isError) {
    return (
      <main>
        <h1>Unable to verify your session</h1>
        <p role="alert">
          Please refresh the page and try again.
        </p>
      </main>
    )
  }

  if (currentUserQuery.data === null) {
    const returnTo =
      `${location.pathname}${location.search}${location.hash}`

    return (
      <Navigate
        to={buildLoginPath(returnTo)}
        replace
      />
    )
  }

  return (
    <>
      <header>
        <p>
          Signed in as {currentUserQuery.data.displayName}
        </p>
        <LogoutButton />
      </header>

      <Outlet />
    </>
  )
}

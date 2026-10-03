import {
  Link,
  Navigate,
  NavLink,
  Outlet,
  useLocation,
} from 'react-router'

import { useCurrentUserQuery } from '../../features/auth/api/authQueries'
import { LogoutButton } from '../../features/auth/components/LogoutButton'
import { buildLoginPath } from '../../features/auth/navigation/returnTo'
import '../styles/layouts.css'

export function AuthenticatedLayout() {
  const location = useLocation()
  const currentUserQuery = useCurrentUserQuery()

  if (currentUserQuery.isPending) {
    return (
      <main className="session-gate">
        <div className="session-gate__panel">
          <span
            className="session-gate__indicator"
            aria-hidden="true"
          />
          <p>Checking your session...</p>
        </div>
      </main>
    )
  }

  if (currentUserQuery.isError) {
    return (
      <main className="session-gate">
        <div className="session-gate__panel session-gate__panel--error">
          <p className="session-gate__eyebrow">
            Session error
          </p>
          <h1>Unable to verify your session</h1>
          <p role="alert">
            Please refresh the page and try again.
          </p>
        </div>
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

  const currentUser = currentUserQuery.data
  const userInitial =
    currentUser.displayName.trim().charAt(0).toUpperCase() || 'U'

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div className="app-shell__header-inner">
          <Link
            className="app-shell__brand"
            to="/assessments"
            aria-label="Personality Lab assessments"
          >
            <span
              className="app-shell__brand-mark"
              aria-hidden="true"
            >
              P
            </span>
            <span className="app-shell__brand-copy">
              <strong>Personality Lab</strong>
              <span>Assessment workspace</span>
            </span>
          </Link>

          <nav
            className="app-shell__nav"
            aria-label="Primary navigation"
          >
            <NavLink
              className={({ isActive }) =>
                isActive
                  ? 'app-shell__nav-link app-shell__nav-link--active'
                  : 'app-shell__nav-link'
              }
              to="/assessments"
            >
              Assessments
            </NavLink>

            <NavLink
              className={({ isActive }) =>
                isActive
                  ? 'app-shell__nav-link app-shell__nav-link--active'
                  : 'app-shell__nav-link'
              }
              to="/history"
            >
              History
            </NavLink>
          </nav>

          <div className="app-shell__account">
            <div
              className="app-shell__identity"
              aria-label={`Signed in as ${currentUser.displayName}`}
            >
              <span
                className="app-shell__avatar"
                aria-hidden="true"
              >
                {userInitial}
              </span>
              <span className="app-shell__identity-copy">
                <span>Signed in as</span>
                <strong>{currentUser.displayName}</strong>
              </span>
            </div>

            <div className="app-shell__logout">
              <LogoutButton />
            </div>
          </div>
        </div>
      </header>

      <div className="app-shell__content">
        <Outlet />
      </div>
    </div>
  )
}

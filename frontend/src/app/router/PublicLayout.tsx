import { Link, Outlet } from 'react-router'

import '../styles/layouts.css'

export function PublicLayout() {
  return (
    <div className="public-shell">
      <header className="public-shell__header">
        <div className="public-shell__header-inner">
          <Link
            className="public-shell__brand"
            to="/"
            aria-label="Personality Lab home"
          >
            <span
              className="public-shell__brand-mark"
              aria-hidden="true"
            >
              P
            </span>
            <span className="public-shell__brand-copy">
              <strong>Personality Lab</strong>
              <span>Spring AWS Portfolio</span>
            </span>
          </Link>

          <nav className="public-shell__nav" aria-label="Public navigation">
            <Link to="/#about">About</Link>
            <Link to="/#features">Features</Link>
          </nav>

          <div className="public-shell__actions">
            <Link className="public-shell__login-link" to="/login">
              Log in
            </Link>
            <Link className="public-shell__cta" to="/register">
              Get started
            </Link>
          </div>
        </div>
      </header>

      <div className="public-shell__content">
        <Outlet />
      </div>

      <footer className="public-shell__footer">
        <p>
          Built with React, Spring Boot, PostgreSQL, and AWS.
        </p>
      </footer>
    </div>
  )
}

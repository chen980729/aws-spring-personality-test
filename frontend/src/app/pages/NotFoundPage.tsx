import { Link } from 'react-router'

import './NotFoundPage.css'

export function NotFoundPage() {
  return (
    <main className="not-found-page">
      <section className="not-found-card" aria-labelledby="not-found-title">
        <span className="not-found-card__code" aria-hidden="true">
          404
        </span>
        <p className="not-found-card__eyebrow">Page not found</p>
        <h1 id="not-found-title">This route does not exist.</h1>
        <p className="not-found-card__description">
          The page may have moved, or the address may be incorrect. You can
          return to the public home page or sign in to continue.
        </p>

        <div className="not-found-card__actions">
          <Link className="not-found-card__action not-found-card__action--primary" to="/">
            Go home
          </Link>
          <Link className="not-found-card__action" to="/login">
            Sign in
          </Link>
        </div>
      </section>
    </main>
  )
}

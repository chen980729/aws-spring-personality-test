import { Link } from 'react-router'

import { BackendConnectionStatus } from '../components/BackendConnectionStatus'
import { Card } from '../../shared/ui'

import './LandingPage.css'

const features = [
  {
    icon: '16',
    title: '16 personality types',
    description:
      'Explore a clear personality profile built from four familiar preference dimensions.',
  },
  {
    icon: '?',
    title: 'Thoughtful questions',
    description:
      'Move through a focused questionnaire with clarification steps when an answer is close.',
  },
  {
    icon: '◎',
    title: 'Personal insights',
    description:
      'Review your result, dimension balance, and previous assessment history in one place.',
  },
] as const

export function LandingPage() {
  return (
    <main className="landing-page">
      <section className="landing-hero" aria-labelledby="landing-title">
        <div className="landing-hero__content">
          <p className="landing-hero__eyebrow">
            Full-stack personality assessment
          </p>

          <h1 id="landing-title">
            Understand yourself.{' '}
            <span>Discover your patterns.</span>
          </h1>

          <p className="landing-hero__lead">
            Take a structured 16-type personality assessment, explore four
            preference dimensions, and keep a history of how your results
            develop over time.
          </p>

          <div className="landing-hero__actions">
            <Link
              className="landing-action landing-action--primary"
              to="/register"
            >
              Get started
              <span aria-hidden="true">→</span>
            </Link>

            <Link
              className="landing-action landing-action--secondary"
              to="/login"
            >
              I already have an account
            </Link>
          </div>

          <div className="landing-hero__meta" aria-label="Application highlights">
            <span>Guided assessment flow</span>
            <span>Clarification &amp; tie-break stages</span>
            <span>Assessment history</span>
          </div>
        </div>

        <figure className="landing-visual">
          <div className="landing-visual__backdrop" aria-hidden="true" />
          <img
            src="/landing-personality-groups.webp"
            alt="Four illustrated personality profiles representing the assessment experience"
          />
        </figure>
      </section>

      <section
        className="landing-features"
        id="features"
        aria-labelledby="features-title"
      >
        <div className="landing-features__heading" id="about">
          <p className="landing-features__eyebrow">Designed for a clear journey</p>
          <h2 id="features-title">From first answer to useful result</h2>
          <p>
            The experience keeps each stage focused while the application
            handles progress, clarification, and result history behind the
            scenes.
          </p>
        </div>

        <div className="landing-features__grid">
          {features.map((feature) => (
            <Card
              className="landing-feature-card"
              key={feature.title}
              padding="lg"
            >
              <span className="landing-feature-card__icon" aria-hidden="true">
                {feature.icon}
              </span>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </Card>
          ))}
        </div>
      </section>

      {import.meta.env.DEV && (
        <aside className="landing-dev-status" aria-label="Development status">
          <BackendConnectionStatus />
        </aside>
      )}
    </main>
  )
}

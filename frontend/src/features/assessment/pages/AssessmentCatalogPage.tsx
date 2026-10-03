import { Link } from 'react-router'

import { PageHeader } from '../../../shared/ui'
import { useAssessmentCatalogQuery } from '../api/assessmentQueries'
import './AssessmentPages.css'

function AssessmentCatalogState({
  message,
  isError = false,
}: {
  message: string
  isError?: boolean
}) {
  return (
    <main className="assessment-page">
      <PageHeader
        eyebrow="Assessment library"
        title="Assessments"
        description="Choose an assessment and explore a structured view of your personality preferences."
      />

      <section
        className={
          isError
            ? 'assessment-state assessment-state--error'
            : 'assessment-state'
        }
        aria-live={isError ? undefined : 'polite'}
      >
        <span
          className="assessment-state__icon"
          aria-hidden="true"
        >
          {isError ? '!' : '…'}
        </span>
        <p role={isError ? 'alert' : undefined}>{message}</p>
      </section>
    </main>
  )
}

export function AssessmentCatalogPage() {
  const catalogQuery = useAssessmentCatalogQuery()

  if (catalogQuery.isPending) {
    return (
      <AssessmentCatalogState message="Loading assessments..." />
    )
  }

  if (catalogQuery.isError) {
    return (
      <AssessmentCatalogState
        message="Unable to load assessments right now."
        isError
      />
    )
  }

  return (
    <main className="assessment-page">
      <PageHeader
        eyebrow="Assessment library"
        title="Assessments"
        description="Choose an assessment and explore a structured view of your personality preferences."
      />

      {catalogQuery.data.items.length === 0 ? (
        <section className="assessment-empty-state">
          <span
            className="assessment-empty-state__icon"
            aria-hidden="true"
          >
            ◇
          </span>
          <div>
            <h2>No assessments available</h2>
            <p>No assessments are currently available.</p>
          </div>
        </section>
      ) : (
        <ul className="assessment-catalog" aria-label="Available assessments">
          {catalogQuery.data.items.map((assessment) => (
            <li key={assessment.code} className="assessment-catalog__item">
              <article className="assessment-catalog-card">
                <div
                  className="assessment-catalog-card__icon"
                  aria-hidden="true"
                >
                  <svg viewBox="0 0 24 24" role="presentation">
                    <path d="M12 3.75a3.25 3.25 0 1 0 0 6.5 3.25 3.25 0 0 0 0-6.5ZM5.75 13.25a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5ZM18.25 13.25a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5ZM9.5 8.5 7 14m7.5-5.5 2.5 5.5m-9 2h8" />
                  </svg>
                </div>

                <div className="assessment-catalog-card__content">
                  <div className="assessment-catalog-card__heading">
                    <div>
                      <p className="assessment-catalog-card__eyebrow">
                        Personality assessment
                      </p>
                      <h2>
                        <Link
                          to={`/assessments/${encodeURIComponent(
                            assessment.code,
                          )}`}
                        >
                          {assessment.name}
                        </Link>
                      </h2>
                    </div>

                    <span className="assessment-catalog-card__availability">
                      Available
                    </span>
                  </div>

                  <p className="assessment-catalog-card__description">
                    Work through a guided questionnaire and review a clear,
                    reusable result when you finish.
                  </p>

                  <div className="assessment-catalog-card__footer">
                    <p className="assessment-catalog-card__meta">
                      Version {assessment.availableVersion}
                      {' · '}
                      {assessment.questionCount} questions
                    </p>

                    <Link
                      className="assessment-catalog-card__action"
                      to={`/assessments/${encodeURIComponent(
                        assessment.code,
                      )}`}
                    >
                      View assessment
                      <span aria-hidden="true">→</span>
                    </Link>
                  </div>
                </div>
              </article>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}

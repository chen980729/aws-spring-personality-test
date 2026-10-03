import {
  Link,
  useLocation,
  useSearchParams,
} from 'react-router'

import { PageHeader } from '../../../shared/ui'
import { useAssessmentHistoryQuery } from '../api/assessmentQueries'
import type { AssessmentHistoryItem } from '../api/assessmentTypes'
import { buildHistoryReturnTo } from '../history/assessmentHistoryNavigation'
import {
  assessmentHistoryHref,
  assessmentHistoryPageSize,
  parseAssessmentHistoryPage,
} from '../history/historyPagination'
import './AssessmentPages.css'

function formatCompletedAt(completedAt: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(completedAt))
}

function formatAssessmentCode(assessmentCode: string): string {
  return assessmentCode
    .toLowerCase()
    .split('_')
    .map((word) => `${word.charAt(0).toUpperCase()}${word.slice(1)}`)
    .join(' ')
}

function AssessmentHistoryRecord({
  item,
  historyReturnTo,
}: {
  item: AssessmentHistoryItem
  historyReturnTo: string
}) {
  return (
    <article
      className="assessment-history-card"
      aria-labelledby={`history-${item.sessionId}`}
    >
      <div className="assessment-history-card__result">
        <span className="assessment-history-card__result-label">
          Result
        </span>
        <h2 id={`history-${item.sessionId}`}>{item.finalType}</h2>
      </div>

      <div className="assessment-history-card__content">
        <div className="assessment-history-card__title-row">
          <div>
            <p className="assessment-history-card__assessment-name">
              {formatAssessmentCode(item.assessmentCode)}
            </p>
            <p className="assessment-history-card__completed-copy">
              Completed assessment
            </p>
          </div>

          <Link
            className="assessment-history-card__action"
            to={`/assessment-sessions/${encodeURIComponent(
              item.sessionId,
            )}`}
            state={{
              historyReturnTo,
            }}
          >
            View result
            <span aria-hidden="true">→</span>
          </Link>
        </div>

        <dl className="assessment-history-card__metadata">
          <div>
            <dt>Assessment</dt>
            <dd>{item.assessmentCode}</dd>
          </div>

          <div>
            <dt>Version</dt>
            <dd>{item.assessmentVersion}</dd>
          </div>

          <div>
            <dt>Completed</dt>
            <dd>
              <time dateTime={item.completedAt}>
                {formatCompletedAt(item.completedAt)}
              </time>
            </dd>
          </div>
        </dl>
      </div>
    </article>
  )
}

function AssessmentHistoryState({
  message,
  isError = false,
}: {
  message: string
  isError?: boolean
}) {
  return (
    <main className="assessment-page">
      <PageHeader
        eyebrow="Your progress"
        title="Assessment history"
        description="Review completed assessments and revisit the personality results you have already finished."
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

export function AssessmentHistoryPage() {
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const historyReturnTo = buildHistoryReturnTo(
    location.pathname,
    location.search,
  )

  const requestedPage = parseAssessmentHistoryPage(
    searchParams.get('page'),
  )

  const historyQuery = useAssessmentHistoryQuery(
    requestedPage,
    assessmentHistoryPageSize,
  )

  if (historyQuery.isPending) {
    return (
      <AssessmentHistoryState message="Loading completed assessments..." />
    )
  }

  if (historyQuery.isError) {
    return (
      <AssessmentHistoryState
        message="Unable to load assessment history."
        isError
      />
    )
  }

  const history = historyQuery.data
  const completedAssessmentLabel =
    history.totalElements === 1
      ? 'completed assessment'
      : 'completed assessments'

  return (
    <main className="assessment-page">
      <PageHeader
        eyebrow="Your progress"
        title="Assessment history"
        description="Completed assessments are listed from newest to oldest, so your latest result is always easy to find."
        actions={
          <div className="assessment-history-summary" aria-label="History summary">
            <strong>{history.totalElements}</strong>
            <span>{completedAssessmentLabel}</span>
          </div>
        }
      />

      <p className="assessment-history-count">
        {history.totalElements} {completedAssessmentLabel}
      </p>

      {history.totalElements === 0 ? (
        <section className="assessment-empty-state">
          <span
            className="assessment-empty-state__icon"
            aria-hidden="true"
          >
            ◇
          </span>

          <div>
            <h2>No completed assessments yet</h2>
            <p>Complete an assessment and it will appear here.</p>
            <Link
              className="assessment-empty-state__action"
              to="/assessments"
            >
              Browse assessments
              <span aria-hidden="true">→</span>
            </Link>
          </div>
        </section>
      ) : (
        <>
          {history.items.length === 0 ? (
            <section className="assessment-empty-state assessment-empty-state--compact">
              <div>
                <h2>No results on this page</h2>
                <p>There are no completed assessments on this page.</p>
              </div>
            </section>
          ) : (
            <div className="assessment-history-list">
              {history.items.map((item) => (
                <AssessmentHistoryRecord
                  key={item.sessionId}
                  item={item}
                  historyReturnTo={historyReturnTo}
                />
              ))}
            </div>
          )}

          <nav
            className="assessment-pagination"
            aria-label="Assessment history pagination"
          >
            <div className="assessment-pagination__side">
              {history.page > 1 && (
                <Link
                  className="assessment-pagination__link"
                  to={assessmentHistoryHref(history.page - 1)}
                >
                  <span aria-hidden="true">←</span>
                  Previous page
                </Link>
              )}
            </div>

            <p>Page {history.page} of {history.totalPages}</p>

            <div className="assessment-pagination__side assessment-pagination__side--end">
              {history.page < history.totalPages && (
                <Link
                  className="assessment-pagination__link"
                  to={assessmentHistoryHref(history.page + 1)}
                >
                  Next page
                  <span aria-hidden="true">→</span>
                </Link>
              )}
            </div>
          </nav>
        </>
      )}
    </main>
  )
}

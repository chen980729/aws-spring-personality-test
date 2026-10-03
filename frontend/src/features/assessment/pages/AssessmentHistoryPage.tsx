import {
  Link,
  useLocation,
  useSearchParams,
} from 'react-router'

import { useAssessmentHistoryQuery } from '../api/assessmentQueries'
import type { AssessmentHistoryItem } from '../api/assessmentTypes'
import {
  assessmentHistoryHref,
  assessmentHistoryPageSize,
  parseAssessmentHistoryPage,
} from '../history/historyPagination'
import { buildHistoryReturnTo } from '../history/assessmentHistoryNavigation'

function formatCompletedAt(
  completedAt: string,
): string {
  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: 'medium',
      timeStyle: 'short',
    },
  ).format(
    new Date(completedAt),
  )
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
      aria-labelledby={`history-${item.sessionId}`}
    >
      <h2
        id={`history-${item.sessionId}`}
      >
        {item.finalType}
      </h2>

      <dl>
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
              {formatCompletedAt(
                item.completedAt,
              )}
            </time>
          </dd>
        </div>
      </dl>

      <Link
        to={`/assessment-sessions/${encodeURIComponent(
          item.sessionId,
        )}`}
        state={{
          historyReturnTo,
        }}
      >
        View result
      </Link>
    </article>
  )
}

export function AssessmentHistoryPage() {
  const location = useLocation()

  const [searchParams] =
    useSearchParams()

  const historyReturnTo =
    buildHistoryReturnTo(
      location.pathname,
      location.search,
    )

  const requestedPage =
    parseAssessmentHistoryPage(
      searchParams.get('page'),
    )

  const historyQuery =
    useAssessmentHistoryQuery(
      requestedPage,
      assessmentHistoryPageSize,
    )

  if (historyQuery.isPending) {
    return (
      <main>
        <h1>Assessment history</h1>
        <p>Loading completed assessments...</p>
      </main>
    )
  }

  if (historyQuery.isError) {
    return (
      <main>
        <h1>Assessment history</h1>

        <p role="alert">
          Unable to load assessment history.
        </p>
      </main>
    )
  }

  const history = historyQuery.data

  return (
    <main>
      <h1>Assessment history</h1>

      <p>
        Completed assessments are listed from newest to
        oldest.
      </p>

      <p>
        {history.totalElements}{' '}
        {history.totalElements === 1
          ? 'completed assessment'
          : 'completed assessments'}
      </p>

      {history.totalElements === 0 ? (
        <section>
          <h2>No completed assessments yet</h2>

          <p>
            Complete an assessment and it will appear here.
          </p>

          <Link to="/assessments">
            Browse assessments
          </Link>
        </section>
      ) : (
        <>
          {history.items.length === 0 ? (
            <p>
              There are no completed assessments on this
              page.
            </p>
          ) : (
            <div>
              {history.items.map(
                (item) => (
                  <AssessmentHistoryRecord
                    key={item.sessionId}
                    item={item}
                    historyReturnTo={
                      historyReturnTo
                    }
                  />
                ),
              )}
            </div>
          )}

          <nav aria-label="Assessment history pagination">
            <p>
              Page {history.page} of{' '}
              {history.totalPages}
            </p>

            {history.page > 1 && (
              <Link
                to={assessmentHistoryHref(
                  history.page - 1,
                )}
              >
                Previous page
              </Link>
            )}

            {history.page <
              history.totalPages && (
              <Link
                to={assessmentHistoryHref(
                  history.page + 1,
                )}
              >
                Next page
              </Link>
            )}
          </nav>
        </>
      )}
    </main>
  )
}

import { Link } from 'react-router'

import { useAssessmentCatalogQuery } from '../api/assessmentQueries'

export function AssessmentCatalogPage() {
  const catalogQuery = useAssessmentCatalogQuery()

  if (catalogQuery.isPending) {
    return (
      <main>
        <h1>Assessments</h1>
        <p>Loading assessments...</p>
      </main>
    )
  }

  if (catalogQuery.isError) {
    return (
      <main>
        <h1>Assessments</h1>
        <p role="alert">
          Unable to load assessments right now.
        </p>
      </main>
    )
  }

  return (
    <main>
      <h1>Assessments</h1>

      {catalogQuery.data.items.length === 0 ? (
        <p>No assessments are currently available.</p>
      ) : (
        <ul>
          {catalogQuery.data.items.map(
            (assessment) => (
              <li key={assessment.code}>
                <h2>
                  <Link
                    to={`/assessments/${encodeURIComponent(
                      assessment.code,
                    )}`}
                  >
                    {assessment.name}
                  </Link>
                </h2>

                <p>
                  Version {assessment.availableVersion}
                  {' · '}
                  {assessment.questionCount} questions
                </p>
              </li>
            ),
          )}
        </ul>
      )}
    </main>
  )
}

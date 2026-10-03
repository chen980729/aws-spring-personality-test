import { Link, useParams } from 'react-router'

import { useAssessmentSessionQuery } from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function isValidSessionId(
  sessionId: string,
): boolean {
  return uuidPattern.test(sessionId)
}

interface AbandonedSessionViewProps {
  session: AssessmentSession
}

function AbandonedSessionView({
  session,
}: AbandonedSessionViewProps) {
  return (
    <main>
      <h1>Assessment Session</h1>

      <h2>This session was abandoned</h2>

      <p>
        A new assessment was started, so this session can
        no longer be resumed.
      </p>

      <p>
        <Link
          to={`/assessments/${encodeURIComponent(
            session.assessment.code,
          )}`}
        >
          Return to assessment
        </Link>
      </p>
    </main>
  )
}

interface AssessmentSessionContentProps {
  sessionId: string
}

function AssessmentSessionContent({
  sessionId,
}: AssessmentSessionContentProps) {
  const sessionQuery =
    useAssessmentSessionQuery(sessionId)

  if (sessionQuery.isPending) {
    return (
      <main>
        <h1>Assessment Session</h1>
        <p>Loading session...</p>
      </main>
    )
  }

  if (sessionQuery.isError) {
    return (
      <main>
        <h1>Assessment Session</h1>
        <p role="alert">
          Unable to load this assessment session.
        </p>
      </main>
    )
  }

  const session = sessionQuery.data

  if (session.status === 'ABANDONED') {
    return (
      <AbandonedSessionView
        session={session}
      />
    )
  }

  return (
    <main>
      <p>
        <Link
          to={`/assessments/${encodeURIComponent(
            session.assessment.code,
          )}`}
        >
          ← Back to assessment
        </Link>
      </p>

      <h1>Assessment Session</h1>

      <dl>
        <div>
          <dt>Assessment</dt>
          <dd>{session.assessment.code}</dd>
        </div>

        <div>
          <dt>Version</dt>
          <dd>{session.assessment.version}</dd>
        </div>

        <div>
          <dt>Status</dt>
          <dd>{session.status}</dd>
        </div>
      </dl>

      <p>
        Questionnaire execution will be added in the next
        frontend milestone.
      </p>
    </main>
  )
}

export function AssessmentSessionPage() {
  const { sessionId } = useParams()

  if (
    !sessionId ||
    !isValidSessionId(sessionId)
  ) {
    return (
      <main>
        <h1>Assessment Session</h1>

        <p role="alert">
          This assessment session URL is invalid.
        </p>

        <p>
          <Link to="/assessments">
            Return to assessments
          </Link>
        </p>
      </main>
    )
  }

  return (
    <AssessmentSessionContent
      sessionId={sessionId}
    />
  )
}

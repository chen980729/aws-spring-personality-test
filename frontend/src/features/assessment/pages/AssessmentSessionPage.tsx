import { Link, useParams } from 'react-router'

import { useAssessmentSessionQuery } from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { ClarificationStage } from '../clarification/ClarificationStage'
import { QuestionnaireStage } from '../questionnaire/QuestionnaireStage'
import { TieBreakStage } from '../tiebreak/TieBreakStage'
import {
  resolveAssessmentWorkflow,
  type AssessmentWorkflowView,
} from '../workflow/assessmentWorkflowResolver'

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function isValidSessionId(
  sessionId: string,
): boolean {
  return uuidPattern.test(sessionId)
}

function AbandonedSessionView({
  session,
}: {
  session: AssessmentSession
}) {
  return (
    <section>
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
    </section>
  )
}

function CompletedSessionView({
  session,
}: {
  session: AssessmentSession
}) {
  return (
    <section>
      <h2>Assessment complete</h2>

      {session.finalResult ? (
        <p>
          Final type:{' '}
          <strong>
            {session.finalResult.finalType}
          </strong>
        </p>
      ) : (
        <p>
          The assessment is complete.
        </p>
      )}
    </section>
  )
}

function RecoveryView({
  reason,
}: {
  reason:
    Extract<
      AssessmentWorkflowView,
      { kind: 'recovery' }
    >['reason']
}) {
  return (
    <section>
      <h2>Assessment state needs recovery</h2>

      <p role="alert">
        The current assessment response does not describe
        a consistent workflow view. Refresh the page before
        continuing.
      </p>

      <p>
        Recovery reason: {reason}
      </p>
    </section>
  )
}

function AssessmentWorkflowContent({
  session,
}: {
  session: AssessmentSession
}) {
  const workflowView =
    resolveAssessmentWorkflow(session)

  switch (workflowView.kind) {
    case 'questionnaire':
      return (
        <QuestionnaireStage
          sessionId={session.id}
        />
      )

    case 'clarification':
      return (
        <ClarificationStage
          session={session}
          dimensionCode={
            workflowView.dimensionCode
          }
          mode={workflowView.mode}
        />
      )

    case 'tie-break':
      return (
        <TieBreakStage
          key={
            workflowView.dimensionCode
          }
          session={session}
          dimensionCode={
            workflowView.dimensionCode
          }
        />
      )

    case 'completed':
      return (
        <CompletedSessionView
          session={session}
        />
      )

    case 'abandoned':
      return (
        <AbandonedSessionView
          session={session}
        />
      )

    case 'recovery':
      return (
        <RecoveryView
          reason={workflowView.reason}
        />
      )
  }
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

      <AssessmentWorkflowContent
        session={session}
      />
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

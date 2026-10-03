import {
  useState,
} from 'react'
import {
  Link,
  useNavigate,
  useParams,
} from 'react-router'

import {
  useActiveAssessmentSessionQuery,
  useAssessmentDetailsQuery,
  useRestartAssessmentSessionMutation,
  useStartAssessmentSessionMutation,
} from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'

interface ActiveSessionActionsProps {
  assessmentCode: string
  activeSession: AssessmentSession
}

function ActiveSessionActions({
  assessmentCode,
  activeSession,
}: ActiveSessionActionsProps) {
  const navigate = useNavigate()
  const [confirmingRestart, setConfirmingRestart] =
    useState(false)

  const restartMutation =
    useRestartAssessmentSessionMutation(
      assessmentCode,
      activeSession.id,
    )

  async function handleRestart() {
    try {
      const result =
        await restartMutation.mutateAsync()

      navigate(
        `/assessment-sessions/${result.session.id}`,
        {
          replace: true,
        },
      )
    } catch {
      // Keep the confirmation UI visible so the user can
      // understand the failure and deliberately retry.
    }
  }

  return (
    <section aria-labelledby="active-session-heading">
      <h2 id="active-session-heading">
        Assessment in progress
      </h2>

      <p>
        You already have an active session for this
        assessment.
      </p>

      <p>
        <Link
          to={`/assessment-sessions/${activeSession.id}`}
        >
          Resume assessment
        </Link>
      </p>

      {!confirmingRestart ? (
        <button
          type="button"
          onClick={() => setConfirmingRestart(true)}
        >
          Start new assessment
        </button>
      ) : (
        <section aria-labelledby="restart-confirmation-heading">
          <h3 id="restart-confirmation-heading">
            Start over?
          </h3>

          <p>
            Your current assessment progress will be
            abandoned and cannot be resumed.
          </p>

          <button
            type="button"
            onClick={() =>
              setConfirmingRestart(false)
            }
            disabled={restartMutation.isPending}
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={handleRestart}
            disabled={restartMutation.isPending}
          >
            {restartMutation.isPending
              ? 'Starting new assessment...'
              : 'Confirm start new assessment'}
          </button>

          {restartMutation.isError && (
            <p role="alert">
              Unable to start a new assessment right now.
              Your current session has not been replaced
              in this view. Please try again.
            </p>
          )}
        </section>
      )}
    </section>
  )
}

interface AssessmentDetailContentProps {
  assessmentCode: string
}

function AssessmentDetailContent({
  assessmentCode,
}: AssessmentDetailContentProps) {
  const navigate = useNavigate()

  const detailsQuery =
    useAssessmentDetailsQuery(assessmentCode)

  const activeSessionQuery =
    useActiveAssessmentSessionQuery(
      assessmentCode,
    )

  const startMutation =
    useStartAssessmentSessionMutation(
      assessmentCode,
    )

  if (
    detailsQuery.isPending ||
    activeSessionQuery.isPending
  ) {
    return (
      <main>
        <h1>Assessment</h1>
        <p>Loading assessment...</p>
      </main>
    )
  }

  if (
    detailsQuery.isError ||
    activeSessionQuery.isError
  ) {
    return (
      <main>
        <h1>Assessment</h1>
        <p role="alert">
          Unable to load this assessment right now.
        </p>
      </main>
    )
  }

  const assessment = detailsQuery.data
  const activeSession = activeSessionQuery.data

  async function handleStart() {
    try {
      const result =
        await startMutation.mutateAsync()

      navigate(
        `/assessment-sessions/${result.session.id}`,
      )
    } catch {
      // The mutation error is rendered below. Keep the user
      // on the detail page so they can retry deliberately.
    }
  }

  return (
    <main>
      <p>
        <Link to="/assessments">
          ← Back to assessments
        </Link>
      </p>

      <h1>{assessment.name}</h1>

      <dl>
        <div>
          <dt>Version</dt>
          <dd>{assessment.version}</dd>
        </div>

        <div>
          <dt>Questions</dt>
          <dd>{assessment.questionCount}</dd>
        </div>
      </dl>

      {activeSession ? (
        <ActiveSessionActions
          assessmentCode={assessmentCode}
          activeSession={activeSession}
        />
      ) : (
        <section aria-labelledby="new-session-heading">
          <h2 id="new-session-heading">
            Ready to begin
          </h2>

          <p>
            You do not have an active session for this
            assessment.
          </p>

          <button
            type="button"
            onClick={handleStart}
            disabled={startMutation.isPending}
          >
            {startMutation.isPending
              ? 'Starting assessment...'
              : 'Start assessment'}
          </button>

          {startMutation.isError && (
            <p role="alert">
              Unable to start this assessment right now.
              Please try again.
            </p>
          )}
        </section>
      )}
    </main>
  )
}

export function AssessmentDetailPage() {
  const { assessmentCode } = useParams()

  if (!assessmentCode) {
    return (
      <main>
        <h1>Assessment</h1>
        <p role="alert">
          Assessment code is missing.
        </p>
      </main>
    )
  }

  return (
    <AssessmentDetailContent
      assessmentCode={assessmentCode}
    />
  )
}

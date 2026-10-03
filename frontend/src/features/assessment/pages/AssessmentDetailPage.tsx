import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'

import { Button, Card, PageHeader } from '../../../shared/ui'
import {
  useActiveAssessmentSessionQuery,
  useAssessmentDetailsQuery,
  useRestartAssessmentSessionMutation,
  useStartAssessmentSessionMutation,
} from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import './AssessmentPages.css'

interface ActiveSessionActionsProps {
  assessmentCode: string
  activeSession: AssessmentSession
}

function ActiveSessionActions({ assessmentCode, activeSession }: ActiveSessionActionsProps) {
  const navigate = useNavigate()
  const [confirmingRestart, setConfirmingRestart] = useState(false)
  const restartMutation = useRestartAssessmentSessionMutation(
    assessmentCode,
    activeSession.id,
  )

  async function handleRestart() {
    try {
      const result = await restartMutation.mutateAsync()
      navigate(`/assessment-sessions/${result.session.id}`, { replace: true })
    } catch {
      // Keep confirmation visible so retry remains deliberate.
    }
  }

  return (
    <Card className="assessment-detail__action-card" padding="lg">
      <div className="assessment-detail__action-icon" aria-hidden="true">↗</div>
      <div className="assessment-detail__action-copy">
        <p className="assessment-detail__eyebrow">Active session</p>
        <h2 id="active-session-heading">Assessment in progress</h2>
        <p>You already have an active session for this assessment.</p>

        <div className="assessment-detail__actions">
          <Link
            className="assessment-link-button assessment-link-button--primary"
            to={`/assessment-sessions/${activeSession.id}`}
          >
            Resume assessment
          </Link>

          {!confirmingRestart && (
            <Button
              type="button"
              variant="secondary"
              onClick={() => setConfirmingRestart(true)}
            >
              Start new assessment
            </Button>
          )}
        </div>

        {confirmingRestart && (
          <section
            className="assessment-confirmation"
            aria-labelledby="restart-confirmation-heading"
          >
            <div>
              <h3 id="restart-confirmation-heading">Start over?</h3>
              <p>
                Your current assessment progress will be abandoned and cannot be resumed.
              </p>
            </div>

            <div className="assessment-confirmation__actions">
              <Button
                type="button"
                variant="ghost"
                onClick={() => setConfirmingRestart(false)}
                disabled={restartMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="danger"
                onClick={handleRestart}
                isLoading={restartMutation.isPending}
                loadingLabel="Starting new assessment..."
              >
                Confirm start new assessment
              </Button>
            </div>

            {restartMutation.isError && (
              <p className="assessment-inline-alert" role="alert">
                Unable to start a new assessment right now. Your current session has not
                been replaced in this view. Please try again.
              </p>
            )}
          </section>
        )}
      </div>
    </Card>
  )
}

interface AssessmentDetailContentProps {
  assessmentCode: string
}

function AssessmentDetailContent({ assessmentCode }: AssessmentDetailContentProps) {
  const navigate = useNavigate()
  const detailsQuery = useAssessmentDetailsQuery(assessmentCode)
  const activeSessionQuery = useActiveAssessmentSessionQuery(assessmentCode)
  const startMutation = useStartAssessmentSessionMutation(assessmentCode)

  if (detailsQuery.isPending || activeSessionQuery.isPending) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment" eyebrow="Assessment library" />
        <div className="assessment-state">
          <span className="assessment-state__icon" aria-hidden="true">…</span>
          <p>Loading assessment...</p>
        </div>
      </main>
    )
  }

  if (detailsQuery.isError || activeSessionQuery.isError) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment" eyebrow="Assessment library" />
        <div className="assessment-state assessment-state--error">
          <span className="assessment-state__icon" aria-hidden="true">!</span>
          <p role="alert">Unable to load this assessment right now.</p>
        </div>
      </main>
    )
  }

  const assessment = detailsQuery.data
  const activeSession = activeSessionQuery.data

  async function handleStart() {
    try {
      const result = await startMutation.mutateAsync()
      navigate(`/assessment-sessions/${result.session.id}`)
    } catch {
      // Mutation error is rendered below.
    }
  }

  return (
    <main className="assessment-page assessment-detail">
      <Link className="assessment-back-link" to="/assessments">
        ← Back to assessments
      </Link>

      <PageHeader
        eyebrow="Personality assessment"
        title={assessment.name}
        description="Work through the questionnaire at your own pace. Your answers are saved automatically while the session is active."
      />

      <Card className="assessment-detail__overview" padding="lg">
        <div className="assessment-detail__overview-copy">
          <span className="assessment-detail__hero-icon" aria-hidden="true">✦</span>
          <div>
            <p className="assessment-detail__eyebrow">Assessment overview</p>
            <h2>Four dimensions, one structured result</h2>
            <p>
              Complete the questionnaire first. Ambiguous dimensions may move through
              clarification or a final tie-break before your result is finalized.
            </p>
          </div>
        </div>

        <dl className="assessment-detail__meta">
          <div><dt>Version</dt><dd>{assessment.version}</dd></div>
          <div><dt>Questions</dt><dd>{assessment.questionCount}</dd></div>
          <div><dt>Autosave</dt><dd>Enabled</dd></div>
        </dl>
      </Card>

      {activeSession ? (
        <ActiveSessionActions
          assessmentCode={assessmentCode}
          activeSession={activeSession}
        />
      ) : (
        <Card className="assessment-detail__action-card" padding="lg">
          <div className="assessment-detail__action-icon" aria-hidden="true">→</div>
          <div className="assessment-detail__action-copy">
            <p className="assessment-detail__eyebrow">New session</p>
            <h2 id="new-session-heading">Ready to begin</h2>
            <p>You do not have an active session for this assessment.</p>

            <Button
              type="button"
              onClick={handleStart}
              isLoading={startMutation.isPending}
              loadingLabel="Starting assessment..."
            >
              Start assessment
            </Button>

            {startMutation.isError && (
              <p className="assessment-inline-alert" role="alert">
                Unable to start this assessment right now. Please try again.
              </p>
            )}
          </div>
        </Card>
      )}
    </main>
  )
}

export function AssessmentDetailPage() {
  const { assessmentCode } = useParams()

  if (!assessmentCode) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment" eyebrow="Assessment library" />
        <div className="assessment-state assessment-state--error">
          <span className="assessment-state__icon" aria-hidden="true">!</span>
          <p role="alert">Assessment code is missing.</p>
        </div>
      </main>
    )
  }

  return <AssessmentDetailContent assessmentCode={assessmentCode} />
}

import { Link, useLocation, useParams } from 'react-router'

import { PageHeader } from '../../../shared/ui'
import { useAssessmentSessionQuery } from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { ClarificationStage } from '../clarification/ClarificationStage'
import { readHistoryReturnTo } from '../history/assessmentHistoryNavigation'
import { QuestionnaireStage } from '../questionnaire/QuestionnaireStage'
import { AssessmentResultStage } from '../result/AssessmentResultStage'
import { TieBreakStage } from '../tiebreak/TieBreakStage'
import {
  resolveAssessmentWorkflow,
  type AssessmentWorkflowView,
} from '../workflow/assessmentWorkflowResolver'
import './AssessmentPages.css'

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function isValidSessionId(sessionId: string): boolean {
  return uuidPattern.test(sessionId)
}

function workflowStep(view: AssessmentWorkflowView): number | null {
  switch (view.kind) {
    case 'questionnaire': return 0
    case 'clarification': return 1
    case 'tie-break': return 2
    case 'completed': return 3
    case 'abandoned':
    case 'recovery':
      return null
  }
}

function WorkflowProgress({ view }: { view: AssessmentWorkflowView }) {
  const current = workflowStep(view)
  if (current === null) return null

  const steps = ['Questionnaire', 'Clarification', 'Tie-break', 'Result']

  return (
    <ol className="assessment-workflow-progress" aria-label="Assessment progress">
      {steps.map((label, index) => {
        const state = index < current ? 'complete' : index === current ? 'current' : 'upcoming'
        return (
          <li key={label} className={`assessment-workflow-progress__step assessment-workflow-progress__step--${state}`}>
            <span className="assessment-workflow-progress__marker" aria-hidden="true">
              {index < current ? '✓' : index + 1}
            </span>
            <span>{label}</span>
          </li>
        )
      })}
    </ol>
  )
}

function AbandonedSessionView({ session }: { session: AssessmentSession }) {
  return (
    <section className="assessment-stage assessment-terminal-state">
      <span className="assessment-terminal-state__icon" aria-hidden="true">×</span>
      <div>
        <h2>This session was abandoned</h2>
        <p>A new assessment was started, so this session can no longer be resumed.</p>
        <Link
          className="assessment-link-button"
          to={`/assessments/${encodeURIComponent(session.assessment.code)}`}
        >
          Return to assessment
        </Link>
      </div>
    </section>
  )
}

function RecoveryView({ reason }: {
  reason: Extract<AssessmentWorkflowView, { kind: 'recovery' }>['reason']
}) {
  return (
    <section className="assessment-stage assessment-terminal-state assessment-terminal-state--warning">
      <span className="assessment-terminal-state__icon" aria-hidden="true">!</span>
      <div>
        <h2>Assessment state needs recovery</h2>
        <p role="alert">
          The current assessment response does not describe a consistent workflow view.
          Refresh the page before continuing.
        </p>
        <p className="assessment-terminal-state__reason">Recovery reason: {reason}</p>
      </div>
    </section>
  )
}

function AssessmentWorkflowContent({ session, view }: {
  session: AssessmentSession
  view: AssessmentWorkflowView
}) {
  switch (view.kind) {
    case 'questionnaire':
      return <QuestionnaireStage sessionId={session.id} />
    case 'clarification':
      return (
        <ClarificationStage
          session={session}
          dimensionCode={view.dimensionCode}
          mode={view.mode}
        />
      )
    case 'tie-break':
      return (
        <TieBreakStage
          key={view.dimensionCode}
          session={session}
          dimensionCode={view.dimensionCode}
        />
      )
    case 'completed':
      return <AssessmentResultStage session={session} />
    case 'abandoned':
      return <AbandonedSessionView session={session} />
    case 'recovery':
      return <RecoveryView reason={view.reason} />
  }
}

interface AssessmentSessionContentProps { sessionId: string }

function AssessmentSessionContent({ sessionId }: AssessmentSessionContentProps) {
  const location = useLocation()
  const sessionQuery = useAssessmentSessionQuery(sessionId)

  if (sessionQuery.isPending) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment Session" eyebrow="Assessment workflow" />
        <div className="assessment-state">
          <span className="assessment-state__icon" aria-hidden="true">…</span>
          <p>Loading session...</p>
        </div>
      </main>
    )
  }

  if (sessionQuery.isError) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment Session" eyebrow="Assessment workflow" />
        <div className="assessment-state assessment-state--error">
          <span className="assessment-state__icon" aria-hidden="true">!</span>
          <p role="alert">Unable to load this assessment session.</p>
        </div>
      </main>
    )
  }

  const session = sessionQuery.data
  const historyReturnTo = readHistoryReturnTo(location.state)
  const workflowView = resolveAssessmentWorkflow(session)

  return (
    <main className="assessment-page assessment-session-page">
      {historyReturnTo ? (
        <Link className="assessment-back-link" to={historyReturnTo}>← Back to history</Link>
      ) : (
        <Link
          className="assessment-back-link"
          to={`/assessments/${encodeURIComponent(session.assessment.code)}`}
        >
          ← Back to assessment
        </Link>
      )}

      <PageHeader
        eyebrow="Assessment workflow"
        title="Assessment Session"
        description="Move through each stage in order. Progress is saved against this session as the Backend finalizes each workflow transition."
        actions={<span className="assessment-status-badge">{session.status}</span>}
      />

      <dl className="assessment-session-meta">
        <div><dt>Assessment</dt><dd>{session.assessment.code}</dd></div>
        <div><dt>Version</dt><dd>{session.assessment.version}</dd></div>
        <div><dt>Status</dt><dd>{session.status}</dd></div>
      </dl>

      <WorkflowProgress view={workflowView} />

      <AssessmentWorkflowContent session={session} view={workflowView} />
    </main>
  )
}

export function AssessmentSessionPage() {
  const { sessionId } = useParams()

  if (!sessionId || !isValidSessionId(sessionId)) {
    return (
      <main className="assessment-page">
        <PageHeader title="Assessment Session" eyebrow="Assessment workflow" />
        <div className="assessment-state assessment-state--error">
          <span className="assessment-state__icon" aria-hidden="true">!</span>
          <div>
            <p role="alert">This assessment session URL is invalid.</p>
            <Link className="assessment-link-button" to="/assessments">Return to assessments</Link>
          </div>
        </div>
      </main>
    )
  }

  return <AssessmentSessionContent sessionId={sessionId} />
}

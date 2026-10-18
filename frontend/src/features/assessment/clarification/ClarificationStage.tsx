import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { Button } from '../../../shared/ui'
import { isApiError } from '../../../shared/api/apiError'
import {
  useCurrentClarificationTurnQuery,
  useStartClarificationMutation,
  useContinueClarificationMutation,
  useRetryClarificationMutation,
  useSkipClarificationMutation,
  useSkipRemainingClarificationsMutation,
} from '../api/assessmentQueries'
import type { AssessmentSession } from '../api/assessmentTypes'
import { assessmentQueryKeys } from '../api/assessmentQueryKeys'
import type { ClarificationMode } from '../workflow/assessmentWorkflowResolver'
import { buildClarificationReadModel } from './clarificationReadModel'

interface ClarificationStageProps {
  session: AssessmentSession
  dimensionCode: string
  mode: ClarificationMode
}

type ConfirmationAction = 'skip-one' | 'skip-remaining' | null

function stageHeading(mode: ClarificationMode): string {
  switch (mode) {
    case 'pending': return 'Clarification required'
    case 'in-progress': return 'Clarification in progress'
    case 'retryable': return 'Clarification needs retry'
  }
}

function stageDescription(mode: ClarificationMode): string {
  switch (mode) {
    case 'pending': return 'You may answer AI follow-up questions or use the deterministic skip fallback.'
    case 'in-progress': return 'Answer the latest saved AI question to continue.'
    case 'retryable': return 'The previous AI attempt failed. You may explicitly retry or skip.'
  }
}

function skipErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code === 'CLARIFICATION_NOT_ALLOWED') {
      return 'This clarification can no longer be skipped in the current assessment state. Refresh the session before trying another action.'
    }
    if (error.code === 'ASSESSMENT_SESSION_CONCURRENT_MODIFICATION') {
      return 'This assessment changed in another request or tab. Refresh the session before trying again.'
    }
  }
  return 'Unable to update the clarification workflow right now.'
}

function aiErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    switch (error.code) {
      case 'AI_CLARIFICATION_UNAVAILABLE':
        return 'AI clarification is currently unavailable. You can skip this dimension instead.'
      case 'CLARIFICATION_TURN_CONFLICT':
        return 'The AI turn has changed or is still processing. The latest Session and question are being reloaded. Do not resend the previous answer automatically.'
      case 'CLARIFICATION_NOT_ALLOWED':
        return 'This AI clarification action is no longer allowed. Refresh the Session to continue.'
      case 'ASSESSMENT_SESSION_NOT_FOUND':
        return 'This assessment Session is no longer available to your account.'
    }
  }
  return 'The AI request could not be confirmed. Refresh the current state before taking another action; do not automatically repeat the answer.'
}

export function ClarificationStage({ session, dimensionCode, mode }: ClarificationStageProps) {
  const [confirmationAction, setConfirmationAction] = useState<ConfirmationAction>(null)
  const [answerDraft, setAnswerDraft] = useState<{ sequence: number; text: string } | null>(null)
  const queryClient = useQueryClient()
  const currentTurnQuery = useCurrentClarificationTurnQuery(session.id, dimensionCode, mode === 'in-progress')
  const startMutation = useStartClarificationMutation(session.id, dimensionCode)
  const continueMutation = useContinueClarificationMutation(session.id, dimensionCode)
  const retryMutation = useRetryClarificationMutation(session.id, dimensionCode)
  const aiPending = startMutation.isPending || continueMutation.isPending || retryMutation.isPending
  const aiError = startMutation.error ?? continueMutation.error ?? retryMutation.error
  const activeQuestion = mode === 'in-progress' ? currentTurnQuery.data : undefined
  const currentAnswer = activeQuestion && answerDraft?.sequence === activeQuestion.assistantSequence
    ? answerDraft.text : ''
  const answerLength = Array.from(currentAnswer.trim()).length
  const answerValid = answerLength > 0 && answerLength <= 1000

  const skipOneMutation = useSkipClarificationMutation(session.id)
  const skipRemainingMutation = useSkipRemainingClarificationsMutation(session.id)
  const readModel = buildClarificationReadModel(session, dimensionCode)

  if (!readModel) {
    return (
      <section className="assessment-stage">
        <h2>Clarification</h2>
        <p className="assessment-inline-alert" role="alert">
          Unable to resolve clarification details from the current assessment state.
        </p>
      </section>
    )
  }

  const { evidence } = readModel
  const mutationPending = skipOneMutation.isPending || skipRemainingMutation.isPending || aiPending
  const mutationError = skipOneMutation.error ?? skipRemainingMutation.error

  function resetAiErrors() {
    startMutation.reset()
    continueMutation.reset()
    retryMutation.reset()
  }

  async function refreshAuthoritativeState() {
    await queryClient.invalidateQueries({
      queryKey: assessmentQueryKeys.session(session.id),
      exact: true,
    })
    if (mode === 'in-progress') {
      await currentTurnQuery.refetch()
    }
  }

  async function startAi() {
    if (mutationPending) return
    resetAiErrors()
    try {
      await startMutation.mutateAsync(undefined)
    } catch {
      // Stable error and safe server-state recovery are shown below.
    }
  }

  async function retryAi() {
    if (mutationPending) return
    resetAiErrors()
    try {
      await retryMutation.mutateAsync(undefined)
    } catch {
      // Never issue an automatic second model call.
    }
  }

  async function sendAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!activeQuestion || !answerValid || mutationPending) return
    resetAiErrors()
    try {
      await continueMutation.mutateAsync({
        assistantSequence: activeQuestion.assistantSequence,
        answer: currentAnswer.trim(),
      })
      setAnswerDraft(null)
    } catch {
      // Keep draft; re-read authoritative Session and current turn before any retry.
    }
  }

  function beginConfirmation(action: Exclude<ConfirmationAction, null>) {
    skipOneMutation.reset()
    skipRemainingMutation.reset()
    setConfirmationAction(action)
  }

  function cancelConfirmation() {
    if (!mutationPending) setConfirmationAction(null)
  }

  async function handleSkipOne() {
    try {
      await skipOneMutation.mutateAsync(dimensionCode)
      setConfirmationAction(null)
    } catch {
      // Error rendered below.
    }
  }

  async function handleSkipRemaining() {
    try {
      await skipRemainingMutation.mutateAsync()
      setConfirmationAction(null)
    } catch {
      // Error rendered below.
    }
  }

  return (
    <section className="assessment-stage clarification-stage" aria-labelledby="clarification-heading">
      <div className="assessment-stage__header">
        <div>
          <p className="assessment-stage__eyebrow">Stage 2 of 4 · {dimensionCode}</p>
          <h2 id="clarification-heading">{stageHeading(mode)}</h2>
          <p>{stageDescription(mode)}</p>
        </div>
        <span className="assessment-stage__dimension-badge" data-dimension={readModel.dimensionCode} aria-label={`Dimension ${readModel.dimensionCode}`} />
      </div>

      <div className="clarification-grid">
        <div className="clarification-summary">
          <dl className="assessment-data-grid">
            <div><dt>Current dimension</dt><dd>{readModel.dimensionCode}</dd></div>
            <div><dt>Clarification state</dt><dd>{readModel.status}</dd></div>
            <div>
              <dt>Questionnaire preference</dt>
              <dd>{readModel.questionnairePreference ?? 'No baseline preference (exact tie)'}</dd>
            </div>
          </dl>

          <section className="clarification-evidence" aria-labelledby="questionnaire-evidence-heading">
            <h3 id="questionnaire-evidence-heading">Questionnaire evidence</h3>
            <p className="clarification-evidence__text">
              {evidence.poleA}: {evidence.poleAPercentage.toFixed(1)}% · {evidence.poleB}: {evidence.poleBPercentage.toFixed(1)}%
            </p>
            <div className="clarification-evidence__bar" aria-hidden="true">
              <span style={{ width: `${evidence.poleAPercentage}%` }} />
            </div>
            <div className="clarification-evidence__labels" aria-hidden="true">
              <span>{evidence.poleA}</span><span>{evidence.poleB}</span>
            </div>
          </section>
        </div>

        <aside className="clarification-queue" aria-label="Clarification queue">
          <h3>Workflow queue</h3>
          {session.workflow.pendingClarificationDimensions.length > 0 && (
            <p>Pending dimensions: {session.workflow.pendingClarificationDimensions.join(', ')}</p>
          )}
          {session.workflow.retryableClarificationDimensions.length > 0 && (
            <p>Retryable dimensions: {session.workflow.retryableClarificationDimensions.join(', ')}</p>
          )}
          <p className="clarification-queue__note">
            Each response is bound to the latest saved AI turn. You can refresh safely without repeating a model call.
          </p>
        </aside>
      </div>

      <section className="clarification-ai" aria-labelledby="clarification-ai-heading">
        <div>
          <p className="assessment-stage__eyebrow">Optional AI clarification</p>
          <h3 id="clarification-ai-heading">Explore your preference</h3>
          <p className="clarification-ai__hint">
            The AI uses your questionnaire evidence to ask a focused question.
            Answers are temporary. You may skip instead; your assessment can still finish.
          </p>
        </div>

        {mode === 'pending' && (
          <div className="clarification-ai__actions">
            <Button type="button" onClick={() => { void startAi() }}
              isLoading={startMutation.isPending} loadingLabel="Starting AI clarification..."
              disabled={mutationPending}>
              Start AI clarification
            </Button>
          </div>
        )}

        {mode === 'retryable' && (
          <div className="clarification-ai__actions">
            <Button type="button" onClick={() => { void retryAi() }}
              isLoading={retryMutation.isPending} loadingLabel="Retrying AI clarification..."
              disabled={mutationPending}>
              Retry AI clarification
            </Button>
          </div>
        )}

        {mode === 'in-progress' && (
          <>
            {currentTurnQuery.isPending && (
              <p role="status">Loading the latest saved AI question...</p>
            )}
            {currentTurnQuery.isError && (
              <p className="assessment-inline-alert" role="alert">
                {isApiError(currentTurnQuery.error) &&
                currentTurnQuery.error.code === 'CLARIFICATION_TURN_CONFLICT'
                  ? 'There is no answerable question yet. The AI may still be processing, or this turn expired. Refresh status to recover.'
                  : 'Unable to read the latest AI question. Refresh status to recover.'}
              </p>
            )}
            {activeQuestion && !currentTurnQuery.isError && (
              <form className="clarification-ai__form" onSubmit={(event) => { void sendAnswer(event) }}>
                <div className="clarification-ai__question">
                  <span>Current question · turn {activeQuestion.assistantSequence}</span>
                  <p>{activeQuestion.question}</p>
                </div>
                <label htmlFor={`clarification-answer-${dimensionCode}`}>Your answer</label>
                <textarea
                  id={`clarification-answer-${dimensionCode}`}
                  value={currentAnswer}
                  onChange={(event) => setAnswerDraft({
                    sequence: activeQuestion.assistantSequence,
                    text: event.target.value,
                  })}
                  rows={4}
                  disabled={mutationPending || currentTurnQuery.isFetching}
                  placeholder="Describe what usually feels most natural to you..."
                />
                <p className="clarification-ai__counter">
                  {answerLength} / 1000 Unicode characters
                </p>
                {answerLength > 1000 && (
                  <p role="alert" className="assessment-inline-alert">
                    Keep your answer within 1000 characters.
                  </p>
                )}
                <div className="clarification-ai__actions">
                  <Button type="submit" disabled={!answerValid || mutationPending || currentTurnQuery.isFetching}
                    isLoading={continueMutation.isPending} loadingLabel="Sending answer...">
                    Send answer
                  </Button>
                </div>
              </form>
            )}
            <div className="clarification-ai__actions">
              <Button type="button" variant="ghost" disabled={mutationPending}
                onClick={() => { void refreshAuthoritativeState() }}>
                Refresh current status
              </Button>
            </div>
          </>
        )}

        {aiError && (
          <p className="assessment-inline-alert" role="alert">
            {aiErrorMessage(aiError)}
          </p>
        )}
      </section>

      <section className="clarification-actions" aria-labelledby="clarification-actions-heading">
        <div>
          <p className="assessment-stage__eyebrow">Manual fallback</p>
          <h3 id="clarification-actions-heading">Clarification actions</h3>
        </div>

        {confirmationAction === null && (
          <div className="clarification-actions__buttons">
            <Button type="button" variant="secondary" onClick={() => beginConfirmation('skip-one')} disabled={mutationPending}>
              Skip this dimension
            </Button>
            <Button type="button" variant="ghost" onClick={() => beginConfirmation('skip-remaining')} disabled={mutationPending}>
              Skip all remaining clarifications
            </Button>
          </div>
        )}

        {confirmationAction === 'skip-one' && (
          <div className="assessment-confirmation">
            <p>Skip {dimensionCode}? This clarification will become terminal and cannot be resumed.</p>
            <div className="assessment-confirmation__actions">
              <Button type="button" variant="ghost" onClick={cancelConfirmation} disabled={mutationPending}>Cancel</Button>
              <Button
                type="button"
                variant="danger"
                onClick={handleSkipOne}
                isLoading={skipOneMutation.isPending}
                loadingLabel="Skipping..."
              >
                Confirm skip {dimensionCode}
              </Button>
            </div>
          </div>
        )}

        {confirmationAction === 'skip-remaining' && (
          <div className="assessment-confirmation">
            <p>
              Skip every remaining clarification? Pending, active, and retryable clarification work will be marked as skipped. Exact ties may still require a tie-break.
            </p>
            <div className="assessment-confirmation__actions">
              <Button type="button" variant="ghost" onClick={cancelConfirmation} disabled={mutationPending}>Cancel</Button>
              <Button
                type="button"
                variant="danger"
                onClick={handleSkipRemaining}
                isLoading={skipRemainingMutation.isPending}
                loadingLabel="Skipping remaining..."
              >
                Confirm skip all remaining
              </Button>
            </div>
          </div>
        )}

        {mutationError && (
          <p className="assessment-inline-alert" role="alert">{skipErrorMessage(mutationError)}</p>
        )}
      </section>
    </section>
  )
}
